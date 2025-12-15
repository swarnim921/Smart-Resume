package com.smartresume.service;

import com.smartresume.model.ResumeMeta;
import com.smartresume.model.User;
import com.smartresume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final GridFsTemplate gridFsTemplate;
    private final ResumeRepository resumeRepository;

    public ResumeMeta store(MultipartFile file, User user) throws Exception {
        ObjectId gridId;

        try (InputStream is = file.getInputStream()) {
            gridId = gridFsTemplate.store(is, file.getOriginalFilename(), file.getContentType());
        }

        ResumeMeta meta = new ResumeMeta();
        meta.setUserId(user.getId());
        meta.setGridFsId(gridId.toHexString());
        meta.setFilename(file.getOriginalFilename());
        meta.setContentType(file.getContentType());

        return resumeRepository.save(meta);
    }

    public Optional<ResumeMeta> findById(String id) {
        return resumeRepository.findById(id);
    }

    public GridFsResource getFileResourceByGridId(String gridId) {
        Query query = Query.query(Criteria.where("_id").is(new ObjectId(gridId)));
        return gridFsTemplate.getResource(gridFsTemplate.findOne(query));
    }
}
