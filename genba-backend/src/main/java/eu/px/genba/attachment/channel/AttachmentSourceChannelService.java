package eu.px.genba.attachment.channel;

import eu.px.genba.common.exception.GenbaException;
import eu.px.genba.common.exception.NotFoundException;
import eu.px.genba.security.CurrentUser;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttachmentSourceChannelService {

    /**
     * Hard cap on custom (non-system) channels per Organization. Keeps the
     * channel picker UI bounded; an org that needs more is likely modelling
     * something that should be a different entity (e.g. Document.kind).
     */
    public static final int MAX_CUSTOM_PER_ORG = 32;

    private final AttachmentSourceChannelRepository repository;
    private final AttachmentSourceChannelMapper mapper;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<AttachmentSourceChannelDto> listForActiveOrg(UUID orgId) {
        return repository.findVisibleForOrg(orgId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public AttachmentSourceChannelDto createCustom(UUID orgId, CreateAttachmentSourceChannelRequest request) {
        if (repository.existsByOrgIdAndCodeAndDeletedAtIsNull(orgId, request.code())) {
            throw new ChannelCodeAlreadyExistsException();
        }
        if (repository.countCustomForOrg(orgId) >= MAX_CUSTOM_PER_ORG) {
            throw new ChannelCapExceededException();
        }
        AttachmentSourceChannel entity = AttachmentSourceChannel.builder()
                .orgId(orgId)
                .code(request.code())
                .labelEn(request.labelEn())
                .labelRo(request.labelRo())
                .systemManaged(false)
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 100)
                .build();
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public void softDelete(UUID orgId, UUID channelId) {
        AttachmentSourceChannel channel = repository.findActiveById(channelId)
                .orElseThrow(NotFoundException::new);
        if (channel.isSystemManaged() || channel.getOrgId() == null) {
            throw new SystemChannelImmutableException();
        }
        if (!orgId.equals(channel.getOrgId())) {
            // Treat cross-org as a NotFoundException to avoid leaking existence.
            throw new NotFoundException();
        }
        channel.setDeletedAt(OffsetDateTime.now());
        repository.save(channel);
    }

    public static final class ChannelCodeAlreadyExistsException extends GenbaException {
        public ChannelCodeAlreadyExistsException() {
            super("attachment.error.channelCodeExists", "CHANNEL_CODE_EXISTS");
        }
    }

    public static final class ChannelCapExceededException extends GenbaException {
        public ChannelCapExceededException() {
            super("attachment.error.channelCapExceeded", "CHANNEL_CAP_EXCEEDED");
        }
    }

    public static final class SystemChannelImmutableException extends GenbaException {
        public SystemChannelImmutableException() {
            super("attachment.error.systemChannelImmutable", "SYSTEM_CHANNEL_IMMUTABLE");
        }
    }
}
