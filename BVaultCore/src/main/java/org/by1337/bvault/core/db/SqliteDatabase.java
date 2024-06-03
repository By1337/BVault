package org.by1337.bvault.core.db;

import com.zaxxer.hikari.HikariConfig;
import org.bukkit.plugin.Plugin;
import org.by1337.bvault.core.top.BalTop;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class SqliteDatabase extends SqlDataBase {

    public SqliteDatabase(Plugin plugin, BalTop balTop) {
        super(createHikariConfig(plugin), plugin, balTop);
    }

    private static HikariConfig createHikariConfig( Plugin plugin) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(String.format("jdbc:sqlite:%s", new File(plugin.getDataFolder(), "data.db").getPath()));
        return hikariConfig;
    }
    @Override
    public void flushUser(User user, String bank) {

        ioExecutor.execute(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO user_balance (bank_name, user_id, balance)
                     VALUES (?, ?, ?)
                     ON CONFLICT(bank_name, user_id) DO UPDATE SET balance = excluded.balance;
                      """)
            ) {
                Double balance = user.getBalance(bank);
                balTop.updateBalance(user.getUuid(), balance, bank);
                statement.setString(1, bank);
                statement.setString(2, user.getUuid().toString());
                statement.setDouble(3, balance);
                statement.execute();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to flush user!", e);
            }
        });
    }

}