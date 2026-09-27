# ---------------------------------------------------------------------------
# ATM API test script — run this any time to verify the whole system.
#
#   1. Start the app:   .\mvnw.cmd spring-boot:run
#   2. Run this:        .\test-api.ps1
# ---------------------------------------------------------------------------
$base = "http://localhost:8080"
$script:pass = 0
$script:fail = 0

function Check($name, $condition, $detail) {
    if ($condition) {
        Write-Host "[PASS] $name -- $detail" -ForegroundColor Green
        $script:pass++
    } else {
        Write-Host "[FAIL] $name -- $detail" -ForegroundColor Red
        $script:fail++
    }
}

# Sends a request and returns @{ ok; status; data; error } instead of throwing.
function Req($method, $path, $body, $headers) {
    try {
        $p = @{ Uri = "$base$path"; Method = $method }
        if ($body)   { $p.Body = $body; $p.ContentType = 'application/json' }
        if ($headers) { $p.Headers = $headers }
        $r = Invoke-RestMethod @p
        return @{ ok = $true; status = 200; data = $r }
    } catch {
        $status = 0
        $err = $null
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode.value__
            # Windows PowerShell does not always populate ErrorDetails.Message,
            # so read the response body directly from the stream.
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                $stream.Position = 0
                $reader = New-Object IO.StreamReader($stream)
                $err = $reader.ReadToEnd()
                $reader.Close()
            } catch { }
        }
        if (-not $err) { $err = $_.ErrorDetails.Message }
        return @{ ok = $false; status = $status; error = $err }
    }
}

Write-Host "`n===== 1. AUTHENTICATION =====" -ForegroundColor Cyan

$login = Req Post '/api/auth/login' '{"accountNumber":"1001","pin":"1234"}'
Check "login with correct PIN" $login.ok "status 200, got token"
$token = $login.data.token
$auth  = @{ Authorization = "Bearer $token" }
$id    = $login.data.accountId

$badLogin = Req Post '/api/auth/login' '{"accountNumber":"1001","pin":"0000"}'
Check "login with wrong PIN rejected" ($badLogin.status -eq 401) "HTTP $($badLogin.status)"

$noToken = Req Get "/api/accounts/$id/balance"
Check "request without token rejected" ($noToken.status -eq 401) "HTTP $($noToken.status)"

Write-Host "`n===== 2. BALANCE =====" -ForegroundColor Cyan

$bal1 = Req Get "/api/accounts/$id/balance" $null $auth
Check "read balance" $bal1.ok "balance = $($bal1.data.balance)"
$startBalance = $bal1.data.balance

Write-Host "`n===== 3. DEPOSIT =====" -ForegroundColor Cyan

$dep = Req Post "/api/accounts/$id/deposit" '{"amount":100}' $auth
Check "deposit 100" ($dep.ok -and $dep.data.balanceAfter -eq ($startBalance + 100)) "balance $($startBalance) -> $($dep.data.balanceAfter)"

Write-Host "`n===== 4. WITHDRAWAL =====" -ForegroundColor Cyan

$wd = Req Post "/api/accounts/$id/withdraw" '{"amount":40,"pin":"1234"}' $auth
Check "withdraw 40 with correct PIN" ($wd.ok -and $wd.data.balanceAfter -eq ($startBalance + 60)) "balance now $($wd.data.balanceAfter)"

$wdPin = Req Post "/api/accounts/$id/withdraw" '{"amount":10,"pin":"9999"}' $auth
Check "withdraw with wrong PIN rejected" ($wdPin.status -eq 401) "HTTP $($wdPin.status)"

$wdFunds = Req Post "/api/accounts/$id/withdraw" '{"amount":99999999,"pin":"1234"}' $auth
Check "withdraw more than balance rejected" ($wdFunds.status -eq 400) "HTTP $($wdFunds.status)"

Write-Host "`n===== 5. TRANSFER =====" -ForegroundColor Cyan

$tr = Req Post '/api/transfers' '{"toAccountNumber":"1002","amount":25,"pin":"1234"}' $auth
Check "transfer 25 to account 1002" ($tr.ok -and $tr.data.Count -eq 2) "got 2 ledger rows (TRANSFER_OUT + TRANSFER_IN)"

$trSelf = Req Post '/api/transfers' '{"toAccountNumber":"1001","amount":5,"pin":"1234"}' $auth
Check "transfer to same account rejected" ($trSelf.status -eq 400) "HTTP $($trSelf.status)"

$trMissing = Req Post '/api/transfers' '{"toAccountNumber":"9999","amount":5,"pin":"1234"}' $auth
Check "transfer to unknown account -> 404" ($trMissing.status -eq 404) "HTTP $($trMissing.status)"

Write-Host "`n===== 6. STATEMENT =====" -ForegroundColor Cyan

$hist = Req Get "/api/accounts/$id/transactions" $null $auth
Check "statement has entries" ($hist.ok -and $hist.data.Count -gt 0) "$($hist.data.Count) transactions, newest first"

Write-Host "`n===== 7. SECURITY =====" -ForegroundColor Cyan

$foreign = Req Get "/api/accounts/2/balance" $null $auth
Check "cannot read someone else's account" ($foreign.status -eq 403) "HTTP $($foreign.status)"

Write-Host "`n===== 8. INPUT VALIDATION =====" -ForegroundColor Cyan

$invalid = Req Post '/api/accounts' '{"accountNumber":"ab","pin":"1","ownerName":""}'
$hasFields = $invalid.error -and $invalid.error.Contains("fields")
Check "bad account data -> 400 + field errors" (($invalid.status -eq 400) -and $hasFields) "HTTP $($invalid.status) with per-field messages"

$duplicate = Req Post '/api/accounts' '{"accountNumber":"1001","pin":"1234","ownerName":"X"}'
Check "duplicate account number -> 409" ($duplicate.status -eq 409) "HTTP $($duplicate.status)"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "RESULT: $script:pass passed, $script:fail failed" -ForegroundColor $(if ($script:fail -eq 0) { "Green" } else { "Red" })
Write-Host "========================================`n"
if ($script:fail -gt 0) { exit 1 }
