package org.by1337.bvault.core.top;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.by1337.blib.nbt.DefaultNbtByteBuffer;
import org.by1337.blib.nbt.NbtType;
import org.by1337.blib.nbt.impl.CompoundTag;
import org.by1337.blib.nbt.impl.ListNBT;
import org.by1337.bvault.core.db.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;

public class BalTop implements Closeable, Listener {
    private final Object lock = new Object();
    private final Plugin plugin;
    private final File dataFolder;
    @VisibleForTesting
    final ExecutorService ioExecutor;
    @VisibleForTesting
    final HikariDataSource uuidToNameDb;
    private final int topSize;
    private final Map<String, Top> topMap = new HashMap<>();

    public BalTop(Plugin plugin, ExecutorService ioExecutor, int topSize) {
        this.ioExecutor = ioExecutor;
        this.plugin = plugin;
        dataFolder = new File(plugin.getDataFolder(), "topData");
        this.topSize = topSize;
        dataFolder.mkdirs();
        load();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(String.format("jdbc:sqlite:%s", new File(dataFolder, "uuidToName.db").getPath()));
        uuidToNameDb = new HikariDataSource(hikariConfig);
        createTableIfNotExist();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

    }

    public BalTop(Plugin plugin, int topSize) {
        this(
                plugin,
                Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("BVault balTop IO #%d").build()),
                topSize
        );
    }

    void save() {
        try {
            File file = new File(dataFolder, "data.nbnt");
            CompoundTag compoundTag = new CompoundTag();
            ListNBT listNBT = new ListNBT();
            synchronized (lock) {
                for (Top value : topMap.values()) {
                    listNBT.add(value.save());
                }
            }
            compoundTag.putTag("tops", listNBT);
            DefaultNbtByteBuffer buffer = new DefaultNbtByteBuffer();
            compoundTag.write(buffer);
            Files.write(file.toPath(), buffer.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save!", e);
        }
    }


    public void clear() {
        synchronized (lock) {
            topMap.clear();
        }
    }

    public void clearBalancesIn(String bank) {
        synchronized (lock) {
            topMap.remove(bank);
        }
    }

    private void load() {
        try {
            File file = new File(dataFolder, "data.nbnt");
            if (file.exists()) {
                byte[] arr = Files.readAllBytes(file.toPath());
                DefaultNbtByteBuffer buffer = new DefaultNbtByteBuffer(arr);
                CompoundTag compoundTag = (CompoundTag) NbtType.COMPOUND.read(buffer);
                for (Top top : compoundTag.getAsList("tops", n -> new Top(((CompoundTag) n)))) {
                    topMap.put(top.bank, top);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load!", e);
        }
    }

    public List<TopInfo> getTop(String bank, int limit) {
        List<TopInfo> list = new ArrayList<>();
        synchronized (lock) {
            var top = topMap.get(bank);
            int x = 0;
            if (top != null) {
                for (User user : top.top) {
                    if (x >= limit) break;
                    list.add(new TopInfo(
                            user.uuid,
                            user.name,
                            user.balance,
                            x
                    ));
                    x++;
                }
            }
            for (; x < limit; x++) {
                list.add(TopInfo.EMPTY);
            }
        }
        return list;
    }

    public void updateBalance(UUID player, double balance, String bank) {
        ioExecutor.execute(() -> updateBalance0(player, balance, bank));
    }

    private void updateBalance0(UUID player, double balance, String bank) {
        Top top;
        User oldUser;
        User user;
        synchronized (lock) {
            top = topMap.computeIfAbsent(bank, Top::new);
            oldUser = top.users.get(player);
        }
        if (oldUser == null) {
            user = new User(player, getNickName(player), balance);
        } else {
            user = new User(player, oldUser.name, balance);
        }
        synchronized (lock) {
            top.addUser(user);
        }
    }

    @Override
    public void close() {
        save();
        PlayerJoinEvent.getHandlerList().unregister(this);
        synchronized (uuidToNameDb) {
            uuidToNameDb.close();
        }
        ioExecutor.shutdown();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        ioExecutor.execute(() -> {
            synchronized (uuidToNameDb) {
                try (Connection connection = uuidToNameDb.getConnection();
                     PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO users (uuid, username) VALUES (?, ?)")
                ) {
                    statement.setString(1, event.getPlayer().getUniqueId().toString());
                    statement.setString(2, event.getPlayer().getName());
                    statement.execute();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private String getNickName(UUID uuid) {
        var player = plugin.getServer().getPlayer(uuid);
        if (player != null) return player.getName();
        synchronized (uuidToNameDb) {
            try (Connection connection = uuidToNameDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT username FROM users WHERE uuid = ?")
            ) {
                var result = statement.executeQuery();
                if (result.next()) {
                    return result.getString("username");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get user nickname", e);
            }
        }
        return "unknown";
    }

    private void createTableIfNotExist() {
        synchronized (uuidToNameDb) {
            try (Connection connection = uuidToNameDb.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                          CREATE TABLE IF NOT EXISTS users (
                              uuid VARCHAR(32) PRIMARY KEY,
                              username TEXT NOT NULL
                          );
                         """)
            ) {
                statement.execute();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class Top {
        private final TreeSet<BalTop.User> top = new TreeSet<>(BalTop.User::compareTo);
        private final Map<UUID, BalTop.User> users = new HashMap<>();
        private final String bank;

        public Top(String bank) {
            this.bank = bank;
        }

        public Top(CompoundTag data) {
            bank = data.getAsString("bank");
            for (User user : data.getAsList("users", n -> User.build(((CompoundTag) n)))) {
                addUser(user);
            }
        }

        private void addUser(BalTop.User user) {
            if (top.isEmpty() ||
                top.last().balance < user.balance ||
                users.containsKey(user.uuid) ||
                top.size() < topSize
            ) {
                top.removeIf(u -> u.uuid.equals(user.uuid));
                if (!top.add(user)) {
                    throw new IllegalStateException("Failed to add user into top!", new Throwable());
                }
                users.put(user.uuid, user);
                trim();
            }
        }

        private void trim() {
            while (top.size() > topSize) {
                var last = top.last();
                top.remove(last);
                users.remove(last.uuid);
            }
        }

        private CompoundTag save() {
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.putString("bank", bank);
            ListNBT listNBT = new ListNBT();
            for (User user : top) {
                listNBT.add(user.save());
            }
            compoundTag.putTag("users", listNBT);
            return compoundTag;
        }
    }

    record User(UUID uuid, String name, double balance) implements Comparable<BalTop.User> {
        @Override
        public int compareTo(@NotNull BalTop.User o) {
            var i = Double.compare(o.balance, balance);
            return i == 0 ? uuid.compareTo(o.uuid) : i;
        }

        private CompoundTag save() {
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.putUUID("uuid", uuid);
            compoundTag.putString("name", name);
            compoundTag.putDouble("balance", balance);
            return compoundTag;
        }

        public static User build(CompoundTag compoundTag) {
            var uuid = compoundTag.getAsUUID("uuid");
            var name = compoundTag.getAsString("name");
            var balance = compoundTag.getAsDouble("balance");
            return new User(uuid, name, balance);
        }
    }
}
