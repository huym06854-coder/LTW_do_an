package vn.iotstar.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.domain.entity.User;
import vn.iotstar.domain.enums.*;
import vn.iotstar.repository.*;

/**
 * Only runs when a trusted operator configures ALL environment variables.
 * Never logs or persists the plaintext bootstrap password.
 */
@Service
public class AdminBootstrapService implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapService.class);
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    @Value("${ADMIN_BOOTSTRAP_USERNAME:${ADMIN_USERNAME:}}")
    private String username;
    @Value("${ADMIN_BOOTSTRAP_EMAIL:${ADMIN_EMAIL:}}")
    private String email;
    @Value("${ADMIN_BOOTSTRAP_PASSWORD:${ADMIN_PASSWORD:}}")
    private String password;
    @Value("${ADMIN_BOOTSTRAP_FULL_NAME:${ADMIN_FULL_NAME:System Administrator}}")
    private String fullName;

    public AdminBootstrapService(UserRepository users, RoleRepository roles, PasswordEncoder encoder) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username == null || username.isBlank())
            return;
        var existing = users.findByUsernameIgnoreCase(username.trim());
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getRole().getName() != UserRole.ADMIN)
                throw new IllegalStateException("Bootstrap username already belongs to a non-admin");
            if (user.getStatus() == UserStatus.ACTIVE)
                return; // Never silently rotate an active admin's password.
        }
        if (email == null || email.isBlank() || password == null || password.length() < 8) {
            log.warn(
                    "Admin bootstrap skipped: provide an email and a password of at least 8 characters to create or reactivate the configured admin account");
            return;
        }
        if (existing.isPresent()) {
            User user = existing.get();
            user.setStatus(UserStatus.ACTIVE);
            user.setPassword(encoder.encode(password));
            return;
        }
        if (users.existsByEmailIgnoreCase(email.trim()))
            throw new IllegalStateException("Bootstrap email is already in use");
        var role = roles.findByName(UserRole.ADMIN).orElseThrow(() -> new IllegalStateException("Missing ADMIN role"));
        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email.trim());
        user.setFullName(fullName == null || fullName.isBlank() ? "System Administrator" : fullName.trim());
        user.setPassword(encoder.encode(password));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        users.save(user);
    }
}
