// 获取URL参数
function getUrlParams() {
    const params = new URLSearchParams(window.location.search);
    return {
        tsCode: params.get('tsCode'),
        startDate: params.get('startDate'),
        endDate: params.get('endDate')
    };
}

// 初始化页面
async function initPage() {
    const params = getUrlParams();
    if (!params.tsCode) {
        alert('股票代码不能为空');
        window.location.href = '/';
        return;
    }

    try {
        // 获取股票数据
        const [stockData, indicators] = await Promise.all([
            fetch(`/api/stock/data/${params.tsCode}?startDate=${params.startDate}&endDate=${params.endDate}`).then(res => res.json()),
            fetch(`/api/stock/indicators/${params.tsCode}?startDate=${params.startDate}&endDate=${params.endDate}`).then(res => res.json())
        ]);

        // 更新页面标题和日期范围
        document.getElementById('stockTitle').textContent = `${stockData[0]?.name || ''} (${params.tsCode})`;
        document.getElementById('dateRange').textContent = `日期范围: ${params.startDate} 至 ${params.endDate}`;

        // 渲染K线图
        renderKlineChart(stockData, indicators);

        // 渲染数据表格
        renderDataTable(stockData);
    } catch (error) {
        console.error('获取数据失败:', error);
        alert('获取数据失败，请稍后重试');
    }
}

// 渲染K线图
function renderKlineChart(stockData, indicators) {
    const chart = echarts.init(document.getElementById('klineChart'));

    // 准备数据
    const data = stockData.map(item => ([
        item.tradeDate,
        item.open,
        item.close,
        item.low,
        item.high,
        item.vol,
        item.amount
    ]));

    // 计算MA数据
    const ma5Data = calculateMA(5, data);
    const ma10Data = calculateMA(10, data);
    const ma20Data = calculateMA(20, data);

    // 配置项
    const option = {
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'cross'
            }
        },
        legend: {
            data: ['K线', 'MA5', 'MA10', 'MA20', '成交量']
        },
        grid: [
            {
                left: '10%',
                right: '8%',
                height: '50%'
            },
            {
                left: '10%',
                right: '8%',
                top: '63%',
                height: '16%'
            }
        ],
        xAxis: [
            {
                type: 'category',
                data: data.map(item => item[0]),
                scale: true,
                boundaryGap: false,
                axisLine: { onZero: false },
                splitLine: { show: false },
                min: 'dataMin',
                max: 'dataMax'
            },
            {
                type: 'category',
                gridIndex: 1,
                data: data.map(item => item[0]),
                scale: true,
                boundaryGap: false,
                axisLine: { onZero: false },
                axisTick: { show: false },
                splitLine: { show: false },
                axisLabel: { show: false },
                min: 'dataMin',
                max: 'dataMax'
            }
        ],
        yAxis: [
            {
                scale: true,
                splitArea: {
                    show: true
                }
            },
            {
                scale: true,
                gridIndex: 1,
                splitNumber: 2,
                axisLabel: { show: false },
                axisLine: { show: false },
                axisTick: { show: false },
                splitLine: { show: false }
            }
        ],
        dataZoom: [
            {
                type: 'inside',
                xAxisIndex: [0, 1],
                start: 0,
                end: 100
            },
            {
                show: true,
                xAxisIndex: [0, 1],
                type: 'slider',
                top: '85%',
                start: 0,
                end: 100
            }
        ],
        series: [
            {
                name: 'K线',
                type: 'candlestick',
                data: data.map(item => [
                    item[1], // 开盘
                    item[2], // 收盘
                    item[3], // 最低
                    item[4]  // 最高
                ]),
                itemStyle: {
                    color: '#ef232a',
                    color0: '#14b143',
                    borderColor: '#ef232a',
                    borderColor0: '#14b143'
                }
            },
            {
                name: 'MA5',
                type: 'line',
                data: ma5Data,
                smooth: true,
                lineStyle: {
                    opacity: 0.5
                }
            },
            {
                name: 'MA10',
                type: 'line',
                data: ma10Data,
                smooth: true,
                lineStyle: {
                    opacity: 0.5
                }
            },
            {
                name: 'MA20',
                type: 'line',
                data: ma20Data,
                smooth: true,
                lineStyle: {
                    opacity: 0.5
                }
            },
            {
                name: '成交量',
                type: 'bar',
                xAxisIndex: 1,
                yAxisIndex: 1,
                data: data.map(item => item[5])
            }
        ]
    };

    // 设置图表配置项
    chart.setOption(option);

    // 响应窗口大小变化
    window.addEventListener('resize', () => chart.resize());
}

// 计算移动平均线
function calculateMA(dayCount, data) {
    const result = [];
    for (let i = 0, len = data.length; i < len; i++) {
        if (i < dayCount - 1) {
            result.push('-');
            continue;
        }
        let sum = 0;
        for (let j = 0; j < dayCount; j++) {
            sum += +data[i - j][2];
        }
        result.push((sum / dayCount).toFixed(2));
    }
    return result;
}

// 渲染数据表格
function renderDataTable(data) {
    const tbody = document.getElementById('stockDataBody');
    tbody.innerHTML = data.map(item => `
        <tr>
            <td>${item.tradeDate}</td>
            <td>${item.open}</td>
            <td>${item.high}</td>
            <td>${item.low}</td>
            <td>${item.close}</td>
            <td>${item.pctChg}</td>
            <td>${item.vol}</td>
            <td>${item.amount}</td>
        </tr>
    `).join('');
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', initPage);