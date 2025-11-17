package com.mock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mock-controller")
public class MockControllerConfig {
    
    private String url = "http://localhost:8080";
    private long checkIntervalSeconds = 5;
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public long getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }
    
    public void setCheckIntervalSeconds(long checkIntervalSeconds) {
        this.checkIntervalSeconds = checkIntervalSeconds;
    }
}

