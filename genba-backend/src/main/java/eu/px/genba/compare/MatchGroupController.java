package eu.px.genba.compare;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Match groups", description = "User-declared equivalence of OfferLines across offers")
public class MatchGroupController {

    private final MatchGroupService service;

    @PostMapping("/api/projects/{projectId}/match-groups")
    public ResponseEntity<MatchGroupDto> create(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateMatchGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(orgId, projectId, request));
    }

    @PostMapping("/api/projects/{projectId}/match-groups/{groupId}/lines/{lineId}")
    public ResponseEntity<MatchGroupDto> addLine(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID projectId,
            @PathVariable UUID groupId,
            @PathVariable UUID lineId) {
        return ResponseEntity.ok(service.addLine(orgId, projectId, groupId, lineId));
    }

    @DeleteMapping("/api/projects/{projectId}/match-groups/{groupId}/lines/{lineId}")
    public ResponseEntity<Void> removeLine(
            @PathVariable UUID projectId,
            @PathVariable UUID groupId,
            @PathVariable UUID lineId) {
        service.removeLine(projectId, groupId, lineId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/projects/{projectId}/match-groups/{groupId}")
    public ResponseEntity<MatchGroupDto> update(
            @PathVariable UUID projectId,
            @PathVariable UUID groupId,
            @Valid @RequestBody UpdateMatchGroupRequest request) {
        return ResponseEntity.ok(service.update(projectId, groupId, request));
    }

    @DeleteMapping("/api/projects/{projectId}/match-groups/{groupId}")
    public ResponseEntity<Void> dissolve(
            @PathVariable UUID projectId,
            @PathVariable UUID groupId) {
        service.dissolve(projectId, groupId);
        return ResponseEntity.noContent().build();
    }
}
