package com.emergencymeeting.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Datos guardados en el mundo: configuración de reuniones y color de cada jugador. */
public class EmergencyData extends SavedData {
    private static final String NAME = "emergencymeeting_data";

    public int durationSeconds = 60;
    public boolean endEarly = true;
    public int cooldownSeconds = 30;
    private final Map<UUID, Integer> colors = new HashMap<>();

    public static EmergencyData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(EmergencyData::load, EmergencyData::new, NAME);
    }

    public static EmergencyData load(CompoundTag tag) {
        EmergencyData data = new EmergencyData();
        if (tag.contains("duration")) {
            data.durationSeconds = tag.getInt("duration");
        }
        if (tag.contains("endEarly")) {
            data.endEarly = tag.getBoolean("endEarly");
        }
        if (tag.contains("cooldown")) {
            data.cooldownSeconds = tag.getInt("cooldown");
        }
        ListTag list = tag.getList("colors", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            data.colors.put(entry.getUUID("uuid"), entry.getInt("color"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("duration", durationSeconds);
        tag.putBoolean("endEarly", endEarly);
        tag.putInt("cooldown", cooldownSeconds);
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Integer> e : colors.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("uuid", e.getKey());
            entry.putInt("color", e.getValue());
            list.add(entry);
        }
        tag.put("colors", list);
        return tag;
    }

    /** -1 si el jugador no eligió color. */
    public int getColorId(UUID uuid) {
        return colors.getOrDefault(uuid, -1);
    }

    public void setColorId(UUID uuid, int id) {
        if (id < 0) {
            colors.remove(uuid);
        } else {
            colors.put(uuid, id);
        }
        setDirty();
    }

    public Map<UUID, Integer> allColors() {
        return new HashMap<>(colors);
    }

    public void setConfig(int duration, boolean endEarly, int cooldown) {
        this.durationSeconds = Math.max(5, Math.min(900, duration));
        this.endEarly = endEarly;
        this.cooldownSeconds = Math.max(0, Math.min(3600, cooldown));
        setDirty();
    }
}
