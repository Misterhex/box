package com.kafka.adminapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafka.adminapi.model.AclBindingInfo;
import com.kafka.adminapi.model.CreateAclRequest;
import com.kafka.adminapi.service.AclService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AclController.class)
class AclControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AclService aclService;

    @Test
    void listAcls_shouldReturnAclList() throws Exception {
        // Given
        List<AclBindingInfo> acls = Arrays.asList(
                new AclBindingInfo("User:user1", "TOPIC", "topic1", "READ", "ALLOW"),
                new AclBindingInfo("User:user2", "TOPIC", "topic2", "WRITE", "ALLOW")
        );
        when(aclService.listAcls()).thenReturn(acls);

        // When / Then
        mockMvc.perform(get("/api/acls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].principal").value("User:user1"))
                .andExpect(jsonPath("$[0].resourceType").value("TOPIC"))
                .andExpect(jsonPath("$[0].resourceName").value("topic1"))
                .andExpect(jsonPath("$[0].operation").value("READ"))
                .andExpect(jsonPath("$[0].permission").value("ALLOW"))
                .andExpect(jsonPath("$[1].principal").value("User:user2"));

        verify(aclService).listAcls();
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
        doNothing().when(aclService).createAcl(any(CreateAclRequest.class));

        // When / Then
        mockMvc.perform(post("/api/acls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(aclService).createAcl(any(CreateAclRequest.class));
    }

    @Test
    void deleteAcls_shouldDeleteAclsSuccessfully() throws Exception {
        // Given
        doNothing().when(aclService).deleteAcls(any(), any(), any(), any());

        // When / Then
        mockMvc.perform(delete("/api/acls")
                        .param("resourceType", "TOPIC")
                        .param("resourceName", "test-topic")
                        .param("principal", "User:testuser")
                        .param("operation", "READ"))
                .andExpect(status().isNoContent());

        verify(aclService).deleteAcls("TOPIC", "test-topic", "User:testuser", "READ");
    }

    @Test
    void deleteAcls_shouldWorkWithoutQueryParams() throws Exception {
        // Given
        doNothing().when(aclService).deleteAcls(null, null, null, null);

        // When / Then
        mockMvc.perform(delete("/api/acls"))
                .andExpect(status().isNoContent());

        verify(aclService).deleteAcls(null, null, null, null);
    }

    @Test
    void listAcls_shouldReturnErrorOnServiceFailure() throws Exception {
        // Given
        when(aclService.listAcls()).thenThrow(new RuntimeException("Connection failed"));

        // When / Then
        mockMvc.perform(get("/api/acls"))
                .andExpect(status().is5xxServerError());

        verify(aclService).listAcls();
    }

    @Test
    void createAcl_shouldReturnErrorOnInvalidRequest() throws Exception {
        // Given - invalid JSON
        String invalidJson = "{\"principal\": \"User:test\"}";

        // When / Then
        mockMvc.perform(post("/api/acls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().is4xxClientError());

        verify(aclService, never()).createAcl(any());
    }

    @Test
    void deleteAcls_shouldDeleteWithPartialParams() throws Exception {
        // Given
        doNothing().when(aclService).deleteAcls(any(), any(), any(), any());

        // When / Then
        mockMvc.perform(delete("/api/acls")
                        .param("resourceType", "TOPIC"))
                .andExpect(status().isNoContent());

        verify(aclService).deleteAcls("TOPIC", null, null, null);
    }
}
