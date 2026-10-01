package com.vyrriox.lauramod.client.render;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.client.model.ClientModels;
import com.vyrriox.lauramod.model.AnimationSampler;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * What the renderer keeps of Laura for one frame. The game draws entities after it has read them
 * all, so everything the model, the layers and the bubbles need is copied here first. It is not an
 * avatar state on purpose: the game draws every avatar state with the vanilla player renderer.
 *
 * @author vyrriox
 */
public class LauraRenderState extends HumanoidRenderState {
    public Identifier texture = LauraMod.id("textures/entity/laura/laura.png");
    public boolean slim;
    /** The custom Blockbench model she wears, or null for the default player-like model. */
    @Nullable
    public ClientModels.Loaded custom;
    /** Bone poses of this frame, by bone name (default model parts or custom model bones). */
    public Map<String, AnimationSampler.BonePose> poses = new HashMap<>();
    /** Custom model: the bone that follows her look, added to the poses when she is drawn. */
    @Nullable
    public String headBone;
    boolean headApplied;
    /** Whole-model offset (pixels, Y down) and forward tilt (degrees) from the "root" track. */
    public float rootOffsetY;
    public float rootRotX;
    /** Sitting on the ground, not riding. */
    public boolean sitting;
    public boolean asleep;
    public boolean gagged;
    public boolean eating;
    /** Drawn by a screen as a preview: no name tag or bubbles over the GUI. */
    public boolean preview;
    public final ItemStackRenderState backItem = new ItemStackRenderState();
    public final ItemStackRenderState carriedItem = new ItemStackRenderState();
    public final ItemStackRenderState thoughtIcon = new ItemStackRenderState();
    /** Text of her speech bubble, or null. */
    @Nullable
    public Component speech;
}
