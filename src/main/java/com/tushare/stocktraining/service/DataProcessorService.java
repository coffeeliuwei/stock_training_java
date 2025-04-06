package com.tushare.stocktraining.service;

import com.tushare.stocktraining.config.AppConfig;
import com.tushare.stocktraining.model.Stock;
import com.tushare.stocktraining.model.StockDaily;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据处理服务
 * 对应Python项目中的data_processor.py
 */
@Service
@Slf4j
public class DataProcessorService {

    @Autowired
    private AppConfig config;

    /**
     * 加载指定股票的日线数据
     * 对应Python项目中的load_stock_data函数
     *
     * @param tsCode 股票代码
     * @return 股票日线数据列表
     */
    public List<StockDaily> loadStockData(String tsCode) {
        Path filePath = Paths.get(config.getDailyDir(), tsCode + ".csv");
        if (!Files.exists(filePath)) {
            log.warn("股票 {} 的数据文件不存在", tsCode);
            return null;
        }

        List<StockDaily> dailyData = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            // 跳过表头
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                if (fields.length >= 11) {
                    StockDaily daily = StockDaily.builder()
                            .tsCode(fields[0])
                            .tradeDate(fields[1])
                            .open(new BigDecimal(fields[2]))
                            .high(new BigDecimal(fields[3]))
                            .low(new BigDecimal(fields[4]))
                            .close(new BigDecimal(fields[5]))
                            .preClose(new BigDecimal(fields[6]))
                            .change(new BigDecimal(fields[7]))
                            .pctChg(new BigDecimal(fields[8]))
                            .vol(new BigDecimal(fields[9]))
                            .amount(new BigDecimal(fields[10]))
                            .build();
                    dailyData.add(daily);
                }
            }
        } catch (IOException e) {
            log.error("读取股票 {} 数据失败: {}", tsCode, e.getMessage());
            return null;
        }

        return dailyData;
    }

    /**
     * 处理股票数据，进行必要的转换和清洗
     * 对应Python项目中的process_stock_data函数
     *
     * @param dailyData 原始股票数据列表
     * @return 处理后的数据列表
     */
    public List<StockDaily> processStockData(List<StockDaily> dailyData) {
        if (dailyData == null || dailyData.isEmpty()) {
            return null;
        }

        // 按交易日期排序（从旧到新）
        return dailyData.stream()
                .sorted(Comparator.comparing(StockDaily::getTradeDate))
                .collect(Collectors.toList());
    }

    /**
     * 准备用于可视化的数据
     * 对应Python项目中的prepare_data_for_visualization函数
     *
     * @param tsCode 股票代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 处理后的数据列表
     */
    public List<StockDaily> prepareDataForVisualization(String tsCode, String startDate, String endDate) {
        // 加载数据
        List<StockDaily> dailyData = loadStockData(tsCode);
        if (dailyData == null) {
            return null;
        }

        // 处理数据
        List<StockDaily> processedData = processStockData(dailyData);
        if (processedData == null) {
            return null;
        }

        // 过滤日期范围
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        
        // 如果未提供日期参数，使用默认范围
        final LocalDate start = startDate != null && !startDate.trim().isEmpty() ?
                LocalDate.parse(startDate.replace("-", ""), formatter) :
                LocalDate.now().minusYears(1); // 默认显示最近一年的数据
        
        final LocalDate end = endDate != null && !endDate.trim().isEmpty() ?
                LocalDate.parse(endDate.replace("-", ""), formatter) :
                LocalDate.now();

        return processedData.stream()
                .filter(daily -> {
                    LocalDate tradeDate = LocalDate.parse(daily.getTradeDate(), formatter);
                    return !tradeDate.isBefore(start) && !tradeDate.isAfter(end);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取股票名称
     * 对应Python项目中的get_stock_name函数
     *
     * @param tsCode 股票代码
     * @return 股票名称
     */
    public String getStockName(String tsCode) {
        Path filePath = Paths.get(config.getDataDir(), "stock_basic.csv");
        if (!Files.exists(filePath)) {
            log.warn("股票基本信息文件不存在");
            return tsCode;
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            // 跳过表头
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                if (fields.length >= 3 && fields[0].equals(tsCode)) {
                    return fields[2]; // 返回股票名称
                }
            }
        } catch (IOException e) {
            log.error("读取股票基本信息失败: {}", e.getMessage());
        }

        return tsCode; // 如果找不到，返回股票代码
    }

    /**
     * 加载所有股票的基本信息
     *
     * @return 股票基本信息列表
     */
    public List<Stock> loadStockBasic() {
        Path filePath = Paths.get(config.getDataDir(), "stock_basic.csv");
        if (!Files.exists(filePath)) {
            log.warn("股票基本信息文件不存在");
            return new ArrayList<>();
        }

        List<Stock> stocks = new ArrayList<>();
        try {
            // 尝试使用UTF-8编码读取文件
            try (BufferedReader reader = Files.newBufferedReader(filePath, java.nio.charset.StandardCharsets.UTF_8)) {
                // 跳过表头
                String line = reader.readLine();
                if (line == null) {
                    log.warn("股票基本信息文件为空");
                    return stocks;
                }
                
                while ((line = reader.readLine()) != null) {
                    // 跳过空行
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    
                    try {
                        String[] fields = line.split(",");
                        if (fields.length >= 8) {
                            Stock stock = Stock.builder()
                                    .tsCode(fields[0])
                                    .symbol(fields[1])
                                    .name(fields[2])
                                    .area(fields[3])
                                    .industry(fields[4])
                                    .market(fields[5])
                                    .listDate(fields[6])
                                    .isHs(fields[7])
                                    .delistDate(fields.length > 8 ? fields[8] : null)
                                    .build();
                            stocks.add(stock);
                        } else {
                            log.warn("股票基本信息格式不正确: {}", line);
                        }
                    } catch (Exception ex) {
                        log.warn("处理股票基本信息行时出错: {}, 错误: {}", line, ex.getMessage());
                    }
                }
            } catch (java.nio.charset.MalformedInputException encodingEx) {
                // 如果UTF-8解码失败，尝试使用系统默认编码
                log.warn("使用UTF-8编码读取文件失败，尝试使用系统默认编码");
                try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                    // 跳过表头
                    String line = reader.readLine();
                    if (line == null) {
                        log.warn("股票基本信息文件为空");
                        return stocks;
                    }
                    
                    while ((line = reader.readLine()) != null) {
                        // 跳过空行
                        if (line.trim().isEmpty()) {
                            continue;
                        }
                        
                        try {
                            String[] fields = line.split(",");
                            if (fields.length >= 8) {
                                Stock stock = Stock.builder()
                                        .tsCode(fields[0])
                                        .symbol(fields[1])
                                        .name(fields[2])
                                        .area(fields[3])
                                        .industry(fields[4])
                                        .market(fields[5])
                                        .listDate(fields[6])
                                        .isHs(fields[7])
                                        .delistDate(fields.length > 8 ? fields[8] : null)
                                        .build();
                                stocks.add(stock);
                            } else {
                                log.warn("股票基本信息格式不正确: {}", line);
                            }
                        } catch (Exception lineEx) {
                            log.warn("处理股票基本信息行时出错: {}, 错误: {}", line, lineEx.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("读取股票基本信息失败: {}", e.getMessage());
        }

        return stocks;
    }
}