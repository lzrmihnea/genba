package eu.px.genba.compare;

import eu.px.genba.compare.MatchGroupLine.Key;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchGroupLineRepository extends JpaRepository<MatchGroupLine, Key> {

    List<MatchGroupLine> findByMatchGroupId(UUID matchGroupId);

    Optional<MatchGroupLine> findByOfferLineId(UUID offerLineId);

    long countByMatchGroupId(UUID matchGroupId);

    void deleteByMatchGroupId(UUID matchGroupId);

    void deleteByOfferLineId(UUID offerLineId);

    /** Memberships for every group in a project — used by the comparison query. */
    @Query("""
            SELECT mgl FROM MatchGroupLine mgl
            WHERE mgl.matchGroupId IN (
                SELECT g.id FROM OfferLineMatchGroup g WHERE g.projectId = :projectId
            )
            """)
    List<MatchGroupLine> findAllForProject(UUID projectId);
}
