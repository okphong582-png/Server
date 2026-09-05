package com.funserver;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyManager implements Listener {
    private final FunServerCore plugin;
    private Location lobbyLocation;
    private final double lobbyRadius = 90.0;
    private final Set<UUID> buildModePlayers = ConcurrentHashMap.newKeySet();

    public LobbyManager(FunServerCore plugin) {
        this.plugin = plugin;
        loadLobbyLocation();
    }

    private void loadLobbyLocation() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("lobby.world")) {
            World world = plugin.getServer().getWorld(config.getString("lobby.world"));
            if (world != null) {
                double x = config.getDouble("lobby.x");
                double y = config.getDouble("lobby.y");
                double z = config.getDouble("lobby.z");
                float yaw = (float) config.getDouble("lobby.yaw");
                float pitch = (float) config.getDouble("lobby.pitch");
                lobbyLocation = new Location(world, x, y, z, yaw, pitch);
                return;
            }
        }
        World defaultWorld = plugin.getServer().getWorlds().get(0);
        lobbyLocation = defaultWorld.getSpawnLocation().add(0.5, 0, 0.5);
    }

    public void setLobby(Player player) {
        lobbyLocation = player.getLocation();
        FileConfiguration config = plugin.getConfig();
        config.set("lobby.world", lobbyLocation.getWorld().getName());
        config.set("lobby.x", lobbyLocation.getX());
        config.set("lobby.y", lobbyLocation.getY());
        config.set("lobby.z", lobbyLocation.getZ());
        config.set("lobby.yaw", lobbyLocation.getYaw());
        config.set("lobby.pitch", lobbyLocation.getPitch());
        plugin.saveConfig();

        player.sendMessage(ChatColor.GREEN + "========================================");
        player.sendMessage(ChatColor.GREEN + "  ĐÃ THIẾT LẬP VỊ TRÍ SẢNH CHỜ (LOBBY)!");
        player.sendMessage(ChatColor.GREEN + "========================================");
    }

    public void teleportToLobby(Player player) {
        if (lobbyLocation != null) {
            player.teleport(lobbyLocation);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.sendMessage(ChatColor.GREEN + "Đã dịch chuyển về Sảnh chính!");
        }
    }

    public boolean isInLobby(Location loc) {
        if (lobbyLocation == null || loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().equals(lobbyLocation.getWorld())) return false;
        return loc.distanceSquared(lobbyLocation) <= (lobbyRadius * lobbyRadius);
    }

    public void toggleBuildMode(Player player) {
        if (buildModePlayers.contains(player.getUniqueId())) {
            buildModePlayers.remove(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "Đã tắt chế độ xây dựng tại Sảnh!");
        } else {
            buildModePlayers.add(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Đã bật chế độ xây dựng tại Sảnh (OP Build Mode)!");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isInLobby(event.getBlock().getLocation())) {
            if (!buildModePlayers.contains(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Khu vực Sảnh được bảo vệ, không thể đập khối!");
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (isInLobby(event.getBlock().getLocation())) {
            if (!buildModePlayers.contains(player.getUniqueId())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Khu vực Sảnh được bảo vệ, không thể đặt khối!");
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isInLobby(player.getLocation())) {
                event.setCancelled(true);
                if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                    teleportToLobby(player);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            if (isInLobby(victim.getLocation())) {
                event.setCancelled(true);
                if (event.getDamager() instanceof Player) {
                    ((Player) event.getDamager()).sendMessage(ChatColor.YELLOW + "Khu vực Sảnh hòa bình, không thể tấn công người chơi khác!");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isInLobby(player.getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isInLobby(player.getLocation())) {
            if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                if (player.isOnGround()) {
                    player.setAllowFlight(true);
                }
            }
        }
    }

    @EventHandler
    public void onDoubleJump(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (isInLobby(player.getLocation())) {
            if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                event.setCancelled(true);
                player.setAllowFlight(false);

                player.setVelocity(player.getLocation().getDirection().multiply(1.4).setY(0.9));
                player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.2f);
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 15, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }
}
