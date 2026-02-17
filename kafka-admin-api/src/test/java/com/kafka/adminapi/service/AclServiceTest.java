package com.kafka.adminapi.service;

import com.kafka.adminapi.model.AclBindingInfo;
import com.kafka.adminapi.model.CreateAclRequest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateAclsResult;
import org.apache.kafka.clients.admin.DeleteAclsResult;
import org.apache.kafka.clients.admin.DescribeAclsResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AclServiceTest {

    @Mock
    private AdminClient adminClient;

    @Mock
    private DescribeAclsResult describeAclsResult;

    @Mock
    private CreateAclsResult createAclsResult;

    @Mock
    private DeleteAclsResult deleteAclsResult;

    private AclService aclService;

    @BeforeEach
    void setUp() {
        aclService = new AclService(adminClient);
    }

    @Test
    void listAcls_shouldReturnAclList() throws Exception {
        // Given
        ResourcePattern resourcePattern = new ResourcePattern(ResourceType.TOPIC, "test-topic", PatternType.LITERAL);
        AccessControlEntry entry = new AccessControlEntry("User:testuser", "*", AclOperation.READ, AclPermissionType.ALLOW);
        AclBinding aclBinding = new AclBinding(resourcePattern, entry);

        Collection<AclBinding> aclBindings = Collections.singleton(aclBinding);
        KafkaFuture<Collection<AclBinding>> aclsFuture = KafkaFuture.completedFuture(aclBindings);

        when(adminClient.describeAcls(any(AclBindingFilter.class))).thenReturn(describeAclsResult);
        when(describeAclsResult.values()).thenReturn(aclsFuture);

        // When
        List<AclBindingInfo> result = aclService.listAcls();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        AclBindingInfo info = result.get(0);
        assertEquals("User:testuser", info.principal());
        assertEquals("TOPIC", info.resourceType());
        assertEquals("test-topic", info.resourceName());
        assertEquals("READ", info.operation());
        assertEquals("ALLOW", info.permission());
        verify(adminClient).describeAcls(AclBindingFilter.ANY);
    }

    @Test
    void createAcl_shouldCreateAclSuccessfully() throws Exception {
        // Given
        CreateAclRequest request = new CreateAclRequest(
                "User:testuser",
                "TOPIC",
                "test-topic",
                "READ",
                "ALLOW"
        );

        KafkaFuture<Void> createFuture = KafkaFuture.completedFuture(null);
        when(adminClient.createAcls(any(Collection.class))).thenReturn(createAclsResult);
        when(createAclsResult.all()).thenReturn(createFuture);

        // When
        aclService.createAcl(request);

        // Then
        ArgumentCaptor<Collection<AclBinding>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(adminClient).createAcls(captor.capture());
        Collection<AclBinding> capturedAcls = captor.getValue();
        assertEquals(1, capturedAcls.size());

        AclBinding aclBinding = capturedAcls.iterator().next();
        assertEquals("test-topic", aclBinding.pattern().name());
        assertEquals(ResourceType.TOPIC, aclBinding.pattern().resourceType());
        assertEquals("User:testuser", aclBinding.entry().principal());
        assertEquals(AclOperation.READ, aclBinding.entry().operation());
        assertEquals(AclPermissionType.ALLOW, aclBinding.entry().permissionType());
    }

    @Test
    void deleteAcls_shouldDeleteAclsSuccessfully() throws Exception {
        // Given
        KafkaFuture<Void> deleteFuture = KafkaFuture.completedFuture(null);
        when(adminClient.deleteAcls(any(Collection.class))).thenReturn(deleteAclsResult);
        when(deleteAclsResult.all()).thenReturn(deleteFuture);

        // When
        aclService.deleteAcls("TOPIC", "test-topic", "User:testuser", "READ");

        // Then
        ArgumentCaptor<Collection<AclBindingFilter>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(adminClient).deleteAcls(captor.capture());
        Collection<AclBindingFilter> capturedFilters = captor.getValue();
        assertEquals(1, capturedFilters.size());
    }

    @Test
    void listAcls_shouldThrowExceptionOnFailure() throws Exception {
        // Given
        KafkaFuture<Collection<AclBinding>> failedFuture = mock(KafkaFuture.class);
        when(failedFuture.get(anyLong(), any())).thenThrow(new RuntimeException("Connection failed"));
        when(adminClient.describeAcls(any(AclBindingFilter.class))).thenReturn(describeAclsResult);
        when(describeAclsResult.values()).thenReturn(failedFuture);

        // When / Then
        assertThrows(RuntimeException.class, () -> aclService.listAcls());
    }

    @Test
    void createAcl_shouldThrowExceptionOnFailure() throws Exception {
        // Given
        CreateAclRequest request = new CreateAclRequest(
                "User:testuser",
                "TOPIC",
                "test-topic",
                "READ",
                "ALLOW"
        );

        KafkaFuture<Void> failedFuture = mock(KafkaFuture.class);
        when(failedFuture.get(anyLong(), any())).thenThrow(new RuntimeException("ACL creation failed"));
        when(adminClient.createAcls(any(Collection.class))).thenReturn(createAclsResult);
        when(createAclsResult.all()).thenReturn(failedFuture);

        // When / Then
        assertThrows(RuntimeException.class, () -> aclService.createAcl(request));
    }
}
