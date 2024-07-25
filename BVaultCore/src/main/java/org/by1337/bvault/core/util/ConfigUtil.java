package org.by1337.bvault.core.util;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.bukkit.plugin.Plugin;
import org.by1337.blib.configuration.YamlConfig;

import java.io.File;

public class ConfigUtil {
    public static YamlConfig load(String path, Plugin plugin) {
        return tryRun(() -> new YamlConfig(trySave(path, plugin)));
    }

    @CanIgnoreReturnValue
    public static File trySave(String path, Plugin plugin) {
        path = path.replace('\\', '/');
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        var f = new File(plugin.getDataFolder(), path);
        if (!f.exists()) {
            plugin.saveResource(path, false);
        }
        return f;
    }

    public static <T> T tryRun(ThrowableRunnable<T> runnable) {
        try {
            return runnable.run();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    public interface ThrowableRunnable<T> {
        T run() throws Throwable;
    }
}
