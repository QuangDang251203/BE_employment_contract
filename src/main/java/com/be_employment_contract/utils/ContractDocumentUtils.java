package com.be_employment_contract.utils;

import com.be_employment_contract.constant.AppConstants;
import com.be_employment_contract.dto.CreateContractRequestDTO;
import com.be_employment_contract.entity.Contract;
import org.docx4j.Docx4J;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.P;
import org.docx4j.wml.Text;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ContractDocumentUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Logger log = LoggerFactory.getLogger(ContractDocumentUtils.class);

    private ContractDocumentUtils() {
    }

    public static GeneratedContractFile generatePdfFromTemplate(CreateContractRequestDTO request, Contract contract) throws IOException {
        Path outputDir = Paths.get(AppConstants.CONTRACT_OUTPUT_DIR).toAbsolutePath().normalize();
        Files.createDirectories(outputDir);

        String baseName = contract.getContractCode() + "_" + System.currentTimeMillis();
        Path outputDocx = outputDir.resolve(baseName + ".docx");
        Path outputPdf = outputDir.resolve(baseName + ".pdf");

        try (InputStream inputStream = new ClassPathResource(AppConstants.CONTRACT_TEMPLATE_CLASSPATH).getInputStream()) {
            WordprocessingMLPackage document = WordprocessingMLPackage.load(inputStream);
            Map<String, String> placeholders = buildPlaceholderMap(request, contract);
            logCriticalPlaceholders(contract, request, placeholders);
            replacePlaceholders(document, placeholders);

            document.save(outputDocx.toFile());
            try (OutputStream pdfOut = Files.newOutputStream(outputPdf)) {
                Docx4J.toPDF(document, pdfOut);
            }
        } catch (Docx4JException exception) {
            throw new IOException("Failed to process DOCX template", exception);
        }

        return new GeneratedContractFile(outputPdf.getFileName().toString(), outputPdf.toString(), "application/pdf");
    }

    public static GeneratedContractFile createSignedPdf(Path sourcePdfPath, MultipartFile signatureImage, String contractCode) throws IOException {
        if (signatureImage == null || signatureImage.isEmpty()) {
            throw new IOException("Signature image is empty");
        }

        Path outputDir = Paths.get(AppConstants.CONTRACT_OUTPUT_DIR).toAbsolutePath().normalize();
        Files.createDirectories(outputDir);

        String signedName = contractCode + "_signed_" + System.currentTimeMillis() + ".pdf";
        Path signedPdfPath = outputDir.resolve(signedName);

        try (PDDocument document = PDDocument.load(sourcePdfPath.toFile())) {
            BufferedImage signature = ImageIO.read(signatureImage.getInputStream());
            if (signature == null) {
                throw new IOException("Cannot read signature image");
            }

            PDPage page = document.getPage(document.getNumberOfPages() - 1);
            PDImageXObject image = LosslessFactory.createFromImage(document, signature);
            float width = 160f;
            float height = 60f;
            // Place signature under 'NGUOI LAO DONG' block (left column, higher position).
            float x = 120f;
            float y = 490f;

            try (PDPageContentStream contentStream = new PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
            )) {
                contentStream.drawImage(image, x, y, width, height);
            }

            document.save(signedPdfPath.toFile());
        }

        Path signatureDir = outputDir.resolve("signatures");
        Files.createDirectories(signatureDir);
        String signatureName = contractCode + "_" + UUID.randomUUID() + "_signature.png";
        Path signaturePath = signatureDir.resolve(signatureName);
        Files.copy(signatureImage.getInputStream(), signaturePath, StandardCopyOption.REPLACE_EXISTING);

        return new GeneratedContractFile(signedPdfPath.getFileName().toString(), signedPdfPath.toString(), "application/pdf");
    }

    public static GeneratedContractFile createStampedPdf(Path sourcePdfPath, String contractCode) throws IOException {
        Path outputDir = Paths.get(AppConstants.CONTRACT_OUTPUT_DIR).toAbsolutePath().normalize();
        Files.createDirectories(outputDir);

        String stampedName = contractCode + "_stamped_" + System.currentTimeMillis() + ".pdf";
        Path stampedPdfPath = outputDir.resolve(stampedName);

        try (PDDocument document = PDDocument.load(sourcePdfPath.toFile());
             InputStream stampInput = new ClassPathResource(AppConstants.STAMP_IMAGE_CLASSPATH).getInputStream()) {
            BufferedImage stamp = ImageIO.read(stampInput);
            if (stamp == null) {
                throw new IOException("Cannot read static stamp image");
            }

            PDPage page = document.getPage(document.getNumberOfPages() - 1);
            PDImageXObject image = LosslessFactory.createFromImage(document, stamp);
            float width = 160f;
            float height = 120f;
            // Mirror employee signature block to the right side under "NGUOI SU DUNG LAO DONG".
            float x = page.getMediaBox().getWidth() - 120f - width;
            // Lower stamp by ~1.5cm (about 42.5 PDF points): 490 -> 448.
            float y = 448f;

            try (PDPageContentStream contentStream = new PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
            )) {
                contentStream.drawImage(image, x, y, width, height);
            }

            document.save(stampedPdfPath.toFile());
        }

        Path stampDir = outputDir.resolve("stamps");
        Files.createDirectories(stampDir);
        String stampName = contractCode + "_" + UUID.randomUUID() + "_stamp.png";
        Path stampPath = stampDir.resolve(stampName);
        try (InputStream stampCopyInput = new ClassPathResource(AppConstants.STAMP_IMAGE_CLASSPATH).getInputStream()) {
            Files.copy(stampCopyInput, stampPath, StandardCopyOption.REPLACE_EXISTING);
        }

        return new GeneratedContractFile(stampedPdfPath.getFileName().toString(), stampedPdfPath.toString(), "application/pdf");
    }

    private static Map<String, String> buildPlaceholderMap(CreateContractRequestDTO request, Contract contract) {
        Map<String, String> values = new HashMap<>();
        values.put("{{so_hop_dong}}", safe(contract.getDecisionNumber()));
        values.put("{{ngay_quyet_dinh}}", formatDate(contract.getDecisionDate()));
        values.put("{{ngay_thang_nam}}", formatDate(LocalDate.now()));
        values.put("{{ten_nguoi_lao_dong}}", safe(request.getFullName()));
        values.put("{{ngay_sinh}}", formatDate(request.getDateOfBirth()));
        values.put("{{so_cccd}}", safe(request.getSoCCCD()));
        values.put("{{ngay_cap_can_cuoc_va_noi_cap}}", formatDate(request.getDateIssued()) + " - " + safe(request.getIssuingLocation()));
        values.put("{{dia_chi}}", safe(request.getAddress()));
        values.put("{{trinh_do_dao_tao}}", safe(request.getLevelOfTraining()));
        values.put("{{ten_chi_nhanh}}", contract.getBranch() == null ? "" : safe(contract.getBranch().getBranchName()));
        values.put("{{vi_tri_cong_viec}}", safe(request.getJobPosition()));
        values.put("{{ngay_bat_dau}}", formatDate(request.getStartDate()));
        values.put("{{ngay_ket_thuc}}", formatDate(request.getEndDate()));
        values.put("{{tien_luong}}", formatMoney(request.getProbationarySalary()));
        values.put("{{tien_luong_bang_chu}}", NumberToWordsUtils.moneyToWords(request.getProbationarySalary()));
        return values;
    }

    private static void logCriticalPlaceholders(
            Contract contract,
            CreateContractRequestDTO request,
            Map<String, String> placeholders
    ) {
        if (!log.isInfoEnabled()) {
            return;
        }

        log.info(
                "Template mapping check contractCode={}: decisionDateRaw={}, soCCCDRaw={}, dateIssuedRaw={}, issuingLocationRaw={}, probationarySalaryRaw={}",
                contract == null ? "" : safe(contract.getContractCode()),
                contract == null ? "" : contract.getDecisionDate(),
                request == null ? "" : safe(request.getSoCCCD()),
                request == null ? "" : request.getDateIssued(),
                request == null ? "" : safe(request.getIssuingLocation()),
                request == null ? "" : request.getProbationarySalary()
        );

        log.info(
                "Template placeholders before replace: {{ngay_quyet_dinh}}='{}', {{so_cccd}}='{}', {{ngay_cap_can_cuoc_va_noi_cap}}='{}', {{tien_luong_bang_chu}}='{}'",
                placeholders.getOrDefault("{{ngay_quyet_dinh}}", ""),
                placeholders.getOrDefault("{{so_cccd}}", ""),
                placeholders.getOrDefault("{{ngay_cap_can_cuoc_va_noi_cap}}", ""),
                placeholders.getOrDefault("{{tien_luong_bang_chu}}", "")
        );
    }

    private static void replacePlaceholders(WordprocessingMLPackage document, Map<String, String> placeholders) {
        // Replace at paragraph scope to handle placeholders split across multiple runs/text nodes.
        List<Object> paragraphs = getAllElementFromObject(document.getMainDocumentPart(), P.class);
        int changedParagraphs = 0;

        for (Object paragraphObj : paragraphs) {
            P paragraph = (P) paragraphObj;
            List<Text> texts = getAllElementFromObject(paragraph, Text.class)
                    .stream()
                    .map(Text.class::cast)
                    .toList();

            if (texts.isEmpty()) {
                continue;
            }

            String original = texts.stream()
                    .map(Text::getValue)
                    .map(value -> value == null ? "" : value)
                    .collect(Collectors.joining());

            String updated = applyPlaceholders(original, placeholders);
            if (updated.equals(original)) {
                continue;
            }

            texts.get(0).setValue(updated);
            for (int i = 1; i < texts.size(); i++) {
                texts.get(i).setValue("");
            }
            changedParagraphs++;
        }

        log.info("Template replace completed: changedParagraphs={}", changedParagraphs);
        logUnresolvedPlaceholders(document, placeholders);
    }

    private static String applyPlaceholders(String value, Map<String, String> placeholders) {
        String updated = value == null ? "" : value;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            updated = updated.replace(entry.getKey(), entry.getValue());
        }
        return updated;
    }

    private static void logUnresolvedPlaceholders(WordprocessingMLPackage document, Map<String, String> placeholders) {
        String allText = getAllElementFromObject(document.getMainDocumentPart(), Text.class)
                .stream()
                .map(Text.class::cast)
                .map(Text::getValue)
                .map(value -> value == null ? "" : value)
                .collect(Collectors.joining("\n"));

        List<String> unresolved = placeholders.keySet().stream()
                .filter(allText::contains)
                .toList();

        if (unresolved.isEmpty()) {
            log.info("Template replace verification: all placeholders were resolved");
            return;
        }

        log.warn("Template replace verification: unresolved placeholders={}", unresolved);
    }

    private static List<Object> getAllElementFromObject(Object obj, Class<?> toSearch) {
        List<Object> result = new java.util.ArrayList<>();
        if (obj == null) {
            return result;
        }

        if (obj.getClass().equals(toSearch)) {
            result.add(obj);
            return result;
        }

        if (obj instanceof jakarta.xml.bind.JAXBElement<?> jaxbElement) {
            return getAllElementFromObject(jaxbElement.getValue(), toSearch);
        }

        if (obj instanceof org.docx4j.wml.ContentAccessor contentAccessor) {
            for (Object child : contentAccessor.getContent()) {
                result.addAll(getAllElementFromObject(child, toSearch));
            }
        }
        return result;
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "" : DATE_FORMATTER.format(date);
    }

    private static String formatMoney(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record GeneratedContractFile(String fileName, String filePath, String fileType) {
    }
}
