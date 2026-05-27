package eu.px.genba.attachment.channel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AttachmentSourceChannelRepository
        extends JpaRepository<AttachmentSourceChannel, UUID> {

    /**
     * All channels visible to an Organization: system-managed rows + the
     * org's own custom rows. Excludes soft-deleted entries.
     */
    @Query("""
            SELECT c FROM AttachmentSourceChannel c
            WHERE c.deletedAt IS NULL
              AND (c.orgId IS NULL OR c.orgId = :orgId)
            ORDER BY c.sortOrder ASC, c.code ASC
            """)
    List<AttachmentSourceChannel> findVisibleForOrg(UUID orgId);

    @Query("""
            SELECT c FROM AttachmentSourceChannel c
            WHERE c.id = :id AND c.deletedAt IS NULL
            """)
    Optional<AttachmentSourceChannel> findActiveById(UUID id);

    @Query("""
            SELECT COUNT(c) FROM AttachmentSourceChannel c
            WHERE c.orgId = :orgId AND c.deletedAt IS NULL
            """)
    long countCustomForOrg(UUID orgId);

    boolean existsByOrgIdAndCodeAndDeletedAtIsNull(UUID orgId, String code);
}
