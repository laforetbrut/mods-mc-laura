package com.vyrriox.lauramod.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A parsed Blockbench model, already "baked" the way GeckoLib does it: every position is in pixels
 * (1/16 block), X is mirrored from the Bedrock file, rotations are in radians and applied Z, then Y,
 * then X. Pure data: no Minecraft class, so the server can validate uploads with it.
 *
 * @author vyrriox
 */
public final class ModelData {
    public final int textureWidth;
    public final int textureHeight;
    public final List<Bone> roots = new ArrayList<>();
    public final Map<String, Bone> bones = new LinkedHashMap<>();
    /** PNG bytes of the textures, in the order faces refer to them. */
    public final List<byte[]> textures = new ArrayList<>();
    public final Map<String, Animation> animations = new LinkedHashMap<>();
    /** Model height in pixels, used to scale the shadow and the name tag. */
    public float height = 32;
    private volatile Map<String, Bone> humanoidParts;

    public ModelData(int textureWidth, int textureHeight) {
        this.textureWidth = Math.max(1, textureWidth);
        this.textureHeight = Math.max(1, textureHeight);
    }

    public Bone bone(String name) {
        return name == null ? null : bones.get(name.toLowerCase(Locale.ROOT));
    }

    /** Finds a bone among several usual names (head, Head, bipedHead...). */
    public Bone findBone(String... names) {
        for (String name : names) {
            Bone b = bone(name);
            if (b != null) {
                return b;
            }
        }
        return null;
    }

    /** Animation by its short name (the part after the last dot: "animation.laura.walk" is "walk"). */
    public Animation animation(String name) {
        return animations.get(name);
    }

    /** Head, body, arms and legs of this model, found by bone name (see {@link HumanoidParts}). */
    public Map<String, Bone> humanoidParts() {
        Map<String, Bone> parts = humanoidParts;
        if (parts == null) {
            parts = HumanoidParts.find(roots);
            humanoidParts = parts;
        }
        return parts;
    }

    /** One part ("head", "right_arm"...), or null when the model has none. */
    public Bone part(String part) {
        return humanoidParts().get(part);
    }

    public int cubeCount() {
        int n = 0;
        for (Bone b : bones.values()) {
            n += b.cubes.size();
        }
        return n;
    }

    /** A group of cubes that moves together. */
    public static final class Bone {
        public final String name;
        public Bone parent;
        public final List<Bone> children = new ArrayList<>();
        public final List<Cube> cubes = new ArrayList<>();
        /** Pivot, mirrored X, in pixels. */
        public float pivotX;
        public float pivotY;
        public float pivotZ;
        /** Rest rotation in radians. */
        public float rotX;
        public float rotY;
        public float rotZ;

        public Bone(String name) {
            this.name = name;
        }
    }

    /** One textured box (six quads, some possibly missing). */
    public static final class Cube {
        public float pivotX;
        public float pivotY;
        public float pivotZ;
        public float rotX;
        public float rotY;
        public float rotZ;
        public final List<Quad> quads = new ArrayList<>(6);
    }

    /** Four vertices in pixels with their UV (0..1) and a normal. */
    public record Quad(float[] positions, float[] uvs, float nx, float ny, float nz, int texture) {
    }

    /** A Bedrock style animation. */
    public static final class Animation {
        public final String name;
        public final float length;
        public final Loop loop;
        public final Map<String, BoneTimeline> bones = new LinkedHashMap<>();

        public Animation(String name, float length, Loop loop) {
            this.name = name;
            this.length = length;
            this.loop = loop;
        }
    }

    public enum Loop {
        ONCE, LOOP, HOLD
    }

    /** Channels of one bone. Any of them can be null. */
    public static final class BoneTimeline {
        public Channel rotation;
        public Channel position;
        public Channel scale;
    }

    /** Keyframes of one channel, sorted by time. */
    public static final class Channel {
        public final List<Keyframe> keyframes = new ArrayList<>();

        public List<Keyframe> keyframes() {
            return Collections.unmodifiableList(keyframes);
        }
    }

    /** One keyframe: time in seconds, three expressions, interpolation towards the next keyframe. */
    public record Keyframe(float time, Molang.Expr x, Molang.Expr y, Molang.Expr z, Molang.Expr preX, Molang.Expr preY, Molang.Expr preZ, Interpolation interpolation) {
    }

    public enum Interpolation {
        LINEAR, CATMULLROM, STEP, BEZIER
    }
}
