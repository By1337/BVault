package org.by1337.bvault.core;

import com.google.common.base.Charsets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.by1337.blib.chat.util.Message;
import org.by1337.blib.command.Command;
import org.by1337.blib.command.CommandException;
import org.by1337.blib.configuration.YamlConfig;
import org.by1337.bvault.api.BEconomy;
import org.by1337.bvault.core.db.Database;
import org.by1337.bvault.core.db.DisabledDatabase;
import org.by1337.bvault.core.db.SqliteDatabase;
import org.by1337.bvault.core.db.SwapableDatabase;
import org.by1337.bvault.core.hook.DefaultVaultEconomyAdapter;
import org.by1337.bvault.core.hook.PAPIHook;
import org.by1337.bvault.core.impl.BEconomyImpl;
import org.by1337.bvault.core.top.BalTop;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BVaultCore extends JavaPlugin {
    private Database dataBase;
    private SwapableDatabase swapableDatabase;
    private Command<CommandSender> command;
    private Message message;
    private YamlConfig config;
    private BalTop balTop;
    private PAPIHook papiHook;
    private Map<String, String> lang;

    @Override
    public void onLoad() {
        message = new Message(getLogger());
        try {
            config = new YamlConfig(trySave("config.yml"));
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
        lang = config.getMap("lang", String.class);
        swapableDatabase = new SwapableDatabase(new DisabledDatabase());

        BEconomy bEconomy = new BEconomyImpl(swapableDatabase);
        Bukkit.getServicesManager().register(BEconomy.class, bEconomy, this, ServicePriority.Lowest);
        Bukkit.getServicesManager().register(Economy.class, new DefaultVaultEconomyAdapter(bEconomy), this, ServicePriority.High);
    }

    private void setStaticField(Class<?> in, String field, Object value) {
        try {
            Field field1 = in.getDeclaredField(field);
            field1.setAccessible(true);
            field1.set(null, value);
        } catch (Exception t) {
            throw new RuntimeException(t);
        }
    }


    @Override
    public void onEnable() {
        balTop = new BalTop(this, config.getAsInteger("balTop.size", 100));
        // dataBase = DataBaseFactory.create(this, config.getAsYamlValue("dataBase").getAsYamlContext(), balTop);
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(String.format("jdbc:sqlite:%s", new File(getDataFolder(), "data2.db").getPath()));

        dataBase = new SqliteDatabase(hikariConfig, this, balTop);

        swapableDatabase.setSource(dataBase);

        command = new Commands().create(this);
        papiHook = new PAPIHook(config.getAsYamlValue("balTop").getAsYamlContext(), balTop, this);
        papiHook.register();
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregisterAll(this);
        dataBase.close();
        balTop.close();
        papiHook.unregister();
    }


    public Message getMessage() {
        return message;
    }

    public BEconomy getEconomy() {
        RegisteredServiceProvider<BEconomy> provider = Bukkit.getServicesManager().getRegistration(BEconomy.class);
        return Objects.requireNonNull(provider, "Economy provider not found!").getProvider();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        return command.getTabCompleter(sender, args);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        try {
            command.process(sender, args);
            return true;
        } catch (CommandException e) {
            sender.sendMessage(e.getMessage());
        } catch (Throwable t) {
            message.error(t);
        }
        return false;
    }

    @CanIgnoreReturnValue
    public File trySave(String path) {
        path = path.replace('\\', '/');
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        File f = new File(getDataFolder(), path);
        if (!f.exists()) {
            saveResource(path, false);
        }
        return f;
    }

    public Map<String, String> getLang() {
        return lang;
    }
}
