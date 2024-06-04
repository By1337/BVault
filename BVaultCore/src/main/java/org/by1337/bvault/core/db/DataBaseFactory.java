package org.by1337.bvault.core.db;

import org.bukkit.plugin.Plugin;
import org.by1337.blib.configuration.YamlContext;
import org.by1337.bvault.core.top.BalTop;

import java.io.File;

public enum DataBaseFactory {
    SQLITE("sqlite", (plugin, cfg, top) -> new SqliteDatabase(plugin, top)),
    MYSQL("mysql", MysqlDatabase::new),
    FILE("file", (plugin, cfg, top) -> new FileDatabase(new File(plugin.getDataFolder(), "data"), plugin, top)),
    NONE("none", (plugin, cfg, top) -> new EmptyDatabase());

    private final String id;
    private final Creator creator;

    DataBaseFactory(String id, Creator creator) {
        this.id = id;
        this.creator = creator;
    }

    public static Database create(Plugin plugin, YamlContext cfg, BalTop balTop) {
        String type = cfg.getAsString("type");
        for (DataBaseFactory value : values()) {
            if (value.id.equalsIgnoreCase(type) || value.name().equalsIgnoreCase(type)) {
                return value.creator.create(plugin, cfg, balTop);
            }
        }
        throw new IllegalArgumentException("Unknown database type: " + type);
    }

    private interface Creator {
        Database create(Plugin plugin, YamlContext cfg, BalTop balTop);
    }
}
