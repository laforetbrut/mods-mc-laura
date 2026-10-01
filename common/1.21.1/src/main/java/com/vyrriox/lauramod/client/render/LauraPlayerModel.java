package com.vyrriox.lauramod.client.render;

import com.vyrriox.lauramod.client.anim.DefaultAnimations;
import com.vyrriox.lauramod.client.anim.LauraAnimator;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.model.AnimationSampler;
import com.vyrriox.lauramod.model.Molang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

import java.util.Map;

/**
 * The default, player-like model of Laura, with her animations applied on top of the vanilla
 * poses (walking, looking around).
 *
 * @author vyrriox
 */
public class LauraPlayerModel extends PlayerModel<LauraEntity> {
    private final Molang.Context context = new Molang.Context();
    private float partialTick;

    public LauraPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    public void setPartialTick(float partialTick) {
        this.partialTick = partialTick;
    }

    @Override
    public void setupAnim(LauraEntity laura, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Vanilla does not reset every offset each frame: start from the rest pose so animations never drift.
        for (ModelPart part : new ModelPart[]{this.head, this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg}) {
            part.resetPose();
        }
        this.riding = this.riding || laura.isInSittingPose();
        super.setupAnim(laura, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (laura.isAsleep()) {
            this.head.xRot = 0;
            this.head.yRot = 0;
        }
        context.lifeTime = ageInTicks / 20F;
        context.groundSpeed = limbSwingAmount * 4;
        context.health = laura.getHealth();
        context.maxHealth = laura.getMaxHealth();
        context.isSitting = laura.isInSittingPose() ? 1 : 0;
        float gameTime = laura.level() == null ? 0 : laura.level().getGameTime() + partialTick;
        boolean moving = limbSwingAmount > 0.08F;
        Map<String, AnimationSampler.BonePose> poses = LauraAnimator.compute(laura, DefaultAnimations.get(), ageInTicks / 20F, gameTime, moving, context);
        apply(poses.get("head"), this.head);
        apply(poses.get("body"), this.body);
        apply(poses.get("right_arm"), this.rightArm);
        apply(poses.get("left_arm"), this.leftArm);
        apply(poses.get("right_leg"), this.rightLeg);
        apply(poses.get("left_leg"), this.leftLeg);
        this.hat.copyFrom(this.head);
        this.jacket.copyFrom(this.body);
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightPants.copyFrom(this.rightLeg);
        this.leftPants.copyFrom(this.leftLeg);
        AnimationSampler.BonePose root = poses.get("root");
        rootOffsetY = root == null ? 0 : root.posY;
        rootRotX = root == null ? 0 : root.rotX;
    }

    /** Whole-model offset (pixels, Y down) and forward tilt (degrees) from the "root" track. */
    float rootOffsetY;
    float rootRotX;

    private static void apply(AnimationSampler.BonePose pose, ModelPart part) {
        if (pose == null) {
            return;
        }
        part.xRot += (float) Math.toRadians(pose.rotX);
        part.yRot += (float) Math.toRadians(pose.rotY);
        part.zRot += (float) Math.toRadians(pose.rotZ);
        part.x += pose.posX;
        part.y += pose.posY;
        part.z += pose.posZ;
        if (pose.scaleX != 1 || pose.scaleY != 1 || pose.scaleZ != 1) {
            part.xScale = pose.scaleX;
            part.yScale = pose.scaleY;
            part.zScale = pose.scaleZ;
        } else {
            part.xScale = 1;
            part.yScale = 1;
            part.zScale = 1;
        }
    }

    static float partialTickNow() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
    }
}
