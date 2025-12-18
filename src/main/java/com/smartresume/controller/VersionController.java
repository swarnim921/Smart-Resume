package com.smartresume.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class VersionController {

    @GetMapping("/version")
    public Map<String, String> getVersion() {
        Map<String, String> version = new HashMap<>();
        version.put("version", "2.0.0");
        version.put("commit", "e49f564");
        version.put("jwtRoleClaimEnabled", "true");
        version.put("staticFileBypassEnabled", "true");
        version.put("buildDate", java.time.LocalDateTime.now().toString());
        return version;
    }
}
