package me.maxt.cv.store.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import me.maxt.cv.config.AppConfig;
import me.maxt.cv.config.DatabaseConfig;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 数据源配置管理器，负责创建 HikariCP 连接池和 JOOQ DSLContext。
 *
 * <p>根据 {@code database.mode} 配置自动选择 H2 或 MySQL 数据源。
 * 首次启动时自动执行 DDL 初始化脚本和预置数据。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    private static volatile DataSource dataSource;
    private static volatile DSLContext dslContext;
    private static volatile SQLDialect dialect;

    /**
     * 初始化数据源和数据库表结构。
     *
     * <p>线程安全：使用双重检查锁定确保只初始化一次。</p>
     *
     * @param config 应用配置
     */
    public static synchronized void initialize(AppConfig config) {
        if (dataSource != null) {
            log.debug("数据源已初始化，跳过");
            return;
        }

        log.info("开始初始化数据源...");

        // 1. 执行 DDL 脚本和预置数据
        DatabaseConfig dbConfig = new DatabaseConfig(config);
        dbConfig.initialize();

        // 2. 创建 HikariCP 数据源
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getEffectiveJdbcUrl());
        hikariConfig.setUsername(config.getEffectiveUsername());
        hikariConfig.setPassword(config.getEffectivePassword());
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);

        // H2 和 MySQL 的驱动类
        if ("mysql".equalsIgnoreCase(config.getDbMode())) {
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dialect = SQLDialect.MYSQL;
        } else {
            hikariConfig.setDriverClassName("org.h2.Driver");
            dialect = SQLDialect.H2;
        }

        dataSource = new HikariDataSource(hikariConfig);
        dslContext = DSL.using(dataSource, dialect);
        log.info("数据源初始化完成: mode={}, dialect={}", config.getDbMode(), dialect);
    }

    /**
     * 获取 JOOQ DSLContext，用于执行所有数据库操作。
     *
     * @return DSLContext 实例
     * @throws AppException 如果数据源未初始化
     */
    public static DSLContext getDSLContext() {
        if (dslContext == null) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "数据源尚未初始化，请先调用 initialize()");
        }
        return dslContext;
    }

    /**
     * 获取原始 DataSource（供 HikariCP 直接使用）。
     *
     * @return DataSource 实例
     */
    public static DataSource getDataSource() {
        if (dataSource == null) {
            throw new AppException(ErrorCode.DATABASE_ERROR, "数据源尚未初始化");
        }
        return dataSource;
    }

    /**
     * 使用 H2 内存数据库初始化数据源（用于测试）。
     *
     * <p>使用唯一的数据库名避免测试间冲突，自动执行 DDL 脚本。</p>
     *
     * @param jdbcUrl H2 内存数据库 JDBC URL
     */
    public static synchronized void initializeForTest(String jdbcUrl) {
        // 先关闭旧数据源
        if (dataSource != null) {
            shutdown();
        }
        log.info("使用测试数据源初始化: {}", jdbcUrl);

        // 执行 DDL 和预置数据
        try (Connection conn = java.sql.DriverManager.getConnection(jdbcUrl, "sa", "")) {
            java.io.InputStream ddlStream = DataSourceConfig.class.getClassLoader()
                    .getResourceAsStream("db/init-h2.sql");
            if (ddlStream == null) {
                throw new AppException(ErrorCode.CONFIG_ERROR, "找不到 db/init-h2.sql");
            }
            org.h2.tools.RunScript.execute(conn,
                    new java.io.InputStreamReader(ddlStream, java.nio.charset.StandardCharsets.UTF_8));
            log.info("测试 DDL 执行完成");

            // 预置数据
            java.io.InputStream dataStream = DataSourceConfig.class.getClassLoader()
                    .getResourceAsStream("db/data-h2.sql");
            if (dataStream != null) {
                // 重置连接自动提交（RunScript 可能改变了设置）
                conn.setAutoCommit(true);
                // 先检查预置数据是否已存在
                try (Statement checkStmt = conn.createStatement()) {
                    var rs = checkStmt.executeQuery("SELECT COUNT(*) FROM cv_template WHERE is_preset = TRUE");
                    if (rs.next() && rs.getInt(1) == 0) {
                        org.h2.tools.RunScript.execute(conn,
                                new java.io.InputStreamReader(dataStream, java.nio.charset.StandardCharsets.UTF_8));
                        log.info("预置数据执行完成");
                    }
                } catch (Exception e) {
                    // 表可能刚创建，忽略
                    log.debug("检查预置数据时忽略: {}", e.getMessage());
                }
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("测试数据源 DDL 执行失败", e);
            throw new AppException(ErrorCode.DATABASE_ERROR, e);
        }

        // 创建 HikariCP 数据源
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(5);
        hikariConfig.setDriverClassName("org.h2.Driver");

        dialect = SQLDialect.H2;
        dataSource = new HikariDataSource(hikariConfig);
        dslContext = DSL.using(dataSource, dialect);
        log.info("测试数据源初始化完成");
    }

    /**
     * 关闭数据源，释放连接池资源。
     */
    public static synchronized void shutdown() {
        if (dataSource instanceof HikariDataSource) {
            log.info("关闭数据源...");
            ((HikariDataSource) dataSource).close();
            dataSource = null;
            dslContext = null;
        }
    }
}
