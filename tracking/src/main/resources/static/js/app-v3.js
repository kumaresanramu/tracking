// Main Application Controller
class ExpenseTrackerApp {
    constructor() {
        this.currentPage = 'dashboard';
        this.currentMonth = new Date().getMonth();
        this.currentYear = new Date().getFullYear();
        this.expenses = [];
        this.categories = [];
        
        this.init();
    }

    async init() {
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
        
        console.log('Expense Tracker App initialized');
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

        // Expense form
        const expenseForm = document.getElementById('expense-form');
        if (expenseForm) {
            expenseForm.addEventListener('submit', (e) => this.handleExpenseSubmit(e));
            
            // Real-time validation
            const amountInput = document.getElementById('expense-amount');
            const descriptionInput = document.getElementById('expense-description');
            const categorySelect = document.getElementById('expense-category');
            
            if (amountInput) {
                amountInput.addEventListener('input', (e) => {
                    const value = parseFloat(e.target.value);
                    if (e.target.value && (isNaN(value) || value <= 0)) {
                        this.ui.addFieldError(e.target);
                        this.ui.showToast('Amount must be a positive number', 'error', 2000);
                    } else {
                        this.ui.removeFieldError(e.target);
                    }
                });
            }
            
            if (descriptionInput) {
                descriptionInput.addEventListener('input', (e) => {
                    if (e.target.value.trim().length === 0) {
                        this.ui.addFieldError(e.target);
                    } else {
                        this.ui.removeFieldError(e.target);
                    }
                });
            }
            
            if (categorySelect) {
                categorySelect.addEventListener('change', (e) => {
                    if (!e.target.value) {
                        this.ui.addFieldError(e.target);
                    } else {
                        this.ui.removeFieldError(e.target);
                    }
                });
            }
        }

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
        
        // Auto-sync functionality has been removed
    }

    async initIndexedDB() {
        return new Promise((resolve, reject) => {
            const request = indexedDB.open('ExpenseTrackerDB', 2);
            
            request.onerror = () => {
                console.error('Failed to open IndexedDB');
                reject(request.error);
            };
            
            request.onsuccess = (event) => {
                this.db = event.target.result;
                console.log('IndexedDB initialized');
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
                    const categoryStore = db.createObjectStore('categories', { keyPath: 'id', autoIncrement: true });
                    categoryStore.createIndex('name', 'name', { unique: false });
                    categoryStore.createIndex('parentId', 'parentId', { unique: false });
                }
                
                // Create app settings store
                if (!db.objectStoreNames.contains('settings')) {
                    db.createObjectStore('settings', { keyPath: 'key' });
                }
                
                // Remove sync-related stores - no longer needed
                
                console.log('IndexedDB schema created/updated');
            };
        });
    }

    async loadInitialData() {
        try {
            // Load categories first
            await this.loadCategories();
            
            // Load expenses for current month
            await this.loadMonthlyExpenses();
            
            // Update dashboard stats
            this.updateDashboardStats();
            
        } catch (error) {
            console.error('Failed to load initial data:', error);
            
            // Use error handler for better user experience
            this.errorHandler.handleError(error, 'Initial Data Load', {
                onRetry: () => this.loadInitialData(),
                silent: false
            });
            
            // Try to load from local storage
            await this.loadOfflineData();
        }
    }

    async loadCategories() {
        try {
            this.categories = await this.expenseService.getCategories();
            this.populateCategoryDropdown();
        } catch (error) {
            console.error('Failed to load categories:', error);
            
            // Handle error with retry option
            this.errorHandler.handleApiError(error, '/api/categories', {
                onRetry: () => this.loadCategories(),
                silent: true // Don't show toast since this is called from loadInitialData
            });
            
            // Load from IndexedDB if available
            this.categories = await this.getFromIndexedDB('categories') || [];
            this.populateCategoryDropdown();
        }
    }

    async loadMonthlyExpenses() {
        try {
            this.expenses = await this.expenseService.getMonthlyExpenses(this.currentYear, this.currentMonth + 1);
            this.displayMonthlyExpenses();
        } catch (error) {
            console.error('Failed to load monthly expenses:', error);
            
            // Handle error with retry option
            this.errorHandler.handleApiError(error, '/api/expenses/month', {
                onRetry: () => this.loadMonthlyExpenses(),
                silent: true // Don't show toast since this is called from loadInitialData
            });
            
            // Load from IndexedDB if available
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
            this.updateDashboardStats();
        } catch (error) {
            console.error('Failed to load offline data:', error);
        }
    }

    setupConnectivityMonitoring() {
        // Monitor online/offline status - simplified for local-only operation
        window.addEventListener('online', () => {
            console.log('Connection restored');
        });
        
        window.addEventListener('offline', () => {
            console.log('Connection lost - operating in offline mode');
        });
    }

    navigateToPage(page) {
        // Update navigation
        document.querySelectorAll('.nav-item').forEach(item => {
            item.classList.remove('active');
        });
        
        document.querySelector(`[data-page="${page}"]`).classList.add('active');
        
        // Show/hide pages
        document.querySelectorAll('.page').forEach(pageEl => {
            pageEl.classList.remove('active');
        });
        
        document.getElementById(`${page}-page`).classList.add('active');
        
        this.currentPage = page;
        
        // Load page-specific data
        this.loadPageData(page);
    }

    async loadPageData(page) {
        switch (page) {
            case 'dashboard':
                this.updateDashboardStats();
                this.displayRecentExpenses();
                break;
            case 'expenses':
                this.displayMonthlyExpenses();
                break;
            case 'analytics':
                console.log('App: Navigating to analytics page');
                // Wait for the page to be visible before loading charts
                requestAnimationFrame(async () => {
                    await this.analytics.loadCharts();
                    console.log('App: Analytics charts loading completed');
                });
                break;
            case 'reminders':
                await this.loadReminders();
                break;
            case 'settings':
                // Load settings
                break;
        }
    }

    async handleExpenseSubmit(e) {
        e.preventDefault();
        
        // Validate form
        const validationErrors = this.ui.validateForm(e.target);
        if (validationErrors.length > 0) {
            this.errorHandler.handleFormValidationError(e.target, 
                validationErrors.map(msg => ({ field: 'general', message: msg }))
            );
            return;
        }
        
        const formData = new FormData(e.target);
        const expense = {
            description: formData.get('description').trim(),
            amount: parseFloat(formData.get('amount')),
            date: formData.get('date'),
            categoryId: parseInt(formData.get('categoryId')),
            createdAt: new Date().toISOString()
        };
        
        // Additional validation
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
        expense.year = expenseDate.getFullYear();
        expense.month = expenseDate.getMonth() + 1;
        
        try {
            let savedExpense;
            
            // Check if we're editing an existing expense
            if (this.editingExpenseId) {
                // Update existing expense
                savedExpense = await this.expenseService.updateExpense(this.editingExpenseId, expense);
                
                // Update in local array
                const index = this.expenses.findIndex(e => e.id === this.editingExpenseId);
                if (index !== -1) {
                    this.expenses[index] = savedExpense;
                }
                
                // Also update in IndexedDB
                await this.saveToIndexedDB('expenses', savedExpense);
                
                this.ui.showToast('Expense updated successfully!', 'success');
                
                // Clear editing mode
                this.editingExpenseId = null;
            } else {
                // Create new expense
                savedExpense = await this.expenseService.createExpense(expense);
                this.expenses.unshift(savedExpense);
                
                // Also save to IndexedDB for offline access
                await this.saveToIndexedDB('expenses', savedExpense);
                
                this.ui.showToast('Expense added successfully!', 'success');
            }
            
            // Update UI
            this.displayMonthlyExpenses();
            this.updateDashboardStats();
            
            // Reset form
            e.target.reset();
            
            // Set today's date as default
            document.getElementById('expense-date').value = new Date().toISOString().split('T')[0];
            
            // Clear any validation errors
            e.target.querySelectorAll('.form-group input, .form-group select').forEach(field => {
                this.ui.removeFieldError(field);
            });
            
        } catch (error) {
            console.error('Failed to save expense:', error);
            
            // Use error handler for better error reporting
            this.errorHandler.handleApiError(error, '/api/expenses', {
                onRetry: () => this.handleExpenseSubmit(e),
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
        if (!categorySelect) return;
        
        // Clear existing options (except the first one)
        while (categorySelect.children.length > 1) {
            categorySelect.removeChild(categorySelect.lastChild);
        }
        
        // Build hierarchical category options
        const hierarchicalCategories = this.buildCategoryHierarchy(this.categories);
        this.addCategoryOptionsToSelect(categorySelect, hierarchicalCategories, 0);
    }

    buildCategoryHierarchy(categories) {
        const categoryMap = {};
        const rootCategories = [];
        
        // Create category map
        categories.forEach(category => {
            categoryMap[category.id] = { ...category, children: [] };
        });
        
        // Build hierarchy
        categories.forEach(category => {
            if (category.parentId && categoryMap[category.parentId]) {
                categoryMap[category.parentId].children.push(categoryMap[category.id]);
            } else {
                rootCategories.push(categoryMap[category.id]);
            }
        });
        
        return rootCategories;
    }

    addCategoryOptionsToSelect(selectElement, categories, level) {
        categories.forEach(category => {
            const option = document.createElement('option');
            option.value = category.id;
            
            // Add indentation for subcategories
            const indent = '  '.repeat(level);
            option.textContent = `${indent}${category.name}`;
            
            selectElement.appendChild(option);
            
            // Add subcategories recursively
            if (category.children && category.children.length > 0) {
                this.addCategoryOptionsToSelect(selectElement, category.children, level + 1);
            }
        });
    }

    displayMonthlyExpenses() {
        const container = document.getElementById('monthly-expenses-list');
        if (!container) return;
        
        if (this.expenses.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">💰</div>
                    <div class="empty-state-title">No expenses yet</div>
                    <div class="empty-state-description">Add your first expense to get started!</div>
                </div>
            `;
            return;
        }
        
        container.innerHTML = this.expenses.map(expense => {
            // Handle both API formats: expense.category.name (from API) or expense.categoryId (from local)
            let categoryName = 'Unknown';
            if (expense.category && expense.category.name) {
                // API format: expense has category object with name
                categoryName = expense.category.name;
            } else if (expense.categoryId) {
                // Local format: expense has categoryId, need to find in categories array
                const category = this.categories.find(c => c.id === expense.categoryId);
                categoryName = category ? category.name : 'Unknown';
            }
            
            return `
                <div class="expense-item">
                    <div class="expense-details">
                        <div class="expense-description">${expense.description}</div>
                        <div class="expense-meta">${categoryName} • ${new Date(expense.date).toLocaleDateString()}</div>
                    </div>
                    <div class="expense-amount">$${expense.amount.toFixed(2)}</div>
                    <div class="expense-item-actions">
                        <button class="edit-btn" onclick="app.editExpense(${expense.id})">✏️</button>
                        <button class="delete-btn" onclick="app.deleteExpense(${expense.id})">🗑️</button>
                    </div>
                </div>
            `;
        }).join('');
    }

    displayRecentExpenses() {
        const container = document.getElementById('recent-expenses-list');
        if (!container) return;
        
        const recentExpenses = this.expenses.slice(0, 5);
        
        if (recentExpenses.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-title">No recent expenses</div>
                    <button class="btn btn-primary" onclick="app.navigateToPage('expenses')">Add your first expense</button>
                </div>
            `;
            return;
        }
        
        container.innerHTML = recentExpenses.map(expense => {
            // Handle both API formats: expense.category.name (from API) or expense.categoryId (from local)
            let categoryName = 'Unknown';
            if (expense.category && expense.category.name) {
                // API format: expense has category object with name
                categoryName = expense.category.name;
            } else if (expense.categoryId) {
                // Local format: expense has categoryId, need to find in categories array
                const category = this.categories.find(c => c.id === expense.categoryId);
                categoryName = category ? category.name : 'Unknown';
            }
            
            return `
                <div class="expense-item">
                    <div class="expense-details">
                        <div class="expense-description">${expense.description}</div>
                        <div class="expense-meta">${categoryName} • ${new Date(expense.date).toLocaleDateString()}</div>
                    </div>
                    <div class="expense-amount">$${expense.amount.toFixed(2)}</div>
                </div>
            `;
        }).join('');
    }

    updateDashboardStats() {
        const now = new Date();
        const thisMonth = this.expenses.filter(e => {
            const expenseDate = new Date(e.date);
            return expenseDate.getMonth() === now.getMonth() && 
                   expenseDate.getFullYear() === now.getFullYear();
        });
        
        const last7Days = this.expenses.filter(e => {
            const expenseDate = new Date(e.date);
            const daysDiff = (now - expenseDate) / (1000 * 60 * 60 * 24);
            return daysDiff <= 7;
        });
        
        const monthlyTotal = thisMonth.reduce((sum, e) => sum + e.amount, 0);
        const weeklyTotal = last7Days.reduce((sum, e) => sum + e.amount, 0);
        
        // Update UI
        const monthlyElement = document.getElementById('monthly-total');
        const weeklyElement = document.getElementById('weekly-total');
        const countElement = document.getElementById('total-count');
        
        if (monthlyElement) monthlyElement.textContent = `$${monthlyTotal.toFixed(2)}`;
        if (weeklyElement) weeklyElement.textContent = `$${weeklyTotal.toFixed(2)}`;
        if (countElement) countElement.textContent = this.expenses.length.toString();
    }

    async editExpense(id) {
        try {
            // Find the expense to edit
            const expense = this.expenses.find(e => e.id === id);
            if (!expense) {
                this.ui.showToast('Expense not found', 'error');
                return;
            }

            // Get the category ID - handle both API formats
            let categoryId = expense.categoryId;
            if (expense.category && expense.category.id) {
                categoryId = expense.category.id;
            }

            // Pre-fill the form with expense data
            document.getElementById('expense-description').value = expense.description;
            document.getElementById('expense-amount').value = expense.amount;
            document.getElementById('expense-date').value = expense.date;
            document.getElementById('expense-category').value = categoryId;

            // Navigate to expenses page
            this.navigateToPage('expenses');

            // Show edit mode message
            this.ui.showToast('Editing expense - modify the form and submit to update', 'info', 5000);

            // Store the expense ID for updating instead of creating
            this.editingExpenseId = id;

        } catch (error) {
            console.error('Failed to edit expense:', error);
            this.ui.showToast('Failed to load expense for editing', 'error');
        }
    }

    async deleteExpense(id) {
        if (!confirm('Are you sure you want to delete this expense?')) {
            return;
        }
        
        try {
            if (navigator.onLine) {
                await this.expenseService.deleteExpense(id);
            }
            
            // Remove from local arrays
            this.expenses = this.expenses.filter(e => e.id !== id);
            
            // Remove from IndexedDB
            await this.deleteFromIndexedDB('expenses', id);
            
            // Update UI
            this.displayMonthlyExpenses();
            this.updateDashboardStats();
            
            this.ui.showToast('Expense deleted successfully!', 'success');
            
        } catch (error) {
            console.error('Failed to delete expense:', error);
            
            // Use error handler for better error reporting
            this.errorHandler.handleApiError(error, `/api/expenses/${id}`, {
                onRetry: () => this.deleteExpense(id),
                retryable: true
            });
        }
    }

    updateUI() {
        // Set today's date as default in expense form
        const dateInput = document.getElementById('expense-date');
        if (dateInput && !dateInput.value) {
            dateInput.value = new Date().toISOString().split('T')[0];
        }
        
        // Update month display
        this.updateMonthDisplay();
        
        // Focus on description field for better UX
        const descriptionInput = document.getElementById('expense-description');
        if (descriptionInput && this.currentPage === 'expenses') {
            setTimeout(() => descriptionInput.focus(), 100);
        }
    }

    // IndexedDB helper methods
    async saveToIndexedDB(storeName, data) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([storeName], 'readwrite');
            const store = transaction.objectStore(storeName);
            const request = store.put(data);
            
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    async getFromIndexedDB(storeName, key = null) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([storeName], 'readonly');
            const store = transaction.objectStore(storeName);
            const request = key ? store.get(key) : store.getAll();
            
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    async getMonthlyExpensesFromIndexedDB() {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction(['expenses'], 'readonly');
            const store = transaction.objectStore('expenses');
            const index = store.index('month');
            const request = index.getAll([this.currentYear, this.currentMonth + 1]);
            
            request.onsuccess = () => {
                const expenses = request.result || [];
                // Sort by date descending
                expenses.sort((a, b) => new Date(b.date) - new Date(a.date));
                resolve(expenses);
            };
            
            request.onerror = () => reject(request.error);
        });
    }

    async deleteFromIndexedDB(storeName, id) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([storeName], 'readwrite');
            const store = transaction.objectStore(storeName);
            const request = store.delete(id);
            
            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    async clearIndexedDBStore(storeName) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([storeName], 'readwrite');
            const store = transaction.objectStore(storeName);
            const request = store.clear();
            
            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    }

    async getOfflineExpenseCount() {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction(['offlineExpenses'], 'readonly');
            const store = transaction.objectStore('offlineExpenses');
            const request = store.count();
            
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    async saveSetting(key, value) {
        return this.saveToIndexedDB('settings', { key, value, updatedAt: new Date().toISOString() });
    }

    async getSetting(key, defaultValue = null) {
        try {
            const setting = await this.getFromIndexedDB('settings', key);
            return setting ? setting.value : defaultValue;
        } catch (error) {
            console.error('Failed to get setting:', error);
            return defaultValue;
        }
    }

    async loadReminders() {
        try {
            const reminders = await this.expenseService.getPaymentReminders();
            this.displayReminders(reminders);
        } catch (error) {
            console.error('Failed to load reminders:', error);
            
            // Handle error with retry option
            this.errorHandler.handleApiError(error, '/api/reminders', {
                onRetry: () => this.loadReminders(),
                silent: false
            });
        }
    }

    displayReminders(reminders) {
        const container = document.getElementById('reminders-list');
        if (!container) return;
        
        if (reminders.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">🔔</div>
                    <div class="empty-state-title">No payment reminders</div>
                    <div class="empty-state-description">Set up reminders for recurring payments</div>
                    <button class="btn btn-primary" onclick="app.showAddReminderForm()">Add Reminder</button>
                </div>
            `;
            return;
        }
        
        container.innerHTML = reminders.map(reminder => {
            const dueDate = new Date(reminder.dueDate);
            const isOverdue = dueDate < new Date();
            const statusClass = isOverdue ? 'overdue' : 'upcoming';
            
            return `
                <div class="reminder-item ${statusClass}">
                    <div class="reminder-details">
                        <div class="reminder-name">${reminder.name}</div>
                        <div class="reminder-meta">
                            ${reminder.amount.toFixed(2)} • Due: ${dueDate.toLocaleDateString()}
                            ${reminder.frequency ? ` • ${reminder.frequency}` : ''}
                        </div>
                    </div>
                    <div class="reminder-actions">
                        <button class="btn btn-small btn-primary" onclick="app.markReminderAsPaid(${reminder.id})">
                            Mark Paid
                        </button>
                        <button class="btn btn-small btn-secondary" onclick="app.editReminder(${reminder.id})">
                            Edit
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    }

    async markReminderAsPaid(reminderId) {
        try {
            await this.expenseService.markReminderAsPaid(reminderId);
            this.ui.showToast('Reminder marked as paid!', 'success');
            await this.loadReminders(); // Refresh the list
        } catch (error) {
            console.error('Failed to mark reminder as paid:', error);
            this.errorHandler.handleApiError(error, `/api/reminders/${reminderId}/mark-paid`, {
                onRetry: () => this.markReminderAsPaid(reminderId),
                retryable: true
            });
        }
    }

    showAddReminderForm() {
        this.ui.showModal('Add Payment Reminder', this.createReminderFormHTML(), {
            onConfirm: () => this.handleAddReminder(),
            confirmText: 'Add Reminder',
            cancelText: 'Cancel'
        });
        
        // Populate categories dropdown after modal is shown
        this.populateReminderCategories();
    }

    createReminderFormHTML() {
        return `
            <form id="add-reminder-form" class="form">
                <div class="form-group">
                    <label for="reminder-name">Reminder Name *</label>
                    <input type="text" id="reminder-name" name="name" required 
                           placeholder="e.g., Rent, Electricity Bill" maxlength="200">
                </div>

                <div class="form-group">
                    <label for="reminder-amount">Amount *</label>
                    <input type="number" id="reminder-amount" name="amount" required 
                           step="0.01" min="0.01" placeholder="0.00">
                </div>

                <div class="form-group">
                    <label for="reminder-due-date">Due Date *</label>
                    <input type="date" id="reminder-due-date" name="dueDate" required>
                </div>

                <div class="form-group">
                    <label for="reminder-frequency">Frequency *</label>
                    <select id="reminder-frequency" name="frequency" required>
                        <option value="">Select frequency</option>
                        <option value="MONTHLY">Monthly</option>
                        <option value="QUARTERLY">Quarterly</option>
                        <option value="YEARLY">Yearly</option>
                    </select>
                </div>

                <div class="form-group">
                    <label for="reminder-category">Category</label>
                    <select id="reminder-category" name="categoryId">
                        <option value="">Select category (optional)</option>
                    </select>
                </div>

                <div class="form-group">
                    <label for="reminder-days-before">Notify Days Before</label>
                    <input type="number" id="reminder-days-before" name="daysBefore" 
                           min="1" max="30" value="3" placeholder="3">
                    <small>Number of days before due date to send notification</small>
                </div>

                <div class="form-group">
                    <label for="reminder-notification-time">Notification Time</label>
                    <input type="time" id="reminder-notification-time" name="preferredNotificationTime" 
                           value="09:00">
                </div>

                <div class="form-group">
                    <label>
                        <input type="checkbox" id="reminder-email-notification" name="enableEmailNotification" checked>
                        Enable email notifications
                    </label>
                </div>

                <div class="form-group">
                    <label>
                        <input type="checkbox" id="reminder-push-notification" name="enablePushNotification" checked>
                        Enable push notifications
                    </label>
                </div>

                <div class="form-group">
                    <label for="reminder-custom-message">Custom Message</label>
                    <textarea id="reminder-custom-message" name="customMessage" 
                              placeholder="Optional custom message for notifications" 
                              maxlength="500" rows="3"></textarea>
                </div>
            </form>
        `;
    }

    async handleAddReminder() {
        const form = document.getElementById('add-reminder-form');
        const formData = new FormData(form);
        
        try {
            // Validate required fields
            const name = formData.get('name')?.trim();
            const amount = formData.get('amount');
            const dueDate = formData.get('dueDate');
            const frequency = formData.get('frequency');

            if (!name || !amount || !dueDate || !frequency) {
                this.ui.showToast('Please fill in all required fields', 'error');
                return false;
            }

            // Prepare reminder data
            const reminderData = {
                name: name,
                amount: parseFloat(amount),
                dueDate: dueDate,
                frequency: frequency,
                categoryId: formData.get('categoryId') || null,
                daysBefore: parseInt(formData.get('daysBefore')) || 3,
                preferredNotificationTime: formData.get('preferredNotificationTime') || '09:00',
                enableEmailNotification: formData.has('enableEmailNotification'),
                enablePushNotification: formData.has('enablePushNotification'),
                customMessage: formData.get('customMessage')?.trim() || null,
                active: true
            };

            // Create the reminder
            await this.expenseService.createPaymentReminder(reminderData);
            
            this.ui.showToast('Payment reminder created successfully!', 'success');
            await this.loadReminders(); // Refresh the list
            return true;

        } catch (error) {
            console.error('Failed to create reminder:', error);
            this.errorHandler.handleApiError(error, '/api/reminders', {
                onRetry: () => this.handleAddReminder(),
                retryable: true
            });
            return false;
        }
    }

    async populateReminderCategories() {
        try {
            const categories = await this.expenseService.getCategories();
            const categorySelect = document.getElementById('reminder-category');
            
            if (categorySelect && categories) {
                // Clear existing options except the first one
                categorySelect.innerHTML = '<option value="">Select category (optional)</option>';
                
                // Add categories
                categories.forEach(category => {
                    const option = document.createElement('option');
                    option.value = category.id;
                    option.textContent = category.name;
                    categorySelect.appendChild(option);
                });
            }
        } catch (error) {
            console.error('Failed to load categories for reminder form:', error);
            // Don't show error to user as categories are optional
        }
    }

    async editReminder(reminderId) {
        try {
            // Get the reminder details first
            const reminder = await this.expenseService.getReminderById(reminderId);
            
            this.ui.showModal('Edit Payment Reminder', this.createReminderFormHTML(reminder), {
                onConfirm: () => this.handleEditReminder(reminderId),
                confirmText: 'Update Reminder',
                cancelText: 'Cancel'
            });
            
            // Populate form with existing data and categories
            this.populateReminderForm(reminder);
            this.populateReminderCategories();
            
        } catch (error) {
            console.error('Failed to load reminder for editing:', error);
            this.ui.showToast('Failed to load reminder details', 'error');
        }
    }

    populateReminderForm(reminder) {
        // Populate form fields with existing reminder data
        document.getElementById('reminder-name').value = reminder.name || '';
        document.getElementById('reminder-amount').value = reminder.amount || '';
        document.getElementById('reminder-due-date').value = reminder.dueDate || '';
        document.getElementById('reminder-frequency').value = reminder.frequency || '';
        document.getElementById('reminder-category').value = reminder.categoryId || '';
        document.getElementById('reminder-days-before').value = reminder.daysBefore || 3;
        document.getElementById('reminder-notification-time').value = reminder.preferredNotificationTime || '09:00';
        document.getElementById('reminder-email-notification').checked = reminder.enableEmailNotification !== false;
        document.getElementById('reminder-push-notification').checked = reminder.enablePushNotification !== false;
        document.getElementById('reminder-custom-message').value = reminder.customMessage || '';
    }

    async handleEditReminder(reminderId) {
        const form = document.getElementById('add-reminder-form');
        const formData = new FormData(form);
        
        try {
            // Validate required fields
            const name = formData.get('name')?.trim();
            const amount = formData.get('amount');
            const dueDate = formData.get('dueDate');
            const frequency = formData.get('frequency');

            if (!name || !amount || !dueDate || !frequency) {
                this.ui.showToast('Please fill in all required fields', 'error');
                return false;
            }

            // Prepare reminder data
            const reminderData = {
                name: name,
                amount: parseFloat(amount),
                dueDate: dueDate,
                frequency: frequency,
                categoryId: formData.get('categoryId') || null,
                daysBefore: parseInt(formData.get('daysBefore')) || 3,
                preferredNotificationTime: formData.get('preferredNotificationTime') || '09:00',
                enableEmailNotification: formData.has('enableEmailNotification'),
                enablePushNotification: formData.has('enablePushNotification'),
                customMessage: formData.get('customMessage')?.trim() || null,
                active: true
            };

            // Update the reminder
            await this.expenseService.updatePaymentReminder(reminderId, reminderData);
            
            this.ui.showToast('Payment reminder updated successfully!', 'success');
            await this.loadReminders(); // Refresh the list
            return true;

        } catch (error) {
            console.error('Failed to update reminder:', error);
            this.errorHandler.handleApiError(error, `/api/reminders/${reminderId}`, {
                onRetry: () => this.handleEditReminder(reminderId),
                retryable: true
            });
            return false;
        }
    }

    editReminder(reminderId) {
        // TODO: Implement edit reminder functionality
        this.ui.showToast('Edit reminder coming soon!', 'info');
    }

    async updateSyncMetadata(data) {
        const metadata = {
            id: 'sync',
            lastSync: new Date().toISOString(),
            ...data
        };
        return this.saveToIndexedDB('syncMetadata', metadata);
    }

    // Theme Management
    async initializeTheme() {
        // Load saved theme or default to light
        const savedTheme = await this.getSetting('theme', 'light');
        this.applyTheme(savedTheme);
        
        // Set the theme selector value
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
}

// Initialize the app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.app = new ExpenseTrackerApp();
});