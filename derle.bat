@echo off
chcp 65001 >nul
echo ========================================================
echo     Tam Ekran: Eksiksiz Ana Arayüz - Derleme Betiği
echo ========================================================
python "%~dp0build.py" %*
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [HATA] Derleme sirasinda hata olustu!
    pause
    exit /b %ERRORLEVEL%
)
echo.
pause
