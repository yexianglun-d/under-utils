package com.undernine.utils.security.mask;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingJsonSerializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMaskAnnotatedResponseFields() throws Exception {
        String json = objectMapper.writeValueAsString(new UserResponse(
                "13812345678",
                "a@example.com",
                "secret"
        ));

        assertThat(json).contains("\"phone\":\"138****5678\"");
        assertThat(json).contains("\"email\":\"a@example.com\"");
        assertThat(json).contains("\"password\":\"***\"");
    }

    @Test
    void shouldApplyCustomMaskRule() throws Exception {
        String json = objectMapper.writeValueAsString(new CustomResponse("ABCDEFGH"));

        assertThat(json).contains("\"value\":\"AB****GH\"");
    }

    static class UserResponse {
        @Mask(type = MaskType.MOBILE_PHONE)
        private final String phone;
        @Mask(type = MaskType.EMAIL)
        private final String email;
        @Mask(type = MaskType.PASSWORD)
        private final String password;

        UserResponse(String phone, String email, String password) {
            this.phone = phone;
            this.email = email;
            this.password = password;
        }

        public String getPhone() {
            return phone;
        }

        public String getEmail() {
            return email;
        }

        public String getPassword() {
            return password;
        }
    }

    static class CustomResponse {
        @Mask(type = MaskType.CUSTOM, customRule = "2,2")
        private final String value;

        CustomResponse(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
