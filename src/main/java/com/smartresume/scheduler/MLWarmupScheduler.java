package com.smartresume.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Keeps the ML service warm by pinging its /health endpoint every 10 minutes.
 * Prevents cold start delays on free-tier hosting (Render, Railway).
 */
@Component
public class MLWarmupScheduler {

    @Value("${ml.service.url:http://localhost:5000}")
    private String mlServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedRate = 600_000) // every 10 minutes
    public void pingMLService() {
        try {
            String url = mlServiceUrl + "/health";
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("🟢 ML service ping OK: " + response);
        } catch (Exception e) {
            // Don't crash the app if ML is down — just log
            System.out.println("🔴 ML service warmup ping failed: " + e.getMessage());
        }
    }
}
