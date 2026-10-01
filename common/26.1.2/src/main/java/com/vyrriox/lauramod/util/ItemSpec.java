package com.vyrriox.lauramod.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * An item id ({@code minecraft:cake}) or an item tag ({@code #minecraft:logs}) written in a config
 * or dialogue file.
 *
 * @author vyrriox
 */
public final class ItemSpec implements Predicate<ItemStack> {
    private final String raw;
    private final Item item;
    private final TagKey<Item> tag;

    private ItemSpec(String raw, Item item, TagKey<Item> tag) {
        this.raw = raw;
        this.item = item;
        this.tag = tag;
    }

    /** Parses a spec. Returns empty for invalid ids or unknown items. */
    public static Optional<ItemSpec> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return Optional.empty();
        }
        if (s.startsWith("#")) {
            Identifier id = Identifier.tryParse(s.substring(1));
            return id == null ? Optional.empty() : Optional.of(new ItemSpec(s, null, TagKey.create(Registries.ITEM, id)));
        }
        Identifier id = Identifier.tryParse(s.contains(":") ? s : "minecraft:" + s);
        if (id == null) {
            return Optional.empty();
        }
        Optional<Item> found = BuiltInRegistries.ITEM.getOptional(id);
        if (found.isEmpty() || found.get() == Items.AIR) {
            return Optional.empty();
        }
        return Optional.of(new ItemSpec(s, found.get(), null));
    }

    public static ItemSpec of(Item item) {
        return new ItemSpec(BuiltInRegistries.ITEM.getKey(item).toString(), item, null);
    }

    public boolean isTag() {
        return tag != null;
    }

    public String raw() {
        return raw;
    }

    /** The single item, or null for tags. */
    public Item item() {
        return item;
    }

    @Override
    public boolean test(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return tag != null ? stack.is(tag) : stack.is(item);
    }

    /** An item representing this spec, used for icons and names. */
    public ItemStack icon() {
        if (item != null) {
            return new ItemStack(item);
        }
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            return new ItemStack(holder.value());
        }
        return ItemStack.EMPTY;
    }

    /** A translatable display name (each client shows it in its own language). */
    public Component displayName() {
        if (item != null) {
            return Component.translatable(item.getDescriptionId());
        }
        ItemStack icon = icon();
        if (!icon.isEmpty()) {
            return Component.translatable(icon.getItem().getDescriptionId());
        }
        return Component.literal(raw);
    }

    @Override
    public String toString() {
        return raw;
    }
}
