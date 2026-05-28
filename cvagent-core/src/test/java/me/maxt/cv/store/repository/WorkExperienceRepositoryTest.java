package me.maxt.cv.store.repository;

import me.maxt.cv.store.datasource.DataSourceConfig;
import me.maxt.cv.store.entity.WorkExperience;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WorkExperienceRepository 集成测试（H2 内存数据库）。
 *
 * @author maxt
 * @since 1.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkExperienceRepositoryTest {

    private static WorkExperienceRepository repository;
    private static Long savedId;

    @BeforeAll
    static void setUp() {
        DataSourceConfig.initializeForTest("jdbc:h2:mem:test_work_exp;DB_CLOSE_DELAY=-1");
        repository = new WorkExperienceRepository();
    }

    @AfterAll
    static void tearDown() {
        DataSourceConfig.shutdown();
    }

    @Test
    @Order(1)
    @DisplayName("insert：插入并返回带 ID 的实体")
    void testInsert() {
        WorkExperience we = new WorkExperience();
        we.setPersonName("测试用户");
        we.setPersonEmail("test@test.com");
        we.setSkills("Java, Spring");
        we.setProfessionalExp("3年开发经验");
        we.setRawFileName("test_cv.txt");
        we.setRawFileType("text/plain");

        WorkExperience saved = repository.insert(we);
        assertNotNull(saved.getId());
        assertEquals("测试用户", saved.getPersonName());
        savedId = saved.getId();
    }

    @Test
    @Order(2)
    @DisplayName("findById：根据 ID 查询")
    void testFindById() {
        var result = repository.findById(savedId);
        assertTrue(result.isPresent());
        assertEquals("测试用户", result.get().getPersonName());
    }

    @Test
    @Order(3)
    @DisplayName("findAll：分页查询")
    void testFindAll() {
        List<WorkExperience> list = repository.findAll(0, 10);
        assertFalse(list.isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("count：查询总数")
    void testCount() {
        assertTrue(repository.count() >= 1);
    }

    @Test
    @Order(5)
    @DisplayName("update：更新工作经历")
    void testUpdate() {
        var existing = repository.findById(savedId).get();
        existing.setPersonName("更新用户名");
        existing.setSkills("Java, Spring, Docker");

        WorkExperience updated = repository.update(existing);
        assertEquals("更新用户名", updated.getPersonName());

        // 验证持久化
        var reloaded = repository.findById(savedId).get();
        assertEquals("更新用户名", reloaded.getPersonName());
        assertEquals("Java, Spring, Docker", reloaded.getSkills());
    }

    @Test
    @Order(6)
    @DisplayName("deleteById：删除")
    void testDeleteById() {
        WorkExperience we = new WorkExperience();
        we.setPersonName("待删除用户");
        WorkExperience saved = repository.insert(we);

        int rows = repository.deleteById(saved.getId());
        assertEquals(1, rows);
        assertFalse(repository.findById(saved.getId()).isPresent());
    }

    @Test
    @Order(7)
    @DisplayName("findById：不存在的 ID")
    void testFindByIdNotFound() {
        assertFalse(repository.findById(99999L).isPresent());
    }
}
