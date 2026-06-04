package com.undernine.utils.samples.openapi;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiSampleControllerTest {

    private final OpenApiSampleController controller = new OpenApiSampleController();

    @Test
    void mockGatewayMasksAuthorizationHeader() {
        OpenApiSampleController.GatewayOrderCommand command =
                new OpenApiSampleController.GatewayOrderCommand("req-1", "sku-1", 1);
        Map<String, String> headers = Map.of(
                "authorization", "Bearer sample-token",
                "X-Signature", "sample-signature",
                "X-Trace-Id", "trace-1",
                "Idempotency-Key", "req-1"
        );

        OpenApiSampleController.GatewayOrderResponse response = controller.mockGateway(command, headers);

        assertThat(response.authorization()).isEqualTo("******");
        assertThat(response.signature()).isEqualTo("sample-signature");
        assertThat(response.traceId()).isEqualTo("trace-1");
        assertThat(response.idempotencyKey()).isEqualTo("req-1");
    }
}
