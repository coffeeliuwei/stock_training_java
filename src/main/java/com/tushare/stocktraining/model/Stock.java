package com.tushare.stocktraining.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 股票基本信息模型类
 * 对应Python项目中的股票基本信息数据结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stock {
    private String tsCode;     // TS代码
    private String symbol;     // 股票代码
    private String name;       // 股票名称
    private String area;       // 地区
    private String industry;   // 行业
    private String market;     // 市场类型（主板/创业板/科创板/CDR）
    private String listDate;   // 上市日期
    private String isHs;       // 是否沪深港通标的，N否 H沪股通 S深股通
    private String delistDate; // 退市日期
}