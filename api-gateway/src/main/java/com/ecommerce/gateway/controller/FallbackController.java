package com.ecommerce.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller de fallback acionado pelo Circuit Breaker quando
 * um microsserviço está indisponível. Retorna respostas JSON
 * amigáveis em vez de erros 503 genéricos.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping(value = "/produto", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> produtoFallback(ServerWebExchange exchange) {
        return buildFallbackResponse("produto-service", exchange);
    }

    @RequestMapping(value = "/carrinho", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> carrinhoFallback(ServerWebExchange exchange) {
        return buildFallbackResponse("carrinho-service", exchange);
    }

    @RequestMapping(value = "/pedido", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> pedidoFallback(ServerWebExchange exchange) {
        return buildFallbackResponse("pedido-service", exchange);
    }

    @RequestMapping(value = "/pagamento", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> pagamentoFallback(ServerWebExchange exchange) {
        return buildFallbackResponse("pagamento-service", exchange);
    }

    private Mono<Map<String, Object>> buildFallbackResponse(String serviceName, ServerWebExchange exchange) {
        log.warn("Circuit Breaker ativado para: {}", serviceName);

        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("service", serviceName);
        response.put("message", String.format(
                "O servico '%s' esta temporariamente indisponivel. Tente novamente em instantes.",
                serviceName
        ));
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return Mono.just(response);
    }
}
