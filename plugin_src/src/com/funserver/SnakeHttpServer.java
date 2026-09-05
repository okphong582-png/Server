package com.funserver;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class SnakeHttpServer {
    private final FunServerCore plugin;
    private HttpServer server;
    private final int port = 8088;

    // Chess room states for web chess
    public static final Map<String, String> chessRooms = new ConcurrentHashMap<>();

    public SnakeHttpServer(FunServerCore plugin) {
        this.plugin = plugin;
        startServer();
    }

    private void startServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newCachedThreadPool());

            // Route: Game Rắn Săn Mồi HTML5
            server.createContext("/snake", new SnakeHandler());

            // Route: Game Cờ Vua HTML5
            server.createContext("/chess", new ChessHandler());

            // Route: API đồng bộ nước đi cờ vua
            server.createContext("/api/chess", new ChessApiHandler());

            server.start();
            plugin.getLogger().info("Mini-Games Web Server da khoi dong tai http://localhost:" + port);
        } catch (IOException e) {
            plugin.getLogger().warning("Khong the khoi dong Web Server Mini-Games: " + e.getMessage());
        }
    }

    public void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    public int getPort() {
        return port;
    }

    static class SnakeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = """
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>🐍 Rắn Săn Mồi - HoangHa Network</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }
        body {
            background: radial-gradient(circle at center, #1a1f35 0%, #080a10 100%);
            color: #fff;
            font-family: 'Segoe UI', Roboto, sans-serif;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            overflow: hidden;
        }
        .header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            width: 100%;
            max-width: 520px;
            padding: 12px 16px;
            margin-bottom: 8px;
        }
        .title {
            font-size: 22px;
            font-weight: 800;
            background: linear-gradient(45deg, #00ff88, #00d2ff);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            text-shadow: 0 0 20px rgba(0,255,136,0.3);
        }
        .btn-close {
            background: #ff3366;
            color: #fff;
            border: none;
            padding: 8px 16px;
            border-radius: 8px;
            font-weight: 700;
            cursor: pointer;
            font-size: 14px;
            box-shadow: 0 4px 15px rgba(255,51,102,0.4);
            transition: all 0.2s ease;
        }
        .btn-close:hover {
            background: #ff1744;
            transform: scale(1.05);
        }
        .stats {
            display: flex;
            gap: 20px;
            font-size: 16px;
            font-weight: 600;
            margin-bottom: 10px;
        }
        .stat-badge {
            background: rgba(255,255,255,0.08);
            padding: 6px 14px;
            border-radius: 20px;
            border: 1px solid rgba(255,255,255,0.15);
        }
        .stat-badge span { color: #00ff88; }
        #canvas-container {
            position: relative;
            box-shadow: 0 0 35px rgba(0, 255, 136, 0.25);
            border-radius: 12px;
            border: 2px solid rgba(0, 255, 136, 0.4);
            background: #0d111a;
        }
        canvas {
            display: block;
            border-radius: 10px;
        }
        .controls-hint {
            margin-top: 14px;
            color: #8892b0;
            font-size: 13px;
            text-align: center;
        }
        .d-pad {
            display: grid;
            grid-template-columns: repeat(3, 50px);
            grid-template-rows: repeat(2, 50px);
            gap: 6px;
            margin-top: 12px;
        }
        .d-btn {
            background: rgba(255,255,255,0.1);
            border: 1px solid rgba(255,255,255,0.2);
            color: #fff;
            font-size: 20px;
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
        }
        .d-btn:active { background: rgba(0,255,136,0.4); }
    </style>
</head>
<body>
    <div class="header">
        <div class="title">🐍 RẮN SĂN MỒI</div>
        <button class="btn-close" onclick="closeGame()">❌ ĐÓNG GAME</button>
    </div>

    <div class="stats">
        <div class="stat-badge">Điểm: <span id="score">0</span></div>
        <div class="stat-badge">Kỷ Lục: <span id="highScore">0</span></div>
    </div>

    <div id="canvas-container">
        <canvas id="gameCanvas" width="400" height="400"></canvas>
    </div>

    <div class="controls-hint">
        🎮 Dùng các phím <b>Mũi tên (⬆️ ⬇️ ⬅️ ➡️)</b> hoặc <b>W A S D</b> để di chuyển
    </div>

    <div class="d-pad">
        <div></div>
        <div class="d-btn" onclick="changeDir('UP')">⬆️</div>
        <div></div>
        <div class="d-btn" onclick="changeDir('LEFT')">⬅️</div>
        <div class="d-btn" onclick="changeDir('DOWN')">⬇️</div>
        <div class="d-btn" onclick="changeDir('RIGHT')">➡️</div>
    </div>

    <script>
        function closeGame() {
            window.open('', '_self', '');
            window.close();
            document.body.innerHTML = `
                <div style="display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;background:#080a10;color:#fff;text-align:center;">
                    <h1 style="color:#00ff88;font-size:28px;margin-bottom:12px;">✅ ĐÃ ĐÓNG GAME THÀNH CÔNG!</h1>
                    <p style="color:#8892b0;font-size:16px;max-width:400px;line-height:1.6;">
                        Hãy bấm quay lại cửa sổ <b>Minecraft</b> để tiếp tục sinh tồn và chơi cùng bạn bè nhé!
                    </p>
                    <button onclick="location.reload()" style="margin-top:20px;padding:10px 24px;border-radius:8px;background:#00ff88;color:#000;font-weight:700;border:none;cursor:pointer;">Chơi Lại</button>
                </div>
            `;
        }

        const canvas = document.getElementById('gameCanvas');
        const ctx = canvas.getContext('2d');
        const grid = 20;
        let count = 0;
        let score = 0;
        let highScore = localStorage.getItem('snake_highscore') || 0;
        document.getElementById('highScore').innerText = highScore;

        let snake = {
            x: 160, y: 160,
            dx: grid, dy: 0,
            cells: [],
            maxCells: 4
        };
        let apple = { x: 320, y: 320 };

        function getRandomInt(min, max) {
            return Math.floor(Math.random() * (max - min)) + min;
        }

        function resetGame() {
            snake.x = 160;
            snake.y = 160;
            snake.cells = [];
            snake.maxCells = 4;
            snake.dx = grid;
            snake.dy = 0;
            score = 0;
            document.getElementById('score').innerText = score;
            apple.x = getRandomInt(0, 20) * grid;
            apple.y = getRandomInt(0, 20) * grid;
        }

        function loop() {
            requestAnimationFrame(loop);
            if (++count < 6) return; // 10 FPS
            count = 0;

            ctx.clearRect(0, 0, canvas.width, canvas.height);

            // Draw subtle grid
            ctx.strokeStyle = 'rgba(255,255,255,0.03)';
            for (let i = 0; i < canvas.width; i += grid) {
                ctx.beginPath();
                ctx.moveTo(i, 0); ctx.lineTo(i, canvas.height);
                ctx.moveTo(0, i); ctx.lineTo(canvas.width, i);
                ctx.stroke();
            }

            snake.x += snake.dx;
            snake.y += snake.dy;

            // Screen wrap
            if (snake.x < 0) snake.x = canvas.width - grid;
            else if (snake.x >= canvas.width) snake.x = 0;
            if (snake.y < 0) snake.y = canvas.height - grid;
            else if (snake.y >= canvas.height) snake.y = 0;

            snake.cells.unshift({ x: snake.x, y: snake.y });
            if (snake.cells.length > snake.maxCells) {
                snake.cells.pop();
            }

            // Draw Apple
            ctx.shadowBlur = 15;
            ctx.shadowColor = '#ff3366';
            ctx.fillStyle = '#ff3366';
            ctx.beginPath();
            ctx.arc(apple.x + grid/2, apple.y + grid/2, grid/2 - 2, 0, Math.PI * 2);
            ctx.fill();

            // Draw Snake
            ctx.shadowBlur = 12;
            ctx.shadowColor = '#00ff88';
            snake.cells.forEach((cell, index) => {
                ctx.fillStyle = index === 0 ? '#ffffff' : '#00ff88';
                ctx.fillRect(cell.x + 1, cell.y + 1, grid - 2, grid - 2);

                // Check self collision
                for (let i = index + 1; i < snake.cells.length; i++) {
                    if (cell.x === snake.cells[i].x && cell.y === snake.cells[i].y) {
                        resetGame();
                    }
                }
            });
            ctx.shadowBlur = 0;

            // Check eat apple
            if (snake.cells[0].x === apple.x && snake.cells[0].y === apple.y) {
                snake.maxCells++;
                score += 10;
                document.getElementById('score').innerText = score;
                if (score > highScore) {
                    highScore = score;
                    localStorage.setItem('snake_highscore', highScore);
                    document.getElementById('highScore').innerText = highScore;
                }
                apple.x = getRandomInt(0, 20) * grid;
                apple.y = getRandomInt(0, 20) * grid;
            }
        }

        function changeDir(dir) {
            if (dir === 'LEFT' && snake.dx === 0) { snake.dx = -grid; snake.dy = 0; }
            else if (dir === 'UP' && snake.dy === 0) { snake.dy = -grid; snake.dx = 0; }
            else if (dir === 'RIGHT' && snake.dx === 0) { snake.dx = grid; snake.dy = 0; }
            else if (dir === 'DOWN' && snake.dy === 0) { snake.dy = grid; snake.dx = 0; }
        }

        document.addEventListener('keydown', function (e) {
            if ((e.which === 37 || e.key === 'a' || e.key === 'A') && snake.dx === 0) changeDir('LEFT');
            else if ((e.which === 38 || e.key === 'w' || e.key === 'W') && snake.dy === 0) changeDir('UP');
            else if ((e.which === 39 || e.key === 'd' || e.key === 'D') && snake.dx === 0) changeDir('RIGHT');
            else if ((e.which === 40 || e.key === 's' || e.key === 'S') && snake.dy === 0) changeDir('DOWN');
        });

        requestAnimationFrame(loop);
    </script>
</body>
</html>
            """;
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ChessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = """
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>♟️ Cờ Vua Đối Kháng 1v1 - HoangHa Network</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            background: #0f141c;
            color: #fff;
            font-family: 'Segoe UI', Roboto, sans-serif;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            padding: 10px;
        }
        .header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            width: 100%;
            max-width: 500px;
            margin-bottom: 12px;
        }
        .title {
            font-size: 20px;
            font-weight: 800;
            color: #f1c40f;
        }
        .btn-close {
            background: #e74c3c;
            color: #fff;
            border: none;
            padding: 8px 16px;
            border-radius: 8px;
            font-weight: 700;
            cursor: pointer;
        }
        #board {
            display: grid;
            grid-template-columns: repeat(8, 55px);
            grid-template-rows: repeat(8, 55px);
            border: 4px solid #34495e;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: 0 10px 30px rgba(0,0,0,0.6);
        }
        .cell {
            width: 55px;
            height: 55px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 36px;
            cursor: pointer;
            user-select: none;
            transition: background 0.15s;
        }
        .white { background: #ecf0f1; color: #2c3e50; }
        .black { background: #7f8c8d; color: #111; }
        .selected { background: #f39c12 !important; }
        .highlight { background: #2ecc71 !important; }
        .turn-info {
            margin-top: 15px;
            font-size: 17px;
            font-weight: 700;
            color: #00ff88;
        }
    </style>
</head>
<body>
    <div class="header">
        <div class="title">♟️ CỜ VUA 1V1</div>
        <button class="btn-close" onclick="window.close()">❌ ĐÓNG</button>
    </div>

    <div id="board"></div>
    <div class="turn-info" id="turnInfo">Lượt đi: Quân Trắng ⚪</div>

    <script>
        const boardEl = document.getElementById('board');
        const turnInfo = document.getElementById('turnInfo');
        let currentTurn = 'W'; // W or B
        let selectedSquare = null;

        // Standard Chess Unicode
        const initialPieces = [
            ['♜','♞','♝','♛','♚','♝','♞','♜'],
            ['♟','♟','♟','♟','♟','♟','♟','♟'],
            ['','','','','','','',''],
            ['','','','','','','',''],
            ['','','','','','','',''],
            ['','','','','','','',''],
            ['♙','♙','♙','♙','♙','♙','♙','♙'],
            ['♖','♘','♗','♕','♔','♗','♘','♖']
        ];
        let boardState = JSON.parse(JSON.stringify(initialPieces));

        function isWhitePiece(p) { return '♙♖♘♗♕♔'.includes(p); }
        function isBlackPiece(p) { return '♟♜♞♝♛♚'.includes(p); }

        function renderBoard() {
            boardEl.innerHTML = '';
            for (let r = 0; r < 8; r++) {
                for (let c = 0; c < 8; c++) {
                    const cell = document.createElement('div');
                    cell.className = 'cell ' + ((r + c) % 2 === 0 ? 'white' : 'black');
                    cell.dataset.r = r;
                    cell.dataset.c = c;
                    cell.innerText = boardState[r][c];

                    cell.onclick = () => onCellClick(r, c);
                    boardEl.appendChild(cell);
                }
            }
        }

        function onCellClick(r, c) {
            const piece = boardState[r][c];
            if (selectedSquare) {
                const sr = selectedSquare.r;
                const sc = selectedSquare.c;
                const spiece = boardState[sr][sc];

                // Move piece
                if (r !== sr || c !== sc) {
                    boardState[r][c] = spiece;
                    boardState[sr][sc] = '';
                    currentTurn = currentTurn === 'W' ? 'B' : 'W';
                    turnInfo.innerText = currentTurn === 'W' ? 'Lượt đi: Quân Trắng ⚪' : 'Lượt đi: Quân Đen ⚫';
                }
                selectedSquare = null;
                renderBoard();
            } else {
                if (piece) {
                    if ((currentTurn === 'W' && isWhitePiece(piece)) || (currentTurn === 'B' && isBlackPiece(piece))) {
                        selectedSquare = { r, c };
                        renderBoard();
                        const idx = r * 8 + c;
                        boardEl.children[idx].classList.add('selected');
                    }
                }
            }
        }

        renderBoard();
    </script>
</body>
</html>
            """;
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ChessApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String resp = "{\"status\":\"ok\"}";
            byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
}
