package com.emergencymeeting.logic;

import com.emergencymeeting.data.EmergencyData;
import com.emergencymeeting.data.PlayerColors;
import com.emergencymeeting.network.Packets;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Lógica del servidor de la reunión: teletransporte, inmovilización, votos, resultado y expulsión. */
public final class MeetingManager {
    private MeetingManager() {}

    public enum Phase { IDLE, VOTING, RESULT }

    private static final UUID SKIP = new UUID(0L, 0L);
    private static final int RESULT_TICKS = 100;

    private static Phase phase = Phase.IDLE;
    private static ServerLevel level;
    private static int ticksLeft;
    private static boolean endEarly;
    private static long lastEnd = Long.MIN_VALUE / 2;
    private static UUID ejected;

    /** Asiento fijo de cada participante (posición donde queda inmovilizado). */
    private static final Map<UUID, Vec3> seats = new LinkedHashMap<>();
    private static final Map<UUID, String> names = new HashMap<>();
    private static final Map<UUID, UUID> votes = new HashMap<>();

    public static boolean isActive() {
        return phase != Phase.IDLE;
    }

    public static boolean isParticipant(UUID uuid) {
        return phase != Phase.IDLE && seats.containsKey(uuid);
    }

    /** Intenta iniciar una reunión. Devuelve un mensaje de error, o null si todo salió bien. */
    public static Component tryStart(ServerPlayer caller, ServerLevel lvl, BlockPos pos) {
        if (phase != Phase.IDLE) {
            return Component.literal("§cYa hay una reunión en curso.");
        }
        MinecraftServer server = lvl.getServer();
        EmergencyData data = EmergencyData.get(server);
        long now = server.overworld().getGameTime();
        long cooldown = data.cooldownSeconds * 20L;
        if (now - lastEnd < cooldown) {
            long secs = (cooldown - (now - lastEnd) + 19) / 20;
            return Component.literal("§cEl botón se está recargando: " + secs + "s");
        }

        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isAlive() && p.gameMode.getGameModeForPlayer() == GameType.ADVENTURE) {
                players.add(p);
            }
        }
        if (players.isEmpty()) {
            return Component.literal("§cNo hay jugadores en modo aventura.");
        }

        level = lvl;
        endEarly = data.endEarly;
        ticksLeft = data.durationSeconds * 20;
        ejected = null;
        seats.clear();
        names.clear();
        votes.clear();

        double cx = pos.getX() + 0.5D;
        double cz = pos.getZ() + 0.5D;
        int n = players.size();
        double radius = Math.max(2.5D, n * 0.9D / (2.0D * Math.PI));
        for (int i = 0; i < n; i++) {
            ServerPlayer p = players.get(i);
            double angle = 2.0D * Math.PI * i / n;
            double x = cx + radius * Math.cos(angle);
            double z = cz + radius * Math.sin(angle);
            double y = findStandY(lvl, x, z, pos.getY());
            if (y < -1000) {
                x = cx;
                z = cz;
                y = pos.getY() + 1.0D;
            }
            float yaw = (float) (Mth.atan2(-(cx - x), cz - z) * (180.0D / Math.PI));
            p.stopRiding();
            p.teleportTo(lvl, x, y, z, yaw, 12.0F);
            p.fallDistance = 0.0F;
            p.setDeltaMovement(Vec3.ZERO);
            seats.put(p.getUUID(), new Vec3(x, y, z));
            names.put(p.getUUID(), p.getGameProfile().getName());
        }

        int callerColorId = data.getColorId(caller.getUUID());
        int callerRgb = callerColorId < 0 ? 0xFFFFFF : PlayerColors.rgb(callerColorId);
        List<UUID> ids = new ArrayList<>(seats.keySet());
        List<String> nameList = new ArrayList<>();
        for (UUID id : ids) {
            nameList.add(names.get(id));
        }
        phase = Phase.VOTING;
        for (UUID id : ids) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                send(p, new Packets.MeetingStart(caller.getGameProfile().getName(), callerRgb,
                        ticksLeft, ids, nameList));
            }
        }
        return null;
    }

    /** Altura (Y de los pies) más alta, cerca de baseY, donde cabe un jugador. -9999 si no hay. */
    private static double findStandY(ServerLevel lvl, double x, double z, int baseY) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        for (int dy = 2; dy >= -4; dy--) {
            BlockPos feet = new BlockPos(bx, baseY + dy, bz);
            BlockPos below = feet.below();
            boolean ground = !lvl.getBlockState(below).getCollisionShape(lvl, below).isEmpty();
            boolean free1 = lvl.getBlockState(feet).getCollisionShape(lvl, feet).isEmpty();
            boolean free2 = lvl.getBlockState(feet.above()).getCollisionShape(lvl, feet.above()).isEmpty();
            if (ground && free1 && free2) {
                return feet.getY();
            }
        }
        return -9999.0D;
    }

    public static void vote(ServerPlayer voter, boolean skip, UUID target) {
        if (phase != Phase.VOTING) {
            return;
        }
        UUID me = voter.getUUID();
        if (!seats.containsKey(me) || votes.containsKey(me)) {
            return;
        }
        if (!skip && (target == null || !seats.containsKey(target))) {
            return;
        }
        votes.put(me, skip ? SKIP : target);
        broadcastUpdate(voter.server);
    }

    private static void broadcastUpdate(MinecraftServer server) {
        Packets.MeetingUpdate packet = new Packets.MeetingUpdate(new ArrayList<>(votes.keySet()), ticksLeft);
        for (UUID id : seats.keySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                send(p, packet);
            }
        }
    }

    public static void tick(MinecraftServer server) {
        if (phase == Phase.IDLE) {
            return;
        }

        Iterator<Map.Entry<UUID, Vec3>> it = seats.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Vec3> e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null || !p.isAlive()) {
                it.remove();
                continue;
            }
            Vec3 s = e.getValue();
            if (p.level() != level) {
                p.teleportTo(level, s.x, s.y, s.z, p.getYRot(), p.getXRot());
            } else if (p.position().distanceToSqr(s) > 0.04D) {
                p.connection.teleport(s.x, s.y, s.z, p.getYRot(), p.getXRot());
            }
        }

        if (seats.isEmpty()) {
            end(server);
            return;
        }

        ticksLeft--;
        if (phase == Phase.VOTING) {
            boolean allVoted = endEarly && votes.keySet().containsAll(seats.keySet());
            if (ticksLeft <= 0 || allVoted) {
                finishVoting(server);
            } else if (ticksLeft % 40 == 0) {
                broadcastUpdate(server);
            }
        } else if (phase == Phase.RESULT && ticksLeft <= 0) {
            end(server);
        }
    }

    private static void finishVoting(MinecraftServer server) {
        Map<UUID, Integer> counts = new LinkedHashMap<>();
        for (UUID id : seats.keySet()) {
            counts.put(id, 0);
        }
        int skip = 0;
        for (UUID target : votes.values()) {
            if (target.equals(SKIP)) {
                skip++;
            } else {
                counts.merge(target, 1, Integer::sum);
            }
        }
        int max = 0;
        UUID top = null;
        boolean tie = false;
        for (Map.Entry<UUID, Integer> e : counts.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                top = e.getKey();
                tie = false;
            } else if (e.getValue() == max && max > 0) {
                tie = true;
            }
        }
        ejected = (top != null && !tie && max > skip) ? top : null;

        EmergencyData data = EmergencyData.get(server);
        Component message;
        String ejectedName = "";
        if (ejected != null) {
            ejectedName = names.getOrDefault(ejected, "?");
            int colorId = data.getColorId(ejected);
            Component nameComponent = Component.literal(ejectedName);
            if (colorId >= 0) {
                nameComponent = ColorStore.colorize(nameComponent, colorId);
            }
            message = Component.empty().append(nameComponent).append(Component.literal(" fue expulsado de la nave."));
        } else {
            message = Component.literal("Nadie fue expulsado (empate o se saltó la votación).");
        }

        List<UUID> ids = new ArrayList<>(counts.keySet());
        List<Integer> countList = new ArrayList<>(counts.values());
        Packets.MeetingResult packet = new Packets.MeetingResult(ids, countList, skip, ejected, ejectedName);
        for (UUID id : seats.keySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                send(p, packet);
                p.sendSystemMessage(message);
            }
        }
        phase = Phase.RESULT;
        ticksLeft = RESULT_TICKS;
    }

    private static void end(MinecraftServer server) {
        for (UUID id : seats.keySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                send(p, new Packets.MeetingEnd());
            }
        }
        if (ejected != null) {
            ServerPlayer p = server.getPlayerList().getPlayer(ejected);
            if (p != null) {
                p.setGameMode(GameType.SPECTATOR);
                p.sendSystemMessage(Component.literal("§cFuiste expulsado de la nave. Ahora eres espectador."));
            }
        }
        lastEnd = server.overworld().getGameTime();
        clear();
    }

    /** Termina la reunión de inmediato sin expulsar a nadie (comando /meeting stop). */
    public static boolean forceStop(MinecraftServer server) {
        if (phase == Phase.IDLE) {
            return false;
        }
        ejected = null;
        end(server);
        return true;
    }

    public static void clear() {
        phase = Phase.IDLE;
        level = null;
        ejected = null;
        seats.clear();
        names.clear();
        votes.clear();
    }

    private static void send(ServerPlayer player, Object packet) {
        Packets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
