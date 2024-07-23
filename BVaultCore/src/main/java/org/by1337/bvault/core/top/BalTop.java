package org.by1337.bvault.core.top;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BalTop implements Closeable {
    private final Object lock = new Object();
    private final Plugin plugin;
    private final int topSize;
    private final ExecutorService ioExecutor;
    private final Map<String, Top> topMap = new HashMap<>();

    public BalTop(Plugin plugin, ExecutorService ioExecutor, int topSize) {
        this.plugin = plugin;
        this.topSize = topSize;
        this.ioExecutor = ioExecutor;
    }

    public BalTop(Plugin plugin, int topSize) {
        this(
                plugin,
                Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("BVault balTop IO #%d").build()),
                topSize
        );
    }

    public void clear() {
        synchronized (lock) {
            topMap.clear();
        }
    }

    public void clearBalancesIn(String bank) {
        synchronized (lock) {
            topMap.remove(bank);
        }
    }


    public List<TopInfo> getTop(String bank, int limit) {
        List<TopInfo> list = new ArrayList<>();
        synchronized (lock) {
            var top = topMap.get(bank);
            int x = 0;
            if (top != null) {
                for (User user : top.top) {
                    if (x >= limit) break;
                    list.add(new TopInfo(
                            user.uuid,
                            user.name,
                            user.balance,
                            x
                    ));
                    x++;
                }
            }
            for (; x < limit; x++) {
                list.add(TopInfo.EMPTY);
            }
        }
        return list;
    }

    public void updateBalance(UUID player, double balance, String bank, String nickName) {
        ioExecutor.execute(() -> updateBalance0(player, balance, bank, nickName));
    }

    private void updateBalance0(UUID player, double balance, String bank, String nickName) {
        Top top;
        User oldUser;
        User user;
        synchronized (lock) {
            top = topMap.computeIfAbsent(bank, Top::new);
            oldUser = top.users.get(player);
        }
        if (oldUser == null) {
            user = new User(player, nickName, balance);
        } else {
            user = new User(player, nickName, balance);
        }
        synchronized (lock) {
            top.addUser(user);
        }
    }

    public void setTop(List<TopInfo> users, String bank) {
        synchronized (lock) {
            Top top = new Top(bank);
            for (TopInfo user : users) {
                if (user != TopInfo.EMPTY) {
                    top.addUser(new User(user.uuid(), user.nickName(), user.balance()));
                }
            }
            topMap.put(bank, top);
        }
    }

    public int getTopSize() {
        return topSize;
    }

    @Override
    public void close() {
        ioExecutor.shutdown();
    }

    private class Top {
        private final TreeSet<BalTop.User> top = new TreeSet<>(BalTop.User::compareTo);
        private final Map<UUID, BalTop.User> users = new HashMap<>();
        private final String bank;

        public Top(String bank) {
            this.bank = bank;
        }

        private void addUser(BalTop.User user) {
            if (top.isEmpty() ||
                top.last().balance < user.balance ||
                users.containsKey(user.uuid) ||
                top.size() < topSize
            ) {
                top.removeIf(u -> u.uuid.equals(user.uuid));
                if (!top.add(user)) {
                    throw new IllegalStateException("Failed to add user into top!", new Throwable());
                }
                users.put(user.uuid, user);
                trim();
            }
        }

        private void trim() {
            while (top.size() > topSize) {
                var last = top.last();
                top.remove(last);
                users.remove(last.uuid);
            }
        }
    }

    record User(UUID uuid, String name, double balance) implements Comparable<BalTop.User> {
        @Override
        public int compareTo(@NotNull BalTop.User o) {
            var i = Double.compare(o.balance, balance);
            return i == 0 ? uuid.compareTo(o.uuid) : i;
        }
    }
}
