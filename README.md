# E-commerce Distribuído - Arquitetura de Microsserviços

Este projeto implementa um sistema de E-commerce robusto baseado em microsserviços, demonstrando conceitos fundamentais de sistemas distribuídos como Service Discovery, RPC (gRPC), Mensageria Assíncrona (RabbitMQ) e Persistência Poliglota/Isolada.

## 1. Arquitetura do Sistema

O sistema é composto por 7 componentes principais:

1.  **Eureka Server (Discovery):** Centraliza o registro de todos os serviços, permitindo que eles se encontrem dinamicamente sem endereços IP fixos.
2.  **Produto Service:** Gerencia o catálogo de produtos (Spring Boot + PostgreSQL).
3.  **Carrinho Service:** Gerencia itens temporários do usuário. Atua como **Cliente gRPC** para o serviço de Pedido.
4.  **Pedido Service:** Gerencia o ciclo de vida da compra. Atua como **Servidor gRPC**.
5.  **Pagamento Service:** Simula o processamento financeiro e publica eventos no **RabbitMQ** (Fanout Exchange).
6.  **Estoque Service:** Consome eventos de pagamento para realizar a reserva física de produtos.
7.  **Notificação Service:** Consome eventos de pagamento para enviar alertas (email/SMS simulado).
8.  **Frontend (React):** Interface visual em TypeScript que consome as APIs REST.

## 2. Transparências em Sistemas Distribuídos

O projeto aplica diversas transparências:

*   **Transparência de Localização:** O uso do **Eureka Server** permite que os serviços se comuniquem via nomes lógicos (ex: `pedido-service`), sem conhecer a localização física (IP/Porta) uns dos outros.
*   **Transparência de Acesso:** A comunicação entre Carrinho e Pedido via **gRPC** oculta a complexidade da serialização e do protocolo de rede, tratando a chamada remota quase como uma chamada de função local.
*   **Transparência de Replicação:** O sistema está preparado para ser escalado horizontalmente (via Docker), e o Eureka balanceará as requisições entre as instâncias disponíveis.

## 3. Comunicação e Integração

### Síncrona (gRPC)
Utilizamos gRPC entre o **Carrinho** e o **Pedido**. Esta escolha garante alta performance e tipagem forte através de Protocol Buffers (`.proto`), sendo ideal para operações críticas como o fechamento de uma compra onde a resposta imediata é necessária.

### Assíncrona (RabbitMQ)
Implementamos o padrão **Pub/Sub** para a confirmação de pagamentos.
- O **Pagamento Service** publica uma mensagem na `pagamento.exchange`.
- O **Estoque Service** e o **Notificação Service** possuem filas próprias vinculadas a esta exchange, reagindo de forma independente e paralela. Isso garante o **desacoplamento** e a **escalabilidade**.

## 4. Como Executar

### Pré-requisitos
- Docker e Docker Compose
- Java 21+
- Maven

### Passos
1.  **Infraestrutura:** Na raiz do projeto, execute `docker-compose up -d`. Isso subirá o PostgreSQL e o RabbitMQ.
2.  **Eureka Server:** Entre na pasta `eureka-server` e execute `mvn spring-boot:run`.
3.  **Contratos gRPC:** Na pasta `grpc-contracts`, execute `mvn install` para gerar as classes necessárias.
4.  **Serviços Backend:** Inicie cada microsserviço (Produto, Carrinho, Pedido, Pagamento, Estoque, Notificacao) usando `mvn spring-boot:run`.
5.  **Frontend:** Na pasta `frontend`, execute `npm install` e `npm start`.

---

## 5. Mapeamento Teórico

- **Middleware:** O Spring Cloud e o gRPC atuam como a camada de middleware que gerencia a heterogeneidade e facilita a comunicação.
- **Isolamento de Falhas:** Se o serviço de Notificação cair, o fluxo de compra e estoque continua funcionando perfeitamente, evidenciando a resiliência da arquitetura assíncrona.
- **Consistência Eventual:** O sistema utiliza eventos para sincronizar o estado entre serviços (Pagamento -> Estoque), seguindo o modelo de consistência eventual comum em larga escala.
