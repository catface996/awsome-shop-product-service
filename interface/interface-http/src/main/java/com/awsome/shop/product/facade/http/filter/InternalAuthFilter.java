package com.awsome.shop.product.facade.http.filter;

import com.awsome.shop.product.common.enums.StockErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 内网鉴权过滤器
 *
 * <p>保护所有 {@code /api/v1/private/**} 路径，仅放行携带正确 {@code X-Internal-Token}
 * 的内网/网关请求，外部直连请求一律拒绝（401）。</p>
 *
 * <p>通过 Spring Boot 自动注册为 Servlet 过滤器（默认匹配 /*），并由
 * {@link #shouldNotFilter} 将实际生效范围限制为内部路径前缀。</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class InternalAuthFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PATH_PREFIX = "/api/v1/private/";
    private static final String TOKEN_HEADER = "X-Internal-Token";

    @Value("${awsomeshop.internal-auth.token}")
    private String expectedToken;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || !token.equals(expectedToken)) {
            String reason = token == null ? "missing token" : "invalid token";
            log.warn("[InternalAuthFilter] 拒绝内部接口请求: path={}, reason={}, remoteAddr={}",
                    request.getRequestURI(), reason, request.getRemoteAddr());

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"code\":\"" + StockErrorCode.INTERNAL_UNAUTHORIZED.getCode()
                            + "\",\"message\":\"" + StockErrorCode.INTERNAL_UNAUTHORIZED.getMessage()
                            + "\",\"data\":null}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
