package eu.px.genba.attachment;

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
 * Polymorphic attachment — any domain entity can carry zero-or-more.
 *
 * <p>NOT mapped here: {@code embedding} (VECTOR(1024)) and
 * {@code embedding_source_hash}. Those columns exist in the schema (see the
 * Liquibase changeset) but the Phase-5 {@code add-semantic-search} change
 * owns their JPA mapping. Leaving them off keeps the entity narrow and lets
 * {@code ddl-auto: validate} pass without pulling the pgvector type
 * dependency into the core data layer.
 */
@Entity
@Table(name = "attachment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "attachable_type", nullable = false, length = 64)
    private String attachableType;

    @Column(name = "attachable_id", nullable = false)
    private UUID attachableId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private AttachmentKind kind;

    @Column(name = "source_channel_id", nullable = false)
    private UUID sourceChannelId;

    @Column(name = "file_url", length = 1024)
    private String fileUrl;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "original_filename", length = 512)
    private String originalFilename;

    @Column(name = "mime_type", length = 128)
    private String mimeType;

    @Column(name = "byte_size")
    private Long byteSize;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;
}
