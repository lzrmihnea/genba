package eu.px.genba.compare;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Records that, while editing {@code currentOfferId}, the user dismissed the
 * suggestion to add {@code sourceLineId} (a line from another offer). Keeps
 * dismissed recommendations from reappearing. UNIQUE(current_offer_id,
 * source_line_id).
 */
@Entity
@Table(name = "dismissed_recommendation")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DismissedRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "current_offer_id", nullable = false)
    private UUID currentOfferId;

    @Column(name = "source_line_id", nullable = false)
    private UUID sourceLineId;

    @CreatedBy
    @Column(name = "dismissed_by", updatable = false)
    private UUID dismissedBy;

    @CreatedDate
    @Column(name = "dismissed_at", nullable = false, updatable = false)
    private OffsetDateTime dismissedAt;
}
