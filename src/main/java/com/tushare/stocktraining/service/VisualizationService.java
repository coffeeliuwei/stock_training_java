package com.tushare.stocktraining.service;

import com.tushare.stocktraining.model.StockDaily;
import com.tushare.stocktraining.model.StockIndicator;
import com.tushare.stocktraining.service.DataProcessorService;
import com.tushare.stocktraining.service.IndicatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 可视化服务
 * 提供前端ECharts所需的数据格式
 */
@Service
@Slf4j
public class VisualizationService {

    @Autowired
    private DataProcessorService dataProcessorService;

    @Autowired
    private IndicatorService indicatorService;

    /**
     * 生成K线图数据
     * @param dailyData 股票日线数据列表
     * @return ECharts所需的数据格式
     */
    public Map<String, Object> generateCandlestickData(List<StockDaily> dailyData) {
        if (dailyData == null || dailyData.isEmpty()) {
            log.error("数据为空，无法生成K线图数据");
            return null;
        }

        List<String> dates = new ArrayList<>();
        List<List<Double>> candlestickData = new ArrayList<>();
        List<Double> volumes = new ArrayList<>();

        for (StockDaily daily : dailyData) {
            // 添加日期
            dates.add(daily.getTradeDate());
            
            // 添加K线数据 [open, close, low, high]
            List<Double> item = new ArrayList<>();
            item.add(daily.getOpen().doubleValue());
            item.add(daily.getClose().doubleValue());
            item.add(daily.getLow().doubleValue());
            item.add(daily.getHigh().doubleValue());
            candlestickData.add(item);
            
            // 添加成交量数据
            volumes.add(daily.getVol().doubleValue());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("candlestickData", candlestickData);
        result.put("volumes", volumes);

        return result;
    }

    /**
     * 生成带技术指标的K线图数据
     * @param dailyData 股票日线数据列表
     * @param indicators 技术指标数据列表
     * @return ECharts所需的数据格式
     */
    public Map<String, Object> generateIndicatorData(List<StockDaily> dailyData, List<StockIndicator> indicators) {
        if (dailyData == null || dailyData.isEmpty() || indicators == null || indicators.isEmpty()) {
            log.error("数据为空，无法生成图表数据");
            return null;
        }

        // 获取基础K线图数据
        Map<String, Object> baseData = generateCandlestickData(dailyData);

        // 添加MA数据
        List<Map<String, Object>> maData = new ArrayList<>();
        if (indicators.get(0).getMaValues() != null && !indicators.get(0).getMaValues().isEmpty()) {
            for (Map.Entry<Integer, BigDecimal> entry : indicators.get(0).getMaValues().entrySet()) {
                int period = entry.getKey();
                List<Double> values = new ArrayList<>();
                for (StockIndicator indicator : indicators) {
                    Map<Integer, BigDecimal> maValues = indicator.getMaValues();
                    if (maValues != null && maValues.containsKey(period)) {
                        values.add(maValues.get(period).doubleValue());
                    } else {
                        values.add(null);
                    }
                }
                Map<String, Object> ma = new HashMap<>();
                ma.put("period", period);
                ma.put("values", values);
                maData.add(ma);
            }
        }

        // 添加MACD数据
        Map<String, List<Double>> macdData = new HashMap<>();
        if (indicators.get(0).getMacd() != null) {
            List<Double> macd = new ArrayList<>();
            List<Double> signal = new ArrayList<>();
            List<Double> hist = new ArrayList<>();

            for (StockIndicator indicator : indicators) {
                if (indicator.getMacd() != null) {
                    macd.add(indicator.getMacd().doubleValue());
                    signal.add(indicator.getSignal().doubleValue());
                    hist.add(indicator.getHist().doubleValue());
                }
            }

            macdData.put("macd", macd);
            macdData.put("signal", signal);
            macdData.put("hist", hist);
        }

        // 组合所有数据
        baseData.put("ma", maData);
        baseData.put("macd", macdData);

        return baseData;
    }

    /**
     * 生成股票图表数据
     * @param tsCode 股票代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param showIndicators 是否显示技术指标
     * @return 图表数据
     */
    public Map<String, Object> generateChartData(String tsCode, String startDate, String endDate, boolean showIndicators) {
        // 准备数据
        List<StockDaily> dailyData = dataProcessorService.prepareDataForVisualization(tsCode, startDate, endDate);
        if (dailyData == null || dailyData.isEmpty()) {
            log.error("无法获取股票数据");
            return null;
        }
        
        if (showIndicators) {
            // 计算所有技术指标
            List<StockIndicator> indicators = indicatorService.calculateAllIndicators(dailyData);
            return generateIndicatorData(dailyData, indicators);
        } else {
            return generateCandlestickData(dailyData);
        }
    }
}