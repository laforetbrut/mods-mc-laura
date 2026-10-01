package com.vyrriox.lauramod.test;

import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.entity.CombatMode;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.world.LauraManager;
import com.vyrriox.lauramod.world.LauraWorldData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Self tests of the fights between players: the PvP setting of the server, the teams, and the
 * companions and pets of the same player.
 *
 * @author vyrriox
 */
public final class CombatTests {
    private CombatTests() {
    }

    private record Case(String name, int timeoutTicks, TestBody body) implements TestRunner.TestCase {
        @Override
        public void run(TestRunner.Context ctx) throws Exception {
            body.run(ctx);
        }
    }

    @FunctionalInterface
    private interface TestBody {
        void run(TestRunner.Context ctx) throws Exception;
    }

    private static void add(List<TestRunner.TestCase> tests, String name, int timeoutTicks, TestBody body) {
        tests.add(new Case(name, timeoutTicks, body));
    }

    public static void addTo(List<TestRunner.TestCase> tests) {
        // Without PvP she neither fights another player nor takes damage from them.
        add(tests, "pvp_off_no_fight", 300, ctx -> withRival(ctx, (laura, rival) -> {
            pvp(ctx, false);
            float health = laura.getHealth();
            ctx.check(!laura.mayFight(rival) && !laura.canAttack(rival), "she may fight a player while PvP is off");
            ctx.check(!laura.hurt(laura.damageSources().playerAttack(rival), 2.0F), "another player hurt her while PvP is off");
            ctx.check(laura.getHealth() == health, "she lost health while PvP is off");
            laura.setTarget(rival);
            ctx.check(!laura.doHurtTarget(rival) && laura.getTarget() == null, "she hit a player while PvP is off");
            // Her partner always can, and she never fights back against them.
            ctx.check(laura.hurt(laura.damageSources().playerAttack(ctx.player()), 1.0F), "her partner could not hit her");
            ctx.check(!laura.mayFight(ctx.player()), "she may fight her partner");
            ctx.after(40, () -> {
                ctx.check(laura.getTarget() == null, "she targets " + laura.getTarget() + " while PvP is off");
                ctx.succeed();
            });
        }));
        // With PvP she defends herself, unless the config keeps her out of fights between players.
        add(tests, "pvp_on_fights_back", 300, ctx -> withRival(ctx, (laura, rival) -> {
            pvp(ctx, true);
            ctx.check(laura.mayFight(rival) && laura.canAttack(rival), "she may not fight a player while PvP is on");
            boolean allowed = LauraConfig.attackPlayers.get();
            ctx.onCleanup(() -> LauraConfig.attackPlayers.set(allowed));
            LauraConfig.attackPlayers.set(false);
            ctx.check(!laura.mayFight(rival), "attackPlayers = false did not keep her out of the fight");
            LauraConfig.attackPlayers.set(true);
            ctx.check(laura.hurt(laura.damageSources().playerAttack(rival), 1.0F), "another player could not hurt her while PvP is on");
            ctx.waitFor("her to fight back", 100, () -> laura.getTarget() == rival, ctx::succeed);
        }));
        // Teammates without friendly fire are out of reach, whatever the PvP setting.
        add(tests, "team_no_friendly_fire", 300, ctx -> withRival(ctx, (laura, rival) -> {
            pvp(ctx, true);
            Scoreboard scoreboard = ctx.server().getScoreboard();
            String name = "lauratest" + (ctx.origin.getZ() / 64);
            PlayerTeam old = scoreboard.getPlayerTeam(name);
            if (old != null) {
                scoreboard.removePlayerTeam(old);
            }
            PlayerTeam team = scoreboard.addPlayerTeam(name);
            ctx.onCleanup(() -> scoreboard.removePlayerTeam(team));
            team.setAllowFriendlyFire(false);
            scoreboard.addPlayerToTeam(ctx.player().getScoreboardName(), team);
            scoreboard.addPlayerToTeam(rival.getScoreboardName(), team);
            ctx.check(!laura.mayFight(rival), "she may fight a teammate of her partner");
            ctx.check(!laura.hurt(laura.damageSources().playerAttack(rival), 2.0F), "a teammate of her partner hurt her");
            team.setAllowFriendlyFire(true);
            ctx.check(laura.mayFight(rival), "friendly fire did not open the fight");
            ctx.succeed();
        }));
        // Companions and pets: never those of her partner, those of another player like the player.
        add(tests, "companions_and_pets", 300, ctx -> withRival(ctx, (laura, rival) -> {
            ServerPlayer player = ctx.player();
            Wolf pet = EntityType.WOLF.create(ctx.level);
            pet.moveTo(laura.getX() + 2, laura.getY(), laura.getZ(), 0, 0);
            pet.tame(player);
            ctx.level.addFreshEntity(pet);
            ctx.onCleanup(pet::discard);
            Zombie zombie = EntityType.ZOMBIE.create(ctx.level);
            zombie.moveTo(laura.getX() - 3, laura.getY(), laura.getZ(), 0, 0);
            zombie.setNoAi(true);
            ctx.level.addFreshEntity(zombie);
            ctx.onCleanup(zombie::discard);
            LauraWorldData.get(ctx.server()).meta(rival.getUUID()).lastSummon = 0;
            LauraManager.summon(rival, false);
            ctx.waitFor("the companion of the other player", 100, () -> !LauraManager.findAll(rival).isEmpty(), () -> {
                LauraEntity other = LauraManager.findAll(rival).get(0);
                pvp(ctx, true);
                ctx.check(laura.mayFight(zombie), "she may not fight a monster");
                ctx.check(!laura.mayFight(pet) && !laura.wantsToAttack(pet, player), "she may fight a pet of her partner");
                ctx.check(laura.mayFight(other), "she may not fight the companion of an enemy player while PvP is on");
                pvp(ctx, false);
                ctx.check(laura.mayFight(zombie), "PvP off keeps her from fighting monsters");
                ctx.check(!laura.mayFight(other) && !other.mayFight(laura), "two companions may fight while PvP is off");
                ctx.check(!other.hurt(other.damageSources().mobAttack(laura), 2.0F), "she hurt the companion of another player while PvP is off");
                ctx.check(!other.hurt(other.damageSources().mobAttack(pet), 2.0F), "a pet hurt the companion of another player while PvP is off");
                ctx.check(other.hurt(other.damageSources().mobAttack(zombie), 1.0F), "a monster could not hurt her while PvP is off");
                ctx.succeed();
            });
        }));
    }

    private static void pvp(TestRunner.Context ctx, boolean allowed) {
        ctx.server().setPvpAllowed(allowed);
    }

    /** Summons a defensive companion for the test player and brings a second player next to her. */
    private static void withRival(TestRunner.Context ctx, BiConsumer<LauraEntity, ServerPlayer> body) {
        ServerPlayer player = ctx.player();
        player.getInventory().clearContent();
        LauraWorldData.get(ctx.server()).meta(player.getUUID()).lastSummon = 0;
        boolean invulnerable = LauraConfig.invulnerable.get();
        ctx.onCleanup(() -> LauraConfig.invulnerable.set(invulnerable));
        LauraConfig.invulnerable.set(false);
        MinecraftServer server = ctx.server();
        boolean pvp = server.isPvpAllowed();
        ctx.onCleanup(() -> server.setPvpAllowed(pvp));
        LauraManager.summon(player, false);
        ctx.waitFor("the summon", 100, () -> !LauraManager.findAll(player).isEmpty(), () -> {
            LauraEntity laura = LauraManager.findAll(player).get(0);
            laura.brain().needs().fillAll();
            laura.setCombatMode(CombatMode.DEFENSIVE);
            ServerPlayer rival = MockPlayers.create(ctx.level, "LauraRival" + (ctx.origin.getZ() / 64));
            ctx.onCleanup(() -> MockPlayers.remove(rival));
            rival.teleportTo(laura.getX() + 3, laura.getY(), laura.getZ());
            body.accept(laura, rival);
        });
    }
}
