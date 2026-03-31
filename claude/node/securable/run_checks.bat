@echo off
cd /d "G:\Generated\fiasse_benchmark_output\claude\node\securable"

echo === npm install ===
call npm install 2>&1
echo npm install exit code: %ERRORLEVEL%

echo.
echo === security.js load check ===
node -e "require('./src/config/security.js'); console.log('security ok')" 2>&1
echo security.js exit code: %ERRORLEVEL%

echo.
echo === node --check src/app.js ===
node --check src/app.js 2>&1
echo app.js check exit code: %ERRORLEVEL%

echo.
echo === node --check src/server.js ===
node --check src/server.js 2>&1
echo server.js check exit code: %ERRORLEVEL%

echo.
echo === node --check src/routes/notes.js ===
node --check src/routes/notes.js 2>&1
echo notes.js check exit code: %ERRORLEVEL%
