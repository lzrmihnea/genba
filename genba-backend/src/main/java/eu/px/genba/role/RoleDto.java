package eu.px.genba.role;

import java.util.UUID;
import lombok.Builder;

@Builder
public record RoleDto(
        UUID id,
        RoleCode code,
        String nameEn,
        String nameRo,
        String description) {
}
