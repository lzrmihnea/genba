package eu.px.genba.project;

import eu.px.genba.common.exception.NotFoundException;
import eu.px.genba.organization.Organization;
import eu.px.genba.organization.OrganizationRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository repository;
    private final OrganizationRepository organizationRepository;
    private final ProjectMapper mapper;

    @Transactional(readOnly = true)
    public List<ProjectDto> listForOrg(UUID orgId) {
        return repository.findActiveByOrg(orgId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDto get(UUID orgId, UUID projectId) {
        return repository.findActiveByIdInOrg(projectId, orgId)
                .map(mapper::toDto)
                .orElseThrow(NotFoundException::new);
    }

    @Transactional
    public ProjectDto create(UUID orgId, CreateProjectRequest request) {
        String baseCurrency = request.baseCurrency();
        if (baseCurrency == null || baseCurrency.isBlank()) {
            // Inherit the Organization's default currency when caller omits it.
            Organization org = organizationRepository.findActiveById(orgId)
                    .orElseThrow(NotFoundException::new);
            baseCurrency = org.getCurrencyCode();
        }
        Project entity = Project.builder()
                .orgId(orgId)
                .name(request.name())
                .description(request.description())
                .address(request.address())
                .baseCurrency(baseCurrency)
                .status(ProjectStatus.PLANNING)
                .build();
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public ProjectDto update(UUID orgId, UUID projectId, UpdateProjectRequest request) {
        Project entity = repository.findActiveByIdInOrg(projectId, orgId)
                .orElseThrow(NotFoundException::new);
        if (request.name() != null) entity.setName(request.name());
        if (request.description() != null) entity.setDescription(request.description());
        if (request.address() != null) entity.setAddress(request.address());
        if (request.baseCurrency() != null) entity.setBaseCurrency(request.baseCurrency());
        if (request.status() != null) entity.setStatus(request.status());
        return mapper.toDto(repository.save(entity));
    }

    @Transactional
    public void softDelete(UUID orgId, UUID projectId) {
        Project entity = repository.findActiveByIdInOrg(projectId, orgId)
                .orElseThrow(NotFoundException::new);
        entity.setDeletedAt(OffsetDateTime.now());
        repository.save(entity);
    }
}
