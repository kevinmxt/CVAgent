package me.maxt.cv.service;

import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import me.maxt.cv.store.entity.CvTemplate;
import me.maxt.cv.store.repository.CvTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CvTemplateService 单元测试。
 *
 * @author maxt
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class CvTemplateServiceTest {

    @Mock
    private CvTemplateRepository repository;

    private CvTemplateService service;

    @BeforeEach
    void setUp() {
        service = new CvTemplateService(repository);
    }

    @Test
    @DisplayName("listAll 应调用 repository.findAll 并返回结果")
    void testListAll() {
        CvTemplate template = new CvTemplate();
        template.setId(1L);
        template.setName("测试模板");
        when(repository.findAll()).thenReturn(List.of(template));

        List<CvTemplate> result = service.listAll();

        assertEquals(1, result.size());
        assertEquals("测试模板", result.get(0).getName());
        verify(repository).findAll();
    }

    @Test
    @DisplayName("getById 找到记录时应返回实体")
    void testGetByIdFound() {
        CvTemplate template = new CvTemplate();
        template.setId(1L);
        template.setName("测试模板");
        when(repository.findById(1L)).thenReturn(Optional.of(template));

        CvTemplate result = service.getById(1L);

        assertEquals(1L, result.getId());
        assertEquals("测试模板", result.getName());
    }

    @Test
    @DisplayName("getById 找不到记录时应抛出 CV_TEMPLATE_NOT_FOUND 异常")
    void testGetByIdNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.getById(999L));
        assertEquals(ErrorCode.CV_TEMPLATE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("create 应设置 isPreset 为 false 并调用 insert")
    void testCreate() {
        CvTemplate template = new CvTemplate();
        template.setName("新模板");
        template.setTemplateContent("<html>test</html>");

        when(repository.insert(any(CvTemplate.class))).thenAnswer(inv -> {
            CvTemplate arg = inv.getArgument(0);
            arg.setId(1L);
            return arg;
        });

        CvTemplate result = service.create(template);

        assertNotNull(result.getId());
        assertFalse(result.getIsPreset());  // 新建的模板不是预置模板
        verify(repository).insert(any(CvTemplate.class));
    }

    @Test
    @DisplayName("update 找到记录时应调用 repository.update")
    void testUpdate() {
        CvTemplate existing = new CvTemplate();
        existing.setId(1L);
        existing.setName("旧名称");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        CvTemplate update = new CvTemplate();
        update.setName("新名称");

        when(repository.update(any(CvTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        CvTemplate result = service.update(1L, update);

        assertEquals("新名称", result.getName());
        verify(repository).update(any(CvTemplate.class));
    }

    @Test
    @DisplayName("delete 预置模板时应抛出 CANNOT_DELETE_PRESET_TEMPLATE 异常")
    void testDeletePresetTemplate() {
        CvTemplate preset = new CvTemplate();
        preset.setId(1L);
        preset.setIsPreset(true);
        when(repository.findById(1L)).thenReturn(Optional.of(preset));

        AppException ex = assertThrows(AppException.class, () -> service.delete(1L));
        assertEquals(ErrorCode.CANNOT_DELETE_PRESET_TEMPLATE, ex.getErrorCode());
        verify(repository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("delete 非预置模板时应调用 repository.deleteById")
    void testDeleteNormalTemplate() {
        CvTemplate normal = new CvTemplate();
        normal.setId(1L);
        normal.setIsPreset(false);
        when(repository.findById(1L)).thenReturn(Optional.of(normal));

        service.delete(1L);

        verify(repository).deleteById(1L);
    }
}
