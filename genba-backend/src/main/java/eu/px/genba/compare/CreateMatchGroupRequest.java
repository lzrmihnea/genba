package eu.px.genba.compare;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateMatchGroupRequest(
        @NotNull @Size(min = 2, message = "a match group needs at least two lines") List<UUID> lines,
        String label) {
}
