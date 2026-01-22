// Notification Service for handling notifications and reminders
class NotificationService {
    constructor() {
        this.baseUrl = '/api/notifications';
        this.settingsUrl = '/api/notification-settings';
        this.notifications = [];
        this.settings = null;
        this.unreadCount = 0;
    }

    // Notification CRUD operations
    async getAllNotifications() {
        try {
            const response = await fetch(this.baseUrl);
            if (response.ok) {
                this.notifications = await response.json();
                return this.notifications;
            }
            throw new Error('Failed to fetch notifications');
        } catch (error) {
            console.error('Error fetching notifications:', error);
            return [];
        }
    }

    async getUnreadNotifications() {
        try {
            const response = await fetch(`${this.baseUrl}/unread`);
            if (response.ok) {
                const unreadNotifications = await response.json();
                this.unreadCount = unreadNotifications.length;
                return unreadNotifications;
            }
            throw new Error('Failed to fetch unread notifications');
        } catch (error) {
            console.error('Error fetching unread notifications:', error);
            return [];
        }
    }

    async getRecentNotifications() {
        try {
            const response = await fetch(`${this.baseUrl}/recent`);
            if (response.ok) {
                return await response.json();
            }
            throw new Error('Failed to fetch recent notifications');
        } catch (error) {
            console.error('Error fetching recent notifications:', error);
            return [];
        }
    }

    async getUnreadCount() {
        try {
            const response = await fetch(`${this.baseUrl}/count/unread`);
            if (response.ok) {
                this.unreadCount = await response.json();
                return this.unreadCount;
            }
            throw new Error('Failed to fetch unread count');
        } catch (error) {
            console.error('Error fetching unread count:', error);
            return 0;
        }
    }

    async createNotification(notificationData) {
        try {
            const response = await fetch(this.baseUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(notificationData)
            });
            
            if (response.ok) {
                const notification = await response.json();
                this.notifications.unshift(notification);
                return notification;
            }
            throw new Error('Failed to create notification');
        } catch (error) {
            console.error('Error creating notification:', error);
            throw error;
        }
    }

    async markAsRead(notificationId) {
        try {
            const response = await fetch(`${this.baseUrl}/${notificationId}/read`, {
                method: 'PUT'
            });
            
            if (response.ok) {
                const notification = await response.json();
                // Update local notification
                const index = this.notifications.findIndex(n => n.id === notificationId);
                if (index !== -1) {
                    this.notifications[index] = notification;
                }
                this.unreadCount = Math.max(0, this.unreadCount - 1);
                return notification;
            }
            throw new Error('Failed to mark notification as read');
        } catch (error) {
            console.error('Error marking notification as read:', error);
            throw error;
        }
    }

    async markAllAsRead() {
        try {
            const response = await fetch(`${this.baseUrl}/read-all`, {
                method: 'PUT'
            });
            
            if (response.ok) {
                // Update all local notifications
                this.notifications.forEach(n => n.isRead = true);
                this.unreadCount = 0;
                return true;
            }
            throw new Error('Failed to mark all notifications as read');
        } catch (error) {
            console.error('Error marking all notifications as read:', error);
            throw error;
        }
    }

    async deleteNotification(notificationId) {
        try {
            const response = await fetch(`${this.baseUrl}/${notificationId}`, {
                method: 'DELETE'
            });
            
            if (response.ok) {
                // Remove from local notifications
                this.notifications = this.notifications.filter(n => n.id !== notificationId);
                return true;
            }
            throw new Error('Failed to delete notification');
        } catch (error) {
            console.error('Error deleting notification:', error);
            throw error;
        }
    }

    // Quick notification creation methods
    async createDailyReminder() {
        try {
            const response = await fetch(`${this.baseUrl}/daily-reminder`, {
                method: 'POST'
            });
            
            if (response.ok) {
                const notification = await response.json();
                this.notifications.unshift(notification);
                return notification;
            }
            throw new Error('Failed to create daily reminder');
        } catch (error) {
            console.error('Error creating daily reminder:', error);
            throw error;
        }
    }

    async createBudgetAlert(percentage, spent, budget) {
        try {
            const response = await fetch(`${this.baseUrl}/budget-alert?percentage=${percentage}&spent=${spent}&budget=${budget}`, {
                method: 'POST'
            });
            
            if (response.ok) {
                const notification = await response.json();
                this.notifications.unshift(notification);
                return notification;
            }
            throw new Error('Failed to create budget alert');
        } catch (error) {
            console.error('Error creating budget alert:', error);
            throw error;
        }
    }

    async createStreakReward(days) {
        try {
            const response = await fetch(`${this.baseUrl}/streak-reward?days=${days}`, {
                method: 'POST'
            });
            
            if (response.ok) {
                const notification = await response.json();
                this.notifications.unshift(notification);
                return notification;
            }
            throw new Error('Failed to create streak reward');
        } catch (error) {
            console.error('Error creating streak reward:', error);
            throw error;
        }
    }

    async createWeeklySummary(totalSpent, topCategory) {
        try {
            const response = await fetch(`${this.baseUrl}/weekly-summary?totalSpent=${totalSpent}&topCategory=${encodeURIComponent(topCategory)}`, {
                method: 'POST'
            });
            
            if (response.ok) {
                const notification = await response.json();
                this.notifications.unshift(notification);
                return notification;
            }
            throw new Error('Failed to create weekly summary');
        } catch (error) {
            console.error('Error creating weekly summary:', error);
            throw error;
        }
    }

    // Settings management
    async getSettings() {
        try {
            const response = await fetch(this.settingsUrl);
            if (response.ok) {
                this.settings = await response.json();
                return this.settings;
            }
            throw new Error('Failed to fetch notification settings');
        } catch (error) {
            console.error('Error fetching notification settings:', error);
            return null;
        }
    }

    async updateSettings(settingsData) {
        try {
            const response = await fetch(this.settingsUrl, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(settingsData)
            });
            
            if (response.ok) {
                this.settings = await response.json();
                return this.settings;
            }
            throw new Error('Failed to update notification settings');
        } catch (error) {
            console.error('Error updating notification settings:', error);
            throw error;
        }
    }

    async resetSettings() {
        try {
            const response = await fetch(`${this.settingsUrl}/reset`, {
                method: 'POST'
            });
            
            if (response.ok) {
                this.settings = null;
                return true;
            }
            throw new Error('Failed to reset notification settings');
        } catch (error) {
            console.error('Error resetting notification settings:', error);
            throw error;
        }
    }

    // Utility methods
    formatNotificationTime(dateTimeString) {
        const date = new Date(dateTimeString);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMins / 60);
        const diffDays = Math.floor(diffHours / 24);

        if (diffMins < 1) return 'Just now';
        if (diffMins < 60) return `${diffMins}m ago`;
        if (diffHours < 24) return `${diffHours}h ago`;
        if (diffDays < 7) return `${diffDays}d ago`;
        
        return date.toLocaleDateString();
    }

    getNotificationIcon(type) {
        const icons = {
            'DAILY_EXPENSE_REMINDER': '📝',
            'RECURRING_BILL_ALERT': '💰',
            'BUDGET_THRESHOLD_WARNING': '⚠️',
            'BUDGET_EXCEEDED_ALERT': '🚨',
            'WEEKLY_SUMMARY': '📊',
            'MONTHLY_REPORT': '📈',
            'STREAK_REWARD': '🎉',
            'BADGE_EARNED': '🏆',
            'OVERDUE_EXPENSE': '⏰',
            'CUSTOM_REMINDER': '🔔'
        };
        return icons[type] || '🔔';
    }

    getPriorityClass(priority) {
        switch (priority) {
            case 3: return 'priority-high';
            case 2: return 'priority-medium';
            case 1: 
            default: return 'priority-low';
        }
    }
}