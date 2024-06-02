package org.by1337.bvault.core.impl;

import org.by1337.bvault.api.BEconomy;
import org.by1337.bvault.core.db.DataBase;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BEconomyImpl extends BEconomy {
    private final DataBase dataBase;

    public BEconomyImpl(DataBase dataBase) {
        this.dataBase = dataBase;
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
     * Withdraws a specified amount of money from the player's account in the specified bank.
     * To be implemented by subclasses.
     *
     * @param bank   The name of the bank.
     * @param player The UUID of the player.
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
     * Deposits a specified amount of money into the player's account in the specified bank.
     * To be implemented by subclasses.
     *
     * @param bank   The name of the bank.
     * @param player The UUID of the player.
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
     * Gets the balance of the player's account in the specified bank.
     * To be implemented by subclasses.
     *
     * @param bank   The name of the bank.
     * @param player The UUID of the player.
     * @return A CompletableFuture containing the balance.
     */
    @Override
    protected CompletableFuture<Double> getBalance0(@NotNull String bank, @NotNull UUID player) {
        return CompletableFuture.supplyAsync(() -> dataBase.getUser(player).join().getBalance(bank));
    }
}
