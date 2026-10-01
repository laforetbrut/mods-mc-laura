package com.vyrriox.lauramod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.vyrriox.lauramod.client.anim.DefaultAnimations;
import com.vyrriox.lauramod.client.anim.LauraAnimator;
import com.vyrriox.lauramod.client.model.ClientModels;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.model.AnimationSampler;
import com.vyrriox.lauramod.model.HumanoidParts;
import com.vyrriox.lauramod.model.ModelData;
import com.vyrriox.lauramod.model.Molang;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.HumanoidArm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapts a custom Blockbench model to the vanilla entity renderer. A vanilla model can only draw
 * its own model parts, so the cubes are submitted as custom geometry; the model and its poses of
 * the frame travel in the {@link LauraRenderState}.
 *
 * @author vyrriox
 */
public class CustomEntityModel implements ArmedModel<LauraRenderState> {
    private final Molang.Context context = new Molang.Context();

    /** Computes her animation for this frame into the render state, while the entity can still be read. */
    public void extract(LauraEntity laura, LauraRenderState state, float partialTick) {
        ClientModels.Loaded model = state.custom;
        if (model == null) {
            return;
        }
        float ageInTicks = state.ageInTicks;
        float limbSwing = state.walkAnimationPos;
        float limbSwingAmount = state.walkAnimationSpeed;
        context.lifeTime = ageInTicks / 20F;
        context.groundSpeed = limbSwingAmount * 4;
        context.distanceMoved = limbSwing;
        context.health = laura.getHealth();
        context.maxHealth = laura.getMaxHealth();
        context.headYaw = state.yRot;
        context.headPitch = state.xRot;
        context.isOnGround = laura.onGround() ? 1 : 0;
        context.isInWater = laura.isInWater() ? 1 : 0;
        context.isSitting = laura.isInSittingPose() ? 1 : 0;
        context.isSleeping = laura.isAsleep() ? 1 : 0;
        float gameTime = laura.level().getGameTime() + partialTick;
        ModelData data = model.data();
        // A model without animations borrows the default ones, played on the bones found by name.
        boolean borrowed = data.animations.isEmpty();
        Map<String, ModelData.Animation> set = borrowed ? DefaultAnimations.get() : data.animations;
        Map<String, AnimationSampler.BonePose> poses = LauraAnimator.compute(laura, set, ageInTicks / 20F, gameTime, limbSwingAmount > 0.08F, context);
        if (borrowed) {
            poses = onModelBones(poses, data);
        }
        // Without its own walk animation, arms and legs swing like a player's.
        if (!data.animations.containsKey("walk") && !laura.isInSittingPose() && !laura.isAsleep()) {
            float swing = (float) Math.toDegrees(Math.cos(limbSwing * 0.6662F) * limbSwingAmount);
            rotateX(poses, data.part(HumanoidParts.RIGHT_ARM), -swing);
            rotateX(poses, data.part(HumanoidParts.LEFT_ARM), swing);
            rotateX(poses, data.part(HumanoidParts.RIGHT_LEG), swing * 1.4F);
            rotateX(poses, data.part(HumanoidParts.LEFT_LEG), -swing * 1.4F);
        }
        ModelData.Bone head = data.findBone("head", "bipedhead", "neck");
        if (head == null) {
            head = data.part(HumanoidParts.HEAD);
        }
        state.poses = poses;
        // Her look is added when she is drawn: a screen preview turns her head after this.
        state.headBone = head != null && !laura.isAsleep() ? head.name.toLowerCase(Locale.ROOT) : null;
        state.headApplied = false;
    }

    /** Turns the head bone to where she looks. Called once, right before she is drawn. */
    public void applyHeadLook(LauraRenderState state) {
        if (state.headBone == null || state.headApplied) {
            return;
        }
        state.headApplied = true;
        AnimationSampler.BonePose pose = state.poses.computeIfAbsent(state.headBone, k -> new AnimationSampler.BonePose());
        pose.rotY += state.yRot;
        pose.rotX += state.xRot;
    }

    private static AnimationSampler.BonePose pose(Map<String, AnimationSampler.BonePose> poses, ModelData.Bone bone) {
        return poses.computeIfAbsent(bone.name.toLowerCase(Locale.ROOT), k -> new AnimationSampler.BonePose());
    }

    private static void rotateX(Map<String, AnimationSampler.BonePose> poses, ModelData.Bone bone, float degrees) {
        if (bone != null) {
            pose(poses, bone).rotX += degrees;
        }
    }

    /**
     * Moves poses computed for the default model ("head", "right_arm"...) onto the matching bones of
     * a custom model. Rotations mean the same in both; positions of the default model point Y down.
     * "root" goes to the model's root bone, or to every top level bone when it has none.
     */
    private static Map<String, AnimationSampler.BonePose> onModelBones(Map<String, AnimationSampler.BonePose> byPart, ModelData data) {
        Map<String, AnimationSampler.BonePose> out = new HashMap<>();
        for (Map.Entry<String, AnimationSampler.BonePose> e : byPart.entrySet()) {
            AnimationSampler.BonePose p = e.getValue();
            p.posY = -p.posY;
            ModelData.Bone target = data.part(e.getKey());
            if (target != null) {
                add(out, target, p);
            } else if (HumanoidParts.ROOT.equals(e.getKey())) {
                for (ModelData.Bone root : data.roots) {
                    add(out, root, p);
                }
            }
        }
        return out;
    }

    private static void add(Map<String, AnimationSampler.BonePose> out, ModelData.Bone bone, AnimationSampler.BonePose p) {
        AnimationSampler.BonePose t = out.computeIfAbsent(bone.name.toLowerCase(Locale.ROOT), k -> new AnimationSampler.BonePose());
        t.rotX += p.rotX;
        t.rotY += p.rotY;
        t.rotZ += p.rotZ;
        t.posX += p.posX;
        t.posY += p.posY;
        t.posZ += p.posZ;
        t.scaleX *= p.scaleX;
        t.scaleY *= p.scaleY;
        t.scaleZ *= p.scaleZ;
    }

    /** Submits the quads of one texture of the model (0 is the main one). */
    public void submit(LauraRenderState state, PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType,
                       int packedLight, int packedOverlay, int color, int textureIndex) {
        ClientModels.Loaded model = state.custom;
        if (model == null) {
            return;
        }
        ModelData data = model.data();
        Map<String, AnimationSampler.BonePose> poses = state.poses;
        poseStack.pushPose();
        // Undo the flip of the living entity renderer: Blockbench models are Y up.
        poseStack.translate(0.0F, 1.501F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            PoseStack stack = new PoseStack();
            stack.last().set(pose);
            CustomModelRenderer.render(data, poses, stack, buffer, packedLight, packedOverlay, color, textureIndex);
        });
        poseStack.popPose();
    }

    /** Moves the pose stack to a bone, following its parents and their animations. */
    public boolean translateToBone(LauraRenderState state, PoseStack poseStack, String... names) {
        ClientModels.Loaded model = state.custom;
        if (model == null) {
            return false;
        }
        ModelData.Bone bone = model.data().findBone(names);
        // Not found under its English names: the same part under another name ("tete", "corps"...).
        for (int i = 0; bone == null && i < names.length; i++) {
            String part = HumanoidParts.partOf(names[i]);
            if (part != null) {
                bone = model.data().part(part);
            }
        }
        return translateToBone(state, poseStack, bone);
    }

    private boolean translateToBone(LauraRenderState state, PoseStack poseStack, ModelData.Bone bone) {
        if (state.custom == null || bone == null) {
            return false;
        }
        List<ModelData.Bone> chain = new ArrayList<>();
        for (ModelData.Bone b = bone; b != null; b = b.parent) {
            chain.add(0, b);
        }
        poseStack.translate(0.0F, 1.501F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        for (ModelData.Bone b : chain) {
            CustomModelRenderer.transform(b, state.poses.get(b.name.toLowerCase(Locale.ROOT)), poseStack);
        }
        poseStack.translate(bone.pivotX / 16F, bone.pivotY / 16F, bone.pivotZ / 16F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        return true;
    }

    @Override
    public void translateToHand(LauraRenderState state, HumanoidArm side, PoseStack poseStack) {
        boolean right = side == HumanoidArm.RIGHT;
        // A dedicated hand or item bone first, otherwise the arm found by HumanoidParts.
        boolean found = right
                ? translateToBone(state, poseStack, "right_hand", "rightitem", "right_item", "hand_right")
                : translateToBone(state, poseStack, "left_hand", "leftitem", "left_item", "hand_left");
        if (!found && state.custom != null) {
            found = translateToBone(state, poseStack, state.custom.data().part(right ? HumanoidParts.RIGHT_ARM : HumanoidParts.LEFT_ARM));
        }
        if (found) {
            poseStack.translate(0.0F, -0.1F, 0.0F);
        }
    }

    public float heightInBlocks(LauraRenderState state) {
        return state.custom == null ? 1.8F : state.custom.data().height / 16F;
    }
}
