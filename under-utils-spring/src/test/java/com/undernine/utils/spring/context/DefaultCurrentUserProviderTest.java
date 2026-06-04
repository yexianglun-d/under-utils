package com.undernine.utils.spring.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultCurrentUserProviderTest {

    @AfterEach
    void tearDown() {
        OperationContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldIgnoreUserHeaderByDefault() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "user-a");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String userId = new DefaultCurrentUserProvider().getCurrentUserId();

        assertThat(userId).isEqualTo("127.0.0.1");
    }

    @Test
    void shouldReadUserHeaderWhenTrustedIdentityHeadersEnabled() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", " user-a ");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String userId = new DefaultCurrentUserProvider(true).getCurrentUserId();

        assertThat(userId).isEqualTo("user-a");
    }

    @Test
    void shouldPreferOperationContext() {
        OperationContext context = OperationContext.builder()
                .userId("user-context")
                .build();

        try (OperationContextHolder.Scope ignored = OperationContextHolder.scope(context)) {
            assertThat(new DefaultCurrentUserProvider().getCurrentUserId()).isEqualTo("user-context");
        }
    }
}
