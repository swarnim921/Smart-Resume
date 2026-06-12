package com.smartresume.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document(collection = "batch_jobs")
public class BatchJob {

    @Id
    private String id;
    
    private String status; // PROCESSING, COMPLETED, FAILED
    private int totalResumes;
    private int processedResumes;
    private int totalJds;
    private String jdId;
    
    private List<Map<String, Object>> results;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTotalResumes() { return totalResumes; }
    public void setTotalResumes(int totalResumes) { this.totalResumes = totalResumes; }

    public int getProcessedResumes() { return processedResumes; }
    public void setProcessedResumes(int processedResumes) { this.processedResumes = processedResumes; }

    public int getTotalJds() { return totalJds; }
    public void setTotalJds(int totalJds) { this.totalJds = totalJds; }

    public String getJdId() { return jdId; }
    public void setJdId(String jdId) { this.jdId = jdId; }

    public List<Map<String, Object>> getResults() { return results; }
    public void setResults(List<Map<String, Object>> results) { this.results = results; }
}
