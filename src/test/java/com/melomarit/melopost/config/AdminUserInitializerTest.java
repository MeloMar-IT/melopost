package com.melomarit.melopost.config;

import com.melomarit.melopost.model.User;
import com.melomarit.melopost.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.data.cassandra.core.EntityWriteResult;
import org.springframework.data.cassandra.core.InsertOptions;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminUserInitializerTest {

    private final UserRepository users = mock(UserRepository.class);
    private final CassandraOperations operations = mock(CassandraOperations.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    private void run(String password) throws Exception {
        new AdminUserInitializer().adminUserBootstrap(users, operations, encoder, password).run();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsAdminWithConditionalInsertWhenNoUsersExist() throws Exception {
        when(users.count()).thenReturn(0L);
        when(encoder.encode("a-long-password-1")).thenReturn("ENCODED");
        EntityWriteResult<User> result = mock(EntityWriteResult.class);
        when(result.wasApplied()).thenReturn(true);
        when(operations.insert(any(User.class), any(InsertOptions.class))).thenReturn(result);

        run("a-long-password-1");

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<InsertOptions> options = ArgumentCaptor.forClass(InsertOptions.class);
        verify(operations, times(1)).insert(user.capture(), options.capture());
        assertEquals("admin", user.getValue().getUsername());
        assertEquals("ENCODED", user.getValue().getPassword());
        assertEquals(Set.of("ADMIN", "USER"), user.getValue().getRoles());
        assertEquals(Boolean.TRUE, user.getValue().getActive());
        assertEquals(AdminUserInitializer.BOOTSTRAP_ADMIN_UUID, user.getValue().getUuid());
        assertTrue(options.getValue().isIfNotExists());
        verify(users, never()).save(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "short-pw-11"})
    void createsNothingForMissingBlankOrShortPassword(String password) throws Exception {
        when(users.count()).thenReturn(0L);

        run(password);

        verify(operations, never()).insert(any(User.class), any(InsertOptions.class));
        verify(encoder, never()).encode(any());
    }

    @Test
    void createsNothingWhenAUserAlreadyExists() throws Exception {
        when(users.count()).thenReturn(1L);

        run("a-long-password-1");

        verify(operations, never()).insert(any(User.class), any(InsertOptions.class));
    }
}
