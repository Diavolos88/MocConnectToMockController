package com.mock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mock-controller")
public class MockControllerConfig {
    
    private String url = "http://localhost:8080";
    private long checkIntervalSeconds = 5;
    private String healthcheckPath = "/service/healthcheck";
    private long healthcheckTimeoutSeconds = 5;
    private long healthcheckIntervalSeconds = 10;
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
    
    public String getHealthcheckPath() {
        return healthcheckPath;
    }
    
    public void setHealthcheckPath(String healthcheckPath) {
        this.healthcheckPath = healthcheckPath;
    }
    
    public long getHealthcheckTimeoutSeconds() {
        return healthcheckTimeoutSeconds;
    }
    
    public void setHealthcheckTimeoutSeconds(long healthcheckTimeoutSeconds) {
        this.healthcheckTimeoutSeconds = healthcheckTimeoutSeconds;
    }
    
    public long getHealthcheckIntervalSeconds() {
        return healthcheckIntervalSeconds;
    }
    
    public void setHealthcheckIntervalSeconds(long healthcheckIntervalSeconds) {
        this.healthcheckIntervalSeconds = healthcheckIntervalSeconds;
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

