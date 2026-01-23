# PWA Testing Guide

This guide covers how to test all the Progressive Web App (PWA) features implemented in the Expense Tracker application.

## Prerequisites

1. **HTTPS Required**: PWA features only work over HTTPS or localhost
2. **Modern Browser**: Chrome 67+, Firefox 60+, Safari 11.1+, Edge 79+
3. **Running Application**: Make sure the Spring Boot app is running on `http://localhost:8080`

## 1. Testing PWA Install Prompt Management

### 1.1 Basic Install Prompt Test

**Steps:**
1. Open Chrome and navigate to `http://localhost:8080`
2. Wait 30 seconds (minimum session time before prompt appears)
3. Look for the custom install banner at the bottom of the screen
4. Click "Install" to test the installation flow
5. Click "Not now" to test dismissal behavior

**Expected Results:**
- Install banner appears after 30 seconds
- Clicking "Install" triggers browser's native install prompt
- Clicking "Not now" hides banner and sets 7-day cooldown
- Banner won't appear again for 7 days after dismissal

### 1.2 Install State Persistence Test

**Steps:**
1. Dismiss the install prompt
2. Refresh the page
3. Check that prompt doesn't appear immediately
4. Open DevTools → Application → Local Storage
5. Look for `pwa-install-state` key

**Expected Results:**
- Install state is saved in localStorage
- Prompt respects dismissal state across sessions

### 1.3 Platform-Specific Instructions Test

**Steps:**
1. If native install prompt fails, click "Install" again
2. Check that platform-specific instructions appear
3. Test on different browsers (Chrome, Firefox, Safari, Edge)

**Expected Results:**
- Different instructions for each browser
- Clear, actionable guidance for manual installation

## 2. Testing App Update Notification System

### 2.1 Update Detection Test

**Steps:**
1. Open the app in Chrome
2. Open DevTools → Application → Service Workers
3. Check "Update on reload" checkbox
4. Modify any file in `/static/js/` (add a comment)
5. Reload the page
6. Look for update notification banner at the top

**Expected Results:**
- Update banner appears when new service worker is detected
- Banner shows "Update Available" message with action buttons

### 2.2 Manual Update Check Test

**Steps:**
1. Open DevTools Console
2. Run: `app.appUpdateManager.manualUpdateCheck()`
3. Check for update detection logs

**Expected Results:**
- Console shows update checking process
- Service worker update is triggered

### 2.3 Update Application Test

**Steps:**
1. When update banner appears, click "Update Now"
2. Watch for progress indicator
3. App should reload automatically

**Expected Results:**
- Progress bar appears at top of screen
- App reloads with new version
- Update banner disappears

## 3. Testing Enhanced Manifest Features

### 3.1 PWA Installation Test

**Steps:**
1. Open Chrome → Menu → "Install Expense Tracker"
2. Or look for install icon in address bar
3. Install the app
4. Launch from desktop/home screen

**Expected Results:**
- App installs as standalone application
- Launches without browser UI
- Shows proper app icon and name

### 3.2 App Shortcuts Test

**Steps:**
1. Install the PWA
2. Right-click on app icon (desktop) or long-press (mobile)
3. Check available shortcuts

**Expected Results:**
- "Add Expense" shortcut
- "View Analytics" shortcut  
- "Monthly Summary" shortcut

### 3.3 Shortcut Functionality Test

**Steps:**
1. Use "Add Expense" shortcut
2. Check URL contains `?shortcut=true`
3. Verify app navigates to expenses page
4. Check that description field gets focus

**Expected Results:**
- App opens to correct page
- URL parameters are processed
- UI responds appropriately

## 4. Testing Share Target Feature

### 4.1 Share to App Test (Android/Mobile)

**Steps:**
1. Install PWA on mobile device
2. From another app, share text containing expense info
3. Select "Expense Tracker" from share menu
4. Check that shared content populates expense form

**Expected Results:**
- App appears in system share menu
- Shared text is parsed for amount and description
- Form is pre-filled with extracted data

### 4.2 Web Share API Test

**Steps:**
1. Add share buttons to the app (if not present)
2. Click share button
3. Check that native share dialog appears

**Expected Results:**
- Native share dialog opens
- Can share to other apps/contacts

## 5. Testing Offline Capabilities

### 5.1 Service Worker Installation Test

**Steps:**
1. Open DevTools → Application → Service Workers
2. Check that service worker is registered and active
3. Look for cached resources in Cache Storage

**Expected Results:**
- Service worker shows as "activated and running"
- Multiple caches present (static, API, runtime)
- Static resources are cached

### 5.2 Offline Functionality Test

**Steps:**
1. Load the app normally
2. Open DevTools → Network → Check "Offline"
3. Refresh the page
4. Try navigating between pages

**Expected Results:**
- App loads from cache when offline
- Navigation works offline
- Cached content is displayed

## 6. Testing Keyboard Shortcuts

### 6.1 App Shortcuts Test

**Steps:**
1. Focus on the app
2. Press `Ctrl+N` (or `Cmd+N` on Mac)
3. Press `Ctrl+A` (or `Cmd+A` on Mac)
4. Press `Ctrl+D` (or `Cmd+D` on Mac)

**Expected Results:**
- `Ctrl+N`: Opens expenses page and focuses description field
- `Ctrl+A`: Opens analytics page
- `Ctrl+D`: Opens dashboard page

## 7. Browser-Specific Testing

### 7.1 Chrome Testing
- Install prompt appears
- Service worker works correctly
- Push notifications supported
- App shortcuts work

### 7.2 Firefox Testing
- Install prompt may differ
- Service worker functionality
- Limited push notification support

### 7.3 Safari Testing (iOS/macOS)
- Add to Home Screen functionality
- Service worker limitations
- No push notifications on iOS

### 7.4 Edge Testing
- Similar to Chrome behavior
- Windows integration features

## 8. Developer Tools for PWA Testing

### 8.1 Chrome DevTools

**Lighthouse PWA Audit:**
1. Open DevTools → Lighthouse
2. Select "Progressive Web App" category
3. Run audit
4. Check for 100% PWA score

**Application Tab:**
- Service Workers: Check registration and updates
- Storage: View cached data and localStorage
- Manifest: Validate manifest.json

### 8.2 PWA Testing Checklist

**Manifest Requirements:**
- [ ] Valid manifest.json
- [ ] Name and short_name
- [ ] Icons (192x192, 512x512)
- [ ] Start URL
- [ ] Display mode
- [ ] Theme color

**Service Worker Requirements:**
- [ ] Service worker registered
- [ ] Caches static resources
- [ ] Works offline
- [ ] Handles updates

**Installation Requirements:**
- [ ] Installable (meets PWA criteria)
- [ ] Install prompt appears
- [ ] Can be installed
- [ ] Launches standalone

## 9. Testing Commands

### 9.1 Console Testing Commands

```javascript
// Test install manager
app.pwaInstallManager.forceShowInstallPrompt();
app.pwaInstallManager.isAppInstalled();
app.pwaInstallManager.canShowInstallPrompt();

// Test update manager
app.appUpdateManager.manualUpdateCheck();
app.appUpdateManager.isUpdateAvailable();
app.appUpdateManager.forceShowUpdateNotification();

// Test service worker
navigator.serviceWorker.ready.then(reg => console.log('SW ready:', reg));
navigator.serviceWorker.getRegistrations().then(regs => console.log('SW registrations:', regs));

// Test caches
caches.keys().then(names => console.log('Cache names:', names));
caches.open('expense-tracker-static-v2').then(cache => cache.keys()).then(keys => console.log('Cached resources:', keys));
```

### 9.2 Network Testing

```bash
# Test with different network conditions
# In Chrome DevTools → Network → Throttling:
- Fast 3G
- Slow 3G  
- Offline
```

## 10. Common Issues and Troubleshooting

### 10.1 Install Prompt Not Appearing
- Check HTTPS requirement
- Verify manifest.json is valid
- Ensure service worker is registered
- Check browser support
- Wait for minimum session time (30 seconds)

### 10.2 Service Worker Not Updating
- Hard refresh (Ctrl+Shift+R)
- Clear browser cache
- Check "Update on reload" in DevTools
- Verify service worker version

### 10.3 Offline Not Working
- Check service worker registration
- Verify resources are cached
- Check network throttling settings
- Inspect cache contents

### 10.4 Push Notifications Not Working
- Check HTTPS requirement
- Verify VAPID keys are configured
- Check browser permissions
- Test notification permission request

## 11. Automated Testing

### 11.1 Lighthouse CI
```bash
# Install Lighthouse CI
npm install -g @lhci/cli

# Run PWA audit
lhci autorun --upload.target=temporary-public-storage
```

### 11.2 PWA Testing Tools
- **PWA Builder**: https://www.pwabuilder.com/
- **Workbox**: For service worker testing
- **Web App Manifest Validator**: Online validation tools

## 12. Performance Testing

### 12.1 Load Time Testing
- First Contentful Paint (FCP) < 2s
- Largest Contentful Paint (LCP) < 2.5s
- Time to Interactive (TTI) < 3s

### 12.2 Cache Performance
- Static resources load from cache
- API responses cached appropriately
- Cache size stays within limits

## 13. Security Testing

### 13.1 HTTPS Verification
- All resources served over HTTPS
- Mixed content warnings resolved
- Service worker only works on HTTPS

### 13.2 Content Security Policy
- CSP headers properly configured
- No unsafe-inline or unsafe-eval (where possible)
- External resources whitelisted

This comprehensive testing guide covers all aspects of the PWA implementation. Start with basic functionality tests and progress to advanced features and edge cases.