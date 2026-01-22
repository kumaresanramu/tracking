// Analytics - Handles chart generation and data visualization
class Analytics {
    constructor() {
        this.monthlyTrendsChart = null;
        this.categoryBreakdownChart = null;
        this.expenseService = new ExpenseService();
        this.selectedYear = new Date().getFullYear();
        this.selectedMonth = new Date().getMonth() + 1;
    }

    async loadCharts() {
        console.log('Analytics: Starting to load charts...');
        
        // Initialize month selector
        this.initializeMonthSelector();
        
        // Clear any existing status messages and add loading message
        const analyticsContainer = document.querySelector('.analytics-container');
        let statusDiv = null;
        
        if (analyticsContainer) {
            // Remove any existing status messages
            const existingStatusDivs = analyticsContainer.querySelectorAll('.analytics-status');
            existingStatusDivs.forEach(div => div.remove());
            
            // Add new loading message right after the h2 title
            statusDiv = document.createElement('div');
            statusDiv.className = 'analytics-status';
            statusDiv.style.cssText = `
                background: #e8f5e8;
                border: 1px solid #4caf50;
                border-radius: 4px;
                padding: 0.75rem;
                margin: 1rem 0;
                text-align: center;
            `;
            statusDiv.innerHTML = '<p style="color: #2e7d32; font-weight: bold; margin: 0;">Analytics loading started...</p>';
            
            // Insert after the h2 title
            const title = analyticsContainer.querySelector('h2');
            if (title && title.nextSibling) {
                analyticsContainer.insertBefore(statusDiv, title.nextSibling);
            } else {
                analyticsContainer.appendChild(statusDiv);
            }
        }
        
        try {
            await Promise.all([
                this.loadMonthlyTrendsChart(),
                this.loadCategoryBreakdownChart(),
                this.loadExpenseSummary()
            ]);
            console.log('Analytics: All charts loaded successfully');
            
            // Update status message to success
            if (statusDiv && statusDiv.parentNode) {
                statusDiv.style.cssText = `
                    background: #e3f2fd;
                    border: 1px solid #2196f3;
                    border-radius: 4px;
                    padding: 0.75rem;
                    margin: 1rem 0;
                    text-align: center;
                `;
                statusDiv.innerHTML = '<p style="color: #1976d2; font-weight: bold; margin: 0;">Analytics loaded successfully!</p>';
                
                // Remove the success message after 3 seconds
                setTimeout(() => {
                    if (statusDiv && statusDiv.parentNode) {
                        console.log('Analytics: Removing success message');
                        statusDiv.remove();
                    }
                }, 3000);
            }
        } catch (error) {
            console.error('Analytics: Failed to load charts:', error);
            
            // Update status message with error
            if (statusDiv && statusDiv.parentNode) {
                statusDiv.style.cssText = `
                    background: #ffebee;
                    border: 1px solid #f44336;
                    border-radius: 4px;
                    padding: 0.75rem;
                    margin: 1rem 0;
                    text-align: center;
                `;
                statusDiv.innerHTML = `<p style="color: #d32f2f; font-weight: bold; margin: 0;">Analytics failed to load: ${error.message}</p>`;
            }
            
            this.showChartError();
        }
    }

    async loadMonthlyTrendsChart() {
        const canvas = document.getElementById('monthly-trends-chart');
        console.log('Analytics: Loading monthly trends chart, canvas found:', !!canvas);
        if (!canvas) {
            console.warn('Analytics: Monthly trends canvas not found');
            return;
        }

        try {
            // Get monthly trends data
            console.log('Analytics: Fetching monthly trends data...');
            const trendsData = await this.getMonthlyTrendsData();
            console.log('Analytics: Monthly trends data received:', trendsData);
            console.log('Analytics: Labels array:', trendsData.labels);
            console.log('Analytics: Data array:', trendsData.data);
            
            // Validate data
            if (!trendsData || !trendsData.labels || !trendsData.data) {
                throw new Error('Invalid trends data received');
            }
            
            if (trendsData.labels.length === 0) {
                console.log('Analytics: No data available for monthly trends');
                this.showChartError(canvas, 'No data available');
                return;
            }
            
            // Check for Invalid Date in labels
            const invalidDates = trendsData.labels.filter(label => label === 'Invalid Date');
            if (invalidDates.length > 0) {
                console.error('Analytics: Found Invalid Date labels:', invalidDates);
                console.error('Analytics: Full labels array:', trendsData.labels);
            }
            
            // Destroy existing chart if it exists
            if (this.monthlyTrendsChart) {
                this.monthlyTrendsChart.destroy();
                this.monthlyTrendsChart = null;
            }

            console.log('Analytics: Creating Chart.js with labels:', trendsData.labels);
            
            // Create new chart
            this.monthlyTrendsChart = new Chart(canvas, {
                type: 'line',
                data: {
                    labels: trendsData.labels,
                    datasets: [{
                        label: 'Monthly Expenses',
                        data: trendsData.data,
                        borderColor: '#2196F3',
                        backgroundColor: 'rgba(33, 150, 243, 0.1)',
                        borderWidth: 2,
                        fill: true,
                        tension: 0.4
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        title: {
                            display: true,
                            text: 'Monthly Expense Trends'
                        },
                        legend: {
                            display: false
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                callback: function(value) {
                                    return '$' + value.toFixed(2);
                                }
                            }
                        }
                    },
                    interaction: {
                        intersect: false,
                        mode: 'index'
                    }
                }
            });

            console.log('Analytics: Monthly trends chart created successfully');

        } catch (error) {
            console.error('Analytics: Failed to load monthly trends chart:', error);
            this.showChartError(canvas, 'Failed to load monthly trends');
        }
    }

    async loadCategoryBreakdownChart() {
        const canvas = document.getElementById('category-breakdown-chart');
        if (!canvas) return;

        try {
            // Get category breakdown data for selected month
            const breakdownData = await this.getCategoryBreakdownData(this.selectedYear, this.selectedMonth);
            
            // Destroy existing chart if it exists
            if (this.categoryBreakdownChart) {
                this.categoryBreakdownChart.destroy();
            }

            // Create new chart
            this.categoryBreakdownChart = new Chart(canvas, {
                type: 'doughnut',
                data: {
                    labels: breakdownData.labels,
                    datasets: [{
                        data: breakdownData.data,
                        backgroundColor: [
                            '#2196F3',
                            '#4CAF50',
                            '#FF9800',
                            '#F44336',
                            '#9C27B0',
                            '#00BCD4',
                            '#FFEB3B',
                            '#795548',
                            '#607D8B',
                            '#E91E63'
                        ],
                        borderWidth: 2,
                        borderColor: '#fff'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        title: {
                            display: true,
                            text: `Category Breakdown (${new Date(this.selectedYear, this.selectedMonth - 1).toLocaleDateString('en-US', { month: 'long', year: 'numeric' })})`
                        },
                        legend: {
                            position: 'bottom',
                            labels: {
                                padding: 20,
                                usePointStyle: true
                            }
                        },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    const label = context.label || '';
                                    const value = context.parsed;
                                    const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                    const percentage = ((value / total) * 100).toFixed(1);
                                    return `${label}: $${value.toFixed(2)} (${percentage}%)`;
                                }
                            }
                        }
                    }
                }
            });

        } catch (error) {
            console.error('Failed to load category breakdown chart:', error);
            this.showChartError(canvas, 'Failed to load category breakdown');
        }
    }

    async loadExpenseSummary() {
        try {
            const now = new Date();
            const year = now.getFullYear();
            const month = now.getMonth() + 1;
            
            let summaryData;
            
            if (navigator.onLine) {
                summaryData = await this.expenseService.getExpenseSummary(year, month);
            } else {
                summaryData = await this.getExpenseSummaryFromLocal(year, month);
            }
            
            this.displayExpenseSummary(summaryData);
            
        } catch (error) {
            console.error('Failed to load expense summary:', error);
        }
    }

    displayExpenseSummary(summaryData) {
        // Create summary section if it doesn't exist
        let summarySection = document.querySelector('.analytics-summary');
        if (!summarySection) {
            const analyticsContainer = document.querySelector('.analytics-container');
            if (analyticsContainer) {
                summarySection = document.createElement('div');
                summarySection.className = 'analytics-summary';
                analyticsContainer.insertBefore(summarySection, analyticsContainer.firstChild.nextSibling);
            }
        }
        
        if (summarySection) {
            summarySection.innerHTML = `
                <div class="summary-cards">
                    <div class="summary-card">
                        <h4>Total Expenses</h4>
                        <p class="summary-value">$${parseFloat(summaryData.totalAmount || 0).toFixed(2)}</p>
                    </div>
                    <div class="summary-card">
                        <h4>Expense Count</h4>
                        <p class="summary-value">${summaryData.expenseCount || 0}</p>
                    </div>
                    <div class="summary-card">
                        <h4>Average Expense</h4>
                        <p class="summary-value">$${parseFloat(summaryData.averageExpense || 0).toFixed(2)}</p>
                    </div>
                    <div class="summary-card">
                        <h4>Categories Used</h4>
                        <p class="summary-value">${summaryData.uniqueCategories || 0}</p>
                    </div>
                </div>
            `;
        }
    }

    async getExpenseSummaryFromLocal(year, month) {
        const expenses = await this.getExpensesFromIndexedDB();
        
        // Filter expenses for the specified month
        const monthlyExpenses = expenses.filter(expense => {
            const expenseDate = new Date(expense.date);
            return expenseDate.getFullYear() === year && expenseDate.getMonth() === month - 1;
        });
        
        const totalAmount = monthlyExpenses.reduce((sum, expense) => sum + expense.amount, 0);
        const expenseCount = monthlyExpenses.length;
        const averageExpense = expenseCount > 0 ? totalAmount / expenseCount : 0;
        const uniqueCategories = new Set(monthlyExpenses.map(e => e.categoryId)).size;
        
        return {
            totalAmount,
            expenseCount,
            averageExpense,
            uniqueCategories,
            year,
            month
        };
    }

    async getMonthlyTrendsData() {
        console.log('Analytics: Getting monthly trends data, online:', navigator.onLine);
        try {
            // Try to get data from API first
            if (navigator.onLine) {
                console.log('Analytics: Fetching from API...');
                const apiData = await this.expenseService.getMonthlyTrends(12);
                console.log('Analytics: API data received:', apiData);
                return this.formatMonthlyTrendsForChart(apiData);
            }
        } catch (error) {
            console.log('Analytics: API unavailable, using local data. Error:', error);
        }

        // Fallback to local data
        console.log('Analytics: Using local data fallback');
        return await this.getMonthlyTrendsFromLocal();
    }

    async getCategoryBreakdownData(year, month) {
        try {
            // Try to get data from API first
            if (navigator.onLine) {
                const apiData = await this.expenseService.getCategoryBreakdown(year, month);
                return this.formatCategoryBreakdownForChart(apiData);
            }
        } catch (error) {
            console.log('API unavailable, using local data');
        }

        // Fallback to local data
        return await this.getCategoryBreakdownFromLocal(year, month);
    }

    formatMonthlyTrendsForChart(apiData) {
        console.log('Analytics: formatMonthlyTrendsForChart called with:', apiData);
        
        // Quick test of date parsing
        const testItem = {month: '2025-11', totalAmount: 12.75};
        console.log('Analytics: Testing date parsing with:', testItem.month);
        const [testYear, testMonth] = testItem.month.split('-');
        console.log('Analytics: Test parsed year:', testYear, 'month:', testMonth);
        const testDate = new Date(parseInt(testYear), parseInt(testMonth) - 1);
        console.log('Analytics: Test created date:', testDate);
        const testFormatted = testDate.toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
        console.log('Analytics: Test formatted:', testFormatted);
        
        if (!apiData || !Array.isArray(apiData)) {
            console.warn('Analytics: Invalid API data for monthly trends:', apiData);
            return { labels: [], data: [] };
        }
        
        const labels = apiData.map((item, index) => {
            console.log(`Analytics: Processing item ${index}:`, item);
            try {
                // Handle different date formats from API
                let date;
                if (item.month && typeof item.month === 'object' && item.month.year && item.month.monthValue) {
                    // Handle Java YearMonth format: {year: 2024, monthValue: 1}
                    console.log('Analytics: Using Java YearMonth format');
                    date = new Date(item.month.year, item.month.monthValue - 1);
                } else if (item.month && typeof item.month === 'string') {
                    // Handle string format like "2024-01" or "2025-02"
                    console.log('Analytics: Using string format:', item.month);
                    const [year, month] = item.month.split('-');
                    console.log('Analytics: Parsed year:', year, 'month:', month);
                    date = new Date(parseInt(year), parseInt(month) - 1);
                    console.log('Analytics: Created date:', date);
                } else {
                    // Fallback to current date
                    console.warn('Analytics: Unknown month format:', item.month);
                    date = new Date();
                }
                
                const formattedDate = date.toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
                console.log('Analytics: Formatted date:', formattedDate);
                return formattedDate;
            } catch (error) {
                console.error('Analytics: Error formatting date:', error, item);
                return 'Invalid Date';
            }
        });
        
        const data = apiData.map(item => {
            const amount = parseFloat(item.totalAmount || 0);
            return isNaN(amount) ? 0 : amount;
        });
        
        console.log('Analytics: Final formatted monthly trends - labels:', labels, 'data:', data);
        return { labels, data };
    }

    formatCategoryBreakdownForChart(apiData) {
        const labels = apiData.map(item => item.categoryName);
        const data = apiData.map(item => parseFloat(item.totalAmount));
        
        return { labels, data };
    }

    async getMonthlyTrendsFromLocal() {
        try {
            // Get expenses from IndexedDB
            const expenses = await this.getExpensesFromIndexedDB();
            console.log('Analytics: Local expenses for trends:', expenses.length);
            
            // Group expenses by month
            const monthlyTotals = {};
            const now = new Date();
            
            // Initialize last 12 months with zero values
            for (let i = 11; i >= 0; i--) {
                const date = new Date(now.getFullYear(), now.getMonth() - i, 1);
                const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
                monthlyTotals[key] = 0;
            }
            
            // Sum expenses by month
            expenses.forEach(expense => {
                try {
                    const expenseDate = new Date(expense.date);
                    if (!isNaN(expenseDate.getTime())) {
                        const key = `${expenseDate.getFullYear()}-${String(expenseDate.getMonth() + 1).padStart(2, '0')}`;
                        if (monthlyTotals.hasOwnProperty(key)) {
                            monthlyTotals[key] += (expense.amount || 0);
                        }
                    }
                } catch (error) {
                    console.warn('Analytics: Invalid expense date:', expense.date, error);
                }
            });
            
            // Convert to chart format
            const labels = Object.keys(monthlyTotals).map(key => {
                try {
                    const [year, month] = key.split('-');
                    const date = new Date(parseInt(year), parseInt(month) - 1);
                    return date.toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
                } catch (error) {
                    console.warn('Analytics: Error formatting local date:', key, error);
                    return 'Invalid Date';
                }
            });
            
            const data = Object.values(monthlyTotals);
            
            console.log('Analytics: Local monthly trends - labels:', labels, 'data:', data);
            return { labels, data };
        } catch (error) {
            console.error('Analytics: Error getting local monthly trends:', error);
            // Return empty data as fallback
            return { labels: [], data: [] };
        }
    }

    async getCategoryBreakdownFromLocal(year, month) {
        // Get expenses from IndexedDB
        const expenses = await this.getExpensesFromIndexedDB();
        const categories = await this.getCategoriesFromIndexedDB();
        
        // Filter expenses for the specified month
        const monthlyExpenses = expenses.filter(expense => {
            const expenseDate = new Date(expense.date);
            return expenseDate.getFullYear() === year && expenseDate.getMonth() === month - 1;
        });
        
        // Group by category
        const categoryTotals = {};
        
        monthlyExpenses.forEach(expense => {
            const category = categories.find(c => c.id === expense.categoryId);
            const categoryName = category ? category.name : 'Unknown';
            
            if (!categoryTotals[categoryName]) {
                categoryTotals[categoryName] = 0;
            }
            categoryTotals[categoryName] += expense.amount;
        });
        
        // Convert to chart format
        const labels = Object.keys(categoryTotals);
        const data = Object.values(categoryTotals);
        
        return { labels, data };
    }

    async getExpensesFromIndexedDB() {
        return new Promise((resolve) => {
            if (!window.app || !window.app.db) {
                resolve([]);
                return;
            }

            const transaction = window.app.db.transaction(['expenses'], 'readonly');
            const store = transaction.objectStore('expenses');
            const request = store.getAll();

            request.onsuccess = () => {
                resolve(request.result || []);
            };

            request.onerror = () => {
                console.error('Failed to get expenses from IndexedDB:', request.error);
                resolve([]);
            };
        });
    }

    async getCategoriesFromIndexedDB() {
        return new Promise((resolve) => {
            if (!window.app || !window.app.db) {
                resolve([]);
                return;
            }

            const transaction = window.app.db.transaction(['categories'], 'readonly');
            const store = transaction.objectStore('categories');
            const request = store.getAll();

            request.onsuccess = () => {
                resolve(request.result || []);
            };

            request.onerror = () => {
                console.error('Failed to get categories from IndexedDB:', request.error);
                resolve([]);
            };
        });
    }

    showChartError(canvas, message = 'Failed to load chart') {
        if (!canvas) {
            console.warn('Analytics: No canvas provided for error display');
            return;
        }
        
        try {
            const ctx = canvas.getContext('2d');
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            
            // Draw error message
            ctx.fillStyle = '#666';
            ctx.font = '16px Arial';
            ctx.textAlign = 'center';
            ctx.fillText(message, canvas.width / 2, canvas.height / 2);
            
            ctx.font = '12px Arial';
            ctx.fillText('Check your connection and try again', canvas.width / 2, canvas.height / 2 + 25);
        } catch (error) {
            console.error('Analytics: Error drawing chart error message:', error);
        }
    }

    // Utility methods for analytics calculations
    calculateMonthlyAverage(expenses) {
        if (expenses.length === 0) return 0;
        
        const total = expenses.reduce((sum, expense) => sum + expense.amount, 0);
        return total / expenses.length;
    }

    calculateCategoryPercentages(expenses, categories) {
        const total = expenses.reduce((sum, expense) => sum + expense.amount, 0);
        const categoryTotals = {};
        
        expenses.forEach(expense => {
            const category = categories.find(c => c.id === expense.categoryId);
            const categoryName = category ? category.name : 'Unknown';
            
            if (!categoryTotals[categoryName]) {
                categoryTotals[categoryName] = 0;
            }
            categoryTotals[categoryName] += expense.amount;
        });
        
        const percentages = {};
        Object.keys(categoryTotals).forEach(category => {
            percentages[category] = (categoryTotals[category] / total) * 100;
        });
        
        return percentages;
    }

    findTopCategories(expenses, categories, limit = 5) {
        const categoryTotals = {};
        
        expenses.forEach(expense => {
            const category = categories.find(c => c.id === expense.categoryId);
            const categoryName = category ? category.name : 'Unknown';
            
            if (!categoryTotals[categoryName]) {
                categoryTotals[categoryName] = 0;
            }
            categoryTotals[categoryName] += expense.amount;
        });
        
        return Object.entries(categoryTotals)
            .sort(([,a], [,b]) => b - a)
            .slice(0, limit)
            .map(([name, amount]) => ({ name, amount }));
    }

    calculateSpendingTrend(expenses, months = 3) {
        const now = new Date();
        const monthlyTotals = [];
        
        for (let i = months - 1; i >= 0; i--) {
            const targetDate = new Date(now.getFullYear(), now.getMonth() - i, 1);
            const monthExpenses = expenses.filter(expense => {
                const expenseDate = new Date(expense.date);
                return expenseDate.getFullYear() === targetDate.getFullYear() &&
                       expenseDate.getMonth() === targetDate.getMonth();
            });
            
            const total = monthExpenses.reduce((sum, expense) => sum + expense.amount, 0);
            monthlyTotals.push(total);
        }
        
        // Calculate trend (positive = increasing, negative = decreasing)
        if (monthlyTotals.length < 2) return 0;
        
        const firstHalf = monthlyTotals.slice(0, Math.floor(monthlyTotals.length / 2));
        const secondHalf = monthlyTotals.slice(Math.floor(monthlyTotals.length / 2));
        
        const firstAvg = firstHalf.reduce((a, b) => a + b, 0) / firstHalf.length;
        const secondAvg = secondHalf.reduce((a, b) => a + b, 0) / secondHalf.length;
        
        return ((secondAvg - firstAvg) / firstAvg) * 100;
    }

    // Export chart data
    exportChartData(chartType) {
        let data;
        let filename;
        
        switch (chartType) {
            case 'monthly-trends':
                data = this.monthlyTrendsChart?.data;
                filename = 'monthly-trends.json';
                break;
            case 'category-breakdown':
                data = this.categoryBreakdownChart?.data;
                filename = 'category-breakdown.json';
                break;
            default:
                console.error('Unknown chart type:', chartType);
                return;
        }
        
        if (!data) {
            console.error('No data available for export');
            return;
        }
        
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }

    // Initialize month selector for category breakdown
    initializeMonthSelector() {
        const selector = document.getElementById('category-month-selector');
        if (!selector) return;

        // Clear existing options
        selector.innerHTML = '';

        // Generate last 12 months
        const months = [];
        const now = new Date();
        
        for (let i = 0; i < 12; i++) {
            const date = new Date(now.getFullYear(), now.getMonth() - i, 1);
            months.push({
                year: date.getFullYear(),
                month: date.getMonth() + 1,
                display: date.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
            });
        }

        // Add options to selector
        months.forEach(monthData => {
            const option = document.createElement('option');
            option.value = `${monthData.year}-${monthData.month}`;
            option.textContent = monthData.display;
            
            // Select current month by default
            if (monthData.year === this.selectedYear && monthData.month === this.selectedMonth) {
                option.selected = true;
            }
            
            selector.appendChild(option);
        });

        // Add event listener for month changes
        selector.addEventListener('change', (e) => {
            const [year, month] = e.target.value.split('-');
            this.selectedYear = parseInt(year);
            this.selectedMonth = parseInt(month);
            
            console.log('Analytics: Month changed to:', this.selectedYear, this.selectedMonth);
            
            // Reload category breakdown chart with new month
            this.loadCategoryBreakdownChart();
        });
    }

    // Update category breakdown chart for selected month
    async updateCategoryBreakdownChart() {
        await this.loadCategoryBreakdownChart();
    }
}