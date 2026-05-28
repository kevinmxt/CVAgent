package me.maxt.cv.store.repository;

import me.maxt.cv.store.datasource.DataSourceConfig;
import me.maxt.cv.store.entity.CvGenerationRecord;
import me.maxt.cv.store.entity.GeneratedCv;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GeneratedCvRepository 集成测试（H2 内存数据库）。
 *
 * @author maxt
 * @since 1.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GeneratedCvRepositoryTest {

    private static GeneratedCvRepository repository;

    @BeforeAll
    static void setUp() throws Exception {
        DataSourceConfig.initializeForTest("jdbc:h2:mem:test_gen_cv;DB_CLOSE_DELAY=-1");
        repository = new GeneratedCvRepository();

        // 插入父表数据以满足 FK 约束
        // cv_template 已通过 data-h2.sql 预置了 2 条数据（id=1, id=2）
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                "jdbc:h2:mem:test_gen_cv;DB_CLOSE_DELAY=-1", "sa", "");
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO work_experience (person_name, created_at, updated_at) VALUES ('test', NOW(), NOW())");
            stmt.execute("INSERT INTO job_description (title, content, created_at, updated_at) VALUES ('test_jd', 'test', NOW(), NOW())");
        }
    }

    @AfterAll
    static void tearDown() {
        DataSourceConfig.shutdown();
    }

    @Test
    @Order(1)
    @DisplayName("insert：插入生成简历")
    void testInsert() {
        GeneratedCv cv = new GeneratedCv();
        cv.setWorkExpId(1L);
        cv.setTemplateId(1L);
        cv.setJdId(1L);
        cv.setFinalContent("<html>测试简历</html>");
        cv.setFinalScore(0.85);
        cv.setFinalFeedback("很好");
        cv.setRoleScores("{\"hr\":0.8,\"tech\":0.9}");
        cv.setIterationCount(2);
        cv.setStatus(GeneratedCv.STATUS_DRAFT);

        GeneratedCv saved = repository.insert(cv);
        assertNotNull(saved.getId());
        assertEquals(0.85, saved.getFinalScore());
        assertEquals(GeneratedCv.STATUS_DRAFT, saved.getStatus());
    }

    @Test
    @Order(2)
    @DisplayName("findById：根据 ID 查询")
    void testFindById() {
        GeneratedCv cv = new GeneratedCv();
        cv.setWorkExpId(1L);
        cv.setTemplateId(1L);
        cv.setJdId(1L);
        cv.setFinalContent("<html>test2</html>");
        cv.setFinalScore(0.6);
        cv.setIterationCount(3);
        cv.setStatus(GeneratedCv.STATUS_FINAL);
        GeneratedCv saved = repository.insert(cv);

        var result = repository.findById(saved.getId());
        assertTrue(result.isPresent());
        assertEquals(0.6, result.get().getFinalScore());
    }

    @Test
    @Order(3)
    @DisplayName("findAll：分页查询")
    void testFindAll() {
        List<GeneratedCv> list = repository.findAll(0, 10);
        assertFalse(list.isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("count：查询总数")
    void testCount() {
        assertTrue(repository.count() >= 2);
    }

    @Test
    @Order(5)
    @DisplayName("update：更新状态")
    void testUpdate() {
        GeneratedCv cv = new GeneratedCv();
        cv.setWorkExpId(1L);
        cv.setTemplateId(1L);
        cv.setJdId(1L);
        cv.setFinalContent("<html>old</html>");
        cv.setFinalScore(0.5);
        cv.setIterationCount(1);
        cv.setStatus(GeneratedCv.STATUS_DRAFT);
        GeneratedCv saved = repository.insert(cv);

        saved.setFinalContent("<html>new</html>");
        saved.setStatus(GeneratedCv.STATUS_EXPORTED);
        repository.update(saved);

        var reloaded = repository.findById(saved.getId()).get();
        assertEquals("<html>new</html>", reloaded.getFinalContent());
        assertEquals(GeneratedCv.STATUS_EXPORTED, reloaded.getStatus());
    }

    @Test
    @Order(6)
    @DisplayName("insertRecord：插入迭代记录并查询")
    void testInsertAndFindRecords() {
        GeneratedCv cv = new GeneratedCv();
        cv.setWorkExpId(1L);
        cv.setTemplateId(1L);
        cv.setJdId(1L);
        cv.setFinalContent("<html>cv</html>");
        cv.setFinalScore(0.7);
        cv.setIterationCount(1);
        cv.setStatus(GeneratedCv.STATUS_DRAFT);
        GeneratedCv saved = repository.insert(cv);

        // 插入迭代记录
        CvGenerationRecord record = new CvGenerationRecord();
        record.setGeneratedCvId(saved.getId());
        record.setIteration(1);
        record.setRoleScores("{\"hr\":0.7}");
        record.setOverallScore(0.7);
        record.setFeedback("good");
        record.setCvSnapshot("<html>snapshot1</html>");
        CvGenerationRecord savedRecord = repository.insertRecord(record);
        assertNotNull(savedRecord.getId());

        // 查询迭代记录
        List<CvGenerationRecord> records = repository.findRecordsByGeneratedCvId(saved.getId());
        assertEquals(1, records.size());
        assertEquals(1, records.get(0).getIteration());
        assertEquals(0.7, records.get(0).getOverallScore());
    }

    @Test
    @Order(7)
    @DisplayName("deleteById：级联删除迭代记录")
    void testDeleteById() {
        GeneratedCv cv = new GeneratedCv();
        cv.setWorkExpId(1L);
        cv.setTemplateId(1L);
        cv.setJdId(1L);
        cv.setFinalContent("<html>del</html>");
        cv.setFinalScore(0.3);
        cv.setIterationCount(1);
        cv.setStatus(GeneratedCv.STATUS_DRAFT);
        GeneratedCv saved = repository.insert(cv);

        // 插入迭代记录
        CvGenerationRecord record = new CvGenerationRecord();
        record.setGeneratedCvId(saved.getId());
        record.setIteration(1);
        record.setOverallScore(0.3);
        record.setCvSnapshot("<html>s</html>");
        repository.insertRecord(record);

        // 删除
        repository.deleteById(saved.getId());
        assertFalse(repository.findById(saved.getId()).isPresent());
    }
}
