package org.by1337.bvault.core.db;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.by1337.blib.nbt.DefaultNbtByteBuffer;
import org.by1337.blib.nbt.NBT;
import org.by1337.blib.nbt.NbtType;
import org.by1337.blib.nbt.impl.CompoundTag;
import org.by1337.blib.nbt.impl.DoubleNBT;
import org.by1337.bvault.core.top.BalTop;
import org.by1337.bvault.core.util.CachedMap;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;

public class FileDataBase implements DataBase, Listener {
    private final Object lock = new Object();
    private final File dataFolder;
    private final Plugin plugin;
    @VisibleForTesting
    final CachedMap<UUID, CompoundTag> editCash;
    @VisibleForTesting
    final CachedMap<UUID, User> userCash2;
    @VisibleForTesting
    final Map<UUID, User> userCash = new HashMap<>();
    private final ExecutorService ioExecutor;
    private final BalTop balTop;

    public FileDataBase(File dataFolder, Plugin plugin, BalTop balTop, ExecutorService ioExecutor) {
        this.ioExecutor = ioExecutor;
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.balTop = balTop;
        this.dataFolder = dataFolder;
        this.plugin = plugin;

        editCash = new CachedMap<>(5, TimeUnit.MINUTES, plugin, 60 * 20);
        userCash2 = new CachedMap<>(5, TimeUnit.MINUTES, plugin, 60 * 20);

        userCash2.onExpiration(pair -> {
            if (plugin.getServer().getPlayer(pair.getLeft()) != null) {
                userCash2.put(pair.getKey(), pair.getRight());
            }
        });
        editCash.onExpiration(pair -> CompletableFuture.runAsync(() -> {
            save(pair.getValue());
        }, ioExecutor));


        for (Player player : plugin.getServer().getOnlinePlayers()) {
            getUser(player.getUniqueId()).whenComplete((u, t) -> {
                if (t != null) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to load user", t);
                }
            });
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public FileDataBase(File dataFolder, Plugin plugin, BalTop balTop) {
        this(dataFolder, plugin, balTop, Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("BVault IO #%d").build()));
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

    private void save(CompoundTag compoundTag) {
        try {
            File file = new File(dataFolder, compoundTag.getAsUUID("uuid").toString() + ".bnbt");
            DefaultNbtByteBuffer defaultNbtByteBuffer = new DefaultNbtByteBuffer();
            compoundTag.write(defaultNbtByteBuffer);
            Files.write(file.toPath(), defaultNbtByteBuffer.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private User loadUser(UUID uuid) {
        CompoundTag data = loadFromFile(uuid);
        User user;
        if (data != null) {
            user = deserialize(data);
        } else {
            user = new User(uuid, this);
        }
        synchronized (lock) {
            if (plugin.getServer().getPlayer(user.getUuid()) != null) {
                userCash.put(uuid, user);
            } else {
                userCash2.put(uuid, user);
            }
        }
        return user;
    }

    private User deserialize(CompoundTag compoundTag) {
        Map<String, Double> balances = new HashMap<>();
        UUID uuid = compoundTag.getAsUUID("uuid");
        for (Map.Entry<String, NBT> entry : compoundTag.getAsCompoundTag("balances", new CompoundTag()).entrySet()) {
            balances.put(entry.getKey(), ((DoubleNBT) entry.getValue()).getValue());
        }
        return new User(balances, uuid, this);
    }

    @Nullable
    private CompoundTag loadFromFile(UUID uuid) {
        File file = new File(dataFolder, uuid.toString() + ".bnbt");
        try {
            if (file.exists()) {
                byte[] bytes = Files.readAllBytes(file.toPath());
                DefaultNbtByteBuffer defaultNbtByteBuffer = new DefaultNbtByteBuffer(bytes);
                return (CompoundTag) NbtType.COMPOUND.read(defaultNbtByteBuffer);
            }
            return null;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Failed to read user data!", t);
            if (file.exists()) {
                file.delete();
            }
            return null;
        }
    }

    @Override
    public void flushUser(User user, String bank) {
        ioExecutor.execute(() -> {
            synchronized (lock) {
                CompoundTag nbt = editCash.computeIfAbsent(user.getUuid(), k -> {
                    var v = loadFromFile(k);
                    if (v != null) return v;
                    v = new CompoundTag();
                    v.putUUID("uuid", k);
                    return v;
                });
                var balance = user.getBalance(bank);
                nbt.computeIfAbsent("balances", CompoundTag::new).putDouble(bank, balance);
                balTop.updateBalance(user.getUuid(), balance, bank);
            }
        });
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

    @Override
    public void close() {
        PlayerJoinEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
        for (Map.Entry<UUID, CompoundTag> entry : editCash.entrySet()) {
            save(entry.getValue());
        }
        ioExecutor.shutdown();
    }
}
