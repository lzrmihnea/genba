package eu.px.genba.offer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OfferLineRepository extends JpaRepository<OfferLine, UUID> {

    @Query("""
            SELECT l FROM OfferLine l
            WHERE l.offerId = :offerId
            ORDER BY l.lineOrder ASC
            """)
    List<OfferLine> findByOffer(UUID offerId);

    Optional<OfferLine> findByIdAndOfferId(UUID id, UUID offerId);

    /** Highest line_order already present on the offer; returns 0 when empty. */
    @Query("SELECT COALESCE(MAX(l.lineOrder), 0) FROM OfferLine l WHERE l.offerId = :offerId")
    int maxLineOrder(UUID offerId);

    void deleteByOfferId(UUID offerId);
}
