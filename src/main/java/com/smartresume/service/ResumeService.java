package com.smartresume.service;

import com.smartresume.model.ResumeMeta;
import com.smartresume.model.User;
import com.smartresume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.springframework.data.mongodb.gridfs.GridFsResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResumeService {
    private final ResumeRepository resumeRepository;
    private final GridFsTemplate gridFsTemplate;

    public ResumeMeta store(MultipartFile file, User owner) throws IOException {
        return store(file, owner, "RESUME");
    }

    public ResumeMeta store(MultipartFile file, User owner, String fileType) throws IOException {
        ObjectId gridId;
        try {
            gridId = gridFsTemplate.store(file.getInputStream(), file.getOriginalFilename(), file.getContentType());
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file in GridFS", e);
        }
        ResumeMeta meta = new ResumeMeta();
        meta.setFilename(file.getOriginalFilename());
        meta.setContentType(file.getContentType());
        meta.setSize(file.getSize());
        meta.setGridFsId(gridId.toHexString());
        meta.setOwnerId(owner.getId());
        meta.setExtractedText(null); // Text will be extracted by ML service later if needed, saving huge memory
        meta.setFileType(fileType != null ? fileType : "RESUME");
        return resumeRepository.save(meta);
    }

    public GridFsResource getFileResourceByGridId(String gridId) {
        GridFSFile gridFSFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(gridId))));
        if (gridFSFile == null)
            return null;
        return gridFsTemplate.getResource(gridFSFile);
    }

    public Optional<ResumeMeta> findById(String id) {
        return resumeRepository.findById(id);
    }

    /**
     * Extract text from PDF resume for ML analysis
     */
    public String extractTextFromResume(String resumeId) throws IOException {
        // Get resume metadata
        Optional<ResumeMeta> resumeMetaOpt = resumeRepository.findById(resumeId);
        if (resumeMetaOpt.isEmpty()) {
            throw new RuntimeException("Resume not found");
        }

        ResumeMeta resumeMeta = resumeMetaOpt.get();

        // Get file from GridFS
        GridFsResource resource = getFileResourceByGridId(resumeMeta.getGridFsId());
        if (resource == null) {
            throw new RuntimeException("Resume file not found in GridFS");
        }

        String contentType = resumeMeta.getContentType();
        String filename = resumeMeta.getFilename() != null ? resumeMeta.getFilename().toLowerCase() : "";

        try (InputStream inputStream = resource.getInputStream()) {
            if (contentType != null && (contentType.equals("text/plain") || filename.endsWith(".txt"))) {
                return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } else if (contentType != null && (contentType.startsWith("image/") || filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg"))) {
                java.io.File tempFile = java.io.File.createTempFile("ocr_", filename);
                try {
                    java.nio.file.Files.copy(inputStream, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    net.sourceforge.tess4j.ITesseract tesseract = new net.sourceforge.tess4j.Tesseract();
                    // On Ubuntu (Render), tessdata is usually here
                    if (new java.io.File("/usr/share/tesseract-ocr/4.00/tessdata").exists()) {
                        tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");
                    } else if (new java.io.File("/usr/share/tesseract-ocr/5/tessdata").exists()) {
                        tesseract.setDatapath("/usr/share/tesseract-ocr/5/tessdata");
                    }
                    tesseract.setLanguage("eng");
                    return tesseract.doOCR(tempFile);
                } catch (Exception | Error e) {
                    throw new IOException("OCR processing failed. Please ensure Tesseract is installed: " + e.getMessage(), e);
                } finally {
                    tempFile.delete();
                }
            } else if (contentType != null && (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || filename.endsWith(".docx"))) {
                try (org.apache.poi.xwpf.usermodel.XWPFDocument docx = new org.apache.poi.xwpf.usermodel.XWPFDocument(inputStream);
                     org.apache.poi.xwpf.extractor.XWPFWordExtractor extractor = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(docx)) {
                    return extractor.getText();
                }
            } else if (contentType != null && (contentType.equals("application/msword") || filename.endsWith(".doc"))) {
                try (org.apache.poi.hwpf.HWPFDocument doc = new org.apache.poi.hwpf.HWPFDocument(inputStream);
                     org.apache.poi.hwpf.extractor.WordExtractor extractor = new org.apache.poi.hwpf.extractor.WordExtractor(doc)) {
                    return extractor.getText();
                }
            } else {
                // Default to PDF
                try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(inputStream)) {
                    org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                    return stripper.getText(document);
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to extract text: " + e.getMessage(), e);
        }
    }
}
