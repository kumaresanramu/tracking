// Analytics - Handles chart generation and data visualization
class Analytics {
    constructor() {
        this.monthlyTrendsChart = null;
        this.categoryBreakdownChart = null;
        this.expenseService = new ExpenseService();
    }

    async loadCharts() {
        try {
            await Promise.all([
                this.loadMonthlyTrendsChart(),
                this.loadCategoryBreakdownChart(),
                this.loadExpenseSummary()
            ]);
        } catch (error) {
            console.error('Failed to load charts:', error);
            this.showChartError();
        }
    }

    async loadMonthlyTrendsChart() {
        const canvas = document.getElementById('monthly-trends-chart');
        if (!canvas) return;

        try {
            // Get monthly trends data
            const trendsData = await this.getMonthlyTrendsData();
            
            // Destroy existing chart if it exists
            if (this.monthlyTrendsChart) {
                this.monthlyTrendsChart.destroy();
            }

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

        } catch (error) {
            console.error('Failed to load monthly trends chart:', error);
            this.showChartError(canvas, 'Failed to load monthly trends');
        }
    }

    async loadCategoryBreakdownChart() {
        const canvas = document.getElementById('category-breakdown-chart');
        if (!canvas) return;

        try {
            // Get category breakdown data for current month
            const now = new Date();
            const breakdownData = await this.getCategoryBreakdownData(now.getFullYear(), now.getMonth() + 1);
            
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
                            text: 'Category Breakdown (This Month)'
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
        const categories = await this.getCategoriesFromIndexedDB();
        
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
        try {
            // Try to get data from API first
            if (navigator.onLine) {
                const apiData = await this.expenseService.getMonthlyTrends(12);
                return this.formatMonthlyTrendsForChart(apiData);
            }
        } catch (error) {
            console.log('API unavailable, using local data');
        }

        // Fallback to local data
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
        const labels = apiData.map(item => {
            const date = new Date(item.month.year, item.month.monthValue - 1);
            return date.toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
        });
        
        const data = apiData.map(item => parseFloat(item.totalAmount));
        
        return { labels, data };
    }

    formatCategoryBreakdownForChart(apiData) {
        const labels = apiData.map(item => item.categoryName);
        const data = apiData.map(item => parseFloat(item.totalAmount));
        
        return { labels, data };
    }

    async getMonthlyTrendsFromLocal() {
        // Get expenses from IndexedDB
        const expenses = await this.getExpensesFromIndexedDB();
        const categories = await this.getCategoriesFromIndexedDB();
        
        // Group expenses by month
        const monthlyTotals = {};
        const now = new Date();
        
        // Initialize last 12 months
        for (let i = 11; i >= 0; i--) {
            const date = new Date(now.getFullYear(), now.getMonth() - i, 1);
            const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
            monthlyTotals[key] = 0;
        }
        
        // Sum expenses by month
        expenses.forEach(expense => {
            const expenseDate = new Date(expense.date);
            const key = `${expenseDate.getFullYear()}-${String(expenseDate.getMonth() + 1).padStart(2, '0')}`;
            if (monthlyTotals.hasOwnProperty(key)) {
                monthlyTotals[key] += expense.amount;
            }
        });
        
        // Convert to chart format
        const labels = Object.keys(monthlyTotals).map(key => {
            const [year, month] = key.split('-');
            const date = new Date(parseInt(year), parseInt(month) - 1);
            return date.toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
        });
        
        const data = Object.values(monthlyTotals);
        
        return { labels, data };
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
        return new Promise((resolve, reject) => {
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
        return new Promise((resolve, reject) => {
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
        if (!canvas) return;
        
        const ctx = canvas.getContext('2d');
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        
        // Draw error message
        ctx.fillStyle = '#666';
        ctx.font = '16px Arial';
        ctx.textAlign = 'center';
        ctx.fillText(message, canvas.width / 2, canvas.height / 2);
        
        ctx.font = '12px Arial';
        ctx.fillText('Check your connection and try again', canvas.width / 2, canvas.height / 2 + 25);
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
}