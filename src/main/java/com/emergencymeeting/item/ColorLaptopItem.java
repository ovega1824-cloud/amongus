package com.emergencymeeting.item;

import com.emergencymeeting.data.EmergencyData;
import com.emergencymeeting.network.Packets;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

/** Laptop: permite elegir el color del jugador. */
public class ColorLaptopItem extends Item {
    public ColorLaptopItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            int current = EmergencyData.get(sp.server).getColorId(sp.getUUID());
            Packets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new Packets.OpenLaptop(current));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
