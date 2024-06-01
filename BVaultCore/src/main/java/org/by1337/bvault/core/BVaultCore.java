package org.by1337.bvault.core;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.by1337.blib.chat.util.Message;
import org.by1337.blib.command.Command;
import org.by1337.blib.command.CommandException;
import org.by1337.blib.command.argument.ArgumentIntegerAllowedMath;
import org.by1337.blib.command.argument.ArgumentPlayer;
import org.by1337.blib.command.argument.ArgumentString;
import org.by1337.blib.command.requires.RequiresPermission;
import org.by1337.bvault.api.BEconomy;
import org.by1337.bvault.core.db.DataBase;
import org.by1337.bvault.core.db.FileDataBase;
import org.by1337.bvault.core.hook.DefaultVaultEconomyAdapter;
import org.by1337.bvault.core.impl.BEconomyImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

public class BVaultCore extends JavaPlugin {
    private DataBase dataBase;
    private Command<CommandSender> command;
    private Message message;

    @Override
    public void onLoad() {
        message = new Message(getLogger());
    }

    private BEconomy getEconomy() {
        RegisteredServiceProvider<BEconomy> provider = Bukkit.getServicesManager().getRegistration(BEconomy.class);
        return provider.getProvider();
    }

    @Override
    public void onEnable() {
        dataBase = new FileDataBase(new File(getDataFolder(), "data"), this);
        BEconomy bEconomy = new BEconomyImpl(dataBase);
        Bukkit.getServicesManager().register(BEconomy.class, bEconomy, this, ServicePriority.Lowest);
        Bukkit.getServicesManager().register(Economy.class, new DefaultVaultEconomyAdapter(bEconomy), this, ServicePriority.High);

        command = new Command<CommandSender>("root")
                .requires(new RequiresPermission<>("bvault.use"))
                .addSubCommand(new Command<CommandSender>("balance")
                        .requires(new RequiresPermission<>("bvault.balance"))
                        .argument(new ArgumentPlayer<>("player"))
                        .argument(new ArgumentString<>("bank", List.of(BEconomy.DEFAULT_BANK)))
                        .executor(((sender, args) -> {
                            Player player = (Player) args.getOrThrow("player", "Use: /bv balance <player> <bank>");
                            String bank = (String) args.getOrDefault("bank", BEconomy.DEFAULT_BANK);
                            getEconomy().getBalance(bank, player.getUniqueId()).whenComplete((d, t) -> {
                                if (t != null) {
                                    message.error(t);
                                }
                                if (sender instanceof Player) { // no log
                                    if (d != null) {
                                        message.sendMsg(sender, "Игрок %s имеет %s монет в банке %s.",
                                                player.getName(),
                                                d,
                                                bank
                                        );
                                    } else {
                                        message.sendMsg(sender, "&cНе удалось получить баланс игрока :(");
                                    }
                                }
                            });
                        }))
                )
                .addSubCommand(new Command<CommandSender>("give")
                        .requires(new RequiresPermission<>("bvault.give"))
                        .argument(new ArgumentPlayer<>("player"))
                        .argument(new ArgumentString<>("bank", List.of(BEconomy.DEFAULT_BANK)))
                        .argument(new ArgumentIntegerAllowedMath<>("count", List.of("100", "1k", "1kk"), 0))
                        .executor(((sender, args) -> {
                            Player player = (Player) args.getOrThrow("player", "Use: /bv give <player> <bank> <count>");
                            String bank = (String) args.getOrDefault("bank", BEconomy.DEFAULT_BANK);
                            int count = (int) args.getOrThrow("count", "Use: /bv give <player> <bank> <count>");

                            getEconomy().deposit(bank, player.getUniqueId(), count).whenComplete((d, t) -> {
                                if (t != null) {
                                    message.error(t);
                                }
                                if (sender instanceof Player) { // no log
                                    if (d != null) {
                                        message.sendMsg(sender, "Игрок %s теперь имеет %s монет в банке %s.",
                                                player.getName(),
                                                d,
                                                bank
                                        );
                                    } else {
                                        message.sendMsg(sender, "&cНе удалось выполнить операцию :(");
                                    }
                                }
                            });

                        }))
                )
                .addSubCommand(new Command<CommandSender>("take")
                        .requires(new RequiresPermission<>("bvault.take"))
                        .argument(new ArgumentPlayer<>("player"))
                        .argument(new ArgumentString<>("bank", List.of(BEconomy.DEFAULT_BANK)))
                        .argument(new ArgumentIntegerAllowedMath<>("count", List.of("100", "1k", "1kk"), 0))
                        .executor(((sender, args) -> {
                            Player player = (Player) args.getOrThrow("player", "Use: /bv take <player> <bank> <count>");
                            String bank = (String) args.getOrDefault("bank", BEconomy.DEFAULT_BANK);
                            int count = (int) args.getOrThrow("count", "Use: /bv take <player> <bank> <count>");

                            getEconomy().withdraw(bank, player.getUniqueId(), count).whenComplete((d, t) -> {
                                if (t != null) {
                                    message.error(t);
                                }
                                if (sender instanceof Player) { // no log
                                    if (d != null) {
                                        message.sendMsg(sender, "Игрок %s теперь имеет %s монет в банке %s.",
                                                player.getName(),
                                                d,
                                                bank
                                        );
                                    } else {
                                        message.sendMsg(sender, "&cНе удалось выполнить операцию :(");
                                    }
                                }
                            });

                        }))
                )
                .addSubCommand(new Command<CommandSender>("ecoName")
                        .requires(new RequiresPermission<>("bvault.ecoName"))
                        .executor(((sender, args) -> {
                           message.sendMsg(sender, "Текущий поставщик экономики '%s'", getEconomy().getName());
                        }))
                )
        ;
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregisterAll(this);
        dataBase.close();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        try {
            command.process(sender, args);
            return true;
        } catch (CommandException e) {
            this.getLogger().log(Level.SEVERE, "", e);
            return false;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        return command.getTabCompleter(sender, args);
    }
}
