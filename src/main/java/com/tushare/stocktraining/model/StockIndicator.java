package com.tushare.stocktraining.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 股票技术指标数据模型类
 * 对应Python项目中计算的各种技术指标
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockIndicator {
    private String tsCode;      // TS代码
    private String tradeDate;   // 交易日期
    
    // MACD指标
    private BigDecimal macd;    // MACD值
    private BigDecimal signal;  // MACD信号线
    private BigDecimal hist;    // MACD柱状图
    
    // RSI指标
    private BigDecimal rsi6;    // 6日RSI
    private BigDecimal rsi12;   // 12日RSI
    private BigDecimal rsi24;   // 24日RSI
    
    // KDJ指标
    private BigDecimal k;       // K值
    private BigDecimal d;       // D值
    private BigDecimal j;       // J值
    
    // 布林带指标
    private BigDecimal upper;   // 上轨
    private BigDecimal middle;  // 中轨
    private BigDecimal lower;   // 下轨
    
    // 移动平均线，使用Map存储不同周期的MA值
    private Map<Integer, BigDecimal> maValues; // 键为周期，值为MA值
}