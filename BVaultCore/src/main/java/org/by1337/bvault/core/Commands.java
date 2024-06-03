package org.by1337.bvault.core;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.by1337.blib.command.Command;
import org.by1337.blib.command.argument.ArgumentIntegerAllowedMath;
import org.by1337.blib.command.argument.ArgumentPlayer;
import org.by1337.blib.command.argument.ArgumentString;
import org.by1337.blib.command.requires.RequiresPermission;
import org.by1337.bvault.api.BEconomy;

import java.util.List;

public class Commands {

    public Command<CommandSender> create(BVaultCore core) {
        return new Command<CommandSender>("root")
                .requires(new RequiresPermission<>("bvault.use"))
                .addSubCommand(new Command<CommandSender>("balance")
                        .requires(new RequiresPermission<>("bvault.balance"))
                        .argument(new ArgumentPlayer<>("player"))
                        .argument(new ArgumentString<>("bank", List.of(BEconomy.DEFAULT_BANK)))
                        .executor(((sender, args) -> {
                            Player player = (Player) args.getOrThrow("player", "Use: /bv balance <player> <bank>");
                            String bank = (String) args.getOrDefault("bank", BEconomy.DEFAULT_BANK);
                            core.getEconomy().getBalance(bank, player.getUniqueId()).whenComplete((d, t) -> {
                                if (t != null) {
                                    core.getMessage().error(t);
                                }
                                if (sender instanceof Player) { // no log
                                    if (d != null) {
                                        core.getMessage().sendMsg(sender, core.getLang().get("hasMoney"),
                                                player.getName(),
                                                d,
                                                bank
                                        );
                                    } else {
                                        core.getMessage().sendMsg(sender, core.getLang().get("failedToGetBalance"));
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

                            core.getEconomy().deposit(bank, player.getUniqueId(), count).whenComplete((d, t) -> {
                                if (t != null) {
                                    core.getMessage().error(t);
                                }
                                if (sender instanceof Player) { // no log
                                    if (d != null) {
                                        core.getMessage().sendMsg(sender, core.getLang().get("updateBalance"),
                                                player.getName(),
                                                d,
                                                bank
                                        );
                                    } else {
                                        core.getMessage().sendMsg(sender, core.getLang().get("failed"));
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

                            core.getEconomy().withdraw(bank, player.getUniqueId(), count).whenComplete((d, t) -> {
                                if (t != null) {
                                    core.getMessage().error(t);
                                }
                                if (sender instanceof Player) { // no log
                                    if (d != null) {
                                        core.getMessage().sendMsg(sender, core.getLang().get("updateBalance"),
                                                player.getName(),
                                                d,
                                                bank
                                        );
                                    } else {
                                        core.getMessage().sendMsg(sender, core.getLang().get("failed"));
                                    }
                                }
                            });

                        }))
                )
                .addSubCommand(new Command<CommandSender>("ecoName")
                        .requires(new RequiresPermission<>("bvault.ecoName"))
                        .executor(((sender, args) -> {
                            core.getMessage().sendMsg(sender, core.getLang().get("vaultProvider"), core.getEconomy().getName());
                        }))
                )
                ;
    }
}
