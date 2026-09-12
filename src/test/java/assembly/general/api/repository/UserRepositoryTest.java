package assembly.general.api.repository;

import assembly.general.api.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("dev") // reuses H2 config; ddl-auto=create-drop gives a fresh schema per test class
class UserRepositoryTest {

    @Autowired private UserRepository userRepository;

    private User buildUser(String email) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("hashed");
        u.setFirstName("Test");
        u.setLastName("User");
        u.setRole(Role.PATRON);
        u.setMembershipStatus(MembershipStatus.ACTIVE);
        u.setMemberSince(LocalDateTime.now());
        return u;
    }

    @Test
    void findByEmail_returnsUser_whenExists() {
        userRepository.save(buildUser("findme@example.com"));

        Optional<User> found = userRepository.findByEmail("findme@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("findme@example.com");
    }

    @Test
    void findByEmail_returnsEmpty_whenNotFound() {
        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void existsByEmail_reflectsPresence() {
        userRepository.save(buildUser("exists@example.com"));

        assertThat(userRepository.existsByEmail("exists@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("absent@example.com")).isFalse();
    }

    @Test
    void duplicateEmail_violatesUniqueConstraint() {
        userRepository.saveAndFlush(buildUser("dup@example.com"));

        assertThatThrownBy(() ->
                userRepository.saveAndFlush(buildUser("dup@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}