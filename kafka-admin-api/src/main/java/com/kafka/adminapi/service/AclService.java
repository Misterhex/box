package com.kafka.adminapi.service;

import com.kafka.adminapi.model.AclBindingInfo;
import com.kafka.adminapi.model.CreateAclRequest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.acl.AclPermissionType;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.apache.kafka.common.security.auth.KafkaPrincipal;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AclService {

    private final AdminClient adminClient;

    public AclService(AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    public List<AclBindingInfo> listAcls() {
        try {
            var aclBindings = adminClient.describeAcls(AclBindingFilter.ANY)
                    .values()
                    .get(30, TimeUnit.SECONDS);

            return aclBindings.stream()
                    .map(this::toAclBindingInfo)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to list ACLs", e);
        }
    }

    public void createAcl(CreateAclRequest request) {
        try {
            var resourcePattern = new ResourcePattern(
                    ResourceType.fromString(request.resourceType()),
                    request.resourceName(),
                    PatternType.LITERAL
            );

            var entry = new org.apache.kafka.common.acl.AccessControlEntry(
                    request.principal(),
                    "*",
                    AclOperation.fromString(request.operation()),
                    AclPermissionType.fromString(request.permission())
            );

            var aclBinding = new AclBinding(resourcePattern, entry);

            adminClient.createAcls(Collections.singleton(aclBinding))
                    .all()
                    .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ACL", e);
        }
    }

    public void deleteAcls(String resourceType, String resourceName, String principal, String operation) {
        try {
            var resourcePattern = new ResourcePattern(
                    resourceType != null ? ResourceType.fromString(resourceType) : ResourceType.ANY,
                    resourceName != null ? resourceName : null,
                    PatternType.ANY
            );

            var entry = new org.apache.kafka.common.acl.AccessControlEntry(
                    principal != null ? principal : null,
                    "*",
                    operation != null ? AclOperation.fromString(operation) : AclOperation.ANY,
                    AclPermissionType.ANY
            );

            var filter = new AclBindingFilter(
                    new org.apache.kafka.common.resource.ResourcePatternFilter(
                            resourcePattern.resourceType(),
                            resourcePattern.name(),
                            PatternType.ANY
                    ),
                    new org.apache.kafka.common.acl.AccessControlEntryFilter(
                            entry.principal(),
                            entry.host(),
                            entry.operation(),
                            entry.permissionType()
                    )
            );

            adminClient.deleteAcls(Collections.singleton(filter))
                    .all()
                    .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete ACLs", e);
        }
    }

    private AclBindingInfo toAclBindingInfo(AclBinding binding) {
        return new AclBindingInfo(
                binding.entry().principal(),
                binding.pattern().resourceType().toString(),
                binding.pattern().name(),
                binding.entry().operation().toString(),
                binding.entry().permissionType().toString()
        );
    }
}
