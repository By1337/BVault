package org.by1337.bvault.core.db;


import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EmptyDatabase implements Database {
    @Override
    public CompletableFuture<User> getUser(UUID uuid) {
        throw new UnsupportedOperationException("Database selected as none");
    }

    @Override
    public void flushUser(User user, String bank) {
        throw new UnsupportedOperationException("Database selected as none");
    }

    @Override
    public void close() {
    }

    @Override
    public CompletableFuture<Void> clearDb(String bank) {
        throw new UnsupportedOperationException("Database selected as none");
    }

    @Override
    public CompletableFuture<Void> clearDb() {
        throw new UnsupportedOperationException("Database selected as none");
    }
}
