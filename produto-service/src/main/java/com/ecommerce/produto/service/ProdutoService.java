package com.ecommerce.produto.service;

import com.ecommerce.produto.model.Produto;
import com.ecommerce.produto.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProdutoService {
    private final ProdutoRepository produtoRepository;

    public List<Produto> listarTodos() {
        return produtoRepository.findAll();
    }

    public Produto buscarPorId(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
    }

    public Produto salvar(Produto produto) {
        return produtoRepository.save(produto);
    }

    public Produto baixarEstoque(Long id, Integer quantidade) {
        Produto produto = buscarPorId(id);

        if (quantidade == null || quantidade <= 0) {
            throw new RuntimeException("Quantidade deve ser maior que zero");
        }

        if (produto.getEstoque() < quantidade) {
            throw new RuntimeException("Estoque insuficiente para o produto " + id);
        }

        produto.setEstoque(produto.getEstoque() - quantidade);
        return produtoRepository.save(produto);
    }

    public void deletar(Long id) {
        produtoRepository.deleteById(id);
    }
}
