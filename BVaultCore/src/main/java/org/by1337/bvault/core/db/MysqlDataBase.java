package org.by1337.bvault.core.db;

import com.zaxxer.hikari.HikariConfig;
import org.bukkit.plugin.Plugin;
import org.by1337.blib.configuration.YamlContext;
import org.by1337.bvault.core.top.BalTop;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

public class MysqlDataBase extends SqlDataBase {
    public MysqlDataBase(Plugin plugin, YamlContext cfg, BalTop balTop) {
        super(createHikariConfig(cfg), plugin,balTop);
    }

    private static HikariConfig createHikariConfig(YamlContext cfg) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setMaximumPoolSize(cfg.getAsInteger("maxPoolSize"));
        hikariConfig.setPassword(cfg.getAsString("password"));
        hikariConfig.setUsername(cfg.getAsString("user"));
        hikariConfig.setJdbcUrl(String.format("jdbc:mysql://%s:%s/%s",
                cfg.getAsString("host"),
                cfg.getAsString("port"),
                cfg.getAsString("dbName")
        ));
        return hikariConfig;
    }

    @Override
    public void flushUser(User user, String bank) {
        ioExecutor.execute(() -> {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO user_balance (bank_name, user_id, balance) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE balance = ?;"
                 )
            ) {
                Double balance = user.getBalance(bank);
                balTop.updateBalance(user.getUuid(), balance, bank);
                statement.setString(1, bank);
                statement.setString(2, user.getUuid().toString());
                statement.setDouble(3, balance);
                statement.setDouble(4, balance);
                statement.execute();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to flush user!", e);
            }
        });
    }
}
