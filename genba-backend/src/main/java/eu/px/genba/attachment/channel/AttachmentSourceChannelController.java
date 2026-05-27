package eu.px.genba.attachment.channel;

import eu.px.genba.security.CurrentUser;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attachment-channels")
@RequiredArgsConstructor
@Tag(name = "Attachment channels", description = "System + org-custom source channels for polymorphic attachments")
public class AttachmentSourceChannelController {

    private final AttachmentSourceChannelService service;

    @GetMapping
    @Operation(summary = "List source channels visible to the active organization (system + custom)")
    public ResponseEntity<List<AttachmentSourceChannelDto>> list(
            @RequestHeader(name = "X-Genba-Org-Id") UUID orgId) {
        return ResponseEntity.ok(service.listForActiveOrg(orgId));
    }

    @PostMapping
    @Operation(summary = "Create a custom source channel scoped to the active organization (admin only)")
    public ResponseEntity<AttachmentSourceChannelDto> create(
            @RequestHeader(name = "X-Genba-Org-Id") UUID orgId,
            @Valid @RequestBody CreateAttachmentSourceChannelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createCustom(orgId, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a custom source channel (admin only; system rows are immutable)")
    public ResponseEntity<Void> delete(
            @RequestHeader(name = "X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID id) {
        service.softDelete(orgId, id);
        return ResponseEntity.noContent().build();
    }
}
