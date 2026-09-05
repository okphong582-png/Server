package com.funserver;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public class FunServerCore extends JavaPlugin {
    private static FunServerCore instance;
    private AuthManager authManager;
    private LobbyManager lobbyManager;
    private ArenaManager arenaManager;
    private EconomyManager economyManager;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        instance = this;

        try {
            saveDefaultConfig();
        } catch (Exception e) {
            getLogger().warning("Khong the luu config mac dinh: " + e.getMessage());
        }

        authManager = new AuthManager(this);
        lobbyManager = new LobbyManager(this);
        arenaManager = new ArenaManager(this);
        economyManager = new EconomyManager(this);
        gameManager = new GameManager(this);

        getServer().getPluginManager().registerEvents(authManager, this);
        getServer().getPluginManager().registerEvents(lobbyManager, this);
        getServer().getPluginManager().registerEvents(arenaManager, this);
        getServer().getPluginManager().registerEvents(economyManager, this);
        getServer().getPluginManager().registerEvents(gameManager, this);

        getLogger().info("=================================================");
        getLogger().info("  FunServerCore v1.0.0 da kich hoat thanh cong!");
        getLogger().info("  Mini-Games + Chess + Snake In-Game: READY!");
        getLogger().info("=================================================");
    }

    @Override
    public void onDisable() {
        if (authManager != null) {
            authManager.shutdown();
        }
        if (economyManager != null) {
            economyManager.shutdown();
        }
        if (gameManager != null) {
            gameManager.shutdown();
        }
        getLogger().info("FunServerCore da dung hoat dong an toan.");
    }

    public static FunServerCore getInstance() {
        return instance;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();

        // --- LỆNH XÁC THỰC TÀI KHOẢN (AI CŨNG DÙNG ĐƯỢC) ---
        if (name.equals("dangki") || name.equals("register")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Chỉ người chơi trong game mới dùng được lệnh này!");
                return true;
            }
            authManager.handleRegister((Player) sender, args);
            return true;
        }

        if (name.equals("dangnhap") || name.equals("login")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Chỉ người chơi trong game mới dùng được lệnh này!");
                return true;
            }
            authManager.handleLogin((Player) sender, args);
            return true;
        }

        if (name.equals("doimk")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Chỉ người chơi trong game mới dùng được lệnh này!");
                return true;
            }
            authManager.handleChangePassword((Player) sender, args);
            return true;
        }

        // --- LỆNH MINI-GAMES & CỜ VUA (/game, /covua) ---
        if (name.equals("game") || name.equals("minigame") || name.equals("trochoi")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Chỉ người chơi trong game mới dùng được lệnh này!");
                return true;
            }
            if (args.length > 0 && (args[0].equalsIgnoreCase("ran") || args[0].equalsIgnoreCase("snake"))) {
                // Mở game Rắn trực tiếp ngay trên màn hình game
                gameManager.startInGameSnake((Player) sender);
            } else {
                gameManager.openGameMenu((Player) sender);
            }
            return true;
        }

        if (name.equals("covua") || name.equals("chess")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Chỉ người chơi trong game mới dùng được lệnh này!");
                return true;
            }
            gameManager.getChessManager().handleChallenge((Player) sender, args);
            return true;
        }

        // --- LỆNH KHU VỰC VÀ TIỆN ÍCH DÀNH CHO TẤT CẢ NGƯỜI CHƠI ---
        if (name.equals("lobby") || name.equals("spawn")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Chỉ người chơi trong game mới dùng được lệnh này!");
                return true;
            }
            lobbyManager.teleportToLobby((Player) sender);
            return true;
        }

        if (name.equals("pvp")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Chỉ người chơi trong game mới dùng được lệnh này!");
                return true;
            }
            arenaManager.teleportToPvP((Player) sender);
            return true;
        }

        if (name.equals("mine")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Chỉ người chơi trong game mới dùng được lệnh này!");
                return true;
            }
            arenaManager.teleportToMine((Player) sender);
            return true;
        }

        if (name.equals("sinhton") || name.equals("rtp")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Chỉ người chơi trong game mới dùng được lệnh này!");
                return true;
            }
            arenaManager.randomTeleportSurvival((Player) sender);
            return true;
        }

        if (name.equals("ban") || name.equals("sell")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Chỉ người chơi trong game mới dùng được lệnh này!");
                return true;
            }
            economyManager.handleSell((Player) sender);
            return true;
        }

        if (name.equals("mua") || name.equals("shop")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Chỉ người chơi trong game mới dùng được lệnh này!");
                return true;
            }
            economyManager.handleShop((Player) sender, args);
            return true;
        }

        if (name.equals("tien") || name.equals("money")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Chỉ người chơi trong game mới dùng được lệnh này!");
                return true;
            }
            Player p = (Player) sender;
            EconomyManager.PlayerData d = economyManager.getData(p);
            p.sendMessage(ChatColor.GOLD + "Số dư hiện tại của bạn: " + ChatColor.YELLOW + d.coins + " Xu ⛃" +
                    ChatColor.GRAY + " (Cấp: Lv." + d.level + ")");
            return true;
        }

        // --- LỆNH DÀNH RIÊNG CHO QUẢN TRỊ VIÊN (ADMIN / OP) ---
        if (name.equals("setlobby")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Chỉ người chơi trong game mới dùng được lệnh này!");
                return true;
            }
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "✖ Bạn không có quyền dùng lệnh quản trị này!");
                return true;
            }
            lobbyManager.setLobby((Player) sender);
            return true;
        }

        if (name.equals("setpvp")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Chỉ người chơi trong game mới dùng được lệnh này!");
                return true;
            }
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "✖ Bạn không có quyền dùng lệnh quản trị này!");
                return true;
            }
            arenaManager.setPvP((Player) sender);
            return true;
        }

        if (name.equals("setmine")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Chỉ người chơi trong game mới dùng được lệnh này!");
                return true;
            }
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "✖ Bạn không có quyền dùng lệnh quản trị này!");
                return true;
            }
            arenaManager.setMine((Player) sender);
            return true;
        }

        if (name.equals("build")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Chỉ người chơi trong game mới dùng được lệnh này!");
                return true;
            }
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "✖ Bạn không có quyền dùng lệnh quản trị này!");
                return true;
            }
            lobbyManager.toggleBuildMode((Player) sender);
            return true;
        }

        if (name.equals("congtien")) {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "✖ Bạn không có quyền dùng lệnh quản trị này!");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Cú pháp: /congtien <tên_người_chơi> <số_tiền>");
                return true;
            }
            Player target = getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Không tìm thấy người chơi này!");
                return true;
            }
            try {
                long amount = Long.parseLong(args[1]);
                economyManager.addCoins(target, amount);
                sender.sendMessage(ChatColor.GREEN + "Đã cộng " + amount + " Xu cho " + target.getName());
                target.sendMessage(ChatColor.GOLD + "Bạn vừa được Admin cộng " + ChatColor.YELLOW + amount + " Xu ⛃!");
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Số tiền không hợp lệ!");
            }
            return true;
        }

        return false;
    }
}
