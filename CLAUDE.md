## Build
- `mvn compile` — compile all modules
- `mvn test -pl cvagent-core` — run tests for a specific module
- `mvn verify` — run full build with tests (allowed in settings)

## Stack
- Java 17, Maven multi-module (cvagent-core → cvagent-agent → cvagent-web)
- LangChain4j 1.14.0, Javalin 6.4.0, JOOQ 3.19.11, H2/MySQL
- DeepSeek API as LLM backend (OpenAI-compatible)
- Tests: JUnit 5 + Mockito + JaCoCo

## Config
- Environment variables prefixed `CV_*` (see README for full list)
- Config file: `config.json` in working directory or classpath
