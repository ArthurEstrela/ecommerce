# 🛒 E-commerce Distribuído — Arquitetura de Microsserviços

> Sistema de compras online com fluxo completo de pedido, desenvolvido como projeto prático da disciplina de **Sistemas Distribuídos**, aplicando conceitos de RPC, Mensageria, Pub/Sub e Service Discovery.

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

Implementamos uma **fila dedicada** com `DirectExchange` para processamento ponto-a-ponto:

- **Produtor:** [`PedidoGrpcService.java`](pedido-service/src/main/java/com/ecommerce/pedido/service/PedidoGrpcService.java) — Publica na exchange `pedido.exchange` com routing key `pedido.criado`.
- **Consumidor:** [`NotificacaoConsumer.java`](notificacao-service/src/main/java/com/ecommerce/notificacao/consumer/NotificacaoConsumer.java) — Método `consumirPedidoCriado()` processa a fila `pedido.criado.queue`.
- **Configuração:** [`RabbitConfig.java`](pedido-service/src/main/java/com/ecommerce/pedido/config/RabbitConfig.java) (pedido) e [`RabbitConfig.java`](notificacao-service/src/main/java/com/ecommerce/notificacao/config/RabbitConfig.java) (notificação)

**Diferença fundamental da Fila vs Pub/Sub:**
- **Fila (Direct):** 1 mensagem → 1 consumidor (ponto-a-ponto)
- **Pub/Sub (Fanout):** 1 mensagem → N consumidores (broadcast)

#### Publish/Subscribe (Eventos) — Fanout Exchange

Implementamos o padrão **Pub/Sub** com `FanoutExchange` para eventos de pagamento:

- **Publicador:** [`PagamentoService.java`](pagamento-service/src/main/java/com/ecommerce/pagamento/service/PagamentoService.java) — Publica na `pagamento.exchange`.
- **Assinantes (3 consumidores independentes):**
  - [`PagamentoConsumer.java`](estoque-service/src/main/java/com/ecommerce/estoque/consumer/PagamentoConsumer.java) (Estoque) — Reserva produtos.
  - [`NotificacaoConsumer.java`](notificacao-service/src/main/java/com/ecommerce/notificacao/consumer/NotificacaoConsumer.java) (Notificação) — Envia alerta ao usuário.
  - [`PagamentoConsumer.java`](pedido-service/src/main/java/com/ecommerce/pedido/consumer/PagamentoConsumer.java) (Pedido) — Atualiza status para "PAGO".

Cada serviço possui sua própria fila (`estoque.queue`, `notificacao.queue`, `pedido.pagamento.queue`) vinculada à mesma Fanout Exchange, garantindo que **todos recebam o evento de forma independente**.

### 1.3 Serviço de Nomes — Service Discovery (Eureka)

- **Servidor Eureka:** [`EurekaServerApplication.java`](eureka-server/src/main/java/com/ecommerce/eureka/EurekaServerApplication.java) — Porta 8761, com `@EnableEurekaServer`.
- **Clientes:** Todos os 6 microsserviços usam `spring-cloud-starter-netflix-eureka-client` e registram-se automaticamente via `application.yml`.
- **Uso prático:** O `CarrinhoService` usa o nome lógico `pagamento-service` (via `@LoadBalanced RestTemplate`) para chamar o pagamento, sem saber IP/porta.
- **gRPC Discovery:** O cliente gRPC usa `discovery:///pedido-service` no `application.yml` para resolver o endereço via Eureka.

---

## 2. Arquitetura do Sistema

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          EUREKA SERVER (8761)                          │
│                      Service Discovery / Registry                      │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │ Registro/Descoberta
        ┌──────────────────────┼──────────────────────────────┐
        │                      │                              │
   ┌────▼────┐          ┌──────▼──────┐              ┌───────▼───────┐
   │ PRODUTO │          │  CARRINHO   │──── gRPC ───▶│    PEDIDO     │
   │ (8087)  │          │   (8083)    │              │  (8082/9090)  │
   │ REST    │          │ REST+gRPC   │──── REST ───▶│  gRPC Server  │
   └─────────┘          │  Client     │              │  + RabbitMQ   │
                        └─────────────┘              └───────┬───────┘
                              │                              │
                              │ REST                   Direct Exchange
                              ▼                        (Fila Dedicada)
                     ┌────────────────┐                      │
                     │   PAGAMENTO    │                      ▼
                     │    (8084)      │          ┌───────────────────┐
                     │    RabbitMQ    │          │   NOTIFICAÇÃO     │
                     │   Publisher    │          │     (8086)        │
                     └───────┬───────┘          │  Consome FILA +   │
                             │                  │  PUB/SUB          │
                    Fanout Exchange              └───────────────────┘
                    (Pub/Sub Eventos)                     ▲
                             │                           │
               ┌─────────────┼─────────────┐            │
               │             │             │   Fanout    │
               ▼             ▼             ▼             │
        ┌──────────┐  ┌──────────┐  ┌──────────┐        │
        │ ESTOQUE  │  │NOTIFICAÇÃO│  │  PEDIDO  │        │
        │ (8085)   │  │  (8086)  │  │ Consumer │        │
        │ Consumer │  │ Consumer │  │          │        │
        └──────────┘  └──────────┘  └──────────┘        │
```

### Stack Tecnológico

| Tecnologia | Uso |
|---|---|
| Java 21 + Spring Boot 3.2 | Backend de todos os microsserviços |
| Spring Cloud Netflix Eureka | Service Discovery |
| gRPC + Protocol Buffers | Comunicação síncrona (RPC) |
| RabbitMQ | Mensageria assíncrona (Filas + Pub/Sub) |
| PostgreSQL | Persistência (schema separado por serviço) |
| React + TypeScript | Frontend |
| Docker Compose | Infraestrutura (Postgres + RabbitMQ + PgAdmin) |

---

## 3. Descrição dos Serviços

### 3.1 Eureka Server (Service Discovery)
- **Porta:** 8761
- **Função:** Centraliza o registro de todos os microsserviços, permitindo comunicação por nomes lógicos.
- **Tecnologia:** Spring Cloud Netflix Eureka Server.

### 3.2 Produto Service
- **Porta:** 8087
- **Função:** CRUD completo do catálogo de produtos.
- **Banco:** PostgreSQL (`produto_db`)
- **API REST:** `GET/POST/DELETE /api/produtos`

### 3.3 Carrinho Service
- **Porta:** 8083
- **Função:** Gerencia itens do carrinho do usuário e orquestra o checkout.
- **Banco:** PostgreSQL (`carrinho_db`)
- **Integrações:**
  - **Cliente gRPC** → Pedido Service (criar pedido)
  - **REST + Eureka** → Pagamento Service (processar pagamento)

### 3.4 Pedido Service
- **Porta REST:** 8082 | **Porta gRPC:** 9090
- **Função:** Gerencia o ciclo de vida do pedido.
- **Banco:** PostgreSQL (`pedido_db`)
- **Integrações:**
  - **Servidor gRPC** (recebe chamadas do Carrinho)
  - **Produtor RabbitMQ** (Direct Exchange → fila de notificação)
  - **Consumidor RabbitMQ** (Fanout Exchange → atualiza status com pagamento)

### 3.5 Pagamento Service
- **Porta:** 8084
- **Função:** Simula o processamento financeiro.
- **Integrações:**
  - **Publicador RabbitMQ** (Fanout Exchange → evento para todos os assinantes)

### 3.6 Estoque Service (Proposto)
- **Porta:** 8085
- **Função:** Consome eventos de pagamento para reservar produtos.
- **Banco:** PostgreSQL (`estoque_db`)
- **Integrações:**
  - **Consumidor RabbitMQ** (Fanout Exchange)

### 3.7 Notificação Service (Proposto)
- **Porta:** 8086
- **Função:** Envia alertas simulados (email/SMS) ao usuário.
- **Integrações:**
  - **Consumidor RabbitMQ Pub/Sub** (Fanout Exchange — pagamento aprovado)
  - **Consumidor RabbitMQ Fila** (Direct Exchange — pedido criado)

---

## 4. Análise Conceitual — Mapeamento Código × Teoria

### 4.1 Invocação Remota (RPC)

| Conceito Teórico | Implementação no Código |
|---|---|
| Stub do cliente | `PedidoServiceGrpc.PedidoServiceBlockingStub` em `CarrinhoService.java` |
| Skeleton do servidor | `PedidoGrpcService extends PedidoServiceImplBase` em `PedidoGrpcService.java` |
| IDL (Interface Definition Language) | Arquivo `pedido.proto` com Protocol Buffers |
| Serialização/Marshalamento | Protocol Buffers (binário, mais eficiente que JSON) |
| Transparência de Acesso | A chamada `pedidoStub.criarPedido(request)` parece uma chamada local |

### 4.2 Comunicação Assíncrona (Filas)

| Conceito Teórico | Implementação no Código |
|---|---|
| Fila (Queue) | `pedido.criado.queue` — fila durável no RabbitMQ |
| Produtor | `PedidoGrpcService` publica via `RabbitTemplate.convertAndSend()` |
| Consumidor | `NotificacaoConsumer.consumirPedidoCriado()` com `@RabbitListener` |
| Direct Exchange | `pedido.exchange` com routing key `pedido.criado` |
| Desacoplamento temporal | Pedido e Notificação não precisam estar online ao mesmo tempo |

### 4.3 Publish/Subscribe (Eventos)

| Conceito Teórico | Implementação no Código |
|---|---|
| Tópico/Exchange | `pagamento.exchange` (FanoutExchange) |
| Publisher | `PagamentoService.processarPagamento()` |
| Subscribers | Estoque (`PagamentoConsumer`), Notificação (`NotificacaoConsumer`), Pedido (`PagamentoConsumer`) |
| Filas por assinante | `estoque.queue`, `notificacao.queue`, `pedido.pagamento.queue` |
| Desacoplamento | Publisher não sabe quantos nem quais subscribers existem |

### 4.4 Serviço de Nomes (Service Discovery)

| Conceito Teórico | Implementação no Código |
|---|---|
| Name Server | Eureka Server (`@EnableEurekaServer`) |
| Registro | `@EnableDiscoveryClient` + `eureka.client.serviceUrl` em cada serviço |
| Resolução de nomes | `@LoadBalanced RestTemplate` usa `http://pagamento-service/...` |
| gRPC Discovery | `address: 'discovery:///pedido-service'` no `application.yml` do Carrinho |

### 4.5 Middleware

O **Spring Cloud** e o **gRPC** atuam como a camada de middleware que:
- Gerencia a **heterogeneidade** entre serviços
- Fornece **abstrações** que simplificam a comunicação distribuída
- Oculta a **complexidade** de rede, serialização e discovery

---

## 5. Transparências Aplicadas

## 5. Mapeamento Teórico

1. **Cadastre produtos** via POST `/api/produtos`
2. **Adicione ao carrinho** via POST `/api/carrinho/1/adicionar`
3. **Faça checkout** via POST `/api/carrinho/1/checkout`
4. **Observe nos logs** de cada serviço as mensagens de comunicação
5. **Verifique no RabbitMQ** (http://localhost:15672) as filas e exchanges criadas
6. **Consulte pedidos** via GET `/api/pedidos` para ver o status "PAGO"
