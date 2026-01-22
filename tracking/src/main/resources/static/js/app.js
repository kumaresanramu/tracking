// Main Application Controller
class ExpenseTrackerApp {
    constructor() {
        this.version = '20260122161500'; // Updated version identifier
        this.currentPage = 'dashboard';
        this.currentMonth = new Date().getMonth();
        this.currentYear = new Date().getFullYear();
        this.expenses = [];
        this.categories = [];
        
        // Enhanced Dashboard properties
        this.currentDateRange = 'this-month';
        this.currentFilters = {
            category: 'all',
            paymentMethod: 'all',
            tags: 'all',
            startDate: null,
            endDate: null
        };
        this.monthlyBudget = 0;
        this.savingsGoal = 0;
        this.enhancedDashboardInitialized = false;
        
        console.log('APP VERSION:', this.version);
        this.init();
    }

    async init() {
        try {
            // Initialize services
            this.expenseService = new ExpenseService();
            this.analytics = new Analytics();
            this.ui = new UIComponents();
            this.errorHandler = new ErrorHandler();
            
            // Connect error handler to UI
            this.errorHandler.setUI(this.ui);
            
            // Set up event listeners
            this.setupEventListeners();
            
            // Initialize IndexedDB for offline storage
            await this.initIndexedDB();
            
            // Load initial data
            await this.loadInitialData();
            
            // Set up connectivity monitoring
            this.setupConnectivityMonitoring();
            
            // Update UI
            this.updateUI();
            
            // Initialize theme
            await this.initializeTheme();
            
            console.log('Expense Tracker App initialized successfully');
        } catch (error) {
            console.error('Failed to initialize app:', error);
        }
    }

    setupEventListeners() {
        // Mobile navigation toggle
        const navToggle = document.getElementById('nav-menu-toggle');
        const navMenu = document.getElementById('nav-menu');
        
        if (navToggle && navMenu) {
            navToggle.addEventListener('click', () => {
                navMenu.classList.toggle('active');
                const isOpen = navMenu.classList.contains('active');
                navToggle.setAttribute('aria-expanded', isOpen);
                navToggle.querySelector('span').textContent = isOpen ? '✕' : '☰';
            });
            
            // Close mobile menu when clicking outside
            document.addEventListener('click', (e) => {
                if (!navToggle.contains(e.target) && !navMenu.contains(e.target)) {
                    navMenu.classList.remove('active');
                    navToggle.setAttribute('aria-expanded', 'false');
                    navToggle.querySelector('span').textContent = '☰';
                }
            });
            
            // Close mobile menu on window resize to desktop
            window.addEventListener('resize', () => {
                if (window.innerWidth >= 768) {
                    navMenu.classList.remove('active');
                    navToggle.setAttribute('aria-expanded', 'false');
                    navToggle.querySelector('span').textContent = '☰';
                }
            });
        }

        // Navigation
        document.querySelectorAll('.nav-item').forEach(item => {
            item.addEventListener('click', (e) => {
                const page = e.target.dataset.page;
                this.navigateToPage(page);
                
                // Close mobile menu after navigation
                if (navMenu && navMenu.classList.contains('active')) {
                    navMenu.classList.remove('active');
                    if (navToggle) {
                        navToggle.setAttribute('aria-expanded', 'false');
                        navToggle.querySelector('span').textContent = '☰';
                    }
                }
            });
        });

        // Expense form submission
        const expenseForm = document.getElementById('expense-form');
        if (expenseForm) {
            expenseForm.addEventListener('submit', (e) => this.handleExpenseSubmit(e));
        }

        // Form validation
        this.setupFormValidation();

        // Month navigation
        const prevMonthBtn = document.getElementById('prev-month');
        const nextMonthBtn = document.getElementById('next-month');
        
        if (prevMonthBtn) {
            prevMonthBtn.addEventListener('click', () => this.navigateMonth(-1));
        }
        
        if (nextMonthBtn) {
            nextMonthBtn.addEventListener('click', () => this.navigateMonth(1));
        }

        // Quick actions
        const quickAddBtn = document.getElementById('quick-add-expense');
        const viewAnalyticsBtn = document.getElementById('view-analytics');
        
        if (quickAddBtn) {
            quickAddBtn.addEventListener('click', () => this.navigateToPage('expenses'));
        }
        
        if (viewAnalyticsBtn) {
            viewAnalyticsBtn.addEventListener('click', () => this.navigateToPage('analytics'));
        }

        // Settings - Theme selector
        const themeSelector = document.getElementById('theme-selector');
        if (themeSelector) {
            themeSelector.addEventListener('change', (e) => this.changeTheme(e.target.value));
        }
    }

    setupFormValidation() {
        const amountInput = document.getElementById('expense-amount');
        const descriptionInput = document.getElementById('expense-description');
        const categorySelect = document.getElementById('expense-category');

        if (amountInput) {
            amountInput.addEventListener('blur', (e) => {
                if (e.target.value && (isNaN(e.target.value) || parseFloat(e.target.value) <= 0)) {
                    this.ui.showFieldError(e.target, 'Please enter a valid amount greater than 0');
                } else {
                    this.ui.removeFieldError(e.target);
                }
            });
        }
        
        if (descriptionInput) {
            descriptionInput.addEventListener('blur', (e) => {
                if (!e.target.value.trim()) {
                    this.ui.showFieldError(e.target, 'Description is required');
                } else {
                    this.ui.removeFieldError(e.target);
                }
            });
        }
        
        if (categorySelect) {
            categorySelect.addEventListener('change', (e) => {
                if (!e.target.value) {
                    this.ui.showFieldError(e.target, 'Please select a category');
                } else {
                    this.ui.removeFieldError(e.target);
                }
            });
        }
    }

    async initIndexedDB() {
        return new Promise((resolve, reject) => {
            const request = indexedDB.open('ExpenseTrackerDB', 3);
            
            request.onerror = () => reject(request.error);
            request.onsuccess = () => {
                this.db = request.result;
                resolve();
            };
            
            request.onupgradeneeded = (event) => {
                const db = event.target.result;
                
                // Create expenses object store
                if (!db.objectStoreNames.contains('expenses')) {
                    const expenseStore = db.createObjectStore('expenses', { keyPath: 'id', autoIncrement: true });
                    expenseStore.createIndex('date', 'date', { unique: false });
                    expenseStore.createIndex('categoryId', 'categoryId', { unique: false });
                    expenseStore.createIndex('month', ['year', 'month'], { unique: false });
                }
                
                // Create categories object store
                if (!db.objectStoreNames.contains('categories')) {
                    const categoryStore = db.createObjectStore('categories', { keyPath: 'id' });
                    categoryStore.createIndex('name', 'name', { unique: false });
                    categoryStore.createIndex('parentId', 'parentId', { unique: false });
                }
                
                // Create app settings store
                if (!db.objectStoreNames.contains('settings')) {
                    db.createObjectStore('settings', { keyPath: 'key' });
                }
            };
        });
    }

    async loadInitialData() {
        try {
            // Load categories first
            await this.loadCategories();
            
            // Load monthly expenses
            await this.loadMonthlyExpenses();
        } catch (error) {
            console.error('Failed to load initial data:', error);
            // Try to load from local storage
            await this.loadOfflineData();
        }
    }

    async loadCategories() {
        try {
            const response = await fetch('/api/categories');
            if (response.ok) {
                this.categories = await response.json();
                await this.saveToIndexedDB('categories', this.categories);
                this.populateCategoryDropdown();
            } else {
                throw new Error('Failed to load categories');
            }
        } catch (error) {
            console.error('Failed to load categories:', error);
            this.categories = await this.getFromIndexedDB('categories') || [];
            this.populateCategoryDropdown();
        }
    }

    async loadMonthlyExpenses() {
        try {
            const response = await fetch(`/api/expenses/month/${this.currentYear}/${this.currentMonth + 1}`);
            if (response.ok) {
                this.expenses = await response.json();
                await this.saveMonthlyExpensesToIndexedDB();
                this.displayMonthlyExpenses();
            } else {
                throw new Error('Failed to load expenses');
            }
        } catch (error) {
            console.error('Failed to load monthly expenses:', error);
            this.expenses = await this.getMonthlyExpensesFromIndexedDB() || [];
            this.displayMonthlyExpenses();
        }
    }

    async loadOfflineData() {
        try {
            this.categories = await this.getFromIndexedDB('categories') || [];
            this.expenses = await this.getMonthlyExpensesFromIndexedDB() || [];
            this.populateCategoryDropdown();
            this.displayMonthlyExpenses();
        } catch (error) {
            console.error('Failed to load offline data:', error);
        }
    }

    setupConnectivityMonitoring() {
        window.addEventListener('online', () => {
            console.log('Connection restored - syncing data');
            this.loadInitialData();
        });
        
        window.addEventListener('offline', () => {
            console.log('Connection lost - operating in offline mode');
        });
    }

    navigateToPage(page) {
        // Update active nav item
        document.querySelectorAll('.nav-item').forEach(item => {
            item.classList.remove('active');
        });
        document.querySelector(`[data-page="${page}"]`)?.classList.add('active');
        
        // Hide all pages
        document.querySelectorAll('.page').forEach(p => {
            p.classList.remove('active');
        });
        
        // Show selected page
        const targetPage = document.getElementById(`${page}-page`);
        if (targetPage) {
            targetPage.classList.add('active');
        }
        
        this.currentPage = page;
        
        // Load page-specific data
        this.loadPageData(page);
    }

    async loadPageData(page) {
        switch (page) {
            case 'dashboard':
                this.updateDashboardStats();
                break;
            case 'expenses':
                this.displayMonthlyExpenses();
                break;
            case 'analytics':
                if (this.analytics) {
                    await this.analytics.loadCharts();
                }
                break;
            case 'reminders':
                // Load reminders
                break;
            case 'settings':
                // Load settings
                break;
        }
    }

    async handleExpenseSubmit(e) {
        e.preventDefault();
        
        if (!this.ui.validateForm(
            document.getElementById('expense-form'),
            ['amount', 'description', 'category']
        )) {
            return;
        }
        
        const formData = new FormData(e.target);
        const expense = {
            amount: parseFloat(formData.get('amount')),
            description: formData.get('description'),
            categoryId: parseInt(formData.get('categoryId')),
            date: formData.get('date') || new Date().toISOString().split('T')[0],
            paymentMethod: formData.get('paymentMethod') || '',
            tags: formData.get('tags') || ''
        };
        
        // Validate amount
        if (expense.amount <= 0) {
            this.ui.showToast('Amount must be greater than 0', 'error');
            return;
        }
        
        if (!expense.description) {
            this.ui.showToast('Description is required', 'error');
            return;
        }
        
        if (!expense.categoryId) {
            this.ui.showToast('Please select a category', 'error');
            return;
        }
        
        // Add month/year for indexing
        const expenseDate = new Date(expense.date);
        expense.month = expenseDate.getMonth() + 1;
        expense.year = expenseDate.getFullYear();
        
        try {
            const savedExpense = await this.expenseService.createExpense(expense);
            
            if (savedExpense) {
                // Update local expenses array
                const index = this.expenses.findIndex(e => e.id === savedExpense.id);
                if (index !== -1) {
                    this.expenses[index] = savedExpense;
                } else {
                    this.expenses.push(savedExpense);
                }
                
                // Also update in IndexedDB
                await this.saveToIndexedDB('expenses', savedExpense);
                
                // Reset form
                e.target.reset();
                
                // Update UI
                this.displayMonthlyExpenses();
                this.updateDashboardStats();
                
                this.ui.showToast('Expense added successfully!', 'success');
            }
            
            // Update UI
            this.displayMonthlyExpenses();
            this.updateDashboardStats();
            
        } catch (error) {
            console.error('Failed to save expense:', error);
            this.errorHandler.handleError(error, {
                message: 'Failed to save expense. Please try again.',
                retryable: true
            });
        }
    }

    navigateMonth(direction) {
        this.currentMonth += direction;
        
        if (this.currentMonth < 0) {
            this.currentMonth = 11;
            this.currentYear--;
        } else if (this.currentMonth > 11) {
            this.currentMonth = 0;
            this.currentYear++;
        }
        
        this.updateMonthDisplay();
        this.loadMonthlyExpenses();
    }

    updateMonthDisplay() {
        const monthNames = [
            'January', 'February', 'March', 'April', 'May', 'June',
            'July', 'August', 'September', 'October', 'November', 'December'
        ];
        
        const monthYearElement = document.getElementById('current-month-year');
        if (monthYearElement) {
            monthYearElement.textContent = `${monthNames[this.currentMonth]} ${this.currentYear}`;
        }
    }

    populateCategoryDropdown() {
        const categorySelect = document.getElementById('expense-category');
        if (!categorySelect || this.categories.length === 0) return;
        
        // Clear existing options except the first one (placeholder)
        while (categorySelect.children.length > 1) {
            categorySelect.removeChild(categorySelect.lastChild);
        }
        
        // Build hierarchical category options
        const hierarchicalCategories = this.buildCategoryHierarchy(this.categories);
        this.addCategoryOptionsToSelect(categorySelect, hierarchicalCategories, 0);
    }

    buildCategoryHierarchy(categories) {
        const categoryMap = new Map();
        const rootCategories = [];
        
        // First pass: create map of all categories
        categories.forEach(category => {
            categoryMap.set(category.id, { ...category, children: [] });
        });
        
        // Second pass: build hierarchy
        categories.forEach(category => {
            if (category.parentId) {
                const parent = categoryMap.get(category.parentId);
                if (parent) {
                    parent.children.push(categoryMap.get(category.id));
                }
            } else {
                rootCategories.push(categoryMap.get(category.id));
            }
        });
        
        return rootCategories;
    }

    addCategoryOptionsToSelect(select, categories, level) {
        categories.forEach(category => {
            const option = document.createElement('option');
            option.value = category.id;
            option.textContent = '  '.repeat(level) + category.name;
            select.appendChild(option);
            
            if (category.children && category.children.length > 0) {
                this.addCategoryOptionsToSelect(select, category.children, level + 1);
            }
        });
    }

    displayMonthlyExpenses() {
        const expensesList = document.getElementById('monthly-expenses-list');
        if (!expensesList) return;
        
        if (this.expenses.length === 0) {
            expensesList.innerHTML = '<div class="no-expenses">No expenses found for this month.</div>';
            return;
        }
        
        // Sort expenses by date (newest first)
        const sortedExpenses = [...this.expenses].sort((a, b) => new Date(b.date) - new Date(a.date));
        
        expensesList.innerHTML = sortedExpenses.map(expense => {
            const category = expense.category || this.categories.find(c => c.id === expense.categoryId);
            const categoryName = category ? category.name : 'Unknown';
            
            return `
                <div class="expense-item" data-id="${expense.id}">
                    <div class="expense-info">
                        <div class="expense-description">${expense.description}</div>
                        <div class="expense-details">
                            <span class="expense-category">${categoryName}</span>
                            <span class="expense-date">${new Date(expense.date).toLocaleDateString()}</span>
                            ${expense.paymentMethod ? `<span class="expense-payment">${expense.paymentMethod}</span>` : ''}
                        </div>
                        ${expense.tags ? `<div class="expense-tags">${expense.tags}</div>` : ''}
                    </div>
                    <div class="expense-amount">$${expense.amount.toFixed(2)}</div>
                    <div class="expense-actions">
                        <button class="btn-edit" onclick="app.editExpense(${expense.id})" title="Edit expense">
                            ✏️ Edit
                        </button>
                        <button class="btn-delete" onclick="app.deleteExpense(${expense.id})" title="Delete expense">
                            🗑️ Delete
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    }

    async editExpense(expenseId) {
        const expense = this.expenses.find(e => e.id === expenseId);
        if (!expense) return;
        
        // Populate form with expense data
        document.getElementById('expense-amount').value = expense.amount;
        document.getElementById('expense-description').value = expense.description;
        document.getElementById('expense-category').value = expense.categoryId;
        document.getElementById('expense-date').value = expense.date;
        
        const paymentMethodField = document.getElementById('expense-payment-method');
        if (paymentMethodField) {
            paymentMethodField.value = expense.paymentMethod || '';
        }
        
        const tagsField = document.getElementById('expense-tags');
        if (tagsField) {
            tagsField.value = expense.tags || '';
        }
        
        // Switch to expenses page
        this.navigateToPage('expenses');
        
        // Store the expense ID for updating
        document.getElementById('expense-form').dataset.editingId = expenseId;
        
        // Change submit button text
        const submitBtn = document.querySelector('#expense-form button[type="submit"]');
        if (submitBtn) {
            submitBtn.textContent = 'Update Expense';
        }
    }

    async deleteExpense(expenseId) {
        if (!confirm('Are you sure you want to delete this expense?')) {
            return;
        }
        
        try {
            await this.expenseService.deleteExpense(expenseId);
            
            // Remove from local array
            this.expenses = this.expenses.filter(e => e.id !== expenseId);
            
            // Remove from IndexedDB
            await this.deleteFromIndexedDB('expenses', expenseId);
            
            // Update UI
            this.displayMonthlyExpenses();
            this.updateDashboardStats();
            
            this.ui.showToast('Expense deleted successfully!', 'success');
        } catch (error) {
            console.error('Failed to delete expense:', error);
            this.ui.showToast('Failed to delete expense. Please try again.', 'error');
        }
    }

    updateUI() {
        this.updateMonthDisplay();
        this.displayMonthlyExpenses();
        this.updateDashboardStats();
    }

    updateDashboardStats() {
        // Initialize enhanced dashboard if not already done
        if (!this.enhancedDashboardInitialized) {
            this.setupEnhancedDashboard();
            this.populateFilterDropdowns();
            this.enhancedDashboardInitialized = true;
        }
        
        // Update the dashboard data
        this.updateDashboardData();
    }

    // Enhanced Dashboard Methods
    setupEnhancedDashboard() {
        // Date range filter
        const dateRangeFilter = document.getElementById('date-range-filter');
        if (dateRangeFilter) {
            dateRangeFilter.addEventListener('change', (e) => {
                this.currentDateRange = e.target.value;
                this.handleDateRangeChange();
            });
        }

        // Other filters
        const categoryFilter = document.getElementById('category-filter');
        const paymentMethodFilter = document.getElementById('payment-method-filter');
        const tagFilter = document.getElementById('tag-filter');

        if (categoryFilter) {
            categoryFilter.addEventListener('change', (e) => {
                this.currentFilters.category = e.target.value;
                this.updateDashboardData();
            });
        }

        if (paymentMethodFilter) {
            paymentMethodFilter.addEventListener('change', (e) => {
                this.currentFilters.paymentMethod = e.target.value;
                this.updateDashboardData();
            });
        }

        if (tagFilter) {
            tagFilter.addEventListener('change', (e) => {
                this.currentFilters.tags = e.target.value;
                this.updateDashboardData();
            });
        }

        // Custom date range
        const applyDateRange = document.getElementById('apply-date-range');
        if (applyDateRange) {
            applyDateRange.addEventListener('click', () => {
                const startDate = document.getElementById('start-date').value;
                const endDate = document.getElementById('end-date').value;
                
                if (startDate && endDate) {
                    this.currentFilters.startDate = startDate;
                    this.currentFilters.endDate = endDate;
                    this.updateDashboardData();
                }
            });
        }

        // Budget and goals buttons
        const setBudgetBtn = document.getElementById('set-budget-btn');
        const setGoalBtn = document.getElementById('set-goal-btn');

        if (setBudgetBtn) {
            setBudgetBtn.addEventListener('click', () => this.showSetBudgetModal());
        }

        if (setGoalBtn) {
            setGoalBtn.addEventListener('click', () => this.showSetGoalModal());
        }

        // Export data button
        const exportDataBtn = document.getElementById('export-data');
        if (exportDataBtn) {
            exportDataBtn.addEventListener('click', () => this.showExportModal());
        }

        // Load saved budget and goals
        this.loadBudgetAndGoals();
    }

    handleDateRangeChange() {
        const customDateRange = document.getElementById('custom-date-range');
        
        if (this.currentDateRange === 'custom') {
            if (customDateRange) {
                customDateRange.style.display = 'block';
            }
        } else {
            if (customDateRange) {
                customDateRange.style.display = 'none';
                // Reset custom date filters when switching away from custom
                this.currentFilters.startDate = null;
                this.currentFilters.endDate = null;
                // Clear the date input fields
                const startDateInput = document.getElementById('start-date');
                const endDateInput = document.getElementById('end-date');
                if (startDateInput) startDateInput.value = '';
                if (endDateInput) endDateInput.value = '';
            }
            this.updateDashboardData();
        }
    }

    getFilteredExpenses(expensesToFilter = null) {
        const expenses = expensesToFilter || this.expenses;
        let filtered = [...expenses];
        
        // Date range filtering
        const now = new Date();
        let startDate, endDate;

        switch (this.currentDateRange) {
            case 'this-month':
                startDate = new Date(now.getFullYear(), now.getMonth(), 1);
                endDate = new Date(now.getFullYear(), now.getMonth() + 1, 0);
                break;
            case 'last-7-days':
                startDate = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
                endDate = now;
                break;
            case 'last-30-days':
                startDate = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
                endDate = now;
                break;
            case 'last-3-months':
                startDate = new Date(now.getFullYear(), now.getMonth() - 3, 1);
                endDate = now;
                break;
            case 'custom':
                if (this.currentFilters.startDate && this.currentFilters.endDate) {
                    startDate = new Date(this.currentFilters.startDate);
                    endDate = new Date(this.currentFilters.endDate);
                }
                break;
        }

        if (startDate && endDate) {
            filtered = filtered.filter(expense => {
                const expenseDate = new Date(expense.date);
                return expenseDate >= startDate && expenseDate <= endDate;
            });
        }

        // Category filtering
        if (this.currentFilters.category !== 'all') {
            filtered = filtered.filter(expense => 
                expense.categoryId === parseInt(this.currentFilters.category)
            );
        }

        // Payment method filtering
        if (this.currentFilters.paymentMethod !== 'all') {
            filtered = filtered.filter(expense => 
                expense.paymentMethod === this.currentFilters.paymentMethod
            );
        }

        // Tag filtering
        if (this.currentFilters.tags !== 'all') {
            filtered = filtered.filter(expense => 
                expense.tags && expense.tags.includes(this.currentFilters.tags)
            );
        }

        return filtered;
    }

    async updateDashboardData() {
        // Get all expenses for filtering (not just current month)
        const allExpenses = await this.getAllExpensesForFiltering();
        const filteredExpenses = this.getFilteredExpenses(allExpenses);
        
        this.updateQuickInsights(filteredExpenses);
        this.updateBudgetTracking(filteredExpenses);
        this.updateSmartHighlights(filteredExpenses);
    }

    async getAllExpensesForFiltering() {
        // Try to get expenses from multiple months for better filtering
        try {
            const currentDate = new Date();
            const promises = [];
            
            // Get last 12 months of data for comprehensive filtering
            for (let i = 0; i < 12; i++) {
                const date = new Date(currentDate.getFullYear(), currentDate.getMonth() - i, 1);
                const year = date.getFullYear();
                const month = date.getMonth() + 1;
                
                promises.push(
                    fetch(`/api/expenses/month/${year}/${month}`)
                        .then(response => response.ok ? response.json() : [])
                        .catch(() => [])
                );
            }
            
            const monthlyResults = await Promise.all(promises);
            const allExpenses = monthlyResults.flat();
            
            return allExpenses.length > 0 ? allExpenses : this.expenses;
        } catch (error) {
            console.error('Failed to load expenses for filtering:', error);
            return this.expenses;
        }
    }

    updateQuickInsights(expenses) {
        // Top 3 Categories
        const categoryTotals = {};
        expenses.forEach(expense => {
            const categoryName = expense.category?.name || 
                               this.categories.find(c => c.id === expense.categoryId)?.name || 
                               'Unknown';
            categoryTotals[categoryName] = (categoryTotals[categoryName] || 0) + expense.amount;
        });

        const topCategories = Object.entries(categoryTotals)
            .sort(([,a], [,b]) => b - a)
            .slice(0, 3);

        const topCategoriesElement = document.getElementById('top-categories-list');
        if (topCategoriesElement) {
            topCategoriesElement.innerHTML = topCategories.length > 0 
                ? topCategories.map(([name, amount]) => 
                    `<div class="insight-item">${name}: $${amount.toFixed(2)}</div>`
                  ).join('')
                : '<div class="insight-item">No data available</div>';
        }

        // Biggest Expense
        const biggestExpense = expenses.reduce((max, expense) => 
            expense.amount > (max?.amount || 0) ? expense : max, null);

        const biggestExpenseElement = document.getElementById('biggest-expense');
        if (biggestExpenseElement) {
            if (biggestExpense) {
                biggestExpenseElement.innerHTML = `
                    <div class="expense-amount">$${biggestExpense.amount.toFixed(2)}</div>
                    <div class="expense-details">${biggestExpense.description}</div>
                `;
            } else {
                biggestExpenseElement.innerHTML = `
                    <div class="expense-amount">$0.00</div>
                    <div class="expense-details">No expenses yet</div>
                `;
            }
        }

        // Daily Average
        const totalAmount = expenses.reduce((sum, expense) => sum + expense.amount, 0);
        const dayCount = this.getDayCountForPeriod();
        const dailyAverage = dayCount > 0 ? totalAmount / dayCount : 0;

        const dailyAverageElement = document.getElementById('daily-average');
        if (dailyAverageElement) {
            dailyAverageElement.innerHTML = `
                <div class="average-amount">$${dailyAverage.toFixed(2)}</div>
                <div class="average-period">per day</div>
            `;
        }
    }

    getDayCountForPeriod() {
        const now = new Date();
        switch (this.currentDateRange) {
            case 'this-month':
                return now.getDate();
            case 'last-7-days':
                return 7;
            case 'last-30-days':
                return 30;
            case 'last-3-months':
                return 90;
            case 'custom':
                if (this.currentFilters.startDate && this.currentFilters.endDate) {
                    const start = new Date(this.currentFilters.startDate);
                    const end = new Date(this.currentFilters.endDate);
                    return Math.ceil((end - start) / (1000 * 60 * 60 * 24)) + 1;
                }
                return 1;
            default:
                return 1;
        }
    }

    updateBudgetTracking(expenses) {
        const totalSpent = expenses.reduce((sum, expense) => sum + expense.amount, 0);
        
        // Budget Progress
        const budgetProgressElement = document.getElementById('budget-progress-fill');
        const budgetSpentElement = document.getElementById('budget-spent');
        const budgetTotalElement = document.getElementById('budget-total');
        const budgetPercentageElement = document.getElementById('budget-percentage');

        if (this.monthlyBudget > 0) {
            const percentage = Math.min((totalSpent / this.monthlyBudget) * 100, 100);
            
            if (budgetProgressElement) {
                budgetProgressElement.style.width = `${percentage}%`;
                budgetProgressElement.className = `progress-fill ${percentage > 90 ? 'over-budget' : percentage > 75 ? 'warning' : ''}`;
            }
            
            if (budgetSpentElement) {
                budgetSpentElement.textContent = `$${totalSpent.toFixed(2)}`;
            }
            
            if (budgetTotalElement) {
                budgetTotalElement.textContent = `$${this.monthlyBudget.toFixed(2)}`;
            }
            
            if (budgetPercentageElement) {
                budgetPercentageElement.textContent = `${percentage.toFixed(1)}%`;
            }
        } else {
            if (budgetSpentElement) {
                budgetSpentElement.textContent = '$0';
            }
            if (budgetTotalElement) {
                budgetTotalElement.textContent = '$0';
            }
            if (budgetPercentageElement) {
                budgetPercentageElement.textContent = '0%';
            }
        }

        // Savings Goal Progress
        const savingsProgressElement = document.getElementById('savings-progress-fill');
        const savingsAmountElement = document.getElementById('savings-amount');
        const savingsGoalElement = document.getElementById('savings-goal');

        if (this.savingsGoal > 0) {
            const saved = Math.max(this.monthlyBudget - totalSpent, 0);
            const percentage = Math.min((saved / this.savingsGoal) * 100, 100);
            
            if (savingsProgressElement) {
                savingsProgressElement.style.width = `${percentage}%`;
            }
            
            if (savingsAmountElement) {
                savingsAmountElement.textContent = `$${saved.toFixed(2)}`;
            }
            
            if (savingsGoalElement) {
                savingsGoalElement.textContent = `$${this.savingsGoal.toFixed(2)}`;
            }
        } else {
            if (savingsAmountElement) {
                savingsAmountElement.textContent = '$0';
            }
            if (savingsGoalElement) {
                savingsGoalElement.textContent = '$0';
            }
        }
    }

    updateSmartHighlights(expenses) {
        const highlights = [];
        
        // Budget warnings
        if (this.monthlyBudget > 0) {
            const totalSpent = expenses.reduce((sum, expense) => sum + expense.amount, 0);
            const percentage = (totalSpent / this.monthlyBudget) * 100;
            
            if (percentage > 100) {
                highlights.push({
                    type: 'warning',
                    message: `You've exceeded your budget by $${(totalSpent - this.monthlyBudget).toFixed(2)}`
                });
            } else if (percentage > 90) {
                highlights.push({
                    type: 'warning',
                    message: `You're close to your budget limit (${percentage.toFixed(1)}% used)`
                });
            }
        }

        // Spending patterns
        if (expenses.length > 0) {
            const avgExpense = expenses.reduce((sum, e) => sum + e.amount, 0) / expenses.length;
            const largeExpenses = expenses.filter(e => e.amount > avgExpense * 2);
            
            if (largeExpenses.length > 0) {
                highlights.push({
                    type: 'info',
                    message: `${largeExpenses.length} unusually large expense(s) detected`
                });
            }
        }

        const highlightsElement = document.getElementById('highlights-list');
        if (highlightsElement) {
            if (expenses.length === 0) {
                highlightsElement.innerHTML = '<div class="highlight info">No expenses found for the selected period.</div>';
                return;
            }
            
            // Enhanced highlights logic
            const totalSpent = expenses.reduce((sum, expense) => sum + expense.amount, 0);
            
            // Add more detailed spending analysis
            const avgExpense = totalSpent / expenses.length;
            const largeExpenses = expenses.filter(e => e.amount > avgExpense * 2);
            
            if (largeExpenses.length > 0) {
                highlights.push({
                    type: 'info',
                    message: `${largeExpenses.length} unusually large expense(s) detected (over $${(avgExpense * 2).toFixed(2)})`
                });
            }
            
            // Category concentration analysis
            const categoryTotals = {};
            expenses.forEach(expense => {
                const categoryName = expense.category?.name || 
                                   this.categories.find(c => c.id === expense.categoryId)?.name || 
                                   'Unknown';
                categoryTotals[categoryName] = (categoryTotals[categoryName] || 0) + expense.amount;
            });
            
            const topCategory = Object.entries(categoryTotals)
                .sort(([,a], [,b]) => b - a)[0];
                
            if (topCategory && topCategory[1] > totalSpent * 0.5) {
                highlights.push({
                    type: 'info',
                    message: `${topCategory[0]} accounts for ${((topCategory[1] / totalSpent) * 100).toFixed(1)}% of your spending`
                });
            }
            
            highlightsElement.innerHTML = highlights.length > 0
                ? highlights.map(h => `<div class="highlight ${h.type}">${h.message}</div>`).join('')
                : '<div class="highlight info">All looks good! Keep tracking your expenses.</div>';
        }
    }

    populateFilterDropdowns() {
        // Populate category filter
        const categoryFilter = document.getElementById('category-filter');
        if (categoryFilter && this.categories.length > 0) {
            // Clear existing options except "All Categories"
            while (categoryFilter.children.length > 1) {
                categoryFilter.removeChild(categoryFilter.lastChild);
            }
            
            this.categories.forEach(category => {
                const option = document.createElement('option');
                option.value = category.id;
                option.textContent = category.name;
                categoryFilter.appendChild(option);
            });
        }
    }

    async showSetBudgetModal() {
        const budget = prompt('Enter your monthly budget:', this.monthlyBudget || '');
        if (budget && !isNaN(budget) && parseFloat(budget) > 0) {
            this.monthlyBudget = parseFloat(budget);
            await this.saveSetting('monthlyBudget', this.monthlyBudget);
            this.updateDashboardData();
            this.ui.showToast('Monthly budget updated!', 'success');
        }
    }

    async showSetGoalModal() {
        const goal = prompt('Enter your savings goal:', this.savingsGoal || '');
        if (goal && !isNaN(goal) && parseFloat(goal) > 0) {
            this.savingsGoal = parseFloat(goal);
            await this.saveSetting('savingsGoal', this.savingsGoal);
            this.updateDashboardData();
            this.ui.showToast('Savings goal updated!', 'success');
        }
    }

    async showExportModal() {
        const format = prompt('Export format (csv/pdf):', 'csv');
        if (format === 'csv' || format === 'pdf') {
            await this.exportData(format);
        }
    }

    async exportData(format) {
        let expensesToExport;
        
        // If we're on the analytics page, export the selected month's data
        if (this.currentPage === 'analytics' && this.analytics) {
            expensesToExport = await this.analytics.getSelectedMonthExpenses();
        } else {
            // Otherwise, use filtered expenses from dashboard
            expensesToExport = this.getFilteredExpenses();
        }
        
        if (expensesToExport.length === 0) {
            this.ui.showToast('No expenses found for the selected period to export', 'warning');
            return;
        }
        
        if (format === 'csv') {
            this.exportCSV(expensesToExport);
        } else if (format === 'pdf') {
            this.exportPDF(expensesToExport);
        }
    }

    exportCSV(expenses) {
        const headers = ['Date', 'Description', 'Amount', 'Category', 'Payment Method', 'Tags'];
        const rows = expenses.map(expense => [
            expense.date,
            expense.description,
            expense.amount,
            expense.category?.name || this.categories.find(c => c.id === expense.categoryId)?.name || 'Unknown',
            expense.paymentMethod || 'Not specified',
            expense.tags || ''
        ]);

        const csvContent = [headers, ...rows]
            .map(row => row.map(field => `"${field}"`).join(','))
            .join('\n');

        const blob = new Blob([csvContent], { type: 'text/csv' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `expenses-${new Date().toISOString().split('T')[0]}.csv`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);

        this.ui.showToast('CSV exported successfully!', 'success');
    }

    exportPDF(expenses) {
        // Simple PDF export using browser print
        const printWindow = window.open('', '_blank');
        const totalAmount = expenses.reduce((sum, expense) => sum + expense.amount, 0);
        
        printWindow.document.write(`
            <html>
                <head>
                    <title>Expense Report</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 20px; }
                        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
                        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                        th { background-color: #f2f2f2; }
                        .header { text-align: center; margin-bottom: 20px; }
                        .summary { margin-top: 20px; font-weight: bold; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h1>Expense Report</h1>
                        <p>Generated on ${new Date().toLocaleDateString()}</p>
                        <p>Period: ${this.currentDateRange}</p>
                    </div>
                    
                    <table>
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Description</th>
                                <th>Amount</th>
                                <th>Category</th>
                                <th>Payment Method</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${expenses.map(expense => `
                                <tr>
                                    <td>${expense.date}</td>
                                    <td>${expense.description}</td>
                                    <td>$${expense.amount.toFixed(2)}</td>
                                    <td>${expense.category?.name || this.categories.find(c => c.id === expense.categoryId)?.name || 'Unknown'}</td>
                                    <td>${expense.paymentMethod || 'Not specified'}</td>
                                </tr>
                            `).join('')}
                        </tbody>
                    </table>
                    
                    <div class="summary">
                        <p>Total Expenses: $${totalAmount.toFixed(2)}</p>
                        <p>Number of Transactions: ${expenses.length}</p>
                    </div>
                </body>
            </html>
        `);
        
        printWindow.document.close();
        printWindow.print();
        
        this.ui.showToast('PDF export ready for printing!', 'success');
    }

    async loadBudgetAndGoals() {
        this.monthlyBudget = await this.getSetting('monthlyBudget', 0);
        this.savingsGoal = await this.getSetting('savingsGoal', 0);
    }

    // Theme Management
    async initializeTheme() {
        const savedTheme = await this.getSetting('theme', 'light');
        this.applyTheme(savedTheme);
        
        // Update theme selector if it exists
        const themeSelector = document.getElementById('theme-selector');
        if (themeSelector) {
            themeSelector.value = savedTheme;
        }
    }

    async changeTheme(theme) {
        // Apply the theme
        this.applyTheme(theme);
        
        // Save the theme preference
        await this.saveSetting('theme', theme);
        
        // Show confirmation
        this.ui.showToast(`Theme changed to ${theme}`, 'success', 2000);
    }

    applyTheme(theme) {
        // Set the data-theme attribute on the document element
        document.documentElement.setAttribute('data-theme', theme);
        
        // Also set it on the body for compatibility
        document.body.setAttribute('data-theme', theme);
        
        console.log('Theme applied:', theme);
    }

    // IndexedDB Helper Methods
    async saveToIndexedDB(storeName, data) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([storeName], 'readwrite');
            const store = transaction.objectStore(storeName);
            
            if (Array.isArray(data)) {
                // Save multiple items
                data.forEach(item => store.put(item));
            } else {
                // Save single item
                store.put(data);
            }
            
            transaction.oncomplete = () => resolve();
            transaction.onerror = () => reject(transaction.error);
        });
    }

    async getFromIndexedDB(storeName, key = null) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([storeName], 'readonly');
            const store = transaction.objectStore(storeName);
            
            if (key) {
                const request = store.get(key);
                request.onsuccess = () => resolve(request.result);
                request.onerror = () => reject(request.error);
            } else {
                const request = store.getAll();
                request.onsuccess = () => resolve(request.result);
                request.onerror = () => reject(request.error);
            }
        });
    }

    async deleteFromIndexedDB(storeName, key) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([storeName], 'readwrite');
            const store = transaction.objectStore(storeName);
            const request = store.delete(key);
            
            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    async saveMonthlyExpensesToIndexedDB() {
        const monthKey = `${this.currentYear}-${this.currentMonth + 1}`;
        await this.saveSetting(`expenses-${monthKey}`, this.expenses);
    }

    async getMonthlyExpensesFromIndexedDB() {
        const monthKey = `${this.currentYear}-${this.currentMonth + 1}`;
        return await this.getSetting(`expenses-${monthKey}`, []);
    }

    async saveSetting(key, value) {
        await this.saveToIndexedDB('settings', { key, value });
    }

    async getSetting(key, defaultValue = null) {
        const setting = await this.getFromIndexedDB('settings', key);
        return setting ? setting.value : defaultValue;
    }

    // Missing methods for reminders and other functionality
    showAddReminderForm() {
        // Simple implementation for now
        const reminderName = prompt('Enter reminder name:');
        const reminderAmount = prompt('Enter reminder amount:');
        const reminderDate = prompt('Enter due date (YYYY-MM-DD):');
        
        if (reminderName && reminderAmount && reminderDate) {
            this.ui.showToast('Reminder functionality coming soon!', 'info');
        }
    }

    // Override displayRecentExpenses to do nothing since we removed that section
    displayRecentExpenses() {
        // This method is now empty since we removed the recent expenses section
    }
}

// Initialize the app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.app = new ExpenseTrackerApp();
});