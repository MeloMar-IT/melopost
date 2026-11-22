package com.melomarit.melopost.config;

import com.melomarit.melopost.model.DataSource;
import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.repository.DataSourceRepository;
import com.melomarit.melopost.repository.IncidentNoteRepository;
import com.melomarit.melopost.repository.PostmortemRepository;
import com.melomarit.melopost.repository.UserRepository;
import com.melomarit.melopost.service.DataSourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TestDataInitializerConditionTest {

    private final PostmortemRepository postmortems = mock(PostmortemRepository.class);
    private final DataSourceRepository dataSources = mock(DataSourceRepository.class);
    private final DataSourceService dataSourceService = mock(DataSourceService.class);
    private final IncidentNoteRepository notes = mock(IncidentNoteRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    private ApplicationContextRunner runner(String flag, String... profiles) {
        ApplicationContextRunner r = new ApplicationContextRunner()
                .withUserConfiguration(TestDataInitializer.class)
                .withBean(PostmortemRepository.class, () -> postmortems)
                .withBean(DataSourceRepository.class, () -> dataSources)
                .withBean(DataSourceService.class, () -> dataSourceService)
                .withBean(IncidentNoteRepository.class, () -> notes)
                .withBean(UserRepository.class, () -> users)
                .withBean(PasswordEncoder.class, () -> encoder)
                .withInitializer(ctx -> ctx.getEnvironment().setActiveProfiles(profiles));
        return flag == null ? r : r.withPropertyValues("melopost.seed.enabled=" + flag);
    }

    static Stream<Object[]> disabledCases() {
        return Stream.of(
                new Object[]{"true", new String[]{}},
                new Object[]{"true", new String[]{"prod"}},
                new Object[]{"true", new String[]{"acc"}},
                new Object[]{"true", new String[]{"prod", "dev"}},
                new Object[]{null, new String[]{"dev"}},
                new Object[]{null, new String[]{"test"}},
                new Object[]{"false", new String[]{"dev"}});
    }

    @ParameterizedTest
    @MethodSource("disabledCases")
    void seederIsMissing(String flag, String[] profiles) {
        runner(flag, profiles).run(ctx -> assertThat(ctx).doesNotHaveBean(CommandLineRunner.class));
    }

    @Test
    void seederExistsForDevAndTestWithFlag() {
        runner("true", "dev").run(ctx -> assertThat(ctx).hasSingleBean(CommandLineRunner.class));
        runner("true", "test").run(ctx -> assertThat(ctx).hasSingleBean(CommandLineRunner.class));
    }

    @Test
    void doesNothingWhenPostmortemsExist() {
        when(users.count()).thenReturn(5L);
        when(postmortems.count()).thenReturn(3L);
        runner("true", "dev").run(ctx -> {
            ctx.getBean(CommandLineRunner.class).run();
            verify(postmortems, never()).save(any());
            verify(postmortems, never()).deleteAll();
            verify(dataSources, never()).deleteAll();
            verify(notes, never()).deleteAll();
        });
    }

    @Test
    void addsSamplesWithoutDeletingWhenEmpty() {
        when(users.count()).thenReturn(5L);
        when(postmortems.count()).thenReturn(0L);
        DataSource template = new DataSource();
        template.setName("T");
        template.setType("Jira");
        template.setUrl("http://x");
        when(dataSourceService.getTemplates()).thenReturn(List.of(template));
        runner("true", "dev").run(ctx -> {
            ctx.getBean(CommandLineRunner.class).run();
            verify(postmortems, times(5)).save(any(Postmortem.class));
            verify(dataSources, times(1)).save(any(DataSource.class));
            verify(postmortems, never()).deleteAll();
            verify(dataSources, never()).deleteAll();
            verify(notes, never()).deleteAll();
        });
    }
}
