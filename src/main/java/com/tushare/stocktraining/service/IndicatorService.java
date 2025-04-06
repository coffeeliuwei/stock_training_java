package com.tushare.stocktraining.service;

import com.tushare.stocktraining.config.AppConfig;
import com.tushare.stocktraining.model.StockDaily;
import com.tushare.stocktraining.model.StockIndicator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 技术指标计算服务
 * 对应Python项目中的indicators.py
 */
@Service
@Slf4j
public class IndicatorService {

    @Autowired
    private AppConfig config;

    /**
     * 计算移动平均线
     * 对应Python项目中的calculate_ma函数
     *
     * @param dailyData 股票日线数据列表
     * @param periods 移动平均的周期列表
     * @return 添加了移动平均线的股票指标列表
     */
    public List<StockIndicator> calculateMA(List<StockDaily> dailyData, List<Integer> periods) {
        if (dailyData == null || dailyData.isEmpty()) {
            return new ArrayList<>();
        }

        List<StockIndicator> result = new ArrayList<>();
        int size = dailyData.size();

        // 初始化结果列表
        for (int i = 0; i < size; i++) {
            StockDaily daily = dailyData.get(i);
            StockIndicator indicator = new StockIndicator();
            indicator.setTsCode(daily.getTsCode());
            indicator.setTradeDate(daily.getTradeDate());
            indicator.setMaValues(new HashMap<>());
            result.add(indicator);
        }

        // 计算各周期的MA
        for (Integer period : periods) {
            for (int i = 0; i < size; i++) {
                if (i >= period - 1) {
                    BigDecimal sum = BigDecimal.ZERO;
                    for (int j = 0; j < period; j++) {
                        sum = sum.add(dailyData.get(i - j).getClose());
                    }
                    BigDecimal ma = sum.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
                    result.get(i).getMaValues().put(period, ma);
                }
            }
        }

        return result;
    }

    /**
     * 计算MACD指标
     * 对应Python项目中的calculate_macd函数
     *
     * @param dailyData 股票日线数据列表
     * @param fastPeriod 快线周期
     * @param slowPeriod 慢线周期
     * @param signalPeriod 信号线周期
     * @return 添加了MACD指标的股票指标列表
     */
    public List<StockIndicator> calculateMACD(List<StockDaily> dailyData, int fastPeriod, int slowPeriod, int signalPeriod) {
        if (dailyData == null || dailyData.isEmpty()) {
            return new ArrayList<>();
        }

        List<StockIndicator> result = new ArrayList<>();
        int size = dailyData.size();

        // 计算EMA
        BigDecimal[] emaFast = new BigDecimal[size];
        BigDecimal[] emaSlow = new BigDecimal[size];
        BigDecimal[] macd = new BigDecimal[size];
        BigDecimal[] signal = new BigDecimal[size];
        BigDecimal[] hist = new BigDecimal[size];

        // 初始化
        emaFast[0] = dailyData.get(0).getClose();
        emaSlow[0] = dailyData.get(0).getClose();

        // 计算EMA和MACD
        for (int i = 1; i < size; i++) {
            BigDecimal close = dailyData.get(i).getClose();
            
            // 计算快线EMA
            BigDecimal fastMultiplier = BigDecimal.valueOf(2.0 / (fastPeriod + 1));
            emaFast[i] = close.multiply(fastMultiplier)
                    .add(emaFast[i-1].multiply(BigDecimal.ONE.subtract(fastMultiplier)));
            
            // 计算慢线EMA
            BigDecimal slowMultiplier = BigDecimal.valueOf(2.0 / (slowPeriod + 1));
            emaSlow[i] = close.multiply(slowMultiplier)
                    .add(emaSlow[i-1].multiply(BigDecimal.ONE.subtract(slowMultiplier)));
            
            // 计算MACD线
            macd[i] = emaFast[i].subtract(emaSlow[i]);
        }

        // 初始化信号线和柱状图的第一个值
        if (macd[0] == null) {
            macd[0] = BigDecimal.ZERO;
        }
        signal[0] = macd[0];
        hist[0] = BigDecimal.ZERO;

        // 计算信号线和柱状图
        for (int i = 1; i < size; i++) {
            // 确保MACD值不为null
            if (macd[i] == null) {
                macd[i] = BigDecimal.ZERO;
            }
            
            // 确保前一个信号值不为null
            if (signal[i-1] == null) {
                signal[i-1] = BigDecimal.ZERO;
            }
            
            BigDecimal signalMultiplier = BigDecimal.valueOf(2.0 / (signalPeriod + 1));
            signal[i] = macd[i].multiply(signalMultiplier)
                    .add(signal[i-1].multiply(BigDecimal.ONE.subtract(signalMultiplier)));
            
            // 计算柱状图
            hist[i] = macd[i].subtract(signal[i]);
        }

        // 构建结果
        for (int i = 0; i < size; i++) {
            StockDaily daily = dailyData.get(i);
            StockIndicator indicator = new StockIndicator();
            indicator.setTsCode(daily.getTsCode());
            indicator.setTradeDate(daily.getTradeDate());
            indicator.setMacd(macd[i]);
            indicator.setSignal(signal[i]);
            indicator.setHist(hist[i]);
            result.add(indicator);
        }

        return result;
    }

    /**
     * 计算RSI指标
     * 对应Python项目中的calculate_rsi函数
     *
     * @param dailyData 股票日线数据列表
     * @param periods RSI周期列表，默认为[6, 12, 24]
     * @return 添加了RSI指标的股票指标列表
     */
    public List<StockIndicator> calculateRSI(List<StockDaily> dailyData, int[] periods) {
        if (dailyData == null || dailyData.isEmpty()) {
            return new ArrayList<>();
        }

        List<StockIndicator> result = new ArrayList<>();
        int size = dailyData.size();

        // 计算价格变化
        BigDecimal[] changes = new BigDecimal[size];
        changes[0] = BigDecimal.ZERO;
        for (int i = 1; i < size; i++) {
            changes[i] = dailyData.get(i).getClose().subtract(dailyData.get(i-1).getClose());
        }

        // 计算各周期的RSI
        for (int period : periods) {
            BigDecimal[] rsi = new BigDecimal[size];
            BigDecimal[] avgGain = new BigDecimal[size];
            BigDecimal[] avgLoss = new BigDecimal[size];

            // 初始化
            avgGain[0] = BigDecimal.ZERO;
            avgLoss[0] = BigDecimal.ZERO;
            rsi[0] = BigDecimal.valueOf(50); // 默认值

            // 计算第一个周期的平均涨跌
            if (size > period) {
                BigDecimal sumGain = BigDecimal.ZERO;
                BigDecimal sumLoss = BigDecimal.ZERO;
                for (int i = 1; i <= period; i++) {
                    if (changes[i].compareTo(BigDecimal.ZERO) > 0) {
                        sumGain = sumGain.add(changes[i]);
                    } else {
                        sumLoss = sumLoss.add(changes[i].abs());
                    }
                }
                avgGain[period] = sumGain.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
                avgLoss[period] = sumLoss.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);

                // 计算RSI
                if (avgLoss[period].compareTo(BigDecimal.ZERO) == 0) {
                    rsi[period] = BigDecimal.valueOf(100);
                } else {
                    BigDecimal rs = avgGain[period].divide(avgLoss[period], 4, RoundingMode.HALF_UP);
                    rsi[period] = BigDecimal.valueOf(100).subtract(
                            BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 4, RoundingMode.HALF_UP));
                }

                // 计算后续周期的RSI
                for (int i = period + 1; i < size; i++) {
                    // 更新平均涨跌
                    BigDecimal gain = changes[i].compareTo(BigDecimal.ZERO) > 0 ? changes[i] : BigDecimal.ZERO;
                    BigDecimal loss = changes[i].compareTo(BigDecimal.ZERO) < 0 ? changes[i].abs() : BigDecimal.ZERO;

                    avgGain[i] = avgGain[i-1].multiply(BigDecimal.valueOf(period - 1))
                            .add(gain).divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
                    avgLoss[i] = avgLoss[i-1].multiply(BigDecimal.valueOf(period - 1))
                            .add(loss).divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);

                    // 计算RSI
                    if (avgLoss[i].compareTo(BigDecimal.ZERO) == 0) {
                        rsi[i] = BigDecimal.valueOf(100);
                    } else {
                        BigDecimal rs = avgGain[i].divide(avgLoss[i], 4, RoundingMode.HALF_UP);
                        rsi[i] = BigDecimal.valueOf(100).subtract(
                                BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 4, RoundingMode.HALF_UP));
                    }
                }
            }

            // 添加RSI值到结果
            for (int i = 0; i < size; i++) {
                if (i >= result.size()) {
                    StockDaily daily = dailyData.get(i);
                    StockIndicator indicator = new StockIndicator();
                    indicator.setTsCode(daily.getTsCode());
                    indicator.setTradeDate(daily.getTradeDate());
                    result.add(indicator);
                }
                
                StockIndicator indicator = result.get(i);
                if (period == 6) {
                    indicator.setRsi6(i < rsi.length ? rsi[i] : null);
                } else if (period == 12) {
                    indicator.setRsi12(i < rsi.length ? rsi[i] : null);
                } else if (period == 24) {
                    indicator.setRsi24(i < rsi.length ? rsi[i] : null);
                }
            }
        }

        return result;
    }

    /**
     * 计算KDJ指标
     * 对应Python项目中的calculate_kdj函数
     *
     * @param dailyData 股票日线数据列表
     * @param period KDJ周期
     * @return 添加了KDJ指标的股票指标列表
     */
    public List<StockIndicator> calculateKDJ(List<StockDaily> dailyData, int period) {
        if (dailyData == null || dailyData.isEmpty()) {
            return new ArrayList<>();
        }

        List<StockIndicator> result = new ArrayList<>();
        int size = dailyData.size();

        // 计算最高价和最低价的周期内最大值和最小值
        BigDecimal[] highestHigh = new BigDecimal[size];
        BigDecimal[] lowestLow = new BigDecimal[size];

        for (int i = 0; i < size; i++) {
            int startIdx = Math.max(0, i - period + 1);
            BigDecimal high = dailyData.get(i).getHigh();
            BigDecimal low = dailyData.get(i).getLow();

            for (int j = startIdx; j < i; j++) {
                high = high.max(dailyData.get(j).getHigh());
                low = low.min(dailyData.get(j).getLow());
            }

            highestHigh[i] = high;
            lowestLow[i] = low;
        }

        // 计算KDJ
        BigDecimal[] k = new BigDecimal[size];
        BigDecimal[] d = new BigDecimal[size];
        BigDecimal[] j = new BigDecimal[size];

        // 初始化
        k[0] = BigDecimal.valueOf(50);
        d[0] = BigDecimal.valueOf(50);
        j[0] = BigDecimal.valueOf(50);

        for (int i = 1; i < size; i++) {
            // 计算RSV
            BigDecimal close = dailyData.get(i).getClose();
            BigDecimal rsv;
            if (highestHigh[i].equals(lowestLow[i])) {
                rsv = BigDecimal.valueOf(50);
            } else {
                rsv = close.subtract(lowestLow[i])
                        .multiply(BigDecimal.valueOf(100))
                        .divide(highestHigh[i].subtract(lowestLow[i]), 4, RoundingMode.HALF_UP);
            }

            // 计算KDJ
            k[i] = k[i-1].multiply(BigDecimal.valueOf(2))
                    .add(rsv).divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP);
            d[i] = d[i-1].multiply(BigDecimal.valueOf(2))
                    .add(k[i]).divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP);
            j[i] = k[i].multiply(BigDecimal.valueOf(3))
                    .subtract(d[i].multiply(BigDecimal.valueOf(2)));
        }

        // 构建结果
        for (int i = 0; i < size; i++) {
            StockDaily daily = dailyData.get(i);
            StockIndicator indicator = new StockIndicator();
            indicator.setTsCode(daily.getTsCode());
            indicator.setTradeDate(daily.getTradeDate());
            indicator.setK(k[i]);
            indicator.setD(d[i]);
            indicator.setJ(j[i]);
            result.add(indicator);
        }

        return result;
    }

    /**
     * 计算布林带指标
     * 对应Python项目中的calculate_bollinger_bands函数
     *
     * @param dailyData 股票日线数据列表
     * @param period 布林带周期
     * @param stdDev 标准差倍数
     * @return 添加了布林带指标的股票指标列表
     */
    public List<StockIndicator> calculateBollingerBands(List<StockDaily> dailyData, int period, double stdDev) {
        if (dailyData == null || dailyData.isEmpty()) {
            return new ArrayList<>();
        }

        List<StockIndicator> result = new ArrayList<>();
        int size = dailyData.size();

        // 计算移动平均线
        BigDecimal[] middle = new BigDecimal[size];
        BigDecimal[] upper = new BigDecimal[size];
        BigDecimal[] lower = new BigDecimal[size];

        for (int i = 0; i < size; i++) {
            if (i >= period - 1) {
                // 计算移动平均
                BigDecimal sum = BigDecimal.ZERO;
                for (int j = 0; j < period; j++) {
                    sum = sum.add(dailyData.get(i - j).getClose());
                }
                middle[i] = sum.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);

                // 计算标准差
                DescriptiveStatistics stats = new DescriptiveStatistics();
                for (int j = 0; j < period; j++) {
                    stats.addValue(dailyData.get(i - j).getClose().doubleValue());
                }
                double standardDeviation = stats.getStandardDeviation();

                // 计算上下轨
                upper[i] = middle[i].add(BigDecimal.valueOf(standardDeviation * stdDev));
                lower[i] = middle[i].subtract(BigDecimal.valueOf(standardDeviation * stdDev));
            } else {
                // 不足周期的数据点
                middle[i] = dailyData.get(i).getClose();
                upper[i] = middle[i];
                lower[i] = middle[i];
            }
        }

        // 构建结果
        for (int i = 0; i < size; i++) {
            StockDaily daily = dailyData.get(i);
            StockIndicator indicator = new StockIndicator();
            indicator.setTsCode(daily.getTsCode());
            indicator.setTradeDate(daily.getTradeDate());
            indicator.setMiddle(middle[i]);
            indicator.setUpper(upper[i]);
            indicator.setLower(lower[i]);
            result.add(indicator);
        }

        return result;
    }

    /**
     * 计算所有技术指标
     * 对应Python项目中的calculate_all_indicators函数
     *
     * @param dailyData 股票日线数据列表
     * @return 添加了所有技术指标的股票指标列表
     */
    public List<StockIndicator> calculateAllIndicators(List<StockDaily> dailyData) {
        if (dailyData == null || dailyData.isEmpty()) {
            return new ArrayList<>();
        }

        // 计算移动平均线
        List<StockIndicator> maResult = calculateMA(dailyData, config.getMaPeriods());

        // 计算MACD
        List<StockIndicator> macdResult = calculateMACD(dailyData, 12, 26, 9);

        // 计算RSI
        List<StockIndicator> rsiResult = calculateRSI(dailyData, new int[]{6, 12, 24});

        // 计算KDJ
        List<StockIndicator> kdjResult = calculateKDJ(dailyData, 9);

        // 计算布林带
        List<StockIndicator> bollResult = calculateBollingerBands(dailyData, 20, 2.0);

        // 合并结果
        List<StockIndicator> result = new ArrayList<>();
        int size = dailyData.size();

        for (int i = 0; i < size; i++) {
            StockDaily daily = dailyData.get(i);
            StockIndicator indicator = new StockIndicator();
            indicator.setTsCode(daily.getTsCode());
            indicator.setTradeDate(daily.getTradeDate());

            // 合并MA
            if (i < maResult.size()) {
                indicator.setMaValues(maResult.get(i).getMaValues());
            }

            // 合并MACD
            if (i < macdResult.size()) {
                indicator.setMacd(macdResult.get(i).getMacd());
                indicator.setSignal(macdResult.get(i).getSignal());
                indicator.setHist(macdResult.get(i).getHist());
            }

            // 合并RSI
            if (i < rsiResult.size()) {
                indicator.setRsi6(rsiResult.get(i).getRsi6());
                indicator.setRsi12(rsiResult.get(i).getRsi12());
                indicator.setRsi24(rsiResult.get(i).getRsi24());
            }

            // 合并KDJ
            if (i < kdjResult.size()) {
                indicator.setK(kdjResult.get(i).getK());
                indicator.setD(kdjResult.get(i).getD());
                indicator.setJ(kdjResult.get(i).getJ());
            }

            // 合并布林带
            if (i < bollResult.size()) {
                indicator.setUpper(bollResult.get(i).getUpper());
                indicator.setMiddle(bollResult.get(i).getMiddle());
                indicator.setLower(bollResult.get(i).getLower());
            }

            result.add(indicator);
        }

        return result;
    }
}