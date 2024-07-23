package org.by1337.bvault.core.hook;

import com.google.common.base.Joiner;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.by1337.blib.configuration.YamlContext;
import org.by1337.blib.hook.papi.Placeholder;
import org.by1337.blib.util.Pair;
import org.by1337.bvault.api.BEconomy;
import org.by1337.bvault.core.BVaultCore;
import org.by1337.bvault.core.top.BalTop;
import org.by1337.bvault.core.top.TopInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class PAPIHook extends PlaceholderExpansion {
    private final long cashTime;
    private final DecimalFormat decimalFormat;
    private final String thousandSeparator;
    private final String integerSeparator;
    private final String empty;
    private final BalTop balTop;
    private final Plugin plugin;
    private final Placeholder placeholder;
    private final Map<String, Pair<List<TopInfo>, Long>> cashedTops = new HashMap<>();
    private final int topSize;

    public PAPIHook(YamlContext context, BalTop balTop, Plugin plugin) {
        cashTime = TimeUnit.SECONDS.toMillis(context.getAsInteger("cashTime", 60));
        decimalFormat = new DecimalFormat(context.getAsString("balTop.format.decimal-format", "#.##"));
        thousandSeparator = context.getAsString("balTop.format.thousand-separator", " ");
        integerSeparator = context.getAsString("balTop.format.integer-separator", " ");
        empty = context.getAsString("emptyPos", "----");
        topSize = context.getAsInteger("size", 100);
        this.balTop = balTop;
        this.plugin = plugin;
        placeholder = new Placeholder("root");

        placeholder.addSubPlaceholder(new Placeholder("top_nick")
                .executor(((player, args) -> {
                    if (args.length != 2) return "use %bvault_top_nick_<bank>_<position>%";
                    try {
                        String bank = args[0];
                        int pos = Integer.parseInt(args[1]);
                        var v = getTop(bank).get(pos);
                        if (v == TopInfo.EMPTY) return empty;
                        return v.nickName();
                    } catch (NumberFormatException | NullPointerException e) {
                        plugin.getLogger().log(Level.SEVERE, "", e);
                        return "use %bvault_top_nick_<bank>_<position>%! gotten " + Joiner.on("_").join(args);
                    }
                }))
        );
        placeholder.addSubPlaceholder(new Placeholder("top_balance")
                .executor(((player, args) -> {
                    if (args.length != 2) return "use %bvault_top_balance_<bank>_<position>%";
                    try {
                        String bank = args[0];
                        int pos = Integer.parseInt(args[1]);
                        return formatNumberWithThousandsSeparator(decimalFormat.format(getTop(bank).get(pos).balance()));
                    } catch (NumberFormatException | NullPointerException e) {
                        plugin.getLogger().log(Level.SEVERE, "", e);
                        return "use %bvault_top_balance_<bank>_<position>%! gotten " + Joiner.on("_").join(args);
                    }
                }))
        );
        placeholder.addSubPlaceholder(new Placeholder("balance")
                .executor(((player, args) -> {
                    if (player == null) return "only for players";
                    String bank;
                    if (args.length == 1) {
                        bank = args[0];
                    } else {
                        bank = BEconomy.DEFAULT_BANK;
                    }
                    return formatNumberWithThousandsSeparator(
                            decimalFormat.format(
                                    getEconomy().getBalance(bank, player.getUniqueId()).join()
                            ));
                }))
        );
        placeholder.build();
    }

    private List<TopInfo> getTop(String bank) {
        synchronized (this) {
            var pair = cashedTops.get(bank);
            if (pair == null || pair.getRight() < System.currentTimeMillis()) {
                List<TopInfo> list = balTop.getTop(bank, topSize);
                cashedTops.put(bank, Pair.of(list, System.currentTimeMillis() + cashTime));
                return list;
            }
            return pair.getLeft();
        }
    }

    public BEconomy getEconomy() {
        RegisteredServiceProvider<BEconomy> provider = Bukkit.getServicesManager().getRegistration(BEconomy.class);
        return Objects.requireNonNull(provider, "Economy provider not found!").getProvider();
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        return placeholder.process(player, params.split("_"));
    }

    public String formatNumberWithThousandsSeparator(String raw) {
        StringBuilder formatted = new StringBuilder();
        String[] parts = raw.split("\\.");
        String integerPart = parts[0];
        String decimalPart = (parts.length > 1) ? integerSeparator + parts[1] : "";

        char[] integerDigits = integerPart.toCharArray();
        for (int i = integerDigits.length - 1, count = 0; i >= 0; i--, count++) {
            if (count > 0 && count % 3 == 0) {
                formatted.append(thousandSeparator);
            }
            formatted.append(integerDigits[i]);
        }
        return formatted.reverse() + decimalPart;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "BVault";
    }

    @Override
    public @NotNull String getAuthor() {
        return "By1337";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

}
