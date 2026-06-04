package com.undernine.utils.spring.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultCurrentTenantProviderTest {

    @AfterEach
    void tearDown() {
        OperationContextHolder.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldIgnoreTenantHeaderByDefault() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant-a");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String tenantId = new DefaultCurrentTenantProvider().getCurrentTenantId();

        assertThat(tenantId).isEqualTo("default");
    }

    @Test
    void shouldReadTenantHeaderWhenTrustedIdentityHeadersEnabled() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", " tenant-a ");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String tenantId = new DefaultCurrentTenantProvider(true).getCurrentTenantId();

        assertThat(tenantId).isEqualTo("tenant-a");
    }

    @Test
    void shouldPreferOperationContext() {
        OperationContext context = OperationContext.builder()
                .tenantId("tenant-context")
                .build();

        try (OperationContextHolder.Scope ignored = OperationContextHolder.scope(context)) {
            assertThat(new DefaultCurrentTenantProvider().getCurrentTenantId()).isEqualTo("tenant-context");
        }
    }
}
