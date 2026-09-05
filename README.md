# 🌟 HOANGHA MINECRAFT 1.21.4 SERVER (CROSS-PLAY + MINI-GAMES + FIREBASE)

Máy chủ Minecraft Paper 1.21.4 sinh tồn đa tính năng hỗ trợ cả máy tính (PC Java) và điện thoại (Minecraft PE / Bedrock).

---

## ✨ Tính Năng Nổi Bật

1. **🔐 Xác Thực Tài Khoản Bằng Firebase Realtime Database:**
   - Người chơi mới vào bắt buộc đăng ký/đăng nhập.
   - Mật khẩu được mã hóa SHA-256 an toàn tuyệt đối.
   - Lệnh: `/dangki <mật_khẩu>`, `/dangnhap <mật_khẩu>`, `/doimk <cũ> <mới>`.

2. **📊 Giao Diện HUD HoangHa & Action Bar:**
   - Scoreboard góc phải: Tên máy chủ, Số dư xu `💰`, Cấp độ & Exp `🏆`, Số mạng hạ gục `⚔`, Địa chỉ IP máy chủ.
   - Action Bar trên thanh máu liên tục cập nhật lấp lánh thời gian thực.

3. **🎮 Khu Trò Chơi Mini-Games Độc Quyền (`/game`):**
   - 🐍 **Rắn Săn Mồi Trực Tiếp Trên Màn Hình (`/game ran`):**
     - Màn hình Arcade hiện thẳng trong game không cần Alt-Tab.
     - Rắn bò ăn táo tính điểm, nhận thưởng xu.
     - Có sẵn nút to rõ **`[❌ ĐÓNG GAME]`** (hoặc bấm `ESC`) để quay lại thế giới game bất cứ lúc nào!
   - ♟️ **Cờ Vua Đối Kháng 1v1 (`/covua <tên_bạn>`):**
     - Thách đấu người chơi trong server, mở bàn cờ tương tác 2 người thời gian thực.
     - Người chiến thắng được nhận thưởng nóng **+500 Xu ⛃**, bắn pháo hoa và vinh danh toàn server!

4. **⛏ Mỏ Khoáng Sản (Mine) & Kinh Tế Xu:**
   - Lệnh `/mine`: Đào quặng tăng Exp, rơi xu may mắn 40% (+2 đến +70 xu).
   - Lệnh `/ban`: Bán toàn bộ quặng và đá cuội lấy tiền xu.
   - Lệnh `/mua`: Cửa hàng mua thức ăn, cúp, kiếm, ngọc.
   - Lệnh `/tien`: Xem số dư tài khoản.

5. **🌐 Hỗ Trợ Chơi Chung PC & Điện Thoại PE (Geyser + Floodgate + Playit.gg):**
   - Người chơi điện thoại (Minecraft PE / Bedrock) có thể vào chơi chung mượt mà với người chơi máy tính.

---

## 🚀 Cách Chạy Máy Chủ

### Cách 1: Chạy thủ công
Nhấp đúp chuột vào file **`start_server.bat`**.

### Cách 2: Tự động chạy ngầm không cần bật
Nhấp đúp chuột vào file **`cai_dat_tu_dong_chay.bat`** để máy tính tự động bật máy chủ chạy ngầm mỗi khi mở máy!

### Cách 3: Chạy trên GitHub Actions (Cloud 24/7)
Vào tab **Actions** trên GitHub ➔ Chọn workflow **Run Minecraft Server** ➔ Bấm **Run workflow**.

---

## 📜 Danh Sách Lệnh (Commands)
- `/game` : Mở menu chọn trò chơi Mini-games.
- `/game ran` : Mở ngay Rắn Săn Mồi trên màn hình game.
- `/covua <tên>` : Thách đấu Cờ Vua 1v1.
- `/covua accept` : Chấp nhận thách đấu cờ vua.
- `/covua huy` : Đầu hàng / Hủy ván cờ.
- `/lobby` : Về sảnh chờ an toàn.
- `/pvp` : Vào đấu trường PvP.
- `/mine` : Vào khu đào mỏ.
- `/sinhton` : Dịch chuyển ngẫu nhiên sinh tồn.
- `/ban` : Bán quặng đổi tiền xu.
- `/mua` : Mở cửa hàng.
- `/tien` : Xem số dư tiền xu.
- `/setlobby`, `/setpvp`, `/setmine`, `/build`, `/congtien` : Lệnh quản trị viên (Admin/OP).
