package vn.iotstar.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import vn.iotstar.domain.entity.Role;
import vn.iotstar.domain.entity.User;
import vn.iotstar.domain.enums.UserRole;
import vn.iotstar.repository.RoleRepository;
import vn.iotstar.repository.UserRepository;

class AdminBootstrapServiceTest {

    @Test
    void incompleteOptionalBootstrapDoesNotPreventApplicationStartup() {
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AdminBootstrapService service = new AdminBootstrapService(users, roles, encoder);
        ReflectionTestUtils.setField(service, "username", "admin");
        ReflectionTestUtils.setField(service, "email", "");
        ReflectionTestUtils.setField(service, "password", "short");

        assertDoesNotThrow(() -> service.run(null));
        verify(users).findByUsernameIgnoreCase("admin");
        verifyNoInteractions(roles, encoder);
    }

    @Test
    void eightCharacterPasswordIsAcceptedForBootstrap() {
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AdminBootstrapService service = new AdminBootstrapService(users, roles, encoder);
        Role adminRole = new Role();
        adminRole.setName(UserRole.ADMIN);
        when(users.findByUsernameIgnoreCase("admin")).thenReturn(Optional.empty());
        when(users.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);
        when(roles.findByName(UserRole.ADMIN)).thenReturn(Optional.of(adminRole));
        when(encoder.encode("12345678")).thenReturn("hashed-password");
        ReflectionTestUtils.setField(service, "username", "admin");
        ReflectionTestUtils.setField(service, "email", "admin@example.com");
        ReflectionTestUtils.setField(service, "password", "12345678");

        assertDoesNotThrow(() -> service.run(null));
        verify(users).save(any(User.class));
    }
}
