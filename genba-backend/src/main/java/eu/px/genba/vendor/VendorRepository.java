package eu.px.genba.vendor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    /**
     * Vendors visible to an Org: every org-scoped vendor (project_id IS NULL)
     * plus any vendors scoped to the requested project. Project-scoped vendors
     * from OTHER projects are excluded so a busy org doesn't bury its pickers.
     */
    @Query("""
            SELECT v FROM Vendor v
            WHERE v.orgId = :orgId AND v.deletedAt IS NULL
              AND (v.projectId IS NULL OR v.projectId = :projectId)
            ORDER BY LOWER(v.name) ASC
            """)
    List<Vendor> findVisibleForProject(UUID orgId, UUID projectId);

    @Query("""
            SELECT v FROM Vendor v
            WHERE v.orgId = :orgId AND v.deletedAt IS NULL
              AND v.projectId IS NULL
            ORDER BY LOWER(v.name) ASC
            """)
    List<Vendor> findOrgScoped(UUID orgId);

    @Query("""
            SELECT v FROM Vendor v
            WHERE v.id = :id AND v.orgId = :orgId AND v.deletedAt IS NULL
            """)
    Optional<Vendor> findActiveByIdInOrg(UUID id, UUID orgId);
}
