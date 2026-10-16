package com.emergencymeeting.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Paleta de colores que se pueden elegir en la laptop. */
public final class PlayerColors {
    private PlayerColors() {}

    public record Entry(String id, String name, int rgb) {}

    public static final List<Entry> ALL = List.of(
            new Entry("red", "Rojo", 0xC51111),
            new Entry("blue", "Azul", 0x2F4BEB),
            new Entry("green", "Verde", 0x1B9A3A),
            new Entry("pink", "Rosa", 0xED54BA),
            new Entry("orange", "Naranja", 0xEF7D0D),
            new Entry("yellow", "Amarillo", 0xF5F557),
            new Entry("black", "Negro", 0x5A6470),
            new Entry("white", "Blanco", 0xE6EEF8),
            new Entry("purple", "Morado", 0x7C3FD0),
            new Entry("brown", "Cafe", 0x8A5A2B),
            new Entry("cyan", "Cian", 0x38FEDC),
            new Entry("lime", "Lima", 0x50EF39),
            new Entry("maroon", "Granate", 0x8C2A3D),
            new Entry("rose", "Rosa claro", 0xECC0D3),
            new Entry("banana", "Banana", 0xFFFFBE),
            new Entry("gray", "Gris", 0x8A97A5),
            new Entry("tan", "Beige", 0xB5A68A),
            new Entry("coral", "Coral", 0xEC7578)
    );

    public static int rgb(int id) {
        if (id < 0 || id >= ALL.size()) {
            return 0xFFFFFF;
        }
        return ALL.get(id).rgb();
    }

    public static List<String> ids() {
        List<String> list = new ArrayList<>();
        for (Entry e : ALL) {
            list.add(e.id());
        }
        list.add("ninguno");
        return list;
    }

    /** Devuelve el índice del color por id o nombre, -1 si es "ninguno", -2 si no existe. */
    public static int find(String text) {
        String t = text.toLowerCase(Locale.ROOT);
        if (t.equals("ninguno") || t.equals("none") || t.equals("reset")) {
            return -1;
        }
        for (int i = 0; i < ALL.size(); i++) {
            if (ALL.get(i).id().equals(t) || ALL.get(i).name().toLowerCase(Locale.ROOT).equals(t)) {
                return i;
            }
        }
        return -2;
    }
}
