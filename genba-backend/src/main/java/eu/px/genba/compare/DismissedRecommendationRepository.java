package eu.px.genba.compare;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DismissedRecommendationRepository extends JpaRepository<DismissedRecommendation, UUID> {

    List<DismissedRecommendation> findByCurrentOfferId(UUID currentOfferId);

    boolean existsByCurrentOfferIdAndSourceLineId(UUID currentOfferId, UUID sourceLineId);

    void deleteByCurrentOfferIdAndSourceLineId(UUID currentOfferId, UUID sourceLineId);
}
