package com.mock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mock-controller")
public class MockControllerConfig {
    
    private String url = "http://localhost:8080";
    private long checkIntervalSeconds = 5;
    private long connectTimeoutSeconds = 10;
    private long readTimeoutSeconds = 10;
    
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
    
    public long getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }
    
    public void setConnectTimeoutSeconds(long connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }
    
    public long getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }
    
    public void setReadTimeoutSeconds(long readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }
}

