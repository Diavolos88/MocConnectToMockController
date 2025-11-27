package com.mock.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Базовый класс для сервисов, которые хотят подключиться к MockController.
 * Наследуйтесь от этого класса, чтобы автоматически получить функциональность
 * синхронизации конфигурации с MockController.
 * 
 * Каждый наследник должен быть помечен аннотацией @Service.
 * 
 * Вся логика синхронизации с MockController выполняется централизованно
 * через ConfigAggregator, который автоматически находит все сервисы,
 * наследующиеся от этого класса.
 */
public class MockControllerClientBase {
    
    protected static final Logger logger = LoggerFactory.getLogger(MockControllerClientBase.class);
}

