# E-commerce Distribuído - Arquitetura de Microsserviços

Este projeto implementa um sistema de E-commerce robusto baseado em microsserviços, demonstrando conceitos fundamentais de sistemas distribuídos como Service Discovery, RPC (gRPC), Mensageria Assíncrona (RabbitMQ) e Persistência Poliglota/Isolada.

## Como Rodar com Docker Compose

### Pré-requisitos

- Docker
- Docker Compose

### Subir o projeto

Na raiz do projeto, execute:

```bash
docker compose up -d --build
```

Esse comando sobe a infraestrutura e os microsserviços:

- PostgreSQL
- pgAdmin
- RabbitMQ
- Eureka Server
- Produto Service
- Carrinho Service
- Pedido Service
- Pagamento Service
- Estoque Service
- Notificação Service

### Verificar containers

```bash
docker compose ps
```

### Ver logs

```bash
docker compose logs -f
```

Para ver logs de um serviço específico:

```bash
docker compose logs -f produto-service
```

### Parar o projeto

```bash
docker compose down
```

### Acessos da infraestrutura

| Serviço | URL | Credenciais |
| --- | --- | --- |
| Eureka Dashboard | http://localhost:8761 | - |
| RabbitMQ Management | http://localhost:15672 | `guest` / `guest` |
| pgAdmin | http://localhost:5050 | `admin@admin.com` / `admin` |
| PostgreSQL | `localhost:5432` | `root` / `rootpassword` |

## Endpoints Atuais

### Produto Service

Base URL:

```text
http://localhost:8081
```

| Metodo | Endpoint | Descricao |
| --- | --- | --- |
| GET | `/api/produtos` | Lista produtos |
| GET | `/api/produtos/{id}` | Busca produto por ID |
| POST | `/api/produtos` | Cria produto |
| DELETE | `/api/produtos/{id}` | Remove produto |

Exemplo de criacao de produto:

```http
POST http://localhost:8081/api/produtos
Content-Type: application/json
```

```json
{
  "nome": "Notebook",
  "descricao": "Notebook Dell",
  "preco": 3500.0,
  "estoque": 10
}
```

### Carrinho Service

Base URL:

```text
http://localhost:8083
```

| Metodo | Endpoint | Descricao |
| --- | --- | --- |
| GET | `/api/carrinho/{usuarioId}` | Busca ou cria carrinho do usuario |
| POST | `/api/carrinho/{usuarioId}/adicionar` | Adiciona item ao carrinho |
| POST | `/api/carrinho/{usuarioId}/checkout` | Finaliza carrinho e cria pedido via gRPC |

Exemplo de item no carrinho:

```http
POST http://localhost:8083/api/carrinho/1/adicionar
Content-Type: application/json
```

```json
{
  "produtoId": 1,
  "quantidade": 1,
  "precoUnitario": 3500.0
}
```

### Pagamento Service

Base URL:

```text
http://localhost:8084
```

| Metodo | Endpoint | Descricao |
| --- | --- | --- |
| POST | `/api/pagamento/processar?pedidoId={pedidoId}&valor={valor}` | Processa pagamento manualmente e publica evento no RabbitMQ |

Exemplo:

```http
POST http://localhost:8084/api/pagamento/processar?pedidoId=1&valor=3500.0
```

### Serviços Sem Endpoint REST Publico

| Serviço | Porta | Como funciona |
| --- | --- | --- |
| Pedido Service | `8082` / `9090` | Recebe chamadas gRPC do Carrinho Service na porta `9090` |
| Estoque Service | `8085` | Consome eventos RabbitMQ de pagamento aprovado |
| Notificação Service | `8086` | Consome eventos RabbitMQ e simula envio de notificacao |

### Fluxo de Teste Sugerido

1. Criar produto em `POST http://localhost:8081/api/produtos`.
2. Listar produtos em `GET http://localhost:8081/api/produtos`.
3. Adicionar produto ao carrinho em `POST http://localhost:8083/api/carrinho/1/adicionar`.
4. Consultar carrinho em `GET http://localhost:8083/api/carrinho/1`.
5. Finalizar carrinho em `POST http://localhost:8083/api/carrinho/1/checkout`.
6. Processar pagamento em `POST http://localhost:8084/api/pagamento/processar?pedidoId=1&valor=3500.0`.
7. Verificar logs do Estoque e Notificação:

```bash
docker compose logs -f estoque-service notificacao-service
```

> Observacao: atualmente o pagamento ainda e disparado manualmente pelo endpoint REST do Pagamento Service. O fluxo automatico `Pedido -> fila RabbitMQ -> Pagamento` ainda nao esta implementado.

---

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

## 5. Mapeamento Teórico

- **Middleware:** O Spring Cloud e o gRPC atuam como a camada de middleware que gerencia a heterogeneidade e facilita a comunicação.
- **Isolamento de Falhas:** Se o serviço de Notificação cair, o fluxo de compra e estoque continua funcionando perfeitamente, evidenciando a resiliência da arquitetura assíncrona.
- **Consistência Eventual:** O sistema utiliza eventos para sincronizar o estado entre serviços (Pagamento -> Estoque), seguindo o modelo de consistência eventual comum em larga escala.
