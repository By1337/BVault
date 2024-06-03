package org.by1337.bvault.core.top;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.bukkit.plugin.Plugin;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

    @BeforeEach
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(server.getPlayer(any(UUID.class))).thenAnswer(invocation -> null);
        Mockito.when(plugin.getDataFolder()).thenReturn(new File("testData"));
        balTop = new BalTop(plugin, TOP_SIZE);
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
        balTop.updateBalance(player1, 100.0, "bank1");
        balTop.updateBalance(player2, 200.0, "bank1");
        Thread.sleep(15);

        List<TopInfo> topList = balTop.getTop("bank1", 2);
        assertEquals(2, topList.size());
        assertEquals(player2, topList.get(0).player());
        assertEquals(200.0, topList.get(0).balance());
        assertEquals(player1, topList.get(1).player());
        assertEquals(100.0, topList.get(1).balance());
        balTop.clear();
    }

    @Test
    public void testUpdateBalanceNewUser() throws InterruptedException {
        UUID player1 = UUID.randomUUID();
        balTop.updateBalance(player1, 100.0, "bank1");
        Thread.sleep(15);
        List<TopInfo> topList = balTop.getTop("bank1", 1);
        assertEquals(player1, topList.get(0).player());
        assertEquals(100.0, topList.get(0).balance());
        balTop.clear();
    }

    @Test
    public void testUpdateBalance() throws InterruptedException {
        UUID player1 = UUID.randomUUID();
        balTop.updateBalance(player1, 100.0, "bank1");
        Thread.sleep(15);
        List<TopInfo> topList = balTop.getTop("bank1", 1);
        assertEquals(player1, topList.get(0).player());
        assertEquals(100.0, topList.get(0).balance());

        balTop.updateBalance(player1, 200.0, "bank1");
        Thread.sleep(15);
        topList = balTop.getTop("bank1", 1);
        assertEquals(player1, topList.get(0).player());
        assertEquals(200.0, topList.get(0).balance());
        balTop.clear();
    }

    @Test
    public void testUpdateBalanceExistingUser() throws InterruptedException {
        UUID player1 = UUID.randomUUID();
        balTop.updateBalance(player1, 100.0, "bank1");
        balTop.updateBalance(player1, 150.0, "bank1");
        Thread.sleep(15);

        List<TopInfo> topList = balTop.getTop("bank1", 1);
        assertEquals(player1, topList.get(0).player());
        assertEquals(150.0, topList.get(0).balance());
        balTop.clear();
    }
    @AfterEach
    public void tearDown() throws Exception {
        File dataFolder = new File("testData/topData");
        if (dataFolder.exists()) {
            for (File file : dataFolder.listFiles()) {
                file.delete();
            }
            dataFolder.delete();
        }
        closeable.close();
    }

    @Test
    public void testSaveAndLoad() throws Exception {
        UUID player1 = UUID.randomUUID();
        balTop.updateBalance(player1, 100.0, "bank1");
        Thread.sleep(15);

        balTop.save();

        BalTop newBalTop = new BalTop(plugin, TOP_SIZE);
        List<TopInfo> topList = newBalTop.getTop("bank1", 1);
        assertEquals(player1, topList.get(0).player());
        assertEquals(100.0, topList.get(0).balance());
        balTop.clear();
    }

    @Test
    public void testOnJoin() throws Exception {
        UUID playerUUID = UUID.randomUUID();
        String playerName = "testPlayer";

        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(playerUUID);
        Mockito.when(player.getName()).thenReturn(playerName);

        PlayerJoinEvent event = new PlayerJoinEvent(player, (String) null);
        balTop.onJoin(event);

        try (Connection connection = balTop.uuidToNameDb.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT username FROM users WHERE uuid = ?")) {
            statement.setString(1, playerUUID.toString());
            var resultSet = statement.executeQuery();
            assertTrue(resultSet.next());
            assertEquals(playerName, resultSet.getString("username"));
        }
    }
}
