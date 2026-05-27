package com.ecommerce.pagamento.controller;

import com.ecommerce.pagamento.service.PagamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagamento")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PagamentoController {
    private final PagamentoService pagamentoService;

    @PostMapping("/processar")
    public String pagar(@RequestParam Long pedidoId, @RequestParam Double valor) {
        pagamentoService.processarPagamento(pedidoId, valor);
        return "Pagamento processado com sucesso!";
    }
}
