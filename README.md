# CVAgent

基于 LangChain4j 的智能简历生成平台，使用 LLM 自动审阅简历并针对特定职位描述进行定制优化。

## 技术栈

- **语言**: Java 17
- **构建工具**: Maven（多模块项目）
- **AI 框架**: LangChain4j 1.14.0（langchain4j、langchain4j-open-ai）；1.14.0-beta24（langchain4j-agentic、langchain4j-embeddings-bge-small-en-v15-q）
- **LLM 后端**: DeepSeek API（兼容 OpenAI 接口）
- **Web 框架**: Javalin 6.4.0
- **数据库**: JOOQ 3.19.11 + HikariCP 6.2.1 + H2 2.3.232 / MySQL 9.1.0
- **前端**: React 18 + Vite 5 + TypeScript 5.4
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
│       ├── main/java/me/maxt/cv/web/
│       │   ├── App.java                            # Web 应用入口（Javalin 启动，路由注册）
│       │   ├── dto/
│       │   │   ├── request/
│       │   │   │   ├── WorkExperienceImportRequest.java  # 工作经历导入请求 DTO
│       │   │   │   ├── CvGenerateRequest.java            # CV 生成请求 DTO
│       │   │   │   └── CvContentUpdateRequest.java       # CV 内容更新请求 DTO
│       │   │   └── response/
│       │   │       └── PageResult.java                   # 分页响应 DTO
│       │   ├── interceptor/
│       │   │   ├── CorsHandler.java                      # CORS 跨域处理
│       │   │   └── ExceptionHandler.java                 # 全局异常处理
│       │   └── route/
│       │       ├── CvGenerationRoutes.java               # CV 生成 REST 路由
│       │       ├── CvTemplateRoutes.java                 # 简历模板 REST 路由
│       │       ├── JobDescriptionRoutes.java             # 岗位描述 REST 路由
│       │       └── WorkExperienceRoutes.java             # 工作经历 REST 路由
│       ├── main/frontend/                          # React 前端 SPA
│       │   ├── package.json                            # React 18 + Vite 5 + TypeScript
│       │   ├── vite.config.ts                          # API 代理到 localhost:8080
│       │   └── src/
│       │       ├── api/                                # REST API 客户端（fetch 封装）
│       │       ├── components/                         # 通用组件（DataTable/Pagination/Modal/FileUpload/ScoreGauge）
│       │       ├── pages/                               # 页面（工作经历/模板/JD/CV生成/CV结果）
│       │       └── hooks/                              # 自定义 Hooks（分页/CRUD/CV生成状态机）
│       ├── main/scripts/                           # 启动脚本
│       │   ├── start.bat / start.sh                    # 含 JVM 参数（-Xmx512m）
│       └── main/resources/
│           └── public/                             # 前端构建产物（构建时自动生成）
```

## 模块说明

| 模块 | 描述 |
|------|------|
| `cvagent-core` | 核心模块，提供公共设施（ErrorCode 全局错误码、AppException 异常基类、ErrorResponse 错误响应体、FileImportUtil 文件导入）、配置管理（AgentPromptConfig 提示词加载、AppConfig 环境变量、DatabaseConfig 数据库、DataSourceConfig 数据源连接池）、数据实体（CvTemplate 简历模板、WorkExperience 工作经历、JobDescription 岗位描述、GeneratedCv 生成简历、CvGenerationRecord 迭代记录）、数据仓库（CvTemplateRepository、GeneratedCvRepository、JobDescriptionRepository、WorkExperienceRepository）、业务服务（CvGenerationService 简历生成编排、CvTemplateService 模板管理、ExportService 简历导出、JobDescriptionService JD 管理、WorkExperienceService 工作经历管理）、ChatModel 工厂等 |
| `cvagent-agent` | Agent 模块，承载多角色简历评审、定制与编排体系。包含 `ReviewerRole` 抽象接口（定义角色标识、名称、描述、系统提示词、用户提示词和评分权重）、`CvReviewerAgent` 通用评审 Agent 接口（基于 LangChain4j AI Service，支持动态注入角色 Prompt）、`CvTailorAgent` 简历定制 Agent 接口（基于 LangChain4j AI Service）、`CvGenerationOrchestrator` 编排器（协调多角色 Agent 循环评审与优化，聚合结果并记录迭代快照）、`CvReviewResult` 单角色评审 DTO 和 `MultiRoleReviewResult` 多角色综合评审 DTO（含加权评分和合并反馈）。依赖 `cvagent-core` |
| `cvagent-web` | Web 模块，基于 Javalin 提供 REST API + React 前端 SPA。包含 `App` 启动入口（含 Jackson JavaTimeModule、静态文件服务、SPA 回退）、请求/响应 DTO、拦截器（CORS、全局异常处理）、4 组 REST 路由。前端使用 React 18 + Vite 5 + TypeScript，`mvn package` 时自动构建并打包到 Fat JAR 中。依赖 `cvagent-core` 和 `cvagent-agent` |

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
| `CV_DATA_DIR` | H2 数据库目录（绝对路径） | JAR 同级 `data/` |
| `CV_SERVER_PORT` | HTTP 服务端口 | `8080` |

> 还支持 `CV_LLM_PROVIDER`、`CV_LLM_SYSTEM_PROMPT`、`CV_DB_H2_URL`、`CV_DB_MYSQL_URL`、`CV_DB_MYSQL_USERNAME`、`CV_DB_MYSQL_PASSWORD` 等环境变量，详见 `cvagent-core` 中的 `AppConfig.java`。

3. **编译并打包**

```bash
# 编译所有模块
mvn compile

# 一键打包（含前端构建、Fat JAR、启动脚本）
mvn package -pl cvagent-web -am -DskipTests
```

打包后在 `cvagent-web/target/` 生成：
- `cvagent-web-1.0.0-SNAPSHOT.jar` — 自包含 Fat JAR
- `start.bat` / `start.sh` — 启动脚本

4. **启动服务**

```bash
# 方式一：直接运行 JAR
java -jar cvagent-web/target/cvagent-web-1.0.0-SNAPSHOT.jar

# 方式二：使用启动脚本（Windows）
cvagent-web\target\start.bat

# 方式三：使用启动脚本（Linux/Mac）
./cvagent-web/target/start.sh
```

启动后访问 `http://localhost:8080` 即可使用前端界面。

5. **前端开发模式**（热更新，独立于后端）

```bash
cd cvagent-web/src/main/frontend
npm install      # 首次运行
npm run dev      # 启动 Vite 开发服务器，API 代理到 localhost:8080
```

6. **运行测试**

```bash
# 运行全部测试
mvn verify

# 运行指定模块测试
mvn test -pl cvagent-core
```

## 核心功能

### AI Agent 简历审阅

`CvReviewerAgent` 基于 LangChain4j AI Service，支持多角色评审体系：HR（权重 0.3）、技术专家（权重 0.4）、团队领导（权重 0.3）。每个角色以独立视角对简历评分和反馈，最终按权重计算综合评分。

> 各角色的系统提示词和用户提示词可通过 `config.json` 中的 `agent.reviewerRoles` 配置自定义，`AgentPromptConfig` 提供了默认的中文 Prompt。

### AI Agent 简历定制

`CvTailorAgent` 根据多角色评审反馈对简历进行针对性优化，使其更匹配目标职位。核心原则：不编造事实，只基于已有信息优化，保持 HTML 格式完整。

### Agent 编排器

`CvGenerationOrchestrator`（位于 `cvagent-agent` 模块）协调迭代优化流水线：

1. 调用 `CvGenerationService.fillTemplate()` 将工作经历数据填入 HTML 模板
2. `performMultiRoleReview()` 依次调用各角色 Agent 独立评审，计算加权综合评分
3. 达到通过阈值（默认 0.8）或最大迭代次数（默认 3）时退出
4. `performTailoring()` 根据合并反馈优化简历
5. 记录每轮迭代的完整快照（评分、反馈、简历内容）

### REST API（cvagent-web）

Web 模块基于 Javalin 提供 RESTful 接口：

- **CV 生成路由**（`CvGenerationRoutes`）：`POST /api/v1/cv-generations/generate` 生成、`GET /{id}` 查询、`GET /{id}/history` 迭代历史、`GET /{id}/preview` HTML 预览、`PUT /{id}` 更新内容、`POST /{id}/export` 导出下载、`DELETE /{id}` 删除
- **基础 CRUD 路由**：`CvTemplateRoutes`、`JobDescriptionRoutes`、`WorkExperienceRoutes` 提供对应的 CRUD 接口
- **认证体系**：`AuthProvider` 接口 + `PermissionCheck` 权限校验 + `UserIdentity` 用户身份模型
- **全局处理**：`ExceptionHandler` 统一异常处理、`CorsHandler` 跨域支持
- **分页支持**：`PageResult` 泛型分页响应 DTO

### 前端 SPA（React + Vite + TypeScript）

`cvagent-web/src/main/frontend/` 目录包含完整的单页应用：

- **工作经历维护**：支持从 txt/docx/html/pdf 文件导入，AI 自动解析姓名、邮箱、电话、技能、个人简介、工作经历、教育背景等字段，支持在线编辑和删除
- **简历模板维护**：预置 2 套模板（标准专业/简洁高效），支持自定义模板上传，预置模板受保护不可删除
- **JD 维护**：支持文件导入和手动创建，存储职位和公司信息
- **CV 生成**：选择工作经历 + 模板 + JD，由 AI Agent 多角色评审（HR/技术/领导），评分达标后生成 HTML 简历，支持预览、在线编辑和导出下载。低于阈值时展示评分详情和提升建议
- **构建自动化**：`mvn package` 时自动执行 `npm install && npm run build`，产物输出到 `resources/public/`，由 Javalin 作为静态文件提供服务

### 灵活配置

`AppConfig`（cvagent-core 版）支持四级配置优先级：**代码默认值 → 类路径 config.json → 工作目录 config.json → 环境变量**（`CV_*` 前缀），覆盖 LLM 参数、数据库、Agent 角色权重、服务器端口等方面配置。

### 业务服务层

`cvagent-core` 提供完整的业务服务层：

- `CvGenerationService`：简历生成编排服务，负责模板填充（将工作经历数据替换 HTML 模板占位符）、加载生成上下文、保存生成结果与迭代记录、管理生成简历生命周期（查询、更新、导出、删除）
- `CvTemplateService`：简历模板管理服务，支持模板 CRUD，预置模板受保护不可删除
- `ExportService`：简历导出服务，将生成的 HTML 简历导出为可下载的文件流，支持生成文件名和导出状态标记
- `JobDescriptionService`：岗位描述管理服务，支持从文件导入（txt/docx/html/pdf）、手动创建、分页查询、编辑和删除
- `WorkExperienceService`：工作经历管理服务，支持从文件导入（AI 自动解析姓名/邮箱/电话/技能/个人简介/工作经历/教育背景）、手动编辑个人信息和履历字段

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

### 日志

使用 Logback 统一日志输出，支持控制台和文件追加器，日志级别可通过 `logback.xml` 配置。JOOQ 的 SQL 执行日志和 LLM API 调用日志均被纳入统一体系。

