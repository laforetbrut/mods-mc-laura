package com.vyrriox.lauramod.entity.ai;

import com.vyrriox.lauramod.config.LauraConfig;
import com.vyrriox.lauramod.entity.CombatMode;
import com.vyrriox.lauramod.entity.LauraEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;

/**
 * Combat goals, all gated by her combat mode.
 *
 * @author vyrriox
 */
public final class LauraTargetGoals {
    private LauraTargetGoals() {
    }

    static boolean canFight(LauraEntity laura) {
        return laura.getCombatMode() != CombatMode.PASSIVE && !laura.isAsleep() && !laura.isOrderedToSit() && !laura.brain().isSulking();
    }

    public static void register(LauraEntity laura, GoalSelector targets) {
        targets.addGoal(1, new OwnerHurtByTargetGoal(laura) {
            @Override
            public boolean canUse() {
                return canFight(laura) && super.canUse();
            }
        });
        targets.addGoal(2, new OwnerHurtTargetGoal(laura) {
            @Override
            public boolean canUse() {
                return canFight(laura) && super.canUse();
            }
        });
        targets.addGoal(3, new HurtByTargetGoal(laura) {
            @Override
            public boolean canUse() {
                LivingEntity attacker = laura.getLastHurtByMob();
                return LauraConfig.retaliate.get() && canFight(laura) && attacker != null && !laura.isOwnedBy(attacker) && super.canUse();
            }
        });
        targets.addGoal(4, new NearestAttackableTargetGoal<>(laura, Monster.class, 10, true, false,
                target -> !(target instanceof Creeper) && laura.getCombatMode() == CombatMode.AGGRESSIVE) {
            @Override
            public boolean canUse() {
                return laura.getCombatMode() == CombatMode.AGGRESSIVE && canFight(laura) && super.canUse();
            }
        });
    }

    /** Melee attack, only when she is allowed to fight. */
    public static class Melee extends MeleeAttackGoal {
        private final LauraEntity laura;

        public Melee(LauraEntity laura) {
            super(laura, 1.2, true);
            this.laura = laura;
        }

        @Override
        public boolean canUse() {
            return canFight(laura) && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return canFight(laura) && super.canContinueToUse();
        }
    }

    /** In passive mode she keeps away from monsters. */
    public static class AvoidMonsters extends AvoidEntityGoal<Monster> {
        private final LauraEntity laura;

        public AvoidMonsters(LauraEntity laura) {
            super(laura, Monster.class, 8.0F, 1.0, 1.25);
            this.laura = laura;
        }

        @Override
        public boolean canUse() {
            return laura.getCombatMode() == CombatMode.PASSIVE && !laura.isOrderedToSit() && !laura.isAsleep() && super.canUse();
        }
    }
}
