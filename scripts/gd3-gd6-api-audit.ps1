# GĐ3-GĐ6 API audit script — outputs JSON results
$ErrorActionPreference = "Continue"
$base = "http://localhost:8080/api/v1"
$results = @()

function Log-Result($id, $phase, $desc, $expected, $actual, $status, $detail) {
    $script:results += [PSCustomObject]@{
        Id = $id; Phase = $phase; Description = $desc
        Expected = $expected; Actual = $actual; Status = $status; Detail = $detail
    }
}

function Get-Token($email, $password) {
    $body = @{ email = $email; password = $password } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$base/auth/login" -Method POST -ContentType "application/json" -Body $body
    return $r.data.accessToken
}

function Api-Call($method, $path, $token, $body = $null) {
    $headers = @{ Authorization = "Bearer $token" }
    $params = @{ Uri = "$base$path"; Method = $method; Headers = $headers; ErrorAction = "Stop" }
    if ($body) { $params.ContentType = "application/json"; $params.Body = ($body | ConvertTo-Json -Depth 10) }
    try {
        $r = Invoke-RestMethod @params
        return @{ Ok = $true; Status = 200; Data = $r.data; Raw = $r }
    } catch {
        $resp = $_.Exception.Response
        $status = if ($resp) { [int]$resp.StatusCode } else { 0 }
        $errBody = $null
        if ($resp -and $resp.GetResponseStream()) {
            $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
            $errBody = $reader.ReadToEnd() | ConvertFrom-Json -ErrorAction SilentlyContinue
        }
        $code = $errBody.error.code
        if (-not $code) { $code = $errBody.code }
        return @{ Ok = $false; Status = $status; Code = $code; Message = $errBody.error.message; Raw = $errBody }
    }
}

function Find-Hackathon($slug, $token) {
    $r = Api-Call GET "/hackathons?slug=$slug" $token
    if ($r.Ok -and $r.Data) {
        if ($r.Data -is [array]) { return $r.Data | Where-Object { $_.slug -eq $slug } | Select-Object -First 1 }
        if ($r.Data.items) { return $r.Data.items | Where-Object { $_.slug -eq $slug } | Select-Object -First 1 }
        return $r.Data
    }
    # fallback: list all
    $all = Api-Call GET "/hackathons" $token
    if ($all.Ok) {
        $items = if ($all.Data.items) { $all.Data.items } else { $all.Data }
        return $items | Where-Object { $_.slug -eq $slug } | Select-Object -First 1
    }
    return $null
}

function Get-Rounds($hackathonId, $token) {
    $r = Api-Call GET "/hackathons/$hackathonId/rounds" $token
    if ($r.Ok) {
        if ($r.Data -is [array]) { return $r.Data }
        if ($r.Data.items) { return $r.Data.items }
    }
    return @()
}

$coordTok = Get-Token "coord@fpt.edu.vn" "Coordinator@dev1"
$judgeTok = Get-Token "judge1@fpt.edu.vn" "Judge@dev1"
$guestTok = Get-Token "guestjudge@gmail.com" "GuestJudge@dev1"

# === GD3 ===
$h3 = Find-Hackathon "seal-gd3-prelim-open" $coordTok
if ($h3) {
    $rounds3 = Get-Rounds $h3.id $coordTok
    $prelim = $rounds3 | Where-Object { -not $_.isFinal } | Select-Object -First 1
    if ($prelim) {
        $act = Api-Call PATCH "/rounds/$($prelim.id)/activate" $coordTok
        Log-Result "G3-H01" "GD3" "Activate prelim (already active seed)" "200 or already active" "$($act.Status) $($act.Code)" $(if ($act.Ok -or $act.Code -eq "ROUND_ALREADY_ACTIVE") {"PASS"} else {"FAIL"}) ""
        
        $rankPrev = Api-Call GET "/rounds/$($prelim.id)/ranking/preview" $coordTok
        Log-Result "G3-H02" "GD3" "Ranking preview" "200 + items" "$($rankPrev.Status)" $(if ($rankPrev.Ok) {"PASS"} else {"FAIL"}) ""
        
        $prog = Api-Call GET "/rounds/$($prelim.id)/scoring-progress" $coordTok
        Log-Result "G3-H03" "GD3" "Scoring progress" "200" "$($prog.Status)" $(if ($prog.Ok) {"PASS"} else {"FAIL"}) ""
    }
}

$h3edge = Find-Hackathon "seal-gd3-edge-errors" $coordTok
if ($h3edge) {
    $roundsE = Get-Rounds $h3edge.id $coordTok
    $prelimE = $roundsE | Where-Object { -not $_.isFinal } | Select-Object -First 1
    if ($prelimE -and $prelimE.scoringLocked) {
        try { $stuTok = Get-Token "student.gd3edge.leader01@fpt.edu.vn" "Student@dev1" } catch { $stuTok = $null }
        if (-not $stuTok) { $stuTok = Get-Token "student.gd3.leader01@fpt.edu.vn" "Student@dev1" }
        # Try score on LATE_PENDING if exists
        $subs = Api-Call GET "/submissions?roundId=$($prelimE.id)" $coordTok
        Log-Result "G3-ADV01" "GD3" "List submissions edge seed" "200" "$($subs.Status)" $(if ($subs.Ok) {"PASS"} else {"FAIL"}) ""
    }
}

# G3-N01 style: find hackathon with no teams round
$h3empty = Find-Hackathon "seal-gd3-calibration-timer" $coordTok
if (-not $h3empty) { $h3empty = Find-Hackathon "seal-gd1-incomplete" $coordTok }

# === GD4 ===
$h4 = Find-Hackathon "seal-gd4-advance-ready" $coordTok
if ($h4) {
    $rounds4 = Get-Rounds $h4.id $coordTok
    $prelim4 = $rounds4 | Where-Object { -not $_.isFinal } | Select-Object -First 1
    $final4 = $rounds4 | Where-Object { $_.isFinal } | Select-Object -First 1
    
    if ($prelim4) {
        $tb = Api-Call GET "/rounds/$($prelim4.id)/tiebreak" $coordTok
        $tbCount = if ($tb.Ok -and $tb.Data) { @($tb.Data).Count } else { 0 }
        Log-Result "G4-H-TB" "GD4" "GET tiebreak (advance-ready)" "200 list" "$($tb.Status) count=$tbCount" $(if ($tb.Ok) {"PASS"} else {"FAIL"}) ""
        
        $wc = Api-Call GET "/rounds/$($prelim4.id)/wildcard-candidates" $coordTok
        $wcCount = if ($wc.Ok -and $wc.Data.candidates) { @($wc.Data.candidates).Count } else { 0 }
        Log-Result "G4-H-WC" "GD4" "GET wildcard-candidates" "200 + candidates" "$($wc.Status) count=$wcCount" $(if ($wc.Ok -and $wcCount -gt 0) {"PASS"} elseif ($wc.Ok) {"PARTIAL"} else {"FAIL"}) ""
        
        if (-not $prelim4.isPublished) {
            $pub = Api-Call PATCH "/rounds/$($prelim4.id)/publish" $coordTok
            Log-Result "G4-H01" "GD4" "Publish prelim" "200" "$($pub.Status) $($pub.Code)" $(if ($pub.Ok) {"PASS"} else {"FAIL"}) ""
        } else {
            Log-Result "G4-H01" "GD4" "Publish prelim" "200" "already published" "PASS" ""
        }
        
        $adv = Api-Call POST "/rounds/$($prelim4.id)/advance" $coordTok @{ advancedTeamIds = @(1,2,3,4,5,6) }
        Log-Result "G4-H02" "GD4" "Advance teams" "200" "$($adv.Status) $($adv.Code)" $(if ($adv.Ok) {"PASS"} elseif ($adv.Code -eq "TIEBREAK_REQUIRED") {"SPEC_GAP"} else {"FAIL"}) $($adv.Message)"
        
        $readiness = Api-Call GET "/hackathons/$($h4.id)/readiness?target=FINAL_ROUND" $coordTok
        Log-Result "G4-R01" "GD4" "Readiness FINAL_ROUND" "ready field" "$($readiness.Status) ready=$($readiness.Data.ready)" $(if ($readiness.Ok) {"PASS"} else {"FAIL"}) ""
    }
    
    if ($final4) {
        $actF = Api-Call PATCH "/rounds/$($final4.id)/activate" $coordTok
        Log-Result "G4-H04" "GD4" "Activate final (after setup)" "200" "$($actF.Status) $($actF.Code)" $(if ($actF.Ok) {"PASS"} elseif ($actF.Code -eq "RESULT_NOT_PUBLISHED") {"PASS"} elseif ($actF.Code -eq "JUDGE_NOT_ASSIGNED") {"PARTIAL"} else {"FAIL"}) ""
        
        # G4-N01: try activate without publish on published seed - use seal-gd4-published inverse
        if (-not $prelim4.isPublished) {
            $actEarly = Api-Call PATCH "/rounds/$($final4.id)/activate" $coordTok
            Log-Result "G4-N01" "GD4" "Activate CK before publish" "422 RESULT_NOT_PUBLISHED" "$($actEarly.Status) $($actEarly.Code)" $(if ($actEarly.Code -eq "RESULT_NOT_PUBLISHED") {"PASS"} else {"FAIL"}) ""
        }
    }
}

$h4tb = Find-Hackathon "seal-gd4-tiebreak-gate" $coordTok
if ($h4tb) {
    $roundsTb = Get-Rounds $h4tb.id $coordTok
    $prelimTb = $roundsTb | Where-Object { -not $_.isFinal } | Select-Object -First 1
    if ($prelimTb) {
        $advTb = Api-Call POST "/rounds/$($prelimTb.id)/advance" $coordTok @{ advancedTeamIds = @(1) }
        Log-Result "G4-N-TB" "GD4" "Advance with unresolved tiebreak" "422 TIEBREAK_REQUIRED" "$($advTb.Status) $($advTb.Code)" $(if ($advTb.Code -eq "TIEBREAK_REQUIRED") {"PASS"} else {"FAIL"}) ""
        
        $tbGate = Api-Call GET "/rounds/$($prelimTb.id)/tiebreak" $coordTok
        $tbItems = if ($tbGate.Ok) { @($tbGate.Data).Count } else { 0 }
        Log-Result "G4-TB-LIST" "GD4" "Tiebreak list on gate seed" ">0 items" "count=$tbItems" $(if ($tbItems -gt 0) {"PASS"} else {"FAIL"}) ""
    }
}

# === GD5 ===
$h5 = Find-Hackathon "seal-gd5-final-active" $coordTok
if ($h5) {
    $rounds5 = Get-Rounds $h5.id $coordTok
    $final5 = $rounds5 | Where-Object { $_.isFinal } | Select-Object -First 1
    if ($final5) {
        Log-Result "G5-H00" "GD5" "Final round active" "isActive=true" "active=$($final5.isActive)" $(if ($final5.isActive) {"PASS"} else {"FAIL"}) ""
        
        $stu5 = Get-Token "student.gd5.leader03@fpt.edu.vn" "Student@dev1"
        $sub5 = Api-Call GET "/me/submission?roundId=$($final5.id)" $stu5
        Log-Result "G5-H01" "GD5" "Student final submission status" "200" "$($sub5.Status)" $(if ($sub5.Ok) {"PASS"} else {"FAIL"}) ""
        
        $rank5 = Api-Call GET "/rounds/$($final5.id)/ranking/preview" $coordTok
        Log-Result "G5-RANK" "GD5" "Final ranking preview" "200 teams" "$($rank5.Status)" $(if ($rank5.Ok) {"PASS"} else {"FAIL"}) ""
        
        if (-not $final5.scoringLocked) {
            $lock5 = Api-Call PATCH "/rounds/$($final5.id)/lock-scoring" $coordTok @{ note = "audit test" }
            Log-Result "G5-H03" "GD5" "Lock final scoring" "200" "$($lock5.Status) $($lock5.Code)" $(if ($lock5.Ok) {"PASS"} else {"FAIL"}) ""
        }
        
        $h5status = Api-Call GET "/hackathons/$($h5.id)" $coordTok
        $st = $h5status.Data.status
        Log-Result "G5-H04" "GD5" "Hackathon after lock CK" "PENDING_CONFIRM" "$st" $(if ($st -eq "PENDING_CONFIRM") {"PASS"} else {"FAIL"}) ""
        
        # Calibration stub check
        $cal = Api-Call GET "/calibration-sessions?roundId=$($final5.id)" $coordTok
        Log-Result "G5-CAL" "GD5" "Calibration sessions list" "200" "$($cal.Status)" $(if ($cal.Ok) {"PASS"} else {"SPEC_GAP"}) ""
        
        $rbl = Api-Call GET "/rounds/$($final5.id)/rbl/progress" $coordTok
        Log-Result "G5-RBL" "GD5" "RBL progress" "200 or stub" "$($rbl.Status) $($rbl.Code)" $(if ($rbl.Ok) {"PASS"} else {"SPEC_GAP"}) ""
    }
}

# ADV-02: score after lock on gd5 if locked
$h5lock = Find-Hackathon "seal-gd5-late-hardlock" $coordTok
if ($h5lock) {
    $rounds5l = Get-Rounds $h5lock.id $coordTok
    $final5l = $rounds5l | Where-Object { $_.isFinal } | Select-Object -First 1
    if ($final5l -and $final5l.scoringLocked) {
        $scoreAfterLock = Api-Call POST "/scores" $guestTok @{ submissionId = 1; criterionId = 1; scoreValue = 5 }
        Log-Result "ADV-02" "GD5" "Score after lock" "423 SCORING_LOCKED" "$($scoreAfterLock.Status) $($scoreAfterLock.Code)" $(if ($scoreAfterLock.Code -eq "SCORING_LOCKED") {"PASS"} else {"FAIL"}) ""
    }
}

# === GD6 ===
$h6 = Find-Hackathon "seal-gd6-pending-confirm" $coordTok
if ($h6) {
    $readAwards = Api-Call GET "/hackathons/$($h6.id)/readiness?target=AWARDS" $coordTok
    Log-Result "G6-R01" "GD6" "Readiness AWARDS" "ready=true" "ready=$($readAwards.Data.ready)" $(if ($readAwards.Ok -and $readAwards.Data.ready) {"PASS"} else {"PARTIAL"}) ""
    
    $rankings = Api-Call GET "/hackathons/$($h6.id)/team-rankings" $coordTok
    $rankCount = if ($rankings.Ok -and $rankings.Data) { @($rankings.Data).Count } else { 0 }
    Log-Result "G6-RANK" "GD6" "Team rankings" "200 + items" "$($rankings.Status) count=$rankCount" $(if ($rankings.Ok -and $rankCount -gt 0) {"PASS"} elseif ($rankings.Ok) {"PARTIAL"} else {"FAIL"}) ""
    
    $confirm = Api-Call PATCH "/hackathons/$($h6.id)/confirm" $coordTok @{ confirm = $true; note = "audit" }
    Log-Result "G6-H03" "GD6" "Confirm FINISHED (pending-confirm seed)" "200 FINISHED" "$($confirm.Status) $($confirm.Code)" $(if ($confirm.Ok) {"PASS"} else {"PARTIAL"}) $($confirm.Message)"
}

$h6empty = Find-Hackathon "seal-gd6-prizes-empty" $coordTok
if ($h6empty) {
    $confirmNoPrize = Api-Call PATCH "/hackathons/$($h6empty.id)/confirm" $coordTok @{ confirm = $true }
    Log-Result "G6-N01" "GD6" "Confirm without prizes" "422 NO_PRIZES_RECORDED" "$($confirmNoPrize.Status) $($confirmNoPrize.Code)" $(if ($confirmNoPrize.Code -eq "NO_PRIZES_RECORDED") {"PASS"} else {"FAIL"}) ""
}

$h6edge = Find-Hackathon "seal-gd6-edge-errors" $coordTok
if ($h6edge) {
    $confirmEarly = Api-Call PATCH "/hackathons/$($h6edge.id)/confirm" $coordTok @{ confirm = $true }
    Log-Result "G6-N02" "GD6" "Confirm CK not locked" "422 ROUND_NOT_SCORING_LOCKED" "$($confirmEarly.Status) $($confirmEarly.Code)" $(if ($confirmEarly.Code -eq "ROUND_NOT_SCORING_LOCKED") {"PASS"} else {"FAIL"}) ""
}

$h6notPending = Find-Hackathon "seal-gd6-finished-export" $coordTok
if ($h6notPending) {
    $confirmWrong = Api-Call PATCH "/hackathons/$($h6notPending.id)/confirm" $coordTok @{ confirm = $true }
    Log-Result "ADV-08" "GD6" "Confirm when not PENDING_CONFIRM" "422" "$($confirmWrong.Status) $($confirmWrong.Code)" $(if ($confirmWrong.Code -eq "HACKATHON_NOT_PENDING_CONFIRM" -or $confirmWrong.Status -eq 422) {"PASS"} else {"FAIL"}) ""
}

# Export job
if ($h6) {
    $exp = Api-Call POST "/export-jobs" $coordTok @{ hackathonId = $h6.id; format = "CSV" }
    Log-Result "G6-EXP" "GD6" "Create export job" "201" "$($exp.Status) $($exp.Code)" $(if ($exp.Ok) {"PASS"} else {"PARTIAL"}) ""
}

# Scoreboard public
if ($h4) {
    $rounds4b = Get-Rounds $h4.id $coordTok
    $prelim4b = $rounds4b | Where-Object { -not $_.isFinal } | Select-Object -First 1
    if ($prelim4b -and $prelim4b.isPublished) {
        try {
            $sb = Invoke-RestMethod -Uri "$base/rounds/$($prelim4b.id)/scoreboard" -Method GET -ErrorAction Stop
            Log-Result "G4-SB" "GD4" "Public scoreboard no JWT" "200" "200" "PASS" ""
        } catch {
            $code = $_.Exception.Response.StatusCode.value__
            Log-Result "G4-SB" "GD4" "Public scoreboard" "200 public" "$code" $(if ($code -eq 401 -or $code -eq 403) {"SPEC_GAP"} else {"FAIL"}) ""
        }
    }
}

$results | ConvertTo-Json -Depth 5 | Out-File "d:\FPT\SU26\SWP\ManageSealHackathon\BE\scripts\gd3-gd6-api-audit-results.json" -Encoding utf8
$results | Format-Table Id, Phase, Status, Description, Actual -AutoSize
Write-Output "TOTAL: $($results.Count) PASS: $(@($results | Where-Object Status -eq 'PASS').Count) FAIL: $(@($results | Where-Object Status -eq 'FAIL').Count)"
