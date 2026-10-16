package com.emergencymeeting.client;

import net.minecraftforge.common.ForgeConfigSpec;

/** Configuración del lado del cliente (se guarda en config/emergencymeeting-client.toml). */
public final class ClientConfig {
    private ClientConfig() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_TABLET;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        SHOW_TABLET = builder
                .comment("Mostrar el botón de la tablet en pantalla")
                .define("showTablet", true);
        SPEC = builder.build();
    }

    public static boolean showTablet() {
        try {
            return SHOW_TABLET.get();
        } catch (Exception e) {
            return true;
        }
    }

    public static void setShowTablet(boolean value) {
        try {
            SHOW_TABLET.set(value);
            SHOW_TABLET.save();
        } catch (Exception ignored) {
            // si la config aún no está cargada no pasa nada
        }
    }
}
