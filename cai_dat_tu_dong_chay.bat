@echo off
chcp 65001 >nul
echo ================================================================
echo    CÀI ĐẶT MÁY CHỦ TỰ ĐỘNG CHẠY KHI BẬT MÁY TÍNH (AUTO-START)
echo ================================================================
echo.

set "SCRIPT_DIR=%~dp0"
set "TARGET_VBS=%SCRIPT_DIR%chay_ngam.vbs"
set "STARTUP_FOLDER=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup"
set "SHORTCUT_PATH=%STARTUP_FOLDER%\MinecraftServerAutoStart.lnk"

echo Đang tạo lối tắt khởi động ngầm cùng Windows...
powershell -Command "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut('%SHORTCUT_PATH%'); $s.TargetPath = 'wscript.exe'; $s.Arguments = '\"%TARGET_VBS%\"'; $s.WorkingDirectory = '%SCRIPT_DIR%'; $s.Save()"

if exist "%SHORTCUT_PATH%" (
    echo.
    echo [THÀNH CÔNG] Đã cài đặt tự động chạy thành công!
    echo Từ nay mỗi khi bạn mở máy tính, máy chủ Minecraft sẽ TỰ ĐỘNG CHẠY NGẦM.
    echo Bạn không cần phải mở bất kỳ cửa sổ màu đen nào nữa!
    echo.
) else (
    echo.
    echo [LỖI] Không thể tạo lối tắt trong thư mục Startup.
    echo.
)

pause
