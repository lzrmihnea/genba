package eu.px.genba.role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * System-managed role definition. Seeded via Liquibase
 * ({@code db.changelog-002-role.xml}); the application never creates rows.
 */
@Entity
@Table(name = "role")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, unique = true, length = 64)
    private RoleCode code;

    @Column(name = "name_en", nullable = false, length = 128)
    private String nameEn;

    @Column(name = "name_ro", nullable = false, length = 128)
    private String nameRo;

    @Column(name = "description")
    private String description;

    @Column(name = "system_managed", nullable = false)
    @Builder.Default
    private boolean systemManaged = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
