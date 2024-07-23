package org.by1337.bvault.core.impl;

import org.by1337.bvault.api.BEconomy;
import org.by1337.bvault.api.User;
import org.by1337.bvault.core.BVaultCore;
import org.by1337.bvault.core.db.Database;
import org.by1337.bvault.core.top.BalTop;
import org.by1337.bvault.core.top.TopInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BEconomyImpl extends BEconomy {
    private final Database dataBase;
    private final BVaultCore plugin;

    public BEconomyImpl(Database dataBase, BVaultCore plugin) {
        this.dataBase = dataBase;
        this.plugin = plugin;
    }

    public Database getDataBase() {
        return dataBase;
    }

    /**
     * Gets the name of the economy system.
     *
     * @return The name of the economy system.
     */
    @Override
    public String getName() {
        return "BVault";
    }

    /**
     * Withdraws a specified amount of money from the uuid's account in the specified bank.
     * To be implemented by subclasses.
     *
     * @param bank   The name of the bank.
     * @param player The UUID of the uuid.
     * @param amount The amount of money to withdraw.
     * @return A CompletableFuture containing the new balance after the withdrawal.
     */
    @Override
    protected CompletableFuture<Double> withdraw0(@NotNull String bank, @NotNull UUID player, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            var user = dataBase.getUser(player).join();
            var result = user.withdraw(bank, amount);
            user.flush();
            return result;
        });
    }

    /**
     * Deposits a specified amount of money into the uuid's account in the specified bank.
     * To be implemented by subclasses.
     *
     * @param bank   The name of the bank.
     * @param player The UUID of the uuid.
     * @param amount The amount of money to deposit.
     * @return A CompletableFuture containing the new balance after the deposit.
     */
    @Override
    protected CompletableFuture<Double> deposit0(@NotNull String bank, @NotNull UUID player, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            var user = dataBase.getUser(player).join();
            var result = user.deposit(bank, amount);
            user.flush();
            return result;
        });
    }

    /**
     * Gets the balance of the uuid's account in the specified bank.
     * To be implemented by subclasses.
     *
     * @param bank   The name of the bank.
     * @param player The UUID of the uuid.
     * @return A CompletableFuture containing the balance.
     */
    @Override
    protected CompletableFuture<Double> getBalance0(@NotNull String bank, @NotNull UUID player) {
        return CompletableFuture.supplyAsync(() -> dataBase.getUser(player).join().getBalance(bank));
    }

    @Override
    protected CompletableFuture<Set<String>> getExistedBanks0(@NotNull UUID player) {
        return CompletableFuture.supplyAsync(() -> dataBase.getUser(player).join().getExistedBanks());
    }

    @Override
    public Set<String> getKnownBanks() {
        return dataBase.getKnownBanks();
    }

    /**
     * Returns the top players by balance in a specific bank.
     *
     * @param bank  - the bank for which the top list will be created.
     * @param limit - the limit on the number of top positions. If the limit is less than zero or exceeds the limit specified in the config, the limit will be set to the number from the config.
     * @return a list of top users by balance.
     */
    @Override
    public List<User> getTopByBank(@NotNull String bank, int limit) {
        BalTop top = plugin.getBalTop();
        if (limit < 0 || limit > top.getTopSize()) {
            limit = top.getTopSize();
        }
        List<User> result = new ArrayList<>();
        List<TopInfo> list = top.getTop(bank, limit);
        for (TopInfo topInfo : list) {
            if (topInfo == TopInfo.EMPTY) continue;
            result.add(new User(topInfo.nickName(), topInfo.uuid(), topInfo.balance()));
        }
        return result;
    }
}
