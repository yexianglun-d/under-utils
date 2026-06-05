package com.undernine.utils.spring.idempotent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonIdempotencyResultCodecTest {

    private final JacksonIdempotencyResultCodec codec = new JacksonIdempotencyResultCodec();

    @Test
    void shouldSerializeAndDeserializeObjectResult() {
        String payload = codec.serialize(new Result("A001", 100), Result.class);

        Object decoded = codec.deserialize(payload, Result.class);

        assertThat(decoded)
                .isInstanceOfSatisfying(Result.class, result -> {
                    assertThat(result.orderNo()).isEqualTo("A001");
                    assertThat(result.amount()).isEqualTo(100);
                });
    }

    @Test
    void shouldKeepNullResultAsNull() {
        assertThat(codec.serialize(null, String.class)).isNull();
        assertThat(codec.deserialize(null, String.class)).isNull();
    }

    @Test
    void shouldKeepVoidResultAsNull() {
        assertThat(codec.serialize("ignored", Void.TYPE)).isNull();
        assertThat(codec.deserialize("{\"value\":1}", Void.TYPE)).isNull();
    }

    record Result(String orderNo, int amount) {
    }
}
