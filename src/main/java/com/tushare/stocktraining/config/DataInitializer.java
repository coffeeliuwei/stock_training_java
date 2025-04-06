package com.tushare.stocktraining.config;

import com.tushare.stocktraining.service.DataFetcherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 数据初始化组件
 * 在应用启动时自动获取股票基本信息
 */
@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private DataFetcherService dataFetcherService;

    @Override
    public void run(String... args) {
        log.info("开始初始化股票基本信息数据...");
        try {
            // 确保数据目录存在
            dataFetcherService.ensureDataDir();
            
            // 获取股票基本信息
            dataFetcherService.getStockBasic();
            
            log.info("股票基本信息数据初始化完成");
        } catch (Exception e) {
            log.error("初始化股票基本信息数据失败", e);
        }
    }
}