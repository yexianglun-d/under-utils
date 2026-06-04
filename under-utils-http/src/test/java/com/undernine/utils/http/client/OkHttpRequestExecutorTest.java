package com.undernine.utils.http.client;

import com.undernine.utils.http.config.HttpConfig;
import com.undernine.utils.http.exception.HttpTimeoutException;
import com.undernine.utils.http.request.HttpRequest;
import com.undernine.utils.http.response.HttpResponse;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OkHttpRequestExecutorTest {

    private MockWebServer mockWebServer;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        baseUrl = mockWebServer.url("/").toString();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldReuseOkHttpClientForSameClientConfig() throws Exception {
        HttpConfig config = HttpConfig.builder()
                .connectTimeout(1000)
                .readTimeout(2000)
                .writeTimeout(3000)
                .maxConnections(50)
                .keepAliveTime(30_000)
                .build();

        OkHttpRequestExecutor first = new OkHttpRequestExecutor(config);
        OkHttpRequestExecutor second = new OkHttpRequestExecutor(config.toBuilder().build());

        assertThat(clientOf(first)).isSameAs(clientOf(second));
    }

    @Test
    void shouldApplyRequestLevelTimeout() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("slow response")
                .setBodyDelay(200, TimeUnit.MILLISECONDS)
                .setResponseCode(200));

        assertThatThrownBy(() -> HttpRequest.get(baseUrl + "slow")
                .timeout(20)
                .retry(0)
                .config(HttpConfig.builder().readTimeout(10_000).build())
                .execute())
                .isInstanceOf(HttpTimeoutException.class);
    }

    @Test
    void shouldDownloadToFileWithoutBufferingBodyInResponse(@TempDir Path tempDir) throws IOException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("File content")
                .setResponseCode(200)
                .addHeader("Content-Type", "text/plain"));
        File targetFile = tempDir.resolve("downloaded.txt").toFile();

        HttpResponse response = HttpRequest.get(baseUrl + "files/123")
                .downloadToFile(targetFile);

        assertThat(Files.readString(targetFile.toPath())).isEqualTo("File content");
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getHeader("Content-Type")).isEqualTo("text/plain");
        assertThat(response.getBody()).isNull();
        assertThat(response.getBodyBytes()).isNull();
    }

    private OkHttpClient clientOf(OkHttpRequestExecutor executor) throws Exception {
        Field clientField = OkHttpRequestExecutor.class.getDeclaredField("client");
        clientField.setAccessible(true);
        return (OkHttpClient) clientField.get(executor);
    }
}
