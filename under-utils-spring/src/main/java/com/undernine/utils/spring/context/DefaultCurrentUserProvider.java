package com.undernine.utils.spring.context;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 默认当前用户提供器。
 * <p>
 * 默认优先读取 {@link OperationContextHolder}，没有上下文时退化为客户端 IP，再没有时返回 {@code anonymous}。
 * 只有显式开启可信身份 Header 时才会读取 {@code X-User-Id}。
 * </p>
 *
 * @author Under-Utils Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DefaultCurrentUserProvider implements CurrentUserProvider {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ANONYMOUS = "anonymous";

    private final boolean trustedIdentityHeaders;

    public DefaultCurrentUserProvider() {
        this(false);
    }

    public DefaultCurrentUserProvider(boolean trustedIdentityHeaders) {
        this.trustedIdentityHeaders = trustedIdentityHeaders;
    }

    @Override
    public String getCurrentUserId() {
        OperationContext context = OperationContextHolder.getContext();
        if (context != null && isNotBlank(context.getUserId())) {
            return context.getUserId().trim();
        }

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return ANONYMOUS;
        }

        HttpServletRequest request = attrs.getRequest();
        if (trustedIdentityHeaders) {
            String userId = request.getHeader(USER_ID_HEADER);
            if (isNotBlank(userId)) {
                return userId.trim();
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return isNotBlank(remoteAddr) ? remoteAddr.trim() : ANONYMOUS;
    }

    public boolean isTrustedIdentityHeaders() {
        return trustedIdentityHeaders;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
