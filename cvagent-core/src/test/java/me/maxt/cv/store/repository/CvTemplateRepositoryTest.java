package me.maxt.cv.store.repository;

import me.maxt.cv.store.datasource.DataSourceConfig;
import me.maxt.cv.store.entity.CvTemplate;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CvTemplateRepository 集成测试（H2 内存数据库）。
 *
 * @author maxt
 * @since 1.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CvTemplateRepositoryTest {

    private static CvTemplateRepository repository;

    @BeforeAll
    static void setUp() {
        DataSourceConfig.initializeForTest("jdbc:h2:mem:test_template;DB_CLOSE_DELAY=-1");
        repository = new CvTemplateRepository();
    }

    @AfterAll
    static void tearDown() {
        DataSourceConfig.shutdown();
    }

    @Test
    @Order(1)
    @DisplayName("insert：插入模板")
    void testInsert() {
        CvTemplate template = new CvTemplate();
        template.setName("测试模板");
        template.setDescription("测试描述");
        template.setTemplateContent("<html>{{person_name}}</html>");
        template.setIsPreset(false);
        template.setFileName("test.html");

        CvTemplate saved = repository.insert(template);
        assertNotNull(saved.getId());
        assertEquals("测试模板", saved.getName());
    }

    @Test
    @Order(2)
    @DisplayName("findById：根据 ID 查询")
    void testFindById() {
        // 先插入一条数据
        CvTemplate template = new CvTemplate();
        template.setName("查找测试");
        template.setTemplateContent("<html>test</html>");
        template.setIsPreset(false);
        CvTemplate saved = repository.insert(template);

        var result = repository.findById(saved.getId());
        assertTrue(result.isPresent());
        assertEquals("查找测试", result.get().getName());
    }

    @Test
    @Order(3)
    @DisplayName("findAll：查询所有模板")
    void testFindAll() {
        List<CvTemplate> list = repository.findAll();
        assertFalse(list.isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("findPresetTemplates：预置模板应存在")
    void testFindPresetTemplates() {
        List<CvTemplate> presets = repository.findPresetTemplates();
        // 预置数据有 2 条
        assertEquals(2, presets.size());
        assertTrue(presets.get(0).getIsPreset());
    }

    @Test
    @Order(5)
    @DisplayName("update：更新模板")
    void testUpdate() {
        CvTemplate template = new CvTemplate();
        template.setName("待更新");
        template.setTemplateContent("<html>old</html>");
        template.setIsPreset(false);
        CvTemplate saved = repository.insert(template);

        saved.setName("已更新");
        saved.setTemplateContent("<html>new</html>");
        repository.update(saved);

        var reloaded = repository.findById(saved.getId()).get();
        assertEquals("已更新", reloaded.getName());
        assertEquals("<html>new</html>", reloaded.getTemplateContent());
    }

    @Test
    @Order(6)
    @DisplayName("deleteById：删除非预置模板")
    void testDeleteById() {
        CvTemplate template = new CvTemplate();
        template.setName("待删除");
        template.setTemplateContent("<html>del</html>");
        template.setIsPreset(false);
        CvTemplate saved = repository.insert(template);

        int rows = repository.deleteById(saved.getId());
        assertEquals(1, rows);
        assertFalse(repository.findById(saved.getId()).isPresent());
    }
}
