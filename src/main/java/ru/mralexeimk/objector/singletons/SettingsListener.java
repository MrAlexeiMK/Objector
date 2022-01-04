package ru.mralexeimk.objector.singletons;

import ru.mralexeimk.objector.models.Settings;

public class SettingsListener {
    private static Settings settings;

    public static void init() {
        settings = new Settings();
    }

    public static Settings get() {
        return settings;
    }

    public static void save() {
        settings.saveToFile();
    }
}
