@echo off
chcp 65001 >nul
echo ================================================================
echo    HỦY CÀI ĐẶT TỰ ĐỘNG CHẠY MÁY CHỦ (DISABLE AUTO-START)
echo ================================================================
echo.

set "STARTUP_FOLDER=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup"
set "SHORTCUT_PATH=%STARTUP_FOLDER%\MinecraftServerAutoStart.lnk"

if exist "%SHORTCUT_PATH%" (
    del /f /q "%SHORTCUT_PATH%"
    echo [THÀNH CÔNG] Đã xóa lối tắt tự động chạy!
    echo Máy chủ sẽ không tự động bật khi khởi động máy tính nữa.
) else (
    echo [THÔNG BÁO] Chưa cài đặt tự động chạy trước đó.
)

pause
