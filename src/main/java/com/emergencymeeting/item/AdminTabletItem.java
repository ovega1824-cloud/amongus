package com.emergencymeeting.item;

import com.emergencymeeting.data.EmergencyData;
import com.emergencymeeting.network.Packets;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

/** Tablet del administrador: abre el menú de configuración de las reuniones. */
public class AdminTabletItem extends Item {
    public AdminTabletItem(Properties properties) {
        super(properties);
    }

    public static boolean isAdmin(ServerPlayer player) {
        MinecraftServer server = player.server;
        return player.createCommandSourceStack().hasPermission(2) || server.isSingleplayerOwner(player.getGameProfile());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (!isAdmin(sp)) {
                sp.displayClientMessage(Component.literal("§cSolo los administradores pueden usar esta tablet."), true);
            } else {
                EmergencyData data = EmergencyData.get(sp.server);
                Packets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                        new Packets.OpenAdmin(data.durationSeconds, data.endEarly, data.cooldownSeconds));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
