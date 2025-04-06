// 初始化日期选择器
flatpickr.localize(flatpickr.l10n.zh);

// 获取当前日期和一年前的日期
const today = new Date();
const oneYearAgo = new Date();
oneYearAgo.setFullYear(today.getFullYear() - 1);

const startDatePicker = flatpickr('#startDate', {
    dateFormat: 'Y-m-d',
    defaultDate: oneYearAgo,
    maxDate: 'today'
});

const endDatePicker = flatpickr('#endDate', {
    dateFormat: 'Y-m-d',
    defaultDate: today,
    maxDate: 'today'
});

// 页面加载完成后执行
document.addEventListener('DOMContentLoaded', () => {
    refreshStockList();
});

// 刷新股票列表
async function refreshStockList(forceRefresh = false) {
    try {
        const response = await fetch(`/api/stock/basic?refresh=${forceRefresh}`);
        if (!response.ok) throw new Error('获取股票列表失败');
        const stocks = await response.json();
        renderStockTable(stocks);
    } catch (error) {
        console.error('刷新股票列表出错:', error);
        alert('获取股票列表失败，请稍后重试');
    }
}

// 渲染股票表格
function renderStockTable(stocks) {
    const tableBody = document.getElementById('stockTableBody');
    tableBody.innerHTML = stocks.map(stock => `
        <tr>
            <td>${stock.tsCode}</td>
            <td>${stock.name}</td>
            <td>${stock.industry || '-'}</td>
            <td>${stock.area || '-'}</td>
            <td>
                <button class="btn btn-sm btn-primary" onclick="viewStockDetail('${stock.tsCode}')">查看详情</button>
            </td>
        </tr>
    `).join('');
}

// 查看股票详情
function viewStockDetail(tsCode) {
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    window.location.href = `/stock-detail.html?tsCode=${tsCode}&startDate=${startDate}&endDate=${endDate}`;
}

// 刷新数据
async function refreshData() {
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    
    if (!startDate || !endDate) {
        alert('请选择日期范围');
        return;
    }
    
    if (new Date(startDate) > new Date(endDate)) {
        alert('起始日期不能大于结束日期');
        return;
    }
    
    try {
        const tableBody = document.getElementById('stockTableBody');
        const rows = tableBody.getElementsByTagName('tr');
        for (let row of rows) {
            const tsCode = row.cells[0].textContent;
            await fetch(`/api/stock/daily/${tsCode}?refresh=true&startDate=${startDate}&endDate=${endDate}`);
        }
        alert('数据刷新成功');
    } catch (error) {
        console.error('刷新数据出错:', error);
        alert('数据刷新失败，请稍后重试');
    }
}