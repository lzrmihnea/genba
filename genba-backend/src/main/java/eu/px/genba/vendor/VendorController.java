package eu.px.genba.vendor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendors", description = "Contractors / suppliers issuing offers; org-scoped or project-scoped")
public class VendorController {

    private final VendorService service;

    @GetMapping
    @Operation(summary = "List vendors visible to the active organization, optionally filtered to a project")
    public ResponseEntity<List<VendorDto>> list(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @RequestParam(value = "project_id", required = false) UUID projectId) {
        return ResponseEntity.ok(service.list(orgId, projectId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorDto> get(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.get(orgId, id));
    }

    @PostMapping
    public ResponseEntity<VendorDto> create(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @Valid @RequestBody CreateVendorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(orgId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VendorDto> update(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVendorRequest request) {
        return ResponseEntity.ok(service.update(orgId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID id) {
        service.softDelete(orgId, id);
        return ResponseEntity.noContent().build();
    }
}
