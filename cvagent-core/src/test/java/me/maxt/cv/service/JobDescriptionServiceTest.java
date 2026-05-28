package me.maxt.cv.service;

import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import me.maxt.cv.store.entity.GeneratedCv;
import me.maxt.cv.store.entity.JobDescription;
import me.maxt.cv.store.repository.JobDescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JobDescriptionService 单元测试。
 *
 * @author maxt
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class JobDescriptionServiceTest {

    @Mock
    private JobDescriptionRepository repository;

    private JobDescriptionService service;

    @BeforeEach
    void setUp() {
        service = new JobDescriptionService(repository);
    }

    @Test
    @DisplayName("list 应返回分页结果")
    void testList() {
        JobDescription jd = new JobDescription();
        jd.setId(1L);
        jd.setTitle("后端工程师");
        when(repository.findAll(anyInt(), anyInt())).thenReturn(List.of(jd));
        when(repository.count()).thenReturn(1);

        List<JobDescription> result = service.list(1, 10);

        assertEquals(1, result.size());
        assertEquals("后端工程师", result.get(0).getTitle());
        assertEquals(1, service.count());
    }

    @Test
    @DisplayName("getById 应返回正确实体")
    void testGetById() {
        JobDescription jd = new JobDescription();
        jd.setId(1L);
        jd.setTitle("测试JD");
        when(repository.findById(1L)).thenReturn(Optional.of(jd));

        JobDescription result = service.getById(1L);
        assertEquals("测试JD", result.getTitle());
    }

    @Test
    @DisplayName("getById 不存在时应抛出异常")
    void testGetByIdNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.getById(999L));
        assertEquals(ErrorCode.JOB_DESCRIPTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("create 应调用 repository.insert")
    void testCreate() {
        JobDescription jd = new JobDescription();
        jd.setTitle("新JD");
        jd.setContent("岗位描述内容");
        when(repository.insert(any(JobDescription.class))).thenAnswer(inv -> {
            JobDescription arg = inv.getArgument(0);
            arg.setId(1L);
            return arg;
        });

        JobDescription result = service.create(jd);
        assertNotNull(result.getId());
        verify(repository).insert(any(JobDescription.class));
    }

    @Test
    @DisplayName("update 应合并非空字段")
    void testUpdate() {
        JobDescription existing = new JobDescription();
        existing.setId(1L);
        existing.setTitle("旧标题");
        existing.setCompany("旧公司");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        JobDescription update = new JobDescription();
        update.setTitle("新标题");
        when(repository.update(any(JobDescription.class))).thenAnswer(inv -> inv.getArgument(0));

        JobDescription result = service.update(1L, update);
        assertEquals("新标题", result.getTitle());
        assertEquals("旧公司", result.getCompany());  // 保持原值
    }

    @Test
    @DisplayName("delete 应调用 repository.deleteById")
    void testDelete() {
        JobDescription jd = new JobDescription();
        jd.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(jd));

        service.delete(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("delete 不存在时应抛出异常")
    void testDeleteNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.delete(999L));
    }
}
