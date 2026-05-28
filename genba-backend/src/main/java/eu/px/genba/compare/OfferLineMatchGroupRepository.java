package eu.px.genba.compare;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OfferLineMatchGroupRepository extends JpaRepository<OfferLineMatchGroup, UUID> {

    List<OfferLineMatchGroup> findByProjectId(UUID projectId);

    @Query("SELECT g FROM OfferLineMatchGroup g WHERE g.id = :id AND g.projectId = :projectId")
    Optional<OfferLineMatchGroup> findByIdInProject(UUID id, UUID projectId);
}
