package org.by1337.bvault.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public abstract class BEconomy {
    public static String DEFAULT_BANK = "vault";

    /**
     * Gets the name of the economy system.
     *
     * @return The name of the economy system.
     */
    public abstract String getName();

    /**
     * Withdraws a specified amount of money from the player's account in the default bank.
     *
     * @param player The UUID of the player.
     * @param amount The amount of money to withdraw.
     * @return A CompletableFuture containing the new balance after the withdrawal.
     * @throws NullPointerException if the player is null.
     * @throws IllegalArgumentException if the amount is not positive.
     */
    public CompletableFuture<Double> withdraw(@NotNull UUID player, double amount) {
        Objects.requireNonNull(player, "Player is null!");
        Validate.assertPositive(amount);
        return withdraw0(DEFAULT_BANK, player, amount);
    }

    /**
     * Withdraws a specified amount of money from the player's account in the specified bank.
     *
     * @param bank   The name of the bank.
     * @param player The UUID of the player.
     * @param amount The amount of money to withdraw.
     * @return A CompletableFuture containing the new balance after the withdrawal.
     * @throws NullPointerException if the player or bank is null.
     * @throws IllegalArgumentException if the amount is not positive.
     */
    public CompletableFuture<Double> withdraw(@NotNull String bank, @NotNull UUID player, double amount) {
        Objects.requireNonNull(player, "Player is null!");
        Objects.requireNonNull(bank, "bank is null!");
        Validate.assertPositive(amount);
        Validate.charactersCheck(bank);
        Validate.checkToLarge(bank, 16);
        return withdraw0(bank, player, amount);
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
    protected abstract CompletableFuture<Double> withdraw0(@NotNull String bank, @NotNull UUID player, double amount);

    /**
     * Deposits a specified amount of money into the player's account in the default bank.
     *
     * @param player The UUID of the player.
     * @param amount The amount of money to deposit.
     * @return A CompletableFuture containing the new balance after the deposit.
     * @throws NullPointerException if the player is null.
     * @throws IllegalArgumentException if the amount is not positive.
     */
    public CompletableFuture<Double> deposit(@NotNull UUID player, double amount) {
        Objects.requireNonNull(player, "Player is null!");
        Validate.assertPositive(amount);
        return deposit0(DEFAULT_BANK, player, amount);
    }

    /**
     * Deposits a specified amount of money into the player's account in the specified bank.
     *
     * @param bank   The name of the bank.
     * @param player The UUID of the player.
     * @param amount The amount of money to deposit.
     * @return A CompletableFuture containing the new balance after the deposit.
     * @throws NullPointerException if the player or bank is null.
     * @throws IllegalArgumentException if the amount is not positive.
     */
    public CompletableFuture<Double> deposit(@NotNull String bank, @NotNull UUID player, double amount) {
        Objects.requireNonNull(player, "Player is null!");
        Objects.requireNonNull(bank, "bank is null!");
        Validate.assertPositive(amount);
        Validate.charactersCheck(bank);
        Validate.checkToLarge(bank, 16);
        return deposit0(bank, player, amount);
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
    protected abstract CompletableFuture<Double> deposit0(@NotNull String bank, @NotNull UUID player, double amount);

    /**
     * Gets the balance of the player's account in the default bank.
     *
     * @param player The UUID of the player.
     * @return A CompletableFuture containing the balance.
     * @throws NullPointerException if the player is null.
     */
    public CompletableFuture<Double> getBalance(@NotNull UUID player) {
        Objects.requireNonNull(player, "Player is null!");
        return getBalance0(DEFAULT_BANK, player);
    }

    /**
     * Gets the balance of the player's account in the specified bank.
     *
     * @param bank   The name of the bank.
     * @param player The UUID of the player.
     * @return A CompletableFuture containing the balance.
     * @throws NullPointerException if the player or bank is null.
     */
    public CompletableFuture<Double> getBalance(@NotNull String bank, @NotNull UUID player) {
        Objects.requireNonNull(player, "Player is null!");
        Objects.requireNonNull(bank, "bank is null!");
        Validate.charactersCheck(bank);
        Validate.checkToLarge(bank, 16);
        return getBalance0(bank, player);
    }

    /**
     * Gets the balance of the player's account in the specified bank.
     * To be implemented by subclasses.
     *
     * @param bank   The name of the bank.
     * @param player The UUID of the player.
     * @return A CompletableFuture containing the balance.
     */
    protected abstract CompletableFuture<Double> getBalance0(@NotNull String bank, @NotNull UUID player);

    /**
     * Gets the set of all existed player's bank accounts.
     *
     * @param player The UUID of the player.
     * @return A CompletableFuture containing the banks.
     * @throws NullPointerException if the player is null.
     */
    public CompletableFuture<Set<String>> getExistedBanks(@NotNull UUID player) {
        Objects.requireNonNull(player, "Player is null!");
        return getExistedBanks0(player);
    }

    /**
     * Gets the set of all existed player's bank accounts.
     * To be implemented by subclasses.
     *
     * @param player The UUID of the player.
     * @return A CompletableFuture containing the banks.
     */
    protected abstract CompletableFuture<Set<String>> getExistedBanks0(@NotNull UUID player);

    /**
     * Returns an unmodifiable set of all known banks.
     * @return a set of known banks.
     */
    public abstract Set<String> getKnownBanks();

}
