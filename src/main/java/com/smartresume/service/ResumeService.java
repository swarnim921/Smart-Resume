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
        meta.setExtractedText(new String(file.getBytes()));
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

        // Extract text from PDF
        try (InputStream inputStream = resource.getInputStream()) {
            org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(inputStream);
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String text = stripper.getText(document);
            document.close();
            return text;
        } catch (Exception e) {
            throw new IOException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }
}
