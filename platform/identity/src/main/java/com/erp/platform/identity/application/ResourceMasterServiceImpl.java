package com.erp.platform.identity.application;

import com.erp.platform.identity.application.dto.CreateResourceMasterRequest;
import com.erp.platform.identity.application.dto.ResourceMasterListResponse;
import com.erp.platform.identity.application.dto.ResourceMasterResponse;
import com.erp.platform.identity.application.dto.UpdateResourceMasterRequest;
import com.erp.platform.identity.application.mapper.ResourceMasterMapper;
import com.erp.platform.identity.domain.ResourceMaster;
import com.erp.platform.identity.domain.ResourceStatus;
import com.erp.platform.identity.domain.exception.ResourceMasterNotFoundException;
import com.erp.platform.identity.infrastructure.persistence.ResourceMasterRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of ResourceMasterService.
 *
 * @since 1.0.0
 */
@Service
@Transactional
public class ResourceMasterServiceImpl implements ResourceMasterService {

    private final ResourceMasterRepository resourceMasterRepository;
    private final ResourceMasterMapper resourceMasterMapper;

    public ResourceMasterServiceImpl(ResourceMasterRepository resourceMasterRepository,
                                     ResourceMasterMapper resourceMasterMapper) {
        this.resourceMasterRepository = resourceMasterRepository;
        this.resourceMasterMapper = resourceMasterMapper;
    }

    @Override
    public ResourceMasterResponse createResource(CreateResourceMasterRequest request) {
        if (resourceMasterRepository.existsByResourceCode(request.resourceCode())) {
            throw new IllegalArgumentException(
                "Resource with code '" + request.resourceCode() + "' already exists"
            );
        }

        ResourceMaster resource = resourceMasterMapper.toEntity(request);
        ResourceMaster savedResource = resourceMasterRepository.save(resource);
        return resourceMasterMapper.toResponse(savedResource);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceMasterResponse getResourceById(Long id) {
        ResourceMaster resource = resourceMasterRepository.findById(id)
            .orElseThrow(() -> new ResourceMasterNotFoundException(
                "Resource not found with ID: " + id
            ));
        return resourceMasterMapper.toResponse(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceMasterResponse getResourceByCode(String resourceCode) {
        ResourceMaster resource = resourceMasterRepository.findByResourceCode(resourceCode)
            .orElseThrow(() -> new ResourceMasterNotFoundException(
                "Resource not found with code: " + resourceCode
            ));
        return resourceMasterMapper.toResponse(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResourceMasterListResponse> listResources(Pageable pageable) {
        return resourceMasterRepository.findAll(pageable).map(resourceMasterMapper::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceMasterListResponse> listActiveResources() {
        return resourceMasterRepository.findByStatus(ResourceStatus.ACTIVE)
            .stream()
            .map(resourceMasterMapper::toListResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceMasterListResponse> listActiveResourcesByName() {
        return resourceMasterRepository.findByStatusOrderByResourceName(ResourceStatus.ACTIVE)
            .stream()
            .map(resourceMasterMapper::toListResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceMasterListResponse> listActiveResourcesByModule(String module) {
        return resourceMasterRepository.findByModuleAndStatusOrderByResourceName(module, ResourceStatus.ACTIVE)
            .stream()
            .map(resourceMasterMapper::toListResponse)
            .toList();
    }

    @Override
    public ResourceMasterResponse updateResource(Long id, UpdateResourceMasterRequest request) {
        ResourceMaster resource = resourceMasterRepository.findById(id)
            .orElseThrow(() -> new ResourceMasterNotFoundException(
                "Resource not found with ID: " + id
            ));

        if (request.resourceCode() != null
                && !request.resourceCode().equalsIgnoreCase(resource.getResourceCode())
                && resourceMasterRepository.existsByResourceCode(request.resourceCode())) {
            throw new IllegalArgumentException(
                "Resource with code '" + request.resourceCode() + "' already exists"
            );
        }

        ResourceMaster updated = resourceMasterMapper.applyUpdate(resource, request);
        ResourceMaster savedResource = resourceMasterRepository.save(updated);
        return resourceMasterMapper.toResponse(savedResource);
    }

    @Override
    public ResourceMasterResponse activateResource(Long id) {
        ResourceMaster resource = resourceMasterRepository.findById(id)
            .orElseThrow(() -> new ResourceMasterNotFoundException(
                "Resource not found with ID: " + id
            ));
        resource.activate();
        ResourceMaster savedResource = resourceMasterRepository.save(resource);
        return resourceMasterMapper.toResponse(savedResource);
    }

    @Override
    public ResourceMasterResponse deactivateResource(Long id) {
        ResourceMaster resource = resourceMasterRepository.findById(id)
            .orElseThrow(() -> new ResourceMasterNotFoundException(
                "Resource not found with ID: " + id
            ));
        resource.deactivate();
        ResourceMaster savedResource = resourceMasterRepository.save(resource);
        return resourceMasterMapper.toResponse(savedResource);
    }

    @Override
    public void deleteResource(Long id) {
        ResourceMaster resource = resourceMasterRepository.findById(id)
            .orElseThrow(() -> new ResourceMasterNotFoundException(
                "Resource not found with ID: " + id
            ));
        resourceMasterRepository.delete(resource);
    }
}
