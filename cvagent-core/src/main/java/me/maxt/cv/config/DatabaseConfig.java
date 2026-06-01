package me.maxt.cv.config;

import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库配置管理，负责根据配置创建数据源并在启动时初始化表结构。
 *
 * <p>支持 H2（内存/文件模式）和 MySQL 两种数据库，根据 {@code database.mode}
 * 配置自动切换。启动时会自动执行 DDL 和预置数据脚本。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    private final AppConfig config;

    /**
     * 使用 AppConfig 构造数据库配置管理器。
     *
     * @param config 应用配置
     */
    public DatabaseConfig(AppConfig config) {
        this.config = config;
    }

    /**
     * 初始化数据库：加载驱动、执行 DDL、插入预置数据。
     *
     * <p>初始化会自动检测表是否已存在，避免重复创建。</p>
     */
    public void initialize() {
        String jdbcUrl = config.getEffectiveJdbcUrl();
        String username = config.getEffectiveUsername();
        String password = config.getEffectivePassword();

        log.info("初始化数据库: mode={}, url={}", config.getDbMode(), jdbcUrl);

        try {
            // 如果是 MySQL 模式，加载驱动
            if ("mysql".equalsIgnoreCase(config.getDbMode())) {
                Class.forName("com.mysql.cj.jdbc.Driver");
            }

            // 执行 DDL 脚本
            String ddlScript = config.getDbMode().equalsIgnoreCase("mysql")
                    ? "db/init-mysql.sql" : "db/init-h2.sql";
            executeSqlScript(jdbcUrl, username, password, ddlScript);
            log.info("数据库 DDL 执行完成");

            // H2 模式下执行预置数据
            if ("h2".equalsIgnoreCase(config.getDbMode())) {
                try {
                    executeSqlScript(jdbcUrl, username, password, "db/data-h2.sql");
                    log.info("预置数据插入完成");
                } catch (Exception e) {
                    // 预置数据可能已存在（唯一约束冲突），仅记录警告
                    log.warn("预置数据插入可能已存在，跳过: {}", e.getMessage());
                }
            }

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
            throw new AppException(ErrorCode.DATABASE_ERROR, e);
        }
    }

    /**
     * 执行类路径下的 SQL 脚本文件。
     *
     * <p>H2 模式使用内置 RunScript 正确解析 SQL（支持字符串内分号），
     * MySQL 模式使用状态机按分号分隔。</p>
     *
     * @param jdbcUrl      JDBC 连接 URL
     * @param username     用户名
     * @param password     密码
     * @param scriptPath   类路径下的脚本路径
     */
    private void executeSqlScript(String jdbcUrl, String username, String password, String scriptPath) {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            if ("h2".equalsIgnoreCase(config.getDbMode())) {
                java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(scriptPath);
                if (is == null) {
                    throw new AppException(ErrorCode.CONFIG_ERROR, "找不到 SQL 脚本: " + scriptPath);
                }
                org.h2.tools.RunScript.execute(conn,
                        new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
            } else {
                String sql = new String(
                        getClass().getClassLoader().getResourceAsStream(scriptPath).readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8
                );
                try (Statement stmt = conn.createStatement()) {
                    for (String statement : splitSqlStatements(sql)) {
                        String trimmed = statement.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                            continue;
                        }
                        try {
                            stmt.execute(trimmed);
                        } catch (Exception e) {
                            log.debug("SQL 执行跳过（可能已存在）: {}", e.getMessage());
                        }
                    }
                }
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("执行 SQL 脚本失败: {}", scriptPath, e);
            throw new AppException(ErrorCode.DATABASE_ERROR, e);
        }
    }

    /**
     * 按分号拆分 SQL 语句，正确处理单引号字符串字面量内的分号。
     *
     * <p>状态机在遇到未转义的单引号时切换字符串内/外状态，
     * 仅在字符串外部遇到分号时才拆分为独立语句。</p>
     *
     * @param sql 原始 SQL 脚本
     * @return 拆分后的 SQL 语句列表
     */
    static List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            if (c == '\'' && !inString) {
                inString = true;
            } else if (c == '\'' && inString) {
                // 检查是否为转义引号 ''
                if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    current.append(c);
                    current.append(sql.charAt(i + 1));
                    i++;
                    continue;
                }
                inString = false;
            }

            if (c == ';' && !inString) {
                statements.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        String remaining = current.toString().trim();
        if (!remaining.isEmpty()) {
            statements.add(remaining);
        }

        return statements;
    }

    /**
     * 获取 JDBC URL。
     *
     * @return JDBC URL
     */
    public String getJdbcUrl() {
        return config.getEffectiveJdbcUrl();
    }

    /**
     * 获取数据库用户名。
     *
     * @return 用户名
     */
    public String getUsername() {
        return config.getEffectiveUsername();
    }

    /**
     * 获取数据库密码。
     *
     * @return 密码
     */
    public String getPassword() {
        return config.getEffectivePassword();
    }

    /**
     * 获取数据库模式。
     *
     * @return h2 或 mysql
     */
    public String getDbMode() {
        return config.getDbMode();
    }
}
