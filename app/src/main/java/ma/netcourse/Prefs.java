package ma.netcourse;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    // Valeurs par défaut relevées dans le code compilé de la version 1.
    static final String DIESEL = "diesel", CONSO = "conso", COMM = "comm";
    static final String WEAR = "wear", BACK = "back", TKM = "tkm", DIAG = "diag";
    private final SharedPreferences p;

    Prefs(Context context) {
        p = context.getSharedPreferences("app_debug", Context.MODE_PRIVATE);
    }
    double get(String key, double defaultValue) {
        try { return Double.parseDouble(p.getString(key, Double.toString(defaultValue)).replace(',', '.')); }
        catch (Exception ignored) { return defaultValue; }
    }
    void put(String key, String value) { p.edit().putString(key, value).apply(); }
    boolean diagnostic() { return p.getBoolean(DIAG, false); }
    void setDiagnostic(boolean value) { p.edit().putBoolean(DIAG, value).apply(); }

    RideCalculator.Config config() {
        return new RideCalculator.Config(
            get(DIESEL, 10.5), get(CONSO, 5.2),
            get(COMM, 10), get(WEAR, 0.4),
            get(BACK, 0), get(TKM, 2.0));
    }
}
