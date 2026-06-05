package com.undernine.utils.http.openapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.undernine.utils.core.json.JsonUtils;
import com.undernine.utils.http.enums.HttpMethod;
import com.undernine.utils.http.response.HttpResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("deprecation")
class DefaultOpenApiClientTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void shouldAttachBearerTokenWhenTokenProvided() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"ok\":true}"));
        OpenApiClient client = new DefaultOpenApiClient(
                testOptions(),
                request -> "access-token",
                RequestSigner.noop(),
                ApiErrorDecoder.httpStatus()
        );

        OpenApiResponse<String> response = client.execute(OpenApiRequest.builder()
                .url(server.url("/token").toString())
                .operationName("token-api")
                .build());

        RecordedRequest recordedRequest = server.takeRequest();
        assertThat(response.isSuccess()).isTrue();
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer access-token");
    }

    @Test
    void shouldSkipAuthorizationWhenTokenBlank() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{}"));
        OpenApiClient client = new DefaultOpenApiClient(
                testOptions(),
                request -> " ",
                RequestSigner.noop(),
                ApiErrorDecoder.httpStatus()
        );

        client.execute(OpenApiRequest.builder()
                .url(server.url("/blank-token").toString())
                .build());

        RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.getHeader("Authorization")).isNull();
    }

    @Test
    void shouldApplySignerHeadersAndQueryParams() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{}"));
        OpenApiClient client = new DefaultOpenApiClient(
                testOptions(),
                AccessTokenProvider.noop(),
                request -> request.header("X-Sign", "signed").query("timestamp", "123456"),
                ApiErrorDecoder.httpStatus()
        );

        client.execute(OpenApiRequest.builder()
                .url(server.url("/sign").toString())
                .query("appId", "demo")
                .build());

        RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.getHeader("X-Sign")).isEqualTo("signed");
        assertThat(recordedRequest.getRequestUrl().queryParameter("appId")).isEqualTo("demo");
        assertThat(recordedRequest.getRequestUrl().queryParameter("timestamp")).isEqualTo("123456");
    }

    @Test
    void shouldPropagateTraceIdAndIdempotencyKey() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{}"));
        OpenApiClient client = new DefaultOpenApiClient(testOptions());

        client.execute(OpenApiRequest.builder()
                .url(server.url("/trace").toString())
                .traceId("trace-001")
                .idempotencyKey("idem-001")
                .build());

        RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.getHeader("X-Trace-Id")).isEqualTo("trace-001");
        assertThat(recordedRequest.getHeader("Idempotency-Key")).isEqualTo("idem-001");
    }

    @Test
    void shouldDecodeBusinessErrorFromSuccessfulHttpResponse() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"code\":\"LIMITED\",\"message\":\"too many requests\"}"));
        OpenApiClient client = new DefaultOpenApiClient(
                testOptions(),
                AccessTokenProvider.noop(),
                RequestSigner.noop(),
                jsonCodeDecoder()
        );

        OpenApiResponse<Map<String, Object>> response = client.execute(
                OpenApiRequest.builder()
                        .url(server.url("/business-error").toString())
                        .build(),
                new TypeReference<Map<String, Object>>() {
                });

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getData()).isNull();
        assertThat(response.getErrorCode()).isEqualTo("LIMITED");
        assertThat(response.getErrorMessage()).isEqualTo("too many requests");
    }

    @Test
    void shouldRetryRetryableBusinessErrorOnly() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"code\":\"BUSY\",\"message\":\"retry later\"}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"code\":\"OK\",\"value\":\"done\"}"));
        OpenApiClient client = new DefaultOpenApiClient(
                OpenApiClientOptions.builder().maxRetries(1).retryInterval(0).build(),
                AccessTokenProvider.noop(),
                RequestSigner.noop(),
                jsonCodeDecoder()
        );

        OpenApiResponse<Map<String, Object>> response = client.execute(
                OpenApiRequest.builder()
                        .url(server.url("/retryable").toString())
                        .method(HttpMethod.GET)
                        .build(),
                new TypeReference<Map<String, Object>>() {
                });

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).containsEntry("value", "done");
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    void shouldNotBlockCurrentThreadBeforeRetryByDefault() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"code\":\"BUSY\",\"message\":\"retry later\"}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"code\":\"OK\",\"value\":\"done\"}"));
        OpenApiClient client = new DefaultOpenApiClient(
                OpenApiClientOptions.builder().maxRetries(1).retryInterval(3_000).build(),
                AccessTokenProvider.noop(),
                RequestSigner.noop(),
                jsonCodeDecoder()
        );

        long startedAt = System.currentTimeMillis();
        OpenApiResponse<Map<String, Object>> response = client.execute(
                OpenApiRequest.builder()
                        .url(server.url("/retry-without-sleep").toString())
                        .build(),
                new TypeReference<Map<String, Object>>() {
                });

        assertThat(response.isSuccess()).isTrue();
        assertThat(System.currentTimeMillis() - startedAt).isLessThan(2_500L);
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    void shouldNotRetryNonRetryableBusinessError() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"code\":\"INVALID\",\"message\":\"bad request\"}"));
        OpenApiClient client = new DefaultOpenApiClient(
                OpenApiClientOptions.builder().maxRetries(2).retryInterval(0).build(),
                AccessTokenProvider.noop(),
                RequestSigner.noop(),
                jsonCodeDecoder()
        );

        OpenApiResponse<String> response = client.execute(OpenApiRequest.builder()
                .url(server.url("/non-retryable").toString())
                .build());

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("INVALID");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    private OpenApiClientOptions testOptions() {
        return OpenApiClientOptions.builder()
                .maxRetries(0)
                .retryInterval(0)
                .build();
    }

    private ApiErrorDecoder jsonCodeDecoder() {
        return (request, httpResponse) -> {
            if (!httpResponse.isSuccess()) {
                return ApiErrorDecoder.httpStatus().decode(request, httpResponse);
            }
            Map<String, Object> body = JsonUtils.fromJson(httpResponse.asString(), new TypeReference<Map<String, Object>>() {
            });
            String code = String.valueOf(body.get("code"));
            if ("OK".equals(code)) {
                return ApiErrorDecodeResult.success();
            }
            return ApiErrorDecodeResult.failure(code, String.valueOf(body.get("message")), "BUSY".equals(code));
        };
    }
}
