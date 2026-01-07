const CACHE_NAME = 'expense-tracker-v1';
const urlsToCache = [
  '/',
  '/index.html',
  '/css/styles.css',
  '/css/components.css',
  '/js/app.js',
  '/js/expense-service.js',
  '/js/sync-service.js',
  '/js/analytics.js',
  '/js/ui-components.js',
  '/manifest.json',
  '/icons/icon-192x192.png',
  '/icons/icon-512x512.png',
  'https://cdn.jsdelivr.net/npm/chart.js'
];

// Install event - cache resources
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('Opened cache');
        return cache.addAll(urlsToCache);
      })
      .catch(error => {
        console.error('Failed to cache resources:', error);
      })
  );
});

// Fetch event - serve from cache when offline
self.addEventListener('fetch', event => {
  event.respondWith(
    caches.match(event.request)
      .then(response => {
        // Return cached version or fetch from network
        if (response) {
          return response;
        }
        
        return fetch(event.request).then(response => {
          // Check if we received a valid response
          if (!response || response.status !== 200 || response.type !== 'basic') {
            return response;
          }

          // Clone the response
          const responseToCache = response.clone();

          caches.open(CACHE_NAME)
            .then(cache => {
              cache.put(event.request, responseToCache);
            });

          return response;
        }).catch(() => {
          // If both cache and network fail, return offline page for navigation requests
          if (event.request.destination === 'document') {
            return caches.match('/index.html');
          }
        });
      })
  );
});

// Activate event - clean up old caches
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cacheName => {
          if (cacheName !== CACHE_NAME) {
            console.log('Deleting old cache:', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
});

// Background sync for offline data
self.addEventListener('sync', event => {
  if (event.tag === 'expense-sync') {
    event.waitUntil(syncOfflineExpenses());
  }
});

// Sync offline expenses when connection is restored
async function syncOfflineExpenses() {
  try {
    // Get offline expenses from IndexedDB
    const offlineExpenses = await getOfflineExpenses();
    
    for (const expense of offlineExpenses) {
      try {
        // Attempt to sync each expense
        const response = await fetch('/api/expenses', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(expense)
        });
        
        if (response.ok) {
          // Remove from offline storage after successful sync
          await removeOfflineExpense(expense.id);
        }
      } catch (error) {
        console.error('Failed to sync expense:', error);
      }
    }
  } catch (error) {
    console.error('Background sync failed:', error);
  }
}

// Helper functions for IndexedDB operations
async function getOfflineExpenses() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('ExpenseTrackerDB', 1);
    
    request.onsuccess = event => {
      const db = event.target.result;
      const transaction = db.transaction(['offlineExpenses'], 'readonly');
      const store = transaction.objectStore('offlineExpenses');
      const getAllRequest = store.getAll();
      
      getAllRequest.onsuccess = () => {
        resolve(getAllRequest.result);
      };
      
      getAllRequest.onerror = () => {
        reject(getAllRequest.error);
      };
    };
    
    request.onerror = () => {
      reject(request.error);
    };
  });
}

async function removeOfflineExpense(expenseId) {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('ExpenseTrackerDB', 1);
    
    request.onsuccess = event => {
      const db = event.target.result;
      const transaction = db.transaction(['offlineExpenses'], 'readwrite');
      const store = transaction.objectStore('offlineExpenses');
      const deleteRequest = store.delete(expenseId);
      
      deleteRequest.onsuccess = () => {
        resolve();
      };
      
      deleteRequest.onerror = () => {
        reject(deleteRequest.error);
      };
    };
    
    request.onerror = () => {
      reject(request.error);
    };
  });
}

// Push notification handling (for future payment reminders)
self.addEventListener('push', event => {
  if (event.data) {
    const data = event.data.json();
    const options = {
      body: data.body,
      icon: '/icons/icon-192x192.png',
      badge: '/icons/icon-72x72.png',
      vibrate: [100, 50, 100],
      data: {
        dateOfArrival: Date.now(),
        primaryKey: data.primaryKey
      },
      actions: [
        {
          action: 'mark-paid',
          title: 'Mark as Paid',
          icon: '/icons/check.png'
        },
        {
          action: 'snooze',
          title: 'Remind Later',
          icon: '/icons/snooze.png'
        }
      ]
    };
    
    event.waitUntil(
      self.registration.showNotification(data.title, options)
    );
  }
});

// Handle notification clicks
self.addEventListener('notificationclick', event => {
  event.notification.close();
  
  if (event.action === 'mark-paid') {
    // Handle mark as paid action
    event.waitUntil(
      clients.openWindow('/expenses?action=add&reminder=' + event.notification.data.primaryKey)
    );
  } else if (event.action === 'snooze') {
    // Handle snooze action
    console.log('Reminder snoozed');
  } else {
    // Default action - open the app
    event.waitUntil(
      clients.openWindow('/')
    );
  }
});