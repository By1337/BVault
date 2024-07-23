package org.by1337.bvault.core.db;

import org.by1337.bvault.core.top.TopInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SwapableDatabase implements Database {
    private Database source;

    public SwapableDatabase(Database source) {
        this.source = source;
    }

    public void setSource(Database source) {
        this.source = source;
    }

    @Override
    public CompletableFuture<User> getUser(@NotNull UUID uuid) {
        return source.getUser(uuid);
    }

    @Override
    public void flushUser(@NotNull User user, @NotNull String bank) {
        source.flushUser(user, bank);
    }

    @Override
    public void close() {
        source.close();
    }

    @Override
    public CompletableFuture<Void> dropBalancesIn(@NotNull String bank) {
        return source.dropBalancesIn(bank);
    }

    @Override
    public CompletableFuture<Void> dropBalances() {
        return source.dropBalances();
    }

    @Override
    public Set<String> getKnownBanks() {
        return source.getKnownBanks();
    }

    @Override
    public CompletableFuture<@NotNull List<@NotNull TopInfo>> getTopByBank(@NotNull String bank, int limit) {
        return source.getTopByBank(bank, limit);
    }

}

