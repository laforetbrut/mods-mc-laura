package com.vyrriox.lauramod.model;

import java.util.List;
import java.util.Map;

/**
 * Samples Bedrock style animations at a given time. Values are returned raw (degrees for rotation,
 * pixels for position, factors for scale); the renderer applies its own axis convention.
 *
 * @author vyrriox
 */
public final class AnimationSampler {
    private AnimationSampler() {
    }

    /** Accumulated transform of one bone. */
    public static final class BonePose {
        public float rotX;
        public float rotY;
        public float rotZ;
        public float posX;
        public float posY;
        public float posZ;
        public float scaleX = 1;
        public float scaleY = 1;
        public float scaleZ = 1;

        public boolean isIdentity() {
            return rotX == 0 && rotY == 0 && rotZ == 0 && posX == 0 && posY == 0 && posZ == 0 && scaleX == 1 && scaleY == 1 && scaleZ == 1;
        }

        public void reset() {
            rotX = rotY = rotZ = posX = posY = posZ = 0;
            scaleX = scaleY = scaleZ = 1;
        }
    }

    /** Local time of an animation, honouring its loop mode. */
    public static float localTime(ModelData.Animation animation, float seconds) {
        float length = animation.length;
        if (length <= 0) {
            return seconds;
        }
        return switch (animation.loop) {
            case LOOP -> seconds % length;
            case HOLD, ONCE -> Math.min(seconds, length);
        };
    }

    /**
     * Adds the animation's contribution at {@code seconds} to the poses, scaled by {@code weight}.
     */
    public static void apply(ModelData.Animation animation, float seconds, float weight, Molang.Context ctx, Map<String, BonePose> poses) {
        if (weight <= 0.001F) {
            return;
        }
        float t = localTime(animation, seconds);
        ctx.animTime = t;
        float[] v = new float[3];
        for (Map.Entry<String, ModelData.BoneTimeline> entry : animation.bones.entrySet()) {
            ModelData.BoneTimeline timeline = entry.getValue();
            BonePose pose = poses.computeIfAbsent(entry.getKey(), k -> new BonePose());
            if (timeline.rotation != null && sample(timeline.rotation, t, ctx, v)) {
                pose.rotX += v[0] * weight;
                pose.rotY += v[1] * weight;
                pose.rotZ += v[2] * weight;
            }
            if (timeline.position != null && sample(timeline.position, t, ctx, v)) {
                pose.posX += v[0] * weight;
                pose.posY += v[1] * weight;
                pose.posZ += v[2] * weight;
            }
            if (timeline.scale != null && sample(timeline.scale, t, ctx, v)) {
                pose.scaleX *= 1 + (v[0] - 1) * weight;
                pose.scaleY *= 1 + (v[1] - 1) * weight;
                pose.scaleZ *= 1 + (v[2] - 1) * weight;
            }
        }
    }

    /** Samples a channel into {@code out}. Returns false for an empty channel. */
    public static boolean sample(ModelData.Channel channel, float t, Molang.Context ctx, float[] out) {
        List<ModelData.Keyframe> keys = channel.keyframes;
        int n = keys.size();
        if (n == 0) {
            return false;
        }
        ModelData.Keyframe first = keys.get(0);
        if (n == 1 || t <= first.time()) {
            eval(first, n == 1 || t > first.time(), ctx, out);
            return true;
        }
        ModelData.Keyframe last = keys.get(n - 1);
        if (t >= last.time()) {
            eval(last, true, ctx, out);
            return true;
        }
        int i = 0;
        while (i < n - 2 && keys.get(i + 1).time() <= t) {
            i++;
        }
        ModelData.Keyframe k0 = keys.get(i);
        ModelData.Keyframe k1 = keys.get(i + 1);
        float span = Math.max(1e-4F, k1.time() - k0.time());
        float alpha = (t - k0.time()) / span;
        if (k0.interpolation() == ModelData.Interpolation.STEP) {
            eval(k0, true, ctx, out);
            return true;
        }
        float[] a = new float[3];
        float[] b = new float[3];
        eval(k0, true, ctx, a);
        eval(k1, false, ctx, b);
        boolean smooth = k0.interpolation() == ModelData.Interpolation.CATMULLROM || k1.interpolation() == ModelData.Interpolation.CATMULLROM
                || k0.interpolation() == ModelData.Interpolation.BEZIER || k1.interpolation() == ModelData.Interpolation.BEZIER;
        if (smooth) {
            float[] p0 = new float[3];
            float[] p3 = new float[3];
            if (i > 0) {
                eval(keys.get(i - 1), true, ctx, p0);
            } else {
                System.arraycopy(a, 0, p0, 0, 3);
            }
            if (i + 2 < n) {
                eval(keys.get(i + 2), false, ctx, p3);
            } else {
                System.arraycopy(b, 0, p3, 0, 3);
            }
            for (int c = 0; c < 3; c++) {
                out[c] = catmullRom(alpha, p0[c], a[c], b[c], p3[c]);
            }
        } else {
            for (int c = 0; c < 3; c++) {
                out[c] = a[c] + (b[c] - a[c]) * alpha;
            }
        }
        return true;
    }

    private static void eval(ModelData.Keyframe k, boolean post, Molang.Context ctx, float[] out) {
        if (post) {
            out[0] = (float) k.x().eval(ctx);
            out[1] = (float) k.y().eval(ctx);
            out[2] = (float) k.z().eval(ctx);
        } else {
            out[0] = (float) k.preX().eval(ctx);
            out[1] = (float) k.preY().eval(ctx);
            out[2] = (float) k.preZ().eval(ctx);
        }
    }

    private static float catmullRom(float t, float p0, float p1, float p2, float p3) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5F * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
    }
}
