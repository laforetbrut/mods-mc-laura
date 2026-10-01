package com.vyrriox.lauramod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.vyrriox.lauramod.LauraMod;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Extra render layers: the hay gag, the item she carries, and the body and extra textures of
 * custom models.
 *
 * @author vyrriox
 */
public final class LauraLayers {
    public static final Identifier GAG_TEXTURE = LauraMod.id("textures/entity/hay_gag.png");

    private LauraLayers() {
    }

    /** A comic tuft of hay stuck on her mouth, with a few straws sticking out: she cannot talk. */
    public static final class Gag extends RenderLayer<LauraRenderState, LauraPlayerModel> {
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
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, LauraRenderState laura, float yRot, float xRot) {
            if (!laura.gagged || laura.isInvisible) {
                return;
            }
            RenderType type = RenderTypes.entityCutout(GAG_TEXTURE);
            int overlay = LivingEntityRenderer.getOverlayCoords(laura, 0.0F);
            poseStack.pushPose();
            if (laura.custom != null) {
                if (renderer.customModel().translateToBone(laura, poseStack, "head", "bipedhead")) {
                    poseStack.translate(0.0F, 0.1F, 0.0F);
                    collector.submitModelPart(band, poseStack, type, light, overlay, null);
                }
                poseStack.popPose();
                return;
            }
            // The band is drawn later, with the pose kept here: it stays at rest and the stack follows her head.
            LauraPlayerModel model = renderer.activePlayerModel();
            model.root().translateAndRotate(poseStack);
            model.head.translateAndRotate(poseStack);
            collector.submitModelPart(band, poseStack, type, light, overlay, null);
            poseStack.popPose();
        }
    }

    /** The backpack she wears, on her back. */
    public static final class Back extends RenderLayer<LauraRenderState, LauraPlayerModel> {
        private final LauraRenderer renderer;

        public Back(LauraRenderer renderer) {
            super(renderer);
            this.renderer = renderer;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, LauraRenderState laura, float yRot, float xRot) {
            if (laura.backItem.isEmpty() || laura.isInvisible) {
                return;
            }
            poseStack.pushPose();
            if (laura.custom != null) {
                if (!renderer.customModel().translateToBone(laura, poseStack, "body", "bipedbody", "torso", "chest")) {
                    poseStack.popPose();
                    return;
                }
            } else {
                LauraPlayerModel model = renderer.activePlayerModel();
                model.root().translateAndRotate(poseStack);
                model.body.translateAndRotate(poseStack);
            }
            // Model space: Y points down and her back faces +Z.
            // 0.26 down from the neck puts the bag between the shoulder blades instead of on the hips.
            poseStack.translate(0.0F, 0.26F, 0.26F);
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.scale(0.55F, 0.55F, 0.55F);
            laura.backItem.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, laura.outlineColor);
            poseStack.popPose();
        }
    }

    /** The item she is bringing back or eating, held in front of her. */
    public static final class Carry extends RenderLayer<LauraRenderState, LauraPlayerModel> {
        private final LauraRenderer renderer;

        public Carry(LauraRenderer renderer) {
            super(renderer);
            this.renderer = renderer;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, LauraRenderState laura, float yRot, float xRot) {
            if (laura.carriedItem.isEmpty() || laura.isInvisible) {
                return;
            }
            poseStack.pushPose();
            boolean eating = laura.eating;
            if (laura.custom != null) {
                poseStack.translate(0.0F, 0.65F, -0.45F);
            } else if (eating) {
                LauraPlayerModel model = renderer.activePlayerModel();
                model.root().translateAndRotate(poseStack);
                model.head.translateAndRotate(poseStack);
                poseStack.translate(0.0F, -0.1F, -0.32F);
            } else {
                LauraPlayerModel model = renderer.activePlayerModel();
                model.root().translateAndRotate(poseStack);
                model.body.translateAndRotate(poseStack);
                poseStack.translate(0.0F, 0.42F, -0.38F);
            }
            poseStack.mulPose(Axis.XP.rotationDegrees(eating ? 20 : 180));
            float scale = eating ? 0.32F : 0.45F;
            poseStack.scale(scale, scale, scale);
            laura.carriedItem.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, laura.outlineColor);
            poseStack.popPose();
        }
    }

    /**
     * The body of a custom model (its first texture). It is a layer because the vanilla renderer only
     * draws its own model itself, and layers get the same pose as that model.
     */
    public static final class CustomBody extends RenderLayer<LauraRenderState, LauraPlayerModel> {
        private final LauraRenderer renderer;

        public CustomBody(LauraRenderer renderer) {
            super(renderer);
            this.renderer = renderer;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, LauraRenderState laura, float yRot, float xRot) {
            if (laura.custom == null) {
                return;
            }
            CustomEntityModel custom = renderer.customModel();
            int overlay = LivingEntityRenderer.getOverlayCoords(laura, 0.0F);
            // Same rules as the vanilla body: see-through for those who can see invisible friends.
            boolean visible = !laura.isInvisible;
            boolean seeThrough = !visible && !laura.isInvisibleToPlayer;
            if (seeThrough) {
                custom.submit(laura, poseStack, collector, RenderTypes.entityTranslucentCullItemTarget(laura.texture), light, overlay, 0x26FFFFFF, 0);
            } else if (visible) {
                custom.submit(laura, poseStack, collector, RenderTypes.entityCutout(laura.texture), light, overlay, -1, 0);
            }
            if (laura.appearsGlowing()) {
                custom.submit(laura, poseStack, collector, RenderTypes.outline(laura.texture), light, overlay, laura.outlineColor, 0);
            }
        }
    }

    /** Custom models with several textures: draws the faces of the other textures. */
    public static final class ExtraTextures extends RenderLayer<LauraRenderState, LauraPlayerModel> {
        private final LauraRenderer renderer;

        public ExtraTextures(LauraRenderer renderer) {
            super(renderer);
            this.renderer = renderer;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, LauraRenderState laura, float yRot, float xRot) {
            if (laura.custom == null || laura.isInvisible) {
                return;
            }
            CustomEntityModel custom = renderer.customModel();
            var textures = laura.custom.textures();
            int overlay = LivingEntityRenderer.getOverlayCoords(laura, 0.0F);
            for (int i = 1; i < textures.size(); i++) {
                custom.submit(laura, poseStack, collector, RenderTypes.entityCutout(textures.get(i)), light, overlay, -1, i);
            }
        }
    }

    /** Runs a player-model layer only while the default model is used. */
    public static final class OnlyDefaultModel extends RenderLayer<LauraRenderState, LauraPlayerModel> {
        private final RenderLayer<LauraRenderState, LauraPlayerModel> delegate;

        public OnlyDefaultModel(LauraRenderer renderer, RenderLayer<LauraRenderState, LauraPlayerModel> delegate) {
            super(renderer);
            this.delegate = delegate;
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, LauraRenderState laura, float yRot, float xRot) {
            if (laura.custom == null) {
                delegate.submit(poseStack, collector, light, laura, yRot, xRot);
            }
        }
    }

    /** Parent given to the vanilla humanoid layers: always the current player model. */
    public static final class PlayerParent implements RenderLayerParent<LauraRenderState, LauraPlayerModel> {
        private final LauraRenderer renderer;

        public PlayerParent(LauraRenderer renderer) {
            this.renderer = renderer;
        }

        @Override
        public LauraPlayerModel getModel() {
            return renderer.activePlayerModel();
        }
    }
}
