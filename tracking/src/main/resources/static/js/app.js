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
            this.notificationService = new NotificationService();
            this.pwaInstallManager = new PWAInstallManager();
            this.appUpdateManager = new AppUpdateManager();
            
            // Connect error handler to UI
            this.errorHandler.setUI(this.ui);
            
            // Set up event listeners
            this.setupEventListeners();
            
            // Handle PWA features (shortcuts, share targets)
            this.handlePWAFeatures();
            
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

    handlePWAFeatures() {
        // Handle URL parameters for shortcuts and share targets
        const urlParams = new URLSearchParams(window.location.search);
        
        // Handle shortcuts
        if (urlParams.get('shortcut') === 'true') {
            const path = window.location.pathname;
            console.log('PWA: Launched from shortcut:', path);
            
            // Handle specific shortcuts
            if (path.includes('/expenses/add')) {
                this.navigateToPage('expenses');
                // Focus on the first input field
                setTimeout(() => {
                    const firstInput = document.querySelector('#expense-description');
                    if (firstInput) firstInput.focus();
                }, 100);
            } else if (path.includes('/analytics')) {
                this.navigateToPage('analytics');
                if (urlParams.get('view') === 'summary') {
                    // Show summary view
                    this.showMonthlySummary();
                }
            } else if (path.includes('/dashboard')) {
                this.navigateToPage('dashboard');
                if (urlParams.get('view') === 'summary') {
                    // Scroll to summary section
                    setTimeout(() => {
                        const summarySection = document.querySelector('.quick-insights');
                        if (summarySection) {
                            summarySection.scrollIntoView({ behavior: 'smooth' });
                        }
                    }, 100);
                }
            }
        }
        
        // Handle share target
        if (urlParams.get('title') || urlParams.get('text')) {
            this.handleShareTarget(urlParams);
        }
        
        // Handle protocol handlers (web+expense://)
        if (urlParams.get('data')) {
            try {
                const expenseData = JSON.parse(decodeURIComponent(urlParams.get('data')));
                this.handleProtocolExpense(expenseData);
            } catch (error) {
                console.error('PWA: Failed to parse protocol expense data:', error);
            }
        }
        
        // Handle file handlers (CSV/JSON import)
        if (window.location.pathname === '/import') {
            this.handleFileImport();
        }
        
        // Set up Web Share API if available
        if (navigator.share) {
            this.setupWebShare();
        }
        
        // Handle app shortcuts from context menu
        this.setupAppShortcuts();
    }

    handleShareTarget(urlParams) {
        console.log('PWA: Handling share target');
        
        const title = urlParams.get('title') || '';
        const text = urlParams.get('text') || '';
        const url = urlParams.get('url') || '';
        
        // Navigate to expenses page and pre-fill form
        this.navigateToPage('expenses');
        
        setTimeout(() => {
            // Try to extract expense information from shared content
            let description = title || text;
            let amount = '';
            
            // Try to extract amount from text
            const amountMatch = text.match(/\$?(\d+(?:\.\d{2})?)/);
            if (amountMatch) {
                amount = amountMatch[1];
                description = text.replace(amountMatch[0], '').trim();
            }
            
            // Pre-fill form
            const descriptionInput = document.getElementById('expense-description');
            const amountInput = document.getElementById('expense-amount');
            
            if (descriptionInput && description) {
                descriptionInput.value = description;
            }
            
            if (amountInput && amount) {
                amountInput.value = amount;
            }
            
            // Show a notification about the shared content
            this.ui.showToast('Shared content loaded into expense form', 'success');
        }, 200);
    }

    handleProtocolExpense(expenseData) {
        console.log('PWA: Handling protocol expense:', expenseData);
        
        this.navigateToPage('expenses');
        
        setTimeout(() => {
            // Pre-fill form with protocol data
            if (expenseData.description) {
                const descInput = document.getElementById('expense-description');
                if (descInput) descInput.value = expenseData.description;
            }
            
            if (expenseData.amount) {
                const amountInput = document.getElementById('expense-amount');
                if (amountInput) amountInput.value = expenseData.amount;
            }
            
            if (expenseData.category) {
                const categorySelect = document.getElementById('expense-category');
                if (categorySelect) {
                    // Try to find matching category
                    const option = Array.from(categorySelect.options)
                        .find(opt => opt.text.toLowerCase().includes(expenseData.category.toLowerCase()));
                    if (option) categorySelect.value = option.value;
                }
            }
            
            this.ui.showToast('Expense data loaded from external app', 'success');
        }, 200);
    }

    handleFileImport() {
        console.log('PWA: Handling file import');
        // This would be implemented when file handling is needed
        // For now, redirect to main app
        this.navigateToPage('dashboard');
        this.ui.showToast('File import feature coming soon', 'info');
    }

    setupWebShare() {
        // Add share buttons to analytics and reports
        const shareButtons = document.querySelectorAll('.share-btn');
        shareButtons.forEach(button => {
            button.addEventListener('click', async (e) => {
                const shareData = {
                    title: 'My Expense Report',
                    text: 'Check out my expense tracking progress',
                    url: window.location.href
                };
                
                try {
                    await navigator.share(shareData);
                    console.log('PWA: Content shared successfully');
                } catch (error) {
                    console.log('PWA: Share cancelled or failed:', error);
                }
            });
        });
    }

    setupAppShortcuts() {
        // Handle keyboard shortcuts for PWA
        document.addEventListener('keydown', (e) => {
            // Ctrl/Cmd + N: New expense
            if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
                e.preventDefault();
                this.navigateToPage('expenses');
                setTimeout(() => {
                    const firstInput = document.querySelector('#expense-description');
                    if (firstInput) firstInput.focus();
                }, 100);
            }
            
            // Ctrl/Cmd + A: Analytics
            if ((e.ctrlKey || e.metaKey) && e.key === 'a' && !e.shiftKey) {
                e.preventDefault();
                this.navigateToPage('analytics');
            }
            
            // Ctrl/Cmd + D: Dashboard
            if ((e.ctrlKey || e.metaKey) && e.key === 'd') {
                e.preventDefault();
                this.navigateToPage('dashboard');
            }
        });
    }

    showMonthlySummary() {
        // Show monthly summary in analytics
        console.log('PWA: Showing monthly summary');
        // This would trigger the monthly summary view
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
                await this.loadNotifications();
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
        
        // Handle multiple tag selection properly
        const tagsSelect = document.getElementById('expense-tags');
        const selectedTags = Array.from(tagsSelect.selectedOptions).map(option => option.value);
        
        const expense = {
            amount: parseFloat(formData.get('amount')),
            description: formData.get('description'),
            categoryId: parseInt(formData.get('categoryId')),
            date: formData.get('date') || new Date().toISOString().split('T')[0],
            paymentMethod: formData.get('paymentMethod') || '',
            tags: selectedTags.join(', ') // Join multiple tags with comma and space
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
                        ${this.formatTags(expense.tags)}
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
        
        // Handle multiple tags properly for editing
        const tagsField = document.getElementById('expense-tags');
        if (tagsField && expense.tags) {
            // Clear all selections first
            Array.from(tagsField.options).forEach(option => option.selected = false);
            
            // Split tags by comma and trim whitespace, then select matching options
            const expenseTags = expense.tags.split(',').map(tag => tag.trim());
            Array.from(tagsField.options).forEach(option => {
                if (expenseTags.includes(option.value)) {
                    option.selected = true;
                }
            });
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

    formatTags(tags) {
        if (!tags) return '';
        
        const tagIcons = {
            'work': '💼',
            'personal': '👤',
            'family': '👨‍👩‍👧‍👦',
            'health': '🏥',
            'education': '📚',
            'entertainment': '🎬'
        };
        
        const tagList = tags.split(',').map(tag => tag.trim());
        const tagBadges = tagList.map(tag => {
            const icon = tagIcons[tag] || '🏷️';
            const displayName = tag.charAt(0).toUpperCase() + tag.slice(1);
            return `<span class="expense-tag">${icon} ${displayName}</span>`;
        }).join('');
        
        return `<div class="expense-tags">${tagBadges}</div>`;
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

    // Enhanced reminder methods
    showAddReminderForm() {
        const modalContent = `
            <div class="reminder-form-container">
                <div class="form-header">
                    <h3>💰 Create Payment Reminder</h3>
                    <p>Set up automated reminders for recurring bills and payments</p>
                </div>
                
                <div class="reminder-form">
                    <div class="form-section">
                        <h4>📝 Basic Information</h4>
                        <div class="form-group">
                            <label for="reminder-title">Reminder Title *</label>
                            <input type="text" id="reminder-title" name="title" 
                                   placeholder="e.g., Monthly Rent Payment" required
                                   class="form-input">
                            <small>Give your reminder a clear, descriptive name</small>
                        </div>
                        
                        <div class="form-group">
                            <label for="reminder-description">Description</label>
                            <textarea id="reminder-description" name="description" 
                                      placeholder="Additional details about this reminder" 
                                      rows="3" class="form-input"></textarea>
                            <small>Optional: Add any additional notes or details</small>
                        </div>
                    </div>
                    
                    <div class="form-section">
                        <h4>💵 Payment Details</h4>
                        <div class="form-row">
                            <div class="form-group">
                                <label for="reminder-amount">Amount</label>
                                <div class="input-with-icon">
                                    <span class="input-icon">$</span>
                                    <input type="number" id="reminder-amount" name="amount" 
                                           step="0.01" min="0" placeholder="0.00" class="form-input">
                                </div>
                                <small>Leave blank if amount varies</small>
                            </div>
                            
                            <div class="form-group">
                                <label for="reminder-category">Category</label>
                                <select id="reminder-category" name="categoryId" class="form-input">
                                    <option value="">Select category</option>
                                    ${this.categories.map(cat => `<option value="${cat.id}">${cat.name}</option>`).join('')}
                                </select>
                                <small>Choose the expense category</small>
                            </div>
                        </div>
                    </div>
                    
                    <div class="form-section">
                        <h4>📅 Scheduling</h4>
                        <div class="form-row">
                            <div class="form-group">
                                <label for="reminder-due-date">Due Date *</label>
                                <input type="date" id="reminder-due-date" name="dueDate" 
                                       required class="form-input" min="${new Date().toISOString().split('T')[0]}">
                                <small>When is this payment due?</small>
                            </div>
                            
                            <div class="form-group">
                                <label for="reminder-frequency">Frequency *</label>
                                <select id="reminder-frequency" name="frequency" required class="form-input">
                                    <option value="ONCE">One-time payment</option>
                                    <option value="WEEKLY">Weekly</option>
                                    <option value="MONTHLY" selected>Monthly</option>
                                    <option value="YEARLY">Yearly</option>
                                </select>
                                <small>How often does this payment occur?</small>
                            </div>
                        </div>
                        
                        <div class="form-group">
                            <label for="reminder-advance-days">Reminder Timing</label>
                            <select id="reminder-advance-days" name="advanceDays" class="form-input">
                                <option value="0">On the due date</option>
                                <option value="1" selected>1 day before</option>
                                <option value="3">3 days before</option>
                                <option value="7">1 week before</option>
                                <option value="14">2 weeks before</option>
                            </select>
                            <small>When should we remind you?</small>
                        </div>
                    </div>
                    
                    <div class="form-section">
                        <h4>⚙️ Options</h4>
                        <div class="form-group">
                            <label class="checkbox-label">
                                <input type="checkbox" id="reminder-auto-create" name="autoCreateExpense">
                                <span class="checkmark"></span>
                                <span class="checkbox-text">
                                    <strong>Auto-create expense</strong>
                                    <small>Automatically add this as an expense when due</small>
                                </span>
                            </label>
                        </div>
                        
                        <div class="form-group">
                            <label class="checkbox-label">
                                <input type="checkbox" id="reminder-high-priority" name="highPriority">
                                <span class="checkmark"></span>
                                <span class="checkbox-text">
                                    <strong>High priority</strong>
                                    <small>Mark this reminder as high priority</small>
                                </span>
                            </label>
                        </div>
                    </div>
                </div>
                
                <div class="form-preview">
                    <h4>📋 Preview</h4>
                    <div id="reminder-preview" class="preview-content">
                        <p>Fill in the form to see a preview of your reminder</p>
                    </div>
                </div>
            </div>
        `;

        this.ui.showModal('Create Payment Reminder', modalContent, {
            onConfirm: () => this.handleAddReminder(),
            confirmText: '✅ Create Reminder',
            cancelText: '❌ Cancel',
            size: 'large'
        });

        // Set up form preview
        this.setupReminderFormPreview();
        
        // Set default date to tomorrow
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        document.getElementById('reminder-due-date').value = tomorrow.toISOString().split('T')[0];
    }

    setupReminderFormPreview() {
        const formInputs = ['reminder-title', 'reminder-amount', 'reminder-due-date', 'reminder-frequency', 'reminder-advance-days'];
        
        formInputs.forEach(inputId => {
            const input = document.getElementById(inputId);
            if (input) {
                input.addEventListener('input', () => this.updateReminderPreview());
                input.addEventListener('change', () => this.updateReminderPreview());
            }
        });
        
        // Initial preview update
        setTimeout(() => this.updateReminderPreview(), 100);
    }

    updateReminderPreview() {
        const title = document.getElementById('reminder-title')?.value || 'Your Reminder';
        const amount = document.getElementById('reminder-amount')?.value;
        const dueDate = document.getElementById('reminder-due-date')?.value;
        const frequency = document.getElementById('reminder-frequency')?.value;
        const advanceDays = document.getElementById('reminder-advance-days')?.value;
        
        const previewElement = document.getElementById('reminder-preview');
        if (!previewElement) return;

        let preview = `<div class="preview-reminder">`;
        preview += `<div class="preview-title">💰 ${title}</div>`;
        
        if (amount && parseFloat(amount) > 0) {
            preview += `<div class="preview-amount">Amount: $${parseFloat(amount).toFixed(2)}</div>`;
        }
        
        if (dueDate) {
            const dueDateObj = new Date(dueDate);
            preview += `<div class="preview-date">Due: ${dueDateObj.toLocaleDateString()}</div>`;
        }
        
        if (frequency) {
            const frequencyText = {
                'ONCE': 'One-time',
                'WEEKLY': 'Weekly',
                'MONTHLY': 'Monthly',
                'YEARLY': 'Yearly'
            };
            preview += `<div class="preview-frequency">Frequency: ${frequencyText[frequency]}</div>`;
        }
        
        if (advanceDays) {
            const advanceText = {
                '0': 'on the due date',
                '1': '1 day before',
                '3': '3 days before',
                '7': '1 week before',
                '14': '2 weeks before'
            };
            preview += `<div class="preview-timing">Reminder: ${advanceText[advanceDays]}</div>`;
        }
        
        preview += `</div>`;
        previewElement.innerHTML = preview;
    }

    async handleAddReminder() {
        const formData = {
            title: document.getElementById('reminder-title').value.trim(),
            description: document.getElementById('reminder-description').value.trim(),
            amount: parseFloat(document.getElementById('reminder-amount').value) || 0,
            categoryId: parseInt(document.getElementById('reminder-category').value) || null,
            dueDate: document.getElementById('reminder-due-date').value,
            frequency: document.getElementById('reminder-frequency').value,
            advanceDays: parseInt(document.getElementById('reminder-advance-days').value),
            autoCreateExpense: document.getElementById('reminder-auto-create')?.checked || false,
            highPriority: document.getElementById('reminder-high-priority')?.checked || false
        };

        // Enhanced validation
        const errors = [];
        
        if (!formData.title) {
            errors.push('Reminder title is required');
        } else if (formData.title.length < 3) {
            errors.push('Reminder title must be at least 3 characters long');
        }
        
        if (!formData.dueDate) {
            errors.push('Due date is required');
        } else {
            const dueDate = new Date(formData.dueDate);
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            
            if (dueDate < today) {
                errors.push('Due date cannot be in the past');
            }
        }
        
        if (!formData.frequency) {
            errors.push('Frequency is required');
        }

        if (errors.length > 0) {
            this.ui.showToast(errors.join('. '), 'error');
            return false;
        }

        try {
            // Create a notification for this reminder
            const priority = formData.highPriority ? 3 : 2;
            const notificationData = {
                title: `Payment Reminder: ${formData.title}`,
                message: `Don't forget: ${formData.title} is due on ${new Date(formData.dueDate).toLocaleDateString()}${formData.amount > 0 ? ` (Amount: $${formData.amount.toFixed(2)})` : ''}`,
                type: 'RECURRING_BILL_ALERT',
                channel: 'IN_APP',
                frequency: formData.frequency,
                scheduledFor: new Date(formData.dueDate).toISOString(),
                actionUrl: '/expenses',
                actionLabel: 'Log Payment',
                icon: '💰',
                priority: priority
            };

            await this.notificationService.createNotification(notificationData);
            await this.loadNotifications();
            
            // Show success message with details
            const successMessage = `Payment reminder "${formData.title}" created successfully! ` +
                                  `You'll be reminded ${formData.advanceDays > 0 ? formData.advanceDays + ' days before' : 'on'} ${new Date(formData.dueDate).toLocaleDateString()}.`;
            
            this.ui.showToast(successMessage, 'success', 5000);
            return true;
        } catch (error) {
            console.error('Failed to create reminder:', error);
            this.ui.showToast('Failed to create reminder. Please check your connection and try again.', 'error');
            return false;
        }
    }

    // Notification methods
    async loadNotifications() {
        try {
            const notifications = await this.notificationService.getRecentNotifications();
            const unreadCount = await this.notificationService.getUnreadCount();
            
            this.displayNotifications(notifications);
            this.updateUnreadCount(unreadCount);
        } catch (error) {
            console.error('Failed to load notifications:', error);
            this.ui.showToast('Failed to load notifications', 'error');
        }
    }

    displayNotifications(notifications) {
        const notificationsList = document.getElementById('notifications-list');
        if (!notificationsList) return;

        if (notifications.length === 0) {
            notificationsList.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">🔔</div>
                    <div class="empty-state-title">No notifications</div>
                    <div class="empty-state-description">You're all caught up!</div>
                </div>
            `;
            return;
        }

        // Group notifications by type
        const groupedNotifications = this.groupNotificationsByType(notifications);
        
        // Separate read and unread notifications
        const unreadNotifications = notifications.filter(n => !n.isRead);
        const readNotifications = notifications.filter(n => n.isRead);
        
        // Create tabs and grouped display
        notificationsList.innerHTML = `
            <div class="notification-tabs">
                <button class="tab-button active" data-tab="all">All (${notifications.length})</button>
                <button class="tab-button" data-tab="unread">Unread (${unreadNotifications.length})</button>
                <button class="tab-button" data-tab="read">Read (${readNotifications.length})</button>
                ${Object.keys(groupedNotifications).map(type => `
                    <button class="tab-button" data-tab="${type}">
                        ${this.getTypeDisplayName(type)} (${groupedNotifications[type].length})
                    </button>
                `).join('')}
            </div>
            
            <div class="notification-controls">
                <div class="search-container">
                    <input type="text" id="notification-search" placeholder="Search notifications..." class="search-input">
                    <span class="search-icon">🔍</span>
                </div>
                <div class="bulk-actions">
                    <button class="btn btn-small" onclick="app.markAllNotificationsRead()">
                        ✓ Mark All Read
                    </button>
                    <button class="btn btn-small btn-secondary" onclick="app.clearAllNotifications()">
                        🗑️ Clear All
                    </button>
                </div>
            </div>
            
            <div class="notification-content-area">
                <div class="tab-content active" data-content="all">
                    ${this.renderNotificationGroup('All Notifications', notifications)}
                </div>
                <div class="tab-content" data-content="unread">
                    ${this.renderNotificationGroup('Unread Notifications', unreadNotifications)}
                </div>
                <div class="tab-content" data-content="read">
                    ${this.renderNotificationGroup('Read Notifications', readNotifications)}
                </div>
                </div>
                ${Object.entries(groupedNotifications).map(([type, typeNotifications]) => `
                    <div class="tab-content" data-content="${type}">
                        ${this.renderNotificationGroup(this.getTypeDisplayName(type), typeNotifications)}
                    </div>
                `).join('')}
            </div>
        `;

        // Set up tab switching
        this.setupNotificationTabs();
        
        // Set up search functionality
        this.setupNotificationSearch(notifications);
    }

    groupNotificationsByType(notifications) {
        const groups = {
            'achievements': [],
            'warnings': [],
            'summaries': [],
            'reminders': []
        };

        notifications.forEach(notification => {
            const type = notification.type;
            if (type === 'STREAK_REWARD' || type === 'BADGE_EARNED') {
                groups.achievements.push(notification);
            } else if (type === 'BUDGET_THRESHOLD_WARNING' || type === 'BUDGET_EXCEEDED_ALERT' || type === 'OVERDUE_EXPENSE') {
                groups.warnings.push(notification);
            } else if (type === 'WEEKLY_SUMMARY' || type === 'MONTHLY_REPORT') {
                groups.summaries.push(notification);
            } else if (type === 'DAILY_EXPENSE_REMINDER' || type === 'RECURRING_BILL_ALERT' || type === 'CUSTOM_REMINDER') {
                groups.reminders.push(notification);
            } else {
                // Default to reminders for unknown types
                groups.reminders.push(notification);
            }
        });

        // Remove empty groups
        Object.keys(groups).forEach(key => {
            if (groups[key].length === 0) {
                delete groups[key];
            }
        });

        return groups;
    }

    getTypeDisplayName(type) {
        const displayNames = {
            'achievements': '🏆 Achievements',
            'warnings': '⚠️ Warnings',
            'summaries': '📊 Summaries',
            'reminders': '🔔 Reminders'
        };
        return displayNames[type] || type;
    }

    renderNotificationGroup(groupTitle, notifications) {
        if (notifications.length === 0) {
            return `<div class="empty-group">No ${groupTitle.toLowerCase()} found</div>`;
        }

        return `
            <div class="notification-group">
                <div class="group-header">
                    <h4>${groupTitle}</h4>
                    <span class="group-count">${notifications.length} items</span>
                </div>
                <div class="group-notifications">
                    ${notifications.map(notification => this.renderNotificationCard(notification)).join('')}
                </div>
            </div>
        `;
    }

    renderNotificationCard(notification) {
        const timeAgo = this.notificationService.formatNotificationTime(notification.createdAt || notification.scheduledFor);
        const typeClass = this.getNotificationTypeClass(notification.type);
        
        return `
            <div class="notification-card ${notification.isRead ? 'read' : 'unread'} ${typeClass} ${this.notificationService.getPriorityClass(notification.priority)}" 
                 data-id="${notification.id}" data-type="${notification.type}">
                <div class="card-header">
                    <div class="notification-icon">
                        ${notification.icon || this.notificationService.getNotificationIcon(notification.type)}
                    </div>
                    <div class="notification-badge ${typeClass}">
                        ${this.getTypeBadgeText(notification.type)}
                    </div>
                    <div class="notification-time">${timeAgo}</div>
                </div>
                
                <div class="card-body">
                    <div class="notification-title">${notification.title}</div>
                    <div class="notification-message">${notification.message}</div>
                    
                    ${notification.actionUrl ? `
                        <div class="notification-action-area">
                            <button class="action-button primary" onclick="app.handleNotificationAction('${notification.actionUrl}', ${notification.id})">
                                ${notification.actionLabel || 'View'}
                            </button>
                        </div>
                    ` : ''}
                </div>
                
                <div class="card-footer">
                    <div class="notification-controls">
                        ${!notification.isRead ? `
                            <button class="control-btn" onclick="app.markNotificationRead(${notification.id})" title="Mark as read">
                                ✓
                            </button>
                        ` : ''}
                        <button class="control-btn delete" onclick="app.deleteNotification(${notification.id})" title="Delete">
                            🗑️
                        </button>
                    </div>
                </div>
            </div>
        `;
    }

    getNotificationTypeClass(type) {
        const typeClasses = {
            'STREAK_REWARD': 'achievement',
            'BADGE_EARNED': 'achievement',
            'BUDGET_THRESHOLD_WARNING': 'warning',
            'BUDGET_EXCEEDED_ALERT': 'warning',
            'OVERDUE_EXPENSE': 'warning',
            'WEEKLY_SUMMARY': 'summary',
            'MONTHLY_REPORT': 'summary',
            'DAILY_EXPENSE_REMINDER': 'reminder',
            'RECURRING_BILL_ALERT': 'reminder',
            'CUSTOM_REMINDER': 'reminder'
        };
        return typeClasses[type] || 'reminder';
    }

    getTypeBadgeText(type) {
        const badgeTexts = {
            'STREAK_REWARD': 'Achievement',
            'BADGE_EARNED': 'Badge',
            'BUDGET_THRESHOLD_WARNING': 'Budget Alert',
            'BUDGET_EXCEEDED_ALERT': 'Budget Alert',
            'OVERDUE_EXPENSE': 'Overdue',
            'WEEKLY_SUMMARY': 'Summary',
            'MONTHLY_REPORT': 'Report',
            'DAILY_EXPENSE_REMINDER': 'Daily',
            'RECURRING_BILL_ALERT': 'Bill',
            'CUSTOM_REMINDER': 'Reminder'
        };
        return badgeTexts[type] || 'Notification';
    }

    setupNotificationTabs() {
        const tabButtons = document.querySelectorAll('.tab-button');
        const tabContents = document.querySelectorAll('.tab-content');

        tabButtons.forEach(button => {
            button.addEventListener('click', () => {
                const tabName = button.dataset.tab;
                
                // Update active tab button
                tabButtons.forEach(btn => btn.classList.remove('active'));
                button.classList.add('active');
                
                // Update active tab content
                tabContents.forEach(content => {
                    content.classList.remove('active');
                    if (content.dataset.content === tabName) {
                        content.classList.add('active');
                    }
                });
            });
        });
    }

    setupNotificationSearch(notifications) {
        const searchInput = document.getElementById('notification-search');
        if (!searchInput) return;

        searchInput.addEventListener('input', (e) => {
            const searchTerm = e.target.value.toLowerCase();
            this.filterNotifications(notifications, searchTerm);
        });
    }

    filterNotifications(notifications, searchTerm) {
        const notificationCards = document.querySelectorAll('.notification-card');
        
        notificationCards.forEach(card => {
            const title = card.querySelector('.notification-title').textContent.toLowerCase();
            const message = card.querySelector('.notification-message').textContent.toLowerCase();
            const type = card.dataset.type.toLowerCase();
            
            const matches = title.includes(searchTerm) || 
                          message.includes(searchTerm) || 
                          type.includes(searchTerm);
            
            card.style.display = matches ? 'block' : 'none';
        });
        
        // Update group counts
        this.updateGroupCounts();
    }

    updateGroupCounts() {
        const groups = document.querySelectorAll('.notification-group');
        groups.forEach(group => {
            const visibleCards = group.querySelectorAll('.notification-card:not([style*="display: none"])');
            const countElement = group.querySelector('.group-count');
            if (countElement) {
                countElement.textContent = `${visibleCards.length} items`;
            }
        });
    }

    async clearAllNotifications() {
        if (!confirm('Are you sure you want to clear all notifications? This action cannot be undone.')) {
            return;
        }

        try {
            // Get all notification IDs
            const notifications = await this.notificationService.getAllNotifications();
            
            // Delete all notifications
            const deletePromises = notifications.map(notification => 
                this.notificationService.deleteNotification(notification.id)
            );
            
            await Promise.all(deletePromises);
            await this.loadNotifications(); // Refresh the list
            this.ui.showToast('All notifications cleared', 'success');
        } catch (error) {
            console.error('Failed to clear all notifications:', error);
            this.ui.showToast('Failed to clear all notifications', 'error');
        }
    }

    updateUnreadCount(count) {
        const unreadBadge = document.getElementById('unread-count');
        if (unreadBadge) {
            unreadBadge.textContent = count;
            unreadBadge.style.display = count > 0 ? 'inline-block' : 'none';
        }
    }

    async markNotificationRead(notificationId) {
        try {
            await this.notificationService.markAsRead(notificationId);
            await this.loadNotifications(); // Refresh the list
            this.ui.showToast('Notification marked as read', 'success');
        } catch (error) {
            console.error('Failed to mark notification as read:', error);
            this.ui.showToast('Failed to mark notification as read', 'error');
        }
    }

    async markAllNotificationsRead() {
        try {
            await this.notificationService.markAllAsRead();
            await this.loadNotifications(); // Refresh the list
            this.ui.showToast('All notifications marked as read', 'success');
        } catch (error) {
            console.error('Failed to mark all notifications as read:', error);
            this.ui.showToast('Failed to mark all notifications as read', 'error');
        }
    }

    async deleteNotification(notificationId) {
        if (!confirm('Are you sure you want to delete this notification?')) {
            return;
        }

        try {
            await this.notificationService.deleteNotification(notificationId);
            await this.loadNotifications(); // Refresh the list
            this.ui.showToast('Notification deleted', 'success');
        } catch (error) {
            console.error('Failed to delete notification:', error);
            this.ui.showToast('Failed to delete notification', 'error');
        }
    }

    async refreshNotifications() {
        await this.loadNotifications();
        this.ui.showToast('Notifications refreshed', 'info');
    }

    handleNotificationAction(actionUrl, notificationId) {
        // Mark as read when action is taken
        this.markNotificationRead(notificationId);
        
        // Navigate to the action URL
        if (actionUrl.startsWith('/')) {
            // Internal navigation
            const page = actionUrl.substring(1);
            this.navigateToPage(page);
        } else {
            // External URL
            window.open(actionUrl, '_blank');
        }
    }

    // Quick notification creation methods
    async createDailyReminder() {
        // Show explanation modal first
        const modalContent = `
            <div class="daily-reminder-info">
                <div class="info-header">
                    <h3>📝 Daily Expense Reminder</h3>
                    <p>This creates a one-time reminder notification to help you remember to log today's expenses.</p>
                </div>
                
                <div class="info-content">
                    <div class="feature-list">
                        <div class="feature-item">
                            <span class="feature-icon">🔔</span>
                            <div class="feature-text">
                                <strong>Instant Reminder</strong>
                                <small>Creates a notification in your notification center</small>
                            </div>
                        </div>
                        <div class="feature-item">
                            <span class="feature-icon">📱</span>
                            <div class="feature-text">
                                <strong>Quick Access</strong>
                                <small>Includes a direct link to add expenses</small>
                            </div>
                        </div>
                        <div class="feature-item">
                            <span class="feature-icon">⏰</span>
                            <div class="feature-text">
                                <strong>One-Time Only</strong>
                                <small>This is not a recurring reminder - use Settings for automatic daily reminders</small>
                            </div>
                        </div>
                    </div>
                    
                    <div class="tip-box">
                        <strong>💡 Tip:</strong> For automatic daily reminders, go to Settings → Notification Settings and enable "Daily expense reminders" with your preferred time.
                    </div>
                </div>
            </div>
        `;

        this.ui.showModal('Daily Expense Reminder', modalContent, {
            onConfirm: async () => {
                try {
                    await this.notificationService.createDailyReminder();
                    await this.loadNotifications();
                    this.ui.showToast('Daily reminder created! Check your notifications.', 'success');
                } catch (error) {
                    console.error('Failed to create daily reminder:', error);
                    this.ui.showToast('Failed to create daily reminder', 'error');
                }
            },
            confirmText: '✅ Create Reminder',
            cancelText: '❌ Cancel'
        });
    }

    async createWeeklySummary() {
        try {
            // Calculate weekly data (last 7 days)
            const now = new Date();
            const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
            
            // Filter expenses from the last 7 days
            const weeklyExpenses = this.expenses.filter(expense => {
                const expenseDate = new Date(expense.date);
                return expenseDate >= weekAgo && expenseDate <= now;
            });
            
            const totalSpent = weeklyExpenses.reduce((sum, expense) => sum + expense.amount, 0);
            
            // Calculate top category from weekly expenses
            const categoryTotals = {};
            weeklyExpenses.forEach(expense => {
                const categoryName = expense.category?.name || 
                                   this.categories.find(c => c.id === expense.categoryId)?.name || 
                                   'Unknown';
                categoryTotals[categoryName] = (categoryTotals[categoryName] || 0) + expense.amount;
            });
            
            const topCategory = Object.entries(categoryTotals)
                .sort(([,a], [,b]) => b - a)[0]?.[0] || 'No expenses';
            
            await this.notificationService.createWeeklySummary(totalSpent, topCategory);
            await this.loadNotifications();
            this.ui.showToast('Weekly summary created!', 'success');
        } catch (error) {
            console.error('Failed to create weekly summary:', error);
            this.ui.showToast('Failed to create weekly summary', 'error');
        }
    }

    // Enhanced smart notification methods
    async createSmartWeeklySummary() {
        try {
            const response = await fetch('/api/notifications/smart-weekly-summary', {
                method: 'POST'
            });
            
            if (response.ok) {
                await this.loadNotifications();
                this.ui.showToast('Weekly summary created with real data!', 'success');
            } else {
                throw new Error('Failed to create weekly summary');
            }
        } catch (error) {
            console.error('Failed to create smart weekly summary:', error);
            this.ui.showToast('Failed to create weekly summary', 'error');
        }
    }

    async createSmartBudgetAlert() {
        try {
            // Get monthly budget from settings
            const monthlyBudget = this.monthlyBudget || 0;
            
            if (monthlyBudget <= 0) {
                this.ui.showToast('Please set a monthly budget first in Dashboard settings', 'warning');
                return;
            }
            
            const response = await fetch(`/api/notifications/smart-budget-alert?monthlyBudget=${monthlyBudget}`, {
                method: 'POST'
            });
            
            if (response.ok) {
                await this.loadNotifications();
                this.ui.showToast('Budget alert created with current spending data!', 'success');
            } else {
                throw new Error('Failed to create budget alert');
            }
        } catch (error) {
            console.error('Failed to create smart budget alert:', error);
            this.ui.showToast('Failed to create budget alert', 'error');
        }
    }

    async createSmartStreakReward() {
        try {
            const response = await fetch('/api/notifications/smart-streak-reward', {
                method: 'POST'
            });
            
            if (response.ok) {
                await this.loadNotifications();
                this.ui.showToast('Streak reward created with your actual logging days!', 'success');
            } else {
                throw new Error('Failed to create streak reward');
            }
        } catch (error) {
            console.error('Failed to create smart streak reward:', error);
            this.ui.showToast('Failed to create streak reward', 'error');
        }
    }

    async createDailyReminder() {
        try {
            const response = await fetch('/api/notifications/daily-reminder', {
                method: 'POST'
            });
            
            if (response.ok) {
                await this.loadNotifications();
                this.ui.showToast('Daily expense reminder created!', 'success');
            } else {
                throw new Error('Failed to create daily reminder');
            }
        } catch (error) {
            console.error('Failed to create daily reminder:', error);
            this.ui.showToast('Failed to create daily reminder', 'error');
        }
    }



    async showNotificationSettings() {
        try {
            // Show loading state
            this.ui.showToast('Loading notification settings...', 'info');
            
            const settings = await this.notificationService.getSettings();
            this.displayNotificationSettingsModal(settings);
        } catch (error) {
            console.error('Failed to load notification settings:', error);
            
            // Show error modal with option to use defaults
            const errorModalContent = `
                <div class="error-content">
                    <div class="error-icon">⚠️</div>
                    <h3>Failed to Load Settings</h3>
                    <p>Unable to load your notification settings from the server.</p>
                    <p>You can still configure settings using default values.</p>
                </div>
            `;
            
            this.ui.showModal('Settings Error', errorModalContent, {
                onConfirm: () => {
                    // Show settings modal with default values
                    this.displayNotificationSettingsModal(null);
                },
                confirmText: 'Use Defaults',
                cancelText: 'Cancel'
            });
        }
    }

    displayNotificationSettingsModal(settings) {
        const modalContent = `
            <div class="notification-settings-form">
                <div class="form-section">
                    <h4>📝 Daily Reminders</h4>
                    <div class="form-group">
                        <label>
                            <input type="checkbox" id="enableDailyReminder" ${settings?.enableDailyReminder ? 'checked' : ''}>
                            Enable daily expense reminders
                        </label>
                    </div>
                    <div class="form-group">
                        <label for="dailyReminderTime">Reminder time:</label>
                        <input type="time" id="dailyReminderTime" value="${settings?.dailyReminderTime || '20:00'}">
                    </div>
                </div>

                <div class="form-section">
                    <h4>💰 Budget Alerts</h4>
                    <div class="form-group">
                        <label>
                            <input type="checkbox" id="enableBudgetAlerts" ${settings?.enableBudgetAlerts ? 'checked' : ''}>
                            Enable budget threshold alerts
                        </label>
                    </div>
                    <div class="form-group">
                        <label for="budgetWarningThreshold">Warning threshold (%):</label>
                        <input type="number" id="budgetWarningThreshold" min="50" max="100" value="${settings?.budgetWarningThreshold || 80}">
                    </div>
                </div>

                <div class="form-section">
                    <h4>📊 Weekly Summary</h4>
                    <div class="form-group">
                        <label>
                            <input type="checkbox" id="enableWeeklySummary" ${settings?.enableWeeklySummary ? 'checked' : ''}>
                            Enable weekly summary emails
                        </label>
                    </div>
                    <div class="form-group">
                        <label for="weeklySummaryTime">Summary time:</label>
                        <input type="time" id="weeklySummaryTime" value="${settings?.weeklySummaryTime || '09:00'}">
                    </div>
                </div>

                <div class="form-section">
                    <h4>🎮 Gamification</h4>
                    <div class="form-group">
                        <label>
                            <input type="checkbox" id="enableStreakRewards" ${settings?.enableStreakRewards ? 'checked' : ''}>
                            Enable streak rewards
                        </label>
                    </div>
                    <div class="form-group">
                        <label>
                            <input type="checkbox" id="enableBadges" ${settings?.enableBadges ? 'checked' : ''}>
                            Enable achievement badges
                        </label>
                    </div>
                </div>

                <div class="form-section">
                    <h4>🌙 Quiet Hours</h4>
                    <div class="form-row">
                        <div class="form-group">
                            <label for="quietHoursStart">Start:</label>
                            <input type="time" id="quietHoursStart" value="${settings?.quietHoursStart || '22:00'}">
                        </div>
                        <div class="form-group">
                            <label for="quietHoursEnd">End:</label>
                            <input type="time" id="quietHoursEnd" value="${settings?.quietHoursEnd || '08:00'}">
                        </div>
                    </div>
                </div>

                <div class="form-section">
                    <h4>📧 Email Notifications</h4>
                    <div class="form-group">
                        <label>
                            <input type="checkbox" id="enableEmailNotifications" ${settings?.enableEmailNotifications ? 'checked' : ''}>
                            Enable email notifications
                        </label>
                    </div>
                    <div class="form-group">
                        <label for="emailAddress">Email address:</label>
                        <input type="email" id="emailAddress" value="${settings?.emailAddress || ''}" placeholder="your@email.com">
                    </div>
                </div>
            </div>
        `;

        this.ui.showModal('Notification Settings', modalContent, {
            onConfirm: () => this.saveNotificationSettings(),
            confirmText: 'Save Settings',
            cancelText: 'Cancel'
        });
    }

    async saveNotificationSettings() {
        try {
            // Validate email if email notifications are enabled
            const enableEmail = document.getElementById('enableEmailNotifications').checked;
            const emailAddress = document.getElementById('emailAddress').value.trim();
            
            if (enableEmail && !emailAddress) {
                this.ui.showToast('Please enter an email address to enable email notifications', 'warning');
                return false; // Don't close modal
            }
            
            if (enableEmail && !this.isValidEmail(emailAddress)) {
                this.ui.showToast('Please enter a valid email address', 'warning');
                return false; // Don't close modal
            }
            
            // Validate time inputs
            const dailyTime = document.getElementById('dailyReminderTime').value;
            const weeklyTime = document.getElementById('weeklySummaryTime').value;
            const quietStart = document.getElementById('quietHoursStart').value;
            const quietEnd = document.getElementById('quietHoursEnd').value;
            
            if (!dailyTime || !weeklyTime || !quietStart || !quietEnd) {
                this.ui.showToast('Please fill in all time fields', 'warning');
                return false;
            }
            
            // Show saving state
            this.ui.showToast('Saving notification settings...', 'info');
            
            const settingsData = {
                enableDailyReminder: document.getElementById('enableDailyReminder').checked,
                dailyReminderTime: dailyTime,
                enableBudgetAlerts: document.getElementById('enableBudgetAlerts').checked,
                budgetWarningThreshold: parseInt(document.getElementById('budgetWarningThreshold').value),
                enableWeeklySummary: document.getElementById('enableWeeklySummary').checked,
                weeklySummaryTime: weeklyTime,
                enableStreakRewards: document.getElementById('enableStreakRewards').checked,
                enableBadges: document.getElementById('enableBadges').checked,
                quietHoursStart: quietStart,
                quietHoursEnd: quietEnd,
                enableEmailNotifications: enableEmail,
                emailAddress: emailAddress
            };

            const savedSettings = await this.notificationService.updateSettings(settingsData);
            
            if (savedSettings) {
                this.ui.showToast('Notification settings saved successfully!', 'success');
                
                // Show confirmation of what was enabled
                const enabledFeatures = [];
                if (settingsData.enableDailyReminder) enabledFeatures.push('Daily reminders');
                if (settingsData.enableBudgetAlerts) enabledFeatures.push('Budget alerts');
                if (settingsData.enableWeeklySummary) enabledFeatures.push('Weekly summaries');
                if (settingsData.enableEmailNotifications) enabledFeatures.push('Email notifications');
                
                if (enabledFeatures.length > 0) {
                    setTimeout(() => {
                        this.ui.showToast(`Enabled: ${enabledFeatures.join(', ')}`, 'info', 4000);
                    }, 1000);
                }
                
                return true; // Close modal
            } else {
                throw new Error('Settings were not saved properly');
            }
        } catch (error) {
            console.error('Failed to save notification settings:', error);
            this.ui.showToast('Failed to save notification settings. Please try again.', 'error');
            return false; // Don't close modal
        }
    }
    
    isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
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