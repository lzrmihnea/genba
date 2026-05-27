package eu.px.genba.project;

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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "One house build per Project, scoped to the active Organization")
public class ProjectController {

    private final ProjectService service;

    @GetMapping
    @Operation(summary = "List all active projects within the active organization")
    public ResponseEntity<List<ProjectDto>> list(
            @RequestHeader("X-Genba-Org-Id") UUID orgId) {
        return ResponseEntity.ok(service.listForOrg(orgId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a single project")
    public ResponseEntity<ProjectDto> get(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.get(orgId, id));
    }

    @PostMapping
    @Operation(summary = "Create a new project; base_currency defaults to the Organization's locale default")
    public ResponseEntity<ProjectDto> create(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(orgId, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Patch project fields and/or status")
    public ResponseEntity<ProjectDto> update(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request) {
        return ResponseEntity.ok(service.update(orgId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete the project; child Offers and Attachments are retained")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID id) {
        service.softDelete(orgId, id);
        return ResponseEntity.noContent().build();
    }
}
