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

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class ChessManager implements Listener {
    private final FunServerCore plugin;
    private final Map<UUID, UUID> pendingChallenges = new ConcurrentHashMap<>();
    private final Map<UUID, ChessSession> activeSessions = new ConcurrentHashMap<>();

    public static class ChessSession {
        public final Player white;
        public final Player black;
        public final Inventory inv;
        public boolean isWhiteTurn = true;
        public int selectedSlot = -1;
        // 6x6 board mapped into 54-slot chest
        // Rows: 0 to 5, Columns: 1 to 6 (slots: r*9 + c)
        public final int[] board = new int[54];

        public ChessSession(Player white, Player black, Inventory inv) {
            this.white = white;
            this.black = black;
            this.inv = inv;
            initBoard();
        }

        private void initBoard() {
            // Piece IDs:
            // 0: Empty, 1: W_Pawn, 2: W_Rook, 3: W_Knight, 4: W_Bishop, 5: W_Queen, 6: W_King
            // -1: B_Pawn, -2: B_Rook, -3: B_Knight, -4: B_Bishop, -5: B_Queen, -6: B_King

            // Black main row (r=0, c=1..6)
            board[1] = -2; board[2] = -3; board[3] = -4; board[4] = -5; board[5] = -6; board[6] = -2;
            // Black pawns (r=1, c=1..6)
            for (int c = 1; c <= 6; c++) board[9 + c] = -1;

            // White pawns (r=4, c=1..6)
            for (int c = 1; c <= 6; c++) board[36 + c] = 1;
            // White main row (r=5, c=1..6)
            board[46] = 2; board[47] = 3; board[48] = 4; board[49] = 5; board[50] = 6; board[51] = 2;
        }
    }

    public ChessManager(FunServerCore plugin) {
        this.plugin = plugin;
    }

    public void handleChallenge(Player sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "================= ♟️ CỜ VUA 1V1 =================");
            sender.sendMessage(ChatColor.YELLOW + "• Thách đấu người chơi: " + ChatColor.AQUA + "/covua <tên_bạn>");
            sender.sendMessage(ChatColor.YELLOW + "• Chấp nhận lời mời: " + ChatColor.GREEN + "/covua accept");
            sender.sendMessage(ChatColor.YELLOW + "• Đầu hàng / Hủy ván: " + ChatColor.RED + "/covua huy");
            sender.sendMessage(ChatColor.YELLOW + "• Link Cờ Vua Web: " + ChatColor.AQUA + "http://localhost:8088/chess");
            sender.sendMessage(ChatColor.GOLD + "================================================");
            return;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("accept")) {
            acceptChallenge(sender);
            return;
        }
        if (sub.equals("huy") || sub.equals("surrender")) {
            cancelGame(sender);
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Người chơi này không online!");
            return;
        }
        if (target.equals(sender)) {
            sender.sendMessage(ChatColor.RED + "Bạn không thể tự thách đấu chính mình!");
            return;
        }

        pendingChallenges.put(target.getUniqueId(), sender.getUniqueId());

        sender.sendMessage(ChatColor.GREEN + "Đã gửi lời mời thách đấu cờ vua đến " + ChatColor.YELLOW + target.getName() + "!");
        target.sendMessage(ChatColor.GOLD + "========================================");
        target.sendMessage(ChatColor.YELLOW + "♟️ " + ChatColor.AQUA + sender.getName() + ChatColor.YELLOW + " đã gửi lời thách đấu CỜ VUA 1v1 đến bạn!");
        target.sendMessage(ChatColor.GREEN + "Gõ " + ChatColor.BOLD + "/covua accept" + ChatColor.GREEN + " để vào bàn cờ chiến ngay!");
        target.sendMessage(ChatColor.GOLD + "========================================");
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.2f);
    }

    public void acceptChallenge(Player target) {
        UUID challengerId = pendingChallenges.remove(target.getUniqueId());
        if (challengerId == null) {
            target.sendMessage(ChatColor.RED + "Bạn không có lời mời thách đấu nào!");
            return;
        }

        Player challenger = Bukkit.getPlayer(challengerId);
        if (challenger == null || !challenger.isOnline()) {
            target.sendMessage(ChatColor.RED + "Người thách đấu đã rời mạng!");
            return;
        }

        startChessGame(challenger, target);
    }

    public void startChessGame(Player white, Player black) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_BLUE + "♟ Cờ Vua: " + white.getName() + " vs " + black.getName());
        ChessSession session = new ChessSession(white, black, inv);
        activeSessions.put(white.getUniqueId(), session);
        activeSessions.put(black.getUniqueId(), session);

        renderBoard(session);
        white.openInventory(inv);
        black.openInventory(inv);

        white.sendMessage(ChatColor.GREEN + "Ván cờ vua bắt đầu! Bạn cầm quân TRẮNG (đi trước).");
        black.sendMessage(ChatColor.YELLOW + "Ván cờ vua bắt đầu! Bạn cầm quân ĐEN (đi sau).");
    }

    private void renderBoard(ChessSession session) {
        Inventory inv = session.inv;
        inv.clear();

        // Background / Border
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            int c = i % 9;
            if (c < 1 || c > 6) {
                inv.setItem(i, border);
            }
        }

        // Side info buttons
        inv.setItem(0, createItem(Material.PLAYER_HEAD, ChatColor.WHITE + "⚪ Quân Trắng: " + session.white.getName()));
        inv.setItem(8, createItem(Material.WITHER_SKELETON_SKULL, ChatColor.DARK_GRAY + "⚫ Quân Đen: " + session.black.getName()));
        inv.setItem(26, createItem(session.isWhiteTurn ? Material.WHITE_CONCRETE : Material.BLACK_CONCRETE,
                session.isWhiteTurn ? ChatColor.GREEN + "Lượt đi: Quân Trắng ⚪" : ChatColor.YELLOW + "Lượt đi: Quân Đen ⚫"));
        inv.setItem(53, createItem(Material.BARRIER, ChatColor.RED + "❌ Đầu Hàng / Thoát"));

        // Render 6x6 Board squares
        for (int r = 0; r < 6; r++) {
            for (int c = 1; c <= 6; c++) {
                int slot = r * 9 + c;
                int piece = session.board[slot];

                if (slot == session.selectedSlot) {
                    inv.setItem(slot, createPieceItem(piece, true));
                } else if (piece != 0) {
                    inv.setItem(slot, createPieceItem(piece, false));
                } else {
                    // Empty square checkered
                    Material tile = ((r + c) % 2 == 0) ? Material.WHITE_CARPET : Material.BLACK_CARPET;
                    inv.setItem(slot, createItem(tile, ChatColor.GRAY + "Ô trống"));
                }
            }
        }
    }

    private ItemStack createPieceItem(int piece, boolean selected) {
        String prefix = selected ? ChatColor.GOLD + "[CHỌN] " : "";
        switch (piece) {
            case 1: return createItem(Material.IRON_INGOT, prefix + ChatColor.WHITE + "Tốt Trắng");
            case 2: return createItem(Material.IRON_BLOCK, prefix + ChatColor.WHITE + "Xe Trắng");
            case 3: return createItem(Material.IRON_HORSE_ARMOR, prefix + ChatColor.WHITE + "Mã Trắng");
            case 4: return createItem(Material.AMETHYST_SHARD, prefix + ChatColor.WHITE + "Tượng Trắng");
            case 5: return createItem(Material.NETHER_STAR, prefix + ChatColor.WHITE + "Hậu Trắng");
            case 6: return createItem(Material.TOTEM_OF_UNDYING, prefix + ChatColor.YELLOW + "" + ChatColor.BOLD + "VUA TRẮNG 👑");

            case -1: return createItem(Material.NETHERITE_INGOT, prefix + ChatColor.DARK_GRAY + "Tốt Đen");
            case -2: return createItem(Material.OBSIDIAN, prefix + ChatColor.DARK_GRAY + "Xe Đen");
            case -3: return createItem(Material.DIAMOND_HORSE_ARMOR, prefix + ChatColor.DARK_GRAY + "Mã Đen");
            case -4: return createItem(Material.ECHO_SHARD, prefix + ChatColor.DARK_GRAY + "Tượng Đen");
            case -5: return createItem(Material.NETHERITE_SCRAP, prefix + ChatColor.DARK_GRAY + "Hậu Đen");
            case -6: return createItem(Material.WITHER_SKELETON_SKULL, prefix + ChatColor.RED + "" + ChatColor.BOLD + "VUA ĐEN 👑");
            default: return new ItemStack(Material.AIR);
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

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ChessSession session = activeSessions.get(player.getUniqueId());
        if (session == null || !event.getInventory().equals(session.inv)) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        // Surrender button
        if (slot == 53) {
            player.closeInventory();
            endGame(session, player.equals(session.white) ? session.black : session.white, player);
            return;
        }

        int c = slot % 9;
        if (c < 1 || c > 6) return; // Outside 6x6 board

        boolean isWhite = player.equals(session.white);
        if (isWhite != session.isWhiteTurn) {
            player.sendMessage(ChatColor.RED + "Chưa tới lượt của bạn!");
            return;
        }

        int piece = session.board[slot];

        if (session.selectedSlot == -1) {
            // Select piece
            if ((session.isWhiteTurn && piece > 0) || (!session.isWhiteTurn && piece < 0)) {
                session.selectedSlot = slot;
                player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1.0f, 1.5f);
                renderBoard(session);
            }
        } else {
            // Move piece to target slot
            int fromSlot = session.selectedSlot;
            int selectedPiece = session.board[fromSlot];

            if (slot == fromSlot) {
                // Deselect
                session.selectedSlot = -1;
                renderBoard(session);
                return;
            }

            // Can't eat own piece
            if ((session.isWhiteTurn && piece > 0) || (!session.isWhiteTurn && piece < 0)) {
                session.selectedSlot = slot; // Switch selected piece
                renderBoard(session);
                return;
            }

            // Execute move!
            boolean kingCaptured = Math.abs(piece) == 6;
            session.board[slot] = selectedPiece;
            session.board[fromSlot] = 0;
            session.selectedSlot = -1;
            session.isWhiteTurn = !session.isWhiteTurn;

            session.white.playSound(session.white.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.2f);
            session.black.playSound(session.black.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.2f);

            renderBoard(session);

            if (kingCaptured) {
                endGame(session, player, isWhite ? session.black : session.white);
            }
        }
    }

    private void endGame(ChessSession session, Player winner, Player loser) {
        activeSessions.remove(session.white.getUniqueId());
        activeSessions.remove(session.black.getUniqueId());

        session.white.closeInventory();
        session.black.closeInventory();

        plugin.getEconomyManager().addCoins(winner, 500);

        winner.sendTitle(ChatColor.GOLD + "👑 BẠN ĐÃ CHIẾN THẮNG!", ChatColor.YELLOW + "Thưởng nóng: +500 Xu!", 10, 50, 15);
        loser.sendTitle(ChatColor.RED + "BẠN ĐÃ THUA CUỘC!", ChatColor.GRAY + "Hãy cố gắng ở ván sau!", 10, 50, 15);

        Bukkit.broadcastMessage(ChatColor.GOLD + "========================================");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "🎉 " + ChatColor.AQUA + winner.getName() +
                ChatColor.YELLOW + " đã xuất sắc chiến thắng " + ChatColor.RED + loser.getName() +
                ChatColor.YELLOW + " trong ván CỜ VUA đỉnh cao!");
        Bukkit.broadcastMessage(ChatColor.GOLD + "========================================");
    }

    public void cancelGame(Player player) {
        ChessSession session = activeSessions.get(player.getUniqueId());
        if (session != null) {
            endGame(session, player.equals(session.white) ? session.black : session.white, player);
        } else {
            player.sendMessage(ChatColor.RED + "Bạn không ở trong ván cờ nào!");
        }
    }
}
