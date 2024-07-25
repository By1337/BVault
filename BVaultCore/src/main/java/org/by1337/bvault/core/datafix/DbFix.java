package org.by1337.bvault.core.datafix;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.by1337.blib.configuration.YamlConfig;
import org.by1337.blib.configuration.YamlContext;
import org.by1337.blib.nbt.DefaultNbtByteBuffer;
import org.by1337.blib.nbt.NbtType;
import org.by1337.blib.nbt.impl.CompoundTag;
import org.by1337.bvault.core.BVaultCore;
import org.by1337.bvault.core.util.ConfigUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class DbFix {
    public static final int CURRENT_VERSION = 2;
    private YamlConfig cfg;
    private int version;

    public void run(Plugin plugin) {
        cfg = ConfigUtil.load("config.yml", plugin);
        version = cfg.getAsInteger("version", 1);
        if (version != CURRENT_VERSION) {
            update(version, plugin, cfg);
            cfg.set("version", CURRENT_VERSION);
            try {
                cfg.save();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void update(int version, Plugin plugin, YamlContext cfg) {
        if (version == 1) {
            deleteFileOrFolder(new File(plugin.getDataFolder(), "topData"));
            YamlContext context = new YamlContext(new YamlConfiguration());
            context.set("decimal-format", "#.##");
            context.set("thousand-separator", " ");
            context.set("integer-separator", ".");
            cfg.set("balTop.format", context);
        }
    }

    public void postEnabled(BVaultCore core) {
        if (version == 1) {
            core.getMessage().log("Starting update db...");
            String dbType = cfg.getAsString("dataBase.type");
            if ("file".equals(dbType)) {
                File data = new File(core.getDataFolder(), "data");
                if (data.exists() && data.isDirectory()) {
                    for (File file : data.listFiles()) {
                        core.getMessage().log("[UPDATER] Read file: %s", file.getPath());
                        if (file.getName().endsWith(".bnbt")) {
                            try {
                                DefaultNbtByteBuffer buffer = new DefaultNbtByteBuffer(Files.readAllBytes(file.toPath()));
                                CompoundTag tag = (CompoundTag) NbtType.COMPOUND.read(buffer);
                                UUID uuid = tag.getAsUUID("uuid");
                                CompoundTag balances = tag.getAsCompoundTag("balances");
                                for (String s : balances.getTags().keySet()) {
                                    core.getEconomy().deposit(s, uuid, balances.getAsDouble(s));
                                }
                            } catch (IOException e) {
                                core.getMessage().error("Failed to read file %s!", e, file.getPath());
                            }
                        }
                    }
                }
                deleteFileOrFolder(data);
            } else if ("sqlite".equals(dbType)) {
                File data = new File(core.getDataFolder(), "data.db");
                core.getMessage().log("[UPDATER] Read file: %s", data.getPath());
                if (data.exists()) {
                    HikariConfig hikariConfig = new HikariConfig();
                    hikariConfig.setJdbcUrl(String.format("jdbc:sqlite:%s", new File(core.getDataFolder(), "data.db").getPath()));
                    updateOld(hikariConfig, core);
                }
                deleteFileOrFolder(data);
            } else if ("mysql".equals(dbType)) {
                YamlContext dbCfg = cfg.get("dataBase").getAsYamlContext();
                HikariConfig hikariConfig = new HikariConfig();
                hikariConfig.setMaximumPoolSize(dbCfg.getAsInteger("maxPoolSize"));
                hikariConfig.setPassword(dbCfg.getAsString("password"));
                hikariConfig.setUsername(dbCfg.getAsString("user"));
                hikariConfig.setJdbcUrl(String.format("jdbc:mysql://%s:%s/%s",
                        dbCfg.getAsString("host"),
                        dbCfg.getAsString("port"),
                        dbCfg.getAsString("dbName")
                ));
                updateOld(hikariConfig, core);
            }
        }
    }
    @SuppressWarnings("SqlResolve")
    private void updateOld(HikariConfig config, BVaultCore core){
        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM 'user_balance'")
            ) {
                ResultSet resultSet = statement.executeQuery();
                int counter = 0;
                while (resultSet.next()) {
                    String bank_name = resultSet.getString("bank_name");
                    String user_id = resultSet.getString("user_id");
                    double balance = resultSet.getDouble("balance");
                    core.getEconomy().deposit(bank_name, UUID.fromString(user_id), balance);
                    counter++;
                }
                core.getMessage().log("Loaded %s users from an outdated database", counter);
            } catch (SQLException e) {
                core.getMessage().error("Failed to get old data!", e);
            }
        }
    }

    private void deleteFileOrFolder(File file) {
        if (!file.exists()) return;
        if (file.isFile()) {
            file.delete();
        } else {
            for (File listFile : file.listFiles()) {
                deleteFileOrFolder(listFile);
            }
            file.delete();
        }

    }
}
