package com.tushare.stocktraining.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tushare.stocktraining.config.AppConfig;
import com.tushare.stocktraining.model.Stock;
import com.tushare.stocktraining.model.StockDaily;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据获取服务
 * 对应Python项目中的data_fetcher.py
 */
@Service
@Slf4j
public class DataFetcherService {

    @Autowired
    private AppConfig config;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 确保数据目录存在
     */
    public void ensureDataDir() {
        try {
            Path dataPath = Paths.get(config.getDataDir());
            Path dailyPath = Paths.get(config.getDailyDir());
            Path chartsPath = Paths.get(config.getChartsDir());

            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
                log.info("Created data directory: {}", dataPath);
            }

            if (!Files.exists(dailyPath)) {
                Files.createDirectories(dailyPath);
                log.info("Created daily data directory: {}", dailyPath);
            }

            if (!Files.exists(chartsPath)) {
                Files.createDirectories(chartsPath);
                log.info("Created charts directory: {}", chartsPath);
            }
        } catch (IOException e) {
            log.error("Failed to create data directories", e);
        }
    }

    /**
     * 获取股票基本信息列表
     * 对应Python项目中的get_stock_basic函数
     *
     * @return 股票基本信息列表
     */
    public List<Stock> getStockBasic() {
        ensureDataDir();

        List<Stock> stocks = new ArrayList<>();
        try {
            // 准备请求参数
            Map<String, Object> params = new HashMap<>();
            params.put("api_name", "stock_basic");
            params.put("token", config.getToken());
            
            Map<String, Object> fields = new HashMap<>();
            fields.put("exchange", "");
            fields.put("list_status", "L");
            fields.put("fields", "ts_code,symbol,name,area,industry,market,list_date,is_hs,delist_date");
            params.put("params", fields);

            // 发送请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);

            String response = restTemplate.postForObject(config.getApiUrl(), request, String.class);
            JsonNode root = objectMapper.readTree(response);

            // 解析响应
            if (root.has("data") && root.get("data").has("items")) {
                JsonNode items = root.get("data").get("items");
                for (JsonNode item : items) {
                    Stock stock = Stock.builder()
                            .tsCode(item.get(0).asText())
                            .symbol(item.get(1).asText())
                            .name(item.get(2).asText())
                            .area(item.get(3).asText())
                            .industry(item.get(4).asText())
                            .market(item.get(5).asText())
                            .listDate(item.get(6).asText())
                            .isHs(item.get(7).asText())
                            .delistDate(item.size() > 8 ? item.get(8).asText() : null)
                            .build();
                    stocks.add(stock);
                }
            }

            // 保存到CSV文件
            saveStocksToCSV(stocks);

        } catch (Exception e) {
            log.error("Failed to fetch stock basic data", e);
        }

        return stocks;
    }

    /**
     * 获取股票日线数据
     * 对应Python项目中的get_daily_data函数
     *
     * @param tsCode 股票代码
     * @return 股票日线数据列表
     */
    public List<StockDaily> getDailyData(String tsCode) {
        return getDailyData(tsCode, config.getStartDate(), config.getEndDate());
    }
    
    /**
     * 获取指定日期范围的股票日线数据
     * @param tsCode 股票代码
     * @param startDateStr 开始日期 (YYYY-MM-DD)
     * @param endDateStr 结束日期 (YYYY-MM-DD)
     * @return 股票日线数据列表
     */
    public List<StockDaily> getDailyData(String tsCode, String startDateStr, String endDateStr) {
        ensureDataDir();

        // 检查股票列表文件是否存在
        Path stockListFile = Paths.get(config.getDataDir(), "stock_basic.csv");
        if (!Files.exists(stockListFile)) {
            log.info("股票列表文件不存在，正在获取...");
            getStockBasic();
        }

        // 检查股票是否在列表中
        boolean stockExists = false;
        try (BufferedReader reader = Files.newBufferedReader(stockListFile)) {
            String line;
            // 跳过表头
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                if (fields.length > 0 && fields[0].equals(tsCode)) {
                    stockExists = true;
                    break;
                }
            }
        } catch (IOException e) {
            log.error("读取股票列表文件失败", e);
        }

        if (!stockExists) {
            log.error("股票代码 {} 不在列表中", tsCode);
            return new ArrayList<>();
        }

        // 检查是否需要增量更新
        String startDate = startDateStr.replace("-", "");
        String endDate = endDateStr.replace("-", "");
        Path filePath = Paths.get(config.getDailyDir(), tsCode + ".csv");
        List<StockDaily> existingData = new ArrayList<>();

        // 检查是否已有相同时间段的CSV文件
        if (Files.exists(filePath)) {
            // 读取现有数据，找到最早和最新日期
            String earliestDate = null;
            String latestDate = null;
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                // 跳过表头
                reader.readLine();
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] fields = line.split(",");
                    if (fields.length >= 11) {
                        String tradeDate = fields[1];
                        if (earliestDate == null || tradeDate.compareTo(earliestDate) < 0) {
                            earliestDate = tradeDate;
                        }
                        if (latestDate == null || tradeDate.compareTo(latestDate) > 0) {
                            latestDate = tradeDate;
                        }
                        
                        // 只加载请求时间范围内的数据
                        if (tradeDate.compareTo(startDate) >= 0 && tradeDate.compareTo(endDate) <= 0) {
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
                            existingData.add(daily);
                        }
                    }
                }
            } catch (IOException e) {
                log.error("读取股票 {} 数据失败: {}", tsCode, e.getMessage());
            }

            // 检查是否已有完整的请求时间范围数据
            if (earliestDate != null && latestDate != null) {
                if (earliestDate.compareTo(startDate) <= 0 && latestDate.compareTo(endDate) >= 0) {
                    log.info("股票 {} 从 {} 到 {} 的数据已存在，无需重复下载", tsCode, startDate, endDate);
                    return existingData;
                }
                
                // 如果只需要更新部分数据，调整开始日期
                if (latestDate.compareTo(startDate) >= 0 && latestDate.compareTo(endDate) < 0) {
                    // 计算最新日期的下一天作为开始日期
                    try {
                        int dateInt = Integer.parseInt(latestDate);
                        dateInt += 1; // 简单加1，可能需要更复杂的日期计算
                        startDate = String.valueOf(dateInt);
                        log.info("股票 {} 已有部分数据，将从 {} 开始增量更新", tsCode, startDate);
                    } catch (NumberFormatException e) {
                        log.error("日期格式转换失败: {}", latestDate, e);
                    }
                }
            }
        }

        log.info("正在下载 {} 从 {} 到 {} 的数据...", tsCode, startDate, endDate);

        List<StockDaily> newData = new ArrayList<>();
        try {
            // 准备请求参数
            Map<String, Object> params = new HashMap<>();
            params.put("api_name", "daily");
            params.put("token", config.getToken());
            
            Map<String, Object> fields = new HashMap<>();
            fields.put("ts_code", tsCode);
            fields.put("start_date", startDate);
            fields.put("end_date", endDate);
            params.put("params", fields);

            // 发送请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(params, headers);

            String response = restTemplate.postForObject(config.getApiUrl(), request, String.class);
            JsonNode root = objectMapper.readTree(response);

            // 解析响应
            if (root.has("data") && root.get("data").has("items")) {
                JsonNode items = root.get("data").get("items");
                for (JsonNode item : items) {
                    StockDaily daily = StockDaily.builder()
                            .tsCode(item.get(0).asText())
                            .tradeDate(item.get(1).asText())
                            .open(new BigDecimal(item.get(2).asText()))
                            .high(new BigDecimal(item.get(3).asText()))
                            .low(new BigDecimal(item.get(4).asText()))
                            .close(new BigDecimal(item.get(5).asText()))
                            .preClose(new BigDecimal(item.get(6).asText()))
                            .change(new BigDecimal(item.get(7).asText()))
                            .pctChg(new BigDecimal(item.get(8).asText()))
                            .vol(new BigDecimal(item.get(9).asText()))
                            .amount(new BigDecimal(item.get(10).asText()))
                            .build();
                    newData.add(daily);
                }
            }

            // 合并新旧数据
            List<StockDaily> mergedData = new ArrayList<>(existingData);
            mergedData.addAll(newData);
            
            // 去重和排序
            List<StockDaily> finalData = mergedData.stream()
                    .collect(Collectors.toMap(
                        StockDaily::getTradeDate,
                        daily -> daily,
                        (existing, replacement) -> replacement))
                    .values()
                    .stream()
                    .sorted(Comparator.comparing(StockDaily::getTradeDate))
                    .collect(Collectors.toList());

            // 保存到CSV文件
            if (!newData.isEmpty()) {
                saveDailyDataToCSV(tsCode, finalData);
                log.info("股票 {} 数据下载完成，共 {} 条记录", tsCode, finalData.size());
            } else {
                log.warn("股票 {} 在指定日期范围内没有新数据", tsCode);
            }

            return finalData;

        } catch (Exception e) {
            log.error("获取股票 {} 日线数据失败", tsCode, e);
            // 如果获取失败但有现有数据，返回现有数据
            if (!existingData.isEmpty()) {
                return existingData;
            }
        }

        return new ArrayList<>();
    }

    /**
     * 将股票基本信息保存到CSV文件
     *
     * @param stocks 股票基本信息列表
     */
    private void saveStocksToCSV(List<Stock> stocks) {
        try {
            Path filePath = Paths.get(config.getDataDir(), "stock_basic.csv");
            StringBuilder csv = new StringBuilder();
            csv.append("ts_code,symbol,name,area,industry,market,list_date,is_hs,delist_date\n");

            for (Stock stock : stocks) {
                csv.append(stock.getTsCode()).append(",")
                   .append(stock.getSymbol()).append(",")
                   .append(stock.getName()).append(",")
                   .append(stock.getArea()).append(",")
                   .append(stock.getIndustry()).append(",")
                   .append(stock.getMarket()).append(",")
                   .append(stock.getListDate()).append(",")
                   .append(stock.getIsHs()).append(",")
                   .append(stock.getDelistDate() != null ? stock.getDelistDate() : "")
                   .append("\n");
            }

            // 使用UTF-8编码保存文件，确保中文字符正确显示
            Files.write(filePath, csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            log.info("Saved {} stocks to {}", stocks.size(), filePath);
        } catch (IOException e) {
            log.error("Failed to save stocks to CSV", e);
        }
    }

    /**
     * 将股票日线数据保存到CSV文件
     *
     * @param tsCode 股票代码
     * @param dailyData 股票日线数据列表
     */
    private void saveDailyDataToCSV(String tsCode, List<StockDaily> dailyData) {
        try {
            Path filePath = Paths.get(config.getDailyDir(), tsCode + ".csv");
            StringBuilder csv = new StringBuilder();
            csv.append("ts_code,trade_date,open,high,low,close,pre_close,change,pct_chg,vol,amount\n");

            for (StockDaily daily : dailyData) {
                csv.append(daily.getTsCode()).append(",")
                   .append(daily.getTradeDate()).append(",")
                   .append(daily.getOpen()).append(",")
                   .append(daily.getHigh()).append(",")
                   .append(daily.getLow()).append(",")
                   .append(daily.getClose()).append(",")
                   .append(daily.getPreClose()).append(",")
                   .append(daily.getChange()).append(",")
                   .append(daily.getPctChg()).append(",")
                   .append(daily.getVol()).append(",")
                   .append(daily.getAmount())
                   .append("\n");
            }

            // 使用UTF-8编码保存文件，确保中文字符正确显示
            Files.write(filePath, csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            log.info("Saved {} daily records for {} to {}", dailyData.size(), tsCode, filePath);
        } catch (IOException e) {
            log.error("Failed to save daily data to CSV for {}", tsCode, e);
        }
    }

    /**
     * 检查股票日线数据文件是否存在
     *
     * @param tsCode 股票代码
     * @return 是否存在
     */
    public boolean dailyDataExists(String tsCode) {
        Path filePath = Paths.get(config.getDailyDir(), tsCode + ".csv");
        return Files.exists(filePath);
    }
    
    /**
     * 检查指定时间段的股票日线数据是否已存在
     * @param tsCode 股票代码
     * @param startDateStr 开始日期 (YYYY-MM-DD)
     * @param endDateStr 结束日期 (YYYY-MM-DD)
     * @return 是否存在完整的指定时间段数据
     */
    public boolean dailyDataExistsForDateRange(String tsCode, String startDateStr, String endDateStr) {
        Path filePath = Paths.get(config.getDailyDir(), tsCode + ".csv");
        if (!Files.exists(filePath)) {
            return false;
        }
        
        // 转换日期格式
        String startDate = startDateStr.replace("-", "");
        String endDate = endDateStr.replace("-", "");
        
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            // 跳过表头
            reader.readLine();
            
            // 读取所有交易日期
            List<String> tradeDates = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                if (fields.length >= 2) {
                    tradeDates.add(fields[1]); // 交易日期在第二列
                }
            }
            
            if (tradeDates.isEmpty()) {
                return false;
            }
            
            // 排序日期
            Collections.sort(tradeDates);
            
            // 检查是否包含指定的日期范围
            String earliestDate = tradeDates.get(0);
            String latestDate = tradeDates.get(tradeDates.size() - 1);
            
            return earliestDate.compareTo(startDate) <= 0 && latestDate.compareTo(endDate) >= 0;
        } catch (IOException e) {
            log.error("检查股票 {} 数据时间范围失败: {}", tsCode, e.getMessage());
            return false;
        }
    }
}