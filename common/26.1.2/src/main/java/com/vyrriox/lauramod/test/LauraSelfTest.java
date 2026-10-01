package com.vyrriox.lauramod.test;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.vyrriox.lauramod.LauraMod;
import net.minecraft.server.MinecraftServer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * In-game test suite. Start a server with {@code -Dlauramod.selftest=true}: once the world is loaded
 * every test of {@link LauraTests} runs on a clean platform high in the sky, results are logged and
 * written to {@code lauramod-selftest.json}, then the server stops (exit status tells the result).
 *
 * @author vyrriox
 */
public final class LauraSelfTest {
    public static final String PROPERTY = "lauramod.selftest";

    private static TestRunner runner;
    private static boolean finished;

    private LauraSelfTest() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean(PROPERTY);
    }

    public static void onServerStarted(MinecraftServer server) {
        if (!enabled()) {
            return;
        }
        LauraMod.LOGGER.info("[SELFTEST] Starting the Laura self test suite");
        runner = new TestRunner(server, LauraTests.all());
    }

    public static void tick(MinecraftServer server) {
        if (runner == null) {
            return;
        }
        if (finished) {
            if (haltIn > 0 && --haltIn == 0) {
                server.halt(false);
            }
            return;
        }
        if (runner.tick()) {
            finished = true;
            report(server, runner.results());
        }
    }

    private static void report(MinecraftServer server, List<TestRunner.Result> results) {
        int passed = 0;
        JsonArray array = new JsonArray();
        List<String> failures = new ArrayList<>();
        for (TestRunner.Result r : results) {
            JsonObject o = new JsonObject();
            o.addProperty("name", r.name());
            o.addProperty("passed", r.passed());
            o.addProperty("message", r.message());
            o.addProperty("ticks", r.ticks());
            array.add(o);
            if (r.passed()) {
                passed++;
            } else {
                failures.add(r.name() + ": " + r.message());
            }
        }
        JsonObject root = new JsonObject();
        root.addProperty("loader", LauraMod.platform().loaderName());
        root.addProperty("version", LauraMod.platform().modVersion());
        root.addProperty("passed", passed);
        root.addProperty("failed", results.size() - passed);
        root.add("results", array);
        try {
            Path out = LauraMod.platform().gameDir().resolve("lauramod-selftest.json");
            Files.writeString(out, new GsonBuilder().setPrettyPrinting().create().toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LauraMod.LOGGER.error("[SELFTEST] Could not write the report", e);
        }
        LauraMod.LOGGER.info("[SELFTEST] {} passed, {} failed", passed, results.size() - passed);
        for (String failure : failures) {
            LauraMod.LOGGER.error("[SELFTEST] FAILED {}", failure);
        }
        LauraMod.LOGGER.info("[SELFTEST] RESULT {}", failures.isEmpty() ? "SUCCESS" : "FAILURE");
        exitCode = failures.isEmpty() ? 0 : 1;
        // Never stop in the tick the last fake player left: chunks around it are still generating,
        // and vanilla's stop loop can then spin forever on "Saving worlds". A few seconds of normal
        // ticks let that work settle first.
        haltIn = SETTLE_TICKS;
        exitWhenStopped(exitCode, STOP_TIMEOUT_MS);
    }

    private static final int SETTLE_TICKS = 100;
    private static final long STOP_TIMEOUT_MS = 90_000L;
    private static int exitCode = -1;
    private static int haltIn = -1;
    private static volatile boolean stopped;
    private static boolean exiting;

    /** Called once the server has stopped. */
    public static void onServerStopped() {
        if (!enabled()) {
            return;
        }
        stopped = true;
        if (exitCode < 0) {
            // Stopped before the tests ran (port taken, crash at start...): that is a failure too.
            LauraMod.LOGGER.error("[SELFTEST] RESULT FAILURE: the server stopped before the tests ran");
            exitWhenStopped(2, 0);
        }
    }

    /**
     * Ends the process with the result as exit code once the server has stopped, or after the
     * timeout if it never does. Some mods keep non-daemon threads alive and a stop can hang, which
     * would otherwise leave the test server running forever.
     */
    private static synchronized void exitWhenStopped(int code, long timeoutMs) {
        if (exiting) {
            return;
        }
        exiting = true;
        Thread exit = new Thread(() -> {
            long deadline = System.currentTimeMillis() + timeoutMs;
            try {
                while (!stopped && System.currentTimeMillis() < deadline) {
                    Thread.sleep(200);
                }
                if (!stopped) {
                    LauraMod.LOGGER.error("[SELFTEST] The server did not stop in time, forcing the exit");
                }
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            Runtime.getRuntime().halt(code);
        }, "Laura self test exit");
        // Not a daemon: if the JVM could end on its own first, it would exit with 0 and hide failures.
        exit.setDaemon(false);
        exit.start();
    }
}
