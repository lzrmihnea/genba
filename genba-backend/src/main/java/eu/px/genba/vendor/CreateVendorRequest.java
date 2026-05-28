package eu.px.genba.vendor;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateVendorRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String contactName,
        @Size(max = 64) String phone,
        @Email @Size(max = 255) String email,
        @Size(max = 64) String vatId,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal defaultRetentionPct,
        String notes,
        UUID projectId) {
}
