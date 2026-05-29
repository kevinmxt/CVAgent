# CVAgent

基于 LangChain4j 的智能简历生成平台，使用 LLM 自动审阅简历并针对特定职位描述进行定制优化。

## 技术栈

- **语言**: Java 17
- **构建工具**: Maven（多模块项目）
- **AI 框架**: LangChain4j 1.14.0（langchain4j、langchain4j-open-ai）；1.14.0-beta24（langchain4j-agentic、langchain4j-embeddings-bge-small-en-v15-q）
- **LLM 后端**: DeepSeek API（兼容 OpenAI 接口）
- **Web 框架**: Javalin 6.4.0
- **数据库**: JOOQ 3.19.11 + HikariCP 6.2.1 + H2 2.3.232 / MySQL 9.1.0
- **文件解析**: Apache Tika 3.0.0
- **序列化**: Jackson 2.17.0（jackson-databind、jackson-datatype-jsr310）
- **日志**: Logback 1.5.12
- **测试**: JUnit 5.11.4 + Mockito 5.14.2（mockito-core、mockito-junit-jupiter）+ JaCoCo 0.8.12

## 项目结构

项目采用 Maven 多模块架构，父 POM 为 `pom.xml`，包含三个子模块：

```
CVAgent/
├── pom.xml                                         # 父 POM（依赖管理与模块聚合）
├── cvagent-core/                                   # 核心模块：公共设施、配置、数据访问、业务服务
│   ├── pom.xml
│   └── src/
│       ├── main/java/me/maxt/cv/
│       │   ├── agent/
│       │   │   └── ChatModelProvider.java          # ChatModel 工厂（支持 openai/ollama，扩展点预留）
│       │   ├── common/error/
│       │   │   ├── ErrorCode.java                  # 全局错误码枚举（系统/校验/业务/Agent）
│       │   │   ├── AppException.java               # 应用全局异常基类
│       │   │   └── ErrorResponse.java              # 统一 JSON 错误响应体
│       │   ├── common/util/
│       │   │   └── FileImportUtil.java             # 文件导入工具（基于 Apache Tika）
│       │   ├── config/
│       │   │   ├── AgentPromptConfig.java          # Agent 提示词配置加载器（含默认角色 Prompt）
│       │   │   ├── AppConfig.java                  # 配置管理（CV_* 环境变量 + config.json）
│       │   │   └── DatabaseConfig.java             # 数据库配置管理（H2 / MySQL 自动切换、DDL 初始化）
│       │   ├── service/
│       │   │   ├── CvGenerationService.java         # 简历生成服务（模板填充、上下文加载、生成结果管理）
│       │   │   ├── CvTemplateService.java           # 简历模板业务服务（CRUD）
│       │   │   ├── ExportService.java               # 简历导出服务（HTML 文件导出、状态管理）
│       │   │   ├── JobDescriptionService.java       # 岗位描述业务服务（文件导入、CRUD）
│       │   │   └── WorkExperienceService.java       # 工作经历业务服务（文件导入、CRUD）
│       │   └── store/
│       │       ├── datasource/
│       │       │   └── DataSourceConfig.java       # 数据源配置（HikariCP 连接池，支持 H2/MySQL）
│       │       ├── entity/
│       │       │   ├── CvTemplate.java             # 简历模板实体（HTML 格式，含占位符）
│       │       │   ├── CvGenerationRecord.java     # CV 生成迭代记录（每次迭代的快照、评分、反馈）
│       │       │   ├── GeneratedCv.java            # 生成的简历实体（关联模板、JD、工作经历，含最终评分与状态）
│       │       │   ├── JobDescription.java         # 岗位描述实体（职位、公司、JD 内容、原始文件信息）
│       │       │   └── WorkExperience.java         # 工作经历实体（人员信息、技能、履历、教育背景）
│       │       └── repository/
│       │           ├── CvTemplateRepository.java      # 简历模板数据访问层（基于 JOOQ）
│       │           ├── GeneratedCvRepository.java     # 生成简历及迭代记录数据访问层（基于 JOOQ）
│       │           ├── JobDescriptionRepository.java  # 岗位描述数据访问层（基于 JOOQ）
│       │           └── WorkExperienceRepository.java  # 工作经历数据访问层（基于 JOOQ）
│       ├── test/java/me/maxt/cv/
│       │   ├── agent/
│       │   │   └── ChatModelProviderTest.java      # ChatModelProvider 单元测试
│       │   ├── common/error/
│       │   │   ├── ErrorCodeTest.java              # ErrorCode 单元测试
│       │   │   └── AppExceptionTest.java           # AppException 单元测试
│       │   ├── common/util/
│       │   │   └── FileImportUtilTest.java         # FileImportUtil 单元测试
│       │   ├── config/
│       │   │   ├── AppConfigTest.java              # AppConfig 单元测试
│       │   │   └── AgentPromptConfigTest.java      # AgentPromptConfig 单元测试
│       │   ├── service/
│       │   │   ├── CvGenerationServiceTest.java    # CvGenerationService 单元测试
│       │   │   ├── CvTemplateServiceTest.java      # CvTemplateService 单元测试
│       │   │   ├── ExportServiceTest.java          # ExportService 单元测试
│       │   │   ├── JobDescriptionServiceTest.java  # JobDescriptionService 单元测试
│       │   │   └── WorkExperienceServiceTest.java  # WorkExperienceService 单元测试
│       │   └── store/repository/
│       │       ├── WorkExperienceRepositoryTest.java  # WorkExperienceRepository 单元测试
│       │       ├── CvTemplateRepositoryTest.java      # CvTemplateRepository 单元测试
│       │       ├── JobDescriptionRepositoryTest.java  # JobDescriptionRepository 单元测试
│       │       └── GeneratedCvRepositoryTest.java     # GeneratedCvRepository 单元测试
│       └── main/resources/
│           ├── config.json                         # 完整配置模板（LLM、数据库、Agent 角色等）
│           ├── logback.xml                         # 日志框架配置
│           └── db/
│               ├── init-h2.sql                     # H2 数据库初始化脚本
│               ├── data-h2.sql                     # H2 测试数据
│               └── init-mysql.sql                  # MySQL 数据库初始化脚本
├── cvagent-agent/                                  # Agent 模块（评审 Agent + 定制 Agent + 编排器 + DTO）
│   ├── pom.xml
│   └── src/main/java/me/maxt/cv/agent/
│       ├── dto/
│       │   ├── CvReviewResult.java                 # 单角色评审结果 DTO（含评分、反馈）
│       │   └── MultiRoleReviewResult.java          # 多角色综合评审结果 DTO（加权评分 + 合并反馈）
│       ├── orchestrator/
│       │   └── CvGenerationOrchestrator.java       # CV 生成编排器（多角色评审 + 迭代循环 + 结果聚合）
│       ├── reviewer/
│       │   ├── ReviewerRole.java                   # 评审角色抽象接口（HR/技术专家/团队领导）
│       │   └── CvReviewerAgent.java                # 通用 CV 评审 Agent 接口（LangChain4j AI Service）
│       └── tailor/
│           └── CvTailorAgent.java                  # CV 定制 Agent 接口（LangChain4j AI Service）
├── cvagent-web/                                    # Web 模块（基于 Javalin）
│   ├── pom.xml
│   └── src/
│       └── main/java/me/maxt/cv/web/
│           ├── App.java                            # Web 应用入口（Javalin 启动，路由注册）
│           ├── dto/
│           │   ├── request/
│           │   │   ├── WorkExperienceImportRequest.java  # 工作经历导入请求 DTO
│           │   │   ├── CvGenerateRequest.java            # CV 生成请求 DTO
│           │   │   └── CvContentUpdateRequest.java       # CV 内容更新请求 DTO
│           │   └── response/
│           │       └── PageResult.java                   # 分页响应 DTO
│           ├── auth/
│           │   ├── AuthProvider.java                     # 认证提供者接口
│           │   ├── PermissionCheck.java                  # 权限检查逻辑
│           │   └── UserIdentity.java                     # 用户身份模型
│           ├── interceptor/
│           │   ├── CorsHandler.java                      # CORS 跨域处理
│           │   └── ExceptionHandler.java                 # 全局异常处理
│           └── route/
│               ├── CvGenerationRoutes.java               # CV 生成 REST 路由（生成、查询、预览、导出、删除）
│               ├── CvTemplateRoutes.java                 # 简历模板 REST 路由
│               ├── JobDescriptionRoutes.java             # 岗位描述 REST 路由
│               └── WorkExperienceRoutes.java             # 工作经历 REST 路由
└── src/                                            # 旧版代码（待迁移至子模块）
    ├── main/java/me/maxt/
    │   ├── App.java                                # 应用入口（占位）
    │   └── cv/
    │       ├── agent/ChatModelProvider.java        # ChatModel 工厂（简化版，仅 OpenAI）
    │       └── config/AppConfig.java               # RAG 主题配置（与 cvagent-core 版本不同）
    └── test/
        ├── java/me/maxt/
        │   ├── AppTest.java                        # 基础测试类（JUnit 3 风格）
        │   └── demo/
        │       ├── CvReviewer.java                 # CV 审阅 Agent 接口（测试用）
        │       ├── ScoredCvTailor.java             # CV 定制 Agent 接口（测试用）
        │       ├── _3b_Loop_Agent_Example_States_And_Fail.java  # 循环 Agent 示例
        │       ├── domain/
        │       │   ├── Cv.java                     # 简历领域模型
        │       │   └── CvReview.java               # 简历审阅结果模型
        │       └── util/
        │           ├── StringLoader.java           # 资源文件加载工具
        │           ├── AgenticScopePrinter.java    # Agent 状态/对话可视化工具
        │           └── log/
        │               ├── BeautifulLogAppender.java  # 自定义 Logback Appender
        │               ├── CustomLogging.java         # 日志级别控制
        │               ├── LogLevels.java              # 日志级别枚举
        │               └── LogParser.java              # HTTP 日志解析与美化输出
        └── resources/
            └── documents/
                ├── master_cv.txt                   # 示例原始简历
                └── job_description_backend.txt     # 示例职位描述
```

## 模块说明

| 模块 | 描述 |
|------|------|
| `cvagent-core` | 核心模块，提供公共设施（ErrorCode 全局错误码、AppException 异常基类、ErrorResponse 错误响应体、FileImportUtil 文件导入）、配置管理（AgentPromptConfig 提示词加载、AppConfig 环境变量、DatabaseConfig 数据库、DataSourceConfig 数据源连接池）、数据实体（CvTemplate 简历模板、WorkExperience 工作经历、JobDescription 岗位描述、GeneratedCv 生成简历、CvGenerationRecord 迭代记录）、数据仓库（CvTemplateRepository、GeneratedCvRepository、JobDescriptionRepository、WorkExperienceRepository）、业务服务（CvGenerationService 简历生成编排、CvTemplateService 模板管理、ExportService 简历导出、JobDescriptionService JD 管理、WorkExperienceService 工作经历管理）、ChatModel 工厂等 |
| `cvagent-agent` | Agent 模块，承载多角色简历评审、定制与编排体系。包含 `ReviewerRole` 抽象接口（定义角色标识、名称、描述、系统提示词、用户提示词和评分权重）、`CvReviewerAgent` 通用评审 Agent 接口（基于 LangChain4j AI Service，支持动态注入角色 Prompt）、`CvTailorAgent` 简历定制 Agent 接口（基于 LangChain4j AI Service）、`CvGenerationOrchestrator` 编排器（协调多角色 Agent 循环评审与优化，聚合结果并记录迭代快照）、`CvReviewResult` 单角色评审 DTO 和 `MultiRoleReviewResult` 多角色综合评审 DTO（含加权评分和合并反馈）。依赖 `cvagent-core` |
| `cvagent-web` | Web 模块，基于 Javalin 提供 REST API 接口。已实现 `App` 应用启动入口（含路由注册与 Javalin 配置）、请求 DTO（`WorkExperienceImportRequest`、`CvGenerateRequest`、`CvContentUpdateRequest`）、响应 DTO（`PageResult` 分页结果）、认证模块（`AuthProvider` 接口、`UserIdentity` 用户身份、`PermissionCheck` 权限检查）、拦截器（`ExceptionHandler` 全局异常处理、`CorsHandler` 跨域处理）、REST 路由（`CvGenerationRoutes` CV 生成、`CvTemplateRoutes` 模板管理、`JobDescriptionRoutes` JD 管理、`WorkExperienceRoutes` 工作经历管理）。依赖 `cvagent-core` 和 `cvagent-agent` |

> **注意**：子模块目前处于开发初期。`cvagent-agent` 已包含 `ReviewerRole` 接口、`CvReviewerAgent` 评审 Agent、`CvTailorAgent` 定制 Agent、`CvGenerationOrchestrator` 编排器、`CvReviewResult` 和 `MultiRoleReviewResult` DTO。`cvagent-web` 已实现 `App` 启动入口、请求/响应 DTO、认证模块、拦截器和全部 REST 路由（`CvGenerationRoutes`、`CvTemplateRoutes`、`JobDescriptionRoutes`、`WorkExperienceRoutes`）。现有示例代码（`CvReviewer`、`ScoredCvTailor`、循环 Agent 示例等）仍位于根目录 `src/` 下，后续将迁移至对应子模块。根目录下的 `ChatModelProvider`（简化版，仅支持 OpenAI）和 `AppConfig`（RAG 主题，使用 `RAG_*` 环境变量前缀）与 `cvagent-core` 中的对应版本不同，属于旧版实现，待迁移完成后移除。

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+

### 安装与运行

1. **克隆项目**

```bash
git clone <repo-url> && cd CVAgent
```

2. **配置 API 密钥**

参考 `cvagent-core/src/main/resources/config.json` 作为完整配置模板。在项目工作目录创建 `config.json` 或通过环境变量配置：

```json
{
  "llm": {
    "apiKey": "your-deepseek-api-key",
    "baseUrl": "https://api.deepseek.com",
    "modelName": "deepseek-v4-flash",
    "temperature": 0.7,
    "maxTokens": 4096
  }
}
```

> 完整的配置模板（含数据库、Agent 多角色评审参数等）参见 `cvagent-core/src/main/resources/config.json`。

也可以通过环境变量配置（环境变量优先级更高）：

| 环境变量 | 说明 | 默认值 |
|---------|------|--------|
| `CV_LLM_API_KEY` | DeepSeek API Key | `demo` |
| `CV_LLM_BASE_URL` | API 基础地址 | `https://api.deepseek.com` |
| `CV_LLM_MODEL_NAME` | 模型名称 | `deepseek-v4-flash` |
| `CV_LLM_TEMPERATURE` | 模型温度 (0~1) | `0.7` |
| `CV_LLM_MAX_TOKENS` | 最大输出 Token 数 | `4096` |
| `CV_LLM_TIMEOUT` | API 超时秒数 | `120` |
| `CV_DB_MODE` | 数据库模式（h2 / mysql） | `h2` |
| `CV_AGENT_MAX_ITERATIONS` | Agent 最大迭代次数 | `3` |
| `CV_AGENT_PASS_SCORE` | Agent 通过评分阈值 (0~1) | `0.8` |
| `CV_SERVER_PORT` | HTTP 服务端口 | `8080` |

> 还支持 `CV_LLM_PROVIDER`、`CV_LLM_SYSTEM_PROMPT`、`CV_DB_H2_URL`、`CV_DB_MYSQL_URL`、`CV_DB_MYSQL_USERNAME`、`CV_DB_MYSQL_PASSWORD` 等环境变量，详见 `cvagent-core` 中的 `AppConfig.java`。

3. **编译项目**

```bash
mvn compile
```

4. **运行 Web 服务**

```bash
mvn exec:java -pl cvagent-web -Dexec.mainClass="me.maxt.cv.web.App"
```

5. **运行示例**

在 IDE 中直接运行 `_3b_Loop_Agent_Example_States_And_Fail` 的 `main` 方法，或通过 Maven exec 插件运行：

```bash
mvn exec:java -Dexec.mainClass="me.maxt.demo._3b_Loop_Agent_Example_States_And_Fail"
```

> **注意**：`exec-maven-plugin` 未在项目 POM 中预设，如需命令行运行请先在 `pom.xml` 中添加该插件配置。

## 核心功能

### AI Agent 简历审阅

`CvReviewer` Agent 以招聘经理的视角对简历进行评分和反馈，输出包含 0~1 分值和详细反馈意见的 `CvReview` 对象。

> 完整配置（`cvagent-core/src/main/resources/config.json`）中预定义了多角色评审体系：HR（权重 0.3）、技术专家（权重 0.4）、团队领导（权重 0.3），支持加权综合评分。`AgentPromptConfig` 提供了默认的中文 Prompt，可通过配置文件自定义各角色的提示词。`cvagent-agent` 模块中的 `ReviewerRole` 接口定义了评审角色的标准契约，`CvReviewerAgent` 基于 LangChain4j AI Service 提供通用的评审 Agent 实现，`CvReviewResult` 和 `MultiRoleReviewResult` DTO 分别承载单角色和多角色综合评审结果。

### AI Agent 简历定制

`CvTailorAgent`（位于 `cvagent-agent` 模块）和 `ScoredCvTailor`（位于 `src/test`）提供了简历定制能力——根据审阅反馈对简历进行针对性优化，使其更匹配目标职位，且不会编造不实信息。

### 循环优化流水线

`_3b_Loop_Agent_Example_States_And_Fail` 展示了完整的 Agent 流水线：

1. 审阅 Agent 对当前简历打分并给出反馈
2. 定制 Agent 根据反馈优化简历
3. 循环执行直到分数达标（≥ 0.8）或达到最大迭代次数（3 次）
4. 输出最终优化后的简历、最终评分及完整的审阅历史

示例程序从 `src/test/resources/documents/` 加载原始简历（`master_cv.txt`）和职位描述（`job_description_backend.txt`），也可替换为其他职位来观察不匹配场景下的 Agent 行为。

### Agent 编排器

`CvGenerationOrchestrator`（位于 `cvagent-agent` 模块）是循环优化流水线的正式实现，提供：

- 多角色评审协调：依次调用各角色 Agent 进行独立评审，计算加权综合评分
- 迭代循环控制：在评分达标或达到最大迭代次数时退出
- 结果聚合：合并各角色的反馈意见，记录完整的迭代历史快照
- 异常处理：单角色评审失败时不影响整体流程，自动降级处理

### REST API（cvagent-web）

Web 模块基于 Javalin 提供 RESTful 接口：

- **CV 生成路由**（`CvGenerationRoutes`）：`POST /api/v1/cv-generations/generate` 生成、`GET /{id}` 查询、`GET /{id}/history` 迭代历史、`GET /{id}/preview` HTML 预览、`PUT /{id}` 更新内容、`POST /{id}/export` 导出下载、`DELETE /{id}` 删除
- **基础 CRUD 路由**：`CvTemplateRoutes`、`JobDescriptionRoutes`、`WorkExperienceRoutes` 提供对应的 CRUD 接口
- **认证体系**：`AuthProvider` 接口 + `PermissionCheck` 权限校验 + `UserIdentity` 用户身份模型
- **全局处理**：`ExceptionHandler` 统一异常处理、`CorsHandler` 跨域支持
- **分页支持**：`PageResult` 泛型分页响应 DTO

### 灵活配置

`AppConfig`（cvagent-core 版）支持四级配置优先级：**代码默认值 → 类路径 config.json → 工作目录 config.json → 环境变量**（`CV_*` 前缀），覆盖 LLM 参数、数据库、Agent 角色权重、服务器端口等方面配置。

### 业务服务层

`cvagent-core` 提供完整的业务服务层：

- `CvGenerationService`：简历生成编排服务，负责模板填充（将工作经历数据替换 HTML 模板占位符）、加载生成上下文、保存生成结果与迭代记录、管理生成简历生命周期（查询、更新、导出、删除）
- `CvTemplateService`：简历模板管理服务，支持模板 CRUD，预置模板受保护不可删除
- `ExportService`：简历导出服务，将生成的 HTML 简历导出为可下载的文件流，支持生成文件名和导出状态标记
- `JobDescriptionService`：岗位描述管理服务，支持从文件导入（txt/docx/html/pdf）、手动创建、分页查询、编辑和删除
- `WorkExperienceService`：工作经历管理服务，支持从文件导入、手动编辑个人信息和履历字段

### 数据实体

`cvagent-core` 定义了完整的简历生成数据模型：

- `CvTemplate`：简历 HTML 模板，含占位符，对应数据库表 `cv_template`
- `WorkExperience`：工作经历实体，记录人员信息、技能、履历、教育背景
- `JobDescription`：岗位描述实体，存储职位标题、公司、JD 内容和原始文件信息
- `GeneratedCv`：生成的简历实体，关联模板、工作经历、JD，记录最终 HTML 内容、综合评分、各角色评分明细和状态（草稿/定稿/已导出）
- `CvGenerationRecord`：生成迭代记录，保存 Agent 每次迭代的完整快照（各角色评分、反馈、简历快照），便于回溯生成过程
- `CvTemplateRepository`：基于 JOOQ 的数据访问层，提供简历模板的 CRUD 操作
- `GeneratedCvRepository`：基于 JOOQ 的数据访问层，提供生成简历及其迭代记录的 CRUD 操作
- `JobDescriptionRepository`：基于 JOOQ 的数据访问层，提供岗位描述的 CRUD 操作
- `WorkExperienceRepository`：基于 JOOQ 的数据访问层，提供工作经历的 CRUD 操作

### 日志美化

`BeautifulLogAppender` + `LogParser` 提供自定义日志输出，可过滤框架噪音，美化 HTTP 请求/响应、Agent 对话和工具调用的展示。通过 `CustomLogging.setLevel()` 切换日志级别（`NONE` / `PRETTY` / `DEBUG` / `INFO`）。

