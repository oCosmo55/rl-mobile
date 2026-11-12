package radiant;

import android.util.Log;

import androidx.compose.ui.text.AnnotatedString;

import androidx.compose.ui.graphics.ColorKt;

// Spicy lyric style: the word being sung grows, rises and glows, long held words go letter by letter
// Same data and same line layout as WordLyrics, only the spans differ
// Java 8, no lambdas, no nested classes (see rl-ext-build/build.sh)
public final class SpicyLyrics {
    private static final String TAG = "RLSpicy";

    // How long a finished word keeps glowing before it settles
    private static final float TAIL_MS = 750f;
    // Average glyph width in em, used to cancel the extra width of a grown letter
    private static final float GLYPH_EM = 0.55f;
    // Reference font size in px for the lift (lift is stored in tenths of a px)
    private static final float LIFT_REF = 28f;

    // A crash here would take the lyrics screen down, so one failure turns Spicy off until restart
    private static volatile boolean broken;

    private SpicyLyrics() {}

    // null = not handled, the caller draws the classic way
    static AnnotatedString render(int idx, int mode, long sung, long unsung) {
        if (broken) return null;
        try {
            return build(idx, mode, sung, unsung);
        } catch (Throwable t) {
            broken = true;
            Log.w(TAG, "Spicy lyrics disabled", t);
            return null;
        }
    }

    private static AnnotatedString build(int idx, int mode, long sung, long unsung) {
        if (!RLAPILyricsHook.isRlState) return null;
        String[][] texts;
        long[][] starts;
        long[][] durs = null;
        boolean[][] bgAll;
        if (mode == RLSettings.SYNC_SYLLABLE) {
            texts = WordLyrics.lineSylText;
            starts = WordLyrics.lineSylStart;
            durs = WordLyrics.lineSylDur;
            bgAll = WordLyrics.lineSylBg;
            if (durs == null) return null;
        } else if (mode == RLSettings.SYNC_WORD) {
            texts = WordLyrics.lineWordText;
            starts = WordLyrics.lineWordStart;
            bgAll = WordLyrics.lineWordBg;
        } else {
            return null;
        }
        if (texts == null || starts == null || idx < 0 || idx >= texts.length || idx >= starts.length) return null;
        String[] row = texts[idx];
        long[] st = starts[idx];
        if (row == null || st == null || row.length == 0 || st.length < row.length) return null;
        int n = row.length;
        boolean[] bg = WordLyrics.bgFlags(bgAll, idx, n);

        long[] du;
        if (durs != null) {
            if (idx >= durs.length || durs[idx] == null || durs[idx].length < n) return null;
            du = durs[idx];
        } else {
            du = wordDurations(idx, st, bg, n);
        }

        // Hand the helpers this line, like WordLyrics.renderSyl does
        WordLyrics.tmpSung = sung;
        WordLyrics.tmpUnsung = unsung;
        WordLyrics.pLineIdx = idx;
        WordLyrics.pN = n;
        WordLyrics.pStarts = st;
        WordLyrics.pDurs = du;
        WordLyrics.pBg = bg;
        WordLyrics.pBeg = new int[n];
        WordLyrics.pFin = new int[n];
        WordLyrics.pVis = new int[n];

        AnnotatedString.Builder b = new AnnotatedString.Builder();
        WordLyrics.emitLine(b, row);
        int total = WordLyrics.pTotal;
        WordLyrics.applyParagraphs(b, idx, WordLyrics.pBgStart, total);

        WordLyrics.lineState().getValue();    // recompose when the playing line changes
        if (WordLyrics.isPastLine(idx)) {
            b.addStyle(WordLyrics.makeSpan(sung), 0, total);
            return b.toAnnotatedString();
        }
        b.addStyle(WordLyrics.makeSpan(unsung), 0, total);
        if (!WordLyrics.isActiveLine(idx)) return b.toAnnotatedString();

        WordLyrics.state().getValue();    // re-render this line every tick
        long now = WordLyrics.computeNow();
        WordLyrics.pNow = now;

        float glow = RLSettings.spicyGlow() / 100f;
        float growth = RLSettings.spicyGrowth() / 100f;
        float lift = RLSettings.spicyLiftTenths() / 10f / LIFT_REF;
        boolean letters = RLSettings.spicyLetters();
        long letterMs = RLSettings.spicyLetterMs();
        float push = RLSettings.spicyPush() ? RLSettings.spicyPushPct() / 100f : 0f;
        float blur = (6f + 14f * glow) * RLSettings.density();

        int[] pBeg = WordLyrics.pBeg;
        int[] pVis = WordLyrics.pVis;
        for (int k = 0; k < n; k++) {
            int beg = pBeg[k];
            int vis = pVis[k];
            if (vis <= beg) continue;
            long s = st[k];
            if (now < s) continue;    // not reached, stays unsung
            float d = Math.max(1L, du[k]);
            int len = vis - beg;
            String piece = bg != null && bg[k] ? WordLyrics.stripBrackets(row[k]) : row[k];
            int cps = piece.codePointCount(0, Math.min(len, piece.length()));
            if (letters && d >= letterMs && cps > 1) {
                float ld = Math.min(d, d / cps * 2.2f);
                int at = 0;
                for (int c = 0; c < cps; c++) {
                    int w = Character.charCount(piece.codePointAt(at));
                    float ls = s + (d - ld) * c / (cps - 1);
                    unit(b, now, ls, ld, beg + at, beg + at + w, sung, unsung, glow, growth, lift, push, blur);
                    at += w;
                }
            } else {
                unit(b, now, s, d, beg, vis, sung, unsung, glow, growth, lift, push, blur);
            }
        }
        return b.toAnnotatedString();
    }

    // Word data has no durations, a word lasts until the next one of its group (or the line end)
    private static long[] wordDurations(int idx, long[] st, boolean[] bg, int n) {
        long[] du = new long[n];
        long[] ends = WordLyrics.lineEnds;
        long lineEnd = ends != null && idx < ends.length ? ends[idx] : 0L;
        for (int k = 0; k < n; k++) {
            long end = 0L;
            for (int j = k + 1; j < n; j++) {
                boolean same = bg == null || bg[j] == bg[k];
                if (same) {
                    end = st[j];
                    break;
                }
            }
            if (end <= st[k]) end = lineEnd > st[k] ? lineEnd : st[k] + 800L;
            du[k] = Math.max(1L, end - st[k]);
        }
        return du;
    }

    // One word or letter: colour follows the progress, size/lift/glow peak while it is sung then settle
    private static void unit(AnnotatedString.Builder b, long now, float start, float dur, int a, int e,
                             long sung, long unsung, float glow, float growth, float lift, float push, float blur) {
        float p = (now - start) / dur;
        if (p <= 0f) return;
        if (p > 1f) p = 1f;
        float lit = p * p * (3f - 2f * p);
        float env;
        if (p < 1f) env = (float) Math.sin(p * Math.PI / 2d);
        else env = Math.max(0f, 1f - (now - start - dur) / TAIL_MS);
        long color = ColorKt.lerpjxsXWHM(unsung, sung, lit);
        if (env < 0.004f) {
            b.addStyle(WordLyrics.makeSpan(color), a, e);
            return;
        }
        float em = 1f + growth * env;
        int alpha = (int) (255f * 0.85f * glow * env);
        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;
        int glowArgb = (alpha << 24) | 0x00FFFFFF;
        // push 1 = neighbours get shoved by the grown letter, push 0 = the extra width is taken back
        float spacing = -(1f - push) * GLYPH_EM * (em - 1f) / em;
        b.addStyle(SpicySpan.make(color, em, lift * env, glowArgb, blur, spacing), a, e);
    }
}
