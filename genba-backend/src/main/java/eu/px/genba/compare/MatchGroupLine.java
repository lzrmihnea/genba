package eu.px.genba.compare;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Join row: which OfferLines belong to a MatchGroup. */
@Entity
@Table(name = "match_group_line")
@IdClass(MatchGroupLine.Key.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchGroupLine {

    @Id
    @Column(name = "match_group_id", nullable = false)
    private UUID matchGroupId;

    @Id
    @Column(name = "offer_line_id", nullable = false)
    private UUID offerLineId;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        private UUID matchGroupId;
        private UUID offerLineId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(matchGroupId, key.matchGroupId)
                    && Objects.equals(offerLineId, key.offerLineId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(matchGroupId, offerLineId);
        }
    }
}
