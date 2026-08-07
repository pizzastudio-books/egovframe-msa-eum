package com.pizzastudio.eum.attachment.api.dto;

import com.pizzastudio.eum.attachment.domain.Attachment;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AttachmentResponseDto {

    private Long attachmentId;
    private String applicationId;
    private String originalName;
    private String contentType;
    private Long fileSize;

    @Builder
    public AttachmentResponseDto(Attachment entity) {
        this.attachmentId = entity.getAttachmentId();
        this.applicationId = entity.getApplicationId();
        this.originalName = entity.getOriginalName();
        this.contentType = entity.getContentType();
        this.fileSize = entity.getFileSize();
    }
}
