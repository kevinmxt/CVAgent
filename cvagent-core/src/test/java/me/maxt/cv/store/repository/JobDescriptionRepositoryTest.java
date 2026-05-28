package me.maxt.cv.store.repository;

import me.maxt.cv.store.datasource.DataSourceConfig;
import me.maxt.cv.store.entity.JobDescription;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JobDescriptionRepository 集成测试（H2 内存数据库）。
 *
 * @author maxt
 * @since 1.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JobDescriptionRepositoryTest {

    private static JobDescriptionRepository repository;

    @BeforeAll
    static void setUp() {
        DataSourceConfig.initializeForTest("jdbc:h2:mem:test_jd;DB_CLOSE_DELAY=-1");
        repository = new JobDescriptionRepository();
    }

    @AfterAll
    static void tearDown() {
        DataSourceConfig.shutdown();
    }

    @Test
    @Order(1)
    @DisplayName("insert：插入岗位描述")
    void testInsert() {
        JobDescription jd = new JobDescription();
        jd.setTitle("后端工程师");
        jd.setCompany("测试公司");
        jd.setContent("需要 Java 开发经验");
        jd.setRawFileName("jd.txt");
        jd.setRawFileType("text/plain");

        JobDescription saved = repository.insert(jd);
        assertNotNull(saved.getId());
        assertEquals("后端工程师", saved.getTitle());
    }

    @Test
    @Order(2)
    @DisplayName("findById：根据 ID 查询")
    void testFindById() {
        JobDescription jd = new JobDescription();
        jd.setTitle("前端工程师");
        jd.setContent("React 经验");
        JobDescription saved = repository.insert(jd);

        var result = repository.findById(saved.getId());
        assertTrue(result.isPresent());
        assertEquals("前端工程师", result.get().getTitle());
    }

    @Test
    @Order(3)
    @DisplayName("findAll：分页查询")
    void testFindAll() {
        List<JobDescription> list = repository.findAll(0, 10);
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
    @DisplayName("update：更新岗位描述")
    void testUpdate() {
        JobDescription jd = new JobDescription();
        jd.setTitle("待更新JD");
        jd.setContent("旧内容");
        JobDescription saved = repository.insert(jd);

        saved.setTitle("已更新JD");
        saved.setContent("新内容");
        repository.update(saved);

        var reloaded = repository.findById(saved.getId()).get();
        assertEquals("已更新JD", reloaded.getTitle());
        assertEquals("新内容", reloaded.getContent());
    }

    @Test
    @Order(6)
    @DisplayName("deleteById：删除")
    void testDeleteById() {
        JobDescription jd = new JobDescription();
        jd.setTitle("待删除JD");
        jd.setContent("xxx");
        JobDescription saved = repository.insert(jd);

        int rows = repository.deleteById(saved.getId());
        assertEquals(1, rows);
        assertFalse(repository.findById(saved.getId()).isPresent());
    }
}
