package com.melomarit.melopost.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmApiServiceTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(LlmApiService.class);

    @Test
    void noBeanByDefault() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(LlmApiService.class));
        runner.withPropertyValues("melopost.llm-api.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(LlmApiService.class));
    }

    @Test
    void contextStartsWhenPythonMissing() {
        runner.withPropertyValues("melopost.llm-api.enabled=true",
                        "melopost.llm-api.python-executable=/nonexistent/python-melopost-test")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(LlmApiService.class);
                });
    }

    @Test
    void processBuilderPassesTokenOnlyViaEnvironment() {
        String token = "x".repeat(40);
        LlmApiService s = new LlmApiService("python3", "llm_api.py", 8123, token);
        ProcessBuilder pb = s.createProcessBuilder();
        assertThat(pb.command()).containsExactly("python3", "llm_api.py");
        assertThat(pb.command()).doesNotContain(token);
        assertThat(pb.environment().get("LLM_API_TOKEN")).isEqualTo(s.getApiToken()).isEqualTo(token);
        assertThat(pb.environment().get("LLM_API_PORT")).isEqualTo("8123");
    }

    @Test
    void generatesRandomTokenWhenBlank() {
        LlmApiService a = new LlmApiService("python3", "llm_api.py", 8000, null);
        LlmApiService b = new LlmApiService("python3", "llm_api.py", 8000, "  ");
        assertThat(a.getApiToken()).hasSizeGreaterThanOrEqualTo(32).matches("[A-Za-z0-9_-]+");
        assertThat(b.getApiToken()).hasSizeGreaterThanOrEqualTo(32).matches("[A-Za-z0-9_-]+");
        assertThat(a.getApiToken()).isNotEqualTo(b.getApiToken());
    }

    @Test
    void rejectsShortSuppliedToken() {
        assertThatThrownBy(() -> new LlmApiService("python3", "llm_api.py", 8000, "short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("short");
    }

    @Test
    void ignoresTokenSpringProperty() {
        String propertyToken = "p".repeat(40);
        runner.withPropertyValues("melopost.llm-api.enabled=true",
                        "melopost.llm-api.python-executable=/nonexistent/python-melopost-test",
                        "melopost.llm-api.token=" + propertyToken)
                .run(ctx -> assertThat(ctx.getBean(LlmApiService.class).getApiToken()).isNotEqualTo(propertyToken));
    }

    @Test
    void rejectsInvalidPort() {
        assertThatThrownBy(() -> new LlmApiService("python3", "llm_api.py", 0, null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new LlmApiService("python3", "llm_api.py", 70000, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void baseUrlIsLoopback() {
        assertThat(new LlmApiService("python3", "llm_api.py", 8123, null).getBaseUrl())
                .isEqualTo("http://127.0.0.1:8123");
    }

    @Test
    void stopIsSafeWithoutProcess() {
        new LlmApiService("python3", "llm_api.py", 8123, null).stopLlmApi();
        assertThat(true).isTrue();
    }
}
