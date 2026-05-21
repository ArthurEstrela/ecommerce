package com.ecommerce.pagamento.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoEvent {
    private Long pedidoId;
    private String status;
    private Double valor;
}
