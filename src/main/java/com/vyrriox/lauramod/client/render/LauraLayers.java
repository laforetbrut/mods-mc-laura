package com.vyrriox.lauramod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Extra render layers: the hay gag, the item she carries, and extra textures of custom models.
 *
 * @author vyrriox
 */
public final class LauraLayers {
    public static final ResourceLocation GAG_TEXTURE = LauraMod.id("textures/entity/hay_gag.png");

    private LauraLayers() {
    }

    /** A comic tuft of hay stuck on her mouth, with a few straws sticking out: she cannot talk. */
    public static final class Gag extends RenderLayer<LauraEntity, EntityModel<LauraEntity>> {
        private final LauraRenderer renderer;
        private final ModelPart band;

        public Gag(LauraRenderer renderer) {
            super(renderer);
            this.renderer = renderer;
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            CubeDeformation straw = new CubeDeformation(-0.35F);
            root.addOrReplaceChild("band", CubeListBuilder.create()
                    .texOffs(0, 0).addBox(-2.5F, -2.8F, -4.8F, 5.0F, 2.0F, 1.0F)
                    .texOffs(0, 4).addBox(-4.3F, -2.5F, -4.6F, 2.0F, 1.0F, 1.0F, straw)
                    .texOffs(8, 4).addBox(2.3F, -2.1F, -4.6F, 2.0F, 1.0F, 1.0F, straw)
                    .texOffs(16, 4).addBox(-0.6F, -1.4F, -4.7F, 1.0F, 2.0F, 1.0F, straw), PartPose.ZERO);
            this.band = LayerDefinition.create(mesh, 32, 16).bakeRoot().getChild("band");
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, LauraEntity laura, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!laura.isGagged() || laura.isInvisible()) {
                return;
            }
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(GAG_TEXTURE));
            int overlay = LivingEntityRenderer.getOverlayCoords(laura, 0.0F);
            if (renderer.isUsingCustomModel()) {
                poseStack.pushPose();
                if (renderer.customModel().translateToBone(poseStack, "head", "bipedhead")) {
                    poseStack.translate(0.0F, 0.1F, 0.0F);
                    band.render(poseStack, consumer, light, overlay);
                }
                poseStack.popPose();
                return;
            }
            PlayerModel<LauraEntity> model = renderer.activePlayerModel();
            band.copyFrom(model.head);
            band.render(poseStack, consumer, light, overlay);
        }
    }

    /** The backpack she wears, on her back. */
    public static final class Back extends RenderLayer<LauraEntity, EntityModel<LauraEntity>> {
        private final LauraRenderer renderer;

        public Back(LauraRenderer renderer) {
            super(renderer);
            this.renderer = renderer;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, LauraEntity laura, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            ItemStack back = laura.getBackItem();
            if (back.isEmpty() || laura.isInvisible()) {
                return;
            }
            poseStack.pushPose();
            if (renderer.isUsingCustomModel()) {
                if (!renderer.customModel().translateToBone(poseStack, "body", "bipedbody", "torso", "chest")) {
                    poseStack.popPose();
                    return;
                }
            } else {
                renderer.activePlayerModel().body.translateAndRotate(poseStack);
            }
            // Model space: Y points down and her back faces +Z.
            // 0.26 down from the neck puts the bag between the shoulder blades instead of on the hips.
            poseStack.translate(0.0F, 0.26F, 0.26F);
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.scale(0.55F, 0.55F, 0.55F);
            Minecraft.getInstance().getItemRenderer().renderStatic(back, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                    poseStack, buffer, laura.level(), laura.getId());
            poseStack.popPose();
        }
    }

    /** The item she is bringing back or eating, held in front of her. */
    public static final class Carry extends RenderLayer<LauraEntity, EntityModel<LauraEntity>> {
        private final LauraRenderer renderer;

        public Carry(LauraRenderer renderer) {
            super(renderer);
            this.renderer = renderer;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, LauraEntity laura, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            ItemStack carried = laura.getCarried();
            if (carried.isEmpty() || laura.isInvisible()) {
                return;
            }
            poseStack.pushPose();
            boolean eating = laura.getEmote() == Emote.EAT;
            if (renderer.isUsingCustomModel()) {
                poseStack.translate(0.0F, 0.65F, -0.45F);
            } else if (eating) {
                PlayerModel<LauraEntity> model = renderer.activePlayerModel();
                model.head.translateAndRotate(poseStack);
                poseStack.translate(0.0F, -0.1F, -0.32F);
            } else {
                PlayerModel<LauraEntity> model = renderer.activePlayerModel();
                model.body.translateAndRotate(poseStack);
                poseStack.translate(0.0F, 0.42F, -0.38F);
            }
            poseStack.mulPose(Axis.XP.rotationDegrees(eating ? 20 : 180));
            float scale = eating ? 0.32F : 0.45F;
            poseStack.scale(scale, scale, scale);
            Minecraft.getInstance().getItemRenderer().renderStatic(carried, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                    poseStack, buffer, laura.level(), laura.getId());
            poseStack.popPose();
        }
    }

    /** Custom models with several textures: draws the faces of the other textures. */
    public static final class ExtraTextures extends RenderLayer<LauraEntity, EntityModel<LauraEntity>> {
        private final LauraRenderer renderer;

        public ExtraTextures(LauraRenderer renderer) {
            super(renderer);
            this.renderer = renderer;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, LauraEntity laura, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!renderer.isUsingCustomModel() || laura.isInvisible()) {
                return;
            }
            CustomEntityModel custom = renderer.customModel();
            var textures = custom.model().textures();
            int overlay = LivingEntityRenderer.getOverlayCoords(laura, 0.0F);
            for (int i = 1; i < textures.size(); i++) {
                custom.renderTexture(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(textures.get(i))), light, overlay, i);
            }
        }
    }

    /** Runs a player-model layer only while the default model is used. */
    public static final class OnlyDefaultModel extends RenderLayer<LauraEntity, EntityModel<LauraEntity>> {
        private final LauraRenderer renderer;
        private final RenderLayer<LauraEntity, PlayerModel<LauraEntity>> delegate;

        public OnlyDefaultModel(LauraRenderer renderer, RenderLayer<LauraEntity, PlayerModel<LauraEntity>> delegate) {
            super(renderer);
            this.renderer = renderer;
            this.delegate = delegate;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, LauraEntity laura, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!renderer.isUsingCustomModel()) {
                delegate.render(poseStack, buffer, light, laura, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
            }
        }
    }

    /** Parent given to the vanilla humanoid layers: always the current player model. */
    public static final class PlayerParent implements RenderLayerParent<LauraEntity, PlayerModel<LauraEntity>> {
        private final LauraRenderer renderer;

        public PlayerParent(LauraRenderer renderer) {
            this.renderer = renderer;
        }

        @Override
        public PlayerModel<LauraEntity> getModel() {
            return renderer.activePlayerModel();
        }

        @Override
        public ResourceLocation getTextureLocation(LauraEntity laura) {
            return renderer.getTextureLocation(laura);
        }
    }
}
