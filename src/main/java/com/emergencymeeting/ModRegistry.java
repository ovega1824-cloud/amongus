package com.emergencymeeting;

import com.emergencymeeting.block.EmergencyButtonBlock;
import com.emergencymeeting.item.AdminTabletItem;
import com.emergencymeeting.item.ColorLaptopItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    private ModRegistry() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, EmergencyMeetingMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EmergencyMeetingMod.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, EmergencyMeetingMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EmergencyMeetingMod.MODID);

    public static final RegistryObject<Block> EMERGENCY_BUTTON = BLOCKS.register("emergency_button",
            () -> new EmergencyButtonBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final RegistryObject<Item> EMERGENCY_BUTTON_ITEM = ITEMS.register("emergency_button",
            () -> new BlockItem(EMERGENCY_BUTTON.get(), new Item.Properties()));

    public static final RegistryObject<Item> ADMIN_TABLET = ITEMS.register("admin_tablet",
            () -> new AdminTabletItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> COLOR_LAPTOP = ITEMS.register("color_laptop",
            () -> new ColorLaptopItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<SoundEvent> MEETING_SOUND = SOUNDS.register("emergency_meeting",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(EmergencyMeetingMod.MODID, "emergency_meeting")));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.emergencymeeting"))
                    .icon(() -> new ItemStack(EMERGENCY_BUTTON_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(EMERGENCY_BUTTON_ITEM.get());
                        output.accept(ADMIN_TABLET.get());
                        output.accept(COLOR_LAPTOP.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        SOUNDS.register(bus);
        TABS.register(bus);
    }
}
