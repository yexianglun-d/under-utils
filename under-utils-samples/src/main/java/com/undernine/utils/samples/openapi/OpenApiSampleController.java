package com.undernine.utils.samples.openapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.undernine.utils.core.json.JsonException;
import com.undernine.utils.http.enums.HttpMethod;
import com.undernine.utils.http.openapi.ApiErrorDecoder;
import com.undernine.utils.http.openapi.ApiErrorDecodeResult;
import com.undernine.utils.http.openapi.DefaultOpenApiClient;
import com.undernine.utils.http.openapi.OpenApiClient;
import com.undernine.utils.http.openapi.OpenApiClientOptions;
import com.undernine.utils.http.openapi.OpenApiRequest;
import com.undernine.utils.http.openapi.OpenApiResponse;
import com.undernine.utils.http.openapi.RefreshingAccessTokenProvider;
import com.undernine.utils.http.openapi.RequestSigner;
import com.undernine.utils.spring.context.OperationContext;
import com.undernine.utils.spring.context.OperationContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/samples/openapi")
public class OpenApiSampleController {

    private static final ObjectMapper JSON_MAPPER = createJsonMapper();
    private static final String MASKED_AUTHORIZATION = "******";

    private final AtomicInteger tokenVersion = new AtomicInteger();
    private final RefreshingAccessTokenProvider accessTokenProvider = new RefreshingAccessTokenProvider(
            request -> RefreshingAccessTokenProvider.AccessToken.of(
                    "sample-token-" + tokenVersion.incrementAndGet(),
                    Instant.now().plusSeconds(30)
            ),
            Duration.ofSeconds(5)
    );

    @PostMapping("/orders")
    public OpenApiResponse<GatewayOrderResponse> createOrder(@RequestBody GatewayOrderCommand command) {
        String gatewayUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/samples/openapi/mock-gateway/orders")
                .toUriString();
        OperationContext context = OperationContextHolder.getContext();

        OpenApiClient client = newClient(ApiErrorDecoder.httpStatus());

        OpenApiRequest request = OpenApiRequest.builder()
                .url(gatewayUrl)
                .method(HttpMethod.POST)
                .body(command)
                .traceId(context == null ? null : context.getTraceId())
                .idempotencyKey(command.requestNo())
                .operationName("sampleCreateOrder")
                .build();
        return client.execute(request, GatewayOrderResponse.class);
    }

    @PostMapping("/orders/envelope")
    public OpenApiResponse<GatewayOrderEnvelope> createOrderWithBusinessErrorDecode(
            @RequestBody GatewayOrderCommand command) {
        String gatewayUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/samples/openapi/mock-gateway/envelope-orders")
                .toUriString();
        OperationContext context = OperationContextHolder.getContext();

        OpenApiRequest request = OpenApiRequest.builder()
                .url(gatewayUrl)
                .method(HttpMethod.POST)
                .body(command)
                .traceId(context == null ? null : context.getTraceId())
                .idempotencyKey(command.requestNo())
                .operationName("sampleCreateOrderWithBusinessErrorDecode")
                .build();
        return newClient(gatewayEnvelopeDecoder()).execute(request, GatewayOrderEnvelope.class);
    }

    @PostMapping("/mock-gateway/orders")
    public GatewayOrderResponse mockGateway(@RequestBody GatewayOrderCommand command,
                                            @RequestHeader Map<String, String> headers) {
        return new GatewayOrderResponse(
                "remote-" + command.requestNo(),
                maskedHeader(headers, "Authorization"),
                header(headers, "X-Signature"),
                header(headers, "X-Trace-Id"),
                header(headers, "Idempotency-Key")
        );
    }

    @PostMapping("/mock-gateway/envelope-orders")
    public GatewayOrderEnvelope mockGatewayEnvelope(@RequestBody GatewayOrderCommand command,
                                                    @RequestHeader Map<String, String> headers) {
        if (command.quantity() <= 0) {
            return new GatewayOrderEnvelope("INVALID_QUANTITY", "quantity must be greater than 0", null);
        }
        GatewayOrderResponse data = new GatewayOrderResponse(
                "remote-" + command.requestNo(),
                maskedHeader(headers, "Authorization"),
                header(headers, "X-Signature"),
                header(headers, "X-Trace-Id"),
                header(headers, "Idempotency-Key")
        );
        return new GatewayOrderEnvelope("OK", "success", data);
    }

    private OpenApiClient newClient(ApiErrorDecoder errorDecoder) {
        RequestSigner signer = request -> request.header("X-Signature", sampleSignature(request));
        return new DefaultOpenApiClient(
                OpenApiClientOptions.builder()
                        .connectTimeout(2000)
                        .readTimeout(2000)
                        .maxRetries(1)
                        .loggingEnabled(true)
                        .build(),
                accessTokenProvider,
                signer,
                errorDecoder
        );
    }

    private ApiErrorDecoder gatewayEnvelopeDecoder() {
        return (request, httpResponse) -> {
            if (!httpResponse.isSuccess()) {
                return ApiErrorDecoder.httpStatus().decode(request, httpResponse);
            }
            Map<String, Object> body = parseEnvelopeBody(httpResponse.asString());
            String code = String.valueOf(body.get("code"));
            if ("OK".equals(code)) {
                return ApiErrorDecodeResult.success();
            }
            return ApiErrorDecodeResult.failure(code, String.valueOf(body.get("message")), "BUSY".equals(code));
        };
    }

    private String sampleSignature(OpenApiRequest request) {
        String source = request.getOperationName() + ":" + request.getIdempotencyKey() + ":" + request.getTraceId();
        return Integer.toHexString(source.hashCode());
    }

    private static Map<String, Object> parseEnvelopeBody(String rawBody) {
        try {
            return JSON_MAPPER.readValue(rawBody, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to deserialize sample gateway envelope", e);
        }
    }

    private static String header(Map<String, String> headers, String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return "";
    }

    private static String maskedHeader(Map<String, String> headers, String name) {
        return header(headers, name).isBlank() ? "" : MASKED_AUTHORIZATION;
    }

    private static ObjectMapper createJsonMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }

    public record GatewayOrderCommand(String requestNo, String skuId, int quantity) {
    }

    public record GatewayOrderResponse(String remoteOrderId,
                                       String authorization,
                                       String signature,
                                       String traceId,
                                       String idempotencyKey) {
    }

    public record GatewayOrderEnvelope(String code, String message, GatewayOrderResponse data) {
    }
}
