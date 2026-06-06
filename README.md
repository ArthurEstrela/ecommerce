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

## 1. Descrição Técnica — Comunicação entre Serviços

### 1.1 Comunicação Assíncrona — Fila Dedicada (Direct Exchange)

Implementamos uma **fila dedicada** com `DirectExchange` para processamento ponto-a-ponto:

- **Produtor:** [`PedidoGrpcService.java`](pedido-service/src/main/java/com/ecommerce/pedido/service/PedidoGrpcService.java) — Publica na exchange `pedido.exchange` com routing key `pedido.criado`.
- **Consumidor:** [`NotificacaoConsumer.java`](notificacao-service/src/main/java/com/ecommerce/notificacao/consumer/NotificacaoConsumer.java) — Método `consumirPedidoCriado()` processa a fila `pedido.criado.queue`.
- **Configuração:** [`RabbitConfig.java`](pedido-service/src/main/java/com/ecommerce/pedido/config/RabbitConfig.java) (pedido) e [`RabbitConfig.java`](notificacao-service/src/main/java/com/ecommerce/notificacao/config/RabbitConfig.java) (notificação)

**Diferença fundamental da Fila vs Pub/Sub:**
- **Fila (Direct):** 1 mensagem → 1 consumidor (ponto-a-ponto)
- **Pub/Sub (Fanout):** 1 mensagem → N consumidores (broadcast)

### 1.2 Publish/Subscribe (Eventos) — Fanout Exchange

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

## 2. Diagrama de Arquitetura

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

A seguir identificamos e discutimos as transparências de sistemas distribuídos (conforme Coulouris et al.) que foram aplicadas no projeto:

### 5.1 Transparência de Acesso

A chamada gRPC no `CarrinhoService.java` utiliza o stub `pedidoStub.criarPedido(request)`, que **parece uma chamada de método local**, embora invoque um serviço remoto rodando em outro container (Pedido Service, porta 9090). O desenvolvedor não precisa lidar diretamente com sockets, serialização ou protocolos de rede — o gRPC abstrai tudo isso.

**Onde no código:** `CarrinhoService.java` — linha `pedidoStub.criarPedido(request)`.

### 5.2 Transparência de Localização

Os serviços **não conhecem o endereço IP/porta** uns dos outros. Toda a resolução é feita por nomes lógicos registrados no Eureka:

- **REST:** O `CarrinhoService` chama `http://pagamento-service/api/pagamento/processar` — o `@LoadBalanced RestTemplate` resolve o nome `pagamento-service` via Eureka automaticamente.
- **gRPC:** O canal gRPC usa `discovery:///pedido-service` no `application.yml`, resolvendo o endereço do Pedido Service pelo Eureka sem hardcoding de IP.

Se um serviço mudar de IP ou porta, nenhum código precisa ser alterado — basta que ele se registre novamente no Eureka.

**Onde no código:** `RestConfig.java` (`@LoadBalanced`) e `docker-compose.yml` (`GRPC_CLIENT_PEDIDO_SERVICE_ADDRESS`).

### 5.3 Transparência de Falha

O `CarrinhoService.java` implementa um bloco `try-catch` ao chamar o serviço de pagamento via REST. Se o Pagamento Service estiver indisponível, o sistema **não quebra** — registra um aviso no log e o pedido permanece criado (pode ser processado posteriormente).

Além disso, as filas do RabbitMQ são configuradas como **duráveis** (`new Queue("pedido.criado.queue", true)`), garantindo que mensagens não sejam perdidas caso um consumidor esteja temporariamente offline.

**Onde no código:** `CarrinhoService.java` — bloco `try-catch` na chamada REST ao pagamento; `RabbitConfig.java` — filas duráveis.

### 5.4 Transparência de Concorrência

Os 3 consumidores da Fanout Exchange (`pagamento.exchange`) — Estoque, Notificação e Pedido — processam o **mesmo evento de pagamento de forma independente e concorrente**, sem interferência entre si. Cada um possui sua própria fila (`estoque.queue`, `notificacao.queue`, `pedido.pagamento.queue`), eliminando condições de corrida.

O RabbitMQ também garante que, dentro de uma mesma fila (Direct Exchange), mensagens sejam entregues a **um único consumidor por vez**, evitando processamento duplicado.

**Onde no código:** `RabbitConfig.java` de cada serviço — filas separadas vinculadas à mesma FanoutExchange.

### 5.5 Transparência de Migração

O uso de **Docker Compose** permite que qualquer microsserviço seja migrado para outro host sem alteração de código. Como a comunicação é feita por nomes lógicos (via Eureka e DNS interno do Docker), basta reconfigurar o `docker-compose.yml` para mover um container — os demais serviços continuam funcionando normalmente.

**Onde no código:** `docker-compose.yml` — todos os serviços referenciados por nome de container na rede `ecommerce-network`.

### 5.6 Transparência de Replicação

Esta transparência **não foi implementada de forma explícita** neste projeto. No entanto, a arquitetura está preparada para isso: o Eureka suporta múltiplas instâncias do mesmo serviço, e o `@LoadBalanced RestTemplate` faria balanceamento de carga automaticamente entre réplicas. Da mesma forma, o RabbitMQ distribui mensagens entre consumidores concorrentes na mesma fila (competing consumers pattern).

---

## 6. Reflexão do Grupo

### 6.1 Dificuldades Encontradas

- **Integração gRPC com Eureka:** A configuração do cliente gRPC para resolver endereços via Service Discovery (`discovery:///`) foi um dos maiores desafios. A documentação do `grpc-spring-boot-starter` para uso com Eureka é limitada, e foi necessário ajustar dependências e configurações do `application.yml` até funcionar corretamente.
- **Ordem de inicialização dos containers:** Como os microsserviços dependem do PostgreSQL, RabbitMQ e Eureka, foi necessário configurar `healthchecks` e `depends_on` com `condition: service_healthy` para evitar falhas de conexão durante a inicialização.
- **Serialização de mensagens no RabbitMQ:** Inicialmente, as mensagens eram serializadas como objetos Java, causando erros de deserialização entre serviços diferentes. A solução foi usar `Jackson2JsonMessageConverter` para todas as mensagens.
- **Separação entre Direct Exchange e Fanout Exchange:** Entender conceitualmente e implementar corretamente a diferença entre fila ponto-a-ponto (Direct) e broadcast (Fanout) exigiu estudo aprofundado da documentação do RabbitMQ.

### 6.2 Decisões Arquiteturais

- **Direct Exchange para "pedido criado" vs Fanout Exchange para "pagamento aprovado":** A criação de pedido gera uma notificação ponto-a-ponto (apenas o serviço de Notificação precisa saber), enquanto o pagamento aprovado precisa ser propagado para múltiplos serviços (Estoque, Notificação, Pedido) — justificando o uso de Fanout.
- **gRPC para Carrinho → Pedido e REST para Carrinho → Pagamento:** Usamos gRPC na comunicação mais crítica (criação de pedido, que envolve dados complexos com itens) e REST na chamada ao pagamento (mais simples, apenas pedidoId e valor), demonstrando ambos os padrões de comunicação síncrona.
- **Banco de dados separado por serviço:** Cada microsserviço tem seu próprio database lógico no PostgreSQL (`produto_db`, `carrinho_db`, `pedido_db`, `estoque_db`), respeitando o princípio de isolamento de dados em microsserviços.
- **Estoque e Notificação como serviços propostos:** Escolhemos esses dois serviços adicionais porque demonstram claramente os padrões de Pub/Sub (Estoque consome eventos de pagamento) e Fila (Notificação consome eventos de pedido criado).

### 6.3 Possíveis Melhorias

- **API Gateway:** Implementar um gateway (Spring Cloud Gateway) para centralizar o roteamento, autenticação e rate limiting.
- **Circuit Breaker:** Adicionar Resilience4j para proteger chamadas entre serviços contra falhas em cascata.
- **Autenticação e Autorização:** Implementar JWT e Spring Security para proteger os endpoints.
- **Saga Pattern:** Implementar o padrão Saga para garantir consistência eventual no fluxo Pedido → Pagamento → Estoque, com compensação em caso de falha.
- **Monitoramento:** Adicionar Spring Actuator, Prometheus e Grafana para observabilidade.
- **Testes automatizados:** Adicionar testes de integração com Testcontainers para validar as comunicações entre serviços.
- **Pagamento real:** Integrar com um gateway de pagamento simulado mais robusto, com estados intermediários (pendente, processando, aprovado, recusado).

---

## 7. Evidências do Sistema Funcionando

### 7.1 Fluxo de Teste Completo

Para reproduzir o funcionamento do sistema:

1. **Cadastre produtos** via `POST http://localhost:8081/api/produtos`
2. **Adicione ao carrinho** via `POST http://localhost:8083/api/carrinho/1/adicionar`
3. **Faça checkout** via `POST http://localhost:8083/api/carrinho/1/checkout`
4. **Observe nos logs** de cada serviço as mensagens de comunicação
5. **Verifique no RabbitMQ** (http://localhost:15672) as filas e exchanges criadas
6. **Consulte pedidos** via `GET http://localhost:8082/api/pedidos` para ver o status "PAGO"

### 7.2 Prints do Sistema

> **Nota:** As capturas de tela abaixo demonstram o sistema funcionando com todos os conceitos distribuídos aplicados.

#### Eureka Dashboard — Serviços Registrados

<!-- Inserir print do Eureka Dashboard (http://localhost:8761) mostrando todos os 6 serviços registrados -->
*Captura: Eureka Dashboard exibindo os serviços produto-service, carrinho-service, pedido-service, pagamento-service, estoque-service e notificacao-service registrados.*

#### RabbitMQ — Exchanges e Filas

<!-- Inserir print do RabbitMQ Management (http://localhost:15672) mostrando as exchanges -->
*Captura: RabbitMQ Management mostrando a `pedido.exchange` (Direct) e `pagamento.exchange` (Fanout) com suas respectivas filas vinculadas.*

#### Logs — Comunicação gRPC (Carrinho → Pedido)

<!-- Inserir print dos logs do carrinho-service e pedido-service durante o checkout -->
*Captura: Logs mostrando o CarrinhoService invocando `criarPedido()` via gRPC e o PedidoGrpcService processando a requisição.*

#### Logs — Fila Dedicada (Direct Exchange)

<!-- Inserir print dos logs do notificacao-service recebendo evento de pedido criado -->
*Captura: Logs do Notificação Service mostrando o consumo da mensagem "pedido.criado" via fila dedicada (Direct Exchange).*

#### Logs — Pub/Sub (Fanout Exchange)

<!-- Inserir print dos logs dos 3 consumidores recebendo o evento de pagamento -->
*Captura: Logs simultâneos do Estoque Service, Notificação Service e Pedido Service recebendo o evento de pagamento aprovado via Fanout Exchange.*

#### Frontend — Interface do Usuário

<!-- Inserir print do frontend React mostrando a lista de produtos e o carrinho -->
*Captura: Interface do e-commerce mostrando catálogo de produtos e funcionalidade de carrinho.*

### 7.3 Onde Cada Conceito Foi Aplicado

| Conceito | Serviços Envolvidos | Arquivo Principal | Evidência |
|---|---|---|---|
| **gRPC (RPC)** | Carrinho → Pedido | `CarrinhoService.java` / `PedidoGrpcService.java` | Checkout cria pedido via chamada remota |
| **Fila (Direct Exchange)** | Pedido → Notificação | `PedidoGrpcService.java` / `NotificacaoConsumer.java` | Notificação de "pedido criado" ponto-a-ponto |
| **Pub/Sub (Fanout Exchange)** | Pagamento → Estoque, Notificação, Pedido | `PagamentoService.java` / 3 consumers | Evento "pagamento aprovado" para N subscribers |
| **Service Discovery** | Todos → Eureka | `EurekaServerApplication.java` + `application.yml` | Resolução por nome lógico (REST e gRPC) |
| **Comunicação entre Serviços** | Carrinho → Pagamento | `CarrinhoService.java` + `@LoadBalanced` | REST via nome lógico do Eureka |
