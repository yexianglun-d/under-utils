package com.undernine.utils.spring.context;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 默认当前租户提供器。
 * <p>
 * 默认只读取 {@link OperationContextHolder}，没有上下文时返回 {@code default}。
 * 只有显式开启可信身份 Header 时才会读取 {@code X-Tenant-Id}。
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DefaultCurrentTenantProvider implements CurrentTenantProvider {

    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String DEFAULT_TENANT = "default";

    private final boolean trustedIdentityHeaders;

    public DefaultCurrentTenantProvider() {
        this(false);
    }

    public DefaultCurrentTenantProvider(boolean trustedIdentityHeaders) {
        this.trustedIdentityHeaders = trustedIdentityHeaders;
    }

    @Override
    public String getCurrentTenantId() {
        OperationContext context = OperationContextHolder.getContext();
        if (context != null && isNotBlank(context.getTenantId())) {
            return context.getTenantId().trim();
        }

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return DEFAULT_TENANT;
        }

        HttpServletRequest request = attrs.getRequest();
        if (trustedIdentityHeaders) {
            String tenantId = request.getHeader(TENANT_ID_HEADER);
            if (isNotBlank(tenantId)) {
                return tenantId.trim();
            }
        }
        return DEFAULT_TENANT;
    }

    public boolean isTrustedIdentityHeaders() {
        return trustedIdentityHeaders;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
