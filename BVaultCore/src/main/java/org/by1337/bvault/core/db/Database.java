package org.by1337.bvault.core.db;

import org.by1337.bvault.core.db.User;
import org.by1337.bvault.core.top.TopInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Database {
    CompletableFuture<@NotNull User> getUser(@NotNull UUID uuid);

    void flushUser(@NotNull User user, @NotNull String bank);

    void close();

    CompletableFuture<@Nullable Void> dropBalancesIn(@NotNull String bank);

    CompletableFuture<@Nullable Void> dropBalances();

    Set<@NotNull String> getKnownBanks();

    CompletableFuture<@NotNull List<@NotNull TopInfo>> getTopByBank(@NotNull String bank, int limit);
}
