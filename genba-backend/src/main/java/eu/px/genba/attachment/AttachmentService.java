package eu.px.genba.attachment;

import eu.px.genba.attachment.channel.AttachmentSourceChannel;
import eu.px.genba.attachment.channel.AttachmentSourceChannelRepository;
import eu.px.genba.common.exception.GenbaException;
import eu.px.genba.common.exception.NotFoundException;
import eu.px.genba.security.CurrentUser;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository repository;
    private final AttachmentSourceChannelRepository channelRepository;
    private final AttachmentMapper mapper;
    private final AttachmentStorageService storage;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<AttachmentDto> listFor(UUID orgId, String attachableType, UUID attachableId) {
        return repository.findByAttachable(orgId, attachableType, attachableId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public AttachmentDto createFile(
            UUID orgId,
            String attachableType,
            UUID attachableId,
            UUID sourceChannelId,
            MultipartFile file) {

        AttachmentSourceChannel channel = resolveChannel(orgId, sourceChannelId);
        AttachmentStorageService.StoredFile stored = storage.store(orgId, file);

        Attachment entity = Attachment.builder()
                .orgId(orgId)
                .attachableType(attachableType)
                .attachableId(attachableId)
                .kind(AttachmentKind.FILE)
                .sourceChannelId(channel.getId())
                .fileUrl(stored.relativePath())
                .originalFilename(stored.originalFilename())
                .mimeType(stored.mimeType())
                .byteSize(stored.byteSize())
                .uploadedBy(currentUser.getUserId().orElse(null))
                .uploadedAt(OffsetDateTime.now())
                .build();

        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public AttachmentDto createText(
            UUID orgId,
            String attachableType,
            UUID attachableId,
            UUID sourceChannelId,
            String rawText) {

        if (rawText == null || rawText.isBlank()) {
            throw new EmptyTextException();
        }
        AttachmentSourceChannel channel = resolveChannel(orgId, sourceChannelId);

        Attachment entity = Attachment.builder()
                .orgId(orgId)
                .attachableType(attachableType)
                .attachableId(attachableId)
                .kind(AttachmentKind.TEXT)
                .sourceChannelId(channel.getId())
                .rawText(rawText)
                .uploadedBy(currentUser.getUserId().orElse(null))
                .uploadedAt(OffsetDateTime.now())
                .build();

        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public AttachmentDto createLink(
            UUID orgId,
            String attachableType,
            UUID attachableId,
            UUID sourceChannelId,
            String url) {

        if (url == null || url.isBlank()) {
            throw new EmptyLinkException();
        }
        AttachmentSourceChannel channel = resolveChannel(orgId, sourceChannelId);

        Attachment entity = Attachment.builder()
                .orgId(orgId)
                .attachableType(attachableType)
                .attachableId(attachableId)
                .kind(AttachmentKind.LINK)
                .sourceChannelId(channel.getId())
                .fileUrl(url)
                .uploadedBy(currentUser.getUserId().orElse(null))
                .uploadedAt(OffsetDateTime.now())
                .build();

        return mapper.toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public Attachment requireForDownload(UUID orgId, UUID id) {
        Attachment attachment = repository.findByIdInOrg(id, orgId)
                .orElseThrow(NotFoundException::new);
        if (attachment.getKind() != AttachmentKind.FILE) {
            throw new NotDownloadableException();
        }
        return attachment;
    }

    @Transactional
    public void delete(UUID orgId, UUID id) {
        Attachment attachment = repository.findByIdInOrg(id, orgId)
                .orElseThrow(NotFoundException::new);
        if (attachment.getKind() == AttachmentKind.FILE && attachment.getFileUrl() != null) {
            storage.delete(attachment.getFileUrl());
        }
        repository.delete(attachment);
    }

    private AttachmentSourceChannel resolveChannel(UUID orgId, UUID sourceChannelId) {
        AttachmentSourceChannel channel = channelRepository.findActiveById(sourceChannelId)
                .orElseThrow(NotFoundException::new);
        // System channels (orgId == null) are visible to every org; custom channels must match.
        if (channel.getOrgId() != null && !channel.getOrgId().equals(orgId)) {
            throw new NotFoundException();
        }
        return channel;
    }

    public static final class EmptyTextException extends GenbaException {
        public EmptyTextException() {
            super("attachment.error.emptyText", "EMPTY_TEXT");
        }
    }

    public static final class EmptyLinkException extends GenbaException {
        public EmptyLinkException() {
            super("attachment.error.emptyLink", "EMPTY_LINK");
        }
    }

    public static final class NotDownloadableException extends GenbaException {
        public NotDownloadableException() {
            super("attachment.error.notDownloadable", "NOT_DOWNLOADABLE");
        }
    }
}
