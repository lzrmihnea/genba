package eu.px.genba.auth;

import eu.px.genba.organization.OrgRole;
import eu.px.genba.organization.Organization;
import eu.px.genba.organization.OrganizationRepository;
import eu.px.genba.organization.UserOrganization;
import eu.px.genba.organization.UserOrganizationRepository;
import eu.px.genba.user.User;
import eu.px.genba.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * First-boot admin-user seed.
 *
 * <p>Reads {@code seed.admin-email} and {@code seed.admin-password} from
 * configuration; if either is blank the runner skips silently so non-dev
 * environments that drive seeding through other channels are not surprised
 * with a bogus user.
 *
 * <p>Idempotent: re-runs find the existing Organization, User, and
 * UserOrganization rows and do nothing. The Organization itself is seeded by
 * Liquibase ({@code db.changelog-005-seed.xml}); this runner just attaches
 * the admin User to it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeedAdminUserRunner implements CommandLineRunner {

    @Value("${seed.admin-email:}")
    private String adminEmail;

    @Value("${seed.admin-password:}")
    private String adminPassword;

    @Value("${seed.organization-name:Mihnea's Builds}")
    private String organizationName;

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("Admin seed skipped — GENBA_SEED_EMAIL or GENBA_SEED_PASSWORD is unset.");
            return;
        }

        Organization org = organizationRepository.findActiveByName(organizationName)
                .orElseGet(() -> {
                    log.info("Admin seed: creating Organization '{}'", organizationName);
                    return organizationRepository.save(
                            Organization.builder().name(organizationName).build());
                });

        User user = userRepository.findActiveByEmail(adminEmail)
                .orElseGet(() -> {
                    log.info("Admin seed: creating User '{}'", adminEmail);
                    return userRepository.save(User.builder()
                            .email(adminEmail)
                            .passwordHash(passwordEncoder.encode(adminPassword))
                            .displayName("Owner")
                            .preferredLocale("en")
                            .active(true)
                            .build());
                });

        boolean membershipExists = userOrganizationRepository
                .existsByUserIdAndOrganizationId(user.getId(), org.getId());
        if (!membershipExists) {
            log.info("Admin seed: linking User {} to Org {} as OWNER", user.getEmail(), org.getName());
            userOrganizationRepository.save(UserOrganization.builder()
                    .user(user)
                    .organization(org)
                    .orgRole(OrgRole.OWNER)
                    .build());
        }

        log.info("Admin seed ready: user={}, org={}", user.getEmail(), org.getName());
    }
}
