package com.ecommerce.carrinho.controller;

import com.ecommerce.carrinho.model.Carrinho;
import com.ecommerce.carrinho.model.ItemCarrinho;
import com.ecommerce.carrinho.service.CarrinhoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrinho")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CarrinhoController {
    private final CarrinhoService carrinhoService;

    @GetMapping("/{usuarioId}")
    public Carrinho buscar(@PathVariable Long usuarioId) {
        return carrinhoService.buscarOuCriar(usuarioId);
    }

    @PostMapping("/{usuarioId}/adicionar")
    public Carrinho adicionar(@PathVariable Long usuarioId, @RequestBody ItemCarrinho item) {
        return carrinhoService.adicionarItem(usuarioId, item);
    }

    @PutMapping("/{usuarioId}/itens/{itemId}")
    public Carrinho alterarQuantidade(
            @PathVariable Long usuarioId,
            @PathVariable Long itemId,
            @RequestParam Integer quantidade
    ) {
        return carrinhoService.alterarQuantidade(usuarioId, itemId, quantidade);
    }

    @DeleteMapping("/{usuarioId}/itens/{itemId}")
    public Carrinho removerItem(@PathVariable Long usuarioId, @PathVariable Long itemId) {
        return carrinhoService.removerItem(usuarioId, itemId);
    }

    @PostMapping("/{usuarioId}/checkout")
    public String checkout(@PathVariable Long usuarioId) {
        return carrinhoService.checkout(usuarioId);
    }
}
