package com.mock.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {
    
    @Value("${logging.level.com.mock:INFO}")
    private String comMock = "INFO";
    
    public String getComMock() {
        return comMock;
    }
    
    public void setComMock(String comMock) {
        this.comMock = comMock;
    }
}

