package com.turing.app.api.document.controller;

import com.turing.app.api.auth.security.AuthenticatedUser;
import com.turing.app.api.document.dto.*;
import com.turing.app.api.document.service.DocumentService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class DocumentController {
    private final DocumentService service;public DocumentController(DocumentService service){this.service=service;}
    @GetMapping("/api/admin/application-periods/{periodId}/document-requirements") public List<DocumentRequirementResponse> adminRequirements(@PathVariable UUID periodId){return service.adminRequirements(periodId);}
    @PostMapping("/api/admin/application-periods/{periodId}/document-requirements") @ResponseStatus(HttpStatus.CREATED) public DocumentRequirementResponse createRequirement(@PathVariable UUID periodId,@Valid @RequestBody DocumentRequirementRequest body){return service.createRequirement(periodId,body);}
    @GetMapping("/api/me/applications/{applicationId}/document-requirements") public List<DocumentRequirementResponse> requirements(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID applicationId){return service.applicationRequirements(user.id(),applicationId);}
    @GetMapping("/api/me/applications/{applicationId}/documents") public List<StoredFileResponse> documents(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID applicationId){return service.list(user.id(),applicationId);}
    @PostMapping(value="/api/me/applications/{applicationId}/documents",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) public StoredFileResponse upload(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID applicationId,@RequestParam UUID requirementId,@RequestPart("file") MultipartFile file){return service.upload(user.id(),applicationId,requirementId,file);}
    @GetMapping("/api/me/documents/{id}") public ResponseEntity<byte[]> download(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID id){DocumentDownload value=service.download(user.id(),id);ContentDisposition disposition=ContentDisposition.attachment().filename(value.originalName(),StandardCharsets.UTF_8).build();return ResponseEntity.ok().contentType(MediaType.parseMediaType(value.mimeType())).header(HttpHeaders.CONTENT_DISPOSITION,disposition.toString()).contentLength(value.content().length).body(value.content());}
    @DeleteMapping("/api/me/documents/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@AuthenticationPrincipal AuthenticatedUser user,@PathVariable UUID id){service.delete(user.id(),id);}
}
