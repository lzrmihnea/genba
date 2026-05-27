package eu.px.genba.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("""
            SELECT p FROM Project p
            WHERE p.orgId = :orgId AND p.deletedAt IS NULL
            ORDER BY p.createdAt DESC
            """)
    List<Project> findActiveByOrg(UUID orgId);

    @Query("""
            SELECT p FROM Project p
            WHERE p.id = :id AND p.orgId = :orgId AND p.deletedAt IS NULL
            """)
    Optional<Project> findActiveByIdInOrg(UUID id, UUID orgId);
}
