package com.vyrriox.lauramod.desire;

import com.vyrriox.lauramod.util.ItemSpec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Something Laura currently wants, with a deadline.
 *
 * @author vyrriox
 */
public final class Desire {
    private final DesireType.Kind kind;
    private final String target;
    private final boolean eat;
    private final long createdAt;
    private final long deadline;
    private int progress;
    private int reminders;
    private double walked;

    public Desire(DesireType.Kind kind, String target, boolean eat, long createdAt, long deadline) {
        this.kind = kind;
        this.target = target;
        this.eat = eat;
        this.createdAt = createdAt;
        this.deadline = deadline;
    }

    /** Rolls a new desire from the desire table. Null when the table is empty. */
    public static Desire roll(RandomSource random, long now, long durationTicks) {
        for (int attempt = 0; attempt < 4; attempt++) {
            int category = DesireTable.rollCategory(random);
            switch (category) {
                case 0 -> {
                    DesireTable.ItemDesire d = DesireTable.weighted(DesireTable.items(), DesireTable.ItemDesire::weight, random);
                    if (d != null) {
                        return new Desire(DesireType.Kind.ITEM, d.item().raw(), d.eat(), now, now + durationTicks);
                    }
                }
                case 1 -> {
                    DesireTable.PlaceDesire d = DesireTable.weighted(DesireTable.places(), DesireTable.PlaceDesire::weight, random);
                    if (d != null) {
                        return new Desire(DesireType.Kind.PLACE, d.id(), false, now, now + durationTicks);
                    }
                }
                case 2 -> {
                    DesireTable.ActivityDesire d = DesireTable.weighted(DesireTable.activities(), DesireTable.ActivityDesire::weight, random);
                    if (d != null) {
                        return new Desire(DesireType.Kind.ACTIVITY, d.activity().key(), false, now, now + durationTicks);
                    }
                }
                default -> {
                    return null;
                }
            }
        }
        return null;
    }

    public DesireType.Kind kind() {
        return kind;
    }

    public String target() {
        return target;
    }

    public boolean mustEat() {
        return eat;
    }

    public long createdAt() {
        return createdAt;
    }

    public long deadline() {
        return deadline;
    }

    public int progress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int reminders() {
        return reminders;
    }

    public void addReminder() {
        reminders++;
    }

    public double walked() {
        return walked;
    }

    public void addWalked(double distance) {
        walked += distance;
    }

    public DesireType.Activity activity() {
        return kind == DesireType.Kind.ACTIVITY ? DesireType.Activity.byName(target) : null;
    }

    public ItemSpec itemSpec() {
        return kind == DesireType.Kind.ITEM ? ItemSpec.parse(target).orElse(null) : null;
    }

    /** Fraction of the allowed time already spent (0 to 1+). */
    public double elapsed(long now) {
        long total = Math.max(1, deadline - createdAt);
        return (double) (now - createdAt) / total;
    }

    /** Short text synced to clients for the thought bubble. */
    public String syncString() {
        return kind.key() + ":" + target;
    }

    /** Translatable description, for chat lines ({item}, {place}, {activity}). */
    public Component describe() {
        return switch (kind) {
            case ITEM -> {
                ItemSpec spec = itemSpec();
                yield spec != null ? spec.displayName() : Component.literal(target);
            }
            case PLACE -> Component.translatable("lauramod.place." + target);
            case ACTIVITY -> Component.translatable("lauramod.activity." + target);
        };
    }

    public ItemStack icon() {
        return switch (kind) {
            case ITEM -> {
                ItemSpec spec = itemSpec();
                yield spec != null ? spec.icon() : new ItemStack(Items.BARRIER);
            }
            case PLACE -> {
                DesireTable.PlaceDesire place = DesireTable.place(target);
                yield new ItemStack(place != null ? place.icon() : Items.GRASS_BLOCK);
            }
            case ACTIVITY -> {
                DesireType.Activity a = activity();
                yield new ItemStack(a != null ? a.icon : Items.JUKEBOX);
            }
        };
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Kind", kind.name());
        tag.putString("Target", target);
        tag.putBoolean("Eat", eat);
        tag.putLong("Created", createdAt);
        tag.putLong("Deadline", deadline);
        tag.putInt("Progress", progress);
        tag.putInt("Reminders", reminders);
        tag.putDouble("Walked", walked);
        return tag;
    }

    public static Desire load(CompoundTag tag) {
        try {
            Desire d = new Desire(DesireType.Kind.valueOf(tag.getString("Kind")), tag.getString("Target"), tag.getBoolean("Eat"),
                    tag.getLong("Created"), tag.getLong("Deadline"));
            d.progress = tag.getInt("Progress");
            d.reminders = tag.getInt("Reminders");
            d.walked = tag.getDouble("Walked");
            return d;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
