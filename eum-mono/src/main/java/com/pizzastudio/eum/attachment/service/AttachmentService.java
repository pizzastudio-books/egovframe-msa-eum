package com.pizzastudio.eum.attachment.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.pizzastudio.eum.attachment.api.dto.AttachmentResponseDto;
import com.pizzastudio.eum.attachment.domain.Attachment;
import com.pizzastudio.eum.attachment.domain.AttachmentRepository;
import com.pizzastudio.eum.common.exception.BusinessMessageException;
import com.pizzastudio.eum.common.exception.EntityNotFoundException;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 증빙 첨부.
 *
 * <p>파일을 <b>서버 로컬 디스크</b>에 쌓는다. 공공에서 아주 흔한 형태이고, 지금은 한 대에서만
 * 도니 문제가 드러나지 않는다. 컨테이너로 옮기고 여러 개로 늘리는 순간 무너진다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;

    @Value("${eum.file.directory:${java.io.tmpdir}/eum-attachments}")
    private String directory;

    private Path root;

    @PostConstruct
    void prepareDirectory() {
        try {
            this.root = Paths.get(directory).toAbsolutePath().normalize();
            Files.createDirectories(root);
            log.info("첨부 파일 저장 위치: {}", root);
        } catch (IOException e) {
            throw new IllegalStateException("첨부 파일 폴더를 만들지 못했습니다. " + directory, e);
        }
    }

    public AttachmentResponseDto upload(String applicationId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessMessageException("올릴 파일이 없습니다.");
        }

        String originalName = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
        String storedName = UUID.randomUUID() + suffixOf(originalName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessMessageException("파일을 저장하지 못했습니다.");
        }

        Attachment saved = attachmentRepository.save(Attachment.builder()
            .applicationId(applicationId)
            .originalName(originalName)
            .storedName(storedName)
            .contentType(file.getContentType())
            .fileSize(file.getSize())
            .build());

        return AttachmentResponseDto.builder().entity(saved).build();
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponseDto> findByApplication(String applicationId) {
        return attachmentRepository.findByApplicationIdOrderByAttachmentId(applicationId).stream()
            .map(a -> AttachmentResponseDto.builder().entity(a).build())
            .toList();
    }

    @Transactional(readOnly = true)
    public Attachment findEntity(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new EntityNotFoundException("첨부가 없습니다. ID=" + attachmentId));
    }

    @Transactional(readOnly = true)
    public Resource loadFile(Long attachmentId) {
        Attachment attachment = findEntity(attachmentId);
        Path path = root.resolve(attachment.getStoredName());
        if (!Files.exists(path)) {
            // 파드를 갈아치우면 메타 정보만 남고 파일은 사라진다
            throw new BusinessMessageException("파일이 서버에 없습니다. 다시 올려 주십시오.");
        }
        return new FileSystemResource(path);
    }

    private String suffixOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot);
    }
}
