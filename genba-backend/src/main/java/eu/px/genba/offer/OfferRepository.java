package eu.px.genba.offer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OfferRepository extends JpaRepository<Offer, UUID> {

    @Query("""
            SELECT o FROM Offer o
            WHERE o.orgId = :orgId
              AND o.projectId = :projectId
              AND o.deletedAt IS NULL
              AND (:status IS NULL OR o.status = :status)
            ORDER BY o.receivedAt DESC NULLS LAST, o.createdAt DESC
            """)
    List<Offer> findActiveForProject(UUID orgId, UUID projectId, OfferStatus status);

    @Query("""
            SELECT o FROM Offer o
            WHERE o.id = :id AND o.orgId = :orgId AND o.deletedAt IS NULL
            """)
    Optional<Offer> findActiveByIdInOrg(UUID id, UUID orgId);

    /** Default comparison selection: non-deleted offers in the project with a comparable status. */
    @Query("""
            SELECT o FROM Offer o
            WHERE o.orgId = :orgId AND o.projectId = :projectId AND o.deletedAt IS NULL
              AND o.status IN :statuses
            ORDER BY o.receivedAt DESC NULLS LAST, o.createdAt DESC
            """)
    List<Offer> findComparable(UUID orgId, UUID projectId, List<OfferStatus> statuses);

    /** Explicit comparison selection: only the requested offer ids, still org+project+active scoped. */
    @Query("""
            SELECT o FROM Offer o
            WHERE o.orgId = :orgId AND o.projectId = :projectId AND o.deletedAt IS NULL
              AND o.id IN :ids
            ORDER BY o.receivedAt DESC NULLS LAST, o.createdAt DESC
            """)
    List<Offer> findSelected(UUID orgId, UUID projectId, List<UUID> ids);
}
