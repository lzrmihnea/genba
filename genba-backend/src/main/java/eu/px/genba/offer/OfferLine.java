package eu.px.genba.offer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * One line item on an {@link Offer}. {@code lineTotalExclVat} and
 * {@code lineTotalInclVat} are recomputed in the service from
 * {@code qty * unitPrice} with {@code vatRate}; never trust client-supplied
 * values.
 *
 * <p>{@code wbsItemId} is populated by Phase 2 ({@code add-wbs-catalog}).
 * {@code normalizedKey} is the Phase 1 matching primitive used by
 * {@code add-bid-comparison-mvp}.
 *
 * <p>{@code embedding} + {@code embedding_source_hash} exist in the schema
 * for Phase 5 but are not mapped here.
 */
@Entity
@Table(name = "offer_line")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Column(name = "line_order", nullable = false)
    private int lineOrder;

    @Column(name = "label", nullable = false, length = 512)
    private String label;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "qty", nullable = false, precision = 14, scale = 4)
    private BigDecimal qty;

    @Column(name = "unit", nullable = false, length = 32)
    @Builder.Default
    private String unit = "buc";

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal vatRate = new BigDecimal("19.00");

    @Column(name = "line_total_excl_vat", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal lineTotalExclVat = BigDecimal.ZERO;

    @Column(name = "line_total_incl_vat", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal lineTotalInclVat = BigDecimal.ZERO;

    @Column(name = "wbs_item_id")
    private UUID wbsItemId;

    @Column(name = "normalized_key", length = 255)
    private String normalizedKey;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
