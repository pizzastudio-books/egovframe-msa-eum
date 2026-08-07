package com.pizzastudio.eum.core.attachment.api;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.pizzastudio.eum.core.attachment.api.dto.AttachmentResponseDto;
import com.pizzastudio.eum.core.attachment.domain.Attachment;
import com.pizzastudio.eum.core.attachment.service.AttachmentService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "증빙 첨부", description = "파일 올리기와 내려받기")
@RestController
@RequiredArgsConstructor
public class AttachmentApiController {

    private final AttachmentService attachmentService;

    @PostMapping("/api/v1/applications/{applicationId}/files")
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponseDto upload(@PathVariable("applicationId") String applicationId,
        @RequestParam("file") MultipartFile file) {
        return attachmentService.upload(applicationId, file);
    }

    @GetMapping("/api/v1/applications/{applicationId}/files")
    @ResponseStatus(HttpStatus.OK)
    public List<AttachmentResponseDto> findByApplication(
        @PathVariable("applicationId") String applicationId) {
        return attachmentService.findByApplication(applicationId);
    }

    @GetMapping("/api/v1/files/{attachmentId}")
    public ResponseEntity<Resource> download(@PathVariable("attachmentId") Long attachmentId) {
        Attachment attachment = attachmentService.findEntity(attachmentId);
        Resource resource = attachmentService.loadFile(attachmentId);

        return ResponseEntity.ok()
            .contentType(attachment.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(attachment.getContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + attachment.getStoredName() + "\"")
            .body(resource);
    }
}
