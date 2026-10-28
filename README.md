# 🤖 Chatbot OpenAI — Java + Spring Boot
![CI](https://github.com/Tampieri-IA/chatbot-openai/actions/workflows/ci.yml/badge.svg)

API REST em **Java 25** e **Spring Boot 4** que conversa com a **API da OpenAI** usando o `RestClient` nativo do Spring, sem biblioteca HTTP externa. Mantém **histórico de conversa por sessão** e tem um **modo mock** para rodar sem chave de API.

Projeto do portfólio **[Tampieri IA](https://www.youtube.com/@Tampieriia)**, canal focado em implementação prática de IA para programação.

---

## ✨ Destaques

- Integração real com a OpenAI (`/chat/completions`) via `RestClient`
- Memória de contexto por sessão: cada `sessaoId` tem seu próprio histórico
- Perfil `mock` para desenvolvimento e testes, sem custo e sem chave
- Validação de entrada com Bean Validation
- Tratamento global de erros (`GlobalExceptionHandler`)
- Documentação interativa com Swagger UI
- Testes unitários, de controller e de contexto

---

## 🚀 Tecnologias

- Java 25
- Spring Boot 4 (Spring Web, Bean Validation)
- RestClient
- Springdoc OpenAPI (Swagger UI)
- JUnit 5 e Mockito
- Maven (Maven Wrapper incluso)
- OpenAI API (modelo `gpt-4o-mini`)

---

## 📁 Estrutura do projeto

```
src/main/java/com/tampieria/chatbot
├── ChatbotOpenaiApplication.java
├── config
│   ├── RestClientConfig.java        # RestClient: base URL e autorização
│   └── OpenApiConfig.java           # Configuração do Swagger/OpenAPI
├── controller
│   └── ChatController.java          # Endpoint REST do chatbot
├── dto
│   ├── ChatRequestDTO.java          # Entrada (sessaoId + mensagemUsuario)
│   ├── ChatResponseDTO.java         # Saída (resposta)
│   └── openai
│       ├── OpenAiRequest.java       # Payload enviado à OpenAI
│       ├── OpenAiResponse.java      # Payload recebido da OpenAI
│       ├── OpenAiMessage.java       # Mensagem (role/content)
│       └── OpenAiChoice.java        # Escolha de resposta
├── service
│   ├── ChatService.java             # Contrato do serviço
│   └── impl
│       ├── ChatServiceImpl.java     # Implementação real (@Profile("!mock"))
│       └── ChatServiceMock.java     # Implementação mock (@Profile("mock"))
└── exception
    ├── OpenAiException.java         # Erros da integração com a OpenAI
    └── GlobalExceptionHandler.java  # Respostas de erro padronizadas
```

---

## 🏗️ Arquitetura

```
Cliente → ChatController → ChatService ─┬─ ChatServiceImpl → RestClient → OpenAI API
                                        └─ ChatServiceMock (perfil mock)
```

- O `ChatController` valida a entrada e delega ao `ChatService`.
- O `ChatServiceImpl` guarda o histórico por `sessaoId` em memória, monta o payload e chama a OpenAI.
- O `ChatServiceMock` devolve uma resposta simulada, sem chamada externa.
- O Spring escolhe a implementação pelo perfil ativo.

---

## ⚙️ Pré-requisitos

- JDK 25
- Maven 3.9+ (ou o Maven Wrapper do projeto)
- Conta na [OpenAI](https://platform.openai.com/) com uma API Key (apenas para o modo real)

---

## ▶️ Como executar

```powershell
git clone https://github.com/Tampieri-IA/chatbot-openai.git
cd chatbot-openai
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

> 💡 **Por padrão, o projeto roda no perfil `mock`** (`spring.profiles.active: mock`). As respostas são simuladas. Para usar a OpenAI de verdade, veja a seção abaixo.

---

## 🔌 Perfis: mock x OpenAI real

| Perfil | Comportamento | Precisa de API Key? |
|---|---|---|
| `mock` (padrão) | Resposta simulada, sem chamada externa | Não |
| qualquer outro (ex.: `openai`) | Chama a OpenAI de verdade | Sim |

A implementação real usa `@Profile("!mock")`, então qualquer perfil diferente de `mock` a ativa.

### Rodando com a OpenAI real

1. Configure a variável de ambiente (veja a próxima seção).
2. Execute com outro perfil:

```powershell
./mvnw spring-boot:run "-Dspring-boot.run.profiles=openai"
```

No IntelliJ, você também pode definir `SPRING_PROFILES_ACTIVE=openai` em **Environment variables**.

---

## 🔑 Configuração da API Key

A chave **nunca é versionada**. Ela é lida da variável de ambiente `OPENAI_API_KEY`.

**Windows (PowerShell):**

```powershell
[System.Environment]::SetEnvironmentVariable("OPENAI_API_KEY", "sua-chave-aqui", "User")
```

Reabra o terminal depois de definir.

**IntelliJ:** em **Run > Edit Configurations > Environment variables**, adicione `OPENAI_API_KEY=sua-chave-aqui`.

**Linux/macOS:**

```bash
export OPENAI_API_KEY="sua-chave-aqui"
```

O `application.yaml` referencia a variável assim:

```yaml
openai:
  api:
    key: ${OPENAI_API_KEY}
    model: gpt-4o-mini
```

> ⚠️ Nunca coloque uma chave real no arquivo. Sem a variável definida, a aplicação não sobe com o perfil real.
> 💰 Dica: defina um limite mensal de gasto no painel da OpenAI e desative a recarga automática.

---

## 📡 Endpoint principal

### `POST /api/chat`

Envia uma mensagem e recebe a resposta da IA. Requisições com o mesmo `sessaoId` compartilham histórico; sessões diferentes são isoladas.

**Campos da requisição**

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `sessaoId` | string | Sim | Identifica a conversa. O mesmo valor compartilha histórico. |
| `mensagemUsuario` | string | Sim | Texto enviado ao chatbot. |

Os dois campos têm `@NotBlank`. Se algum faltar ou estiver vazio, a API retorna erro de validação (400).

**Request**

```json
{
  "sessaoId": "usuario-123",
  "mensagemUsuario": "Meu nome é Tampieri. Qual a capital da França?"
}
```

**Response (OpenAI real)**

```json
{
  "resposta": "Olá, Tampieri! A capital da França é Paris."
}
```

**Response (modo mock)**

```json
{
  "resposta": "Resposta simulada (mock): ..."
}
```

**Exemplo com curl (Linux/macOS/Git Bash)**

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"sessaoId":"usuario-123","mensagemUsuario":"Olá!"}'
```

> 💡 No PowerShell, `curl` é um alias de `Invoke-WebRequest`. Use `curl.exe` ou teste pelo Swagger.

Em uma segunda chamada com o mesmo `sessaoId`, a IA lembra do contexto (por exemplo, o seu nome).

---

## 📑 Documentação (Swagger)

Com a aplicação rodando:

```
http://localhost:8080/swagger-ui.html
```

---

## 🧪 Testes

```powershell
./mvnw test
```

- **Service:** testes unitários do `ChatServiceImpl`
- **Controller:** testes da camada web (`ChatControllerTest`)
- **Contexto:** teste de inicialização com `@ActiveProfiles("mock")`, sem depender de chave

Os testes não chamam a OpenAI e não geram custo.

---

## ⚠️ Limitações conhecidas

- O histórico fica **em memória** e é perdido ao reiniciar a aplicação.
- O histórico cresce sem limite dentro de uma sessão, o que aumenta o custo por requisição.
- Não há autenticação nem limite de requisições (rate limiting).

---

## 🛣️ Roadmap

- [x] Integração com a OpenAI via `RestClient`
- [x] Memória de contexto por sessão
- [x] Perfil mock
- [x] Testes automatizados
- [ ] Persistir o histórico em banco de dados (JPA + PostgreSQL)
- [ ] Limitar o histórico às últimas N mensagens
- [ ] Respostas em streaming
- [ ] RAG: respostas a partir de documentos próprios
- [ ] Dockerizar a aplicação
- [ ] GitHub Actions rodando `mvn test`

---

## 📺 Canal Tampieri IA

Os projetos deste portfólio viram conteúdo no canal, com código aberto.

🔗 [youtube.com/@Tampieriia](https://www.youtube.com/@Tampieriia)
📧 tampieria.canal@gmail.com

---

## 📄 Licença

Distribuído sob a licença MIT. Veja o arquivo `LICENSE`.
