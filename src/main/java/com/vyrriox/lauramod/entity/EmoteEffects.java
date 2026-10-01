package com.vyrriox.lauramod.entity;

import com.vyrriox.lauramod.registry.LauraRegistries.Sound;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Sounds and particles played at precise moments of an emote (claps on each hand contact, notes of
 * a hummed tune...). Runs on the server, so every nearby player hears the same thing at the same time.
 *
 * @author vyrriox
 */
public final class EmoteEffects {
    /** Short public domain tunes, as note block notes (0 = F#3, 12 = F#4, 24 = F#5). */
    private static final int[][] TUNES = {
            {6, 6, 13, 13, 15, 15, 13},          // Twinkle Twinkle Little Star
            {6, 8, 10, 6, 6, 8, 10, 6},          // Frere Jacques
            {10, 8, 6, 8, 10, 10, 10},           // Mary Had a Little Lamb
            {6, 10, 13, 18, 13, 10, 6, 13},      // a happy arpeggio
    };
    private static final int[] GUITAR = {3, 3, 8, 3, 10, 8, 3, 1, 3};

    private EmoteEffects() {
    }

    public static void tick(LauraEntity laura, Emote emote, int t) {
        if (!(laura.level() instanceof ServerLevel level) || t < 0) {
            return;
        }
        switch (emote) {
            case CLAP -> {
                if (t == 6 || t == 13 || t == 20 || t == 27 || t == 34) {
                    play(laura, Sound.CLAP, 0.9F, 0.95F + laura.getRandom().nextFloat() * 0.15F);
                    handParticles(level, laura, ParticleTypes.CRIT, 2);
                }
            }
            case CELEBRATE -> {
                if (t == 8 || t == 15 || t == 22 || t == 29) {
                    play(laura, Sound.CLAP, 1.0F, 1.05F);
                }
                if (t == 36) {
                    play(laura, Sound.SPARKLE, 0.8F, 1.0F);
                    particles(level, laura, ParticleTypes.FIREWORK, 25, 0.6, 2.0);
                }
                if (t == 40) {
                    laura.getJumpControl().jump();
                }
            }
            case SNAP -> {
                if (t == 8 || t == 22) {
                    play(laura, Sound.SNAP, 0.9F, 0.95F + laura.getRandom().nextFloat() * 0.1F);
                    handParticles(level, laura, ParticleTypes.WAX_OFF, 1);
                }
            }
            case TWIRL -> {
                if (t == 3) {
                    play(laura, Sound.WHOOSH, 0.7F, 1.2F);
                }
                if (t % 4 == 0) {
                    particles(level, laura, ParticleTypes.END_ROD, 2, 0.5, 1.0);
                }
                if (t == 28) {
                    play(laura, Sound.SPARKLE, 0.6F, 1.2F);
                }
            }
            case HUM -> {
                int[] tune = TUNES[Math.floorMod(laura.getEmoteStart(), TUNES.length)];
                if (t % 10 == 2 && t / 10 < tune.length) {
                    int note = tune[t / 10];
                    play(laura, SoundEvents.NOTE_BLOCK_FLUTE, 0.7F, notePitch(note));
                    note(level, laura, note);
                }
            }
            case AIR_GUITAR -> {
                if (t % 7 == 6 && t / 7 < GUITAR.length) {
                    int note = GUITAR[t / 7] + (laura.getRandom().nextInt(4) == 0 ? 5 : 0);
                    play(laura, SoundEvents.NOTE_BLOCK_GUITAR, 0.8F, notePitch(note));
                    note(level, laura, note);
                }
            }
            case DANCE -> {
                if (t % 10 == 0) {
                    note(level, laura, laura.getRandom().nextInt(24));
                }
            }
            case STRETCH -> {
                if (t == 12) {
                    play(laura, Sound.YAWN, 0.6F, 1.1F);
                }
            }
            case YAWN -> {
                if (t == 8) {
                    play(laura, Sound.YAWN, 0.8F, 1.0F);
                }
            }
            case TAP_FOOT -> {
                if (t % 10 == 6) {
                    play(laura, Sound.TAP, 0.8F, 0.9F + laura.getRandom().nextFloat() * 0.1F);
                }
            }
            case STOMP -> {
                if (t == 10 || t == 26) {
                    play(laura, Sound.TAP, 1.0F, 0.55F);
                    particles(level, laura, ParticleTypes.POOF, 4, 0.3, 0.0);
                }
            }
            case BLOW_KISS -> {
                if (t == 12) {
                    play(laura, Sound.KISS, 0.9F, 1.1F);
                }
                if (t == 15) {
                    heartTowardsOwner(level, laura);
                }
            }
            case KISS -> {
                if (t == 20) {
                    play(laura, Sound.KISS, 1.0F, 1.0F);
                    particles(level, laura, ParticleTypes.HEART, 3, 0.3, 1.8);
                }
            }
            case HUG -> {
                if (t == 12) {
                    play(laura, Sound.HAPPY, 0.8F, 1.1F);
                    particles(level, laura, ParticleTypes.HEART, 4, 0.4, 1.6);
                }
            }
            case SNEEZE -> {
                if (t == 18) {
                    play(laura, SoundEvents.PANDA_SNEEZE, 0.8F, 1.7F);
                    Vec3 look = laura.getLookAngle();
                    level.sendParticles(ParticleTypes.POOF, laura.getX() + look.x * 0.5, laura.getEyeY() - 0.1, laura.getZ() + look.z * 0.5, 5, 0.05, 0.05, 0.05, 0.02);
                }
            }
            case HAIR_FLIP -> {
                if (t == 8) {
                    play(laura, Sound.WHOOSH, 0.5F, 1.6F);
                }
                if (t == 10) {
                    particles(level, laura, ParticleTypes.WAX_ON, 3, 0.3, 1.7);
                }
            }
            case CHECK_NAILS -> {
                if (t == 44) {
                    play(laura, Sound.SIGH, 0.5F, 1.1F);
                }
            }
            case HICCUP -> {
                if (t == 5 || t == 16 && laura.getRandom().nextBoolean()) {
                    play(laura, Sound.HICCUP, 0.8F, 1.0F + laura.getRandom().nextFloat() * 0.2F);
                    particles(level, laura, ParticleTypes.BUBBLE_POP, 2, 0.1, 1.7);
                }
            }
            case POUT -> {
                if (t == 6) {
                    play(laura, Sound.HMPH, 0.8F, 1.0F);
                    particles(level, laura, ParticleTypes.ANGRY_VILLAGER, 1, 0.2, 2.0);
                }
            }
            case SHIVER -> {
                if (t == 2 || t == 26) {
                    play(laura, Sound.CHATTER, 0.7F, 1.0F);
                }
                if (t % 6 == 0) {
                    particles(level, laura, ParticleTypes.SNOWFLAKE, 2, 0.4, 1.2);
                }
            }
            case FAN -> {
                if (t == 6 || t == 22 || t == 38) {
                    play(laura, Sound.WHOOSH, 0.3F, 1.9F);
                }
                if (t == 30) {
                    play(laura, Sound.SIGH, 0.4F, 1.3F);
                }
            }
            case LAUGH -> {
                if (t == 2) {
                    play(laura, Sound.LAUGH, 1.0F, 1.0F + laura.getRandom().nextFloat() * 0.1F);
                }
            }
            case JUMP -> {
                if (t == 2) {
                    play(laura, Sound.HAPPY, 0.7F, 1.3F);
                    laura.getJumpControl().jump();
                }
            }
            case CRY -> {
                if (t == 4) {
                    play(laura, Sound.SAD, 0.9F, 1.0F);
                }
                if (t % 6 == 0) {
                    level.sendParticles(ParticleTypes.FALLING_WATER, laura.getX(), laura.getEyeY() - 0.05, laura.getZ(), 2, 0.15, 0.02, 0.15, 0.0);
                }
            }
            case BLUSH -> {
                if (t == 5) {
                    particles(level, laura, ParticleTypes.HEART, 2, 0.3, 1.9);
                }
            }
            case FACEPALM -> {
                if (t == 10) {
                    play(laura, Sound.CLAP, 0.6F, 0.7F);
                }
                if (t == 20) {
                    play(laura, Sound.SIGH, 0.6F, 0.9F);
                }
            }
            case THINK -> {
                if (t == 10) {
                    play(laura, Sound.HMPH, 0.35F, 1.4F);
                }
            }
            case WAVE -> {
                if (t == 4) {
                    play(laura, Sound.HAPPY, 0.5F, 1.25F);
                }
            }
            default -> {
            }
        }
    }

    public static float notePitch(int note) {
        return (float) Math.pow(2.0, (note - 12) / 12.0);
    }

    private static void play(LauraEntity laura, Sound sound, float volume, float pitch) {
        laura.playLauraSound(sound, volume, pitch);
    }

    private static void play(LauraEntity laura, SoundEvent sound, float volume, float pitch) {
        laura.playSound(sound, volume, pitch);
    }

    private static void play(LauraEntity laura, Holder<SoundEvent> sound, float volume, float pitch) {
        laura.playSound(sound.value(), volume, pitch);
    }

    private static void particles(ServerLevel level, LauraEntity laura, ParticleOptions particle, int count, double spread, double height) {
        level.sendParticles(particle, laura.getX(), laura.getY() + height, laura.getZ(), count, spread, spread * 0.5, spread, 0.01);
    }

    private static void handParticles(ServerLevel level, LauraEntity laura, ParticleOptions particle, int count) {
        Vec3 look = Vec3.directionFromRotation(0, laura.yBodyRot);
        level.sendParticles(particle, laura.getX() + look.x * 0.4, laura.getY() + 1.25, laura.getZ() + look.z * 0.4, count, 0.05, 0.05, 0.05, 0.02);
    }

    private static void note(ServerLevel level, LauraEntity laura, int note) {
        // With a count of 0 the x offset is the note color, like a note block.
        level.sendParticles(ParticleTypes.NOTE, laura.getX(), laura.getY() + 2.2, laura.getZ(), 0, note / 24.0, 0.0, 0.0, 1.0);
    }

    private static void heartTowardsOwner(ServerLevel level, LauraEntity laura) {
        LivingEntity owner = laura.getOwner();
        Vec3 from = laura.getEyePosition();
        Vec3 dir = owner != null ? owner.getEyePosition().subtract(from).normalize() : laura.getLookAngle();
        for (int i = 1; i <= 4; i++) {
            Vec3 p = from.add(dir.scale(i * 0.6));
            level.sendParticles(ParticleTypes.HEART, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
