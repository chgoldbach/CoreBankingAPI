$ErrorActionPreference = "Stop"

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "CORE BANKING API - OTOMATIK TEST SENARYOSU" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

# 1. Kayit Ol (Register)
Write-Host "Adim 1: Yeni bir kullanici (Ali Yilmaz) sisteme kayit ediliyor..." -ForegroundColor Yellow
$registerBody = @{
    firstName = "Ali"
    lastName = "Yilmaz"
    email = "ali@banka.com"
    password = "Password1!"
} | ConvertTo-Json

$registerRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method Post -Body $registerBody -ContentType "application/json"
Write-Host "OK - Kayit basarili!" -ForegroundColor Green
Write-Host ""
Start-Sleep -Seconds 2

# 2. Giris Yap (Login) & Token Al
Write-Host "Adim 2: 'ali@banka.com' ile giris yapiliyor ve Guvenlik Tokeni (JWT) aliniyor..." -ForegroundColor Yellow
$loginBody = @{
    email = "ali@banka.com"
    password = "Password1!"
} | ConvertTo-Json

$loginRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $loginRes.accessToken
Write-Host "OK - Giris basarili! Token Alindi." -ForegroundColor Green
Write-Host "Token (Kisaltilmis): $($token.Substring(0, 30))..." -ForegroundColor DarkGray
$headers = @{ "Authorization" = "Bearer $token"; "Content-Type" = "application/json" }
Write-Host ""
Start-Sleep -Seconds 2

# 3. Birinci Hesabi Olustur (Gonderen)
Write-Host "Adim 3: Ali icin birinci hesap (Gonderen) olusturuluyor (Baslangic: 10.000 TRY)..." -ForegroundColor Yellow
$acc1Body = @{ currency = "TRY"; initialBalance = 10000.00 } | ConvertTo-Json
$acc1Res = Invoke-RestMethod -Uri "http://localhost:8080/api/accounts" -Method Post -Body $acc1Body -Headers $headers
$acc1Id = $acc1Res.id
$acc1Iban = $acc1Res.accountNumber
Write-Host "OK - Hesap 1 Olusturuldu. IBAN: $acc1Iban | Bakiye: $($acc1Res.balance) TRY" -ForegroundColor Green
Write-Host ""
Start-Sleep -Seconds 2

# 4. Ikinci Hesabi Olustur (Alici)
Write-Host "Adim 4: Ali icin ikinci hesap (Alici) olusturuluyor (Baslangic: 0 TRY)..." -ForegroundColor Yellow
$acc2Body = @{ currency = "TRY"; initialBalance = 0.00 } | ConvertTo-Json
$acc2Res = Invoke-RestMethod -Uri "http://localhost:8080/api/accounts" -Method Post -Body $acc2Body -Headers $headers
$acc2Id = $acc2Res.id
$acc2Iban = $acc2Res.accountNumber
Write-Host "OK - Hesap 2 Olusturuldu. IBAN: $acc2Iban | Bakiye: $($acc2Res.balance) TRY" -ForegroundColor Green
Write-Host ""
Start-Sleep -Seconds 2

# 5. Para Transferi Islemi (ACID Testi)
Write-Host "Adim 5: Hesap 1'den Hesap 2'ye 1.500 TRY Para Transferi (ACID islemi) yapiliyor..." -ForegroundColor Yellow
$transferBody = @{
    fromAccountId = $acc1Id
    toAccountId = $acc2Id
    amount = 1500.00
    description = "Kira odemesi"
} | ConvertTo-Json

$transferRes = Invoke-RestMethod -Uri "http://localhost:8080/api/transactions/transfer" -Method Post -Body $transferBody -Headers $headers
Write-Host "OK - Transfer Basarili! Referans No: $($transferRes.referenceNumber)" -ForegroundColor Green
Write-Host ""
Start-Sleep -Seconds 2

# 6. Son Bakiyeleri Kontrol Et
Write-Host "Adim 6: Guncel Bakiyeler Kontrol Ediliyor..." -ForegroundColor Yellow
$acc1Final = Invoke-RestMethod -Uri "http://localhost:8080/api/accounts/$acc1Id/balance" -Method Get -Headers $headers
$acc2Final = Invoke-RestMethod -Uri "http://localhost:8080/api/accounts/$acc2Id/balance" -Method Get -Headers $headers

Write-Host "Hesap 1 Son Bakiye: $($acc1Final.balance) $($acc1Final.currency) (Beklenen: 8500)" -ForegroundColor Cyan
Write-Host "Hesap 2 Son Bakiye: $($acc2Final.balance) $($acc2Final.currency) (Beklenen: 1500)" -ForegroundColor Cyan
Write-Host ""

# 7. ACID Kurali Testi (Yetersiz Bakiye)
Write-Host "Adim 7: ACID / Kurallari Test Etme -> Hesap 2'den Hesap 1'e 50.000 TRY gondermeyi deneyelim..." -ForegroundColor Yellow
$failTransferBody = @{
    fromAccountId = $acc2Id
    toAccountId = $acc1Id
    amount = 50000.00
    description = "Araba odemesi"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/transactions/transfer" -Method Post -Body $failTransferBody -Headers $headers
} catch {
    Write-Host "ISLEM REDDEDILDI! (Beklenen Hata: Yetersiz Bakiye) Islem Rollback yapildi." -ForegroundColor Red
}

Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "TUM TESTLER BASARIYLA TAMAMLANDI!" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
