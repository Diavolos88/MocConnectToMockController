package com.mock.service;

import com.mock.config.MockControllerClientBase;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserService extends MockControllerClientBase {
    
    // Параметры для User Login эндпоинта
    private long delayUserLogin = 1200; // задержка в миллисекундах
    private String stringUserLoginResponse = "User logged in successfully";
    private int intUserLoginStatusCode = 200; // HTTP статус код для login
    private int intUserId = 12345; // Пример числового значения
    
    // Параметры для Data Fetch эндпоинта
    private long delayDataFetch = 600; // задержка в миллисекундах
    private String stringDataFetchResult = "Data retrieved";
    private int intDataFetchStatusCode = 200; // HTTP статус код
    private boolean isDataAvailable = true; // Boolean параметр для data
    
    // Примеры переменных с большой буквы
    @SuppressWarnings("unused")
    private long DELAY_USER_ACTION = 1500; // задержка с большой буквы
    @SuppressWarnings("unused")
    private String STRING_USER_STATUS = "ACTIVE"; // строка с большой буквы
    @SuppressWarnings("unused")
    private int INT_USER_ID = 99999; // int с большой буквы
    @SuppressWarnings("unused")
    private boolean IS_USER_ACTIVE = true; // boolean с большой буквы
    
    public ResponseEntity<Map<String, String>> getUserLoginResponse() {
        try {
            Thread.sleep(delayUserLogin);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("result", stringUserLoginResponse);
        response.put("userId", String.valueOf(intUserId));
        return ResponseEntity.status(intUserLoginStatusCode).body(response);
    }
    
    public ResponseEntity<Map<String, String>> getDataFetchResponse() {
        try {
            Thread.sleep(delayDataFetch);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("data", stringDataFetchResult);
        response.put("isAvailable", String.valueOf(isDataAvailable));
        return ResponseEntity.status(intDataFetchStatusCode).body(response);
    }
    
    // Геттеры и сеттеры для параметров User Login
    public long getDelayUserLogin() {
        return delayUserLogin;
    }
    
    public void setDelayUserLogin(long delayUserLogin) {
        this.delayUserLogin = delayUserLogin;
    }
    
    public String getStringUserLoginResponse() {
        return stringUserLoginResponse;
    }
    
    public void setStringUserLoginResponse(String stringUserLoginResponse) {
        this.stringUserLoginResponse = stringUserLoginResponse;
    }
    
    // Геттеры и сеттеры для параметров Data Fetch
    public long getDelayDataFetch() {
        return delayDataFetch;
    }
    
    public void setDelayDataFetch(long delayDataFetch) {
        this.delayDataFetch = delayDataFetch;
    }
    
    public String getStringDataFetchResult() {
        return stringDataFetchResult;
    }
    
    public void setStringDataFetchResult(String stringDataFetchResult) {
        this.stringDataFetchResult = stringDataFetchResult;
    }
    
    // Геттеры и сеттеры для int параметров
    public int getIntDataFetchStatusCode() {
        return intDataFetchStatusCode;
    }
    
    public void setIntDataFetchStatusCode(int intDataFetchStatusCode) {
        this.intDataFetchStatusCode = intDataFetchStatusCode;
    }
    
    public int getIntUserLoginStatusCode() {
        return intUserLoginStatusCode;
    }
    
    public void setIntUserLoginStatusCode(int intUserLoginStatusCode) {
        this.intUserLoginStatusCode = intUserLoginStatusCode;
    }
    
    public int getIntUserId() {
        return intUserId;
    }
    
    public void setIntUserId(int intUserId) {
        this.intUserId = intUserId;
    }
    
    // Геттеры и сеттеры для boolean параметров
    public boolean isDataAvailable() {
        return isDataAvailable;
    }
    
    public void setDataAvailable(boolean dataAvailable) {
        isDataAvailable = dataAvailable;
    }
}

