package org.by1337.bvault.core.top;

import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.bukkit.plugin.Plugin;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BalTopTest {
    @Mock
    private Plugin plugin;
    @Mock
    private Server server;
    @Mock
    private PluginManager pluginManager;
    private BalTop balTop;
    private static final int TOP_SIZE = 10;
    private AutoCloseable closeable;
    @Mock
    private ExecutorService executorService;

    @BeforeEach
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(server.getPlayer(any(UUID.class))).thenAnswer(invocation -> null);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));
        balTop = new BalTop(plugin, executorService, TOP_SIZE);
    }

    @Test
    public void testGetTopEmpty() {
        List<TopInfo> topList = balTop.getTop("bank1", 5);
        assertEquals(5, topList.size());
        for (TopInfo topInfo : topList) {
            assertEquals(TopInfo.EMPTY, topInfo);
        }
    }

    @Test
    public void testGetTopWithUsers() throws InterruptedException {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        balTop.updateBalance(player1, 100.0, "bank1", "unknown");
        balTop.updateBalance(player2, 200.0, "bank1", "unknown");

        List<TopInfo> topList = balTop.getTop("bank1", 2);
        assertEquals(2, topList.size());
        assertEquals(player2, topList.get(0).uuid());
        assertEquals(200.0, topList.get(0).balance());
        assertEquals(player1, topList.get(1).uuid());
        assertEquals(100.0, topList.get(1).balance());
        balTop.clear();
    }

    @Test
    public void testUpdateBalanceNewUser() throws InterruptedException {
        UUID player1 = UUID.randomUUID();
        balTop.updateBalance(player1, 100.0, "bank1", "unknown");
        List<TopInfo> topList = balTop.getTop("bank1", 1);
        assertEquals(player1, topList.get(0).uuid());
        assertEquals(100.0, topList.get(0).balance());
        balTop.clear();
    }

    @Test
    public void testUpdateBalance() throws InterruptedException {
        UUID player1 = UUID.randomUUID();
        balTop.updateBalance(player1, 100.0, "bank1", "unknown");
        List<TopInfo> topList = balTop.getTop("bank1", 1);
        assertEquals(player1, topList.get(0).uuid());
        assertEquals(100.0, topList.get(0).balance());

        balTop.updateBalance(player1, 200.0, "bank1", "unknown");
        topList = balTop.getTop("bank1", 1);
        assertEquals(player1, topList.get(0).uuid());
        assertEquals(200.0, topList.get(0).balance());
        balTop.clear();
    }

    @Test
    public void testUpdateBalanceExistingUser() throws InterruptedException {
        UUID player1 = UUID.randomUUID();
        balTop.updateBalance(player1, 100.0, "bank1", "unknown");
        balTop.updateBalance(player1, 150.0, "bank1", "unknown");

        List<TopInfo> topList = balTop.getTop("bank1", 1);
        assertEquals(player1, topList.get(0).uuid());
        assertEquals(150.0, topList.get(0).balance());
        balTop.clear();
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

}
