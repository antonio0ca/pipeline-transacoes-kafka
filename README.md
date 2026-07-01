# Pipeline de Transacoes em Tempo Real

![CI](https://github.com/antonio0ca/pipeline-transacoes-kafka/actions/workflows/ci.yml/badge.svg)

Pipeline de processamento de transacoes financeiras construido com Spring Boot, Apache Kafka e PostgreSQL. Um produtor simula um fluxo continuo de transacoes (Pix, cartao, TED, boleto), um consumidor aplica regras antifraude, grava o resultado no banco e transmite tudo para um dashboard ao vivo.

O projeto explora, de ponta a ponta, um caso de uso tipico de mensageria em fintech: ingestao de eventos, aplicacao de regras de negocio no consumidor e visualizacao em tempo real.

## Arquitetura

```
  Simulador          Apache Kafka          Consumidor            PostgreSQL
 (@Scheduled)  --->   topico:      --->    Antifraude    --->   (JPA / tabela
  produtor            transacoes           + persistencia        transacoes)
                                                |
                                                v
                                          SSE (stream)  --->  Dashboard (navegador)
```

Produtor e consumidor rodam no mesmo servico Spring Boot, deixando o fluxo completo do Kafka visivel em uma unica execucao.

## Stack

- Java 21 e Spring Boot 3.3
- Spring for Apache Kafka (produtor e consumidor)
- Spring Data JPA e PostgreSQL
- Server-Sent Events para o stream ao vivo
- Frontend em HTML, CSS e JavaScript, sem framework
- Docker e Docker Compose

## Como rodar

Pre-requisito: Docker com Docker Compose.

```bash
docker compose up --build
```

O comando sobe o Kafka (modo KRaft), o PostgreSQL e a aplicacao. Quando os servicos ficarem saudaveis, o simulador comeca a publicar transacoes automaticamente.

Abra o dashboard em:

```
http://localhost:8080
```

Para encerrar:

```bash
docker compose down
```

Para limpar tambem o volume do banco:

```bash
docker compose down -v
```

## Regras antifraude

O consumidor classifica cada transacao com base em regras deterministicas e explicaveis:

| Regra | Condicao | Resultado |
|---|---|---|
| Limite de negacao | valor maior ou igual a R$ 50.000 | NEGADA |
| Limite de suspeita | valor maior ou igual a R$ 10.000 | SUSPEITA |
| Velocidade | mais de 5 transacoes da mesma conta em 10 segundos | SUSPEITA |
| Padrao | nenhuma regra acionada | APROVADA |

Os limites e a janela de velocidade sao configuraveis em `application.yml`, na secao `app.antifraude`.

## Testes

As regras antifraude sao cobertas por testes unitarios puros (sem Kafka nem banco). Para rodar, com Docker desligado, basta o JDK 21:

```bash
./mvnw test
```

O projeto inclui o Maven Wrapper (`mvnw`), entao nao e necessario ter o Maven instalado.

## Endpoints

| Metodo | Rota | Descricao |
|---|---|---|
| GET | `/api/stream` | Stream SSE com cada transacao processada |
| GET | `/api/estatisticas` | Agregados atuais (volume, contagens, taxa de aprovacao) |
| GET | `/api/transacoes/recentes` | Ultimas transacoes gravadas |

## Estrutura

```
src/main/java/com/antonio0ca/transacoes
  producer/    Simulador e envio ao Kafka
  consumer/    Listener que orquestra o pipeline
  service/     Antifraude, estatisticas e stream SSE
  repository/  Acesso JPA
  model/       Entidade, evento e enums
  web/         Controller e DTOs
src/main/resources/static   Dashboard (HTML, CSS, JS)
```

## Observacao

Todos os dados sao ficticios e gerados em tempo de execucao apenas para demonstracao. Nao ha integracao com meios de pagamento reais.
