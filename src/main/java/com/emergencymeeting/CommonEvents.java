package com.emergencymeeting;

import com.emergencymeeting.data.PlayerColors;
import com.emergencymeeting.logic.ColorStore;
import com.emergencymeeting.logic.MeetingManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Eventos comunes (cliente y servidor): nombres de colores, tick de la reunión, bloqueo de acciones y comandos. */
@Mod.EventBusSubscriber(modid = EmergencyMeetingMod.MODID)
public final class CommonEvents {
    private CommonEvents() {}

    // ---------- Nombre de color ----------

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        int id = ColorStore.colorIdOf(event.getEntity());
        if (id >= 0) {
            event.setDisplayname(ColorStore.colorize(event.getDisplayname(), id));
        }
    }

    @SubscribeEvent
    public static void onTabListName(PlayerEvent.TabListNameFormat event) {
        int id = ColorStore.colorIdOf(event.getEntity());
        if (id >= 0) {
            event.setDisplayName(ColorStore.colorize(event.getEntity().getName(), id));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ColorStore.syncAll(sp.server);
        }
    }

    // ---------- Reunión ----------

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                MeetingManager.tick(server);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MeetingManager.clear();
    }

    private static boolean frozen(Player player) {
        return player instanceof ServerPlayer && MeetingManager.isParticipant(player.getUUID());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (frozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (frozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (frozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (frozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (frozen(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (frozen(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    /** Nadie puede hacerle daño a un jugador mientras dura la reunión. */
    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && MeetingManager.isParticipant(sp.getUUID())) {
            event.setCanceled(true);
        }
    }

    // ---------- Comandos ----------

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("meeting")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("start").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Component error = MeetingManager.tryStart(p, p.serverLevel(), p.blockPosition());
                    if (error != null) {
                        ctx.getSource().sendFailure(error);
                        return 0;
                    }
                    return 1;
                }))
                .then(Commands.literal("stop").executes(ctx -> {
                    boolean stopped = MeetingManager.forceStop(ctx.getSource().getServer());
                    if (!stopped) {
                        ctx.getSource().sendFailure(Component.literal("No hay ninguna reunión en curso."));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal("Reunión terminada."), true);
                    return 1;
                }))
                .then(Commands.literal("time").then(Commands.argument("segundos", IntegerArgumentType.integer(5, 900))
                        .executes(ctx -> {
                            int secs = IntegerArgumentType.getInteger(ctx, "segundos");
                            var data = com.emergencymeeting.data.EmergencyData.get(ctx.getSource().getServer());
                            data.setConfig(secs, data.endEarly, data.cooldownSeconds);
                            ctx.getSource().sendSuccess(() -> Component.literal("Duración de la reunión: " + secs + "s"), true);
                            return 1;
                        })))
                .then(Commands.literal("color").then(Commands.argument("jugador", EntityArgument.player())
                        .then(Commands.argument("color", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(PlayerColors.ids(), builder))
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "jugador");
                                    int id = PlayerColors.find(StringArgumentType.getString(ctx, "color"));
                                    if (id == -2) {
                                        ctx.getSource().sendFailure(Component.literal("Ese color no existe."));
                                        return 0;
                                    }
                                    ColorStore.setColor(target, id);
                                    return 1;
                                })))));
    }
}
