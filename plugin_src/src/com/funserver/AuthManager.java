package com.funserver;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitTask;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager implements Listener {
    private final FunServerCore plugin;
    private final String firebaseBaseUrl = "https://appchatai-313e3-default-rtdb.firebaseio.com";
    private final HttpClient httpClient;
    private final Set<UUID> authenticated = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> joinTimes = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> ipConnections = new ConcurrentHashMap<>();
    private BukkitTask reminderTask;

    public AuthManager(FunServerCore plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        startReminderTask();
    }

    public boolean isAuthenticated(Player player) {
        return authenticated.contains(player.getUniqueId());
    }

    private void startReminderTask() {
        reminderTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!isAuthenticated(player)) {
                    Long joinTime = joinTimes.get(player.getUniqueId());
                    if (joinTime != null && (now - joinTime) > 60000) {
                        player.kickPlayer(ChatColor.RED + "Ban da qua thoi gian dang nhap (60 giay)!\nVui long vao lai.");
                        continue;
                    }

                    player.sendTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "MÁY CHỦ VUI VẺ",
                            ChatColor.YELLOW + "Gõ " + ChatColor.AQUA + "/dangki <mk>" + ChatColor.YELLOW + " hoặc " + ChatColor.AQUA + "/dangnhap <mk>",
                            0, 40, 10);
                }
            }
        }, 20L, 40L);
    }

    public void shutdown() {
        if (reminderTask != null) {
            reminderTask.cancel();
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return Integer.toString(password.hashCode());
        }
    }

    private String sanitizeUsername(String username) {
        return URLEncoder.encode(username.toLowerCase(), StandardCharsets.UTF_8);
    }

    public void handleRegister(Player player, String[] args) {
        if (isAuthenticated(player)) {
            player.sendMessage(ChatColor.YELLOW + "Ban da dang nhap roi!");
            return;
        }

        String password;
        if (args.length == 1) {
            password = args[0];
        } else if (args.length >= 2) {
            password = args[1];
        } else {
            player.sendMessage(ChatColor.RED + "Cu phap: /dangki <mat_khau> hoac /dangki <tai_khoan> <mat_khau>");
            return;
        }

        if (password.length() < 4) {
            player.sendMessage(ChatColor.RED + "Mat khau phai co it nhat 4 ky tu!");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Dang kiem tra tai khoan tren Firebase...");

        String usernameKey = sanitizeUsername(player.getName());
        String url = firebaseBaseUrl + "/users/" + usernameKey + ".json";

        httpClient.sendAsync(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(6))
                .build(), HttpResponse.BodyHandlers.ofString())
        .thenAccept(response -> {
            String body = response.body();
            if (body != null && !body.equals("null") && !body.trim().isEmpty()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.RED + "Tai khoan nay da ton tai! Vui long dung lenh: /dangnhap <mat_khau>");
                });
                return;
            }

            String passwordHash = hashPassword(password);
            String jsonPayload = "{\"password\":\"" + passwordHash + "\",\"registered_at\":" + System.currentTimeMillis() + ",\"player_name\":\"" + player.getName() + "\"}";

            httpClient.sendAsync(HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(6))
                    .build(), HttpResponse.BodyHandlers.ofString())
            .thenAccept(putResponse -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    authenticated.add(player.getUniqueId());
                    player.sendMessage(ChatColor.GREEN + "========================================");
                    player.sendMessage(ChatColor.GREEN + "  DANG KY THANH CONG! CHAO MUNG BAN!");
                    player.sendMessage(ChatColor.GREEN + "========================================");
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    player.sendTitle(ChatColor.GREEN + "THÀNH CÔNG!", ChatColor.WHITE + "Chúc bạn chơi vui vẻ!", 10, 50, 10);
                });
            }).exceptionally(ex -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.RED + "Loi luu tai khoan vao Firebase! Vui long thu lai.");
                });
                return null;
            });
        }).exceptionally(ex -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.RED + "Loi ket noi Firebase: " + ex.getMessage());
            });
            return null;
        });
    }

    public void handleLogin(Player player, String[] args) {
        if (isAuthenticated(player)) {
            player.sendMessage(ChatColor.YELLOW + "Ban da dang nhap roi!");
            return;
        }

        String password;
        if (args.length == 1) {
            password = args[0];
        } else if (args.length >= 2) {
            password = args[1];
        } else {
            player.sendMessage(ChatColor.RED + "Cu phap: /dangnhap <mat_khau> hoac /dangnhap <tai_khoan> <mat_khau>");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Dang xac thuc tren Firebase...");

        String usernameKey = sanitizeUsername(player.getName());
        String url = firebaseBaseUrl + "/users/" + usernameKey + ".json";

        httpClient.sendAsync(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(6))
                .build(), HttpResponse.BodyHandlers.ofString())
        .thenAccept(response -> {
            String body = response.body();
            if (body == null || body.equals("null") || body.trim().isEmpty()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.RED + "Tai khoan chua dang ky! Vui long dung lenh: /dangki <mat_khau>");
                });
                return;
            }

            String passwordHash = hashPassword(password);
            if (body.contains("\"password\":\"" + passwordHash + "\"")) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    authenticated.add(player.getUniqueId());
                    player.sendMessage(ChatColor.GREEN + "========================================");
                    player.sendMessage(ChatColor.GREEN + "       DANG NHAP THANH CONG!");
                    player.sendMessage(ChatColor.GREEN + "========================================");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    player.sendTitle(ChatColor.GREEN + "ĐĂNG NHẬP THÀNH CÔNG!", ChatColor.WHITE + "Chao mung tro lai!", 10, 40, 10);
                });
            } else {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.RED + "Sai mat khau! Vui long thu lai.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                });
            }
        }).exceptionally(ex -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.RED + "Loi ket noi Firebase: " + ex.getMessage());
            });
            return null;
        });
    }

    public void handleChangePassword(Player player, String[] args) {
        if (!isAuthenticated(player)) {
            player.sendMessage(ChatColor.RED + "Ban phai dang nhap truoc!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Cu phap: /doimk <mat_khau_cu> <mat_khau_moi>");
            return;
        }

        String oldPass = args[0];
        String newPass = args[1];

        if (newPass.length() < 4) {
            player.sendMessage(ChatColor.RED + "Mat khau moi phai co it nhat 4 ky tu!");
            return;
        }

        String usernameKey = sanitizeUsername(player.getName());
        String url = firebaseBaseUrl + "/users/" + usernameKey + ".json";

        httpClient.sendAsync(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(6))
                .build(), HttpResponse.BodyHandlers.ofString())
        .thenAccept(response -> {
            String body = response.body();
            String oldHash = hashPassword(oldPass);
            if (body != null && body.contains("\"password\":\"" + oldHash + "\"")) {
                String newHash = hashPassword(newPass);
                String jsonPayload = "{\"password\":\"" + newHash + "\",\"updated_at\":" + System.currentTimeMillis() + ",\"player_name\":\"" + player.getName() + "\"}";
                httpClient.sendAsync(HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .header("Content-Type", "application/json")
                        .build(), HttpResponse.BodyHandlers.ofString())
                .thenAccept(r -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage(ChatColor.GREEN + "Doi mat khau thanh cong!");
                    });
                });
            } else {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.RED + "Mat khau cu khong dung!");
                });
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String ip = event.getAddress().getHostAddress();
        long now = System.currentTimeMillis();

        List<Long> times = ipConnections.computeIfAbsent(ip, k -> new ArrayList<>());
        synchronized (times) {
            times.removeIf(t -> now - t > 30000);
            if (times.size() >= 5) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        ChatColor.RED + "Ban ket noi qua nhanh (Anti-DDoS / Bot Protection)!\nVui long doi 30 giay.");
                return;
            }
            times.add(now);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        authenticated.remove(player.getUniqueId());
        joinTimes.put(player.getUniqueId(), System.currentTimeMillis());

        player.sendMessage(ChatColor.GOLD + "========================================");
        player.sendMessage(ChatColor.YELLOW + "Chào mừng " + ChatColor.AQUA + player.getName() + ChatColor.YELLOW + " đến với máy chủ!");
        player.sendMessage(ChatColor.YELLOW + "Vui lòng nhập lệnh sau để chơi:");
        player.sendMessage(ChatColor.GREEN + "  /dangki <mật_khẩu>   " + ChatColor.GRAY + "(nếu mới chơi lần đầu)");
        player.sendMessage(ChatColor.AQUA + "  /dangnhap <mật_khẩu> " + ChatColor.GRAY + "(nếu đã có tài khoản)");
        player.sendMessage(ChatColor.GOLD + "========================================");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        authenticated.remove(event.getPlayer().getUniqueId());
        joinTimes.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isAuthenticated(player)) {
            if (event.getFrom().getX() != event.getTo().getX() ||
                event.getFrom().getY() != event.getTo().getY() ||
                event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom().setDirection(event.getTo().getDirection()));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!isAuthenticated(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Vui long dang ky/dang nhap truoc khi chat!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!isAuthenticated(event.getPlayer())) {
            String msg = event.getMessage().toLowerCase();
            if (!msg.startsWith("/dangki") && !msg.startsWith("/register") &&
                !msg.startsWith("/dangnhap") && !msg.startsWith("/login")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "Vui long dung /dangki <mk> hoac /dangnhap <mk> truoc!");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent event) {
        if (!isAuthenticated(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent event) {
        if (!isAuthenticated(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (!isAuthenticated(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (!isAuthenticated(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!isAuthenticated(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (!isAuthenticated(player)) {
                event.setCancelled(true);
            }
        }
    }
}
