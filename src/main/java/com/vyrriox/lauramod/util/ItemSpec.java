package com.vyrriox.lauramod.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.RecordItem;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * An item id ({@code minecraft:cake}), an item tag ({@code #minecraft:logs}) or a built-in group
 * ({@code @music_disc}) written in a config or dialogue file.
 *
 * @author vyrriox
 */
public final class ItemSpec implements Predicate<ItemStack> {
    private final String raw;
    private final Item item;
    private final TagKey<Item> tag;
    private final Group group;

    private ItemSpec(String raw, Item item, TagKey<Item> tag, Group group) {
        this.raw = raw;
        this.item = item;
        this.tag = tag;
        this.group = group;
    }

    /**
     * Item groups matched by what the items are, not by a tag: a tag can be missing from a
     * Minecraft version or a loader, a group means the same thing everywhere.
     */
    private enum Group {
        /** Any item a jukebox can play, modded discs included. */
        MUSIC_DISC {
            @Override
            boolean test(ItemStack stack) {
                return stack.getItem() instanceof RecordItem;
            }

            @Override
            Item icon() {
                return Items.MUSIC_DISC_CAT;
            }
        };

        abstract boolean test(ItemStack stack);

        abstract Item icon();

        static Group byName(String name) {
            for (Group group : values()) {
                if (group.name().equalsIgnoreCase(name)) {
                    return group;
                }
            }
            return null;
        }
    }

    /** Parses a spec. Returns empty for invalid ids, unknown items and unknown groups. */
    public static Optional<ItemSpec> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return Optional.empty();
        }
        if (s.startsWith("@")) {
            Group group = Group.byName(s.substring(1));
            return group == null ? Optional.empty() : Optional.of(new ItemSpec(s, null, null, group));
        }
        if (s.startsWith("#")) {
            ResourceLocation id = ResourceLocation.tryParse(s.substring(1));
            return id == null ? Optional.empty() : Optional.of(new ItemSpec(s, null, TagKey.create(Registries.ITEM, id), null));
        }
        ResourceLocation id = ResourceLocation.tryParse(s.contains(":") ? s : "minecraft:" + s);
        if (id == null) {
            return Optional.empty();
        }
        Optional<Item> found = BuiltInRegistries.ITEM.getOptional(id);
        if (found.isEmpty() || found.get() == Items.AIR) {
            return Optional.empty();
        }
        return Optional.of(new ItemSpec(s, found.get(), null, null));
    }

    public static ItemSpec of(Item item) {
        return new ItemSpec(BuiltInRegistries.ITEM.getKey(item).toString(), item, null, null);
    }

    public boolean isTag() {
        return tag != null;
    }

    public String raw() {
        return raw;
    }

    /** The single item, or null for tags and groups. */
    public Item item() {
        return item;
    }

    @Override
    public boolean test(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (group != null) {
            return group.test(stack);
        }
        return tag != null ? stack.is(tag) : stack.is(item);
    }

    /** An item representing this spec, used for icons and names. */
    public ItemStack icon() {
        if (item != null) {
            return new ItemStack(item);
        }
        if (group != null) {
            return new ItemStack(group.icon());
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
