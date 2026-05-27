package eu.px.genba.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        @Size(max = 512) String address,
        @Pattern(regexp = "^[A-Z]{3}$", message = "ISO 4217 3-letter currency code") String baseCurrency) {
}
