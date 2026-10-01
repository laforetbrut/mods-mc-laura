package com.vyrriox.lauramod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.vyrriox.lauramod.model.AnimationSampler;
import com.vyrriox.lauramod.model.ModelData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Map;

/**
 * Draws a baked Blockbench model with the same transform order as GeckoLib: for every bone,
 * animation offset, pivot, rotation Z then Y then X, scale, back from the pivot; then its cubes.
 *
 * @author vyrriox
 */
public final class CustomModelRenderer {
    private CustomModelRenderer() {
    }

    public static void render(ModelData model, Map<String, AnimationSampler.BonePose> poses, PoseStack poseStack, VertexConsumer consumer,
                              int light, int overlay, int color, int textureIndex) {
        for (ModelData.Bone bone : model.roots) {
            renderBone(bone, poses, poseStack, consumer, light, overlay, color, textureIndex);
        }
    }

    /** Applies a bone's transform (with its animation) to the pose stack. */
    public static void transform(ModelData.Bone bone, AnimationSampler.BonePose pose, PoseStack poseStack) {
        if (pose != null) {
            poseStack.translate(-pose.posX / 16F, pose.posY / 16F, pose.posZ / 16F);
        }
        poseStack.translate(bone.pivotX / 16F, bone.pivotY / 16F, bone.pivotZ / 16F);
        float rx = bone.rotX;
        float ry = bone.rotY;
        float rz = bone.rotZ;
        if (pose != null) {
            rx += (float) Math.toRadians(-pose.rotX);
            ry += (float) Math.toRadians(-pose.rotY);
            rz += (float) Math.toRadians(pose.rotZ);
        }
        if (rz != 0) {
            poseStack.mulPose(Axis.ZP.rotation(rz));
        }
        if (ry != 0) {
            poseStack.mulPose(Axis.YP.rotation(ry));
        }
        if (rx != 0) {
            poseStack.mulPose(Axis.XP.rotation(rx));
        }
        if (pose != null && (pose.scaleX != 1 || pose.scaleY != 1 || pose.scaleZ != 1)) {
            poseStack.scale(pose.scaleX, pose.scaleY, pose.scaleZ);
        }
        poseStack.translate(-bone.pivotX / 16F, -bone.pivotY / 16F, -bone.pivotZ / 16F);
    }

    private static void renderBone(ModelData.Bone bone, Map<String, AnimationSampler.BonePose> poses, PoseStack poseStack, VertexConsumer consumer,
                                   int light, int overlay, int color, int textureIndex) {
        poseStack.pushPose();
        AnimationSampler.BonePose pose = poses.get(bone.name.toLowerCase(java.util.Locale.ROOT));
        transform(bone, pose, poseStack);
        if (pose == null || pose.scaleX != 0 || pose.scaleY != 0 || pose.scaleZ != 0) {
            for (ModelData.Cube cube : bone.cubes) {
                renderCube(cube, poseStack, consumer, light, overlay, color, textureIndex);
            }
        }
        for (ModelData.Bone child : bone.children) {
            renderBone(child, poses, poseStack, consumer, light, overlay, color, textureIndex);
        }
        poseStack.popPose();
    }

    private static void renderCube(ModelData.Cube cube, PoseStack poseStack, VertexConsumer consumer, int light, int overlay, int color, int textureIndex) {
        boolean rotated = cube.rotX != 0 || cube.rotY != 0 || cube.rotZ != 0;
        if (rotated) {
            poseStack.pushPose();
            poseStack.translate(cube.pivotX / 16F, cube.pivotY / 16F, cube.pivotZ / 16F);
            if (cube.rotZ != 0) {
                poseStack.mulPose(Axis.ZP.rotation(cube.rotZ));
            }
            if (cube.rotY != 0) {
                poseStack.mulPose(Axis.YP.rotation(cube.rotY));
            }
            if (cube.rotX != 0) {
                poseStack.mulPose(Axis.XP.rotation(cube.rotX));
            }
            poseStack.translate(-cube.pivotX / 16F, -cube.pivotY / 16F, -cube.pivotZ / 16F);
        }
        PoseStack.Pose last = poseStack.last();
        Matrix4f matrix = last.pose();
        Matrix3f normalMatrix = last.normal();
        Vector4f pos = new Vector4f();
        Vector3f normal = new Vector3f();
        for (ModelData.Quad quad : cube.quads) {
            if (quad.texture() != textureIndex) {
                continue;
            }
            normal.set(quad.nx(), quad.ny(), quad.nz());
            normalMatrix.transform(normal);
            float[] p = quad.positions();
            float[] uv = quad.uvs();
            for (int i = 0; i < 4; i++) {
                pos.set(p[i * 3] / 16F, p[i * 3 + 1] / 16F, p[i * 3 + 2] / 16F, 1F);
                matrix.transform(pos);
                consumer.addVertex(pos.x(), pos.y(), pos.z(), color, uv[i * 2], uv[i * 2 + 1], overlay, light, normal.x(), normal.y(), normal.z());
            }
        }
        if (rotated) {
            poseStack.popPose();
        }
    }
}
