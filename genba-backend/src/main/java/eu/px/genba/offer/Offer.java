package eu.px.genba.offer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Structured bid from a Vendor for a Project. Totals are cached aggregates
 * recomputed by {@code OfferService} on every OfferLine mutation; never
 * write them from outside the service.
 *
 * <p>Lines are intentionally NOT mapped as a JPA child collection — managing
 * them via the line repository keeps recompute explicit and avoids LAZY-load
 * surprises in DTOs.
 */
@Entity
@Table(name = "offer")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "label", length = 255)
    private String label;

    @Column(name = "received_at")
    private LocalDate receivedAt;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "total_amount_excl_vat", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalAmountExclVat = BigDecimal.ZERO;

    @Column(name = "total_amount_incl_vat", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalAmountInclVat = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private OfferStatus status = OfferStatus.DRAFT;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
