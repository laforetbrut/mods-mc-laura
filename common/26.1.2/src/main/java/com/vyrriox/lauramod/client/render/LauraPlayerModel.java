package com.vyrriox.lauramod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vyrriox.lauramod.client.anim.DefaultAnimations;
import com.vyrriox.lauramod.client.anim.LauraAnimator;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.model.AnimationSampler;
import com.vyrriox.lauramod.model.Molang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.HumanoidArm;

import java.util.Map;

/**
 * The default, player-like model of Laura, with her animations applied on top of the vanilla
 * poses (walking, looking around). Also used for her armor, so it follows the same animations.
 * It is baked from the player model layers but is not the vanilla player model, which only takes
 * avatar render states.
 *
 * @author vyrriox
 */
public class LauraPlayerModel extends HumanoidModel<LauraRenderState> {
    private static final Molang.Context CONTEXT = new Molang.Context();

    private final boolean slim;

    public LauraPlayerModel(ModelPart root, boolean slim) {
        // Translucent like the vanilla player model: skins may have see-through outer layers.
        super(root, RenderTypes::entityTranslucent);
        this.slim = slim;
    }

    /** Computes her animation for this frame into the render state, while the entity can still be read. */
    public static void extract(LauraEntity laura, LauraRenderState state, float partialTick) {
        float ageInTicks = state.ageInTicks;
        float limbSwingAmount = state.walkAnimationSpeed;
        CONTEXT.lifeTime = ageInTicks / 20F;
        CONTEXT.groundSpeed = limbSwingAmount * 4;
        CONTEXT.health = laura.getHealth();
        CONTEXT.maxHealth = laura.getMaxHealth();
        CONTEXT.isSitting = laura.isInSittingPose() ? 1 : 0;
        float gameTime = laura.level() == null ? 0 : laura.level().getGameTime() + partialTick;
        boolean moving = limbSwingAmount > 0.08F;
        Map<String, AnimationSampler.BonePose> poses = LauraAnimator.compute(laura, DefaultAnimations.get(), ageInTicks / 20F, gameTime, moving, CONTEXT);
        state.poses = poses;
        AnimationSampler.BonePose root = poses.get("root");
        state.rootOffsetY = root == null ? 0 : root.posY;
        state.rootRotX = root == null ? 0 : root.rotX;
    }

    @Override
    public void setupAnim(LauraRenderState laura) {
        // Starts from the rest pose every frame, so animations never drift.
        super.setupAnim(laura);
        if (laura.asleep) {
            this.head.xRot = 0;
            this.head.yRot = 0;
        }
        Map<String, AnimationSampler.BonePose> poses = laura.poses;
        // The hat, jacket, sleeves and pants are children of these parts and follow them.
        apply(poses.get("head"), this.head);
        apply(poses.get("body"), this.body);
        apply(poses.get("right_arm"), this.rightArm);
        apply(poses.get("left_arm"), this.leftArm);
        apply(poses.get("right_leg"), this.rightLeg);
        apply(poses.get("left_leg"), this.leftLeg);
    }

    @Override
    public void translateToHand(HumanoidRenderState state, HumanoidArm arm, PoseStack poseStack) {
        this.root().translateAndRotate(poseStack);
        ModelPart part = this.getArm(arm);
        if (slim) {
            // Slim arms are a pixel thinner: the held item moves half a pixel towards the body.
            float offset = 0.5F * (arm == HumanoidArm.RIGHT ? 1 : -1);
            part.x += offset;
            part.translateAndRotate(poseStack);
            part.x -= offset;
        } else {
            part.translateAndRotate(poseStack);
        }
    }

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
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }
}
