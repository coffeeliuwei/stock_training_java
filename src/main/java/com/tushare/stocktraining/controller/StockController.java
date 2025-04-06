package com.tushare.stocktraining.controller;

import com.tushare.stocktraining.model.Stock;
import com.tushare.stocktraining.model.StockDaily;
import com.tushare.stocktraining.model.StockIndicator;
import com.tushare.stocktraining.service.DataFetcherService;
import com.tushare.stocktraining.service.DataProcessorService;
import com.tushare.stocktraining.service.IndicatorService;
import com.tushare.stocktraining.service.VisualizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 股票数据控制器
 * 提供Web接口访问股票数据和可视化功能
 */
@Controller
@RequestMapping("/")
@Slf4j
public class StockController {

    @Autowired
    private DataFetcherService dataFetcherService;

    @Autowired
    private DataProcessorService dataProcessorService;

    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private VisualizationService visualizationService;

    /**
     * 首页
     */
    /**
     * 获取股票基本信息列表并返回主页
     */
    @GetMapping("/")
    public String index() {
        return "index.html";
    }

    @GetMapping("/api/stock/basic")
    @ResponseBody
    public ResponseEntity<List<Stock>> getStockBasic(
            @RequestParam(required = false) boolean refresh) {
        List<Stock> stocks;
        if (refresh) {
            stocks = dataFetcherService.getStockBasic();
        } else {
            stocks = dataProcessorService.loadStockBasic();
        }
        return ResponseEntity.ok(stocks);
    }
    

    /**
     * 获取股票日线数据
     */
    @GetMapping("/api/stock/daily/{tsCode}")
    @ResponseBody
    public ResponseEntity<List<StockDaily>> getDailyData(
            @PathVariable String tsCode,
            @RequestParam(required = false) boolean refresh,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        // 默认日期范围处理
        if (startDate == null) {
            startDate = LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (endDate == null) {
            endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        
        List<StockDaily> dailyData;
        if (refresh || !dataFetcherService.dailyDataExists(tsCode)) {
            dailyData = dataFetcherService.getDailyData(tsCode, startDate, endDate);
        } else {
            dailyData = dataProcessorService.loadStockData(tsCode);
        }
        
        return ResponseEntity.ok(dailyData);
    }

    /**
     * 获取指定日期范围的股票数据
     */
    @GetMapping("/api/stock/data/{tsCode}")
    @ResponseBody
    public ResponseEntity<List<StockDaily>> getStockData(
            @PathVariable String tsCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        // 默认日期范围：最近一年
        if (startDate == null) {
            startDate = LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (endDate == null) {
            endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        
        List<StockDaily> data = dataProcessorService.prepareDataForVisualization(tsCode, startDate, endDate);
        return ResponseEntity.ok(data);
    }

    /**
     * 计算技术指标
     */
    @GetMapping("/api/stock/indicators/{tsCode}")
    @ResponseBody
    public ResponseEntity<List<StockIndicator>> calculateIndicators(
            @PathVariable String tsCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        // 默认日期范围：最近一年
        if (startDate == null) {
            startDate = LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (endDate == null) {
            endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        
        List<StockDaily> data = dataProcessorService.prepareDataForVisualization(tsCode, startDate, endDate);
        if (data == null || data.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<StockIndicator> indicators = indicatorService.calculateAllIndicators(data);
        return ResponseEntity.ok(indicators);
    }

    /**
     * 生成股票图表HTML
     */
    @GetMapping("/stock/chart/{tsCode}")
    public ResponseEntity<Map<String, Object>> generateChart(
            @PathVariable String tsCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "true") boolean indicators) {
        
        // 默认日期范围：最近一年
        if (startDate == null) {
            startDate = LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (endDate == null) {
            endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        
        Map<String, Object> chartData = visualizationService.generateChartData(tsCode, startDate, endDate, indicators);
        if (chartData == null) {
            return ResponseEntity.notFound().build();
        }
        
        chartData.put("stockName", dataProcessorService.getStockName(tsCode));
        return ResponseEntity.ok(chartData);
    }

    /**
     * 获取股票详细信息
     */
    /**
     * 显示股票详情页面
     */
    @GetMapping("/stock/view/{tsCode}")
    public String viewStock(@PathVariable String tsCode, Model model) {
        model.addAttribute("tsCode", tsCode);
        return "stock_detail";
    }

    /**
     * 获取股票详细信息
     */
    @GetMapping("/api/stock/view/{tsCode}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStockView(
            @PathVariable String tsCode,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        // 默认日期范围：最近一年
        if (startDate == null) {
            startDate = LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (endDate == null) {
            endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        
        // 获取股票数据
        String stockName = dataProcessorService.getStockName(tsCode);
        
        // 检查是否需要更新数据
        if (!dataFetcherService.dailyDataExistsForDateRange(tsCode, startDate, endDate)) {
            log.info("下载股票 {} 从 {} 到 {} 的数据", tsCode, startDate, endDate);
            dataFetcherService.getDailyData(tsCode, startDate, endDate);
        }
        
        // 准备数据
        List<StockDaily> data = dataProcessorService.prepareDataForVisualization(tsCode, startDate, endDate);
        if (data == null || data.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // 计算技术指标
        List<StockIndicator> indicators = indicatorService.calculateAllIndicators(data);
        
        // 使用可视化服务生成图表数据
        Map<String, Object> chartData = visualizationService.generateIndicatorData(data, indicators);
        if (chartData == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 添加股票基本信息
        chartData.put("tsCode", tsCode);
        chartData.put("stockName", stockName);
        chartData.put("startDate", startDate);
        chartData.put("endDate", endDate);
        
        return ResponseEntity.ok(chartData);
    }
    }
