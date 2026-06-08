package com.ecommerce.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Filtro global que loga todas as requisições passando pelo API Gateway.
 *
 * Registra método, path, IP do cliente, serviço de destino,
 * status code e latência em milissegundos.
 */
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);
    private static final String REQUEST_TIME_ATTR = "requestStartTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = request.getId();
        HttpMethod method = request.getMethod();
        String path = request.getURI().getPath();
        String clientIp = getClientIp(request);

        exchange.getAttributes().put(REQUEST_TIME_ATTR, Instant.now());

        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = (route != null) ? route.getId() : "unknown";
        String routeUri = (route != null) ? route.getUri().toString() : "unknown";

        log.info(">>> GATEWAY REQUEST [{}] {} {} | Client: {} | Route: {} -> {}",
                requestId, method, path, clientIp, routeId, routeUri);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            Instant startTime = exchange.getAttribute(REQUEST_TIME_ATTR);
            long latencyMs = (startTime != null)
                    ? java.time.Duration.between(startTime, Instant.now()).toMillis()
                    : -1;

            int statusCode = (response.getStatusCode() != null)
                    ? response.getStatusCode().value()
                    : 0;

            log.info("<<< GATEWAY RESPONSE [{}] Status: {} | Latency: {}ms | Route: {}",
                    requestId, statusCode, latencyMs, routeId);
        }));
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return (request.getRemoteAddress() != null)
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
