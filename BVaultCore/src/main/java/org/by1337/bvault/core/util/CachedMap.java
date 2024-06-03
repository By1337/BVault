package org.by1337.bvault.core.util;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.by1337.blib.util.Pair;
import org.by1337.bvault.api.Validate;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A map that caches values for a specified duration and automatically removes them after the duration expires.
 *
 * @param <K> The type of keys maintained by this map.
 * @param <V> The type of mapped values.
 */
public class CachedMap<K, V> {
    // Lock object for synchronizing access to the internal maps.
    private final Object lock = new Object();
    // The main source map that stores the current values.
    private final Map<K, V> source = new HashMap<>();
    // A map that stores the removal time and value pairs for the keys.
    private final Map<K, Pair<Long, V>> removeMap = new HashMap<>();
    // The Bukkit task that periodically checks and removes expired entries.
    private final BukkitTask task;
    // Callback to be executed when an entry is removed.
    private Consumer<Pair<K, V>> expirationCallBack;
    private long ticks = 0;
    private final long cashLifeTime;
    /**
     * Constructs a CachedMap with a specified duration and plugin.
     *
     * @param storeTime The duration for which an entry should be stored.
     * @param timeUnit  The time unit of the storeTime parameter.
     * @param plugin    The plugin instance.
     */
    public CachedMap(long storeTime, TimeUnit timeUnit, Plugin plugin, long tickSpeed) {
        cashLifeTime = (timeUnit.toMillis(storeTime) / 50) / tickSpeed;
        Validate.assertPositive(cashLifeTime, "storeTime must be greater than tickSpeed!");
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            synchronized (lock) {
                ticks++;
                var iterator = removeMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    if (entry.getValue().getLeft() <= ticks) {
                        iterator.remove();
                        source.remove(entry.getKey());
                        if (expirationCallBack != null) {
                            expirationCallBack.accept(Pair.of(entry.getKey(), entry.getValue().getValue()));
                        }
                    }
                }
            }
        }, 0, tickSpeed);
    }

    /**
     * Puts a key-value pair into the map and sets its expiration time.
     *
     * @param k The key.
     * @param v The value.
     * @return The previous value associated with the key, or null if there was no mapping for the key.
     */
    @CanIgnoreReturnValue
    public V put(K k, V v) {
        synchronized (lock) {
            V res = source.put(k, v);
            removeMap.put(k, Pair.of(ticks + cashLifeTime, v));
            return res;
        }
    }

    /**
     * Gets the value associated with the key, if it exists, and resets its expiration time.
     *
     * @param k The key.
     * @return The value associated with the key, or null if there is no mapping for the key.
     */
    @Nullable
    public V get(K k) {
        synchronized (lock) {
            V v = source.get(k);
            if (v != null) {
                removeMap.put(k, Pair.of(ticks + cashLifeTime, v));
            }
            return v;
        }
    }

    /**
     * Computes the value if absent and puts it into the map.
     *
     * @param k       The key.
     * @param creator The function to compute the value if absent.
     * @return The current (existing or computed) value associated with the key.
     */
    public V computeIfAbsent(K k, Function<K, V> creator) {
        V v = get(k);
        if (v == null) {
            v = creator.apply(k);
            put(k, v);
        }
        return v;
    }

    /**
     * Gets the value associated with the key, or returns the default value if there is no mapping.
     *
     * @param k   The key.
     * @param def The default value to return if there is no mapping.
     * @return The value associated with the key, or the default value if there is no mapping.
     */
    public V getOrDefault(K k, V def) {
        return getOrDefault(k, () -> def);
    }

    /**
     * Gets the value associated with the key, or returns the value from the supplier if there is no mapping.
     *
     * @param k   The key.
     * @param def The supplier of the default value to return if there is no mapping.
     * @return The value associated with the key, or the value from the supplier if there is no mapping.
     */
    public V getOrDefault(K k, Supplier<V> def) {
        synchronized (lock) {
            V v = get(k);
            return v == null ? def.get() : v;
        }
    }

    /**
     * Checks if the map contains a mapping for the key.
     *
     * @param k The key.
     * @return True if the map contains a mapping for the key, false otherwise.
     */
    public boolean containsKey(K k) {
        synchronized (lock) {
            return source.containsKey(k);
        }
    }

    /**
     * Removes the mapping for the key from the map if it is present.
     *
     * @param k The key.
     * @return The previous value associated with the key, or null if there was no mapping for the key.
     */
    @CanIgnoreReturnValue
    public V remove(K k) {
        synchronized (lock) {
            V v = source.remove(k);
            removeMap.remove(k);
            return v;
        }

    }

    /**
     * Returns a set view of the mappings contained in the map.
     *
     * @return A set view of the mappings contained in the map.
     */
    public Set<Map.Entry<K, V>> entrySet() {
        synchronized (lock) {
            return source.entrySet();
        }
    }

    /**
     * Sets a callback to be executed when an entry is removed from the map.
     *
     * @param consumer The callback to be executed.
     */
    public void onExpiration(Consumer<Pair<K, V>> consumer) {
        expirationCallBack = consumer;
    }

    /**
     * Cancels the scheduled task and stops the automatic removal of entries.
     */
    public void close() {
        task.cancel();
    }
    public boolean isEmpty(){
        return source.isEmpty();
    }
}
