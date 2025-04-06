# 股票数据分析训练系统使用说明

## 项目概述
本项目是一个基于Tushare数据接口的股票数据分析训练系统，提供股票数据获取、存储、分析和可视化等功能。系统使用Spring Boot框架开发，支持多种技术指标分析和数据可视化展示。

## 环境要求
- JDK 8或以上版本
- Maven 3.6或以上版本
- 可用的互联网连接（用于访问Tushare API）

## 配置说明

### Tushare API配置
在`application.properties`文件中配置以下参数：
- `tushare.token`：您的Tushare API令牌
- `tushare.api.url`：Tushare API地址

### 数据配置
- `data.start-date`：数据获取的起始日期
- `data.end-date`：数据获取的结束日期（留空表示当前日期）
- `data.dir`：数据存储根目录
- `data.daily-dir`：日线数据存储目录
- `charts.dir`：图表输出目录

### 服务器配置
- `server.port`：服务器端口号，默认为8080

### 技术指标配置
系统支持以下技术指标：
- MACD指标
- RSI指标
- KDJ指标
- 布林带指标
- 移动平均线（支持5、10、20、30、60日均线）

## 使用步骤

### 1. 启动系统
在项目根目录下执行以下命令启动系统：
```bash
mvn clean spring-boot:run
```

### 2. 数据获取
系统启动后会自动从Tushare获取配置的时间范围内的股票数据，数据将保存在配置的数据目录中。

### 3. 数据分析
- 日线数据将保存在`data/daily`目录下，以CSV格式存储
- 技术分析图表将生成在`charts`目录下，以HTML格式存储

### 4. 访问API
系统提供REST API接口，默认支持跨域访问：
- 基础URL：`http://localhost:8080/api`
- 支持的HTTP方法：GET、POST、PUT、DELETE、OPTIONS

## 目录结构
```
├── charts/                 # 图表输出目录
├── data/                   # 数据存储目录
│   ├── daily/             # 日线数据目录
│   └── stock_basic.csv    # 股票基本信息
├── src/                    # 源代码目录
└── pom.xml                # Maven配置文件
```

## 开发模式
系统支持热部署，修改代码后会自动重启：
- 支持的文件路径：`src/main/java`
- 静态资源位置：`classpath:/static/`
- 模板缓存已禁用，支持实时更新

## 注意事项
1. 首次运行前请确保已正确配置Tushare API令牌
2. 数据获取可能需要一定时间，请耐心等待
3. 确保系统有足够的磁盘空间存储数据和图表
4. 建议定期备份重要的数据文件