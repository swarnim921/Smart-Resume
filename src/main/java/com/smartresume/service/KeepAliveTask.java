package com.smartresume.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class KeepAliveTask {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveTask.class);
    
    private final RestTemplate restTemplate;

    // Render sets RENDER_EXTERNAL_URL automatically (e.g. https://my-app.onrender.com)
    // If not on render, it falls back to the frontend URL from properties
    @Value("${RENDER_EXTERNAL_URL:${app.frontend.url:http://localhost:8080}}")
    private String appUrl;

    public KeepAliveTask(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Run every 10 minutes (600,000 milliseconds)
    @Scheduled(fixedRate = 600000)
    public void pingSelf() {
        try {
            // We use the actuator health endpoint to keep it lightweight
            String url = appUrl + "/actuator/health";
            logger.info("Pinging self to prevent Render sleep: {}", url);
            restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            logger.error("Failed to ping self for keep-alive", e);
        }
    }
}
