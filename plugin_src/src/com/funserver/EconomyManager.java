package com.funserver;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyManager implements Listener {
    private final FunServerCore plugin;
    private final Map<UUID, PlayerData> dataMap = new ConcurrentHashMap<>();
    private final File dataFile;
    private final YamlConfiguration dataConfig;
    private final Random random = new Random();
    private BukkitTask hudTask;

    public static class PlayerData {
        public long coins = 500;
        public int level = 1;
        public int exp = 0;
        public int kills = 0;
    }

    public EconomyManager(FunServerCore plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadAllData();
        startHudTask();
    }

    private void loadAllData() {
        for (String key : dataConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerData data = new PlayerData();
                data.coins = dataConfig.getLong(key + ".coins", 500);
                data.level = dataConfig.getInt(key + ".level", 1);
                data.exp = dataConfig.getInt(key + ".exp", 0);
                data.kills = dataConfig.getInt(key + ".kills", 0);
                dataMap.put(uuid, data);
            } catch (Exception ignored) {}
        }
    }

    public void saveData(UUID uuid) {
        PlayerData data = dataMap.get(uuid);
        if (data != null) {
            String key = uuid.toString();
            dataConfig.set(key + ".coins", data.coins);
            dataConfig.set(key + ".level", data.level);
            dataConfig.set(key + ".exp", data.exp);
            dataConfig.set(key + ".kills", data.kills);
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Khong the luu playerdata.yml: " + e.getMessage());
            }
        }
    }

    public void saveAll() {
        for (UUID uuid : dataMap.keySet()) {
            saveData(uuid);
        }
    }

    public PlayerData getData(Player player) {
        return dataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerData());
    }

    public void addCoins(Player player, long amount) {
        PlayerData data = getData(player);
        data.coins += amount;
        saveData(player.getUniqueId());
        updateScoreboard(player);
    }

    public boolean removeCoins(Player player, long amount) {
        PlayerData data = getData(player);
        if (data.coins >= amount) {
            data.coins -= amount;
            saveData(player.getUniqueId());
            updateScoreboard(player);
            return true;
        }
        return false;
    }

    public void addExp(Player player, int amount) {
        PlayerData data = getData(player);
        data.exp += amount;
        int maxExp = data.level * 100;

        if (data.exp >= maxExp) {
            data.exp -= maxExp;
            data.level++;
            data.coins += 200;
            saveData(player.getUniqueId());

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            spawnFireworks(player);
            player.sendTitle(ChatColor.GOLD + "⭐ LÊN CẤP " + data.level + "! ⭐",
                    ChatColor.YELLOW + "Thưởng nóng: +200 Xu!", 10, 50, 15);
            plugin.getServer().broadcastMessage(ChatColor.GREEN + "🎉 " + ChatColor.YELLOW + player.getName() +
                    ChatColor.AQUA + " đã thăng cấp lên " + ChatColor.GOLD + "Cấp độ " + data.level + "!");
        }
        updateScoreboard(player);
    }

    private void spawnFireworks(Player player) {
        try {
            Firework fw = player.getWorld().spawn(player.getLocation(), Firework.class);
            FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .withColor(Color.YELLOW, Color.ORANGE, Color.AQUA)
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withFlicker()
                    .build());
            meta.setPower(1);
            fw.setFireworkMeta(meta);
        } catch (Exception ignored) {}
    }

    private void startHudTask() {
        hudTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (plugin.getAuthManager().isAuthenticated(player)) {
                    PlayerData data = getData(player);

                    String actionBar = ChatColor.GOLD + "" + ChatColor.BOLD + "XU: " + ChatColor.YELLOW + data.coins + "$ "
                            + ChatColor.DARK_GRAY + " | "
                            + ChatColor.AQUA + "" + ChatColor.BOLD + "CẤP: " + ChatColor.GREEN + "Lv." + data.level
                            + ChatColor.DARK_GRAY + " | "
                            + ChatColor.RED + "" + ChatColor.BOLD + "PVP: " + ChatColor.WHITE + data.kills + " Kills";

                    player.sendActionBar(actionBar);
                    updateScoreboard(player);
                }
            }
        }, 20L, 20L);
    }

    public void shutdown() {
        if (hudTask != null) {
            hudTask.cancel();
        }
        saveAll();
    }

    public void updateScoreboard(Player player) {
        ScoreboardManager manager = plugin.getServer().getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = player.getScoreboard();
        if (board.equals(manager.getMainScoreboard())) {
            board = manager.getNewScoreboard();
            player.setScoreboard(board);
        }

        Objective obj = board.getObjective("hoangha_hud");
        if (obj == null) {
            obj = board.registerNewObjective("hoangha_hud", Criteria.DUMMY,
                    ChatColor.YELLOW + "" + ChatColor.BOLD + "⭐ HOANGHA NETWORK ⭐");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        PlayerData data = getData(player);
        int maxExp = data.level * 100;

        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        obj.getScore(ChatColor.GRAY + "---------------------").setScore(7);
        obj.getScore(ChatColor.WHITE + "👤 Tên: " + ChatColor.GREEN + player.getName()).setScore(6);
        obj.getScore(ChatColor.WHITE + "💰 Tiền xu: " + ChatColor.GOLD + data.coins + " ⛃").setScore(5);
        obj.getScore(ChatColor.WHITE + "🏆 Cấp độ: " + ChatColor.AQUA + "Lv." + data.level + ChatColor.GRAY + " (" + data.exp + "/" + maxExp + ")").setScore(4);
        obj.getScore(ChatColor.WHITE + "⚔ Hạ gục: " + ChatColor.RED + data.kills).setScore(3);
        obj.getScore(ChatColor.DARK_GRAY + "--------------------").setScore(2);
        obj.getScore(ChatColor.YELLOW + "🌐 cynthia-brings.tun.ply.gg").setScore(1);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updateScoreboard(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        saveData(event.getPlayer().getUniqueId());
        dataMap.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMine(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getAuthManager().isAuthenticated(player)) return;

        Material type = event.getBlock().getType();
        String name = type.name();

        int expEarned = 0;
        int minCoin = 0;
        int maxCoin = 0;

        if (name.contains("COAL_ORE")) {
            expEarned = 3; minCoin = 2; maxCoin = 6;
        } else if (name.contains("COPPER_ORE")) {
            expEarned = 4; minCoin = 3; maxCoin = 8;
        } else if (name.contains("IRON_ORE")) {
            expEarned = 7; minCoin = 5; maxCoin = 12;
        } else if (name.contains("GOLD_ORE")) {
            expEarned = 12; minCoin = 10; maxCoin = 20;
        } else if (name.contains("REDSTONE_ORE") || name.contains("LAPIS_ORE")) {
            expEarned = 8; minCoin = 6; maxCoin = 14;
        } else if (name.contains("DIAMOND_ORE")) {
            expEarned = 25; minCoin = 25; maxCoin = 50;
        } else if (name.contains("EMERALD_ORE")) {
            expEarned = 35; minCoin = 35; maxCoin = 70;
        } else if (name.contains("NETHER_QUARTZ_ORE")) {
            expEarned = 6; minCoin = 4; maxCoin = 10;
        } else if (name.contains("ANCIENT_DEBRIS")) {
            expEarned = 60; minCoin = 80; maxCoin = 150;
        } else if (name.contains("STONE") || name.contains("COBBLESTONE")) {
            expEarned = 1;
        }

        if (expEarned > 0) {
            addExp(player, expEarned);
        }

        if (maxCoin > 0 && random.nextInt(100) < 40) {
            int luckyCoin = minCoin + random.nextInt(maxCoin - minCoin + 1);
            addCoins(player, luckyCoin);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
            player.sendActionBar(ChatColor.GOLD + "+ " + luckyCoin + " Xu May Mắn! ⭐");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPvPKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && !killer.equals(victim)) {
            PlayerData kData = getData(killer);
            kData.kills++;
            kData.coins += 60;
            saveData(killer.getUniqueId());

            killer.sendMessage(ChatColor.GREEN + "⚔ Bạn đã hạ gục " + victim.getName() + " và nhận được +60 Xu!");
            updateScoreboard(killer);
        }
    }

    public void handleSell(Player player) {
        long totalEarned = 0;
        int itemsSold = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;

            Material mat = item.getType();
            int amount = item.getAmount();
            long pricePerItem = 0;

            switch (mat) {
                case COAL: pricePerItem = 5; break;
                case RAW_COPPER:
                case COPPER_INGOT: pricePerItem = 8; break;
                case RAW_IRON:
                case IRON_INGOT: pricePerItem = 15; break;
                case RAW_GOLD:
                case GOLD_INGOT: pricePerItem = 30; break;
                case REDSTONE:
                case LAPIS_LAZULI: pricePerItem = 6; break;
                case QUARTZ: pricePerItem = 10; break;
                case DIAMOND: pricePerItem = 100; break;
                case EMERALD: pricePerItem = 150; break;
                case NETHERITE_INGOT: pricePerItem = 1000; break;
                case COBBLESTONE:
                case COBBLED_DEEPSLATE:
                case STONE: pricePerItem = 1; break;
                default: break;
            }

            if (pricePerItem > 0) {
                totalEarned += (pricePerItem * amount);
                itemsSold += amount;
                item.setAmount(0);
            }
        }

        if (totalEarned > 0) {
            addCoins(player, totalEarned);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            player.sendMessage(ChatColor.GREEN + "========================================");
            player.sendMessage(ChatColor.GREEN + "  ĐÃ BÁN THÀNH CÔNG " + itemsSold + " VẬT PHẨM!");
            player.sendMessage(ChatColor.YELLOW + "  Nhận được: " + ChatColor.GOLD + "+" + totalEarned + " Xu ⛃");
            player.sendMessage(ChatColor.GREEN + "========================================");
        } else {
            player.sendMessage(ChatColor.RED + "Kho đồ của bạn không có quặng hoặc đá để bán!");
            player.sendMessage(ChatColor.GRAY + "(Hệ thống thu mua: Than, Đồng, Sắt, Vàng, Kim Cương, Ngọc lục bảo, Đá cuội...)");
        }
    }

    public void handleShop(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(ChatColor.GOLD + "================= 🛒 CỬA HÀNG ==================");
            player.sendMessage(ChatColor.YELLOW + "Gõ: " + ChatColor.AQUA + "/mua <tên_vật_phẩm>" + ChatColor.YELLOW + " để mua:");
            player.sendMessage(ChatColor.GREEN + "• /mua banhmi    " + ChatColor.WHITE + "- 16 Bánh mì      " + ChatColor.GOLD + "(20 Xu)");
            player.sendMessage(ChatColor.GREEN + "• /mua thitbo    " + ChatColor.WHITE + "- 16 Thịt bò nướng " + ChatColor.GOLD + "(50 Xu)");
            player.sendMessage(ChatColor.GREEN + "• /mua taovang   " + ChatColor.WHITE + "- 4 Táo vàng       " + ChatColor.GOLD + "(150 Xu)");
            player.sendMessage(ChatColor.GREEN + "• /mua cupsat    " + ChatColor.WHITE + "- 1 Cúp sắt xịn    " + ChatColor.GOLD + "(80 Xu)");
            player.sendMessage(ChatColor.GREEN + "• /mua cupkc     " + ChatColor.WHITE + "- 1 Cúp kim cương  " + ChatColor.GOLD + "(350 Xu)");
            player.sendMessage(ChatColor.GREEN + "• /mua kiemkc    " + ChatColor.WHITE + "- 1 Kiếm kim cương " + ChatColor.GOLD + "(300 Xu)");
            player.sendMessage(ChatColor.GREEN + "• /mua ngocender " + ChatColor.WHITE + "- 8 Ngọc Ender     " + ChatColor.GOLD + "(120 Xu)");
            player.sendMessage(ChatColor.GREEN + "• /mua phaohoa   " + ChatColor.WHITE + "- 16 Pháo hoa      " + ChatColor.GOLD + "(40 Xu)");
            player.sendMessage(ChatColor.GOLD + "================================================");
            return;
        }

        String item = args[0].toLowerCase();
        Material mat;
        int count;
        long price;
        String displayName;

        switch (item) {
            case "banhmi":
                mat = Material.BREAD; count = 16; price = 20; displayName = "16 Bánh mì"; break;
            case "thitbo":
                mat = Material.COOKED_BEEF; count = 16; price = 50; displayName = "16 Thịt bò nướng"; break;
            case "taovang":
                mat = Material.GOLDEN_APPLE; count = 4; price = 150; displayName = "4 Táo vàng"; break;
            case "cupsat":
                mat = Material.IRON_PICKAXE; count = 1; price = 80; displayName = "1 Cúp sắt"; break;
            case "cupkc":
                mat = Material.DIAMOND_PICKAXE; count = 1; price = 350; displayName = "1 Cúp kim cương"; break;
            case "kiemkc":
                mat = Material.DIAMOND_SWORD; count = 1; price = 300; displayName = "1 Kiếm kim cương"; break;
            case "ngocender":
                mat = Material.ENDER_PEARL; count = 8; price = 120; displayName = "8 Ngọc Ender"; break;
            case "phaohoa":
                mat = Material.FIREWORK_ROCKET; count = 16; price = 40; displayName = "16 Pháo hoa"; break;
            default:
                player.sendMessage(ChatColor.RED + "Vật phẩm không tồn tại! Gõ /mua để xem danh sách.");
                return;
        }

        if (removeCoins(player, price)) {
            player.getInventory().addItem(new ItemStack(mat, count));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            player.sendMessage(ChatColor.GREEN + "✔ Bạn đã mua thành công " + ChatColor.YELLOW + displayName +
                    ChatColor.GREEN + " với giá " + ChatColor.GOLD + price + " Xu!");
        } else {
            player.sendMessage(ChatColor.RED + "✖ Bạn không đủ tiền! Cần: " + price + " Xu.");
        }
    }
}
