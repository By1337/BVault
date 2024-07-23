/*
package org.by1337.bvault.core.db;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.by1337.bvault.core.top.BalTop;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileDatabaseTest {
    @Mock
    private Plugin plugin;
    @Mock
    private BalTop balTop;
    @Mock
    private Player player;
    private boolean playerIsOnline = true;
    @Mock
    private Player offlinePlayer;
    @Mock
    private Server server;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private ExecutorService executorService;
    private FileDatabase fileDataBase;
    private File dataFolder;
    private UUID playerUUID;
    private final List<Runnable> tickMap = new ArrayList<>();
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        dataFolder = new File("./tempData");
        dataFolder.mkdirs();
        clearDirectory(dataFolder);
        playerUUID = UUID.randomUUID();
        UUID offlinePlayerUUID = UUID.randomUUID();
        when(offlinePlayer.getUniqueId()).thenReturn(offlinePlayerUUID);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(player.isOnline()).thenAnswer(invocation -> playerIsOnline);
        when(server.getPlayer(any(UUID.class))).thenAnswer(invocation -> {
            if (invocation.getArgument(0).equals(playerUUID) && player.isOnline()) return player;
            return null;
        });

        when(plugin.getLogger()).thenReturn(Logger.getGlobal());
        when(player.getUniqueId()).thenReturn(playerUUID);
        when(server.getScheduler()).thenReturn(scheduler);

        when(scheduler.runTaskTimerAsynchronously(any(Plugin.class), any(Runnable.class), anyLong(), anyLong())).thenAnswer((Answer<BukkitTask>) invocation -> {
            tickMap.add(invocation.getArgument(1));
            return mock(BukkitTask.class);
        });
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        fileDataBase = new FileDatabase(dataFolder, plugin, balTop, executorService);
    }

    private void tick(long count) {
        for (long i = 0; i < count; i++) {
            for (Runnable runnable : tickMap) {
                runnable.run();
            }
        }
    }

    @Test
    void clearCash2Test() {
        assertNotNull(fileDataBase.getUser(UUID.randomUUID()).join());
        assertFalse(fileDataBase.userCash2.isEmpty());
        tick(6);
        assertTrue(fileDataBase.userCash2.isEmpty());
    }

    @Test
    @SuppressWarnings("deprecation")
    void onQuitTest() throws InterruptedException {
        fileDataBase.onJoin(new PlayerJoinEvent(player, (String) null));
        assertFalse(fileDataBase.userCash.isEmpty());
        assertTrue(fileDataBase.userCash2.isEmpty());
        fileDataBase.onQuit(new PlayerQuitEvent(player, (String) null));
        assertTrue(fileDataBase.userCash.isEmpty());
        assertFalse(fileDataBase.userCash2.isEmpty());
        tick(6);
        assertFalse(fileDataBase.userCash2.isEmpty());
        playerIsOnline = false;
        tick(6);
        assertTrue(fileDataBase.userCash2.isEmpty());
        playerIsOnline = true;
    }

    @Test
    void editCashTest() throws InterruptedException {
        User user = fileDataBase.getUser(UUID.randomUUID()).join();
        user.deposit("test", 1D);
        user.flush();
        assertFalse(fileDataBase.editCash.isEmpty());
        tick(6);
        assertTrue(fileDataBase.editCash.isEmpty());
        user = fileDataBase.getUser(user.getUuid()).join();
        assertTrue(new File(dataFolder, user.getUuid().toString() + ".bnbt").exists());
        assertEquals(user.getBalance("test"), 1D);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
        clearDirectory(dataFolder);
        Files.delete(dataFolder.toPath());
    }

    private void clearDirectory(File file) {
        if (!file.exists() || !file.isDirectory() || file.listFiles() == null) return;
        for (File listFile : file.listFiles()) {
            clearDirectory(listFile);
            listFile.delete();
        }
    }

}*/
