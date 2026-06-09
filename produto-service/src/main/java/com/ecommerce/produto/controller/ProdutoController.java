package com.ecommerce.produto.controller;

import com.ecommerce.produto.model.Produto;
import com.ecommerce.produto.service.ProdutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/produtos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProdutoController {
    private final ProdutoService produtoService;

    @GetMapping
    public List<Produto> listar() {
        return produtoService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Produto> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    @PostMapping
    public Produto criar(@RequestBody Produto produto) {
        return produtoService.salvar(produto);
    }

    @PostMapping("/{id}/baixar-estoque")
    public Produto baixarEstoque(@PathVariable Long id, @RequestParam Integer quantidade) {
        return produtoService.baixarEstoque(id, quantidade);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        produtoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
