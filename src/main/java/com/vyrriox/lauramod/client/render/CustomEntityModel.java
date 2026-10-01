package com.vyrriox.lauramod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vyrriox.lauramod.client.anim.DefaultAnimations;
import com.vyrriox.lauramod.client.anim.LauraAnimator;
import com.vyrriox.lauramod.client.model.ClientModels;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.model.AnimationSampler;
import com.vyrriox.lauramod.model.HumanoidParts;
import com.vyrriox.lauramod.model.ModelData;
import com.vyrriox.lauramod.model.Molang;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.HumanoidArm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapts a custom Blockbench model to the vanilla entity renderer.
 *
 * @author vyrriox
 */
public class CustomEntityModel extends EntityModel<LauraEntity> implements ArmedModel {
    private final Molang.Context context = new Molang.Context();
    private ClientModels.Loaded model;
    private Map<String, AnimationSampler.BonePose> poses = Collections.emptyMap();
    private float partialTick;

    public void setModel(ClientModels.Loaded model, float partialTick) {
        this.model = model;
        this.partialTick = partialTick;
    }

    public ClientModels.Loaded model() {
        return model;
    }

    @Override
    public void setupAnim(LauraEntity laura, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (model == null) {
            return;
        }
        context.lifeTime = ageInTicks / 20F;
        context.groundSpeed = limbSwingAmount * 4;
        context.distanceMoved = limbSwing;
        context.health = laura.getHealth();
        context.maxHealth = laura.getMaxHealth();
        context.headYaw = netHeadYaw;
        context.headPitch = headPitch;
        context.isOnGround = laura.onGround() ? 1 : 0;
        context.isInWater = laura.isInWater() ? 1 : 0;
        context.isSitting = laura.isInSittingPose() ? 1 : 0;
        context.isSleeping = laura.isAsleep() ? 1 : 0;
        float gameTime = laura.level().getGameTime() + partialTick;
        ModelData data = model.data();
        // A model without animations borrows the default ones, played on the bones found by name.
        boolean borrowed = data.animations.isEmpty();
        Map<String, ModelData.Animation> set = borrowed ? DefaultAnimations.get() : data.animations;
        poses = LauraAnimator.compute(laura, set, ageInTicks / 20F, gameTime, limbSwingAmount > 0.08F, context);
        if (borrowed) {
            poses = onModelBones(poses, data);
        }
        // Without its own walk animation, arms and legs swing like a player's.
        if (!data.animations.containsKey("walk") && !laura.isInSittingPose() && !laura.isAsleep()) {
            float swing = (float) Math.toDegrees(Math.cos(limbSwing * 0.6662F) * limbSwingAmount);
            rotateX(data.part(HumanoidParts.RIGHT_ARM), -swing);
            rotateX(data.part(HumanoidParts.LEFT_ARM), swing);
            rotateX(data.part(HumanoidParts.RIGHT_LEG), swing * 1.4F);
            rotateX(data.part(HumanoidParts.LEFT_LEG), -swing * 1.4F);
        }
        ModelData.Bone head = data.findBone("head", "bipedhead", "neck");
        if (head == null) {
            head = data.part(HumanoidParts.HEAD);
        }
        if (head != null && !laura.isAsleep()) {
            AnimationSampler.BonePose pose = pose(head);
            pose.rotY += netHeadYaw;
            pose.rotX += headPitch;
        }
    }

    private AnimationSampler.BonePose pose(ModelData.Bone bone) {
        return poses.computeIfAbsent(bone.name.toLowerCase(Locale.ROOT), k -> new AnimationSampler.BonePose());
    }

    private void rotateX(ModelData.Bone bone, float degrees) {
        if (bone != null) {
            pose(bone).rotX += degrees;
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

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        if (model == null) {
            return;
        }
        poseStack.pushPose();
        // Undo the flip of the living entity renderer: Blockbench models are Y up.
        poseStack.translate(0.0F, 1.501F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        CustomModelRenderer.render(model.data(), poses, poseStack, buffer, packedLight, packedOverlay, color, 0);
        poseStack.popPose();
    }

    /** Renders the quads of an extra texture (models with several textures). */
    public void renderTexture(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int textureIndex) {
        if (model == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.501F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        CustomModelRenderer.render(model.data(), poses, poseStack, buffer, packedLight, packedOverlay, -1, textureIndex);
        poseStack.popPose();
    }

    /** Moves the pose stack to a bone, following its parents and their animations. */
    public boolean translateToBone(PoseStack poseStack, String... names) {
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
        return translateToBone(poseStack, bone);
    }

    private boolean translateToBone(PoseStack poseStack, ModelData.Bone bone) {
        if (model == null || bone == null) {
            return false;
        }
        List<ModelData.Bone> chain = new ArrayList<>();
        for (ModelData.Bone b = bone; b != null; b = b.parent) {
            chain.add(0, b);
        }
        poseStack.translate(0.0F, 1.501F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        for (ModelData.Bone b : chain) {
            CustomModelRenderer.transform(b, poses.get(b.name.toLowerCase(Locale.ROOT)), poseStack);
        }
        poseStack.translate(bone.pivotX / 16F, bone.pivotY / 16F, bone.pivotZ / 16F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        return true;
    }

    @Override
    public void translateToHand(HumanoidArm side, PoseStack poseStack) {
        boolean right = side == HumanoidArm.RIGHT;
        // A dedicated hand or item bone first, otherwise the arm found by HumanoidParts.
        boolean found = right
                ? translateToBone(poseStack, "right_hand", "rightitem", "right_item", "hand_right")
                : translateToBone(poseStack, "left_hand", "leftitem", "left_item", "hand_left");
        if (!found && model != null) {
            found = translateToBone(poseStack, model.data().part(right ? HumanoidParts.RIGHT_ARM : HumanoidParts.LEFT_ARM));
        }
        if (found) {
            poseStack.translate(0.0F, -0.1F, 0.0F);
        }
    }

    public float heightInBlocks() {
        return model == null ? 1.8F : model.data().height / 16F;
    }
}
