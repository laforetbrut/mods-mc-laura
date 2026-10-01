package com.vyrriox.lauramod.entity;

import java.util.Locale;

/**
 * One-shot animations Laura can play. The id is synced to clients, which play the animation with the
 * same name ({@link #animationName()}) from the active animation set. Sounds and particles tied to
 * an emote are scheduled server side by {@link EmoteEffects}, so everybody hears them in sync.
 *
 * @author vyrriox
 */
public enum Emote {
    NONE(0, false, false),
    WAVE(50, true, true),
    HUG(60, true, false),
    KISS(50, true, false),
    DANCE(200, true, true),
    CLAP(42, true, true),
    LAUGH(50, true, true),
    CRY(80, true, false),
    BLUSH(50, true, true),
    FACEPALM(40, true, false),
    JUMP(30, true, true),
    BOW(40, true, true),
    THINK(60, true, false),
    SHRUG(30, true, false),
    STOMP(40, true, false),
    YAWN(50, true, false),
    EAT(32, false, false),
    POKE(20, false, false),
    SLAP(15, false, false),
    CELEBRATE(80, true, false),
    SNAP(34, true, true),
    TWIRL(34, true, true),
    HUM(90, true, true),
    STRETCH(56, true, true),
    TAP_FOOT(52, true, true),
    BLOW_KISS(34, true, true),
    SNEEZE(32, true, true),
    HAIR_FLIP(28, true, true),
    CHECK_NAILS(64, true, true),
    AIR_GUITAR(76, true, true),
    HICCUP(22, true, true),
    POUT(60, true, true),
    SHIVER(52, true, true),
    FAN(54, true, true);

    /** Duration in ticks. */
    public final int duration;
    /** Whether players can trigger it from the menu or the /laura emote command. */
    public final boolean playerTriggered;
    /** Whether she may play it on her own, when she feels like it. */
    public final boolean spontaneous;

    Emote(int duration, boolean playerTriggered, boolean spontaneous) {
        this.duration = duration;
        this.playerTriggered = playerTriggered;
        this.spontaneous = spontaneous;
    }

    public static Emote byId(int id) {
        Emote[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }

    public static Emote byName(String name) {
        for (Emote emote : values()) {
            if (emote.name().equalsIgnoreCase(name) || emote.animationName().equals(name)) {
                return emote;
            }
        }
        return NONE;
    }

    public String animationName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
