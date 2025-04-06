package com.tushare.stocktraining.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 股票日线数据模型类
 * 对应Python项目中的股票日线数据结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDaily {
    private String tsCode;      // TS代码
    private String tradeDate;   // 交易日期
    private BigDecimal open;    // 开盘价
    private BigDecimal high;    // 最高价
    private BigDecimal low;     // 最低价
    private BigDecimal close;   // 收盘价
    private BigDecimal preClose;// 昨收价
    private BigDecimal change;  // 涨跌额
    private BigDecimal pctChg;  // 涨跌幅 （未乘以100）
    private BigDecimal vol;     // 成交量 （手）
    private BigDecimal amount;  // 成交额 （千元）
}