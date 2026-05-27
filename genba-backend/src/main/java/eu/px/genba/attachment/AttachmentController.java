package eu.px.genba.attachment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "Polymorphic file / text / link evidence for any domain entity")
public class AttachmentController {

    private final AttachmentService service;
    private final AttachmentStorageService storage;

    @GetMapping
    @Operation(summary = "List attachments for a (type, id) pair within the active organization")
    public ResponseEntity<List<AttachmentDto>> list(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @RequestParam("type") String attachableType,
            @RequestParam("id") UUID attachableId) {
        return ResponseEntity.ok(service.listFor(orgId, attachableType, attachableId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file as an attachment")
    public ResponseEntity<AttachmentDto> uploadFile(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @RequestParam("attachableType") String attachableType,
            @RequestParam("attachableId") UUID attachableId,
            @RequestParam("sourceChannelId") UUID sourceChannelId,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createFile(orgId, attachableType, attachableId, sourceChannelId, file));
    }

    @PostMapping(path = "/text", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Attach pasted text (e.g. WhatsApp / email body) — no file upload")
    public ResponseEntity<AttachmentDto> createText(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @RequestBody CreateTextAttachmentRequest request) {
        AttachmentDto dto = service.createText(
                orgId,
                request.attachableType(),
                request.attachableId(),
                request.sourceChannelId(),
                request.rawText());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping(path = "/link", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Attach an external link (e.g. Google Drive URL)")
    public ResponseEntity<AttachmentDto> createLink(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @RequestBody CreateLinkAttachmentRequest request) {
        AttachmentDto dto = service.createLink(
                orgId,
                request.attachableType(),
                request.attachableId(),
                request.sourceChannelId(),
                request.url());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Stream the file backing a FILE-kind attachment")
    public ResponseEntity<Resource> download(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID id) {
        Attachment attachment = service.requireForDownload(orgId, id);
        Resource resource = storage.load(attachment.getFileUrl());
        String filename = attachment.getOriginalFilename() != null
                ? attachment.getOriginalFilename()
                : "attachment-" + attachment.getId();
        String contentType = attachment.getMimeType() != null
                ? attachment.getMimeType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID id) {
        service.delete(orgId, id);
        return ResponseEntity.noContent().build();
    }
}
