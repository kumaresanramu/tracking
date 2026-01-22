const CACHE_NAME = 'expense-tracker-local-v1';
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
  '/manifest.json',
  '/icons/icon.svg',
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

// Fetch event - cache-first strategy for local performance
self.addEventListener('fetch', event => {
  // Only handle GET requests for caching
  if (event.request.method !== 'GET') {
    return;
  }

  event.respondWith(
    caches.match(event.request)
      .then(response => {
        // Return cached version if available
        if (response) {
          return response;
        }
        
        // For API requests, use network-first with local fallback
        if (event.request.url.includes('/api/')) {
          return fetch(event.request).then(response => {
            // Only cache successful responses
            if (response && response.status === 200) {
              const responseToCache = response.clone();
              caches.open(CACHE_NAME)
                .then(cache => {
                  cache.put(event.request, responseToCache);
                });
            }
            return response;
          }).catch(() => {
            // Return cached API response if available
            return caches.match(event.request);
          });
        }
        
        // For static resources, fetch and cache
        return fetch(event.request).then(response => {
          // Check if we received a valid response
          if (!response || response.status !== 200 || response.type !== 'basic') {
            return response;
          }

          // Clone and cache the response
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

// Push notification handling for payment reminders (local notifications only)
self.addEventListener('push', event => {
  if (event.data) {
    const data = event.data.json();
    const options = {
      body: data.body,
      icon: '/icons/icon.svg',
      badge: '/icons/icon.svg',
      vibrate: [100, 50, 100],
      data: {
        dateOfArrival: Date.now(),
        primaryKey: data.primaryKey,
        type: 'payment-reminder'
      },
      actions: [
        {
          action: 'mark-paid',
          title: 'Mark as Paid'
        },
        {
          action: 'snooze',
          title: 'Remind Later'
        }
      ],
      requireInteraction: true,
      tag: 'payment-reminder-' + data.primaryKey
    };
    
    event.waitUntil(
      self.registration.showNotification(data.title, options)
    );
  }
});

// Handle notification clicks for local actions only
self.addEventListener('notificationclick', event => {
  event.notification.close();
  
  if (event.action === 'mark-paid') {
    // Open expense entry form with reminder data pre-filled
    event.waitUntil(
      clients.openWindow('/?action=add&reminder=' + event.notification.data.primaryKey)
    );
  } else if (event.action === 'snooze') {
    // Schedule local reminder for later (handled by main app)
    event.waitUntil(
      clients.matchAll().then(clients => {
        if (clients.length > 0) {
          clients[0].postMessage({
            type: 'snooze-reminder',
            reminderId: event.notification.data.primaryKey
          });
        }
      })
    );
  } else {
    // Default action - open the app
    event.waitUntil(
      clients.openWindow('/')
    );
  }
});

// Handle messages from main app for local operations
self.addEventListener('message', event => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  } else if (event.data && event.data.type === 'CACHE_API_RESPONSE') {
    // Cache API responses for offline access
    caches.open(CACHE_NAME).then(cache => {
      cache.put(event.data.url, new Response(JSON.stringify(event.data.data), {
        headers: { 'Content-Type': 'application/json' }
      }));
    });
  }
});