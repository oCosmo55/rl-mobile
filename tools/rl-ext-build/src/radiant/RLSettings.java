package radiant;

import android.content.Context;
import android.content.SharedPreferences;

// Lyrics settings the Quick Menu can change
// Manager options are defaults, re-patching resets them
public final class RLSettings {
    // Sent to the API as ?language=
    static final String[] LANG_VALUES = {"english", "spanish", "french", "german", "portuguese", "italian", "russian", "japanese", "korean", "chinese"};
    static final String[] LANG_LABELS = {"English", "Spanish", "French", "German", "Portuguese", "Italian", "Russian", "Japanese", "Korean", "Chinese"};

    static final int SYNC_LINE = 0;
    static final int SYNC_WORD = 1;
    static final int SYNC_SYLLABLE = 2;
    static final String[] SYNC_LABELS = {"Line", "Word", "Syllable"};

    private static final String PREFS = "radiant_lyrics";

    // RL API is in, set when its hook runs
    public static volatile boolean rlActive;

    private static volatile boolean loaded;
    private static volatile boolean romanize;
    private static volatile boolean translate;
    private static volatile int language;
    // Off hides all AI stuff, Manager only
    private static volatile boolean allowAI = true;
    // Sylla fills missing lyrics and retimes Line ones
    private static volatile boolean radiantAI;
    private static volatile int sync;
    // Menu on uses saved prefs, off uses Manager values
    private static volatile boolean quickMenu = true;
    // Radiant AI toasts, Manager only
    static final int TOASTS_NONE = 0;
    static final int TOASTS_ERRORS = 1;
    static final int TOASTS_ALL = 2;
    private static volatile int toasts;

    // Spicy lyric style, in app only (not Manager options), defaults picked in the browser preview
    static final int SPICY_STYLE = 0;
    static final int SPICY_GLOW = 1;
    static final int SPICY_GROWTH = 2;
    static final int SPICY_LIFT = 3;
    static final int SPICY_LETTER_MS = 4;
    static final int SPICY_PUSH_PCT = 5;
    static final String[] SPICY_KEYS = {"sp_on", "sp_glow", "sp_growth", "sp_lift", "sp_letterMs", "sp_pushPct"};
    // on(1/0), glow 0-100, growth % 0-25, lift in tenths of px 0-100, long word ms, push 0-100
    private static final int[] SPICY_DEFAULTS = {1, 60, 8, 35, 1000, 100};
    private static final int[] SPICY_MIN = {0, 0, 0, 0, 300, 0};
    private static final int[] SPICY_MAX = {1, 100, 25, 100, 2000, 100};
    private static final int[] spicyVals = SPICY_DEFAULTS.clone();
    private static volatile boolean spicyLetters = true;
    private static volatile boolean spicyPush = true;
    private static volatile boolean spicyLoaded;
    private static volatile float density = 3f;

    private RLSettings() {}

    static Context app() {
        try {
            return (Context) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static SharedPreferences prefs() {
        Context c = app();
        return c == null ? null : c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static void load() {
        if (!loaded) loadNow();
    }

    private static synchronized void loadNow() {
        if (loaded) return;
        int r = RLDefaults.romanize(), t = RLDefaults.translate(), l = RLDefaults.language();
        int ai = RLDefaults.radiantAI(), s = RLDefaults.sync();
        allowAI = RLDefaults.allowAI() != 0;
        quickMenu = RLDefaults.quickMenu() != 0;
        toasts = RLDefaults.toasts();
        String baked = r + "," + t + "," + l + "," + ai + "," + s + "," + (quickMenu ? 1 : 0);
        // No menu, ignore anything an older one saved
        SharedPreferences p = quickMenu ? prefs() : null;
        if (p != null && baked.equals(p.getString("baked", null))) {
            romanize = p.getBoolean("romanize", r != 0);
            translate = p.getBoolean("translate", t != 0);
            language = clampLang(p.getInt("language", l));
            radiantAI = p.getBoolean("radiantAI", ai != 0);
            sync = clampSync(p.getInt("sync", s));
        } else {
            // New Manager options win
            romanize = r != 0;
            translate = t != 0;
            language = clampLang(l);
            radiantAI = ai != 0;
            sync = clampSync(s);
            if (p != null) {
                p.edit().putString("baked", baked).apply();
                save();
            }
        }
        loaded = true;
    }

    private static void save() {
        SharedPreferences p = prefs();
        if (p == null) return;
        p.edit()
                .putBoolean("romanize", romanize)
                .putBoolean("translate", translate)
                .putInt("language", language)
                .putBoolean("radiantAI", radiantAI)
                .putInt("sync", sync)
                .apply();
    }

    // Spicy settings

    private static void spicyLoad() {
        if (spicyLoaded) return;
        synchronized (RLSettings.class) {
            if (spicyLoaded) return;
            SharedPreferences p = prefs();
            if (p != null) {
                for (int i = 0; i < SPICY_KEYS.length; i++) {
                    spicyVals[i] = clampSpicy(i, p.getInt(SPICY_KEYS[i], SPICY_DEFAULTS[i]));
                }
                spicyLetters = p.getBoolean("sp_letters", true);
                spicyPush = p.getBoolean("sp_push", true);
                Context c = app();
                if (c != null) density = c.getResources().getDisplayMetrics().density;
            }
            spicyLoaded = true;
        }
    }

    private static int clampSpicy(int key, int v) {
        return v < SPICY_MIN[key] ? SPICY_MIN[key] : v > SPICY_MAX[key] ? SPICY_MAX[key] : v;
    }

    static int spicyMin(int key) {
        return SPICY_MIN[key];
    }

    static int spicyMax(int key) {
        return SPICY_MAX[key];
    }

    static int spicyValue(int key) {
        spicyLoad();
        return spicyVals[key];
    }

    static void setSpicyValue(int key, int v) {
        spicyLoad();
        spicyVals[key] = clampSpicy(key, v);
        SharedPreferences p = prefs();
        if (p != null) p.edit().putInt(SPICY_KEYS[key], spicyVals[key]).apply();
        RLLyrics.bump();
    }

    static void setSpicyLetters(boolean on) {
        spicyLoad();
        spicyLetters = on;
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean("sp_letters", on).apply();
        RLLyrics.bump();
    }

    static void setSpicyPush(boolean on) {
        spicyLoad();
        spicyPush = on;
        SharedPreferences p = prefs();
        if (p != null) p.edit().putBoolean("sp_push", on).apply();
        RLLyrics.bump();
    }

    public static boolean spicy() {
        return spicyValue(SPICY_STYLE) != 0;
    }

    static int spicyGlow() {
        return spicyValue(SPICY_GLOW);
    }

    static int spicyGrowth() {
        return spicyValue(SPICY_GROWTH);
    }

    static int spicyLiftTenths() {
        return spicyValue(SPICY_LIFT);
    }

    static long spicyLetterMs() {
        return spicyValue(SPICY_LETTER_MS);
    }

    static int spicyPushPct() {
        return spicyValue(SPICY_PUSH_PCT);
    }

    static boolean spicyLetters() {
        spicyLoad();
        return spicyLetters;
    }

    static boolean spicyPush() {
        spicyLoad();
        return spicyPush;
    }

    static float density() {
        spicyLoad();
        return density;
    }

    private static int clampLang(int i) {
        return i < 0 || i >= LANG_VALUES.length ? 0 : i;
    }

    private static int clampSync(int i) {
        return i < SYNC_LINE || i > SYNC_SYLLABLE ? SYNC_LINE : i;
    }

    public static boolean romanize() {
        load();
        return romanize;
    }

    public static boolean translate() {
        load();
        return translate;
    }

    public static int languageIndex() {
        load();
        return language;
    }

    public static String language() {
        return LANG_VALUES[languageIndex()];
    }

    public static boolean allowAI() {
        load();
        return allowAI;
    }

    public static boolean radiantAI() {
        load();
        return allowAI && radiantAI;
    }

    public static boolean quickMenu() {
        load();
        return quickMenu;
    }

    public static int toasts() {
        load();
        return toasts;
    }

    public static int sync() {
        load();
        return sync;
    }

    // Setters, RLLyrics refetches or recomposes

    static void setRomanize(boolean on) {
        load();
        romanize = on;
        save();
        RLLyrics.onTextModeChanged();
    }

    static void setTranslate(boolean on) {
        load();
        translate = on;
        save();
        RLLyrics.onTextModeChanged();
    }

    static void setLanguage(int i) {
        load();
        language = clampLang(i);
        save();
        RLLyrics.onTextModeChanged();
    }

    static void setRadiantAI(boolean on) {
        load();
        radiantAI = on;
        save();
        RLLyrics.onRadiantAIChanged();
    }

    static void setSync(int s) {
        load();
        sync = clampSync(s);
        save();
        RLLyrics.onSyncChanged();
    }
}
