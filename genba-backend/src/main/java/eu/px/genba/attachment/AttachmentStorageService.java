package eu.px.genba.attachment;

import eu.px.genba.common.exception.GenbaException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Writes {@link Attachment} files to local disk under
 * {@code ${genba.uploads.path}/{org_id}/{yyyy}/{MM}/{uuid}-{filename}}.
 *
 * <p>Layer 0 deliberately stays on the local filesystem; the abstraction here
 * lets Layer 1 swap to S3-compatible storage without touching the service or
 * controller layer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentStorageService {

    @Value("${genba.uploads.path:./uploads}")
    private String uploadsRoot;

    @Value("${genba.uploads.max-file-size-bytes:26214400}")
    private long maxFileSizeBytes;

    public StoredFile store(UUID orgId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new EmptyFileException();
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new FileTooLargeException();
        }

        OffsetDateTime now = OffsetDateTime.now();
        String yyyy = String.format("%04d", now.getYear());
        String mm = String.format("%02d", now.getMonthValue());
        String safeName = sanitize(file.getOriginalFilename());
        String stored = UUID.randomUUID() + "-" + safeName;

        Path relative = Path.of(orgId.toString(), yyyy, mm, stored);
        Path target = Path.of(uploadsRoot).resolve(relative);

        try {
            Files.createDirectories(target.getParent());
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Failed to write attachment to {}", target, e);
            throw new FileStorageException(e);
        }

        return new StoredFile(
                relative.toString(),
                safeName,
                file.getContentType(),
                file.getSize());
    }

    public Resource load(String fileUrl) {
        Path full = Path.of(uploadsRoot).resolve(fileUrl).normalize();
        if (!full.startsWith(Path.of(uploadsRoot).normalize())) {
            // Path traversal guard — refuse anything that escapes the root.
            throw new FileNotFoundForAttachmentException();
        }
        FileSystemResource resource = new FileSystemResource(full);
        if (!resource.exists() || !resource.isReadable()) {
            throw new FileNotFoundForAttachmentException();
        }
        return resource;
    }

    public void delete(String fileUrl) {
        Path full = Path.of(uploadsRoot).resolve(fileUrl).normalize();
        if (!full.startsWith(Path.of(uploadsRoot).normalize())) {
            return;
        }
        try {
            Files.deleteIfExists(full);
        } catch (IOException e) {
            log.warn("Failed to delete attachment file {}: {}", full, e.getMessage());
        }
    }

    private static String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "upload";
        }
        // Strip path separators and control chars; keep dots / dashes / spaces.
        String cleaned = name.replaceAll("[/\\\\\\x00-\\x1f]", "_");
        if (cleaned.length() > 255) {
            cleaned = cleaned.substring(cleaned.length() - 255);
        }
        return cleaned;
    }

    public record StoredFile(String relativePath, String originalFilename, String mimeType, long byteSize) {
    }

    public static final class EmptyFileException extends GenbaException {
        public EmptyFileException() {
            super("attachment.error.emptyFile", "EMPTY_FILE");
        }
    }

    public static final class FileTooLargeException extends GenbaException {
        public FileTooLargeException() {
            super("attachment.error.fileTooLarge", "FILE_TOO_LARGE");
        }
    }

    public static final class FileStorageException extends GenbaException {
        public FileStorageException(Throwable cause) {
            super("attachment.error.storageFailed", "STORAGE_FAILED", cause);
        }
    }

    public static final class FileNotFoundForAttachmentException extends GenbaException {
        public FileNotFoundForAttachmentException() {
            super("common.error.notFound", "FILE_NOT_FOUND");
        }
    }
}
