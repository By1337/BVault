package org.by1337.bvault.core.top;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record TopInfo(@Nullable UUID player, @Nullable String nickName, double balance, int pos) {
    public static final TopInfo EMPTY = new TopInfo(null, null, 0, 0);
}
