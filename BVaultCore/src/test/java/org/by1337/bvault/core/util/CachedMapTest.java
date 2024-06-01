package org.by1337.bvault.core.util;

import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.by1337.blib.util.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CachedMapTest {
    @Mock
    private Plugin plugin;
    @Mock
    private Server server;

    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private BukkitTask task;

    private Runnable cachedMapTask;

    private CachedMap<String, String> cachedMap;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskTimerAsynchronously(any(Plugin.class), any(Runnable.class), anyLong(), anyLong())).thenAnswer((Answer<BukkitTask>) invocation -> {
            cachedMapTask = invocation.getArgument(1);
            return task;
        });
        cachedMap = new CachedMap<>(1000, TimeUnit.MILLISECONDS, plugin, 1);
    }

    private void tick(long count) {
        for (long i = 0; i < count; i++) {
            cachedMapTask.run();
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void testPutAndGet() {
        cachedMap.put("key1", "value1");
        assertEquals("value1", cachedMap.get("key1"));
    }

    @Test
    void testRemove() {
        cachedMap.put("key1", "value1");
        assertEquals("value1", cachedMap.remove("key1"));
        assertNull(cachedMap.get("key1"));
    }

    @Test
    void testContainsKey() {
        cachedMap.put("key1", "value1");
        assertTrue(cachedMap.containsKey("key1"));
        cachedMap.remove("key1");
        assertFalse(cachedMap.containsKey("key1"));
    }

    @Test
    void testExpiration() {
        cachedMap.put("key1", "value1");
        assertEquals("value1", cachedMap.get("key1"));
        tick(10);
        assertNotNull(cachedMap.get("key1"));
        tick(10);
        assertNotNull(cachedMap.get("key1"));
        tick(21);
        assertNull(cachedMap.get("key1"));
    }

    @Test
    void testOnRemoveCallback() {
        AtomicBoolean backed = new AtomicBoolean(false);
        Consumer<Pair<String, String>> callback = pair -> {
            if (pair.getLeft().equals("key1") && pair.getRight().equals("value1")) {
                backed.set(true);
            }
        };
        cachedMap.onRemove(callback);
        cachedMap.put("key1", "value1");
        tick(21);
        if (!backed.get()) {
            throw new IllegalStateException("onRemove was not called!");
        }
    }

    @Test
    void testComputeIfAbsent() {
        Function<String, String> creator = mock(Function.class);
        when(creator.apply("key1")).thenReturn("value1");

        assertEquals("value1", cachedMap.computeIfAbsent("key1", creator));
        assertEquals("value1", cachedMap.get("key1"));
        verify(creator, times(1)).apply("key1");
    }

    @Test
    void testGetOrDefault() {
        assertEquals("default", cachedMap.getOrDefault("key1", "default"));
        cachedMap.put("key1", "value1");
        assertEquals("value1", cachedMap.getOrDefault("key1", "default"));
    }

    @Test
    void testGetOrDefaultWithSupplier() {
        Supplier<String> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn("default");

        assertEquals("default", cachedMap.getOrDefault("key1", supplier));
        cachedMap.put("key1", "value1");
        assertEquals("value1", cachedMap.get("key1"));
        verify(supplier, times(1)).get();
    }

    @Test
    void testEntrySet() {
        cachedMap.put("key1", "value1");
        assertEquals(1, cachedMap.entrySet().size());
    }

    @Test
    void testClose() {
        cachedMap.close();
        verify(task).cancel();
    }
}
