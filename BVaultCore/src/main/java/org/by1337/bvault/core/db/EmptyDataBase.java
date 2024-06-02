package org.by1337.bvault.core.db;


import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EmptyDataBase implements DataBase {
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
}
