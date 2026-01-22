# Test script for notification functionality
Write-Host "Testing Notification Settings API..." -ForegroundColor Green

# Test 1: Get notification settings (should return defaults)
Write-Host "`n1. Testing GET /api/notification-settings" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/notification-settings" -Method GET -ContentType "application/json"
    Write-Host "✅ GET Settings successful" -ForegroundColor Green
    Write-Host "Settings: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ GET Settings failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: Update notification settings
Write-Host "`n2. Testing PUT /api/notification-settings" -ForegroundColor Yellow
$settingsData = @{
    enableDailyReminder = $true
    dailyReminderTime = "20:00"
    enableBudgetAlerts = $true
    budgetWarningThreshold = 80
    enableWeeklySummary = $true
    weeklySummaryTime = "09:00"
    enableStreakRewards = $true
    enableBadges = $true
    quietHoursStart = "22:00"
    quietHoursEnd = "08:00"
    enableEmailNotifications = $false
    emailAddress = ""
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/notification-settings" -Method PUT -Body $settingsData -ContentType "application/json"
    Write-Host "✅ PUT Settings successful" -ForegroundColor Green
    Write-Host "Updated Settings: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ PUT Settings failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Create a daily reminder notification
Write-Host "`n3. Testing POST /api/notifications/daily-reminder" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/daily-reminder" -Method POST -ContentType "application/json"
    Write-Host "✅ Daily reminder creation successful" -ForegroundColor Green
    Write-Host "Notification: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Daily reminder creation failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Create a weekly summary notification
Write-Host "`n4. Testing POST /api/notifications/weekly-summary" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/weekly-summary?totalSpent=150.50&topCategory=Food" -Method POST -ContentType "application/json"
    Write-Host "✅ Weekly summary creation successful" -ForegroundColor Green
    Write-Host "Notification: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Weekly summary creation failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 5: Get all notifications
Write-Host "`n5. Testing GET /api/notifications" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications" -Method GET -ContentType "application/json"
    Write-Host "✅ GET Notifications successful" -ForegroundColor Green
    Write-Host "Found $($response.Count) notifications" -ForegroundColor Cyan
    if ($response.Count -gt 0) {
        Write-Host "Latest notification: $($response[0] | ConvertTo-Json -Depth 2)" -ForegroundColor Cyan
    }
} catch {
    Write-Host "❌ GET Notifications failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n🎉 Notification testing completed!" -ForegroundColor Green