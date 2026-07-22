package com.travlejava.ai.gateway.filter;

import com.travlejava.ai.common.exception.AuthException;
import com.travlejava.ai.common.exception.BaseException;
import com.travlejava.ai.common.response.ResponseEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Skip authentication for auth service and swagger paths
            if (isAuthPath(request) || isSwaggerPath(request)) {
                return chain.filter(exchange);
            }

            String token = resolveToken(request);
            if (!StringUtils.hasText(token)) {
                return onError(exchange, "JWT token is missing", HttpStatus.UNAUTHORIZED, ResponseEnum.TOKEN_MISSING);
            }

            try {
                if (!validateToken(token)) {
                    return onError(exchange, "JWT token is invalid or expired", HttpStatus.UNAUTHORIZED, ResponseEnum.TOKEN_INVALID);
                }
                Claims claims = getClaimsFromToken(token);
                // Add claims to request headers for downstream services
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-Auth-User-Id", claims.getSubject())
                        .header("X-Auth-Roles", claims.get("roles", String.class))
                        .build();
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } catch (BaseException ex) {
                return onError(exchange, ex.getMessage(), HttpStatus.UNAUTHORIZED, ex.getResponseCode());
            } catch (Exception ex) {
                return onError(exchange, "Authentication failed", HttpStatus.UNAUTHORIZED, ResponseEnum.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(org.springframework.web.server.ServerWebExchange exchange, String err, HttpStatus httpStatus, ResponseEnum responseCode) {
        exchange.getResponse().setStatusCode(httpStatus);
        // Optionally, add a custom error body
        return exchange.getResponse().setComplete();
    }

    private String resolveToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims getClaimsFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    private boolean isAuthPath(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        return path.startsWith("/auth/login") || path.startsWith("/auth/register");
    }

    private boolean isSwaggerPath(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        return path.startsWith("/swagger") || path.startsWith("/v3/api-docs");
    }

    public static class Config {
        // Put the configuration properties
    }
}
