package com.vyrriox.lauramod.cooking;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.util.ItemSpec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Laura's cookbook: the meals she can prepare at a crafting table, from
 * {@code config/lauramod/recipes.json}. Cooking raw food in furnaces, smokers and campfires uses the
 * game's own recipes and needs no entry here.
 *
 * @author vyrriox
 */
public final class Cookbook {
    public record Meal(String id, Item result, int count, Map<ItemSpec, Integer> ingredients, Map<Item, Integer> returns) {
        public ItemStack resultStack() {
            return new ItemStack(result, count);
        }
    }

    private static volatile List<Meal> meals = Collections.emptyList();
    private static volatile boolean requireCraftingTable = true;
    private static volatile boolean useCampfires = true;

    private Cookbook() {
    }

    public static synchronized void load() {
        Path file = LauraMod.configDir().resolve("recipes.json");
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
        com.vyrriox.lauramod.api.ScriptData.extend("recipes", root);
        requireCraftingTable = flag(root, "requireCraftingTable", true);
        useCampfires = flag(root, "useCampfires", true);
        List<Meal> parsed = new ArrayList<>();
        JsonArray array = root.has("meals") && root.get("meals").isJsonArray() ? root.getAsJsonArray("meals") : new JsonArray();
        for (JsonElement e : array) {
            if (!e.isJsonObject()) {
                continue;
            }
            // A wrong value (text where a number is expected, a list, null...) only costs its own meal.
            try {
                Meal meal = meal(e.getAsJsonObject());
                if (meal != null) {
                    parsed.add(meal);
                }
            } catch (RuntimeException ex) {
                LauraMod.LOGGER.warn("recipes.json: meal {} is invalid and ignored ({})", e, ex.toString());
            }
        }
        meals = List.copyOf(parsed);
    }

    /** One meal of the file, or null when its result or an ingredient is unknown. Throws on a value of the wrong kind. */
    private static Meal meal(JsonObject o) {
        Item result = item(o.has("result") ? o.get("result").getAsString() : "");
        if (result == null) {
            LauraMod.LOGGER.warn("recipes.json: unknown result {}", o.get("result"));
            return null;
        }
        Map<ItemSpec, Integer> ingredients = new LinkedHashMap<>();
        JsonObject ing = o.has("ingredients") && o.get("ingredients").isJsonObject() ? o.getAsJsonObject("ingredients") : new JsonObject();
        for (Map.Entry<String, JsonElement> in : ing.entrySet()) {
            var spec = ItemSpec.parse(in.getKey());
            if (spec.isEmpty()) {
                LauraMod.LOGGER.warn("recipes.json: unknown ingredient {} in {}", in.getKey(), o.get("result"));
                return null;
            }
            ingredients.put(spec.get(), Math.max(1, in.getValue().getAsInt()));
        }
        Map<Item, Integer> returns = new LinkedHashMap<>();
        JsonObject ret = o.has("returns") && o.get("returns").isJsonObject() ? o.getAsJsonObject("returns") : new JsonObject();
        for (Map.Entry<String, JsonElement> r : ret.entrySet()) {
            Item item = item(r.getKey());
            if (item != null) {
                returns.put(item, Math.max(1, r.getValue().getAsInt()));
            }
        }
        if (ingredients.isEmpty()) {
            return null;
        }
        String id = o.has("id") ? o.get("id").getAsString() : BuiltInRegistries.ITEM.getKey(result).getPath();
        return new Meal(id, result, o.has("count") ? Math.max(1, o.get("count").getAsInt()) : 1, ingredients, returns);
    }

    private static boolean flag(JsonObject o, String key, boolean def) {
        try {
            return o.has(key) ? o.get(key).getAsBoolean() : def;
        } catch (RuntimeException e) {
            LauraMod.LOGGER.warn("recipes.json: {} must be true or false, using {}", key, def);
            return def;
        }
    }

    public static List<Meal> meals() {
        return meals;
    }

    public static boolean requireCraftingTable() {
        return requireCraftingTable;
    }

    public static boolean useCampfires() {
        return useCampfires;
    }

    /** True if the stack is an ingredient of any meal. */
    public static boolean isIngredient(ItemStack stack) {
        for (Meal meal : meals) {
            for (ItemSpec spec : meal.ingredients().keySet()) {
                if (spec.test(stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Item item(String id) {
        Identifier rl = Identifier.tryParse(id);
        return rl != null && BuiltInRegistries.ITEM.containsKey(rl) ? BuiltInRegistries.ITEM.getValue(rl) : null;
    }

    private static final String DEFAULTS = """
            // My Girlfriend Laura - cookbook.
            // Meals she prepares at a crafting table (set requireCraftingTable to false to let her cook anywhere).
            // Raw meat, fish and potatoes are cooked in furnaces, smokers and campfires with the game's own
            // recipes: they do not need an entry here.
            // ingredients: item id or #tag -> amount. returns: items given back (the buckets of a cake...).
            // Meals are tried in this order.
            {
              "requireCraftingTable": true,
              "useCampfires": true,
              "meals": [
                { "id": "cake", "result": "minecraft:cake", "count": 1,
                  "ingredients": { "minecraft:milk_bucket": 3, "minecraft:sugar": 2, "minecraft:egg": 1, "minecraft:wheat": 3 },
                  "returns": { "minecraft:bucket": 3 } },
                { "id": "pumpkin_pie", "result": "minecraft:pumpkin_pie", "count": 1,
                  "ingredients": { "minecraft:pumpkin": 1, "minecraft:sugar": 1, "minecraft:egg": 1 } },
                { "id": "cookie", "result": "minecraft:cookie", "count": 8,
                  "ingredients": { "minecraft:wheat": 2, "minecraft:cocoa_beans": 1 } },
                { "id": "rabbit_stew", "result": "minecraft:rabbit_stew", "count": 1,
                  "ingredients": { "minecraft:bowl": 1, "minecraft:cooked_rabbit": 1, "minecraft:carrot": 1, "minecraft:baked_potato": 1, "minecraft:brown_mushroom": 1 } },
                { "id": "mushroom_stew", "result": "minecraft:mushroom_stew", "count": 1,
                  "ingredients": { "minecraft:bowl": 1, "minecraft:red_mushroom": 1, "minecraft:brown_mushroom": 1 } },
                { "id": "beetroot_soup", "result": "minecraft:beetroot_soup", "count": 1,
                  "ingredients": { "minecraft:bowl": 1, "minecraft:beetroot": 6 } },
                { "id": "golden_carrot", "result": "minecraft:golden_carrot", "count": 1,
                  "ingredients": { "minecraft:carrot": 1, "minecraft:gold_nugget": 8 } },
                { "id": "bread", "result": "minecraft:bread", "count": 1,
                  "ingredients": { "minecraft:wheat": 3 } },
                { "id": "sugar", "result": "minecraft:sugar", "count": 1,
                  "ingredients": { "minecraft:sugar_cane": 1 } },
                { "id": "sugar_from_honey", "result": "minecraft:sugar", "count": 3,
                  "ingredients": { "minecraft:honey_bottle": 1 },
                  "returns": { "minecraft:glass_bottle": 1 } }
              ]
            }
            """;
}
