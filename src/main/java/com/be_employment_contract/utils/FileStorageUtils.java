package com.be_employment_contract.utils;

import com.be_employment_contract.dto.CreateStaffDocumentRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class FileStorageUtils {

    private FileStorageUtils() {
    }

    public static List<CreateStaffDocumentRequestDTO> storeStaffAttachments(List<MultipartFile> attachments, String targetDirectory)
        throws IOException {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }

        Path targetDir = Paths.get(targetDirectory).toAbsolutePath().normalize();
        Files.createDirectories(targetDir);

        List<CreateStaffDocumentRequestDTO> documents = new ArrayList<>();
        for (MultipartFile attachment : attachments) {
            if (attachment == null || attachment.isEmpty()) {
                continue;
            }

            String originalName = attachment.getOriginalFilename();
            String safeBaseName = sanitizeFileName(originalName);
            String storedName = System.currentTimeMillis() + "_" + UUID.randomUUID() + "_" + safeBaseName;
            Path storedPath = targetDir.resolve(storedName).normalize();

            Files.copy(attachment.getInputStream(), storedPath, StandardCopyOption.REPLACE_EXISTING);

            String fileType = attachment.getContentType();
            if (fileType == null || fileType.isBlank()) {
                fileType = inferFileTypeFromName(safeBaseName);
            }

            documents.add(new CreateStaffDocumentRequestDTO(
                safeBaseName,
                storedPath.toString(),
                fileType
            ));
        }

        return documents;
    }

    public static List<CreateStaffDocumentRequestDTO> normalizePayloadDocumentsToLocal(
            List<CreateStaffDocumentRequestDTO> payloadDocuments,
            String targetDirectory
    ) throws IOException {
        if (payloadDocuments == null || payloadDocuments.isEmpty()) {
            return List.of();
        }

        Path targetDir = Paths.get(targetDirectory).toAbsolutePath().normalize();
        Files.createDirectories(targetDir);

        List<CreateStaffDocumentRequestDTO> normalized = new ArrayList<>();
        for (CreateStaffDocumentRequestDTO document : payloadDocuments) {
            if (document == null) {
                continue;
            }

            String safeFileName = sanitizeFileName(document.getFileName());
            String sourcePath = document.getFilePath() == null ? "" : document.getFilePath().trim();
            String fileType = (document.getFileType() == null || document.getFileType().isBlank())
                    ? inferFileTypeFromName(safeFileName)
                    : document.getFileType().trim();

            // Keep empty source as-is so validation/business layer can decide.
            if (sourcePath.isBlank()) {
                normalized.add(new CreateStaffDocumentRequestDTO(safeFileName, sourcePath, fileType));
                continue;
            }

            String storedPath = tryCopyToLocal(sourcePath, safeFileName, targetDir);
            normalized.add(new CreateStaffDocumentRequestDTO(safeFileName, storedPath, fileType));
        }

        return normalized;
    }

    private static String tryCopyToLocal(String sourcePath, String safeFileName, Path targetDir) throws IOException {
        String storedName = System.currentTimeMillis() + "_" + UUID.randomUUID() + "_" + safeFileName;
        Path storedPath = targetDir.resolve(storedName).normalize();

        if (sourcePath.startsWith("http://") || sourcePath.startsWith("https://")) {
            try (InputStream inputStream = URI.create(sourcePath).toURL().openStream()) {
                Files.copy(inputStream, storedPath, StandardCopyOption.REPLACE_EXISTING);
                return storedPath.toString();
            } catch (Exception ignored) {
                // If external URL cannot be fetched, keep original path to avoid blocking contract creation.
                return sourcePath;
            }
        }

        Path localSource = Paths.get(sourcePath).toAbsolutePath().normalize();
        if (!Files.exists(localSource)) {
            return sourcePath;
        }

        Files.copy(localSource, storedPath, StandardCopyOption.REPLACE_EXISTING);
        return storedPath.toString();
    }

    private static String sanitizeFileName(String originalName) {
        String fallback = "file";
        if (originalName == null || originalName.isBlank()) {
            return fallback;
        }

        String normalized = originalName.replace("\\", "/");
        String fileNameOnly = normalized.substring(normalized.lastIndexOf('/') + 1);
        String cleaned = fileNameOnly.replaceAll("[^A-Za-z0-9._-]", "_").toLowerCase(Locale.ROOT);
        return cleaned.isBlank() ? fallback : cleaned;
    }

    private static String inferFileTypeFromName(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.contains(".")) {
            return "application/octet-stream";
        }

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "pdf" -> "pdf";
            case "doc" -> "doc";
            case "docx" -> "docx";
            case "png" -> "png";
            case "jpg", "jpeg" -> "jpg";
            default -> extension;
        };
    }
}
