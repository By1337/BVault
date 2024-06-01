package org.by1337.bvault.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class BEconomy {
    public static String DEFAULT_BANK = "vault";

    public abstract String getName();

    public CompletableFuture<Double> withdraw(@NotNull UUID player, double amount) {
        Objects.requireNonNull(player, "Player is null!");
        Validate.assertPositive(amount);
        return withdraw0(DEFAULT_BANK, player, amount);
    }

    public CompletableFuture<Double> withdraw(@NotNull String bank, @NotNull UUID player, double amount) {
        Objects.requireNonNull(player, "Player is null!");
        Objects.requireNonNull(bank, "bank is null!");
        Validate.assertPositive(amount);
        return withdraw0(bank, player, amount);
    }

    protected abstract CompletableFuture<Double> withdraw0(@NotNull String bank, @NotNull UUID player, double amount);

    public CompletableFuture<Double> deposit(@NotNull UUID player, double amount) {
        Objects.requireNonNull(player, "Player is null!");
        Validate.assertPositive(amount);
        return withdraw0(DEFAULT_BANK, player, amount);
    }

    public CompletableFuture<Double> deposit(@NotNull String bank, @NotNull UUID player, double amount) {
        Objects.requireNonNull(player, "Player is null!");
        Objects.requireNonNull(bank, "bank is null!");
        Validate.assertPositive(amount);
        return withdraw0(bank, player, amount);
    }

    protected abstract CompletableFuture<Double> deposit0(@NotNull String bank, @NotNull UUID player, double amount);

    public CompletableFuture<Double> getBalance(@NotNull UUID player) {
        Objects.requireNonNull(player, "Player is null!");
        return getBalance0(DEFAULT_BANK, player);
    }

    public CompletableFuture<Double> getBalance(@NotNull String bank, @NotNull UUID player) {
        Objects.requireNonNull(player, "Player is null!");
        Objects.requireNonNull(bank, "bank is null!");
        return getBalance0(bank, player);
    }

    public abstract CompletableFuture<Double> getBalance0(@NotNull String bank, @NotNull UUID player);
}
