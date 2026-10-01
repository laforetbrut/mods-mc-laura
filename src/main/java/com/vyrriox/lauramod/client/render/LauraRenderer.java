package com.vyrriox.lauramod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.client.ClientState;
import com.vyrriox.lauramod.client.model.ClientModels;
import com.vyrriox.lauramod.client.skin.SkinTextures;
import com.vyrriox.lauramod.config.LauraClientConfig;
import com.vyrriox.lauramod.desire.DesireTable;
import com.vyrriox.lauramod.desire.DesireType;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.util.ItemSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Renders Laura: a player-like model with her skin, or a custom Blockbench model, plus her
 * equipment, the hay gag, the item she carries, and her speech and thought bubbles.
 *
 * @author vyrriox
 */
public class LauraRenderer extends LivingEntityRenderer<LauraEntity, EntityModel<LauraEntity>> {
    private static final ResourceLocation BUBBLE = LauraMod.id("textures/entity/thought_bubble.png");
    private static final int SPEECH_TEXT = 0xFF5B2A4D;
    private static final int SPEECH_FILL = 0xF2FFF4F9;
    private static final int SPEECH_BORDER = 0xFFF77FB2;
    /** Wrap widths tried in order: long lines get a wider bubble before anything is cut. */
    private static final int[] SPEECH_WIDTHS = {150, 190, 230};
    private static final int SPEECH_MAX_LINES = 4;

    /** Set while a screen draws her as a preview: no name tag or bubbles over the GUI. */
    private static boolean guiPreview;

    private final LauraPlayerModel wide;
    private final LauraPlayerModel slim;
    private final CustomEntityModel custom = new CustomEntityModel();
    private LauraPlayerModel activePlayer;
    private boolean usingCustom;

    public LauraRenderer(EntityRendererProvider.Context context) {
        super(context, new LauraPlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wide = (LauraPlayerModel) this.model;
        this.slim = new LauraPlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.activePlayer = slim;
        LauraLayers.PlayerParent parent = new LauraLayers.PlayerParent(this);
        this.addLayer(new LauraLayers.OnlyDefaultModel(this, new HumanoidArmorLayer<>(parent,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager())));
        this.addLayer(new LauraLayers.OnlyDefaultModel(this, new ItemInHandLayer<>(parent, context.getItemInHandRenderer())));
        this.addLayer(new LauraLayers.OnlyDefaultModel(this, new CustomHeadLayer<>(parent, context.getModelSet(), context.getItemInHandRenderer())));
        this.addLayer(new LauraLayers.ExtraTextures(this));
        this.addLayer(new LauraLayers.Gag(this));
        this.addLayer(new LauraLayers.Carry(this));
        this.addLayer(new LauraLayers.Back(this));
    }

    /** Draws something with the preview flag on (the flag is always cleared afterwards). */
    public static void preview(Runnable draw) {
        guiPreview = true;
        try {
            draw.run();
        } finally {
            guiPreview = false;
        }
    }

    public boolean isUsingCustomModel() {
        return usingCustom;
    }

    @Override
    protected boolean shouldShowName(LauraEntity laura) {
        return !guiPreview && super.shouldShowName(laura);
    }

    public CustomEntityModel customModel() {
        return custom;
    }

    public PlayerModel<LauraEntity> activePlayerModel() {
        return activePlayer;
    }

    @Override
    public void render(LauraEntity laura, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
        ClientModels.Loaded loaded = LauraClientConfig.customModels.get() ? ClientModels.get(laura.getModelName()) : null;
        usingCustom = loaded != null;
        if (usingCustom) {
            custom.setModel(loaded, partialTick);
            this.model = custom;
        } else {
            activePlayer = laura.isSlim() ? slim : wide;
            activePlayer.setPartialTick(partialTick);
            this.model = activePlayer;
        }
        super.render(laura, entityYaw, partialTick, poseStack, buffer, light);
        if (!guiPreview) {
            renderBubbles(laura, partialTick, poseStack, buffer, light);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(LauraEntity laura) {
        if (usingCustom && custom.model() != null) {
            return custom.model().textures().get(0);
        }
        return SkinTextures.get(laura.getSkin());
    }

    @Override
    protected void setupRotations(LauraEntity laura, PoseStack poseStack, float bob, float yBodyRot, float partialTick) {
        super.setupRotations(laura, poseStack, bob, yBodyRot, partialTick);
        if (!usingCustom && laura.isInSittingPose() && !laura.isPassenger() && !laura.isAsleep()) {
            poseStack.translate(0.0F, -0.62F, 0.0F);
        }
        if (!usingCustom && activePlayer.rootOffsetY != 0) {
            poseStack.translate(0.0F, -activePlayer.rootOffsetY / 16F, 0.0F);
        }
        if (!usingCustom && activePlayer.rootRotX != 0) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(activePlayer.rootRotX));
        }
    }

    // ------------------------------------------------------------------ bubbles

    private void renderBubbles(LauraEntity laura, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
        double distance = this.entityRenderDispatcher.distanceToSqr(laura);
        if (distance > 32 * 32 || laura.isInvisible()) {
            return;
        }
        float top = usingCustom ? Math.max(1.9F, custom.heightInBlocks() + 0.1F) : laura.getBbHeight() + 0.1F;
        if (laura.isAsleep()) {
            top = 0.8F;
        }
        boolean nameShown = this.shouldShowName(laura);
        float y = top + (nameShown ? 0.55F : 0.25F);
        ClientState.Bubble bubble = LauraClientConfig.speechBubbles.get() ? ClientState.bubble(laura.getId()) : null;
        if (bubble != null) {
            y = renderSpeech(laura, bubble.text(), y, poseStack, buffer);
        }
        if (LauraClientConfig.thoughtBubbles.get()) {
            ItemStack icon = thoughtIcon(laura.getThought());
            if (!icon.isEmpty()) {
                renderThought(laura, icon, y + 0.1F, partialTick, poseStack, buffer, light);
            }
        }
    }

    /**
     * One rounded bubble behind all the lines, drawn like vanilla text displays: the background in
     * its own pass and the text with a polygon offset, so they never fight for the same depth (the
     * old per-line background flickered and turned the text unreadable from a few blocks away). Both
     * are full bright: she stays readable at night and in caves.
     */
    private float renderSpeech(LauraEntity laura, Component text, float y, PoseStack poseStack, MultiBufferSource buffer) {
        Font font = this.getFont();
        List<FormattedCharSequence> lines = List.of();
        for (int width : SPEECH_WIDTHS) {
            lines = font.split(text, width);
            if (lines.size() <= SPEECH_MAX_LINES) {
                break;
            }
        }
        if (lines.size() > SPEECH_MAX_LINES) {
            List<FormattedCharSequence> cut = new java.util.ArrayList<>(lines.subList(0, SPEECH_MAX_LINES));
            cut.set(SPEECH_MAX_LINES - 1, FormattedCharSequence.composite(cut.get(SPEECH_MAX_LINES - 1),
                    FormattedCharSequence.forward("...", net.minecraft.network.chat.Style.EMPTY)));
            lines = cut;
        }
        int textWidth = 0;
        for (FormattedCharSequence line : lines) {
            textWidth = Math.max(textWidth, font.width(line));
        }
        float x1 = textWidth / 2F + 4;
        float x0 = -x1;
        float boxHeight = lines.size() * 10 + 6;
        float tail = 3;
        float scale = 0.025F;

        poseStack.pushPose();
        poseStack.translate(0.0F, y + (boxHeight + tail) * scale, 0.0F);
        faceCamera(poseStack);
        poseStack.scale(scale, -scale, scale);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer bg = buffer.getBuffer(RenderType.textBackground());
        // Fill, then a 1 px border made of strips around it (never on top of it), then the tail.
        rect(bg, matrix, x0 + 1, 1, x1 - 1, boxHeight - 1, SPEECH_FILL);
        rect(bg, matrix, x0 + 1, 0, x1 - 1, 1, SPEECH_BORDER);
        rect(bg, matrix, x0 + 1, boxHeight - 1, x1 - 1, boxHeight, SPEECH_BORDER);
        rect(bg, matrix, x0, 1, x0 + 1, boxHeight - 1, SPEECH_BORDER);
        rect(bg, matrix, x1 - 1, 1, x1, boxHeight - 1, SPEECH_BORDER);
        for (int i = 0; i < tail; i++) {
            float half = tail - i;
            rect(bg, matrix, -half, boxHeight + i, half, boxHeight + i + 1, SPEECH_BORDER);
        }
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            font.drawInBatch(line, -font.width(line) / 2F, 4 + i * 10, SPEECH_TEXT, false, matrix, buffer,
                    Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
        }
        poseStack.popPose();
        return y + (boxHeight + tail) * scale + 0.1F;
    }

    /**
     * Turns the pose stack towards the camera, in the frame later versions use (X to the right of
     * the viewer, Z towards the viewer): the camera orientation of Minecraft 1.20.1 looks the other way.
     */
    private void faceCamera(PoseStack poseStack) {
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
    }

    private static void rect(VertexConsumer consumer, Matrix4f matrix, float x0, float y0, float x1, float y1, int color) {
        consumer.vertex(matrix, x0, y0, 0).color(color).uv2(LightTexture.FULL_BRIGHT).endVertex();
        consumer.vertex(matrix, x0, y1, 0).color(color).uv2(LightTexture.FULL_BRIGHT).endVertex();
        consumer.vertex(matrix, x1, y1, 0).color(color).uv2(LightTexture.FULL_BRIGHT).endVertex();
        consumer.vertex(matrix, x1, y0, 0).color(color).uv2(LightTexture.FULL_BRIGHT).endVertex();
    }

    private void renderThought(LauraEntity laura, ItemStack icon, float y, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
        poseStack.pushPose();
        float bob = (float) Math.sin((laura.tickCount + partialTick) * 0.1F) * 0.05F;
        poseStack.translate(0.0F, y + 0.3F + bob, 0.0F);
        faceCamera(poseStack);
        poseStack.pushPose();
        poseStack.scale(0.6F, 0.6F, 0.6F);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(BUBBLE));
        int overlay = OverlayTexture.NO_OVERLAY;
        quad(consumer, matrix, -0.5F, -0.5F, 0.5F, 0.5F, light, overlay);
        poseStack.popPose();
        poseStack.translate(0.0F, 0.02F, -0.01F);
        poseStack.scale(0.3F, 0.3F, 0.3F);
        Minecraft.getInstance().getItemRenderer().renderStatic(icon, ItemDisplayContext.GUI, light, OverlayTexture.NO_OVERLAY, poseStack, buffer, laura.level(), laura.getId());
        poseStack.popPose();
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, float x0, float y0, float x1, float y1, int light, int overlay) {
        consumer.vertex(matrix, x0, y0, 0).color(-1).uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
        consumer.vertex(matrix, x1, y0, 0).color(-1).uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
        consumer.vertex(matrix, x1, y1, 0).color(-1).uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
        consumer.vertex(matrix, x0, y1, 0).color(-1).uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, 0, -1).endVertex();
    }

    /** Icon of the thought synced by the server ("item:minecraft:cake", "place:beach", "need:hunger"...). */
    public static ItemStack thoughtIcon(String thought) {
        if (thought == null || thought.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int colon = thought.indexOf(':');
        if (colon < 0) {
            return ItemStack.EMPTY;
        }
        String kind = thought.substring(0, colon);
        String target = thought.substring(colon + 1);
        return switch (kind) {
            case "item" -> ItemSpec.parse(target).map(ItemSpec::icon).orElse(ItemStack.EMPTY);
            case "place" -> {
                DesireTable.PlaceDesire place = DesireTable.place(target);
                yield new ItemStack(place != null ? place.icon() : Items.FILLED_MAP);
            }
            case "activity" -> {
                DesireType.Activity activity = DesireType.Activity.byName(target);
                yield new ItemStack(activity != null ? activity.icon : Items.JUKEBOX);
            }
            case "need" -> switch (target) {
                case "hunger" -> new ItemStack(Items.BREAD);
                case "energy" -> new ItemStack(Items.RED_BED);
                case "fun" -> new ItemStack(Items.NOTE_BLOCK);
                case "attention" -> new ItemStack(Items.POPPY);
                case "hygiene" -> new ItemStack(Items.WATER_BUCKET);
                default -> ItemStack.EMPTY;
            };
            default -> ItemStack.EMPTY;
        };
    }
}
