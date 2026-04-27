# notification-service

Microserviço de envio de notificações por e-mail, construído com **Spring Boot**. Exposto como API REST: recebe dados da tarefa, monta o corpo com **Thymeleaf** e dispara o e-mail via **Java Mail**.

<div align="center">

[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-8.14.4-02303A?logo=gradle)](https://gradle.org/)
[![CI](https://github.com/Javanauta/notification-service/actions/workflows/gradle.yml/badge.svg)](https://github.com/Javanauta/notification-service/actions/workflows/gradle.yml)
[![PRs welcome](https://img.shields.io/badge/PRs-bem--vindos-brightgreen.svg)]()

</div>

> Se o repositório no GitHub tiver outro `owner` ou nome, atualize a URL do badge de **CI** (ou remova o badge até o remote estar definido).

## Papel no Ecossistema

O `notification-service` é responsável pelo **envio de notificações** a partir de eventos e decisões feitas em outros serviços: ele concentra a lógica de formatação (template) e o disparo do e-mail, sem orquestrar tarefas nem regras de negócio de agendamento.

**Integrações:**

- Consumido pelo `task-scheduler-service` para envio de notificações
- Pode ser acionado por BFF ou jobs internos
- Atua como etapa final do fluxo de notificação do sistema

## Arquitetura (Visão de Sistema)

1. O `task-scheduler-service` identifica tarefas pendentes (ou o equivalente no seu domínio)
2. Envia requisição HTTP para o `notification-service` (ex.: `POST /email` com o payload da tarefa)
3. Este serviço processa o template Thymeleaf e envia o e-mail via SMTP
4. O **status** da tarefa pode ser atualizado em outro serviço ou em um passo posterior (fora do escopo desta API, que hoje responde `200` ao concluir o envio)

```mermaid
flowchart LR
  subgraph orquestracao [Orquestração e domínio]
    TSS["task-scheduler-service"]
    BFF["BFF / jobs internos"]
  end
  NS["notification-service"]
  SMTP["Provedor SMTP"]
  TSS -->|"POST /email"| NS
  BFF -->|"POST /email"| NS
  NS -->|"e-mail transacional"| SMTP
```

Essa visão conecta o `notification-service` ao restante do ecossistema: ele é o **ponto de entrega** da notificação, não a fonte de verdade do estado da tarefa.

## Visão geral

- **Entrada:** `POST /email` com JSON (`TarefasDTO`).
- **Processo:** template HTML `notificacao` + envio SMTP (configurável por variáveis de ambiente).
- **Porta padrão:** `8082` (veja `application.yaml`).

## Tecnologias

| Categoria | Tecnologia |
|-----------|------------|
| Runtime | Java 17 |
| Framework | Spring Boot 3.5.x |
| Web | Spring Web |
| E-mail | Spring Mail (SMTP) |
| Templates | Thymeleaf |
| Build | Gradle 8.14.x |
| Containerização | Docker (multi-stage), Docker Compose |

## Pré-requisitos

**Desenvolvimento local (Gradle)**

- **JDK 17** — o projeto declara [Java toolchain](notification-service/build.gradle) no Gradle
- **Credenciais SMTP** (ex.: Gmail com senha de aplicativo)
- Acesso de rede ao host/porta do servidor de e-mail

**Execução em container**

- **Docker Engine** 20.10+ (ou equivalente compatível)
- **Docker Compose** v2 (`docker compose`), se for usar o arquivo `docker-compose.yml`
- Arquivo **`.env`** na raiz do repositório (o Compose referencia `env_file: .env`; mesmas variáveis descritas em **Configuração**)

## Configuração

As credenciais de envio são lidas de variáveis de ambiente definidas no `application.yaml` do módulo:

| Variável | Descrição |
|----------|-----------|
| `EMAIL_USERNAME` | Usuário/conta SMTP (também usada como remetente `From`) |
| `EMAIL_PASSWORD` | Senha ou token do provedor SMTP |

> Para **Gmail**, utilize **senha de aplicativo** (App Password), pois login direto com a senha da conta pode ser bloqueado pelo provedor.

O nome de exibição do remetente é configurado em `envio.email.nomeRemetente` (padrão no YAML: `Javanauta`).

Crie o arquivo local a partir do modelo:

```bash
cp .env.example .env
```

```powershell
Copy-Item .env.example .env
```

Edite `.env` com valores reais antes de subir a aplicação (local ou Docker). O repositório não versiona `.env`.

## Execução

A API expõe, por padrão, **`http://localhost:8082`** (porta definida em `application.yaml`).

### Desenvolvimento local (Gradle)

Na pasta do módulo **`notification-service/`**:

```bash
cd notification-service
./gradlew bootRun
```

```powershell
cd notification-service
.\gradlew.bat bootRun
```

### Docker e Docker Compose

O **`Dockerfile`** na raiz do repositório usa **build multi-stage**: na primeira etapa gera o JAR com Gradle dentro da imagem; na segunda, apenas o JRE 17 (Eclipse Temurin Alpine) executa o artefato. **Não é necessário** rodar `./gradlew build` na máquina host para construir a imagem.

O **contexto de build** é a **raiz do repositório**, porque o `Dockerfile` copia o subdiretório `notification-service/` (código-fonte do módulo).

#### Docker Compose (recomendado)

Na **raiz do repositório** (onde estão `docker-compose.yml` e `Dockerfile`):

```bash
docker compose up --build -d
```

```powershell
docker compose up --build -d
```

O serviço usa `env_file: .env`, mapeia **`8082:8082`** e define `restart: unless-stopped`. A imagem resultante é etiquetada como **`notification-service:local`**.

Operação do stack:

| Comando | Finalidade |
|---------|------------|
| `docker compose logs -f notification-service` | Acompanhar logs do serviço |
| `docker compose down` | Encerrar e remover containers da stack |
| `docker compose up --build -d` | Rebuild após alterações no código |

#### Apenas Docker (sem Compose)

Construa a imagem na raiz do repositório:

```bash
docker build -t notification-service:local -f Dockerfile .
```

Execute passando o `.env`:

```bash
docker run --rm --env-file .env -p 8082:8082 notification-service:local
```

```powershell
docker run --rm --env-file .env -p 8082:8082 notification-service:local
```

Parâmetros úteis: `--rm` remove o container ao sair; ajuste `-p` se precisar de outra porta no host.

## Comunicação entre serviços

O serviço é acionado via **HTTP** por outros microserviços (ex.: `task-scheduler-service`). Com **Docker Compose**, a resolução de nomes na rede interna do stack usa o nome do serviço definido no Compose (ex.: `notification-service`).

## API

### `POST /email`

Envia um e-mail de notificação com base no corpo recebido.

- **Resposta de sucesso:** `200 OK` (corpo vazio).
- **Content-Type sugerido:** `application/json`

**Exemplo de corpo (campos alinhados ao DTO; apenas parte deles é usada no template atual):**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "nomeTarefa": "Revisar documentação",
  "descricao": "Conferir links e formatação.",
  "dataCriacao": "2026-04-26T10:00:00Z",
  "dataEvento": "2026-04-28T15:00:00Z",
  "emailUsuario": "usuario@exemplo.com",
  "dataAlteracao": null,
  "status": "PENDENTE"
}
```

Valores de `status`: `PENDENTE`, `NOTIFICADO`, `CANCELADO`.

> O fluxo de envio utiliza hoje, no template, principalmente: `nomeTarefa`, `dataEvento`, `descricao` e o endereço em `emailUsuario`.

## Testes e build

```bash
cd notification-service
./gradlew test
./gradlew build
```

## CI

Pull requests para a branch `master` disparam o workflow [`.github/workflows/gradle.yml`](.github/workflows/gradle.yml) (build e testes com JDK 17 no Ubuntu).

## Estrutura (resumo)

```text
Dockerfile                     # build multi-stage; contexto na raiz
docker-compose.yml             # serviço notification-service + .env + porta 8082
.env.example                   # modelo de variáveis (copiar para .env)
notification-service/          ← módulo Gradle (código e recursos)
  src/main/java/.../controller/   # REST (`/email`)
  src/main/java/.../business/     # Serviço de e-mail e DTOs
  src/main/resources/
    application.yaml
    application.properties
    templates/notificacao.html
.github/workflows/             # na raiz do repositório
```

## Observações

- O serviço **não gerencia estado de tarefas**; persistência e transições de status ficam a cargo de outros componentes do ecossistema.
- Atua apenas como **camada de entrega** (*notification layer*): formata o conteúdo e envia o e-mail via SMTP.

## Licença

Definir no repositório (ex.: arquivo `LICENSE`) quando fizer sentido para o projeto.
