package com.vyrriox.lauramod.gift;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.util.ItemSpec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gifts, food preferences and the gifts Laura gives back, from {@code config/lauramod/gifts.json}.
 *
 * @author vyrriox
 */
public final class GiftTable {
    /** How much she likes a gift. Used to pick her answer. */
    public enum Tier {
        GROSS, MEH, NICE, GREAT, AMAZING;

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public record Gift(ItemSpec match, int affection, int fun, Tier tier, double returnChance, int food) {
    }

    public record ReturnGift(Item item, int count, int weight) {
    }

    /** What happens when she eats an item. */
    public record FoodInfo(int nutrition, float saturation, Preference preference) {
    }

    public enum Preference {
        FAVORITE, NORMAL, DISLIKED
    }

    private static volatile List<Gift> gifts = Collections.emptyList();
    private static volatile List<ItemSpec> favorites = Collections.emptyList();
    private static volatile List<ItemSpec> disliked = Collections.emptyList();
    private static volatile List<ReturnGift> returnGifts = Collections.emptyList();

    private GiftTable() {
    }

    public static synchronized void load() {
        Path file = LauraMod.configDir().resolve("gifts.json");
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
        com.vyrriox.lauramod.api.ScriptData.extend("gifts", root);
        List<Gift> parsedGifts = new ArrayList<>();
        for (JsonElement e : array(root, "gifts")) {
            if (!e.isJsonObject()) {
                continue;
            }
            JsonObject o = e.getAsJsonObject();
            ItemSpec.parse(str(o, "match", "")).ifPresentOrElse(spec -> parsedGifts.add(new Gift(spec,
                    integer(o, "affection", 5), integer(o, "fun", 5), tier(str(o, "tier", "nice")),
                    decimal(o, "returnChance", 0.0), integer(o, "food", 0))),
                    () -> LauraMod.LOGGER.warn("gifts.json: unknown item {}", o.get("match")));
        }
        JsonObject foods = root.has("foods") && root.get("foods").isJsonObject() ? root.getAsJsonObject("foods") : new JsonObject();
        List<ItemSpec> fav = specs(array(foods, "favorites"));
        List<ItemSpec> dis = specs(array(foods, "disliked"));
        List<ReturnGift> back = new ArrayList<>();
        for (JsonElement e : array(root, "returnGifts")) {
            if (!e.isJsonObject()) {
                continue;
            }
            JsonObject o = e.getAsJsonObject();
            ResourceLocation id = ResourceLocation.tryParse(str(o, "item", ""));
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                back.add(new ReturnGift(BuiltInRegistries.ITEM.get(id), Math.max(1, integer(o, "count", 1)), Math.max(1, integer(o, "weight", 1))));
            }
        }
        gifts = List.copyOf(parsedGifts);
        favorites = List.copyOf(fav);
        disliked = List.copyOf(dis);
        returnGifts = List.copyOf(back);
    }

    /** The gift entry for an item, or null if she does not care about it. */
    public static Gift find(ItemStack stack) {
        for (Gift gift : gifts) {
            if (gift.match().test(stack)) {
                return gift;
            }
        }
        return null;
    }

    /** Food value of an item for Laura, or null if she cannot eat it. */
    public static FoodInfo food(ItemStack stack) {
        Preference preference = Preference.NORMAL;
        for (ItemSpec spec : favorites) {
            if (spec.test(stack)) {
                preference = Preference.FAVORITE;
                break;
            }
        }
        if (preference == Preference.NORMAL) {
            for (ItemSpec spec : disliked) {
                if (spec.test(stack)) {
                    preference = Preference.DISLIKED;
                    break;
                }
            }
        }
        FoodProperties props = stack.get(DataComponents.FOOD);
        if (props != null) {
            return new FoodInfo(props.nutrition(), props.saturation(), preference);
        }
        Gift gift = find(stack);
        if (gift != null && gift.food() > 0) {
            return new FoodInfo(gift.food(), gift.food() * 0.4F, preference);
        }
        return null;
    }

    public static boolean isFavoriteFood(ItemStack stack) {
        FoodInfo info = food(stack);
        return info != null && info.preference() == Preference.FAVORITE;
    }

    public static List<ItemSpec> favorites() {
        return favorites;
    }

    /** Picks a gift she gives back, or empty. */
    public static ItemStack rollReturnGift(RandomSource random) {
        List<ReturnGift> list = returnGifts;
        int total = 0;
        for (ReturnGift g : list) {
            total += g.weight();
        }
        if (total <= 0) {
            return ItemStack.EMPTY;
        }
        int roll = random.nextInt(total);
        for (ReturnGift g : list) {
            roll -= g.weight();
            if (roll < 0) {
                return new ItemStack(g.item(), g.count());
            }
        }
        return ItemStack.EMPTY;
    }

    private static List<ItemSpec> specs(JsonArray array) {
        List<ItemSpec> out = new ArrayList<>();
        for (JsonElement e : array) {
            if (e.isJsonPrimitive()) {
                ItemSpec.parse(e.getAsString()).ifPresent(out::add);
            }
        }
        return out;
    }

    private static Tier tier(String s) {
        for (Tier t : Tier.values()) {
            if (t.name().equalsIgnoreCase(s)) {
                return t;
            }
        }
        return Tier.NICE;
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

    private static double decimal(JsonObject o, String key, double def) {
        try {
            return o.has(key) ? o.get(key).getAsDouble() : def;
        } catch (RuntimeException e) {
            return def;
        }
    }

    private static final String DEFAULTS = """
            // My Girlfriend Laura - gifts and food.
            // "match" is an item id or an #item_tag. The first matching entry is used.
            // affection: -1000 to 1000. fun: 0 to 100. tier: GROSS, MEH, NICE, GREAT or AMAZING (picks her reaction).
            // returnChance: chance (0 to 1) that she gives you something back (from returnGifts).
            // food: nutrition if she can eat an item that is not normally edible (the cake for example).
            {
              "gifts": [
                { "match": "minecraft:cake", "affection": 35, "fun": 30, "tier": "AMAZING", "returnChance": 0.3, "food": 14 },
                { "match": "minecraft:diamond", "affection": 40, "fun": 20, "tier": "AMAZING", "returnChance": 0.25 },
                { "match": "minecraft:emerald", "affection": 25, "fun": 15, "tier": "GREAT", "returnChance": 0.2 },
                { "match": "minecraft:amethyst_shard", "affection": 15, "fun": 15, "tier": "GREAT", "returnChance": 0.15 },
                { "match": "minecraft:golden_apple", "affection": 30, "fun": 10, "tier": "GREAT", "returnChance": 0.2 },
                { "match": "minecraft:enchanted_golden_apple", "affection": 80, "fun": 30, "tier": "AMAZING", "returnChance": 0.5 },
                { "match": "minecraft:gold_ingot", "affection": 12, "fun": 5, "tier": "NICE", "returnChance": 0.1 },
                { "match": "minecraft:pink_dye", "affection": 10, "fun": 10, "tier": "NICE", "returnChance": 0.1 },
                { "match": "minecraft:pink_tulip", "affection": 15, "fun": 12, "tier": "GREAT", "returnChance": 0.15 },
                { "match": "minecraft:poppy", "affection": 12, "fun": 10, "tier": "NICE", "returnChance": 0.2 },
                { "match": "#minecraft:small_flowers", "affection": 10, "fun": 10, "tier": "NICE", "returnChance": 0.15 },
                { "match": "#minecraft:tall_flowers", "affection": 12, "fun": 10, "tier": "NICE", "returnChance": 0.15 },
                { "match": "minecraft:cookie", "affection": 8, "fun": 8, "tier": "NICE", "returnChance": 0.05 },
                { "match": "minecraft:pumpkin_pie", "affection": 12, "fun": 10, "tier": "GREAT", "returnChance": 0.1 },
                { "match": "minecraft:sweet_berries", "affection": 6, "fun": 5, "tier": "NICE", "returnChance": 0.05 },
                { "match": "minecraft:glow_berries", "affection": 8, "fun": 8, "tier": "NICE", "returnChance": 0.05 },
                { "match": "minecraft:honey_bottle", "affection": 10, "fun": 8, "tier": "NICE", "returnChance": 0.1 },
                { "match": "#minecraft:music_discs", "affection": 20, "fun": 25, "tier": "GREAT", "returnChance": 0.2 },
                { "match": "minecraft:name_tag", "affection": 5, "fun": 5, "tier": "MEH", "returnChance": 0.0 },
                { "match": "minecraft:totem_of_undying", "affection": 50, "fun": 20, "tier": "AMAZING", "returnChance": 0.4 },
                { "match": "minecraft:rotten_flesh", "affection": -25, "fun": 0, "tier": "GROSS", "returnChance": 0.0 },
                { "match": "minecraft:spider_eye", "affection": -20, "fun": 0, "tier": "GROSS", "returnChance": 0.0 },
                { "match": "minecraft:poisonous_potato", "affection": -15, "fun": 0, "tier": "GROSS", "returnChance": 0.0 },
                { "match": "minecraft:dirt", "affection": -5, "fun": 0, "tier": "MEH", "returnChance": 0.0 },
                { "match": "minecraft:stick", "affection": -2, "fun": 2, "tier": "MEH", "returnChance": 0.0 },
                { "match": "minecraft:bone", "affection": -10, "fun": 0, "tier": "GROSS", "returnChance": 0.0 }
              ],
              "foods": {
                "favorites": [
                  "minecraft:cake", "minecraft:cookie", "minecraft:pumpkin_pie", "minecraft:sweet_berries",
                  "minecraft:glow_berries", "minecraft:honey_bottle", "minecraft:golden_apple", "minecraft:cooked_salmon",
                  "minecraft:mushroom_stew", "minecraft:golden_carrot"
                ],
                "disliked": [
                  "minecraft:rotten_flesh", "minecraft:spider_eye", "minecraft:poisonous_potato", "minecraft:pufferfish",
                  "minecraft:beef", "minecraft:porkchop", "minecraft:chicken", "minecraft:mutton", "minecraft:rabbit",
                  "minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish", "minecraft:dried_kelp"
                ]
              },
              "returnGifts": [
                { "item": "minecraft:poppy", "count": 1, "weight": 20 },
                { "item": "minecraft:cornflower", "count": 1, "weight": 12 },
                { "item": "minecraft:cookie", "count": 3, "weight": 15 },
                { "item": "minecraft:apple", "count": 2, "weight": 12 },
                { "item": "minecraft:bread", "count": 2, "weight": 10 },
                { "item": "minecraft:torch", "count": 8, "weight": 8 },
                { "item": "minecraft:iron_ingot", "count": 2, "weight": 6 },
                { "item": "minecraft:gold_ingot", "count": 1, "weight": 4 },
                { "item": "minecraft:experience_bottle", "count": 2, "weight": 4 },
                { "item": "minecraft:emerald", "count": 1, "weight": 2 },
                { "item": "minecraft:diamond", "count": 1, "weight": 1 }
              ]
            }
            """;
}
