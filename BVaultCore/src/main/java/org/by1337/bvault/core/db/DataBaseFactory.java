package org.by1337.bvault.core.db;

import org.bukkit.plugin.Plugin;
import org.by1337.blib.configuration.YamlContext;

import java.io.File;

public enum DataBaseFactory {
    SQLITE("sqlite", (plugin, cfg) -> new SqliteDatabase(plugin)),
    MYSQL("mysql", MysqlDataBase::new),
    FILE("file", (plugin, cfg) -> new FileDataBase(new File(plugin.getDataFolder(), "data"), plugin)),
    NONE("none", (plugin, cfg) -> new EmptyDataBase());

    private final String id;
    private final Creator creator;

    DataBaseFactory(String id, Creator creator) {
        this.id = id;
        this.creator = creator;
    }

    public static DataBase create(Plugin plugin, YamlContext cfg) {
        String type = cfg.getAsString("type");
        for (DataBaseFactory value : values()) {
            if (value.id.equalsIgnoreCase(type) || value.name().equalsIgnoreCase(type)) {
                return value.creator.create(plugin, cfg);
            }
        }
        throw new IllegalArgumentException("Unknown database type: " + type);
    }

    private interface Creator {
        DataBase create(Plugin plugin, YamlContext cfg);
    }
}
