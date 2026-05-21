package com.ecommerce.pagamento.controller;

import com.ecommerce.pagamento.service.PagamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagamento")
@RequiredArgsConstructor
public class PagamentoController {
    private final PagamentoService pagamentoService;

    @PostMapping("/processar")
    public String pagar(@RequestParam Long pedidoId, @RequestParam Double valor) {
        pagamentoService.processarPagamento(pedidoId, valor);
        return "Pagamento processado com sucesso!";
    }
}
