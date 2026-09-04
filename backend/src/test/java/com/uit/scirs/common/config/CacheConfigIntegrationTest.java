package com.uit.scirs.common.config;

import com.uit.scirs.category.dto.CreateCategoryDTO;
import com.uit.scirs.category.repository.CategoryRepository;
import com.uit.scirs.category.service.CategoryService;
import com.uit.scirs.department.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Proves the Caffeine-backed cache abstraction is actually wired in — not
 * just annotated. A plain Mockito unit test (see CategoryServiceTest) never
 * exercises Spring's @Cacheable/@CacheEvict proxy, so this boots the real
 * application context and watches the repository through a spy: a cache hit
 * means the repository method is never called a second time.
 *
 * @SpringBootTest reuses one cached application context (and therefore one
 * singleton CategoryService/CacheManager/spy) across every test method here,
 * so each test resets the cache and the spy's invocation count itself rather
 * than relying on execution order.
 */
@SpringBootTest
@ActiveProfiles("test")
class CacheConfigIntegrationTest {

    @Autowired CategoryService categoryService;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired CacheManager cacheManager;
    @SpyBean CategoryRepository categoryRepository;

    @BeforeEach
    void resetCacheAndSpy() {
        cacheManager.getCache(CacheConfig.CATEGORIES).clear();
        Mockito.clearInvocations(categoryRepository);
    }

    @Test
    void allFiveCachesAreRegistered() {
        assertThat(cacheManager.getCacheNames()).containsExactlyInAnyOrder(
                CacheConfig.CATEGORIES, CacheConfig.DEPARTMENTS,
                CacheConfig.LEADERBOARD, CacheConfig.PUBLIC_MAP, CacheConfig.DEPT_STATS);
    }

    @Test
    void getAll_repeatedCalls_hitTheRepositoryOnlyOnce() {
        categoryService.getAll();
        categoryService.getAll();
        categoryService.getAll();

        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    void create_evictsTheCategoriesCache_soTheNextGetAllReturnsToTheRepository() {
        categoryService.getAll();
        verify(categoryRepository, times(1)).findAll();

        long roadsId = departmentRepository.findByName("Roads").orElseThrow().getId();
        CreateCategoryDTO dto = new CreateCategoryDTO();
        dto.setName("Cache Test Category " + System.nanoTime());
        dto.setDescription("Created by CacheConfigIntegrationTest");
        dto.setDepartmentId(roadsId);
        dto.setIcon("road");
        dto.setColorHex("#F97316");
        categoryService.create(dto);

        categoryService.getAll();

        verify(categoryRepository, times(2)).findAll();
    }
}
