package com.ecommerce.carrinho;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CarrinhoServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CarrinhoServiceApplication.class, args);
    }
}
