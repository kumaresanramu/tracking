// Expense Service - Handles API communication
class ExpenseService {
    constructor() {
        this.baseUrl = '/api';
        this.defaultHeaders = {
            'Content-Type': 'application/json',
        };
    }

    // Expense CRUD operations
    async createExpense(expense) {
        try {
            const response = await fetch(`${this.baseUrl}/expenses`, {
                method: 'POST',
                headers: this.defaultHeaders,
                body: JSON.stringify(expense)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to create expense:', error);
            throw error;
        }
    }

    async getMonthlyExpenses(year, month) {
        try {
            const response = await fetch(`${this.baseUrl}/expenses/month/${year}/${month}`, {
                headers: this.defaultHeaders
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to get monthly expenses:', error);
            throw error;
        }
    }

    async updateExpense(id, expense) {
        try {
            const response = await fetch(`${this.baseUrl}/expenses/${id}`, {
                method: 'PUT',
                headers: this.defaultHeaders,
                body: JSON.stringify(expense)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to update expense:', error);
            throw error;
        }
    }

    async deleteExpense(id) {
        try {
            const response = await fetch(`${this.baseUrl}/expenses/${id}`, {
                method: 'DELETE',
                headers: this.defaultHeaders
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return true;
        } catch (error) {
            console.error('Failed to delete expense:', error);
            throw error;
        }
    }

    // Category operations
    async getCategories() {
        try {
            const response = await fetch(`${this.baseUrl}/categories`, {
                headers: this.defaultHeaders
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to get categories:', error);
            throw error;
        }
    }

    async createCategory(category) {
        try {
            const response = await fetch(`${this.baseUrl}/categories`, {
                method: 'POST',
                headers: this.defaultHeaders,
                body: JSON.stringify(category)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to create category:', error);
            throw error;
        }
    }

    // Analytics operations
    async getMonthlyTrends(months = 12) {
        console.log('ExpenseService: Getting monthly trends for', months, 'months');
        try {
            const response = await fetch(`${this.baseUrl}/analytics/monthly-trends?months=${months}`, {
                headers: this.defaultHeaders
            });

            console.log('ExpenseService: Monthly trends response status:', response.status);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            console.log('ExpenseService: Monthly trends data:', data);
            return data;
        } catch (error) {
            console.error('ExpenseService: Failed to get monthly trends:', error);
            throw error;
        }
    }

    async getCategoryBreakdown(year, month) {
        try {
            const response = await fetch(`${this.baseUrl}/analytics/category-breakdown/${year}/${month}`, {
                headers: this.defaultHeaders
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to get category breakdown:', error);
            throw error;
        }
    }

    async getExpenseSummary(year, month) {
        try {
            const response = await fetch(`${this.baseUrl}/analytics/summary/${year}/${month}`, {
                headers: this.defaultHeaders
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to get expense summary:', error);
            throw error;
        }
    }

    // Payment reminder operations (for future implementation)
    async getPaymentReminders() {
        try {
            const response = await fetch(`${this.baseUrl}/reminders`, {
                headers: this.defaultHeaders
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to get payment reminders:', error);
            throw error;
        }
    }

    async createPaymentReminder(reminder) {
        try {
            const response = await fetch(`${this.baseUrl}/reminders`, {
                method: 'POST',
                headers: this.defaultHeaders,
                body: JSON.stringify(reminder)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to create payment reminder:', error);
            throw error;
        }
    }

    async getReminderById(reminderId) {
        try {
            const response = await fetch(`${this.baseUrl}/reminders/${reminderId}`, {
                headers: this.defaultHeaders
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to get reminder by ID:', error);
            throw error;
        }
    }

    async updatePaymentReminder(reminderId, reminder) {
        try {
            const response = await fetch(`${this.baseUrl}/reminders/${reminderId}`, {
                method: 'PUT',
                headers: this.defaultHeaders,
                body: JSON.stringify(reminder)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to update payment reminder:', error);
            throw error;
        }
    }

    async markReminderAsPaid(reminderId, expenseData) {
        try {
            const response = await fetch(`${this.baseUrl}/reminders/${reminderId}/mark-paid`, {
                method: 'POST',
                headers: this.defaultHeaders,
                body: JSON.stringify(expenseData)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to mark reminder as paid:', error);
            throw error;
        }
    }

    // Utility methods
    async healthCheck() {
        try {
            const response = await fetch(`${this.baseUrl}/health`, {
                headers: this.defaultHeaders
            });

            return response.ok;
        } catch (error) {
            console.error('Health check failed:', error);
            return false;
        }
    }

    // Batch operations for offline sync
    async batchCreateExpenses(expenses) {
        try {
            const response = await fetch(`${this.baseUrl}/expenses/batch`, {
                method: 'POST',
                headers: this.defaultHeaders,
                body: JSON.stringify(expenses)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to batch create expenses:', error);
            throw error;
        }
    }

    async batchUpdateExpenses(expenses) {
        try {
            const response = await fetch(`${this.baseUrl}/expenses/batch`, {
                method: 'PUT',
                headers: this.defaultHeaders,
                body: JSON.stringify(expenses)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to batch update expenses:', error);
            throw error;
        }
    }

    // Error handling helper
    handleApiError(error, operation) {
        console.error(`API Error in ${operation}:`, error);
        
        if (error.name === 'TypeError' && error.message.includes('fetch')) {
            // Network error
            throw new Error('Network error. Please check your connection.');
        } else if (error.message.includes('HTTP error')) {
            // HTTP error
            const status = error.message.match(/status: (\d+)/)?.[1];
            switch (status) {
                case '400':
                    throw new Error('Invalid request. Please check your data.');
                case '401':
                    throw new Error('Authentication required.');
                case '403':
                    throw new Error('Access denied.');
                case '404':
                    throw new Error('Resource not found.');
                case '500':
                    throw new Error('Server error. Please try again later.');
                default:
                    throw new Error('An unexpected error occurred.');
            }
        } else {
            throw error;
        }
    }

    // Request retry with exponential backoff
    async retryRequest(requestFn, maxRetries = 3, baseDelay = 1000) {
        let lastError;
        
        for (let attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return await requestFn();
            } catch (error) {
                lastError = error;
                
                if (attempt < maxRetries - 1) {
                    const delay = baseDelay * Math.pow(2, attempt);
                    console.log(`Request failed, retrying in ${delay}ms...`);
                    await new Promise(resolve => setTimeout(resolve, delay));
                }
            }
        }
        
        throw lastError;
    }
}