package com.sealhackathon.api.config.seed;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parity guard — mọi slug trong {@link DevSeedCatalog} phải có hằng SLUG trong package seed.
 */
class DevSeedCatalogTest {

    private static final int EXPECTED_SLUG_COUNT = 1;

    @Test
    void catalogHasExpectedSlugCount() {
        assertEquals(EXPECTED_SLUG_COUNT, DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS.length);
    }

    @Test
    void catalogSlugsAreUnique() {
        Set<String> unique = new HashSet<>(Arrays.asList(DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS));
        assertEquals(DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS.length, unique.size(),
                "Duplicate slugs in ALL_DEV_HACKATHON_SLUGS");
    }

    @Test
    void everyCatalogSlugHasSeedConstantInPackage() throws Exception {
        Set<String> slugConstants = collectSlugConstantsFromSeedPackage();
        for (String slug : DevSeedCatalog.ALL_DEV_HACKATHON_SLUGS) {
            assertTrue(slugConstants.contains(slug),
                    "Missing seed constant for catalog slug: " + slug);
        }
    }

    @Test
    void profileE2eIsSixTeamsTwoTracks() {
        assertEquals(6, DevSeedCatalog.PROFILE_E2E.teamCount());
        assertEquals(2, DevSeedCatalog.PROFILE_E2E.trackCount());
        assertEquals(2, DevSeedCatalog.PROFILE_E2E.topNAdvance());
        assertEquals(3, DevSeedCatalog.ORPHAN_COUNT);
        assertEquals("6 đội × 2 track (3+3)", DevSeedCatalog.PROFILE_E2E.distributionLabel());
    }

    private static Set<String> collectSlugConstantsFromSeedPackage() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Object.class));

        Set<String> slugs = new HashSet<>();
        for (BeanDefinition beanDef : scanner.findCandidateComponents("com.sealhackathon.api.config.seed")) {
            Class<?> clazz = Class.forName(beanDef.getBeanClassName());
            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (field.getType() != String.class) {
                    continue;
                }
                String name = field.getName();
                if (!name.startsWith("SLUG") && !name.contains("SLUG")) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(null);
                if (value instanceof String s && s.startsWith("seal-")) {
                    slugs.add(s);
                }
            }
        }
        slugs.add(DevSeedCatalog.SLUG_E2E_ONGOING);
        return slugs;
    }
}
