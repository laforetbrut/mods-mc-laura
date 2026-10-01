package com.vyrriox.lauramod.test;

import com.vyrriox.lauramod.LauraMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Runs the tests one after the other on the server thread. Each test gets a fresh stone platform
 * and a fake player that owns the Laura it summons.
 *
 * @author vyrriox
 */
public final class TestRunner {
    public record Result(String name, boolean passed, String message, int ticks) {
    }

    /** A test: sets things up in {@link #run} and schedules checks through the context. */
    public interface TestCase {
        String name();

        void run(Context ctx) throws Exception;

        default int timeoutTicks() {
            return 20 * 30;
        }
    }

    private static final int PLATFORM_Y = 250;
    private static final int PLATFORM_RADIUS = 20;

    private final MinecraftServer server;
    private final List<TestCase> tests;
    private final List<Result> results = new ArrayList<>();
    private int index = -1;
    private Context current;
    private int warmup = 40;

    public TestRunner(MinecraftServer server, List<TestCase> tests) {
        this.server = server;
        this.tests = tests;
    }

    public List<Result> results() {
        return results;
    }

    /** Returns true once every test finished. */
    public boolean tick() {
        if (warmup > 0) {
            warmup--;
            return false;
        }
        if (current == null) {
            index++;
            if (index >= tests.size()) {
                return true;
            }
            TestCase test = tests.get(index);
            current = new Context(this, test, server.overworld(), new BlockPos(0, PLATFORM_Y + 1, index * 64));
            try {
                current.prepare();
                test.run(current);
            } catch (Throwable t) {
                current.fail("exception in setup: " + t);
            }
            return false;
        }
        current.tick();
        return false;
    }

    void finish(Context ctx, boolean passed, String message) {
        if (ctx != current || ctx.done) {
            return;
        }
        ctx.done = true;
        results.add(new Result(ctx.test.name(), passed, message, ctx.age));
        LauraMod.LOGGER.info("[SELFTEST] {} {} ({} ticks){}", passed ? "PASS" : "FAIL", ctx.test.name(), ctx.age, message.isEmpty() ? "" : ": " + message);
        ctx.cleanup();
        current = null;
    }

    /** Everything a test can use. */
    public static final class Context {
        private final TestRunner runner;
        final TestCase test;
        public final ServerLevel level;
        public final BlockPos origin;
        private final List<Step> steps = new ArrayList<>();
        private final List<Runnable> cleanups = new ArrayList<>();
        private ServerPlayer player;
        int age;
        boolean done;

        Context(TestRunner runner, TestCase test, ServerLevel level, BlockPos origin) {
            this.runner = runner;
            this.test = test;
            this.level = level;
            this.origin = origin;
        }

        public MinecraftServer server() {
            return level.getServer();
        }

        void prepare() {
            ChunkPos center = new ChunkPos(origin);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    level.setChunkForced(center.x + dx, center.z + dz, true);
                    int fx = center.x + dx;
                    int fz = center.z + dz;
                    cleanups.add(() -> level.setChunkForced(fx, fz, false));
                }
            }
            for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
                for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                    level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                    for (int y = 0; y < 12; y++) {
                        BlockPos p = origin.offset(x, y, z);
                        if (!level.getBlockState(p).isAir()) {
                            level.removeBlock(p, false);
                        }
                    }
                }
            }
        }

        /** The fake owner, created on first use. */
        public ServerPlayer player() {
            if (player == null) {
                player = MockPlayers.create(level, "LauraTester" + runner.index);
                player.teleportTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
                ServerPlayer p = player;
                cleanups.add(() -> MockPlayers.remove(p));
            }
            return player;
        }

        /** Runs when the test ends, passed or failed (restores a config value, removes blocks...). */
        public void onCleanup(Runnable action) {
            cleanups.add(action);
        }

        public void after(int ticks, Runnable action) {
            steps.add(new Step(age + ticks, action, null));
        }

        /** Checks the condition every tick; runs {@code then} once it holds, fails on timeout. */
        public void waitFor(String what, int maxTicks, BooleanSupplier condition, Runnable then) {
            steps.add(new Step(age + maxTicks, then, new Waiting(what, condition)));
        }

        public void check(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }

        public void succeed() {
            runner.finish(this, true, "");
        }

        public void fail(String message) {
            runner.finish(this, false, message);
        }

        void tick() {
            age++;
            if (age > test.timeoutTicks()) {
                fail("timeout");
                return;
            }
            for (Step step : new ArrayList<>(steps)) {
                if (done) {
                    return;
                }
                boolean due = age >= step.tick;
                if (step.waiting != null) {
                    boolean ok;
                    try {
                        ok = step.waiting.condition.getAsBoolean();
                    } catch (Throwable t) {
                        fail("error while waiting for " + step.waiting.what + ": " + t);
                        return;
                    }
                    if (ok) {
                        steps.remove(step);
                        run(step.action);
                    } else if (due) {
                        steps.remove(step);
                        fail("gave up waiting for " + step.waiting.what);
                        return;
                    }
                } else if (due) {
                    steps.remove(step);
                    run(step.action);
                }
            }
        }

        private void run(Runnable action) {
            try {
                action.run();
            } catch (AssertionError e) {
                fail(e.getMessage());
            } catch (Throwable t) {
                fail("exception: " + t);
                LauraMod.LOGGER.error("[SELFTEST] exception in {}", test.name(), t);
            }
        }

        void cleanup() {
            for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(origin).inflate(PLATFORM_RADIUS + 8, 16, PLATFORM_RADIUS + 8),
                    e -> !(e instanceof ServerPlayer))) {
                e.discard();
            }
            for (Runnable r : cleanups) {
                try {
                    r.run();
                } catch (Throwable ignored) {
                    // Best effort.
                }
            }
        }

        private record Step(int tick, Runnable action, Waiting waiting) {
        }

        private record Waiting(String what, BooleanSupplier condition) {
        }
    }
}
