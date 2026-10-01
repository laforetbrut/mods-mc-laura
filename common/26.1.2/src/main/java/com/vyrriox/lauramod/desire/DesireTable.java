package com.vyrriox.lauramod.desire;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.util.ItemSpec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The pool of things Laura can desire, from {@code config/lauramod/desires.json}.
 *
 * @author vyrriox
 */
public final class DesireTable {
    public record ItemDesire(ItemSpec item, boolean eat, int weight) {
    }

    /** A place is a list of biome ids, biome tags (#...) or dimension ids (dimension:...). */
    public record PlaceDesire(String id, List<String> targets, Item icon, int weight) {
    }

    public record ActivityDesire(DesireType.Activity activity, int weight) {
    }

    private static volatile List<ItemDesire> items = Collections.emptyList();
    private static volatile List<PlaceDesire> places = Collections.emptyList();
    private static volatile List<ActivityDesire> activities = Collections.emptyList();
    private static volatile int itemWeight = 45;
    private static volatile int placeWeight = 20;
    private static volatile int activityWeight = 35;

    private DesireTable() {
    }

    public static synchronized void load() {
        Path file = LauraMod.configDir().resolve("desires.json");
        String text = DEFAULTS;
        try {
            if (Files.isRegularFile(file)) {
                text = Files.readString(file, StandardCharsets.UTF_8);
            } else {
                Files.createDirectories(file.getParent());
                Files.writeString(file, DEFAULTS, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            LauraMod.LOGGER.warn("Could not read {}: {}", file, e.getMessage());
        }
        JsonObject root;
        try {
            root = JsonParser.parseReader(new JsonReader(new StringReader(text))).getAsJsonObject();
        } catch (Exception e) {
            LauraMod.LOGGER.error("{} is invalid ({}), using the defaults", file, e.getMessage());
            root = JsonParser.parseReader(new JsonReader(new StringReader(DEFAULTS))).getAsJsonObject();
        }
        com.vyrriox.lauramod.api.ScriptData.extend("desires", root);
        JsonObject weights = root.has("categoryWeights") && root.get("categoryWeights").isJsonObject() ? root.getAsJsonObject("categoryWeights") : new JsonObject();
        itemWeight = Math.max(0, integer(weights, "items", 45));
        placeWeight = Math.max(0, integer(weights, "places", 20));
        activityWeight = Math.max(0, integer(weights, "activities", 35));

        List<ItemDesire> parsedItems = new ArrayList<>();
        for (JsonElement e : array(root, "items")) {
            if (e.isJsonObject()) {
                JsonObject o = e.getAsJsonObject();
                ItemSpec.parse(str(o, "item", "")).ifPresent(spec -> parsedItems.add(new ItemDesire(spec, bool(o, "eat", false), Math.max(1, integer(o, "weight", 10)))));
            }
        }
        List<PlaceDesire> parsedPlaces = new ArrayList<>();
        for (JsonElement e : array(root, "places")) {
            if (e.isJsonObject()) {
                JsonObject o = e.getAsJsonObject();
                List<String> targets = new ArrayList<>();
                for (JsonElement t : array(o, "biomes")) {
                    // Only text is a biome, a tag or a dimension: anything else is skipped.
                    if (t.isJsonPrimitive()) {
                        targets.add(t.getAsString());
                    }
                }
                String id = str(o, "id", "");
                if (!id.isEmpty() && !targets.isEmpty()) {
                    parsedPlaces.add(new PlaceDesire(id.toLowerCase(Locale.ROOT), List.copyOf(targets), item(str(o, "icon", "minecraft:grass_block")), Math.max(1, integer(o, "weight", 10))));
                }
            }
        }
        List<ActivityDesire> parsedActivities = new ArrayList<>();
        for (JsonElement e : array(root, "activities")) {
            if (e.isJsonObject()) {
                JsonObject o = e.getAsJsonObject();
                DesireType.Activity activity = DesireType.Activity.byName(str(o, "id", ""));
                int weight = integer(o, "weight", 10);
                if (activity != null && weight > 0) {
                    parsedActivities.add(new ActivityDesire(activity, weight));
                }
            }
        }
        items = List.copyOf(parsedItems);
        places = List.copyOf(parsedPlaces);
        activities = List.copyOf(parsedActivities);
    }

    public static List<ItemDesire> items() {
        return items;
    }

    public static List<PlaceDesire> places() {
        return places;
    }

    public static PlaceDesire place(String id) {
        for (PlaceDesire p : places) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    public static List<ActivityDesire> activities() {
        return activities;
    }

    /** Which category the next desire comes from: 0 item, 1 place, 2 activity, -1 none. */
    public static int rollCategory(RandomSource random) {
        int iw = items.isEmpty() ? 0 : itemWeight;
        int pw = places.isEmpty() ? 0 : placeWeight;
        int aw = activities.isEmpty() ? 0 : activityWeight;
        int total = iw + pw + aw;
        if (total <= 0) {
            return -1;
        }
        int roll = random.nextInt(total);
        if (roll < iw) {
            return 0;
        }
        return roll < iw + pw ? 1 : 2;
    }

    static <T> T weighted(List<T> list, java.util.function.ToIntFunction<T> weight, RandomSource random) {
        int total = 0;
        for (T t : list) {
            total += weight.applyAsInt(t);
        }
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (T t : list) {
            roll -= weight.applyAsInt(t);
            if (roll < 0) {
                return t;
            }
        }
        return null;
    }

    private static Item item(String id) {
        Identifier rl = Identifier.tryParse(id);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) {
            return Items.GRASS_BLOCK;
        }
        return BuiltInRegistries.ITEM.getValue(rl);
    }

    private static JsonArray array(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray();
    }

    private static String str(JsonObject o, String key, String def) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : def;
    }

    private static int integer(JsonObject o, String key, int def) {
        try {
            return o.has(key) ? o.get(key).getAsInt() : def;
        } catch (RuntimeException e) {
            return def;
        }
    }

    private static boolean bool(JsonObject o, String key, boolean def) {
        try {
            return o.has(key) ? o.get(key).getAsBoolean() : def;
        } catch (RuntimeException e) {
            return def;
        }
    }

    private static final String DEFAULTS = """
            // My Girlfriend Laura - what she can desire.
            // categoryWeights: how often each kind of desire is picked.
            // items: "eat": true means you must feed it to her, otherwise giving it is enough.
            // places: "biomes" accepts biome ids, #biome_tags and dimension:<id> (for example dimension:minecraft:the_nether).
            // activities: ids are fixed (dance, hug, kiss, compliment, sleep_together, sunset, stargaze, music,
            //   boat_ride, swim, pet, new_outfit, fireworks, campfire, flowers, walk). Set a weight to 0 to disable one.
            {
              "categoryWeights": { "items": 45, "places": 20, "activities": 35 },
              "items": [
                { "item": "minecraft:cake", "eat": true, "weight": 12 },
                { "item": "minecraft:cookie", "eat": true, "weight": 10 },
                { "item": "minecraft:pumpkin_pie", "eat": true, "weight": 8 },
                { "item": "minecraft:sweet_berries", "eat": true, "weight": 8 },
                { "item": "minecraft:glow_berries", "eat": true, "weight": 5 },
                { "item": "minecraft:honey_bottle", "eat": true, "weight": 6 },
                { "item": "minecraft:cooked_salmon", "eat": true, "weight": 6 },
                { "item": "minecraft:mushroom_stew", "eat": true, "weight": 4 },
                { "item": "minecraft:golden_apple", "eat": true, "weight": 2 },
                { "item": "minecraft:poppy", "eat": false, "weight": 8 },
                { "item": "minecraft:pink_tulip", "eat": false, "weight": 6 },
                { "item": "minecraft:cornflower", "eat": false, "weight": 5 },
                { "item": "minecraft:allium", "eat": false, "weight": 5 },
                { "item": "minecraft:lily_of_the_valley", "eat": false, "weight": 4 },
                { "item": "minecraft:sunflower", "eat": false, "weight": 4 },
                { "item": "minecraft:diamond", "eat": false, "weight": 4 },
                { "item": "minecraft:emerald", "eat": false, "weight": 4 },
                { "item": "minecraft:amethyst_shard", "eat": false, "weight": 4 },
                { "item": "minecraft:pink_dye", "eat": false, "weight": 3 },
                { "item": "minecraft:gold_ingot", "eat": false, "weight": 3 },
                { "item": "minecraft:name_tag", "eat": false, "weight": 1 }
              ],
              "places": [
                { "id": "beach", "biomes": ["#minecraft:is_beach"], "icon": "minecraft:sand", "weight": 12 },
                { "id": "ocean", "biomes": ["#minecraft:is_ocean"], "icon": "minecraft:tropical_fish_bucket", "weight": 6 },
                { "id": "forest", "biomes": ["#minecraft:is_forest"], "icon": "minecraft:oak_sapling", "weight": 8 },
                { "id": "flowers", "biomes": ["minecraft:flower_forest", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:cherry_grove"], "icon": "minecraft:pink_tulip", "weight": 10 },
                { "id": "mountains", "biomes": ["#minecraft:is_mountain"], "icon": "minecraft:stone", "weight": 6 },
                { "id": "snow", "biomes": ["minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:ice_spikes", "minecraft:grove", "minecraft:snowy_slopes", "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:snowy_beach"], "icon": "minecraft:snowball", "weight": 6 },
                { "id": "desert", "biomes": ["minecraft:desert", "#minecraft:is_badlands"], "icon": "minecraft:cactus", "weight": 4 },
                { "id": "jungle", "biomes": ["#minecraft:is_jungle"], "icon": "minecraft:cocoa_beans", "weight": 4 },
                { "id": "mushrooms", "biomes": ["minecraft:mushroom_fields"], "icon": "minecraft:red_mushroom", "weight": 2 },
                { "id": "swamp", "biomes": ["minecraft:swamp", "minecraft:mangrove_swamp"], "icon": "minecraft:lily_pad", "weight": 2 },
                { "id": "nether", "biomes": ["dimension:minecraft:the_nether"], "icon": "minecraft:netherrack", "weight": 2 },
                { "id": "end", "biomes": ["dimension:minecraft:the_end"], "icon": "minecraft:end_stone", "weight": 1 }
              ],
              "activities": [
                { "id": "dance", "weight": 10 },
                { "id": "hug", "weight": 12 },
                { "id": "kiss", "weight": 10 },
                { "id": "compliment", "weight": 12 },
                { "id": "sleep_together", "weight": 5 },
                { "id": "sunset", "weight": 6 },
                { "id": "stargaze", "weight": 5 },
                { "id": "music", "weight": 6 },
                { "id": "boat_ride", "weight": 5 },
                { "id": "swim", "weight": 5 },
                { "id": "pet", "weight": 4 },
                { "id": "new_outfit", "weight": 4 },
                { "id": "fireworks", "weight": 3 },
                { "id": "campfire", "weight": 4 },
                { "id": "flowers", "weight": 5 },
                { "id": "walk", "weight": 6 }
              ]
            }
            """;
}
