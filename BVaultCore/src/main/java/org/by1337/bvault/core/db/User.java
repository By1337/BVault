package org.by1337.bvault.core.db;

import org.by1337.bvault.api.Validate;

import java.util.*;

/**
 * Represents a User with a unique identifier and balances for multiple banks.
 */
public class User {
    // Lock object for synchronizing access to balances.
    private final Object lock = new Object();
    // Stores the initial balances of the user for comparison.
    final Map<String, Double> balancesOld;
    // Stores the current balances of the user.
    final Map<String, Double> balances;
    // Unique identifier for the user.
    private final UUID uuid;
    // Reference to the database for persistence operations.
    private final Database dataBase;

    /**
     * Creates a User with specified balances, UUID, and database reference.
     *
     * @param balances Initial balances for the user.
     * @param uuid     Unique identifier for the user.
     * @param dataBase Reference to the database.
     */
    User(Map<String, Double> balances, UUID uuid, Database dataBase) {
        this.balances = balances;
        this.uuid = uuid;
        this.dataBase = dataBase;
        this.balancesOld = new HashMap<>(balances);
    }

    /**
     * Creates a User with a specified UUID and database reference, initializing balances to empty.
     *
     * @param uuid     Unique identifier for the user.
     * @param dataBase Reference to the database.
     */
    User(UUID uuid, Database dataBase) {
        this.uuid = uuid;
        this.dataBase = dataBase;
        this.balances = new HashMap<>();
        this.balancesOld = new HashMap<>();
    }

    /**
     * Flushes the changes in balances to the database if there are any differences.
     */
    public void flush() {
        synchronized (lock) {
            for (String bank : balances.keySet()) {
                var oldBalance = balancesOld.get(bank);
                if (!Objects.equals(oldBalance, balances.get(bank))) {
                    dataBase.flushUser(this, bank);
                    balancesOld.put(bank, balances.get(bank));
                }
            }
        }
    }

    /**
     * Withdraws a specified amount from the balance of a specified bank.
     *
     * @param bank   The bank from which the amount will be withdrawn.
     * @param amount The amount to withdraw.
     * @return The new balance of the bank after the withdrawal.
     */
    public Double withdraw(String bank, Double amount) {
        synchronized (lock) {
            Validate.assertPositive(amount);
            var newBalance = balances.getOrDefault(bank, 0D) - amount;
            balances.put(bank, newBalance);
            return newBalance;
        }
    }

    /**
     * Deposits a specified amount to the balance of a specified bank.
     *
     * @param bank   The bank to which the amount will be deposited.
     * @param amount The amount to deposit.
     * @return The new balance of the bank after the deposit.
     */
    public Double deposit(String bank, Double amount) {
        synchronized (lock) {
            Validate.assertPositive(amount);
            var newBalance = balances.getOrDefault(bank, 0D) + amount;
            balances.put(bank, newBalance);
            return newBalance;
        }
    }

    /**
     * Retrieves the balance of a specified bank.
     *
     * @param bank The bank whose balance is to be retrieved.
     * @return The balance of the bank.
     */
    public Double getBalance(String bank) {
        synchronized (lock) {
            return balances.getOrDefault(bank, 0D);
        }
    }

    /**
     * Retrieves the all names of banks.
     *
     * @return The list with all existed bank accounts.
     */
    public Set<String> getExistedBanks() {
        synchronized (lock) {
            return balances.keySet();
        }
    }

    /**
     * Retrieves the unique identifier of the user.
     *
     * @return The UUID of the user.
     */
    public UUID getUuid() {
        return uuid;
    }

}
