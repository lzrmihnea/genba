package eu.px.genba.compare;

import jakarta.validation.constraints.Size;

public record UpdateMatchGroupRequest(
        @Size(max = 512) String label,
        String notes) {
}
