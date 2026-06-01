package me.maxt.cv.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {

    @Test
    void splitSimpleStatements() {
        List<String> result = DatabaseConfig.splitSqlStatements(
                "CREATE TABLE a (id INT);\nCREATE TABLE b (id INT);");
        assertEquals(2, result.size());
    }

    @Test
    void splitWithSemicolonsInsideStringLiteral() {
        String css = "INSERT INTO t (content) VALUES ('body{color:red;margin:0;padding:10px}');";
        List<String> result = DatabaseConfig.splitSqlStatements(css);
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("color:red;margin:0;padding:10px"));
    }

    @Test
    void splitMultipleStatementsWithStringSemicolons() {
        String sql = "CREATE TABLE t (id BIGINT AUTO_INCREMENT PRIMARY KEY, content CLOB);\n"
                + "INSERT INTO t (content) VALUES ('a{color:red;margin:0}');\n"
                + "INSERT INTO t (content) VALUES ('b{font:14px;padding:5px}');\n"
                + "CREATE INDEX IF NOT EXISTS idx_t ON t(id);";
        List<String> result = DatabaseConfig.splitSqlStatements(sql);
        assertEquals(4, result.size());
    }

    @Test
    void splitPreservesEscapedQuotes() {
        String sql = "INSERT INTO t (name) VALUES ('it''s working');";
        List<String> result = DatabaseConfig.splitSqlStatements(sql);
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("it''s working"));
    }

    @Test
    void splitHandlesDdlAndDmlTogether() {
        String ddl = """
                -- comment
                CREATE TABLE IF NOT EXISTS t (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL COMMENT 'test'
                );

                INSERT INTO t (name) VALUES ('test');
                """;
        List<String> result = DatabaseConfig.splitSqlStatements(ddl);
        assertEquals(2, result.size());
    }

    @Test
    void splitSkipsEmptyAndComments() {
        // splitSqlStatements only splits by semicolons outside strings;
        // filtering empty/comment-only fragments is done by the caller
        List<String> result = DatabaseConfig.splitSqlStatements("-- comment\n; -- another\n   ;");
        assertEquals(2, result.size());
    }
}
