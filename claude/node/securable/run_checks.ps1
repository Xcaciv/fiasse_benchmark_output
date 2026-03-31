$ErrorActionPreference = "Continue"
$workdir = "G:\Generated\fiasse_benchmark_output\claude\node\securable"
$outfile = "$workdir\check_output.txt"

Set-Location $workdir

"=== npm install ===" | Out-File $outfile -Encoding utf8
$result = & npm install 2>&1
$result | Out-File $outfile -Append -Encoding utf8
"npm install exit code: $LASTEXITCODE" | Out-File $outfile -Append -Encoding utf8

"" | Out-File $outfile -Append -Encoding utf8
"=== security.js load check ===" | Out-File $outfile -Append -Encoding utf8
$result = & node -e "require('./src/config/security.js'); console.log('security ok')" 2>&1
$result | Out-File $outfile -Append -Encoding utf8
"security.js exit code: $LASTEXITCODE" | Out-File $outfile -Append -Encoding utf8

"" | Out-File $outfile -Append -Encoding utf8
"=== node --check src/app.js ===" | Out-File $outfile -Append -Encoding utf8
$result = & node --check src/app.js 2>&1
$result | Out-File $outfile -Append -Encoding utf8
"app.js check exit code: $LASTEXITCODE" | Out-File $outfile -Append -Encoding utf8

"" | Out-File $outfile -Append -Encoding utf8
"=== node --check src/server.js ===" | Out-File $outfile -Append -Encoding utf8
$result = & node --check src/server.js 2>&1
$result | Out-File $outfile -Append -Encoding utf8
"server.js check exit code: $LASTEXITCODE" | Out-File $outfile -Append -Encoding utf8

"" | Out-File $outfile -Append -Encoding utf8
"=== node --check src/routes/notes.js ===" | Out-File $outfile -Append -Encoding utf8
$result = & node --check src/routes/notes.js 2>&1
$result | Out-File $outfile -Append -Encoding utf8
"notes.js check exit code: $LASTEXITCODE" | Out-File $outfile -Append -Encoding utf8

"=== DONE ===" | Out-File $outfile -Append -Encoding utf8
