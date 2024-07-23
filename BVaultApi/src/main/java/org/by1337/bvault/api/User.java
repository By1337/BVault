package org.by1337.bvault.api;

import java.util.Objects;
import java.util.UUID;

/**
 * Provides information about a user in the top list.
 * Used in {@link BEconomy#getTopByBank(String, int)}
 * This class is intended to be used as a simple POJO.
 */
public final class User {
    private final String nickName;
    private final UUID uuid;
    private final double balance;

    public User(String nickName, UUID uuid, double balance) {
        this.nickName = nickName;
        this.uuid = uuid;
        this.balance = balance;
    }

    /**
     * THE PLAYER'S NICKNAME MAY NOT BE ACCURATE.
     * The player's nickname might be inaccurate:
     * 1. if the player was not online at the time the balance was first issued.
     * 2. if the server is running in online-mode, the player may change their nickname.
     * @return the player's nickname.
     */
    public String nickName() {
        return nickName;
    }

    /**
     * The player's UUID.
     * @return the player's UUID.
     */
    public UUID uuid() {
        return uuid;
    }

    /**
     * The player's balance.
     * @return the player's balance.
     */
    public double balance() {
        return balance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Double.compare(balance, user.balance) == 0 && Objects.equals(nickName, user.nickName) && Objects.equals(uuid, user.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nickName, uuid, balance);
    }
}
