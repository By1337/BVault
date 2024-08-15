package org.by1337.bvault.core.db;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.by1337.blib.util.collection.ExpiringSynchronizedMap;
import org.by1337.bvault.api.BEconomy;
import org.by1337.bvault.core.top.BalTop;
import org.by1337.bvault.core.top.TopInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

public class SqliteDatabase implements Database, Listener {
    protected final HikariDataSource dataSource;
    protected final Object lock = new Object();
    protected final ThreadFactory ioThreadFactory;
    protected final ExecutorService ioExecutor;
    protected final BalTop balTop;
    protected final ExpiringSynchronizedMap<UUID, User> cache;
    protected final Map<UUID, User> userMap = new HashMap<>();
    protected final Plugin plugin;
    protected final Set<String> knownBanks;

    public SqliteDatabase(HikariConfig hikariConfig, Plugin plugin, BalTop balTop) {
        dataSource = new HikariDataSource(hikariConfig);
        this.plugin = plugin;
        this.balTop = balTop;
        ioThreadFactory = new ThreadFactoryBuilder().setNameFormat("BVault IO #%d").build();
        ioExecutor = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors() / 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                ioThreadFactory);
        cache = new ExpiringSynchronizedMap<>(5, TimeUnit.MINUTES);
        cache.onExpired((uuid, user) -> {
            if (Bukkit.getPlayer(uuid) != null) {
                synchronized (lock) {
                    userMap.put(uuid, user);
                }
            }
        });
        createTableIfNotExist();
        knownBanks = new HashSet<>(loadBanks());
        updateBanks(BEconomy.DEFAULT_BANK);
        for (String knownBank : new ArrayList<>(knownBanks)) {
            getTopByBank(knownBank, balTop.getTopSize()).whenComplete((list, t) -> {
                if (t != null) {
                    plugin.getSLF4JLogger().error("Failed to get top!", t);
                }
                if (list != null) {
                    balTop.setTop(list, knownBank);
                }
            });
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            getUser(onlinePlayer.getUniqueId()).whenComplete((u, t) -> {
                if (t != null) {
                    plugin.getSLF4JLogger().error("Failed to get user!", t);
                }
            });
        }
    }

    @Override
    public CompletableFuture<User> getUser(@NotNull UUID uuid) {
        var user = getUserFromCash(uuid);
        if (user != null) return CompletableFuture.completedFuture(user);
        return CompletableFuture.supplyAsync(() -> loadUser(uuid), ioExecutor);
    }

    @Nullable
    private User getUserFromCash(UUID uuid) {
        User user = cache.get(uuid);
        if (user == null) {
            synchronized (lock) {
                return userMap.get(uuid);
            }
        }
        return user;
    }

    @Override
    public void flushUser(@NotNull User user, @NotNull String bank) {
        updateBanks(bank);
        ioExecutor.execute(() -> {
            synchronized (dataSource) {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement("""
                             INSERT INTO player_balances (uuid, nickname, bank, balance)
                             VALUES (?, ?, ?, ?)
                             ON CONFLICT(uuid, bank) DO UPDATE SET balance = excluded.balance;
                             """)
                ) {
                    Double balance = user.getBalance(bank);
                    balTop.updateBalance(user.getUuid(), balance, bank, user.getNickName());
                    statement.setString(1, user.getUuid().toString());
                    statement.setString(2, user.getNickName());
                    statement.setString(3, bank);
                    statement.setDouble(4, balance);
                    statement.execute();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to flush user!", e);
                }
            }
        });
    }

    private User loadUser(UUID uuid) {
        Map<String, Double> balances = new HashMap<>();
        String nickname = null;
        synchronized (dataSource) {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                         SELECT bank, balance, nickname
                         FROM player_balances
                         WHERE uuid = ?;
                         """)
            ) {
                statement.setString(1, uuid.toString());

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        balances.put(
                                resultSet.getString("bank"),
                                resultSet.getDouble("balance")
                        );
                        nickname = resultSet.getString("nickname");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load user!", e);
            }
        }
        var user = new User(balances, uuid, this, nickname);
        synchronized (lock) {
            knownBanks.addAll(user.getExistedBanks());
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            cache.put(uuid, user);
            return user;
        }
        if (!user.getNickName().equals(player.getName())) {
            user.setName(player.getName());
            updateNickName(uuid, player.getName());
        }
        synchronized (lock) {
            userMap.put(uuid, user);
        }
        return user;
    }

    private void updateNickName(UUID uuid, String newName) {
        ioExecutor.execute(() -> {
            synchronized (dataSource) {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement("""
                             UPDATE player_balances
                             SET nickname = ?
                             WHERE uuid = ?;
                             """)
                ) {
                    statement.setString(1, newName);
                    statement.setString(2, uuid.toString());
                    statement.execute();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to update uuid nick name", e);
                }
            }
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent joinEvent) {
        getUser(joinEvent.getPlayer().getUniqueId()).whenComplete((u, t) -> {
            if (t != null) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load user", t);
            }
            if (u != null) {
                User cached = cache.remove(u.getUuid());
                synchronized (lock) {
                    if (cached != null) {
                        userMap.put(u.getUuid(), u);
                    }
                }
            }
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
        User user;
        synchronized (lock) {
            user = userMap.remove(playerQuitEvent.getPlayer().getUniqueId());
        }
        if (user != null) {
            cache.put(user.getUuid(), user);
        }
    }

    @Override
    public void close() {
        PlayerJoinEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
        synchronized (dataSource) {
            dataSource.close();
        }
        ioExecutor.shutdown();
        cache.shutdown();
    }

    @Override
    public CompletableFuture<Void> dropBalancesIn(@Nullable String bank) {
        return CompletableFuture.runAsync(() -> {
            synchronized (dataSource) {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                             bank != null ?
                                     "DELETE FROM player_balances WHERE bank = ?" :
                                     "DELETE FROM player_balances;"
                     )
                ) {
                    if (bank != null) {
                        statement.setString(1, bank);
                    }
                    statement.execute();
                    synchronized (lock) {
                        for (Collection<User> list : List.of(cache.values(), userMap.values())) {
                            for (User value : list) {
                                if (bank != null) {
                                    value.balances.remove(bank);
                                    value.balancesOld.remove(bank);
                                } else {
                                    value.balances.clear();
                                    value.balancesOld.clear();
                                }
                            }
                        }
                        if (bank == null) {
                            balTop.clear();
                        } else {
                            balTop.clearBalancesIn(bank);
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }, ioExecutor);
    }

    @Override
    public CompletableFuture<Void> dropBalances() {
        return dropBalancesIn(null);
    }

    @Override
    public Set<String> getKnownBanks() {
        synchronized (knownBanks) {
            return Collections.unmodifiableSet(knownBanks);
        }
    }

    @Override
    public CompletableFuture<List<TopInfo>> getTopByBank(@NotNull String bank, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (dataSource) {
                try (Connection connection = dataSource.getConnection();
                     PreparedStatement statement = connection.prepareStatement("""
                             SELECT nickname, balance, uuid
                             FROM player_balances
                             WHERE bank = ?
                             ORDER BY balance DESC
                             LIMIT ?;
                             """)
                ) {
                    statement.setString(1, bank);
                    statement.setInt(2, limit);

                    ResultSet resultSet = statement.executeQuery();

                    List<TopInfo> result = new ArrayList<>(limit);
                    int pos = 0;
                    while (resultSet.next()) {
                        UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                        String nickname = resultSet.getString("nickname");
                        double balance = resultSet.getDouble("balance");
                        result.add(new TopInfo(
                                uuid,
                                nickname,
                                balance,
                                pos++
                        ));
                    }
                    for (; pos < limit; pos++) {
                        result.add(TopInfo.EMPTY);
                    }
                    return result;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }, ioExecutor);
    }

    private void createTableIfNotExist() {
        synchronized (dataSource) {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        CREATE TABLE IF NOT EXISTS player_balances (
                            uuid CHAR(36),
                            nickname VARCHAR(36),
                            bank VARCHAR(16),
                            balance DOUBLE,
                            PRIMARY KEY (uuid, bank)
                        );
                        """)) {
                    statement.execute();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        CREATE INDEX IF NOT EXISTS idx_uuid ON player_balances(uuid);
                        """)) {
                    statement.execute();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        CREATE INDEX IF NOT EXISTS idx_bank_balance ON player_balances(bank, balance DESC);
                        """)) {
                    statement.execute();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        CREATE TABLE IF NOT EXISTS banks (bank_name VARCHAR(16) PRIMARY KEY);
                        """)) {
                    statement.execute();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private List<String> loadBanks() {
        synchronized (dataSource) {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT bank_name FROM banks;")
            ) {
                ResultSet resultSet = statement.executeQuery();
                List<String> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(resultSet.getString("bank_name"));
                }
                return result;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void updateBanks(String bank) {
        boolean b;
        synchronized (knownBanks) {
            b = knownBanks.add(bank);
        }
        if (b) {
            CompletableFuture.runAsync(() -> {
                synchronized (dataSource) {
                    try (Connection connection = dataSource.getConnection();
                         PreparedStatement statement = connection.prepareStatement("""
                                 INSERT OR IGNORE INTO banks (bank_name)
                                 VALUES (?);
                                 """)
                    ) {
                        statement.setString(1, bank);
                        statement.execute();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, ioExecutor);
        }
    }
}
