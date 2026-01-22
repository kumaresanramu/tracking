# API Test Script
Write-Host "Testing Expense Tracker API endpoints..." -ForegroundColor Green

$baseUrl = "http://localhost:8080/api"

# Test 1: Get Categories
Write-Host "`n1. Testing GET /api/categories" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/categories" -Method GET -UseBasicParsing
    Write-Host "✓ Categories endpoint working - Status: $($response.StatusCode)" -ForegroundColor Green
    $categories = $response.Content | ConvertFrom-Json
    Write-Host "  Found $($categories.Count) categories" -ForegroundColor Cyan
} catch {
    Write-Host "✗ Categories endpoint failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: Get Monthly Trends
Write-Host "`n2. Testing GET /api/analytics/monthly-trends" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/analytics/monthly-trends?months=12" -Method GET -UseBasicParsing
    Write-Host "✓ Monthly trends endpoint working - Status: $($response.StatusCode)" -ForegroundColor Green
    $trends = $response.Content | ConvertFrom-Json
    Write-Host "  Received $($trends.Count) months of data" -ForegroundColor Cyan
} catch {
    Write-Host "✗ Monthly trends endpoint failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Get Category Breakdown
Write-Host "`n3. Testing GET /api/analytics/category-breakdown" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/analytics/category-breakdown/2025/1" -Method GET -UseBasicParsing
    Write-Host "✓ Category breakdown endpoint working - Status: $($response.StatusCode)" -ForegroundColor Green
    $breakdown = $response.Content | ConvertFrom-Json
    Write-Host "  Received $($breakdown.Count) category breakdowns" -ForegroundColor Cyan
} catch {
    Write-Host "✗ Category breakdown endpoint failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Get Expense Summary
Write-Host "`n4. Testing GET /api/analytics/summary" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/analytics/summary/2025/1" -Method GET -UseBasicParsing
    Write-Host "✓ Expense summary endpoint working - Status: $($response.StatusCode)" -ForegroundColor Green
    $summary = $response.Content | ConvertFrom-Json
    Write-Host "  Total expenses: $($summary.expenseCount), Total amount: $($summary.totalAmount)" -ForegroundColor Cyan
} catch {
    Write-Host "✗ Expense summary endpoint failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 5: Get Monthly Expenses
Write-Host "`n5. Testing GET /api/expenses/month" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/expenses/month/2025/1" -Method GET -UseBasicParsing
    Write-Host "✓ Monthly expenses endpoint working - Status: $($response.StatusCode)" -ForegroundColor Green
    $expenses = $response.Content | ConvertFrom-Json
    Write-Host "  Found $($expenses.Count) expenses for January 2025" -ForegroundColor Cyan
} catch {
    Write-Host "✗ Monthly expenses endpoint failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 6: Get Payment Reminders
Write-Host "`n6. Testing GET /api/reminders" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/reminders" -Method GET -UseBasicParsing
    Write-Host "✓ Payment reminders endpoint working - Status: $($response.StatusCode)" -ForegroundColor Green
    $reminders = $response.Content | ConvertFrom-Json
    Write-Host "  Found $($reminders.Count) payment reminders" -ForegroundColor Cyan
} catch {
    Write-Host "✗ Payment reminders endpoint failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nAPI testing completed!" -ForegroundColor Green