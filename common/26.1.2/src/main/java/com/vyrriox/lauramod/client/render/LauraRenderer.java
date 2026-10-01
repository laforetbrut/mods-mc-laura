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
import com.vyrriox.lauramod.entity.Emote;
import com.vyrriox.lauramod.entity.LauraEntity;
import com.vyrriox.lauramod.util.ItemSpec;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Renders Laura: a player-like model with her skin, or a custom Blockbench model, plus her
 * equipment, the hay gag, the item she carries, and her speech and thought bubbles.
 *
 * @author vyrriox
 */
public class LauraRenderer extends LivingEntityRenderer<LauraEntity, LauraRenderState, LauraPlayerModel> {
    private static final Identifier BUBBLE = LauraMod.id("textures/entity/thought_bubble.png");
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

    public LauraRenderer(EntityRendererProvider.Context context) {
        super(context, new LauraPlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wide = this.model;
        this.slim = new LauraPlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        LauraLayers.PlayerParent parent = new LauraLayers.PlayerParent(this);
        // Her armor uses her own model class, so it follows her animations like her body.
        this.addLayer(new LauraLayers.OnlyDefaultModel(this, new HumanoidArmorLayer<>(parent,
                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), part -> new LauraPlayerModel(part, false)),
                context.getEquipmentRenderer())));
        this.addLayer(new LauraLayers.OnlyDefaultModel(this, new ItemInHandLayer<>(parent)));
        this.addLayer(new LauraLayers.OnlyDefaultModel(this, new CustomHeadLayer<>(parent, context.getModelSet(), context.getPlayerSkinRenderCache())));
        this.addLayer(new LauraLayers.CustomBody(this));
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

    @Override
    protected boolean shouldShowName(LauraEntity laura, double distanceToCameraSq) {
        return !guiPreview && super.shouldShowName(laura, distanceToCameraSq);
    }

    public CustomEntityModel customModel() {
        return custom;
    }

    /** The player model of the companion being drawn (wide or slim arms). */
    public LauraPlayerModel activePlayerModel() {
        return this.model;
    }

    @Override
    public LauraRenderState createRenderState() {
        return new LauraRenderState();
    }

    @Override
    public void extractRenderState(LauraEntity laura, LauraRenderState state, float partialTick) {
        super.extractRenderState(laura, state, partialTick);
        HumanoidMobRenderer.extractHumanoidRenderState(laura, state, partialTick, this.itemModelResolver);
        state.preview = guiPreview;
        state.slim = laura.isSlim();
        state.sitting = laura.isInSittingPose() && !laura.isPassenger();
        state.asleep = laura.isAsleep();
        state.gagged = laura.isGagged();
        state.eating = laura.getEmote() == Emote.EAT;
        // Sitting on the ground bends her legs like riding does.
        state.isPassenger = state.isPassenger || laura.isInSittingPose();
        state.custom = LauraClientConfig.customModels.get() ? ClientModels.get(laura.getModelName()) : null;
        state.rootOffsetY = 0;
        state.rootRotX = 0;
        state.headBone = null;
        if (state.custom != null) {
            custom.extract(laura, state, partialTick);
            state.texture = state.custom.textures().get(0);
        } else {
            LauraPlayerModel.extract(laura, state, partialTick);
            state.texture = SkinTextures.get(laura.getSkin());
        }
        this.itemModelResolver.updateForLiving(state.backItem, laura.getBackItem(), ItemDisplayContext.FIXED, laura);
        this.itemModelResolver.updateForLiving(state.carriedItem, laura.getCarried(), ItemDisplayContext.FIXED, laura);
        state.speech = null;
        state.thoughtIcon.clear();
        if (!guiPreview) {
            ClientState.Bubble bubble = LauraClientConfig.speechBubbles.get() ? ClientState.bubble(laura.getId()) : null;
            state.speech = bubble == null ? null : bubble.text();
            if (LauraClientConfig.thoughtBubbles.get()) {
                this.itemModelResolver.updateForLiving(state.thoughtIcon, thoughtIcon(laura.getThought()), ItemDisplayContext.GUI, laura);
            }
        }
    }

    @Override
    public void submit(LauraRenderState laura, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        this.model = laura.slim ? slim : wide;
        custom.applyHeadLook(laura);
        super.submit(laura, poseStack, collector, camera);
        if (!laura.preview) {
            submitBubbles(laura, poseStack, collector, camera);
        }
    }

    @Override
    public Identifier getTextureLocation(LauraRenderState laura) {
        return laura.texture;
    }

    /** A custom model is drawn by its own layer: the player model is not submitted at all. */
    @Nullable
    @Override
    protected RenderType getRenderType(LauraRenderState laura, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
        return laura.custom != null ? null : super.getRenderType(laura, isBodyVisible, forceTransparent, appearGlowing);
    }

    @Override
    protected void setupRotations(LauraRenderState laura, PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(laura, poseStack, bodyRot, scale);
        boolean usingCustom = laura.custom != null;
        if (!usingCustom && laura.sitting && !laura.asleep) {
            poseStack.translate(0.0F, -0.62F, 0.0F);
        }
        if (!usingCustom && laura.rootOffsetY != 0) {
            poseStack.translate(0.0F, -laura.rootOffsetY / 16F, 0.0F);
        }
        if (!usingCustom && laura.rootRotX != 0) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(laura.rootRotX));
        }
    }

    // ------------------------------------------------------------------ bubbles

    private void submitBubbles(LauraRenderState laura, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (laura.distanceToCameraSq > 32 * 32 || laura.isInvisible) {
            return;
        }
        float top = laura.custom != null ? Math.max(1.9F, custom.heightInBlocks(laura) + 0.1F) : laura.boundingBoxHeight + 0.1F;
        if (laura.asleep) {
            top = 0.8F;
        }
        boolean nameShown = laura.nameTag != null;
        float y = top + (nameShown ? 0.55F : 0.25F);
        if (laura.speech != null) {
            y = submitSpeech(laura.speech, y, poseStack, collector, camera);
        }
        if (!laura.thoughtIcon.isEmpty()) {
            submitThought(laura, y + 0.1F, poseStack, collector, camera);
        }
    }

    /**
     * One rounded bubble behind all the lines, drawn like vanilla text displays: the background in
     * its own pass and the text with a polygon offset, so they never fight for the same depth (the
     * old per-line background flickered and turned the text unreadable from a few blocks away). Both
     * are full bright: she stays readable at night and in caves.
     */
    private float submitSpeech(Component text, float y, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
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
        poseStack.mulPose(camera.orientation);
        poseStack.scale(scale, -scale, scale);
        collector.submitCustomGeometry(poseStack, RenderTypes.textBackground(), (pose, bg) -> {
            // Fill, then a 1 px border made of strips around it (never on top of it), then the tail.
            rect(bg, pose, x0 + 1, 1, x1 - 1, boxHeight - 1, SPEECH_FILL);
            rect(bg, pose, x0 + 1, 0, x1 - 1, 1, SPEECH_BORDER);
            rect(bg, pose, x0 + 1, boxHeight - 1, x1 - 1, boxHeight, SPEECH_BORDER);
            rect(bg, pose, x0, 1, x0 + 1, boxHeight - 1, SPEECH_BORDER);
            rect(bg, pose, x1 - 1, 1, x1, boxHeight - 1, SPEECH_BORDER);
            for (int i = 0; i < tail; i++) {
                float half = tail - i;
                rect(bg, pose, -half, boxHeight + i, half, boxHeight + i + 1, SPEECH_BORDER);
            }
        });
        // Submitted one order above the background, like the lines of a text display.
        OrderedSubmitNodeCollector textCollector = collector.order(1);
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            textCollector.submitText(poseStack, -font.width(line) / 2F, 4 + i * 10, line, false,
                    Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT, SPEECH_TEXT, 0, 0);
        }
        poseStack.popPose();
        return y + (boxHeight + tail) * scale + 0.1F;
    }

    private static void rect(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1, int color) {
        consumer.addVertex(pose, x0, y0, 0).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
        consumer.addVertex(pose, x0, y1, 0).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
        consumer.addVertex(pose, x1, y1, 0).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
        consumer.addVertex(pose, x1, y0, 0).setColor(color).setLight(LightCoordsUtil.FULL_BRIGHT);
    }

    private void submitThought(LauraRenderState laura, float y, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = laura.lightCoords;
        poseStack.pushPose();
        float bob = (float) Math.sin(laura.ageInTicks * 0.1F) * 0.05F;
        poseStack.translate(0.0F, y + 0.3F + bob, 0.0F);
        poseStack.mulPose(camera.orientation);
        poseStack.pushPose();
        poseStack.scale(0.6F, 0.6F, 0.6F);
        int overlay = OverlayTexture.NO_OVERLAY;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(BUBBLE),
                (pose, consumer) -> quad(consumer, pose, -0.5F, -0.5F, 0.5F, 0.5F, light, overlay));
        poseStack.popPose();
        poseStack.translate(0.0F, 0.02F, -0.01F);
        poseStack.scale(0.3F, 0.3F, 0.3F);
        laura.thoughtIcon.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1, int light, int overlay) {
        consumer.addVertex(pose, x0, y0, 0).setColor(-1).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(pose, x1, y0, 0).setColor(-1).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(pose, x1, y1, 0).setColor(-1).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
        consumer.addVertex(pose, x0, y1, 0).setColor(-1).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(0, 0, -1);
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
