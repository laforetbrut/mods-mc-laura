package com.vyrriox.lauramod.dialogue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Minecraft language codes and fallback chains. A player using {@code fr_ca} gets Canadian lines if
 * someone wrote them, then {@code fr_fr}, then the configured fallback, then {@code en_us}.
 *
 * @author vyrriox
 */
public final class LocaleUtil {
    public static final String DEFAULT = "en_us";

    /** Main variant of each language, used when a regional variant has no file of its own. */
    private static final Map<String, String> PRIMARY = Map.ofEntries(
            Map.entry("en", "en_us"), Map.entry("fr", "fr_fr"), Map.entry("de", "de_de"),
            Map.entry("es", "es_es"), Map.entry("it", "it_it"), Map.entry("pt", "pt_br"),
            Map.entry("ru", "ru_ru"), Map.entry("pl", "pl_pl"), Map.entry("nl", "nl_nl"),
            Map.entry("zh", "zh_cn"), Map.entry("ja", "ja_jp"), Map.entry("ko", "ko_kr"),
            Map.entry("tr", "tr_tr"), Map.entry("uk", "uk_ua"), Map.entry("sv", "sv_se"),
            Map.entry("cs", "cs_cz"), Map.entry("da", "da_dk"), Map.entry("fi", "fi_fi"),
            Map.entry("no", "no_no"), Map.entry("nb", "no_no"), Map.entry("hu", "hu_hu"),
            Map.entry("ro", "ro_ro"), Map.entry("el", "el_gr"), Map.entry("ar", "ar_sa"),
            Map.entry("he", "he_il"), Map.entry("id", "id_id"), Map.entry("vi", "vi_vn"),
            Map.entry("th", "th_th"), Map.entry("bg", "bg_bg"), Map.entry("hr", "hr_hr"),
            Map.entry("sk", "sk_sk"), Map.entry("sl", "sl_si"), Map.entry("sr", "sr_sp"),
            Map.entry("lt", "lt_lt"), Map.entry("lv", "lv_lv"), Map.entry("et", "et_ee"),
            Map.entry("ca", "ca_es"), Map.entry("gl", "gl_es"), Map.entry("eu", "eu_es"),
            Map.entry("be", "be_by"), Map.entry("kk", "kk_kz"), Map.entry("ms", "ms_my"),
            Map.entry("fil", "fil_ph"), Map.entry("hi", "hi_in"), Map.entry("fa", "fa_ir"));

    /** Regional variants that are closer to another variant than to the primary one. */
    private static final Map<String, String> SIBLING = Map.ofEntries(
            Map.entry("zh_hk", "zh_tw"), Map.entry("pt_pt", "pt_br"),
            Map.entry("es_mx", "es_es"), Map.entry("es_ar", "es_mx"), Map.entry("es_cl", "es_mx"),
            Map.entry("es_uy", "es_mx"), Map.entry("es_ve", "es_mx"), Map.entry("es_ec", "es_mx"),
            Map.entry("en_gb", "en_us"), Map.entry("en_au", "en_gb"), Map.entry("en_ca", "en_us"),
            Map.entry("en_nz", "en_gb"), Map.entry("fr_ca", "fr_fr"), Map.entry("de_at", "de_de"),
            Map.entry("de_ch", "de_de"), Map.entry("nl_be", "nl_nl"));

    private LocaleUtil() {
    }

    /** Lower case, underscores, never empty. */
    public static String normalize(String code) {
        if (code == null) {
            return DEFAULT;
        }
        String c = code.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return c.isEmpty() ? DEFAULT : c;
    }

    /** Language part of a code: {@code fr} for {@code fr_ca}. */
    public static String language(String code) {
        String c = normalize(code);
        int i = c.indexOf('_');
        return i < 0 ? c : c.substring(0, i);
    }

    /** Ordered list of codes to try for a player language. */
    public static List<String> chain(String code, String fallback) {
        List<String> out = new ArrayList<>(5);
        String c = normalize(code);
        add(out, c);
        String sibling = SIBLING.get(c);
        if (sibling != null) {
            add(out, sibling);
            String siblingOfSibling = SIBLING.get(sibling);
            if (siblingOfSibling != null) {
                add(out, siblingOfSibling);
            }
        }
        String primary = PRIMARY.get(language(c));
        if (primary != null) {
            add(out, primary);
        }
        add(out, normalize(fallback));
        add(out, DEFAULT);
        return out;
    }

    private static void add(List<String> list, String code) {
        if (!list.contains(code)) {
            list.add(code);
        }
    }
}
