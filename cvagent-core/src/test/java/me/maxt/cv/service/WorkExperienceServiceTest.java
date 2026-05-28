package me.maxt.cv.service;

import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import me.maxt.cv.store.entity.WorkExperience;
import me.maxt.cv.store.repository.WorkExperienceRepository;
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
 * WorkExperienceService 单元测试。
 *
 * @author maxt
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class WorkExperienceServiceTest {

    @Mock
    private WorkExperienceRepository repository;

    private WorkExperienceService service;

    @BeforeEach
    void setUp() {
        service = new WorkExperienceService(repository);
    }

    @Test
    @DisplayName("list 应调用 repository.findAll 并返回分页结果")
    void testList() {
        WorkExperience we = new WorkExperience();
        we.setId(1L);
        we.setPersonName("张三");
        when(repository.findAll(anyInt(), anyInt())).thenReturn(List.of(we));
        when(repository.count()).thenReturn(1);

        List<WorkExperience> result = service.list(1, 10);

        assertEquals(1, result.size());
        assertEquals("张三", result.get(0).getPersonName());
        assertEquals(1, service.count());
    }

    @Test
    @DisplayName("getById 找到记录时应返回实体")
    void testGetByIdFound() {
        WorkExperience we = new WorkExperience();
        we.setId(1L);
        we.setPersonName("张三");
        when(repository.findById(1L)).thenReturn(Optional.of(we));

        WorkExperience result = service.getById(1L);

        assertEquals(1L, result.getId());
        assertEquals("张三", result.getPersonName());
    }

    @Test
    @DisplayName("getById 找不到记录时应抛出 WORK_EXPERIENCE_NOT_FOUND 异常")
    void testGetByIdNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.getById(999L));
        assertEquals(ErrorCode.WORK_EXPERIENCE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("update 应合并非空字段并调用 repository.update")
    void testUpdate() {
        WorkExperience existing = new WorkExperience();
        existing.setId(1L);
        existing.setPersonName("张三");
        existing.setSkills("Java");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        WorkExperience update = new WorkExperience();
        update.setPersonName("李四");  // 修改姓名
        update.setSkills(null);        // 不修改技能

        when(repository.update(any(WorkExperience.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkExperience result = service.update(1L, update);

        assertEquals("李四", result.getPersonName());  // 已修改
        assertEquals("Java", result.getSkills());      // 保持原值
    }

    @Test
    @DisplayName("delete 应调用 repository.deleteById")
    void testDelete() {
        WorkExperience we = new WorkExperience();
        we.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(we));

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("delete 不存在记录时应抛出异常")
    void testDeleteNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> service.delete(999L));
        verify(repository, never()).deleteById(anyLong());
    }
}
