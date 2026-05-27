package eu.px.genba.attachment.channel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Reference data for the {@code source_channel} dimension of an
 * {@link eu.px.genba.attachment.Attachment}. Two flavours:
 *
 * <ul>
 *   <li><b>System-managed</b> rows: {@code orgId IS NULL},
 *       {@code systemManaged = true}, immutable. Seeded by Liquibase.</li>
 *   <li><b>Org-managed</b> custom rows: {@code orgId} set, fully owned by
 *       the Organization's admins. Capped at 32 per org by the service.</li>
 * </ul>
 *
 * <p>Bilingual labels (EN/RO) live here so the channel picker can render in
 * the user's preferred locale without a join to a translation table.
 */
@Entity
@Table(name = "attachment_source_channel")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentSourceChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** NULL means a system-managed global row visible to every Organization. */
    @Column(name = "org_id")
    private UUID orgId;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "label_en", nullable = false, length = 64)
    private String labelEn;

    @Column(name = "label_ro", nullable = false, length = 64)
    private String labelRo;

    @Column(name = "system_managed", nullable = false)
    @Builder.Default
    private boolean systemManaged = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 100;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
