package com.example.hamhaevoca;
import android.content.Context;
import android.content.res.Configuration;
import java.util.Locale;
/** Reads one app-wide language setting and selects Android localized resources. */
public final class Lang {
    private Lang() {}
    public static String getLanguage(Context context) {
        return context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getString("ui_language", "ko");
    }
    public static void saveLanguage(Context context, String language) {
        context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).edit().putString("ui_language",language).apply();
    }
    public static Context wrap(Context base) {
        String code = getLanguage(base);
        Locale locale = new Locale(code);
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        return base.createConfigurationContext(config);
    }
    public static String text(Context context, int resource) { return context.getString(resource); }
}
