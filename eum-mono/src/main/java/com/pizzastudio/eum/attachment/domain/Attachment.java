package com.pizzastudio.eum.attachment.domain;

import com.pizzastudio.eum.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 증빙 첨부 파일의 메타 정보. 실제 파일은 서버 로컬 디스크에 쌓인다.
 *
 * <p>이 구조가 컨테이너로 옮길 때 가장 먼저 걸린다. 컨테이너가 죽으면 디스크도 함께
 * 사라지고, 여러 벌로 늘리면 파드마다 다른 파일을 갖는다. 5.3 과 10.3 의 소재다.</p>
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "attachment")
public class Attachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long attachmentId;

    @Column(name = "application_id", length = 40, nullable = false)
    private String applicationId;

    @Column(name = "original_name", length = 300, nullable = false)
    private String originalName;

    @Column(name = "stored_name", length = 300, nullable = false)
    private String storedName;

    @Column(name = "content_type", length = 200)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Builder
    public Attachment(Long attachmentId, String applicationId, String originalName,
        String storedName, String contentType, Long fileSize) {
        this.attachmentId = attachmentId;
        this.applicationId = applicationId;
        this.originalName = originalName;
        this.storedName = storedName;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }
}
