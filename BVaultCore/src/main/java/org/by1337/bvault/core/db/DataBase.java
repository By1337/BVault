package org.by1337.bvault.core.db;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DataBase {
    CompletableFuture<User> getUser(UUID uuid);

    void flushUser(User user, String bank);
    void close();
}
