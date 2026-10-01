package com.vyrriox.lauramod.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Reads Blockbench files: {@code .bbmodel} projects (any entity format) and Bedrock / GeckoLib
 * {@code .geo.json} geometry with {@code .animation.json} animations. Geometry is baked into the
 * same space GeckoLib uses, so models look exactly like in Blockbench.
 *
 * @author vyrriox
 */
public final class ModelParser {
    private static final int MAX_CUBES = 4096;
    // A file that passes the size check must not be able to freeze or exhaust the clients that render
    // it: every count a client pays for at each frame, or in memory, has a limit.
    public static final int MAX_BONES = 1024;
    public static final int MAX_BONE_DEPTH = 64;
    public static final int MAX_TEXTURES = 8;
    /** Largest width or height of a texture, in pixels. */
    public static final int MAX_TEXTURE_SIZE = 2048;
    /** Pixels of all the textures of a model together (two textures of the largest size). */
    public static final long MAX_TEXTURE_PIXELS = 2L * MAX_TEXTURE_SIZE * MAX_TEXTURE_SIZE;

    private ModelParser() {
    }

    /**
     * Checks the textures of a model from their PNG headers, without decoding them: their number,
     * the size of each one and their total size. Throws IllegalArgumentException when one is not a
     * PNG or a limit is exceeded.
     */
    public static void checkTextures(List<byte[]> textures) {
        if (textures.size() > MAX_TEXTURES) {
            throw new IllegalArgumentException("too many textures (max " + MAX_TEXTURES + ")");
        }
        long pixels = 0;
        for (byte[] png : textures) {
            if (png == null || png.length < 24 || (png[0] & 0xFF) != 0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') {
                throw new IllegalArgumentException("a texture is not a PNG image");
            }
            long width = pngInt(png, 16);
            long height = pngInt(png, 20);
            if (width <= 0 || height <= 0 || width > MAX_TEXTURE_SIZE || height > MAX_TEXTURE_SIZE) {
                throw new IllegalArgumentException("a texture is larger than " + MAX_TEXTURE_SIZE + "x" + MAX_TEXTURE_SIZE);
            }
            pixels += width * height;
        }
        if (pixels > MAX_TEXTURE_PIXELS) {
            throw new IllegalArgumentException("the textures are too large together");
        }
    }

    private static long pngInt(byte[] b, int offset) {
        return ((long) (b[offset] & 0xFF) << 24) | ((b[offset + 1] & 0xFF) << 16) | ((b[offset + 2] & 0xFF) << 8) | (b[offset + 3] & 0xFF);
    }

    /**
     * Parses a model file.
     *
     * @param fileName  name of the main file ({@code x.bbmodel} or {@code x.geo.json})
     * @param json      its content
     * @param sideFiles gives the bytes of files next to it (animation json, texture png), may be null
     */
    public static ModelData parse(String fileName, String json, Function<String, byte[]> sideFiles) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        JsonObject root = JsonParser.parseReader(new JsonReader(new StringReader(json))).getAsJsonObject();
        ModelData model;
        if (lower.endsWith(".bbmodel")) {
            model = parseBbmodel(root);
        } else {
            model = parseGeo(root);
            if (sideFiles != null) {
                String stem = fileName.substring(0, fileName.length() - (lower.endsWith(".geo.json") ? ".geo.json".length() : ".json".length()));
                byte[] anim = sideFiles.apply(stem + ".animation.json");
                if (anim != null) {
                    parseAnimations(model, new String(anim, java.nio.charset.StandardCharsets.UTF_8));
                }
                byte[] texture = sideFiles.apply(stem + ".png");
                if (texture != null) {
                    model.textures.add(texture);
                }
            }
        }
        if (model.cubeCount() == 0) {
            throw new IllegalArgumentException("the model has no cube");
        }
        checkTextures(model.textures);
        return model;
    }

    // ------------------------------------------------------------------ bbmodel

    private static ModelData parseBbmodel(JsonObject root) {
        JsonObject resolution = obj(root, "resolution");
        int width = resolution == null ? 64 : intOf(resolution, "width", 64);
        int height = resolution == null ? 64 : intOf(resolution, "height", 64);
        JsonObject meta = obj(root, "meta");
        boolean projectBoxUv = meta != null && meta.has("box_uv") && meta.get("box_uv").getAsBoolean();
        ModelData model = new ModelData(width, height);

        Map<String, JsonObject> elements = new HashMap<>();
        for (JsonElement e : arr(root, "elements")) {
            JsonObject o = e.getAsJsonObject();
            String type = str(o, "type", "cube");
            if (type.equals("cube") && o.has("uuid")) {
                elements.put(o.get("uuid").getAsString(), o);
            }
        }
        Map<String, JsonObject> groups = new HashMap<>();
        for (JsonElement e : arr(root, "groups")) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("uuid")) {
                groups.put(o.get("uuid").getAsString(), o);
            }
        }
        Map<String, String> uuidToBone = new HashMap<>();
        ModelData.Bone looseRoot = null;
        for (JsonElement node : arr(root, "outliner")) {
            if (node.isJsonPrimitive()) {
                JsonObject element = elements.get(node.getAsString());
                if (element != null) {
                    if (looseRoot == null) {
                        looseRoot = new ModelData.Bone("root_loose");
                        register(model, looseRoot, null);
                    }
                    addBbCube(model, looseRoot, element, projectBoxUv);
                }
            } else if (node.isJsonObject()) {
                parseBbGroup(model, node.getAsJsonObject(), groups, elements, null, projectBoxUv, uuidToBone, 1);
            }
        }
        List<JsonElement> textures = arr(root, "textures");
        for (JsonElement t : textures) {
            String source = str(t.getAsJsonObject(), "source", "");
            int comma = source.indexOf(',');
            if (source.startsWith("data:image/png;base64,") && comma > 0) {
                model.textures.add(Base64.getDecoder().decode(source.substring(comma + 1)));
            }
        }
        for (JsonElement a : arr(root, "animations")) {
            parseBbAnimation(model, a.getAsJsonObject(), uuidToBone);
        }
        computeHeight(model);
        return model;
    }

    private static void parseBbGroup(ModelData model, JsonObject node, Map<String, JsonObject> groups, Map<String, JsonObject> elements,
                                     ModelData.Bone parent, boolean projectBoxUv, Map<String, String> uuidToBone, int depth) {
        if (depth > MAX_BONE_DEPTH) {
            throw new IllegalArgumentException("groups nested too deep (max " + MAX_BONE_DEPTH + ")");
        }
        JsonObject data = node;
        if (!node.has("name") && node.has("uuid") && groups.containsKey(node.get("uuid").getAsString())) {
            data = groups.get(node.get("uuid").getAsString());
        }
        if (data.has("export") && !data.get("export").getAsBoolean()) {
            return;
        }
        ModelData.Bone bone = new ModelData.Bone(str(data, "name", "bone"));
        float[] origin = vec(data, "origin");
        float[] rotation = vec(data, "rotation");
        // Blockbench project space is the baked space: no conversion needed.
        bone.pivotX = origin[0];
        bone.pivotY = origin[1];
        bone.pivotZ = origin[2];
        bone.rotX = (float) Math.toRadians(rotation[0]);
        bone.rotY = (float) Math.toRadians(rotation[1]);
        bone.rotZ = (float) Math.toRadians(rotation[2]);
        register(model, bone, parent);
        if (node.has("uuid")) {
            uuidToBone.put(node.get("uuid").getAsString(), bone.name);
        }
        boolean mirror = data.has("mirror_uv") && data.get("mirror_uv").getAsBoolean();
        for (JsonElement child : arr(node, "children")) {
            if (child.isJsonPrimitive()) {
                JsonObject element = elements.get(child.getAsString());
                if (element != null) {
                    addBbCube(model, bone, element, projectBoxUv || mirror && element.has("uv_offset"));
                }
            } else if (child.isJsonObject()) {
                parseBbGroup(model, child.getAsJsonObject(), groups, elements, bone, projectBoxUv, uuidToBone, depth + 1);
            }
        }
    }

    private static void addBbCube(ModelData model, ModelData.Bone bone, JsonObject e, boolean projectBoxUv) {
        if (e.has("export") && !e.get("export").getAsBoolean() || e.has("visibility") && !e.get("visibility").getAsBoolean()) {
            return;
        }
        if (model.cubeCount() >= MAX_CUBES) {
            throw new IllegalArgumentException("too many cubes");
        }
        float[] from = vec(e, "from");
        float[] to = vec(e, "to");
        float[] origin = vec(e, "origin");
        float[] rotation = vec(e, "rotation");
        float inflate = e.has("inflate") ? e.get("inflate").getAsFloat() : 0;
        boolean boxUv = e.has("box_uv") ? e.get("box_uv").getAsBoolean() : projectBoxUv;
        boolean mirror = e.has("mirror_uv") && e.get("mirror_uv").getAsBoolean();
        float[] size = {to[0] - from[0], to[1] - from[1], to[2] - from[2]};
        ModelData.Cube cube = new ModelData.Cube();
        cube.pivotX = origin[0];
        cube.pivotY = origin[1];
        cube.pivotZ = origin[2];
        cube.rotX = (float) Math.toRadians(rotation[0]);
        cube.rotY = (float) Math.toRadians(rotation[1]);
        cube.rotZ = (float) Math.toRadians(rotation[2]);
        if (boxUv) {
            float[] uv = e.has("uv_offset") ? vec2(e, "uv_offset") : new float[]{0, 0};
            buildBoxUv(model, cube, from, size, inflate, uv[0], uv[1], mirror);
        } else {
            JsonObject faces = obj(e, "faces");
            Map<Dir, FaceUv> uvs = new HashMap<>();
            if (faces != null) {
                for (Dir dir : Dir.values()) {
                    JsonObject face = obj(faces, dir.key);
                    if (face == null || !face.has("uv") || face.has("texture") && face.get("texture").isJsonNull()) {
                        continue;
                    }
                    JsonArray uv = face.getAsJsonArray("uv");
                    float u1 = uv.get(0).getAsFloat();
                    float v1 = uv.get(1).getAsFloat();
                    float u2 = uv.get(2).getAsFloat();
                    float v2 = uv.get(3).getAsFloat();
                    int texture = face.has("texture") && face.get("texture").isJsonPrimitive() ? face.get("texture").getAsInt() : 0;
                    int rot = face.has("rotation") ? face.get("rotation").getAsInt() : 0;
                    // Same conversion as the Blockbench Bedrock export (up and down faces are flipped).
                    float us = u2 - u1;
                    float vs = v2 - v1;
                    if (dir == Dir.UP || dir == Dir.DOWN) {
                        u1 += us;
                        v1 += vs;
                        us = -us;
                        vs = -vs;
                    }
                    uvs.put(dir, new FaceUv(u1, v1, us, vs, rot, texture));
                }
            }
            buildFaceUv(model, cube, from, size, inflate, uvs, mirror);
        }
        bone.cubes.add(cube);
    }

    private static void parseBbAnimation(ModelData model, JsonObject a, Map<String, String> uuidToBone) {
        String fullName = str(a, "name", "animation");
        String name = shortName(fullName);
        String loopRaw = str(a, "loop", "once");
        ModelData.Loop loop = loopRaw.equals("loop") ? ModelData.Loop.LOOP : loopRaw.equals("hold") ? ModelData.Loop.HOLD : ModelData.Loop.ONCE;
        float length = a.has("length") ? a.get("length").getAsFloat() : 0;
        ModelData.Animation animation = new ModelData.Animation(name, length, loop);
        JsonObject animators = obj(a, "animators");
        if (animators != null) {
            for (Map.Entry<String, JsonElement> entry : animators.entrySet()) {
                JsonObject animator = entry.getValue().getAsJsonObject();
                if (!str(animator, "type", "bone").equals("bone")) {
                    continue;
                }
                String boneName = uuidToBone.getOrDefault(entry.getKey(), str(animator, "name", ""));
                ModelData.BoneTimeline timeline = new ModelData.BoneTimeline();
                Map<String, List<JsonObject>> byChannel = new HashMap<>();
                for (JsonElement k : arr(animator, "keyframes")) {
                    JsonObject kf = k.getAsJsonObject();
                    byChannel.computeIfAbsent(str(kf, "channel", "rotation"), c -> new ArrayList<>()).add(kf);
                }
                timeline.rotation = bbChannel(byChannel.get("rotation"));
                timeline.position = bbChannel(byChannel.get("position"));
                timeline.scale = bbChannel(byChannel.get("scale"));
                animation.bones.put(boneName.toLowerCase(Locale.ROOT), timeline);
            }
        }
        model.animations.put(name, animation);
    }

    private static ModelData.Channel bbChannel(List<JsonObject> keyframes) {
        if (keyframes == null || keyframes.isEmpty()) {
            return null;
        }
        keyframes.sort((x, y) -> Float.compare(x.get("time").getAsFloat(), y.get("time").getAsFloat()));
        ModelData.Channel channel = new ModelData.Channel();
        for (JsonObject kf : keyframes) {
            List<JsonElement> points = arr(kf, "data_points");
            if (points.isEmpty()) {
                continue;
            }
            JsonObject pre = points.get(0).getAsJsonObject();
            JsonObject post = points.size() > 1 ? points.get(1).getAsJsonObject() : pre;
            ModelData.Interpolation interpolation = switch (str(kf, "interpolation", "linear")) {
                case "catmullrom" -> ModelData.Interpolation.CATMULLROM;
                case "step" -> ModelData.Interpolation.STEP;
                case "bezier" -> ModelData.Interpolation.BEZIER;
                default -> ModelData.Interpolation.LINEAR;
            };
            channel.keyframes.add(new ModelData.Keyframe(kf.get("time").getAsFloat(),
                    Molang.parse(point(post, "x")), Molang.parse(point(post, "y")), Molang.parse(point(post, "z")),
                    Molang.parse(point(pre, "x")), Molang.parse(point(pre, "y")), Molang.parse(point(pre, "z")), interpolation));
        }
        return channel;
    }

    private static String point(JsonObject p, String axis) {
        JsonElement v = p.get(axis);
        return v == null ? "0" : v.getAsString();
    }

    // ------------------------------------------------------------------ bedrock geometry

    private static ModelData parseGeo(JsonObject root) {
        JsonObject geometry = null;
        if (root.has("minecraft:geometry")) {
            JsonArray list = root.getAsJsonArray("minecraft:geometry");
            if (!list.isEmpty()) {
                geometry = list.get(0).getAsJsonObject();
            }
        } else {
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                if (e.getKey().startsWith("geometry.") && e.getValue().isJsonObject()) {
                    geometry = e.getValue().getAsJsonObject();
                    break;
                }
            }
        }
        if (geometry == null) {
            throw new IllegalArgumentException("no minecraft:geometry");
        }
        JsonObject description = obj(geometry, "description");
        int width = description != null ? intOf(description, "texture_width", 64) : intOf(geometry, "texturewidth", 64);
        int height = description != null ? intOf(description, "texture_height", 64) : intOf(geometry, "textureheight", 64);
        ModelData model = new ModelData(width, height);
        Map<String, ModelData.Bone> byName = new HashMap<>();
        Map<ModelData.Bone, String> parents = new HashMap<>();
        List<ModelData.Bone> order = new ArrayList<>();
        for (JsonElement b : arr(geometry, "bones")) {
            JsonObject o = b.getAsJsonObject();
            ModelData.Bone bone = new ModelData.Bone(str(o, "name", "bone"));
            float[] pivot = vec(o, "pivot");
            float[] rotation = vec(o, "rotation");
            // Bedrock to baked space: mirror X, negate X and Y rotations (like GeckoLib).
            bone.pivotX = -pivot[0];
            bone.pivotY = pivot[1];
            bone.pivotZ = pivot[2];
            bone.rotX = (float) Math.toRadians(-rotation[0]);
            bone.rotY = (float) Math.toRadians(-rotation[1]);
            bone.rotZ = (float) Math.toRadians(rotation[2]);
            boolean boneMirror = o.has("mirror") && o.get("mirror").getAsBoolean();
            float boneInflate = o.has("inflate") ? o.get("inflate").getAsFloat() : 0;
            for (JsonElement c : arr(o, "cubes")) {
                addGeoCube(model, bone, c.getAsJsonObject(), boneMirror, boneInflate);
            }
            byName.put(bone.name.toLowerCase(Locale.ROOT), bone);
            order.add(bone);
            if (o.has("parent")) {
                parents.put(bone, o.get("parent").getAsString().toLowerCase(Locale.ROOT));
            }
        }
        for (ModelData.Bone bone : order) {
            String parentName = parents.get(bone);
            ModelData.Bone parent = parentName == null ? null : byName.get(parentName);
            register(model, bone, parent);
        }
        // Parents are given by name: a loop (a bone that is its own ancestor) or an endless chain
        // would hang everything that walks up from a bone.
        for (ModelData.Bone bone : order) {
            int depth = 0;
            for (ModelData.Bone b = bone.parent; b != null; b = b.parent) {
                if (++depth > MAX_BONE_DEPTH) {
                    throw new IllegalArgumentException("bones nested too deep or in a loop (max " + MAX_BONE_DEPTH + ")");
                }
            }
        }
        computeHeight(model);
        return model;
    }

    private static void addGeoCube(ModelData model, ModelData.Bone bone, JsonObject c, boolean boneMirror, float boneInflate) {
        if (model.cubeCount() >= MAX_CUBES) {
            throw new IllegalArgumentException("too many cubes");
        }
        float[] origin = vec(c, "origin");
        float[] size = vec(c, "size");
        float[] pivot = c.has("pivot") ? vec(c, "pivot") : new float[]{0, 0, 0};
        float[] rotation = vec(c, "rotation");
        float inflate = c.has("inflate") ? c.get("inflate").getAsFloat() : boneInflate;
        boolean mirror = c.has("mirror") ? c.get("mirror").getAsBoolean() : boneMirror;
        ModelData.Cube cube = new ModelData.Cube();
        cube.pivotX = -pivot[0];
        cube.pivotY = pivot[1];
        cube.pivotZ = pivot[2];
        cube.rotX = (float) Math.toRadians(-rotation[0]);
        cube.rotY = (float) Math.toRadians(-rotation[1]);
        cube.rotZ = (float) Math.toRadians(rotation[2]);
        float[] from = {-(origin[0] + size[0]), origin[1], origin[2]};
        JsonElement uv = c.get("uv");
        if (uv == null || uv.isJsonArray()) {
            float u = uv == null ? 0 : uv.getAsJsonArray().get(0).getAsFloat();
            float v = uv == null ? 0 : uv.getAsJsonArray().get(1).getAsFloat();
            buildBoxUv(model, cube, from, size, inflate, u, v, mirror);
        } else {
            JsonObject faces = uv.getAsJsonObject();
            Map<Dir, FaceUv> uvs = new HashMap<>();
            for (Dir dir : Dir.values()) {
                JsonObject face = obj(faces, dir.key);
                if (face == null || !face.has("uv")) {
                    continue;
                }
                float[] f = vec2(face, "uv");
                float[] s = face.has("uv_size") ? vec2(face, "uv_size") : new float[]{0, 0};
                int rot = face.has("uv_rotation") ? face.get("uv_rotation").getAsInt() : 0;
                uvs.put(dir, new FaceUv(f[0], f[1], s[0], s[1], rot, 0));
            }
            buildFaceUv(model, cube, from, size, inflate, uvs, mirror);
        }
        bone.cubes.add(cube);
    }

    // ------------------------------------------------------------------ bedrock animations

    public static void parseAnimations(ModelData model, String json) {
        JsonObject root = JsonParser.parseReader(new JsonReader(new StringReader(json))).getAsJsonObject();
        JsonObject animations = obj(root, "animations");
        if (animations == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject a = entry.getValue().getAsJsonObject();
            ModelData.Loop loop = ModelData.Loop.ONCE;
            JsonElement loopEl = a.get("loop");
            if (loopEl != null && loopEl.isJsonPrimitive()) {
                if (loopEl.getAsJsonPrimitive().isBoolean()) {
                    loop = loopEl.getAsBoolean() ? ModelData.Loop.LOOP : ModelData.Loop.ONCE;
                } else if ("hold_on_last_frame".equals(loopEl.getAsString())) {
                    loop = ModelData.Loop.HOLD;
                }
            }
            float length = a.has("animation_length") ? a.get("animation_length").getAsFloat() : 0;
            String name = shortName(entry.getKey());
            ModelData.Animation animation = new ModelData.Animation(name, length, loop);
            JsonObject bones = obj(a, "bones");
            float maxTime = 0;
            if (bones != null) {
                for (Map.Entry<String, JsonElement> b : bones.entrySet()) {
                    JsonObject bo = b.getValue().getAsJsonObject();
                    ModelData.BoneTimeline timeline = new ModelData.BoneTimeline();
                    timeline.rotation = geoChannel(bo.get("rotation"));
                    timeline.position = geoChannel(bo.get("position"));
                    timeline.scale = geoChannel(bo.get("scale"));
                    for (ModelData.Channel ch : new ModelData.Channel[]{timeline.rotation, timeline.position, timeline.scale}) {
                        if (ch != null && !ch.keyframes.isEmpty()) {
                            maxTime = Math.max(maxTime, ch.keyframes.get(ch.keyframes.size() - 1).time());
                        }
                    }
                    animation.bones.put(b.getKey().toLowerCase(Locale.ROOT), timeline);
                }
            }
            if (length <= 0) {
                animation = copyWithLength(animation, maxTime);
            }
            model.animations.put(name, animation);
        }
    }

    private static ModelData.Animation copyWithLength(ModelData.Animation a, float length) {
        ModelData.Animation copy = new ModelData.Animation(a.name, length, a.loop);
        copy.bones.putAll(a.bones);
        return copy;
    }

    private static ModelData.Channel geoChannel(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        ModelData.Channel channel = new ModelData.Channel();
        if (element.isJsonArray() || element.isJsonPrimitive()) {
            String[] v = triple(element);
            channel.keyframes.add(new ModelData.Keyframe(0, Molang.parse(v[0]), Molang.parse(v[1]), Molang.parse(v[2]),
                    Molang.parse(v[0]), Molang.parse(v[1]), Molang.parse(v[2]), ModelData.Interpolation.LINEAR));
            return channel;
        }
        JsonObject o = element.getAsJsonObject();
        if (o.has("vector")) {
            String[] v = triple(o.get("vector"));
            channel.keyframes.add(new ModelData.Keyframe(0, Molang.parse(v[0]), Molang.parse(v[1]), Molang.parse(v[2]),
                    Molang.parse(v[0]), Molang.parse(v[1]), Molang.parse(v[2]), ModelData.Interpolation.LINEAR));
            return channel;
        }
        List<Map.Entry<String, JsonElement>> entries = new ArrayList<>(o.entrySet());
        entries.sort((x, y) -> Float.compare(Float.parseFloat(x.getKey()), Float.parseFloat(y.getKey())));
        for (Map.Entry<String, JsonElement> e : entries) {
            float time = Float.parseFloat(e.getKey());
            JsonElement value = e.getValue();
            String[] post;
            String[] pre;
            ModelData.Interpolation interpolation = ModelData.Interpolation.LINEAR;
            if (value.isJsonObject()) {
                JsonObject kf = value.getAsJsonObject();
                post = kf.has("post") ? triple(kf.get("post")) : kf.has("vector") ? triple(kf.get("vector")) : triple(kf.get("pre"));
                pre = kf.has("pre") ? triple(kf.get("pre")) : post;
                String lerp = kf.has("lerp_mode") ? kf.get("lerp_mode").getAsString() : "linear";
                if (lerp.equals("catmullrom")) {
                    interpolation = ModelData.Interpolation.CATMULLROM;
                } else if (lerp.equals("step")) {
                    interpolation = ModelData.Interpolation.STEP;
                }
            } else {
                post = triple(value);
                pre = post;
            }
            channel.keyframes.add(new ModelData.Keyframe(time, Molang.parse(post[0]), Molang.parse(post[1]), Molang.parse(post[2]),
                    Molang.parse(pre[0]), Molang.parse(pre[1]), Molang.parse(pre[2]), interpolation));
        }
        return channel;
    }

    private static String[] triple(JsonElement e) {
        if (e == null) {
            return new String[]{"0", "0", "0"};
        }
        if (e.isJsonPrimitive()) {
            String s = e.getAsString();
            return new String[]{s, s, s};
        }
        if (e.isJsonObject() && e.getAsJsonObject().has("vector")) {
            return triple(e.getAsJsonObject().get("vector"));
        }
        JsonArray a = e.getAsJsonArray();
        return new String[]{a.get(0).getAsString(), a.size() > 1 ? a.get(1).getAsString() : "0", a.size() > 2 ? a.get(2).getAsString() : "0"};
    }

    // ------------------------------------------------------------------ baking (GeckoLib compatible)

    private enum Dir {
        WEST("west", -1, 0, 0), EAST("east", 1, 0, 0), NORTH("north", 0, 0, -1), SOUTH("south", 0, 0, 1), UP("up", 0, 1, 0), DOWN("down", 0, -1, 0);

        final String key;
        final float nx;
        final float ny;
        final float nz;

        Dir(String key, float nx, float ny, float nz) {
            this.key = key;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
        }
    }

    private record FaceUv(float u, float v, float uSize, float vSize, int rotation, int texture) {
    }

    /** The eight corners of a cube in baked space. */
    private static float[][] corners(float[] from, float[] size, float inflate) {
        float x0 = from[0] - inflate;
        float y0 = from[1] - inflate;
        float z0 = from[2] - inflate;
        float x1 = from[0] + size[0] + inflate;
        float y1 = from[1] + size[1] + inflate;
        float z1 = from[2] + size[2] + inflate;
        return new float[][]{
                {x0, y0, z0}, // 0 bottomLeftBack
                {x0, y0, z1}, // 1 bottomRightBack
                {x0, y1, z0}, // 2 topLeftBack
                {x0, y1, z1}, // 3 topRightBack
                {x1, y1, z0}, // 4 topLeftFront
                {x1, y1, z1}, // 5 topRightFront
                {x1, y0, z0}, // 6 bottomLeftFront
                {x1, y0, z1}, // 7 bottomRightFront
        };
    }

    private static int[] quadIndices(Dir dir, boolean boxUv, boolean mirror) {
        int[] west = {3, 2, 0, 1};
        int[] east = {4, 5, 7, 6};
        int[] north = {2, 4, 6, 0};
        int[] south = {5, 3, 1, 7};
        int[] up = {3, 5, 4, 2};
        int[] down = {0, 6, 7, 1};
        return switch (dir) {
            case WEST -> mirror ? east : west;
            case EAST -> mirror ? west : east;
            case NORTH -> north;
            case SOUTH -> south;
            case UP -> mirror && !boxUv ? down : up;
            case DOWN -> mirror && !boxUv ? up : down;
        };
    }

    private static void buildBoxUv(ModelData model, ModelData.Cube cube, float[] from, float[] size, float inflate, float u, float v, boolean mirror) {
        float[][] corners = corners(from, size, inflate);
        float w = (float) Math.floor(size[0]);
        float h = (float) Math.floor(size[1]);
        float d = (float) Math.floor(size[2]);
        for (Dir dir : Dir.values()) {
            float[] uvData = switch (dir) {
                case WEST -> new float[]{u + d + w, v + d, d, h};
                case EAST -> new float[]{u, v + d, d, h};
                case NORTH -> new float[]{u + d, v + d, w, h};
                case SOUTH -> new float[]{u + d + w + d, v + d, w, h};
                case UP -> new float[]{u + d, v, w, d};
                case DOWN -> new float[]{u + d + w, v + d, w, -d};
            };
            addQuad(model, cube, corners, quadIndices(dir, true, mirror), dir, uvData[0], uvData[1], uvData[2], uvData[3], 0, mirror, 0);
        }
    }

    private static void buildFaceUv(ModelData model, ModelData.Cube cube, float[] from, float[] size, float inflate, Map<Dir, FaceUv> uvs, boolean mirror) {
        float[][] corners = corners(from, size, inflate);
        for (Dir dir : Dir.values()) {
            FaceUv face = uvs.get(dir);
            if (face == null) {
                continue;
            }
            addQuad(model, cube, corners, quadIndices(dir, false, mirror), dir, face.u(), face.v(), face.uSize(), face.vSize(), face.rotation(), mirror, face.texture());
        }
    }

    private static void addQuad(ModelData model, ModelData.Cube cube, float[][] corners, int[] idx, Dir dir,
                                float u, float v, float uSize, float vSize, int rotation, boolean mirror, int texture) {
        float tw = model.textureWidth;
        float th = model.textureHeight;
        float uWidth = (u + uSize) / tw;
        float vHeight = (v + vSize) / th;
        float u0 = u / tw;
        float v0 = v / th;
        float nx = dir.nx;
        if (!mirror) {
            float tmp = uWidth;
            uWidth = u0;
            u0 = tmp;
        } else {
            nx = -nx;
        }
        float[] uvs = switch (Math.floorMod(rotation, 360) / 90) {
            case 1 -> new float[]{uWidth, v0, uWidth, vHeight, u0, vHeight, u0, v0};
            case 2 -> new float[]{uWidth, vHeight, u0, vHeight, u0, v0, uWidth, v0};
            case 3 -> new float[]{u0, vHeight, u0, v0, uWidth, v0, uWidth, vHeight};
            default -> new float[]{u0, v0, uWidth, v0, uWidth, vHeight, u0, vHeight};
        };
        float[] positions = new float[12];
        for (int i = 0; i < 4; i++) {
            float[] c = corners[idx[i]];
            positions[i * 3] = c[0];
            positions[i * 3 + 1] = c[1];
            positions[i * 3 + 2] = c[2];
        }
        cube.quads.add(new ModelData.Quad(positions, uvs, nx, dir.ny, dir.nz, texture));
    }

    // ------------------------------------------------------------------ helpers

    private static void register(ModelData model, ModelData.Bone bone, ModelData.Bone parent) {
        if (model.bones.size() >= MAX_BONES) {
            throw new IllegalArgumentException("too many bones (max " + MAX_BONES + ")");
        }
        String key = bone.name.toLowerCase(Locale.ROOT);
        if (model.bones.containsKey(key)) {
            key = key + "_" + model.bones.size();
        }
        model.bones.put(key, bone);
        bone.parent = parent;
        if (parent == null) {
            model.roots.add(bone);
        } else {
            parent.children.add(bone);
        }
    }

    private static void computeHeight(ModelData model) {
        float max = 0;
        for (ModelData.Bone bone : model.bones.values()) {
            for (ModelData.Cube cube : bone.cubes) {
                for (ModelData.Quad q : cube.quads) {
                    for (int i = 1; i < 12; i += 3) {
                        max = Math.max(max, q.positions()[i]);
                    }
                }
            }
        }
        model.height = max <= 0 ? 32 : max;
    }

    static String shortName(String full) {
        String n = full.toLowerCase(Locale.ROOT);
        int dot = n.lastIndexOf('.');
        return dot >= 0 && dot + 1 < n.length() ? n.substring(dot + 1) : n;
    }

    private static JsonObject obj(JsonObject o, String key) {
        return o != null && o.has(key) && o.get(key).isJsonObject() ? o.getAsJsonObject(key) : null;
    }

    private static List<JsonElement> arr(JsonObject o, String key) {
        List<JsonElement> out = new ArrayList<>();
        if (o != null && o.has(key) && o.get(key).isJsonArray()) {
            o.getAsJsonArray(key).forEach(out::add);
        }
        return out;
    }

    private static String str(JsonObject o, String key, String def) {
        return o != null && o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : def;
    }

    private static int intOf(JsonObject o, String key, int def) {
        try {
            return o.has(key) ? o.get(key).getAsInt() : def;
        } catch (RuntimeException e) {
            return def;
        }
    }

    private static float[] vec(JsonObject o, String key) {
        float[] out = {0, 0, 0};
        if (o != null && o.has(key) && o.get(key).isJsonArray()) {
            JsonArray a = o.getAsJsonArray(key);
            for (int i = 0; i < 3 && i < a.size(); i++) {
                out[i] = a.get(i).getAsFloat();
            }
        }
        return out;
    }

    private static float[] vec2(JsonObject o, String key) {
        float[] out = {0, 0};
        if (o != null && o.has(key) && o.get(key).isJsonArray()) {
            JsonArray a = o.getAsJsonArray(key);
            for (int i = 0; i < 2 && i < a.size(); i++) {
                out[i] = a.get(i).getAsFloat();
            }
        }
        return out;
    }
}
