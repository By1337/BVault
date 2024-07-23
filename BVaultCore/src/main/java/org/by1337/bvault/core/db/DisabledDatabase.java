package org.by1337.bvault.core.db;

import org.by1337.bvault.core.top.TopInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DisabledDatabase implements Database {


    @Override
    public CompletableFuture<User> getUser(@NotNull UUID uuid) {
        throw new UnsupportedOperationException("BVault is disabled!");
    }

    @Override
    public void flushUser(@NotNull User user, @NotNull String bank) {
        throw new UnsupportedOperationException("BVault is disabled!");
    }

    @Override
    public void close() {
    }

    @Override
    public CompletableFuture<Void> dropBalancesIn(@NotNull String bank) {
        throw new UnsupportedOperationException("BVault is disabled!");
    }

    @Override
    public CompletableFuture<Void> dropBalances() {
        throw new UnsupportedOperationException("BVault is disabled!");
    }

    @Override
    public Set<String> getKnownBanks() {
        throw new UnsupportedOperationException("BVault is disabled!");
    }

    @Override
    public CompletableFuture<@NotNull List<@NotNull TopInfo>> getTopByBank(@NotNull String bank, int limit) {
        throw new UnsupportedOperationException("BVault is disabled!");
    }

}

