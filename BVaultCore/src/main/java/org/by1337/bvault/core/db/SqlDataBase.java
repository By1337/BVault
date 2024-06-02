package org.by1337.bvault.core.db;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.by1337.bvault.core.util.CachedMap;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;

public abstract class SqlDataBase implements DataBase, Listener {
    protected final HikariDataSource dataSource;
    protected final Plugin plugin;
    protected final Object lock = new Object();
    protected final CachedMap<UUID, User> userCash2;
    protected final Map<UUID, User> userCash = new HashMap<>();
    protected final ThreadFactory ioThreadFactory;
    protected final ExecutorService ioExecutor;

    public SqlDataBase(HikariConfig hikariConfig, Plugin plugin) {
        dataSource = new HikariDataSource(hikariConfig);
        this.plugin = plugin;
        ioThreadFactory = new ThreadFactoryBuilder().setNameFormat("BVault IO #%d").build();
        ioExecutor = Executors.newCachedThreadPool(ioThreadFactory);
        userCash2 = new CachedMap<>(5, TimeUnit.MINUTES, plugin, 60 * 20);
        userCash2.onRemove(pair -> {
            if (plugin.getServer().getPlayer(pair.getLeft()) != null) {
                userCash2.put(pair.getKey(), pair.getRight());
            }
        });
        createTableIfNotExist();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            getUser(player.getUniqueId()).whenComplete((u, t) -> {
                if (t != null) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to load user", t);
                }
            });
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public CompletableFuture<User> getUser(UUID uuid) {
        var user = getUserFromCash(uuid);
        if (user != null) return CompletableFuture.completedFuture(user);
        return CompletableFuture.supplyAsync(() -> loadUser(uuid), ioExecutor);
    }

    @Nullable
    private User getUserFromCash(UUID uuid) {
        synchronized (lock) {
            var user = userCash.get(uuid);
            if (user != null) return user;
            user = userCash2.get(uuid);
            if (user != null) return user;
        }
        return null;
    }

    private User loadUser(UUID uuid) {
        Map<String, Double> balances = new HashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT bank_name, balance FROM user_balance WHERE user_id = ?")
        ) {
            statement.setString(1, uuid.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    balances.put(
                            resultSet.getString("bank_name"),
                            resultSet.getDouble("balance")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load user!", e);
        }
        var user = new User(balances, uuid, this);
        synchronized (lock) {
            if (plugin.getServer().getPlayer(user.getUuid()) != null) {
                userCash.put(uuid, user);
            } else {
                userCash2.put(uuid, user);
            }
        }
        return user;
    }

    @Override
    public void close() {
        PlayerJoinEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
        dataSource.close();
        ioExecutor.shutdown();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent joinEvent) {
        getUser(joinEvent.getPlayer().getUniqueId()).whenComplete((u, t) -> {
            if (t != null) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load user", t);
            }
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
        synchronized (lock) {
            var user = userCash.remove(playerQuitEvent.getPlayer().getUniqueId());
            if (user != null) {
                userCash2.put(user.getUuid(), user);
            }
        }
    }

    private void createTableIfNotExist() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     CREATE TABLE IF NOT EXISTS user_balance
                     (
                         bank_name VARCHAR(128),
                         user_id   VARCHAR(36),
                         balance   DOUBLE,
                         PRIMARY KEY (bank_name, user_id)
                     );
                     """)
        ) {
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
