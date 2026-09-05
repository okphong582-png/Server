package com.funserver;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager implements Listener {
    private final FunServerCore plugin;
    private final SnakeHttpServer httpServer;
    private final ChessManager chessManager;
    private final Map<UUID, InGameSnakeSession> activeSnakeGames = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public static class InGameSnakeSession {
        public final Player player;
        public final Inventory inv;
        public final List<Integer> snake = new ArrayList<>();
        public int dirX = 1; // 1: right, -1: left, 0: vertical
        public int dirY = 0; // 1: down, -1: up, 0: horizontal
        public int appleSlot = 22;
        public int score = 0;
        public BukkitTask task;

        public InGameSnakeSession(Player player, Inventory inv) {
            this.player = player;
            this.inv = inv;
        }
    }

    public GameManager(FunServerCore plugin) {
        this.plugin = plugin;
        this.httpServer = new SnakeHttpServer(plugin);
        this.chessManager = new ChessManager(plugin);
        plugin.getServer().getPluginManager().registerEvents(chessManager, plugin);
    }

    public void shutdown() {
        if (httpServer != null) {
            httpServer.stopServer();
        }
        for (InGameSnakeSession s : activeSnakeGames.values()) {
            if (s.task != null) s.task.cancel();
        }
        activeSnakeGames.clear();
    }

    public ChessManager getChessManager() {
        return chessManager;
    }

    public void openGameMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "⭐ KHU TRÒ CHƠI MINI-GAMES ⭐");

        // Background
        ItemStack bg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, bg);

        // Slot 11: Rắn Săn Mồi (Chơi Ngay Trong Game)
        List<String> snakeLore = new ArrayList<>();
        snakeLore.add(ChatColor.YELLOW + "• Chơi trực tiếp ngay trên màn hình game!");
        snakeLore.add(ChatColor.YELLOW + "• Không cần thoát ra ngoài, không cần Alt-Tab.");
        snakeLore.add(ChatColor.RED + "• Có nút bấm [❌ ĐÓNG GAME] tiện lợi.");
        snakeLore.add(ChatColor.GREEN + "👉 Bấm vào đây để chơi ngay!");
        inv.setItem(11, createItemWithLore(Material.SLIME_BALL, ChatColor.GREEN + "" + ChatColor.BOLD + "🐍 RẮN SĂN MỒI (CHƠI NGAY)", snakeLore));

        // Slot 13: Cờ Vua 1v1
        List<String> chessLore = new ArrayList<>();
        chessLore.add(ChatColor.YELLOW + "• Đấu trí Cờ Vua 1v1 trực tiếp với bạn bè.");
        chessLore.add(ChatColor.YELLOW + "• Bàn cờ hiện ngay trong game, chơi thời gian thực.");
        chessLore.add(ChatColor.GOLD + "• Thắng nhận ngay: +500 Xu ⛃");
        chessLore.add(ChatColor.AQUA + "👉 Gõ: /covua <tên_bạn> để thách đấu!");
        inv.setItem(13, createItemWithLore(Material.TOTEM_OF_UNDYING, ChatColor.GOLD + "" + ChatColor.BOLD + "♟️ CỜ VUA 1V1 (ONLINE)", chessLore));

        // Slot 15: Game Web HTML5 (Tùy chọn)
        List<String> webLore = new ArrayList<>();
        webLore.add(ChatColor.YELLOW + "• Mở phiên bản đồ họa Web HTML5 Neon.");
        webLore.add(ChatColor.GRAY + "• Dành cho bạn nào thích chơi trên trình duyệt.");
        webLore.add(ChatColor.GREEN + "👉 Bấm để lấy link web!");
        inv.setItem(15, createItemWithLore(Material.COMPASS, ChatColor.AQUA + "" + ChatColor.BOLD + "🌐 BẢN WEB HTML5 (TÙY CHỌN)", webLore));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    public void launchHtmlSnake(Player player) {
        player.closeInventory();
        String url = "http://localhost:" + httpServer.getPort() + "/snake";

        player.sendMessage(ChatColor.GOLD + "========================================");
        player.sendMessage(ChatColor.GREEN + "🐍 LINK PHIÊN BẢN WEB HTML5 NEON:");
        player.sendMessage(ChatColor.AQUA + "" + ChatColor.UNDERLINE + url);
        player.sendMessage(ChatColor.GRAY + "(Mẹo: Bạn có thể gõ /game để chơi trực tiếp trong game không cần ra ngoài nhé!)");
        player.sendMessage(ChatColor.GOLD + "========================================");
    }

    public void startInGameSnake(Player player) {
        player.closeInventory();
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + "🐍 Rắn Săn Mồi: " + player.getName());
        InGameSnakeSession session = new InGameSnakeSession(player, inv);

        // Initial snake in center
        session.snake.add(20);
        session.snake.add(19);

        renderSnakeBoard(session);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

        // Game loop task (every 7 ticks = 0.35s)
        session.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(inv)) {
                session.task.cancel();
                activeSnakeGames.remove(player.getUniqueId());
                return;
            }

            int head = session.snake.get(0);
            int hr = head / 9;
            int hc = head % 9;

            int nr = hr + session.dirY;
            int nc = hc + session.dirX;

            // Wall collision (playable rows: 1..4, cols: 1..7)
            if (nr < 1 || nr > 4 || nc < 1 || nc > 7) {
                endSnakeGame(session);
                return;
            }

            int newHead = nr * 9 + nc;
            if (session.snake.contains(newHead)) {
                endSnakeGame(session);
                return;
            }

            session.snake.add(0, newHead);

            if (newHead == session.appleSlot) {
                session.score += 10;
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
                spawnApple(session);
            } else {
                session.snake.remove(session.snake.size() - 1);
            }

            renderSnakeBoard(session);
        }, 7L, 7L);

        activeSnakeGames.put(player.getUniqueId(), session);
    }

    private void spawnApple(InGameSnakeSession session) {
        int newApple;
        int attempts = 0;
        do {
            int r = 1 + random.nextInt(4);
            int c = 1 + random.nextInt(7);
            newApple = r * 9 + c;
            attempts++;
        } while (session.snake.contains(newApple) && attempts < 50);
        session.appleSlot = newApple;
    }

    private void renderSnakeBoard(InGameSnakeSession session) {
        Inventory inv = session.inv;
        inv.clear();

        ItemStack border = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, border);

        // Control buttons on bottom row
        inv.setItem(48, createItem(Material.ARROW, ChatColor.YELLOW + "" + ChatColor.BOLD + "⬅️ TRÁI"));
        inv.setItem(49, createItem(Material.ARROW, ChatColor.YELLOW + "" + ChatColor.BOLD + "⬇️ XUỐNG"));
        inv.setItem(50, createItem(Material.ARROW, ChatColor.YELLOW + "" + ChatColor.BOLD + "➡️ PHẢI"));
        inv.setItem(40, createItem(Material.ARROW, ChatColor.YELLOW + "" + ChatColor.BOLD + "⬆️ LÊN"));

        // BIG CLOSE BUTTON
        inv.setItem(53, createItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "❌ ĐÓNG GAME"));

        // Score display
        inv.setItem(45, createItem(Material.GOLD_INGOT, ChatColor.GOLD + "" + ChatColor.BOLD + "Điểm: " + ChatColor.YELLOW + session.score));

        // Draw Apple
        inv.setItem(session.appleSlot, createItem(Material.APPLE, ChatColor.RED + "" + ChatColor.BOLD + "🍎 TÁO ĐỎ"));

        // Draw Snake
        for (int i = 0; i < session.snake.size(); i++) {
            int slot = session.snake.get(i);
            if (i == 0) {
                inv.setItem(slot, createItem(Material.GREEN_STAINED_GLASS, ChatColor.GREEN + "" + ChatColor.BOLD + "🟢 ĐẦU RẮN"));
            } else {
                inv.setItem(slot, createItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "🟩 Thân"));
            }
        }
    }

    private void endSnakeGame(InGameSnakeSession session) {
        if (session.task != null) session.task.cancel();
        activeSnakeGames.remove(session.player.getUniqueId());
        session.player.closeInventory();

        session.player.playSound(session.player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        session.player.sendTitle(ChatColor.RED + "GAME OVER!", ChatColor.YELLOW + "Điểm đạt được: " + session.score, 10, 40, 10);

        if (session.score > 0) {
            long reward = session.score * 2;
            plugin.getEconomyManager().addCoins(session.player, reward);
            session.player.sendMessage(ChatColor.GREEN + "🎉 Bạn nhận được thưởng nóng: +" + reward + " Xu ⛃!");
        }
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        if (title.contains("KHU TRÒ CHƠI MINI-GAMES")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 11) {
                startInGameSnake(player);
            } else if (slot == 13) {
                player.closeInventory();
                chessManager.handleChallenge(player, new String[]{});
            } else if (slot == 15) {
                launchHtmlSnake(player);
            }
            return;
        }

        InGameSnakeSession snakeSession = activeSnakeGames.get(player.getUniqueId());
        if (snakeSession != null && event.getInventory().equals(snakeSession.inv)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            if (slot == 40 && snakeSession.dirY == 0) { // UP
                snakeSession.dirX = 0; snakeSession.dirY = -1;
                player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5f, 1.5f);
            } else if (slot == 49 && snakeSession.dirY == 0) { // DOWN
                snakeSession.dirX = 0; snakeSession.dirY = 1;
                player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5f, 1.5f);
            } else if (slot == 48 && snakeSession.dirX == 0) { // LEFT
                snakeSession.dirX = -1; snakeSession.dirY = 0;
                player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5f, 1.5f);
            } else if (slot == 50 && snakeSession.dirX == 0) { // RIGHT
                snakeSession.dirX = 1; snakeSession.dirY = 0;
                player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5f, 1.5f);
            } else if (slot == 53) {
                // CLOSE BUTTON PRESSED!
                if (snakeSession.task != null) snakeSession.task.cancel();
                activeSnakeGames.remove(player.getUniqueId());
                player.closeInventory();
                player.sendMessage(ChatColor.YELLOW + "Đã đóng game Rắn Săn Mồi thành công!");
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        InGameSnakeSession s = activeSnakeGames.remove(player.getUniqueId());
        if (s != null && s.task != null) {
            s.task.cancel();
        }
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItemWithLore(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
