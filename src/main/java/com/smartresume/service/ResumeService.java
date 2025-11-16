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
        if (gridFSFile == null) return null;
        return gridFsTemplate.getResource(gridFSFile);
    }

    public Optional<ResumeMeta> findById(String id) {
        return resumeRepository.findById(id);
    }
}
