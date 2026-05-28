package me.maxt.cv.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用配置管理类，负责从 config.json 和环境变量中加载全部配置。
 *
 * <p>配置加载优先级链（后者覆盖前者）：</p>
 * <ol>
 *   <li>代码中的默认值</li>
 *   <li>类路径下的 config.json 文件</li>
 *   <li>工作目录下的 config.json 文件（覆盖类路径配置）</li>
 *   <li>环境变量</li>
 * </ol>
 *
 * <p>配置分为以下几个部分：LLM、数据库、Agent、服务器。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class AppConfig {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    // ========== LLM 配置 ==========

    /** LLM 提供者标识（openai / ollama），可通过环境变量 {@code CV_LLM_PROVIDER} 覆盖 */
    private String llmProvider;
    /** API Key，可通过环境变量 {@code CV_LLM_API_KEY} 覆盖 */
    private String apiKey;
    /** API 基础地址，可通过环境变量 {@code CV_LLM_BASE_URL} 覆盖 */
    private String baseUrl;
    /** 模型名称，可通过环境变量 {@code CV_LLM_MODEL_NAME} 覆盖 */
    private String modelName;
    /** 系统提示词，可通过环境变量 {@code CV_LLM_SYSTEM_PROMPT} 覆盖 */
    private String systemPrompt;
    /** 模型温度参数（0~1），可通过环境变量 {@code CV_LLM_TEMPERATURE} 覆盖 */
    private double temperature;
    /** 最大输出 Token 数，可通过环境变量 {@code CV_LLM_MAX_TOKENS} 覆盖 */
    private int maxTokens;
    /** API 超时秒数，可通过环境变量 {@code CV_LLM_TIMEOUT} 覆盖 */
    private int timeoutSeconds;

    // ========== 数据库配置 ==========

    /** 数据库模式（h2 / mysql），可通过环境变量 {@code CV_DB_MODE} 覆盖 */
    private String dbMode;
    /** H2 JDBC URL */
    private String h2Url;
    /** H2 用户名 */
    private String h2Username;
    /** H2 密码 */
    private String h2Password;
    /** MySQL JDBC URL */
    private String mysqlUrl;
    /** MySQL 用户名 */
    private String mysqlUsername;
    /** MySQL 密码 */
    private String mysqlPassword;

    // ========== Agent 配置 ==========

    /** Agent 最大迭代次数，可通过环境变量 {@code CV_AGENT_MAX_ITERATIONS} 覆盖 */
    private int agentMaxIterations;
    /** Agent 通过评分阈值（0~1），可通过环境变量 {@code CV_AGENT_PASS_SCORE} 覆盖 */
    private double agentPassScore;
    /** 多角色评审配置，key 为角色标识 */
    private Map<String, ReviewerRoleConfig> reviewerRoles;
    /** 优化 Agent 配置 */
    private TailorConfig tailorConfig;

    // ========== 服务器配置 ==========

    /** HTTP 服务器端口，可通过环境变量 {@code CV_SERVER_PORT} 覆盖 */
    private int serverPort;

    /**
     * 使用默认值构造配置实例。
     */
    public AppConfig() {
        // LLM 默认值
        this.llmProvider = "openai";
        this.apiKey = "demo";
        this.baseUrl = "https://api.deepseek.com";
        this.modelName = "deepseek-v4-flash";
        this.systemPrompt = "";
        this.temperature = 0.7;
        this.maxTokens = 4096;
        this.timeoutSeconds = 120;

        // 数据库默认值
        this.dbMode = "h2";
        this.h2Url = "jdbc:h2:file:./data/cvagent;AUTO_SERVER=TRUE";
        this.h2Username = "sa";
        this.h2Password = "";
        this.mysqlUrl = "jdbc:mysql://localhost:3306/cvagent?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf-8";
        this.mysqlUsername = "root";
        this.mysqlPassword = "root";

        // Agent 默认值
        this.agentMaxIterations = 3;
        this.agentPassScore = 0.8;
        this.reviewerRoles = new LinkedHashMap<>();
        this.tailorConfig = new TailorConfig();

        // 服务器默认值
        this.serverPort = 8080;
    }

    /**
     * 按优先级链加载配置：代码默认 → config.json（类路径 → 工作目录） → 环境变量。
     *
     * @return 加载完成的配置实例
     */
    public static AppConfig load() {
        AppConfig config = new AppConfig();

        // 1. 尝试从类路径加载 config.json
        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream("config.json")) {
            if (is != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> classpathConfig = MAPPER.readValue(is, Map.class);
                applyFileConfig(config, classpathConfig);
                log.info("已从类路径加载 config.json");
            }
        } catch (IOException e) {
            log.warn("解析类路径 config.json 失败，使用默认值", e);
        }

        // 2. 尝试从工作目录加载 config.json（覆盖类路径配置）
        File workDirConfig = new File(System.getProperty("user.dir"), "config.json");
        if (workDirConfig.exists()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> fileConfig = MAPPER.readValue(workDirConfig, Map.class);
                applyFileConfig(config, fileConfig);
                log.info("已从工作目录加载 config.json（覆盖类路径配置）");
            } catch (IOException e) {
                log.warn("解析工作目录 config.json 失败", e);
            }
        }

        // 3. 应用环境变量覆盖
        applyEnvOverrides(config);

        return config;
    }

    /**
     * 从 config.json 解析的 Map 中读取各组配置并应用到实例。
     */
    @SuppressWarnings("unchecked")
    private static void applyFileConfig(AppConfig config, Map<String, Object> fileConfig) {
        // LLM 配置
        Map<String, Object> llm = (Map<String, Object>) fileConfig.get("llm");
        if (llm != null) {
            config.llmProvider = getString(llm, "provider", config.llmProvider);
            config.apiKey = getString(llm, "apiKey", config.apiKey);
            config.baseUrl = getString(llm, "baseUrl", config.baseUrl);
            config.modelName = getString(llm, "modelName", config.modelName);
            config.systemPrompt = getString(llm, "systemPrompt", config.systemPrompt);
            config.temperature = getDouble(llm, "temperature", config.temperature);
            config.maxTokens = getInt(llm, "maxTokens", config.maxTokens);
            config.timeoutSeconds = getInt(llm, "timeoutSeconds", config.timeoutSeconds);
        }

        // 数据库配置
        Map<String, Object> database = (Map<String, Object>) fileConfig.get("database");
        if (database != null) {
            config.dbMode = getString(database, "mode", config.dbMode);
            Map<String, Object> h2 = (Map<String, Object>) database.get("h2");
            if (h2 != null) {
                config.h2Url = getString(h2, "url", config.h2Url);
                config.h2Username = getString(h2, "username", config.h2Username);
                config.h2Password = getString(h2, "password", config.h2Password);
            }
            Map<String, Object> mysql = (Map<String, Object>) database.get("mysql");
            if (mysql != null) {
                config.mysqlUrl = getString(mysql, "url", config.mysqlUrl);
                config.mysqlUsername = getString(mysql, "username", config.mysqlUsername);
                config.mysqlPassword = getString(mysql, "password", config.mysqlPassword);
            }
        }

        // Agent 配置
        Map<String, Object> agent = (Map<String, Object>) fileConfig.get("agent");
        if (agent != null) {
            config.agentMaxIterations = getInt(agent, "maxIterations", config.agentMaxIterations);
            config.agentPassScore = getDouble(agent, "passScore", config.agentPassScore);

            // 多角色评审
            Map<String, Object> roles = (Map<String, Object>) agent.get("reviewerRoles");
            if (roles != null) {
                config.reviewerRoles = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : roles.entrySet()) {
                    Map<String, Object> roleMap = (Map<String, Object>) entry.getValue();
                    ReviewerRoleConfig roleConfig = new ReviewerRoleConfig();
                    roleConfig.setRoleKey(entry.getKey());
                    roleConfig.setName(getString(roleMap, "name", entry.getKey()));
                    roleConfig.setDescription(getString(roleMap, "description", ""));
                    roleConfig.setWeight(getDouble(roleMap, "weight", 0.0));
                    roleConfig.setSystemPrompt(getString(roleMap, "systemPrompt", ""));
                    roleConfig.setUserPrompt(getString(roleMap, "userPrompt", ""));
                    config.reviewerRoles.put(entry.getKey(), roleConfig);
                }
            }

            // 优化 Agent
            Map<String, Object> tailor = (Map<String, Object>) agent.get("tailor");
            if (tailor != null) {
                config.tailorConfig = new TailorConfig();
                config.tailorConfig.setSystemPrompt(getString(tailor, "systemPrompt", ""));
                config.tailorConfig.setUserPrompt(getString(tailor, "userPrompt", ""));
            }
        }

        // 服务器配置
        Map<String, Object> server = (Map<String, Object>) fileConfig.get("server");
        if (server != null) {
            config.serverPort = getInt(server, "port", config.serverPort);
        }
    }

    /**
     * 应用环境变量覆盖配置值。
     */
    private static void applyEnvOverrides(AppConfig config) {
        config.llmProvider = env("CV_LLM_PROVIDER", config.llmProvider);
        config.apiKey = env("CV_LLM_API_KEY", config.apiKey);
        config.baseUrl = env("CV_LLM_BASE_URL", config.baseUrl);
        config.modelName = env("CV_LLM_MODEL_NAME", config.modelName);
        config.systemPrompt = env("CV_LLM_SYSTEM_PROMPT", config.systemPrompt);
        config.temperature = envDouble("CV_LLM_TEMPERATURE", config.temperature);
        config.maxTokens = envInt("CV_LLM_MAX_TOKENS", config.maxTokens);
        config.timeoutSeconds = envInt("CV_LLM_TIMEOUT", config.timeoutSeconds);

        config.dbMode = env("CV_DB_MODE", config.dbMode);
        config.h2Url = env("CV_DB_H2_URL", config.h2Url);
        config.mysqlUrl = env("CV_DB_MYSQL_URL", config.mysqlUrl);
        config.mysqlUsername = env("CV_DB_MYSQL_USERNAME", config.mysqlUsername);
        config.mysqlPassword = env("CV_DB_MYSQL_PASSWORD", config.mysqlPassword);

        config.agentMaxIterations = envInt("CV_AGENT_MAX_ITERATIONS", config.agentMaxIterations);
        config.agentPassScore = envDouble("CV_AGENT_PASS_SCORE", config.agentPassScore);

        config.serverPort = envInt("CV_SERVER_PORT", config.serverPort);
    }

    // ========== 辅助方法 ==========

    private static String getString(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return (val instanceof String) ? (String) val : defaultVal;
    }

    private static int getInt(Map<String, Object> map, String key, int defaultVal) {
        Object val = map.get(key);
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        return defaultVal;
    }

    private static double getDouble(Map<String, Object> map, String key, double defaultVal) {
        Object val = map.get(key);
        if (val instanceof Double) return (Double) val;
        if (val instanceof Number) return ((Number) val).doubleValue();
        return defaultVal;
    }

    private static String env(String name, String defaultVal) {
        String val = System.getenv(name);
        return (val != null && !val.isEmpty()) ? val : defaultVal;
    }

    private static int envInt(String name, int defaultVal) {
        String val = System.getenv(name);
        if (val != null && !val.isEmpty()) {
            try { return Integer.parseInt(val); } catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }

    private static double envDouble(String name, double defaultVal) {
        String val = System.getenv(name);
        if (val != null && !val.isEmpty()) {
            try { return Double.parseDouble(val); } catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }

    // ========== 内部配置类 ==========

    /**
     * 单个评审角色的配置。
     */
    public static class ReviewerRoleConfig {
        private String roleKey;
        private String name;
        private String description;
        private double weight;
        private String systemPrompt;
        private String userPrompt;

        public String getRoleKey() { return roleKey; }
        public void setRoleKey(String roleKey) { this.roleKey = roleKey; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
        public String getUserPrompt() { return userPrompt; }
        public void setUserPrompt(String userPrompt) { this.userPrompt = userPrompt; }
    }

    /**
     * 优化 Agent 的配置。
     */
    public static class TailorConfig {
        private String systemPrompt;
        private String userPrompt;

        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
        public String getUserPrompt() { return userPrompt; }
        public void setUserPrompt(String userPrompt) { this.userPrompt = userPrompt; }
    }

    // ========== Getters ==========

    public String getLlmProvider() { return llmProvider; }
    public String getApiKey() { return apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public String getModelName() { return modelName; }
    public String getSystemPrompt() { return systemPrompt; }
    public double getTemperature() { return temperature; }
    public int getMaxTokens() { return maxTokens; }
    public int getTimeoutSeconds() { return timeoutSeconds; }

    public String getDbMode() { return dbMode; }
    public String getH2Url() { return h2Url; }
    public String getH2Username() { return h2Username; }
    public String getH2Password() { return h2Password; }
    public String getMysqlUrl() { return mysqlUrl; }
    public String getMysqlUsername() { return mysqlUsername; }
    public String getMysqlPassword() { return mysqlPassword; }

    public int getAgentMaxIterations() { return agentMaxIterations; }
    public double getAgentPassScore() { return agentPassScore; }
    public Map<String, ReviewerRoleConfig> getReviewerRoles() {
        return Collections.unmodifiableMap(reviewerRoles);
    }
    public TailorConfig getTailorConfig() { return tailorConfig; }

    public int getServerPort() { return serverPort; }

    /**
     * 根据当前 dbMode 获取对应的 JDBC URL。
     *
     * @return JDBC URL
     */
    public String getEffectiveJdbcUrl() {
        return "mysql".equalsIgnoreCase(dbMode) ? mysqlUrl : h2Url;
    }

    /**
     * 根据当前 dbMode 获取对应的数据库用户名。
     *
     * @return 数据库用户名
     */
    public String getEffectiveUsername() {
        return "mysql".equalsIgnoreCase(dbMode) ? mysqlUsername : h2Username;
    }

    /**
     * 根据当前 dbMode 获取对应的数据库密码。
     *
     * @return 数据库密码
     */
    public String getEffectivePassword() {
        return "mysql".equalsIgnoreCase(dbMode) ? mysqlPassword : h2Password;
    }
}
