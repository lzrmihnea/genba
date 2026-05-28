package eu.px.genba.vendor;

import eu.px.genba.common.exception.NotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository repository;
    private final VendorMapper mapper;

    @Transactional(readOnly = true)
    public List<VendorDto> list(UUID orgId, UUID projectId) {
        List<Vendor> rows = projectId != null
                ? repository.findVisibleForProject(orgId, projectId)
                : repository.findOrgScoped(orgId);
        return rows.stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public VendorDto get(UUID orgId, UUID id) {
        return repository.findActiveByIdInOrg(id, orgId)
                .map(mapper::toDto)
                .orElseThrow(NotFoundException::new);
    }

    @Transactional
    public VendorDto create(UUID orgId, CreateVendorRequest request) {
        Vendor entity = Vendor.builder()
                .orgId(orgId)
                .projectId(request.projectId())
                .name(request.name())
                .contactName(request.contactName())
                .phone(request.phone())
                .email(request.email())
                .vatId(request.vatId())
                .defaultRetentionPct(request.defaultRetentionPct())
                .notes(request.notes())
                .build();
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public VendorDto update(UUID orgId, UUID id, UpdateVendorRequest request) {
        Vendor vendor = repository.findActiveByIdInOrg(id, orgId)
                .orElseThrow(NotFoundException::new);
        if (request.name() != null) vendor.setName(request.name());
        if (request.contactName() != null) vendor.setContactName(request.contactName());
        if (request.phone() != null) vendor.setPhone(request.phone());
        if (request.email() != null) vendor.setEmail(request.email());
        if (request.vatId() != null) vendor.setVatId(request.vatId());
        if (request.defaultRetentionPct() != null) vendor.setDefaultRetentionPct(request.defaultRetentionPct());
        if (request.notes() != null) vendor.setNotes(request.notes());
        return mapper.toDto(repository.save(vendor));
    }

    @Transactional
    public void softDelete(UUID orgId, UUID id) {
        Vendor vendor = repository.findActiveByIdInOrg(id, orgId)
                .orElseThrow(NotFoundException::new);
        vendor.setDeletedAt(OffsetDateTime.now());
        repository.save(vendor);
    }

    /** Used by OfferService when validating offer creation. */
    @Transactional(readOnly = true)
    public Vendor requireActive(UUID orgId, UUID vendorId) {
        return repository.findActiveByIdInOrg(vendorId, orgId)
                .orElseThrow(NotFoundException::new);
    }
}
