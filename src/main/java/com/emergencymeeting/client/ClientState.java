package com.emergencymeeting.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Estado de la reunión tal como lo ve este cliente. */
public final class ClientState {
    private ClientState() {}

    /** Duración de la animación en ticks (el audio dura unos 3.2 segundos). */
    public static final int ANIM_TOTAL = 68;

    public static boolean active;
    /** 0 = ninguna, 1 = votando, 2 = resultado */
    public static int phase;
    public static int animTick = -1;
    public static int ticksLeft;
    public static boolean autoOpened;

    public static String callerName = "";
    public static int callerRgb = 0xFFFFFF;

    public static final List<UUID> ids = new ArrayList<>();
    public static final Map<UUID, String> names = new HashMap<>();
    public static final Set<UUID> voted = new HashSet<>();
    public static boolean iVoted;

    public static final Map<UUID, Integer> counts = new HashMap<>();
    public static int skipCount;
    public static UUID ejected;
    public static String ejectedName = "";

    public static void reset() {
        active = false;
        phase = 0;
        animTick = -1;
        ticksLeft = 0;
        autoOpened = false;
        callerName = "";
        callerRgb = 0xFFFFFF;
        ids.clear();
        names.clear();
        voted.clear();
        iVoted = false;
        counts.clear();
        skipCount = 0;
        ejected = null;
        ejectedName = "";
    }
}
