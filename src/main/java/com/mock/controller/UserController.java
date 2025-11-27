package com.mock.controller;

import com.mock.config.ConfigAggregator;
import com.mock.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UserController {
    
    private final UserService userService;
    private final ConfigAggregator configAggregator;
    
    @Autowired
    public UserController(UserService userService, ConfigAggregator configAggregator) {
        this.userService = userService;
        this.configAggregator = configAggregator;
    }
    
    @GetMapping("/user/login")
    public ResponseEntity<Map<String, String>> userLogin() {
        return userService.getUserLoginResponse();
    }
    
    @GetMapping("/user/data")
    public ResponseEntity<Map<String, String>> userData() {
        return userService.getDataFetchResponse();
    }
    
    @GetMapping("/user/config/status")
    public ResponseEntity<Map<String, Object>> getUserConfigStatus() {
        return ResponseEntity.ok(configAggregator.getCheckUpdateStatus());
    }
}

