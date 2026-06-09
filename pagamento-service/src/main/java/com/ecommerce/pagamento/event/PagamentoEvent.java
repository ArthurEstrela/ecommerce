package com.ecommerce.pagamento.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoEvent {
    private Long pedidoId;
    private String status;
    private Double valor;
    private List<Map<String, Object>> itens;
}
