# BVault

BVault is a multi-currency economy plugin for Minecraft.

## Description

The plugin provides its own API for interacting with the economy and implements [VaultAPI](https://github.com/MilkBowl/VaultAPI).

### Key Features:

- Support for multiple currencies.
- Implementation of the "bank" concept.
- Compatibility with VaultAPI.
- Ability to create banks dynamically.

### Banks

The plugin introduces a new concept — "bank." A bank is an indicator of the current currency.

- By default, the 'vault' bank is used. It is applied when implementing VaultAPI and is also used in the `getBalance`, `withdraw`, and `deposit` methods if a specific bank is not specified.
- A player's balance in one bank is not related to their balance in another bank.
- There can be an unlimited number of banks.
- Banks can be created dynamically. When calling `BEconomy.deposit("new bank", uuid)`, a new bank is created transparently to the user.

## Example Usage

```java
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.by1337.bvault.api.BEconomy;

RegisteredServiceProvider<BEconomy> rsp = getServer().getServicesManager().getRegistration(BEconomy.class);
BEconomy economy = Objects.requireNonNull(rsp, "Economy provider not found!").getProvider();

Player player = Bukkit.getPlayer("_By1337_");

// Getting the player's balance
economy.getBalance(player.getUniqueId()).whenComplete((balance, t) -> {
    System.out.println("The player has " + balance + " coins");
});

// Or
Double balance = economy.getBalance(player.getUniqueId()).join();

// Withdrawing funds
economy.withdraw(player.getUniqueId(), 100D).whenComplete((newBalance, t) -> {
    System.out.println("Transaction completed!");
});

// Depositing funds
economy.deposit(player.getUniqueId(), 100D).whenComplete((newBalance, t) -> {
    System.out.println("Transaction completed!");
});

// Getting the player's balance in another currency
Double balance1 = economy.getBalance("donat_points", player.getUniqueId()).join();
```

## Dependencies

To use the plugin, add the following repository and dependency to your `pom.xml`:

```xml
<project>
    <repositories>
        <repository>
            <id>by1337-repo</id>
            <url>https://repo.by1337.space/repository/maven-releases/</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>org.by1337.bvault.api</groupId>
            <artifactId>BVaultApi</artifactId>
            <version>1.1</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

## License

This project is licensed under the [MIT License](LICENSE).