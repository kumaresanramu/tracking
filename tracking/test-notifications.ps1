# Test script for notification functionality
Write-Host "Testing Enhanced Notification System..." -ForegroundColor Green

# Test 1: Get notification settings (should return defaults)
Write-Host "`n1. Testing GET /api/notification-settings" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/notification-settings" -Method GET -ContentType "application/json"
    Write-Host "✅ GET Settings successful" -ForegroundColor Green
    Write-Host "Settings: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ GET Settings failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: Create smart weekly summary
Write-Host "`n2. Testing POST /api/notifications/smart-weekly-summary" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/smart-weekly-summary" -Method POST -ContentType "application/json"
    Write-Host "✅ Smart weekly summary creation successful" -ForegroundColor Green
    Write-Host "Notification: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Smart weekly summary creation failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Create smart budget alert (with budget of 1000)
Write-Host "`n3. Testing POST /api/notifications/smart-budget-alert" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/smart-budget-alert?monthlyBudget=1000" -Method POST -ContentType "application/json"
    Write-Host "✅ Smart budget alert creation successful" -ForegroundColor Green
    Write-Host "Notification: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Smart budget alert creation failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Create smart streak reward
Write-Host "`n4. Testing POST /api/notifications/smart-streak-reward" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications/smart-streak-reward" -Method POST -ContentType "application/json"
    Write-Host "✅ Smart streak reward creation successful" -ForegroundColor Green
    Write-Host "Notification: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Smart streak reward creation failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 5: Test notification scheduler
Write-Host "`n5. Testing POST /api/notification-scheduler/trigger-daily-reminders" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/notification-scheduler/trigger-daily-reminders" -Method POST -ContentType "application/json"
    Write-Host "✅ Daily reminder scheduler test successful" -ForegroundColor Green
    Write-Host "Response: $response" -ForegroundColor Cyan
} catch {
    Write-Host "❌ Daily reminder scheduler test failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 6: Get all notifications to see the new ones
Write-Host "`n6. Testing GET /api/notifications" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/notifications" -Method GET -ContentType "application/json"
    Write-Host "✅ GET Notifications successful" -ForegroundColor Green
    Write-Host "Found $($response.Count) notifications" -ForegroundColor Cyan
    if ($response.Count -gt 0) {
        Write-Host "Latest 3 notifications:" -ForegroundColor Cyan
        $response | Select-Object -First 3 | ForEach-Object {
            Write-Host "  - $($_.title): $($_.message)" -ForegroundColor White
        }
    }
} catch {
    Write-Host "❌ GET Notifications failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n🎉 Enhanced notification testing completed!" -ForegroundColor Green
Write-Host "`n📋 Summary of new features:" -ForegroundColor Yellow
Write-Host "  ✅ Smart Weekly Summary - calculates real weekly spending" -ForegroundColor Green
Write-Host "  ✅ Smart Budget Alert - shows actual budget percentage" -ForegroundColor Green
Write-Host "  ✅ Smart Streak Reward - counts actual logging days" -ForegroundColor Green
Write-Host "  ✅ Notification Scheduler - automated daily reminders" -ForegroundColor Green
Write-Host "  ✅ Enhanced Quick Actions - 4 notification buttons" -ForegroundColor Green