package com.vyrriox.lauramod.client.anim;

import com.vyrriox.lauramod.config.LauraClientConfig;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.Mood;
import com.vyrriox.lauramod.model.AnimationSampler;
import com.vyrriox.lauramod.model.ModelData;
import com.vyrriox.lauramod.model.Molang;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Chooses and blends Laura's animations: one looping "state" animation (idle, walk, sit, sad,
 * gagged...) plus the current emote, with short cross fades. Works for the default model and for
 * custom Blockbench models alike: the caller supplies the animation set.
 *
 * @author vyrriox
 */
public final class LauraAnimator {
    private static final float FADE = 0.25F;
    private static final Map<LauraEntity, State> STATES = new WeakHashMap<>();

    private LauraAnimator() {
    }

    private static final class State {
        String base = "";
        float baseStart;
        String previous = "";
        float previousStart;
        float switchTime = -10;
    }

    /** Name of the looping animation that fits what she is doing. */
    public static String baseState(LauraEntity laura, boolean moving, boolean hasWalk) {
        if (laura.isAsleep()) {
            return "sleep";
        }
        if (laura.isInSittingPose() || laura.isPassenger()) {
            return "sit";
        }
        if (laura.isSwimming() || laura.isInWater() && !laura.onGround()) {
            return "swim";
        }
        if (laura.isFetching() || !laura.getCarried().isEmpty() && laura.getEmote() != Emote.EAT) {
            return moving ? "carry_walk" : "carry";
        }
        if (laura.isGagged()) {
            return moving && hasWalk ? "walk" : "gagged";
        }
        if (moving) {
            return "walk";
        }
        Mood mood = laura.getMood();
        return switch (mood) {
            case SAD, SULKING -> "sad";
            case ANGRY, JEALOUS -> "angry";
            case TIRED -> "tired";
            case HUNGRY -> "hungry";
            case HAPPY, IN_LOVE -> "happy";
            default -> "idle";
        };
    }

    /**
     * Computes the bone poses for this frame.
     *
     * @param animations the animation set (by short name)
     * @param time       entity age in seconds, with the partial tick
     * @param gameTime   level game time in ticks, with the partial tick
     */
    public static Map<String, AnimationSampler.BonePose> compute(LauraEntity laura, Map<String, ModelData.Animation> animations,
                                                                 float time, float gameTime, boolean moving, Molang.Context ctx) {
        Map<String, AnimationSampler.BonePose> poses = new HashMap<>();
        if (!LauraClientConfig.animations.get() || animations.isEmpty()) {
            return poses;
        }
        State state = STATES.computeIfAbsent(laura, l -> new State());
        String wanted = resolve(animations, baseState(laura, moving, animations.containsKey("walk")));
        if (!wanted.equals(state.base)) {
            state.previous = state.base;
            state.previousStart = state.baseStart;
            state.base = wanted;
            state.baseStart = time;
            state.switchTime = time;
        }
        float blend = Math.min(1F, (time - state.switchTime) / FADE);

        Emote emote = laura.getEmote();
        ModelData.Animation emoteAnim = emote == Emote.NONE ? null : animations.get(emote.animationName());
        float emoteWeight = 0;
        float emoteTime = 0;
        if (emoteAnim != null) {
            emoteTime = Math.max(0, (gameTime - laura.getEmoteStart()) / 20F);
            float length = emoteAnim.length > 0 ? emoteAnim.length : emote.duration / 20F;
            float fadeIn = Math.min(1F, emoteTime / 0.15F);
            float fadeOut = emoteAnim.loop == ModelData.Loop.ONCE ? Math.min(1F, Math.max(0F, (length - emoteTime) / 0.2F)) : 1F;
            emoteWeight = Math.min(fadeIn, fadeOut);
        }

        float baseWeight = 1F - emoteWeight * 0.8F;
        ModelData.Animation base = animations.get(state.base);
        if (base != null) {
            AnimationSampler.apply(base, time - state.baseStart, blend * baseWeight, ctx, poses);
        }
        if (blend < 1F) {
            ModelData.Animation previous = animations.get(state.previous);
            if (previous != null) {
                AnimationSampler.apply(previous, time - state.previousStart, (1F - blend) * baseWeight, ctx, poses);
            }
        }
        if (emoteAnim != null && emoteWeight > 0) {
            AnimationSampler.apply(emoteAnim, emoteTime, emoteWeight, ctx, poses);
        }
        return poses;
    }

    /** Falls back to simpler animations when a model does not have the exact one. */
    private static String resolve(Map<String, ModelData.Animation> animations, String name) {
        if (animations.containsKey(name)) {
            return name;
        }
        return switch (name) {
            case "carry_walk" -> animations.containsKey("carry") ? "carry" : resolve(animations, "walk");
            case "happy", "tired", "hungry", "angry", "sad", "gagged", "carry", "swim", "sleep", "sit" -> animations.containsKey("idle") ? "idle" : name;
            default -> name;
        };
    }
}
