package com.ecommerce.carrinho.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestConfig {

    /**
     * RestTemplate com @LoadBalanced para usar nomes lógicos do Eureka.
     * Isso demonstra a Transparência de Localização: o serviço não precisa
     * saber o IP/porta do pagamento-service, apenas o nome registrado no Eureka.
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
