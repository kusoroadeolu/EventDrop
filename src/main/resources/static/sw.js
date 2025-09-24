const CACHE_NAME = 'eventdrop-v1';
const STATIC_CACHE_NAME = 'eventdrop-static-v1';

// Static assets to cache (based on your Spring Security matchers)
const STATIC_ASSETS = [
  '/landing-page.html',
  '/create.html',
  '/shared-header.js',
  '/shared-styles.css',
  '/landing-page.js',
  '/create-room.js',
  '/manifest.json',
  // External fonts and icons
  'https://cdnjs.cloudflare.com/ajax/libs/remixicon/4.6.0/remixicon.min.css',
  'https://assets-persist.lovart.ai/agent-static-assets/MiSans-Regular.ttf',
  'https://assets-persist.lovart.ai/agent-static-assets/MiSans-Medium.ttf',
  'https://assets-persist.lovart.ai/agent-static-assets/MiSans-Bold.ttf'
];

// Room-specific files that should NOT be cached (dynamic content)
const DYNAMIC_FILES = [
  'codecopy.js',
  'rooms.html',
  'rooms.js',
  'rooms-mobile.js',
  'rooms-mobile.css'
];

// API endpoints - network first
const API_PATTERNS = [
  '/rooms/',
  '/files/',
  '/metrics'
];

// Install event
self.addEventListener('install', event => {
  console.log('EventDrop Service Worker installing...');
  event.waitUntil(
    caches.open(STATIC_CACHE_NAME)
      .then(cache => {
        console.log('Caching EventDrop static assets...');
        return cache.addAll(STATIC_ASSETS);
      })
      .then(() => {
        console.log('EventDrop assets cached successfully');
        return self.skipWaiting();
      })
      .catch(error => {
        console.error('Failed to cache EventDrop assets:', error);
      })
  );
});

// Activate event
self.addEventListener('activate', event => {
  console.log('EventDrop Service Worker activating...');
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cacheName => {
          if (cacheName !== STATIC_CACHE_NAME && cacheName !== CACHE_NAME) {
            console.log('Deleting old EventDrop cache:', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
    }).then(() => {
      console.log('EventDrop Service Worker activated');
      return self.clients.claim();
    })
  );
});

// Fetch event
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);

  if (event.request.method !== 'GET') return;
  if (url.protocol === 'chrome-extension:') return;

  // Skip dynamic room files
  if (isDynamicFile(url)) return;

  // Handle different request types
  if (isApiRequest(url)) {
    event.respondWith(networkFirstStrategy(event.request));
  } else if (isStaticAsset(url)) {
    event.respondWith(cacheFirstStrategy(event.request));
  } else {
    event.respondWith(networkFirstWithFallback(event.request));
  }
});

function isDynamicFile(url) {
  return DYNAMIC_FILES.some(file => url.pathname.includes(file));
}

function isApiRequest(url) {
  return API_PATTERNS.some(pattern => url.pathname.includes(pattern));
}

function isStaticAsset(url) {
  const pathname = url.pathname;
  return STATIC_ASSETS.some(asset => {
    if (asset.startsWith('http')) {
      return url.href === asset;
    }
    return pathname === asset || pathname.endsWith(asset);
  });
}

// Network first for API calls
async function networkFirstStrategy(request) {
  try {
    const networkResponse = await fetch(request);

    if (networkResponse.ok) {
      const cache = await caches.open(CACHE_NAME);
      cache.put(request, networkResponse.clone());
    }

    return networkResponse;
  } catch (error) {
    console.log('API network failed, trying cache:', request.url);

    const cachedResponse = await caches.match(request);
    if (cachedResponse) {
      return cachedResponse;
    }

    if (request.url.includes('/rooms/')) {
      return createOfflineResponse();
    }

    throw error;
  }
}

// Cache first for static assets
async function cacheFirstStrategy(request) {
  const cachedResponse = await caches.match(request);

  if (cachedResponse) {
    // Update cache in background
    fetch(request).then(networkResponse => {
      if (networkResponse.ok) {
        caches.open(STATIC_CACHE_NAME).then(cache => {
          cache.put(request, networkResponse);
        });
      }
    }).catch(() => {});

    return cachedResponse;
  }

  try {
    const networkResponse = await fetch(request);

    if (networkResponse.ok) {
      const cache = await caches.open(STATIC_CACHE_NAME);
      cache.put(request, networkResponse.clone());
    }

    return networkResponse;
  } catch (error) {
    throw error;
  }
}

// Network first with offline fallback
async function networkFirstWithFallback(request) {
  try {
    return await fetch(request);
  } catch (error) {
    const cachedResponse = await caches.match(request);
    if (cachedResponse) {
      return cachedResponse;
    }

    if (request.headers.get('accept').includes('text/html')) {
      return createOfflineResponse();
    }

    throw error;
  }
}

// Create offline page
function createOfflineResponse() {
  const offlineHTML = `
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>EventDrop - Offline</title>
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/remixicon/4.6.0/remixicon.min.css">
        <style>
            @font-face {
                font-family: 'MiSans';
                src: url('https://assets-persist.lovart.ai/agent-static-assets/MiSans-Bold.ttf') format('truetype');
                font-weight: bold;
            }
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            body {
                font-family: 'MiSans', sans-serif;
                background: linear-gradient(135deg, #f5f7fa 0%, #e4edf5 100%);
                min-height: 100vh;
                display: flex;
                align-items: center;
                justify-content: center;
                color: #333;
                text-align: center;
                padding: 20px;
            }
            .offline-container {
                background: white;
                padding: 60px 40px;
                border-radius: 12px;
                box-shadow: 0 8px 24px rgba(0,0,0,0.08);
                max-width: 500px;
                width: 100%;
            }
            .offline-icon {
                font-size: 80px;
                color: #3498db;
                margin-bottom: 20px;
            }
            h1 {
                font-size: 32px;
                color: #2c3e50;
                margin-bottom: 15px;
                font-weight: bold;
            }
            p {
                font-size: 18px;
                color: #666;
                margin-bottom: 30px;
                line-height: 1.6;
            }
            .btn {
                display: inline-block;
                background-color: #3498db;
                color: white;
                padding: 14px 24px;
                border-radius: 6px;
                text-decoration: none;
                font-weight: 500;
                font-size: 16px;
                transition: all 0.3s ease;
                border: none;
                cursor: pointer;
            }
            .btn:hover {
                background-color: #2980b9;
            }
            .btn i {
                margin-right: 8px;
            }
            @media (max-width: 768px) {
                .offline-container {
                    padding: 40px 20px;
                }
                h1 {
                    font-size: 28px;
                }
                p {
                    font-size: 16px;
                }
                .offline-icon {
                    font-size: 60px;
                }
            }
        </style>
    </head>
    <body>
        <div class="offline-container">
            <div class="offline-icon">
                <i class="ri-wifi-off-line"></i>
            </div>
            <h1>You're Offline</h1>
            <p>EventDrop needs an internet connection for real-time file sharing.</p>
            <button class="btn" onclick="window.location.reload()">
                <i class="ri-refresh-line"></i>
                Try Again
            </button>
        </div>
        <script>
            window.addEventListener('online', function() {
                window.location.reload();
            });
        </script>
    </body>
    </html>
  `;

  return new Response(offlineHTML, {
    status: 200,
    statusText: 'OK',
    headers: {
      'Content-Type': 'text/html; charset=utf-8',
    },
  });
}