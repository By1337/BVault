package org.by1337.bvault.core.db;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SwapableDatabase implements Database {
    private Database source;

    public SwapableDatabase(Database source) {
        this.source = source;
    }

    @Override
    public CompletableFuture<User> getUser(UUID uuid) {
        return source.getUser(uuid);
    }

    @Override
    public void flushUser(User user, String bank) {
        source.flushUser(user, bank);
    }

    @Override
    public void close() {
        source.close();
    }

    @Override
    public CompletableFuture<Void> clearDb(String bank) {
        return source.clearDb(bank);
    }

    @Override
    public CompletableFuture<Void> clearDb() {
        return source.clearDb();
    }

    public Database getSource() {
        return source;
    }

    public void setSource(Database source) {
        this.source = source;
    }
}
