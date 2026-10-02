package com.vyrriox.lauramod.client;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.client.gui.EmoteWheelScreen;
import com.vyrriox.lauramod.client.gui.LauraMenuScreen;
import com.vyrriox.lauramod.config.LauraClientConfig;
import com.vyrriox.lauramod.desire.Desire;
import com.vyrriox.lauramod.desire.DesireType;
import com.vyrriox.lauramod.dialogue.LineFormatter;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.entity.LauraJob;
import com.vyrriox.lauramod.entity.LauraMode;
import com.vyrriox.lauramod.entity.LauraSpeech;
import com.vyrriox.lauramod.entity.Mood;
import com.vyrriox.lauramod.entity.brain.Needs;
import com.vyrriox.lauramod.entity.work.LauraTask;
import com.vyrriox.lauramod.entity.work.WorkArea;
import com.vyrriox.lauramod.skin.SkinRef;
import com.vyrriox.lauramod.world.LauraActions;
import com.vyrriox.lauramod.world.LauraManager;
import com.vyrriox.lauramod.world.LauraWorldData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Capture tool for the page media. It is not part of the mod: copy it next to {@code LauraClient}
 * to record footage, then remove it (see tools/media/README.md). It runs the script named by
 * {@code -Dlauramod.capture=<file in the game directory>}, one command per line.
 *
 * @author vyrriox
 */
public final class CaptureStudio {
    private static final String SCRIPT = System.getProperty("lauramod.capture", "");
    private static List<String> lines;
    private static int pc;
    private static int wait;
    private static boolean worldAsked;
    private static String recName = "";
    private static int recLeft;
    private static int recIndex;
    private static boolean track;
    private static double trackDy = 1.2;
    private static boolean noToast;
    private static boolean mouseLock;
    private static double mouseX;
    private static double mouseY;
    private static BlockPos cropMin;
    private static BlockPos cropMax;
    private static int cropCount = -1;

    private CaptureStudio() {
    }

    public static void tick() {
        if (SCRIPT.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        try {
            step(mc);
        } catch (Throwable t) {
            LauraMod.LOGGER.error("[CAPTURE] line {} failed", pc, t);
        }
    }

    private static void step(Minecraft mc) throws Exception {
        if (lines == null) {
            lines = new ArrayList<>();
            for (String raw : Files.readAllLines(mc.gameDirectory.toPath().resolve(SCRIPT), StandardCharsets.UTF_8)) {
                String line = raw.strip();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    lines.add(line);
                }
            }
            LauraMod.LOGGER.info("[CAPTURE] {} commands", lines.size());
        }
        if (mc.player == null || mc.level == null) {
            if (!worldAsked && mc.getOverlay() == null && mc.screen instanceof TitleScreen) {
                worldAsked = true;
                String[] w = lines.get(0).split("\\s+");
                pc = 1;
                String name = w[1];
                long seed = Long.parseLong(w[2]);
                if (mc.getLevelSource().levelExists(name)) {
                    LauraMod.LOGGER.info("[CAPTURE] opening world {}", name);
                    mc.createWorldOpenFlows().openWorld(name, () -> LauraMod.LOGGER.error("[CAPTURE] could not open {}", name));
                } else {
                    LauraMod.LOGGER.info("[CAPTURE] creating world {} seed {}", name, seed);
                    LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(seed, true, false), WorldPresets::createNormalWorldDimensions, mc.screen);
                }
            }
            return;
        }
        if (mc.getSingleplayerServer() == null) {
            return;
        }
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) {
            mc.setScreen(null);
        }
        if (noToast) {
            mc.getToasts().clear();
        }
        if (mouseLock) {
            // Keeps the pointer where the script put it: no tooltip under the real mouse.
            try {
                for (String name : new String[]{"xpos", "ypos"}) {
                    java.lang.reflect.Field f = net.minecraft.client.MouseHandler.class.getDeclaredField(name);
                    f.setAccessible(true);
                    f.setDouble(mc.mouseHandler, name.equals("xpos") ? mouseX : mouseY);
                }
            } catch (ReflectiveOperationException e) {
                mouseLock = false;
                LauraMod.LOGGER.error("[CAPTURE] cannot move the pointer", e);
            }
        }
        if (track) {
            LauraEntity l = LauraClient.nearestOwned(96);
            if (l != null) {
                mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, l.position().add(0, trackDy, 0));
            }
        }
        if (recLeft > 0) {
            if (cropMin != null) {
                // Logs the frame of every harvest, to time the titles on it.
                int ripe = 0;
                for (BlockPos pos : BlockPos.betweenClosed(cropMin, cropMax)) {
                    BlockState state = mc.level.getBlockState(pos);
                    if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                        ripe++;
                    }
                }
                if (ripe != cropCount) {
                    cropCount = ripe;
                    LauraMod.LOGGER.info("[CAPTURE] crops {} {} {}", recName, recIndex, ripe);
                }
            }
            grab(mc, String.format(Locale.ROOT, "%s_%04d.png", recName, recIndex++));
            recLeft--;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        if (pc >= lines.size()) {
            return;
        }
        String line = lines.get(pc++);
        LauraMod.LOGGER.info("[CAPTURE] > {}", line);
        wait = 2;
        run(mc, line);
    }

    private static void grab(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {
        });
    }

    private static void onServer(Minecraft mc, Consumer<ServerPlayer> action) {
        MinecraftServer server = mc.getSingleplayerServer();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
            if (player != null) {
                try {
                    action.accept(player);
                } catch (Throwable t) {
                    LauraMod.LOGGER.error("[CAPTURE] server action failed", t);
                }
            }
        });
    }

    private static void onLaura(Minecraft mc, java.util.function.BiConsumer<ServerPlayer, LauraEntity> action) {
        onServer(mc, player -> {
            List<LauraEntity> all = LauraManager.findAll(player);
            if (all.isEmpty()) {
                LauraMod.LOGGER.warn("[CAPTURE] no companion");
                return;
            }
            action.accept(player, all.get(0));
        });
    }

    private static void fly(ServerPlayer player) {
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
    }

    private static int ground(ServerLevel level, double x, double z) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        level.getChunk(bx >> 4, bz >> 4);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
    }

    private static void face(LauraEntity l, float yaw) {
        l.setYRot(yaw);
        l.setYBodyRot(yaw);
        l.setYHeadRot(yaw);
        l.setXRot(0);
        l.yRotO = yaw;
        l.yBodyRotO = yaw;
        l.yHeadRotO = yaw;
    }

    private static boolean on(String s) {
        return s.equalsIgnoreCase("on") || s.equalsIgnoreCase("true");
    }

    private static void run(Minecraft mc, String line) {
        String[] a = line.split("\\s+");
        String rest = line.contains(" ") ? line.substring(line.indexOf(' ') + 1).strip() : "";
        switch (a[0]) {
            case "wait" -> wait = Integer.parseInt(a[1]);
            case "log" -> LauraMod.LOGGER.info("[CAPTURE] {}", rest);
            case "quit" -> mc.stop();
            case "cmd" -> onServer(mc, player -> player.getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack().withPermission(4).withSuppressedOutput(), rest));
            case "time" -> onServer(mc, player -> player.serverLevel().setDayTime(Long.parseLong(a[1])));
            case "clear" -> onServer(mc, player -> player.serverLevel().setWeatherParameters(1000000, 0, false, false));
            case "fov" -> mc.options.fov().set(Integer.parseInt(a[1]));
            case "rd" -> mc.options.renderDistance().set(Integer.parseInt(a[1]));
            case "guiscale" -> {
                mc.options.guiScale().set(Integer.parseInt(a[1]));
                mc.resizeDisplay();
            }
            case "hud" -> mc.options.hideGui = !on(a[1]);
            case "notoast" -> noToast = on(a[1]);
            case "mouse" -> {
                mouseLock = !a[1].equals("free");
                if (mouseLock) {
                    mouseX = Double.parseDouble(a[1]);
                    mouseY = Double.parseDouble(a[2]);
                }
            }
            case "bubbles" -> LauraClientConfig.speechBubbles.set(on(a[1]));
            case "thoughts" -> LauraClientConfig.thoughtBubbles.set(on(a[1]));
            case "lang" -> {
                mc.getLanguageManager().setSelected(a[1]);
                mc.options.languageCode = a[1];
                mc.reloadResourcePacks();
            }
            case "needshud" -> LauraClientConfig.showNeedsHud.set(on(a[1]));
            case "track" -> {
                track = on(a[1]);
                if (a.length > 2) {
                    trackDy = Double.parseDouble(a[2]);
                }
            }
            case "shot" -> grab(mc, a[1] + ".png");
            case "record" -> {
                recName = a[1];
                recLeft = Integer.parseInt(a[2]);
                recIndex = 1;
            }
            case "pos" -> onServer(mc, player -> LauraMod.LOGGER.info("[CAPTURE] pos {} {} {} yaw {} pitch {}",
                    String.format(Locale.ROOT, "%.2f", player.getX()), String.format(Locale.ROOT, "%.2f", player.getY()),
                    String.format(Locale.ROOT, "%.2f", player.getZ()), String.format(Locale.ROOT, "%.1f", player.getYRot()),
                    String.format(Locale.ROOT, "%.1f", player.getXRot())));
            case "biome" -> onServer(mc, player -> {
                ServerLevel level = player.serverLevel();
                ResourceLocation id = ResourceLocation.parse(a[1]);
                var found = level.findClosestBiome3d(h -> h.is(id), player.blockPosition(), Integer.parseInt(a[2]), 32, 64);
                if (found == null) {
                    LauraMod.LOGGER.warn("[CAPTURE] biome {} not found", id);
                    return;
                }
                BlockPos p = found.getFirst();
                int y = ground(level, p.getX(), p.getZ());
                fly(player);
                player.teleportTo(level, p.getX() + 0.5, y, p.getZ() + 0.5, player.getYRot(), player.getXRot());
                LauraMod.LOGGER.info("[CAPTURE] biome {} at {} {} {}", id, p.getX(), y, p.getZ());
            });
            case "flat" -> onServer(mc, player -> flat(player, Integer.parseInt(a[1]), a.length > 2 ? a[2] : ""));
            case "scan" -> onServer(mc, player -> {
                // scan <x1> <z1> <x2> <z2>: ground height of every column, one log line per z (a star under leaves).
                ServerLevel level = player.serverLevel();
                for (int z = Integer.parseInt(a[2]); z <= Integer.parseInt(a[4]); z++) {
                    StringBuilder sb = new StringBuilder();
                    for (int x = Integer.parseInt(a[1]); x <= Integer.parseInt(a[3]); x++) {
                        int g = ground(level, x, z);
                        sb.append(g).append(level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) > g ? "* " : "  ");
                    }
                    LauraMod.LOGGER.info("[CAPTURE] scan z={} : {}", z, sb);
                }
            });
            case "field" -> onServer(mc, player -> {
                // field <x1> <z1> <x2> <z2> [path x]: ripe wheat on every grass column, a path on one line.
                ServerLevel level = player.serverLevel();
                int pathX = a.length > 5 ? Integer.parseInt(a[5]) : Integer.MIN_VALUE;
                int placed = 0;
                for (int x = Integer.parseInt(a[1]); x <= Integer.parseInt(a[3]); x++) {
                    for (int z = Integer.parseInt(a[2]); z <= Integer.parseInt(a[4]); z++) {
                        BlockPos soil = new BlockPos(x, ground(level, x, z) - 1, z);
                        BlockState below = level.getBlockState(soil);
                        if (!below.is(Blocks.GRASS_BLOCK) && !below.is(Blocks.DIRT) && !below.is(Blocks.FARMLAND) && !below.is(Blocks.DIRT_PATH)) {
                            continue;
                        }
                        if (x == pathX) {
                            level.setBlock(soil.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                            level.setBlock(soil, Blocks.DIRT_PATH.defaultBlockState(), Block.UPDATE_ALL);
                        } else {
                            level.setBlock(soil, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7), Block.UPDATE_ALL);
                            level.setBlock(soil.above(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7), Block.UPDATE_ALL);
                            placed++;
                        }
                    }
                }
                LauraMod.LOGGER.info("[CAPTURE] field of {} crops", placed);
            });
            case "crops" -> {
                // crops <x1> <y1> <z1> <x2> <y2> <z2> | off: the ripe crops counted while recording.
                cropCount = -1;
                if (a[1].equals("off")) {
                    cropMin = null;
                } else {
                    cropMin = new BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]));
                    cropMax = new BlockPos(Integer.parseInt(a[4]), Integer.parseInt(a[5]), Integer.parseInt(a[6]));
                }
            }
            case "tp" -> onServer(mc, player -> {
                fly(player);
                player.teleportTo(player.serverLevel(), Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3]),
                        a.length > 4 ? Float.parseFloat(a[4]) : player.getYRot(), a.length > 5 ? Float.parseFloat(a[5]) : player.getXRot());
            });
            case "tpr" -> onServer(mc, player -> {
                fly(player);
                player.teleportTo(player.serverLevel(), player.getX() + Double.parseDouble(a[1]), player.getY() + Double.parseDouble(a[2]),
                        player.getZ() + Double.parseDouble(a[3]), player.getYRot(), player.getXRot());
            });
            case "rot" -> onServer(mc, player -> {
                fly(player);
                player.teleportTo(player.serverLevel(), player.getX(), player.getY(), player.getZ(), Float.parseFloat(a[1]), Float.parseFloat(a[2]));
            });
            case "ground" -> onServer(mc, player -> {
                fly(player);
                player.teleportTo(player.serverLevel(), player.getX(), ground(player.serverLevel(), player.getX(), player.getZ()) + (a.length > 1 ? Double.parseDouble(a[1]) : 0),
                        player.getZ(), player.getYRot(), player.getXRot());
            });
            case "cam" -> onLaura(mc, (player, l) -> {
                // cam <distance> <eye height above her feet> <angle around her> <look height> [yaw shift]
                double dist = Double.parseDouble(a[1]);
                double height = Double.parseDouble(a[2]);
                float angle = Float.parseFloat(a[3]);
                double lookHeight = Double.parseDouble(a[4]);
                float shift = a.length > 5 ? Float.parseFloat(a[5]) : 0;
                Vec3 dir = Vec3.directionFromRotation(0, l.getYRot() + angle);
                Vec3 eye = l.position().add(dir.scale(dist)).add(0, height, 0);
                Vec3 target = l.position().add(0, lookHeight, 0);
                double dx = target.x - eye.x;
                double dy = target.y - eye.y;
                double dz = target.z - eye.z;
                float yRot = (float) (Mth.atan2(dz, dx) * 180 / Math.PI) - 90 + shift;
                float xRot = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180 / Math.PI);
                fly(player);
                player.teleportTo(player.serverLevel(), eye.x, eye.y - player.getEyeHeight(), eye.z, yRot, xRot);
            });
            case "bubble" -> {
                LauraEntity l = LauraClient.nearestOwned(96);
                if (l != null) {
                    String text = rest.substring(rest.indexOf(' ') + 1);
                    ClientState.addBubble(l.getId(), Component.literal(text), Integer.parseInt(a[1]));
                }
            }
            case "screen" -> {
                LauraEntity l = LauraClient.nearestOwned(96);
                switch (a[1]) {
                    case "menu" -> {
                        if (l != null) {
                            mc.setScreen(new LauraMenuScreen(l, LauraMenuScreen.Tab.valueOf(a[2])));
                        }
                    }
                    case "wheel" -> {
                        if (l != null) {
                            mc.setScreen(new EmoteWheelScreen(l));
                        }
                    }
                    case "inv" -> onLaura(mc, (player, laura) -> laura.openInventory(player));
                    default -> mc.setScreen(null);
                }
            }
            case "laura" -> laura(mc, a, line);
            default -> LauraMod.LOGGER.warn("[CAPTURE] unknown command {}", line);
        }
    }

    private static void laura(Minecraft mc, String[] a, String line) {
        switch (a[1]) {
            case "summon" -> onServer(mc, player -> {
                MinecraftServer server = player.getServer();
                for (java.util.UUID owner : LauraWorldData.get(server).all().stream().map(r -> r.owner).distinct().toList()) {
                    com.vyrriox.lauramod.test.MockPlayers.forgetCompanions(server, owner);
                }
                for (ServerLevel level : server.getAllLevels()) {
                    List<LauraEntity> left = new ArrayList<>();
                    for (Entity entity : level.getAllEntities()) {
                        if (entity instanceof LauraEntity l) {
                            left.add(l);
                        }
                    }
                    left.forEach(LauraEntity::discard);
                }
                LauraWorldData.get(server).meta(player.getUUID()).lastSummon = 0;
                LauraManager.summon(player, false);
            });
            case "front" -> onLaura(mc, (player, l) -> {
                // laura front <distance> [side]: on the ground in front of the camera, facing it.
                double dist = Double.parseDouble(a[2]);
                double side = a.length > 3 ? Double.parseDouble(a[3]) : 0;
                Vec3 dir = Vec3.directionFromRotation(0, player.getYRot());
                Vec3 right = new Vec3(-dir.z, 0, dir.x);
                double x = player.getX() + dir.x * dist + right.x * side;
                double z = player.getZ() + dir.z * dist + right.z * side;
                int y = ground(player.serverLevel(), x, z);
                l.setNoAi(true);
                l.getNavigation().stop();
                l.teleportTo(x, y, z);
                face(l, player.getYRot() + 180);
                LauraMod.LOGGER.info("[CAPTURE] laura at {} {} {} yaw {}", String.format(Locale.ROOT, "%.2f", x), y,
                        String.format(Locale.ROOT, "%.2f", z), String.format(Locale.ROOT, "%.1f", l.getYRot()));
            });
            case "at" -> onLaura(mc, (player, l) -> {
                l.setNoAi(true);
                l.getNavigation().stop();
                l.teleportTo(Double.parseDouble(a[2]), Double.parseDouble(a[3]), Double.parseDouble(a[4]));
                face(l, Float.parseFloat(a[5]));
            });
            case "hide" -> onLaura(mc, (player, l) -> {
                l.setNoAi(true);
                l.teleportTo(l.getX(), 400, l.getZ());
            });
            case "appear" -> onLaura(mc, (player, l) -> {
                // laura appear <x> <y> <z> <yaw>: as when she is summoned (sparkles, hearts, a wave).
                double x = Double.parseDouble(a[2]);
                double y = Double.parseDouble(a[3]);
                double z = Double.parseDouble(a[4]);
                l.setNoAi(true);
                l.teleportTo(x, y, z);
                face(l, Float.parseFloat(a[5]));
                ServerLevel level = player.serverLevel();
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, x, y + 1, z, 30, 0.4, 0.6, 0.4, 0.05);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, x, y + 1.8, z, 5, 0.4, 0.3, 0.4, 0.0);
                l.playEmote(Emote.WAVE);
            });
            case "hearts" -> onLaura(mc, (player, l) -> player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                    l.getX(), l.getY() + 1.8, l.getZ(), Integer.parseInt(a[2]), 0.4, 0.3, 0.4, 0.0));
            case "turn" -> onLaura(mc, (player, l) -> face(l, l.getYRot() + Float.parseFloat(a[2])));
            case "noai" -> onLaura(mc, (player, l) -> l.setNoAi(on(a[2])));
            case "mode" -> onLaura(mc, (player, l) -> l.setMode(LauraMode.valueOf(a[2])));
            case "skin" -> onLaura(mc, (player, l) -> l.setSkin(SkinRef.parse("builtin:" + a[2]), a.length > 3 ? on(a[3]) : l.isSlim()));
            case "emote" -> onLaura(mc, (player, l) -> {
                Emote emote = Emote.valueOf(a[2]);
                if (a.length > 3 && a[3].equals("say")) {
                    LauraActions.emote(player, l, emote);
                } else {
                    l.playEmote(emote);
                }
            });
            case "say" -> onLaura(mc, (player, l) -> LauraSpeech.say(l, player, a[2], LineFormatter.values()));
            case "mood" -> onLaura(mc, (player, l) -> l.setMood(Mood.valueOf(a[2])));
            case "need" -> onLaura(mc, (player, l) -> l.brain().needs().set(Needs.Need.valueOf(a[2]), Float.parseFloat(a[3])));
            case "fill" -> onLaura(mc, (player, l) -> l.brain().needs().fillAll());
            case "happy" -> onLaura(mc, (player, l) -> l.brain().makeHappy(Integer.parseInt(a[2])));
            case "affection" -> onLaura(mc, (player, l) -> l.setAffection(Integer.parseInt(a[2])));
            case "nodesire" -> onLaura(mc, (player, l) -> l.brain().forceDesire(null));
            case "desire" -> onLaura(mc, (player, l) -> {
                long now = l.level().getGameTime();
                l.brain().forceDesire(new Desire(DesireType.Kind.valueOf(a[2]), a[3], false, now, now + 20 * 900));
            });
            case "back" -> onLaura(mc, (player, l) -> l.setBackItem(stack(a[2])));
            case "hold" -> onLaura(mc, (player, l) -> l.setItemSlot(EquipmentSlot.MAINHAND, stack(a[2])));
            case "carry" -> onLaura(mc, (player, l) -> l.setCarried(stack(a[2])));
            case "gag" -> onLaura(mc, (player, l) -> l.setGagged(on(a[2]), 20 * 300));
            case "name" -> onLaura(mc, (player, l) -> l.setCustomName(Component.literal(a[2])));
            case "task" -> onLaura(mc, (player, l) -> {
                l.setNoAi(false);
                l.workplace().replaceCurrent(new LauraTask(LauraTask.Type.valueOf(a[2]), "", 0,
                        new WorkArea(l.blockPosition(), Integer.parseInt(a[3]), l.level().dimension())));
            });
            case "job" -> onLaura(mc, (player, l) -> {
                l.setNoAi(false);
                l.workplace().enableJob(LauraJob.valueOf(a[2]), new WorkArea(l.blockPosition(), Integer.parseInt(a[3]), l.level().dimension()));
                l.setMode(LauraMode.WORK);
            });
            default -> LauraMod.LOGGER.warn("[CAPTURE] unknown command {}", line);
        }
    }

    private static ItemStack stack(String id) {
        return id.equals("none") ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    /** Moves the player to the flattest open spot around, preferring one with trees nearby. */
    private static void flat(ServerPlayer player, int radius, String biome) {
        ServerLevel level = player.serverLevel();
        BlockPos c = player.blockPosition();
        double bestScore = Double.MAX_VALUE;
        BlockPos best = null;
        for (int dx = -radius; dx <= radius; dx += 4) {
            for (int dz = -radius; dz <= radius; dz += 4) {
                int x = c.getX() + dx;
                int z = c.getZ() + dz;
                int min = Integer.MAX_VALUE;
                int max = Integer.MIN_VALUE;
                int canopy = 0;
                boolean wet = false;
                for (int ox = -4; ox <= 4; ox++) {
                    for (int oz = -4; oz <= 4; oz++) {
                        level.getChunk((x + ox) >> 4, (z + oz) >> 4);
                        int g = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + ox, z + oz);
                        int t = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x + ox, z + oz);
                        min = Math.min(min, g);
                        max = Math.max(max, g);
                        if (t > g) {
                            canopy++;
                        }
                        if (!level.getFluidState(new BlockPos(x + ox, g - 1, z + oz)).isEmpty()) {
                            wet = true;
                        }
                    }
                }
                if (wet) {
                    continue;
                }
                int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (!biome.isEmpty() && !level.getBiome(new BlockPos(x, ground, z)).is(ResourceLocation.parse(biome))) {
                    continue;
                }
                int trees = 0;
                for (int ox = -16; ox <= 16; ox += 2) {
                    for (int oz = -16; oz <= 16; oz += 2) {
                        if (Math.abs(ox) > 6 || Math.abs(oz) > 6) {
                            level.getChunk((x + ox) >> 4, (z + oz) >> 4);
                            if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, x + ox, z + oz) > level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + ox, z + oz)) {
                                trees++;
                            }
                        }
                    }
                }
                double score = (max - min) * 8.0 + canopy * 2.0 - Math.min(trees, 60) * 0.4;
                if (score < bestScore) {
                    bestScore = score;
                    best = new BlockPos(x, ground, z);
                }
            }
        }
        if (best == null) {
            LauraMod.LOGGER.warn("[CAPTURE] no flat spot");
            return;
        }
        fly(player);
        player.teleportTo(level, best.getX() + 0.5, best.getY(), best.getZ() + 0.5, player.getYRot(), player.getXRot());
        LauraMod.LOGGER.info("[CAPTURE] flat spot {} {} {} score {}", best.getX(), best.getY(), best.getZ(), String.format(Locale.ROOT, "%.1f", bestScore));
    }
}
