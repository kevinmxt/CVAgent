package me.maxt.cv.service;

import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import me.maxt.cv.store.entity.CvTemplate;
import me.maxt.cv.store.entity.GeneratedCv;
import me.maxt.cv.store.entity.WorkExperience;
import me.maxt.cv.store.repository.CvTemplateRepository;
import me.maxt.cv.store.repository.GeneratedCvRepository;
import me.maxt.cv.store.repository.JobDescriptionRepository;
import me.maxt.cv.store.repository.WorkExperienceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CvGenerationService 单元测试。
 *
 * @author maxt
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class CvGenerationServiceTest {

    @Mock private WorkExperienceRepository workExpRepo;
    @Mock private CvTemplateRepository templateRepo;
    @Mock private JobDescriptionRepository jdRepo;
    @Mock private GeneratedCvRepository generatedCvRepo;

    private CvGenerationService service;

    @BeforeEach
    void setUp() {
        service = new CvGenerationService(workExpRepo, templateRepo, jdRepo, generatedCvRepo);
    }

    @Test
    @DisplayName("fillTemplate 应替换所有占位符")
    void testFillTemplate() {
        WorkExperience we = new WorkExperience();
        we.setId(1L);
        we.setPersonName("张三");
        we.setPersonEmail("zhangsan@test.com");
        we.setPersonPhone("13800138000");
        we.setSummary("后端工程师");
        we.setProfessionalExp("5年后端开发经验");
        we.setEducation("本科");
        we.setSkills("Java, Spring, MySQL");

        CvTemplate template = new CvTemplate();
        template.setId(1L);
        template.setTemplateContent("<html><h1>{{person_name}}</h1><p>{{summary}}</p><p>{{skills}}</p></html>");

        String result = service.fillTemplate(we, template);

        // 验证占位符被替换
        assertTrue(result.contains("张三"));
        assertTrue(result.contains("后端工程师"));
        assertTrue(result.contains("Java, Spring, MySQL"));
        // 验证占位符已被移除
        assertFalse(result.contains("{{person_name}}"));
        assertFalse(result.contains("{{summary}}"));
    }

    @Test
    @DisplayName("fillTemplate 应将 null 值替换为空字符串")
    void testFillTemplateWithNullValues() {
        WorkExperience we = new WorkExperience();
        we.setPersonName(null);

        CvTemplate template = new CvTemplate();
        template.setTemplateContent("<html><h1>{{person_name}}</h1></html>");

        String result = service.fillTemplate(we, template);

        // null 值应被替换为空字符串
        assertFalse(result.contains("{{person_name}}"));
        assertTrue(result.contains("<h1></h1>"));
    }

    @Test
    @DisplayName("loadContext 应加载工作和模板两个关联数据")
    void testLoadContext() {
        WorkExperience we = new WorkExperience(); we.setId(1L);
        CvTemplate template = new CvTemplate(); template.setId(2L);

        when(workExpRepo.findById(1L)).thenReturn(Optional.of(we));
        when(templateRepo.findById(2L)).thenReturn(Optional.of(template));

        CvGenerationService.GenerationContext ctx = service.loadContext(1L, 2L);

        assertNotNull(ctx.getWorkExperience());
        assertNotNull(ctx.getTemplate());
    }

    @Test
    @DisplayName("loadContext 找不到工作经历时应抛出异常")
    void testLoadContextWorkExpNotFound() {
        when(workExpRepo.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> service.loadContext(999L, 1L));
        assertEquals(ErrorCode.WORK_EXPERIENCE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("getGeneratedCv 找到记录时应返回实体")
    void testGetGeneratedCv() {
        GeneratedCv cv = new GeneratedCv();
        cv.setId(1L);
        cv.setFinalContent("<html>test</html>");
        when(generatedCvRepo.findById(1L)).thenReturn(Optional.of(cv));

        GeneratedCv result = service.getGeneratedCv(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("getGeneratedCv 找不到记录时应抛出异常")
    void testGetGeneratedCvNotFound() {
        when(generatedCvRepo.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> service.getGeneratedCv(999L));
        assertEquals(ErrorCode.GENERATED_CV_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("updateContent 应更新内容和状态为 FINAL")
    void testUpdateContent() {
        GeneratedCv cv = new GeneratedCv();
        cv.setId(1L);
        cv.setStatus(GeneratedCv.STATUS_DRAFT);
        when(generatedCvRepo.findById(1L)).thenReturn(Optional.of(cv));
        when(generatedCvRepo.update(any(GeneratedCv.class))).thenAnswer(inv -> inv.getArgument(0));

        GeneratedCv result = service.updateContent(1L, "<html>updated</html>");

        assertEquals("<html>updated</html>", result.getFinalContent());
        assertEquals(GeneratedCv.STATUS_FINAL, result.getStatus());
        verify(generatedCvRepo).update(any(GeneratedCv.class));
    }

    @Test
    @DisplayName("toRoleScoresJson 应序列化 Map 为 JSON")
    void testToRoleScoresJson() {
        String json = service.toRoleScoresJson(java.util.Map.of("hr", 0.8, "techExpert", 0.7));
        assertTrue(json.contains("hr"));
        assertTrue(json.contains("techExpert"));
    }
}
