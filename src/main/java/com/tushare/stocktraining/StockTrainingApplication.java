package com.tushare.stocktraining;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Tushare股票数据分析Spring Boot应用
 * 主应用入口类
 */
@SpringBootApplication
public class StockTrainingApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockTrainingApplication.class, args);
    }
}