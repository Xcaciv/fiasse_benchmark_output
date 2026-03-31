@echo off
setlocal
set "WORKDIR=G:\Generated\fiasse_benchmark_output\claude\node\securable"

echo === npm install === > "%WORKDIR%\check_output.txt" 2>&1
cd /d "%WORKDIR%" >> "%WORKDIR%\check_output.txt" 2>&1
npm install >> "%WORKDIR%\check_output.txt" 2>&1
echo npm install exit code: %ERRORLEVEL% >> "%WORKDIR%\check_output.txt" 2>&1

echo. >> "%WORKDIR%\check_output.txt" 2>&1
echo === security.js load check === >> "%WORKDIR%\check_output.txt" 2>&1
node -e "require('./src/config/security.js'); console.log('security ok')" >> "%WORKDIR%\check_output.txt" 2>&1
echo security.js exit code: %ERRORLEVEL% >> "%WORKDIR%\check_output.txt" 2>&1

echo. >> "%WORKDIR%\check_output.txt" 2>&1
echo === node --check src/app.js === >> "%WORKDIR%\check_output.txt" 2>&1
node --check src/app.js >> "%WORKDIR%\check_output.txt" 2>&1
echo app.js check exit code: %ERRORLEVEL% >> "%WORKDIR%\check_output.txt" 2>&1

echo. >> "%WORKDIR%\check_output.txt" 2>&1
echo === node --check src/server.js === >> "%WORKDIR%\check_output.txt" 2>&1
node --check src/server.js >> "%WORKDIR%\check_output.txt" 2>&1
echo server.js check exit code: %ERRORLEVEL% >> "%WORKDIR%\check_output.txt" 2>&1

echo. >> "%WORKDIR%\check_output.txt" 2>&1
echo === node --check src/routes/notes.js === >> "%WORKDIR%\check_output.txt" 2>&1
node --check src/routes/notes.js >> "%WORKDIR%\check_output.txt" 2>&1
echo notes.js check exit code: %ERRORLEVEL% >> "%WORKDIR%\check_output.txt" 2>&1
