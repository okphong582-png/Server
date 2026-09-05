package com.funserver;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.block.Sign;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Random;

public class ArenaManager implements Listener {
    private final FunServerCore plugin;
    private Location pvpLocation;
    private Location mineLocation;
    private final Random random = new Random();

    public ArenaManager(FunServerCore plugin) {
        this.plugin = plugin;
        loadLocations();
    }

    private void loadLocations() {
        FileConfiguration config = plugin.getConfig();

        if (config.contains("pvp.world")) {
            World world = plugin.getServer().getWorld(config.getString("pvp.world"));
            if (world != null) {
                double x = config.getDouble("pvp.x");
                double y = config.getDouble("pvp.y");
                double z = config.getDouble("pvp.z");
                float yaw = (float) config.getDouble("pvp.yaw");
                float pitch = (float) config.getDouble("pvp.pitch");
                pvpLocation = new Location(world, x, y, z, yaw, pitch);
            }
        }

        if (pvpLocation == null) {
            World defaultWorld = plugin.getServer().getWorlds().get(0);
            pvpLocation = defaultWorld.getSpawnLocation().clone().add(200, 0, 200);
            Block highest = defaultWorld.getHighestBlockAt(pvpLocation);
            pvpLocation.setY(highest.getY() + 1);
        }

        if (config.contains("mine.world")) {
            World world = plugin.getServer().getWorld(config.getString("mine.world"));
            if (world != null) {
                double x = config.getDouble("mine.x");
                double y = config.getDouble("mine.y");
                double z = config.getDouble("mine.z");
                float yaw = (float) config.getDouble("mine.yaw");
                float pitch = (float) config.getDouble("mine.pitch");
                mineLocation = new Location(world, x, y, z, yaw, pitch);
            }
        }

        if (mineLocation == null) {
            World defaultWorld = plugin.getServer().getWorlds().get(0);
            mineLocation = defaultWorld.getSpawnLocation().clone().add(-200, 0, -200);
            Block highest = defaultWorld.getHighestBlockAt(mineLocation);
            mineLocation.setY(highest.getY() + 1);
        }
    }

    public void setPvP(Player player) {
        pvpLocation = player.getLocation();
        FileConfiguration config = plugin.getConfig();
        config.set("pvp.world", pvpLocation.getWorld().getName());
        config.set("pvp.x", pvpLocation.getX());
        config.set("pvp.y", pvpLocation.getY());
        config.set("pvp.z", pvpLocation.getZ());
        config.set("pvp.yaw", pvpLocation.getYaw());
        config.set("pvp.pitch", pvpLocation.getPitch());
        plugin.saveConfig();

        player.sendMessage(ChatColor.RED + "========================================");
        player.sendMessage(ChatColor.RED + "  ĐÃ THIẾT LẬP ĐẤU TRƯỜNG CHIẾN ĐẤU (PVP)!");
        player.sendMessage(ChatColor.RED + "========================================");
    }

    public void setMine(Player player) {
        mineLocation = player.getLocation();
        FileConfiguration config = plugin.getConfig();
        config.set("mine.world", mineLocation.getWorld().getName());
        config.set("mine.x", mineLocation.getX());
        config.set("mine.y", mineLocation.getY());
        config.set("mine.z", mineLocation.getZ());
        config.set("mine.yaw", mineLocation.getYaw());
        config.set("mine.pitch", mineLocation.getPitch());
        plugin.saveConfig();

        player.sendMessage(ChatColor.GOLD + "========================================");
        player.sendMessage(ChatColor.GOLD + "  ĐÃ THIẾT LẬP KHU ĐÀO MỎ KHOÁNG (MINE)!");
        player.sendMessage(ChatColor.GOLD + "========================================");
    }

    public void teleportToPvP(Player player) {
        if (pvpLocation != null) {
            player.teleport(pvpLocation);
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 1.0f);
            player.sendMessage(ChatColor.RED + "⚔ ĐÃ ĐẾN ĐẤU TRƯỜNG PVP! Hãy chiến đấu hết mình!");
            player.sendTitle(ChatColor.RED + "⚔ ĐẤU TRƯỜNG PVP", ChatColor.YELLOW + "Khu vực chiến đấu tự do!", 10, 40, 10);
        }
    }

    public void teleportToMine(Player player) {
        if (mineLocation != null) {
            player.teleport(mineLocation);
            player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
            player.sendMessage(ChatColor.GOLD + "⛏ ĐÃ ĐẾN KHU ĐÀO MỎ! Hãy đào quặng và gõ /sell để kiếm xu!");
            player.sendTitle(ChatColor.GOLD + "⛏ KHU MỎ KHOÁNG SẢN", ChatColor.YELLOW + "Đào quặng kiếm Exp & Xu!", 10, 40, 10);
        }
    }

    public void randomTeleportSurvival(Player player) {
        World world = plugin.getServer().getWorlds().get(0);
        player.sendMessage(ChatColor.GREEN + "Đang tìm vùng đất sinh tồn an toàn...");

        int min = 800;
        int max = 2500;
        int x = (random.nextBoolean() ? 1 : -1) * (min + random.nextInt(max - min));
        int z = (random.nextBoolean() ? 1 : -1) * (min + random.nextInt(max - min));

        Block highest = world.getHighestBlockAt(x, z);
        Material type = highest.getType();

        if (type == Material.WATER || type == Material.LAVA) {
            highest.setType(Material.STONE);
        }

        Location target = highest.getLocation().add(0.5, 1, 0.5);
        player.teleport(target);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.sendMessage(ChatColor.GREEN + "========================================");
        player.sendMessage(ChatColor.GREEN + "🌲 ĐÃ TỚI THẾ GIỚI SINH TỒN HOANG DÃ!");
        player.sendMessage(ChatColor.YELLOW + "Hãy chặt cây, đào mỏ, xây nhà cùng bạn bè!");
        player.sendMessage(ChatColor.GRAY + "Gõ /lobby hoặc /spawn bất cứ lúc nào để về sảnh.");
        player.sendMessage(ChatColor.GREEN + "========================================");
        player.sendTitle(ChatColor.GREEN + "THẾ GIỚI SINH TỒN", ChatColor.YELLOW + "Chúc bạn sinh tồn vui vẻ!", 10, 50, 10);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && !killer.equals(victim)) {
            event.setDeathMessage(ChatColor.RED + "⚔ " + ChatColor.YELLOW + killer.getName() +
                    ChatColor.GRAY + " đã tiễn " + ChatColor.RED + victim.getName() +
                    ChatColor.GRAY + " lên bảng đếm số!");

            killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.0f);
        }
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null && block.getState() instanceof Sign) {
                Sign sign = (Sign) block.getState();
                String line1 = ChatColor.stripColor(sign.getLine(0)).trim().toLowerCase();
                Player player = event.getPlayer();

                if (line1.contains("sinh ton") || line1.contains("survival") || line1.contains("rtp")) {
                    randomTeleportSurvival(player);
                } else if (line1.contains("pvp")) {
                    teleportToPvP(player);
                } else if (line1.contains("mine") || line1.contains("mo khoang")) {
                    teleportToMine(player);
                } else if (line1.contains("lobby") || line1.contains("sanh") || line1.contains("spawn")) {
                    plugin.getLobbyManager().teleportToLobby(player);
                }
            }
        }
    }
}
