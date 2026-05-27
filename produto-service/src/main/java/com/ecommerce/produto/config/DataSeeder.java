package com.ecommerce.produto.config;

import com.ecommerce.produto.model.Produto;
import com.ecommerce.produto.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ProdutoRepository produtoRepository;

    @Override
    public void run(String... args) throws Exception {
        if (produtoRepository.count() == 0) {
            System.out.println("Populating database with initial products...");
            produtoRepository.saveAll(Arrays.asList(
                    Produto.builder().nome("iPhone 15 Pro").descricao("Smartphone Apple 256GB Titânio").preco(7299.00).estoque(50).build(),
                    Produto.builder().nome("MacBook Air M2").descricao("Notebook Apple 13.6\" 8GB 256GB").preco(8499.00).estoque(30).build(),
                    Produto.builder().nome("Sony WH-1000XM5").descricao("Headphone Wireless Noise Cancelling").preco(2199.00).estoque(100).build(),
                    Produto.builder().nome("Apple Watch Series 9").descricao("Smartwatch 45mm GPS").preco(3499.00).estoque(45).build(),
                    Produto.builder().nome("PlayStation 5").descricao("Console Sony PS5 Edição Física").preco(4199.00).estoque(25).build(),
                    Produto.builder().nome("Câmera Canon EOS R50").descricao("Câmera Mirrorless 4K Lente 18-45mm").preco(4899.00).estoque(15).build()
            ));
            System.out.println("Database populated successfully!");
        }
    }
}
