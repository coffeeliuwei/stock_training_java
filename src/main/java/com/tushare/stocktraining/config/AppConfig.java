package com.tushare.stocktraining.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用配置类
 * 对应Python项目中的config.py
 */
@Configuration
@Component
public class AppConfig {

    @Value("${tushare.token}")
    private String token;

    @Value("${tushare.api.url}")
    private String apiUrl;

    @Value("${data.start-date}")
    private String startDate;

    @Value("${data.end-date:#{T(java.time.LocalDate).now().toString()}}")
    private String endDate;

    @Value("${data.dir}")
    private String dataDir;

    @Value("${data.daily-dir}")
    private String dailyDir;

    @Value("${charts.dir}")
    private String chartsDir;

    @Value("${visualization.default-figsize}")
    private String defaultFigsize;

    @Value("${visualization.default-style}")
    private String defaultStyle;

    @Value("${indicators.ma.periods}")
    private String maPeriods;

    @Value("${indicators.macd.enabled}")
    private boolean macdEnabled;

    @Value("${indicators.rsi.enabled}")
    private boolean rsiEnabled;

    @Value("${indicators.kdj.enabled}")
    private boolean kdjEnabled;

    @Value("${indicators.bollinger.enabled}")
    private boolean bollingerEnabled;

    // Getters
    public String getToken() {
        return token;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        // 如果配置为${current.date}，则返回当前日期
        if ("${current.date}".equals(endDate)) {
            return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return endDate;
    }

    public String getDataDir() {
        return dataDir;
    }

    public String getDailyDir() {
        return dailyDir;
    }

    public String getChartsDir() {
        return chartsDir;
    }

    public int[] getDefaultFigsize() {
        String[] sizes = defaultFigsize.split(",");
        return new int[] {Integer.parseInt(sizes[0]), Integer.parseInt(sizes[1])};
    }

    public String getDefaultStyle() {
        return defaultStyle;
    }

    public List<Integer> getMaPeriods() {
        return Arrays.stream(maPeriods.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    public boolean isMacdEnabled() {
        return macdEnabled;
    }

    public boolean isRsiEnabled() {
        return rsiEnabled;
    }

    public boolean isKdjEnabled() {
        return kdjEnabled;
    }

    public boolean isBollingerEnabled() {
        return bollingerEnabled;
    }
}