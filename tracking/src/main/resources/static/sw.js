const CACHE_NAME = 'expense-tracker-v2';
const STATIC_CACHE = 'expense-tracker-static-v2';
const API_CACHE = 'expense-tracker-api-v2';
const RUNTIME_CACHE = 'expense-tracker-runtime-v2';

// Cache size limits for LRU eviction
const CACHE_LIMITS = {
  [STATIC_CACHE]: 50,
  [API_CACHE]: 100,
  [RUNTIME_CACHE]: 30
};

const urlsToCache = [
  '/',
  '/index.html',
  '/css/styles.css',
  '/css/components.css',
  '/js/app.js',
  '/js/expense-service.js',
  '/js/analytics.js',
  '/js/ui-components.js',
  '/js/error-handler.js',
  '/js/notification-service.js',
  '/manifest.json',
  '/icons/icon.svg',
  '/icons/icon-192x192.svg',
  'https://cdn.jsdelivr.net/npm/chart.js'
];

// LRU Cache Management
class LRUCacheManager {
  constructor() {
    this.accessTimes = new Map();
  }

  async recordAccess(cacheName, url) {
    const key = `${cacheName}:${url}`;
    this.accessTimes.set(key, Date.now());
  }

  async evictLRU(cacheName) {
    const cache = await caches.open(cacheName);
    const requests = await cache.keys();
    
    if (requests.length <= CACHE_LIMITS[cacheName]) {
      return;
    }

    // Sort by access time (oldest first)
    const sortedRequests = requests
      .map(request => ({
        request,
        accessTime: this.accessTimes.get(`${cacheName}:${request.url}`) || 0
      }))
      .sort((a, b) => a.accessTime - b.accessTime);

    // Remove oldest entries
    const toRemove = sortedRequests.slice(0, requests.length - CACHE_LIMITS[cacheName]);
    
    for (const { request } of toRemove) {
      await cache.delete(request);
      this.accessTimes.delete(`${cacheName}:${request.url}`);
    }
  }
}

const lruManager = new LRUCacheManager();

// Background Sync Queue Management
class BackgroundSyncManager {
  constructor() {
    this.syncQueue = [];
    this.retryAttempts = new Map();
    this.maxRetries = 5;
    this.baseDelay = 1000; // 1 second
  }

  async addToQueue(data, syncType = 'expense') {
    const syncItem = {
      id: this.generateId(),
      data,
      type: syncType,
      timestamp: Date.now(),
      retryCount: 0
    };
    
    this.syncQueue.push(syncItem);
    await this.persistQueue();
    return syncItem.id;
  }

  async processQueue() {
    const queue = await this.getQueue();
    const results = [];
    
    for (const item of queue) {
      try {
        const result = await this.syncItem(item);
        results.push({ id: item.id, success: true, result });
        await this.removeFromQueue(item.id);
      } catch (error) {
        console.error(`Sync failed for item ${item.id}:`, error);
        const shouldRetry = await this.handleSyncFailure(item, error);
        results.push({ id: item.id, success: false, error: error.message, willRetry: shouldRetry });
      }
    }
    
    return results;
  }

  async syncItem(item) {
    switch (item.type) {
      case 'expense':
        return await this.syncExpense(item.data);
      case 'analytics':
        return await this.syncAnalytics(item.data);
      default:
        throw new Error(`Unknown sync type: ${item.type}`);
    }
  }

  async syncExpense(expenseData) {
    const response = await fetch('/api/expenses', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(expenseData)
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    return await response.json();
  }

  async syncAnalytics(analyticsData) {
    const response = await fetch('/api/analytics/sync', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(analyticsData)
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    return await response.json();
  }

  async handleSyncFailure(item, error) {
    item.retryCount = (item.retryCount || 0) + 1;
    
    if (item.retryCount >= this.maxRetries) {
      console.error(`Max retries exceeded for item ${item.id}, removing from queue`);
      await this.removeFromQueue(item.id);
      return false;
    }

    // Exponential backoff: 1s, 2s, 4s, 8s, 16s
    const delay = this.baseDelay * Math.pow(2, item.retryCount - 1);
    item.nextRetryAt = Date.now() + delay;
    
    await this.persistQueue();
    
    // Schedule retry
    setTimeout(() => {
      self.registration.sync.register(`retry-${item.id}`);
    }, delay);
    
    return true;
  }

  async getQueue() {
    try {
      const stored = await this.getFromIndexedDB('syncQueue');
      this.syncQueue = stored || [];
      return this.syncQueue.filter(item => 
        !item.nextRetryAt || item.nextRetryAt <= Date.now()
      );
    } catch (error) {
      console.error('Failed to get sync queue:', error);
      return [];
    }
  }

  async persistQueue() {
    try {
      await this.saveToIndexedDB('syncQueue', this.syncQueue);
    } catch (error) {
      console.error('Failed to persist sync queue:', error);
    }
  }

  async removeFromQueue(itemId) {
    this.syncQueue = this.syncQueue.filter(item => item.id !== itemId);
    await this.persistQueue();
  }

  generateId() {
    return Date.now().toString(36) + Math.random().toString(36).substr(2);
  }

  // IndexedDB helpers for queue persistence
  async getFromIndexedDB(key) {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open('ExpenseTrackerSW', 1);
      
      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        const db = request.result;
        const transaction = db.transaction(['syncData'], 'readonly');
        const store = transaction.objectStore('syncData');
        const getRequest = store.get(key);
        
        getRequest.onsuccess = () => resolve(getRequest.result?.data);
        getRequest.onerror = () => reject(getRequest.error);
      };
      
      request.onupgradeneeded = () => {
        const db = request.result;
        if (!db.objectStoreNames.contains('syncData')) {
          db.createObjectStore('syncData', { keyPath: 'key' });
        }
      };
    });
  }

  async saveToIndexedDB(key, data) {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open('ExpenseTrackerSW', 1);
      
      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        const db = request.result;
        const transaction = db.transaction(['syncData'], 'readwrite');
        const store = transaction.objectStore('syncData');
        const putRequest = store.put({ key, data });
        
        putRequest.onsuccess = () => resolve();
        putRequest.onerror = () => reject(putRequest.error);
      };
      
      request.onupgradeneeded = () => {
        const db = request.result;
        if (!db.objectStoreNames.contains('syncData')) {
          db.createObjectStore('syncData', { keyPath: 'key' });
        }
      };
    });
  }
}

const syncManager = new BackgroundSyncManager();

// Install event - cache static resources
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(STATIC_CACHE)
      .then(cache => {
        console.log('Opened static cache');
        return cache.addAll(urlsToCache);
      })
      .catch(error => {
        console.error('Failed to cache static resources:', error);
      })
  );
  // Skip waiting to activate immediately
  self.skipWaiting();
});

// Advanced caching strategies
async function cacheFirstStrategy(request, cacheName) {
  const cache = await caches.open(cacheName);
  const cachedResponse = await cache.match(request);
  
  if (cachedResponse) {
    // Record access for LRU
    await lruManager.recordAccess(cacheName, request.url);
    return cachedResponse;
  }
  
  try {
    const networkResponse = await fetch(request);
    if (networkResponse && networkResponse.status === 200) {
      const responseToCache = networkResponse.clone();
      await cache.put(request, responseToCache);
      await lruManager.recordAccess(cacheName, request.url);
      await lruManager.evictLRU(cacheName);
    }
    return networkResponse;
  } catch (error) {
    console.error('Cache-first strategy failed:', error);
    throw error;
  }
}

async function networkFirstStrategy(request, cacheName) {
  try {
    const networkResponse = await fetch(request);
    if (networkResponse && networkResponse.status === 200) {
      const cache = await caches.open(cacheName);
      const responseToCache = networkResponse.clone();
      await cache.put(request, responseToCache);
      await lruManager.recordAccess(cacheName, request.url);
      await lruManager.evictLRU(cacheName);
    }
    return networkResponse;
  } catch (error) {
    console.log('Network failed, trying cache:', error.message);
    const cache = await caches.open(cacheName);
    const cachedResponse = await cache.match(request);
    
    if (cachedResponse) {
      await lruManager.recordAccess(cacheName, request.url);
      return cachedResponse;
    }
    
    throw error;
  }
}

// Fetch event - advanced caching strategies
self.addEventListener('fetch', event => {
  // Only handle GET requests for caching
  if (event.request.method !== 'GET') {
    return;
  }

  const url = new URL(event.request.url);
  
  event.respondWith(
    (async () => {
      try {
        // API requests - network-first strategy
        if (url.pathname.startsWith('/api/')) {
          return await networkFirstStrategy(event.request, API_CACHE);
        }
        
        // Static resources - cache-first strategy
        if (urlsToCache.some(cachedUrl => {
          const cachedUrlObj = new URL(cachedUrl, self.location.origin);
          return cachedUrlObj.pathname === url.pathname || cachedUrl === event.request.url;
        })) {
          return await cacheFirstStrategy(event.request, STATIC_CACHE);
        }
        
        // Runtime resources (images, fonts, etc.) - cache-first strategy
        if (event.request.destination === 'image' || 
            event.request.destination === 'font' ||
            event.request.destination === 'style' ||
            event.request.destination === 'script') {
          return await cacheFirstStrategy(event.request, RUNTIME_CACHE);
        }
        
        // Navigation requests - network-first with offline fallback
        if (event.request.destination === 'document') {
          try {
            const networkResponse = await fetch(event.request);
            return networkResponse;
          } catch (error) {
            const cache = await caches.open(STATIC_CACHE);
            const offlinePage = await cache.match('/index.html');
            return offlinePage || new Response('Offline', { status: 503 });
          }
        }
        
        // Default: try network first, then cache
        return await networkFirstStrategy(event.request, RUNTIME_CACHE);
        
      } catch (error) {
        console.error('Fetch strategy failed:', error);
        return new Response('Network error', { status: 503 });
      }
    })()
  );
});

// Activate event - clean up old caches and claim clients
self.addEventListener('activate', event => {
  event.waitUntil(
    (async () => {
      // Clean up old caches
      const cacheNames = await caches.keys();
      const validCaches = [STATIC_CACHE, API_CACHE, RUNTIME_CACHE];
      
      await Promise.all(
        cacheNames.map(cacheName => {
          if (!validCaches.includes(cacheName)) {
            console.log('Deleting old cache:', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
      
      // Claim all clients immediately
      await self.clients.claim();
      console.log('Service worker activated and claimed clients');
      
      // Notify clients about update if this is an update
      const clients = await self.clients.matchAll();
      if (clients.length > 0) {
        clients.forEach(client => {
          client.postMessage({
            type: 'UPDATE_AVAILABLE',
            message: 'New version available'
          });
        });
      }
    })()
  );
});

// Enhanced push notification handling with rich notifications
self.addEventListener('push', event => {
  if (!event.data) {
    console.log('Push event received but no data');
    return;
  }

  try {
    const data = event.data.json();
    console.log('Push notification received:', data);
    
    const options = {
      body: data.body || 'You have a new notification',
      icon: data.icon || '/icons/icon-192x192.svg',
      badge: '/icons/icon.svg',
      image: data.image,
      vibrate: data.vibrate || [100, 50, 100],
      sound: data.sound,
      timestamp: Date.now(),
      requireInteraction: data.requireInteraction || false,
      silent: data.silent || false,
      tag: data.tag || `notification-${Date.now()}`,
      renotify: data.renotify || false,
      data: {
        ...data.data,
        dateOfArrival: Date.now(),
        primaryKey: data.primaryKey || Date.now(),
        type: data.type || 'general',
        url: data.url
      }
    };

    // Add action buttons based on notification type
    switch (data.type) {
      case 'payment-reminder':
        options.actions = [
          {
            action: 'mark-paid',
            title: '✓ Mark as Paid',
            icon: '/icons/icon.svg'
          },
          {
            action: 'snooze',
            title: '⏰ Snooze',
            icon: '/icons/icon.svg'
          },
          {
            action: 'view-details',
            title: '👁 View Details',
            icon: '/icons/icon.svg'
          }
        ];
        options.requireInteraction = true;
        break;
        
      case 'budget-alert':
        options.actions = [
          {
            action: 'view-budget',
            title: '📊 View Budget',
            icon: '/icons/icon.svg'
          },
          {
            action: 'add-expense',
            title: '➕ Add Expense',
            icon: '/icons/icon.svg'
          }
        ];
        options.requireInteraction = true;
        break;
        
      case 'daily-reminder':
        options.actions = [
          {
            action: 'add-expense',
            title: '➕ Add Expense',
            icon: '/icons/icon.svg'
          },
          {
            action: 'view-summary',
            title: '📈 View Summary',
            icon: '/icons/icon.svg'
          }
        ];
        break;
        
      case 'weekly-summary':
        options.actions = [
          {
            action: 'view-analytics',
            title: '📊 View Analytics',
            icon: '/icons/icon.svg'
          },
          {
            action: 'export-data',
            title: '💾 Export Data',
            icon: '/icons/icon.svg'
          }
        ];
        break;
        
      default:
        options.actions = [
          {
            action: 'open-app',
            title: '📱 Open App',
            icon: '/icons/icon.svg'
          }
        ];
    }

    event.waitUntil(
      self.registration.showNotification(data.title || 'Expense Tracker', options)
    );
    
  } catch (error) {
    console.error('Error processing push notification:', error);
    
    // Fallback notification
    event.waitUntil(
      self.registration.showNotification('Expense Tracker', {
        body: 'You have a new notification',
        icon: '/icons/icon-192x192.svg',
        badge: '/icons/icon.svg',
        tag: 'fallback-notification'
      })
    );
  }
});

// Enhanced notification click handling with comprehensive action processing
self.addEventListener('notificationclick', event => {
  console.log('Notification clicked:', event.notification.tag, 'Action:', event.action);
  
  event.notification.close();
  
  const notificationData = event.notification.data || {};
  const notificationType = notificationData.type || 'general';
  
  event.waitUntil(
    (async () => {
      try {
        // Handle specific actions
        switch (event.action) {
          case 'mark-paid':
            await handleMarkAsPaid(notificationData);
            break;
            
          case 'snooze':
            await handleSnoozeReminder(notificationData);
            break;
            
          case 'view-details':
            await openAppWithPath(`/expenses/${notificationData.primaryKey}`);
            break;
            
          case 'view-budget':
            await openAppWithPath('/analytics?view=budget');
            break;
            
          case 'add-expense':
            await openAppWithPath('/expenses/add', notificationData);
            break;
            
          case 'view-summary':
            await openAppWithPath('/analytics?view=summary');
            break;
            
          case 'view-analytics':
            await openAppWithPath('/analytics');
            break;
            
          case 'export-data':
            await handleExportData(notificationData);
            break;
            
          case 'open-app':
          default:
            // Default action or no action - open app
            const targetUrl = notificationData.url || '/';
            await openAppWithPath(targetUrl);
            break;
        }
        
        // Log notification interaction for analytics
        await logNotificationInteraction(event.action, notificationType, notificationData);
        
      } catch (error) {
        console.error('Error handling notification click:', error);
        // Fallback - just open the app
        await openAppWithPath('/');
      }
    })()
  );
});

// Helper functions for notification actions
async function handleMarkAsPaid(notificationData) {
  const clients = await self.clients.matchAll({ type: 'window' });
  
  if (clients.length > 0) {
    // If app is open, send message to mark as paid
    clients[0].postMessage({
      type: 'MARK_AS_PAID',
      reminderId: notificationData.primaryKey,
      data: notificationData
    });
    clients[0].focus();
  } else {
    // Open app with pre-filled expense form
    const url = `/expenses/add?reminder=${notificationData.primaryKey}&amount=${notificationData.amount || ''}&description=${encodeURIComponent(notificationData.description || '')}`;
    await self.clients.openWindow(url);
  }
}

async function handleSnoozeReminder(notificationData) {
  const clients = await self.clients.matchAll();
  
  if (clients.length > 0) {
    clients[0].postMessage({
      type: 'SNOOZE_REMINDER',
      reminderId: notificationData.primaryKey,
      snoozeMinutes: 60 // Default 1 hour snooze
    });
  }
  
  // Show confirmation notification
  await self.registration.showNotification('Reminder Snoozed', {
    body: 'You will be reminded again in 1 hour',
    icon: '/icons/icon.svg',
    tag: 'snooze-confirmation',
    silent: true
  });
}

async function handleExportData(notificationData) {
  const clients = await self.clients.matchAll({ type: 'window' });
  
  if (clients.length > 0) {
    clients[0].postMessage({
      type: 'EXPORT_DATA',
      exportType: notificationData.exportType || 'weekly'
    });
    clients[0].focus();
  } else {
    await openAppWithPath('/analytics?action=export');
  }
}

async function openAppWithPath(path, data = null) {
  const clients = await self.clients.matchAll({ type: 'window' });
  
  // If app is already open, navigate to the path
  if (clients.length > 0) {
    const client = clients[0];
    
    if (data) {
      client.postMessage({
        type: 'NAVIGATE_WITH_DATA',
        path: path,
        data: data
      });
    } else {
      client.postMessage({
        type: 'NAVIGATE',
        path: path
      });
    }
    
    return client.focus();
  } else {
    // Open new window
    const url = new URL(path, self.location.origin);
    if (data) {
      // Add data as URL parameters for simple data
      Object.keys(data).forEach(key => {
        if (typeof data[key] === 'string' || typeof data[key] === 'number') {
          url.searchParams.set(key, data[key]);
        }
      });
    }
    return self.clients.openWindow(url.toString());
  }
}

async function logNotificationInteraction(action, type, data) {
  try {
    // Send interaction data to analytics endpoint
    await fetch('/api/analytics/notification-interaction', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        action: action || 'default',
        notificationType: type,
        timestamp: Date.now(),
        notificationId: data.primaryKey
      })
    });
  } catch (error) {
    console.error('Failed to log notification interaction:', error);
    // Don't throw - this is non-critical
  }
}

// Handle messages from main app for local operations and sync
self.addEventListener('message', event => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    console.log('Service Worker: Received SKIP_WAITING message');
    self.skipWaiting();
  } else if (event.data && event.data.type === 'CACHE_API_RESPONSE') {
    // Cache API responses for offline access
    caches.open(API_CACHE).then(cache => {
      cache.put(event.data.url, new Response(JSON.stringify(event.data.data), {
        headers: { 'Content-Type': 'application/json' }
      }));
    });
  } else if (event.data && event.data.type === 'QUEUE_EXPENSE_SYNC') {
    // Queue expense for background sync
    syncManager.addToQueue(event.data.expense, 'expense').then(syncId => {
      // Register background sync
      self.registration.sync.register('expense-sync');
      
      // Notify main app of queued sync
      event.ports[0]?.postMessage({ success: true, syncId });
    }).catch(error => {
      console.error('Failed to queue expense sync:', error);
      event.ports[0]?.postMessage({ success: false, error: error.message });
    });
  } else if (event.data && event.data.type === 'QUEUE_ANALYTICS_SYNC') {
    // Queue analytics for background sync
    syncManager.addToQueue(event.data.analytics, 'analytics').then(syncId => {
      self.registration.sync.register('analytics-sync');
      event.ports[0]?.postMessage({ success: true, syncId });
    }).catch(error => {
      console.error('Failed to queue analytics sync:', error);
      event.ports[0]?.postMessage({ success: false, error: error.message });
    });
  }
});

// Background sync event handler
self.addEventListener('sync', event => {
  console.log('Background sync triggered:', event.tag);
  
  if (event.tag === 'expense-sync' || event.tag === 'analytics-sync' || event.tag.startsWith('retry-')) {
    event.waitUntil(
      syncManager.processQueue().then(results => {
        console.log('Background sync completed:', results);
        
        // Notify all clients about sync results
        return self.clients.matchAll().then(clients => {
          clients.forEach(client => {
            client.postMessage({
              type: 'SYNC_COMPLETED',
              tag: event.tag,
              results: results
            });
          });
        });
      }).catch(error => {
        console.error('Background sync failed:', error);
        
        // Notify clients about sync failure
        return self.clients.matchAll().then(clients => {
          clients.forEach(client => {
            client.postMessage({
              type: 'SYNC_FAILED',
              tag: event.tag,
              error: error.message
            });
          });
        });
        
        throw error; // Re-throw to trigger retry
      })
    );
  }
});