package eu.px.genba.attachment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    @Query("""
            SELECT a FROM Attachment a
            WHERE a.orgId = :orgId
              AND a.attachableType = :type
              AND a.attachableId = :attachableId
            ORDER BY a.uploadedAt DESC
            """)
    List<Attachment> findByAttachable(UUID orgId, String type, UUID attachableId);

    @Query("SELECT a FROM Attachment a WHERE a.id = :id AND a.orgId = :orgId")
    Optional<Attachment> findByIdInOrg(UUID id, UUID orgId);
}
