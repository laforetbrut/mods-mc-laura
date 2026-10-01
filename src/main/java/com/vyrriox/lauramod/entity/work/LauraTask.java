package com.vyrriox.lauramod.entity.work;

import com.vyrriox.lauramod.entity.LauraJob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * One queued errand. Tasks run one after the other; continuous jobs run when the queue is empty.
 *
 * @author vyrriox
 */
public final class LauraTask {
    public enum Type {
        /** Bring an item (arg = item spec, count). */
        FETCH,
        /** Fell one tree and bring the wood back. */
        CHOP_TREE,
        /** Harvest every ripe crop around once and bring it back. */
        HARVEST,
        /** Cook everything she can once and bring the meals back. */
        COOK,
        /** Come next to her partner. */
        COME,
        /** Go home. */
        GO_HOME,
        /** Start following. */
        FOLLOW,
        /** Stay here. */
        STAY,
        /** Go back to her jobs. */
        WORK;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Type byName(String name) {
            for (Type t : values()) {
                if (t.name().equalsIgnoreCase(name)) {
                    return t;
                }
            }
            return null;
        }

        /** Tasks that take time; the others are instant orders. */
        public boolean isErrand() {
            return this == FETCH || this == CHOP_TREE || this == HARVEST || this == COOK;
        }

        public LauraJob job() {
            return switch (this) {
                case CHOP_TREE -> LauraJob.LUMBERJACK;
                case HARVEST -> LauraJob.FARMER;
                case COOK -> LauraJob.COOK;
                default -> LauraJob.NONE;
            };
        }
    }

    private final Type type;
    private final String arg;
    private final int count;
    private final WorkArea area;

    public LauraTask(Type type, String arg, int count, WorkArea area) {
        this.type = type;
        this.arg = arg == null ? "" : arg;
        this.count = count;
        this.area = area;
    }

    public Type type() {
        return type;
    }

    public String arg() {
        return arg;
    }

    public int count() {
        return count;
    }

    public WorkArea area() {
        return area;
    }

    public Component describe() {
        Component base = Component.translatable("lauramod.task." + type.key());
        if (type == Type.FETCH && !arg.isEmpty()) {
            return base.copy().append(" " + count + "x " + arg);
        }
        return base;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", type.name());
        tag.putString("Arg", arg);
        tag.putInt("Count", count);
        if (area != null) {
            tag.put("Area", area.save());
        }
        return tag;
    }

    public static LauraTask load(CompoundTag tag) {
        Type type = Type.byName(tag.getStringOr("Type", ""));
        if (type == null) {
            return null;
        }
        return new LauraTask(type, tag.getStringOr("Arg", ""), tag.getIntOr("Count", 0), tag.contains("Area") ? WorkArea.load(tag.getCompoundOrEmpty("Area")) : null);
    }
}
