package com.kafka.adminapi.controller;

import com.kafka.adminapi.model.AclBindingInfo;
import com.kafka.adminapi.model.CreateAclRequest;
import com.kafka.adminapi.service.AclService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/acls")
public class AclController {

    private final AclService aclService;

    public AclController(AclService aclService) {
        this.aclService = aclService;
    }

    @GetMapping
    public List<AclBindingInfo> listAcls() {
        return aclService.listAcls();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createAcl(@RequestBody CreateAclRequest request) {
        aclService.createAcl(request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAcls(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceName,
            @RequestParam(required = false) String principal,
            @RequestParam(required = false) String operation
    ) {
        aclService.deleteAcls(resourceType, resourceName, principal, operation);
    }
}
