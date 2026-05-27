# 🛒 E-commerce Distribuído — Arquitetura de Microsserviços

> Sistema de compras online com fluxo completo de pedido, desenvolvido como projeto prático da disciplina de **Sistemas Distribuídos**, aplicando conceitos de RPC, Mensageria, Pub/Sub e Service Discovery.

---

## 📑 Índice

1. [Descrição Técnica](#1-descrição-técnica)
2. [Arquitetura do Sistema](#2-arquitetura-do-sistema)
3. [Descrição dos Serviços](#3-descrição-dos-serviços)
4. [Análise Conceitual — Mapeamento Código × Teoria](#4-análise-conceitual--mapeamento-código--teoria)
5. [Transparências Aplicadas](#5-transparências-aplicadas)
6. [Reflexão do Grupo](#6-reflexão-do-grupo)
7. [Como Executar](#7-como-executar)
8. [Evidências](#8-evidências)

---

## 1. Descrição Técnica

### 1.1 Invocação Remota (RPC) — gRPC

Utilizamos **gRPC** para comunicação **síncrona** entre o serviço de **Carrinho** e o serviço de **Pedido**.

- **Arquivo Proto:** [`pedido.proto`](grpc-contracts/src/main/proto/pedido.proto) — Define o contrato de comunicação com Protocol Buffers.
- **Servidor gRPC:** [`PedidoGrpcService.java`](pedido-service/src/main/java/com/ecommerce/pedido/service/PedidoGrpcService.java) — Implementa o serviço `CriarPedido` (porta 9090).
- **Cliente gRPC:** [`CarrinhoService.java`](carrinho-service/src/main/java/com/ecommerce/carrinho/service/CarrinhoService.java) — Injeta o stub `PedidoServiceBlockingStub` e invoca remotamente.

**Como funciona:**
1. O usuário clica em "Finalizar Compra" no frontend.
2. O `CarrinhoService` serializa os itens em Protocol Buffers e faz uma chamada gRPC para o `PedidoGrpcService`.
3. O pedido é persistido no banco e a resposta é retornada com ID e status.

### 1.2 Mensageria e Eventos — RabbitMQ

#### Filas (Processamento Assíncrono) — Direct Exchange

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

### 5.1 Transparência de Localização
O Eureka Server permite que os serviços se comuniquem via **nomes lógicos** (ex: `pedido-service`, `pagamento-service`) sem conhecer a localização física (IP/Porta). Exemplo concreto: o `CarrinhoService` chama `http://pagamento-service/api/pagamento/processar` — o IP real é resolvido pelo Eureka em tempo de execução.

### 5.2 Transparência de Acesso
A comunicação via **gRPC** oculta a complexidade da serialização e do protocolo de rede. A chamada `pedidoStub.criarPedido(request)` no `CarrinhoService` é tratada como uma invocação de método local, mas na realidade envolve serialização Protobuf, transmissão HTTP/2, desserialização e processamento remoto.

### 5.3 Transparência de Concorrência
O RabbitMQ com múltiplos consumidores na **Fanout Exchange** permite que Estoque, Notificação e Pedido processem o mesmo evento de pagamento **em paralelo**, sem interferir uns nos outros. Cada um tem sua fila independente.

### 5.4 Transparência de Falha
Se o serviço de **Notificação** cair, o fluxo principal (Carrinho → Pedido → Pagamento → Estoque) continua funcionando perfeitamente. As mensagens ficam na fila do RabbitMQ e são consumidas quando o serviço volta a funcionar. Isso evidencia a **resiliência** da arquitetura assíncrona.

### 5.5 Transparência de Replicação
O sistema está preparado para escalamento horizontal: é possível rodar múltiplas instâncias de um mesmo serviço, e o Eureka fará o balanceamento de carga via `@LoadBalanced`.

---

## 6. Reflexão do Grupo

### 6.1 Dificuldades Encontradas

1. **Configuração do gRPC com Spring Boot 3:** A integração do `grpc-spring-boot-starter` com a versão 3 do Spring Boot exigiu atenção especial com a compatibilidade de versões e a geração de stubs via Maven.

2. **Service Discovery com gRPC:** Fazer o cliente gRPC resolver o endereço do servidor via Eureka (usando `discovery:///`) foi desafiador, pois exigiu configuração específica do `grpc-client-spring-boot-starter`.

3. **Diferenciação entre Fila e Pub/Sub:** Compreender na prática a diferença entre uma fila dedicada (Direct Exchange) e o padrão Pub/Sub (Fanout Exchange) exigiu estudo aprofundado da documentação do RabbitMQ.

4. **Orquestração de múltiplos serviços:** Subir 6 microsserviços + infraestrutura em ordem correta exigiu a criação de um script automatizado (`start-all.ps1`).

### 6.2 Decisões Arquiteturais

1. **gRPC entre Carrinho e Pedido:** Escolhemos gRPC para a operação de checkout porque é uma operação **síncrona e crítica** — o usuário precisa saber imediatamente se o pedido foi criado. O gRPC oferece alta performance e tipagem forte via Protobuf.

2. **Fanout Exchange para pagamentos:** O evento de pagamento precisa ser consumido por **múltiplos serviços** (Estoque, Notificação, Pedido), tornando o padrão Pub/Sub a escolha natural.

3. **Direct Exchange para pedido criado:** A notificação de "pedido criado" precisa ir para **apenas um consumidor** (Notificação), por isso usamos Direct Exchange com routing key.

4. **PostgreSQL com schemas separados:** Cada microsserviço usa um schema isolado no mesmo PostgreSQL, simulando bancos independentes sem a complexidade de múltiplas instâncias.

5. **RestTemplate com @LoadBalanced:** Para a chamada REST do Carrinho ao Pagamento, usamos RestTemplate com `@LoadBalanced` para demonstrar o Service Discovery na prática.

### 6.3 Possíveis Melhorias

1. **API Gateway:** Implementar um gateway (Spring Cloud Gateway) para centralizar roteamento, autenticação e rate limiting.

2. **Circuit Breaker:** Adicionar Resilience4j para tolerância a falhas nas chamadas entre serviços.

3. **Dockerização completa:** Containerizar todos os microsserviços Java (atualmente só PostgreSQL e RabbitMQ estão no Docker).

4. **Monitoramento:** Adicionar Spring Boot Actuator + Prometheus + Grafana para observabilidade.

5. **Autenticação:** Implementar JWT para autenticação distribuída entre os serviços.

6. **Saga Pattern:** Implementar o padrão Saga para garantir consistência transacional distribuída no fluxo completo de compra.

---

## 7. Como Executar

### Pré-requisitos
- Docker e Docker Compose
- Java 21+
- Maven
- Node.js 18+ e npm

### Passos

**Opção 1 — Script automatizado (recomendado):**
```powershell
.\start-all.ps1
```

**Opção 2 — Manual:**

1. **Infraestrutura:** Na raiz do projeto:
   ```bash
   docker-compose up -d
   ```
   Isso sobe PostgreSQL (5432), RabbitMQ (5672/15672) e PgAdmin (5050).

2. **Contratos gRPC:**
   ```bash
   cd grpc-contracts
   mvn install
   ```

3. **Eureka Server:**
   ```bash
   cd eureka-server
   mvn spring-boot:run
   ```
   Aguarde ficar disponível em http://localhost:8761.

4. **Serviços Backend (em terminais separados):**
   ```bash
   cd produto-service && mvn spring-boot:run    # porta 8087
   cd pedido-service && mvn spring-boot:run     # porta 8082 + gRPC 9090
   cd carrinho-service && mvn spring-boot:run   # porta 8083
   cd pagamento-service && mvn spring-boot:run  # porta 8084
   cd estoque-service && mvn spring-boot:run    # porta 8085
   cd notificacao-service && mvn spring-boot:run # porta 8086
   ```

5. **Frontend:**
   ```bash
   cd frontend
   npm install
   npm start
   ```
   Acesse http://localhost:3000.

### Portas dos Serviços

| Serviço | Porta | URL |
|---|---|---|
| Eureka Dashboard | 8761 | http://localhost:8761 |
| Produto Service | 8087 | http://localhost:8087/api/produtos |
| Pedido Service (REST) | 8082 | http://localhost:8082/api/pedidos |
| Pedido Service (gRPC) | 9090 | — |
| Carrinho Service | 8083 | http://localhost:8083/api/carrinho |
| Pagamento Service | 8084 | http://localhost:8084/api/pagamento |
| Estoque Service | 8085 | — (apenas consumer) |
| Notificação Service | 8086 | — (apenas consumer) |
| Frontend React | 3000 | http://localhost:3000 |
| RabbitMQ Management | 15672 | http://localhost:15672 (guest/guest) |
| PgAdmin | 5050 | http://localhost:5050 (admin@admin.com/admin) |

---

## 8. Evidências

### 8.1 Fluxo Completo de Compra

O fluxo demonstra **todos os conceitos** aplicados:

```
[Frontend]
    │
    ▼ (1) REST: POST /api/carrinho/1/checkout
[Carrinho Service]
    │
    ▼ (2) gRPC: CriarPedido() ──────────────────── INVOCAÇÃO REMOTA (RPC)
[Pedido Service]
    │
    ├─▶ (3) Persiste pedido no banco
    │
    ├─▶ (4) RabbitMQ Direct: pedido.exchange ──── FILA (processamento assíncrono)
    │       └─▶ [Notificação Service] "Pedido criado!"
    │
    ▼ (retorno gRPC)
[Carrinho Service]
    │
    ▼ (5) REST + Eureka: POST pagamento-service ─ SERVICE DISCOVERY + REST
[Pagamento Service]
    │
    ▼ (6) RabbitMQ Fanout: pagamento.exchange ─── PUB/SUB (eventos)
    ├─▶ [Estoque Service] "Reservando produtos"
    ├─▶ [Notificação Service] "Pagamento aprovado!"
    └─▶ [Pedido Service] "Status → PAGO"
```

### 8.2 Onde Cada Conceito Foi Aplicado

| Conceito | Arquivo(s) Principal(is) | Linha-chave |
|---|---|---|
| **gRPC Server** | `PedidoGrpcService.java` | `@GrpcService` + `extends PedidoServiceImplBase` |
| **gRPC Client** | `CarrinhoService.java` | `@GrpcClient("pedido-service")` + `pedidoStub.criarPedido()` |
| **Proto/IDL** | `pedido.proto` | `service PedidoService { rpc CriarPedido(...) }` |
| **Fila (Direct)** | `PedidoGrpcService.java` | `rabbitTemplate.convertAndSend(PEDIDO_EXCHANGE, ROUTING_KEY, evento)` |
| **Pub/Sub (Fanout)** | `PagamentoService.java` | `rabbitTemplate.convertAndSend(PAGAMENTO_EXCHANGE, "", event)` |
| **Consumer Fila** | `NotificacaoConsumer.java` | `@RabbitListener(queues = PEDIDO_CRIADO_QUEUE)` |
| **Consumer Pub/Sub** | `PagamentoConsumer.java` (Estoque) | `@RabbitListener(queues = ESTOQUE_QUEUE)` |
| **Eureka Server** | `EurekaServerApplication.java` | `@EnableEurekaServer` |
| **Eureka Client** | `CarrinhoServiceApplication.java` | `@EnableDiscoveryClient` |
| **Service Discovery** | `CarrinhoService.java` | `http://pagamento-service/api/pagamento/processar` |
| **gRPC via Eureka** | `application.yml` (carrinho) | `address: 'discovery:///pedido-service'` |

### 8.3 Demonstração das Comunicações

Para demonstrar o sistema funcionando, execute o fluxo completo:

1. **Cadastre produtos** via POST `/api/produtos`
2. **Adicione ao carrinho** via POST `/api/carrinho/1/adicionar`
3. **Faça checkout** via POST `/api/carrinho/1/checkout`
4. **Observe nos logs** de cada serviço as mensagens de comunicação
5. **Verifique no RabbitMQ** (http://localhost:15672) as filas e exchanges criadas
6. **Consulte pedidos** via GET `/api/pedidos` para ver o status "PAGO"
