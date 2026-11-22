package com.melomarit.melopost.config;

import com.melomarit.melopost.model.User;
import com.melomarit.melopost.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.EntityWriteResult;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Checks that application.properties maps the property to the runner and BCrypt stores it. */
class AdminUserInitializerConfigTest {

    private final UserRepository users = mock(UserRepository.class);
    private final CassandraOperations operations = mock(CassandraOperations.class);

    @SuppressWarnings("unchecked")
    private ApplicationContextRunner runner() {
        EntityWriteResult<User> result = mock(EntityWriteResult.class);
        when(result.wasApplied()).thenReturn(true);
        when(operations.insert(any(User.class), any(InsertOptions.class))).thenReturn(result);
        when(users.count()).thenReturn(0L);
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(AdminUserInitializer.class)
                .withBean(UserRepository.class, () -> users)
                .withBean(CassandraOperations.class, () -> operations)
                .withBean(PasswordEncoder.class, BCryptPasswordEncoder::new);
    }

    @Test
    void propertyValueIsEncodedWithBcryptAndStored() {
        runner().withPropertyValues("melopost.admin.initial-password=configured-secret-1").run(ctx -> {
            ctx.getBean(CommandLineRunner.class).run();
            ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
            verify(operations).insert(user.capture(), any(InsertOptions.class));
            assertTrue(new BCryptPasswordEncoder().matches("configured-secret-1", user.getValue().getPassword()));
        });
    }

    @Test
    void unsetPasswordCreatesNothing() {
        runner().withPropertyValues("melopost.admin.initial-password=").run(ctx -> {
            ctx.getBean(CommandLineRunner.class).run();
            verify(operations, never()).insert(any(User.class), any(InsertOptions.class));
        });
    }
}
