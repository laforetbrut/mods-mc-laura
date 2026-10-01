package com.vyrriox.lauramod.fabric.compat;

import com.vyrriox.lauramod.api.LauraAPI;
import com.vyrriox.lauramod.api.ScriptData;
import com.vyrriox.lauramod.entity.LauraEntity;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.event.EventResult;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * KubeJS support: the {@code Laura} global (see {@link LauraAPI}) and the {@code LauraEvents}
 * server events. Listed in kubejs.plugins.txt, so it is only loaded when KubeJS is installed.
 * <pre>
 * LauraEvents.chat(e =&gt; {
 *   if (e.detail == 'hello') { Laura.say(e.laura, e.player, 'Hi from a script!'); e.cancel() }
 * })
 * Laura.addGift({ match: 'minecraft:emerald_block', affection: 60, tier: 'AMAZING' })
 * </pre>
 *
 * @author vyrriox
 */
public class LauraKubeJSPlugin extends KubeJSPlugin {
    public static final EventGroup GROUP = EventGroup.of("LauraEvents");
    // hasResult: KubeJS for Minecraft 1.20.1 only lets scripts cancel the events that declare it.
    public static final EventHandler SUMMON = GROUP.server("summon", () -> Event.class).hasResult();
    public static final EventHandler DEATH = GROUP.server("death", () -> Event.class).hasResult();
    public static final EventHandler REVIVE = GROUP.server("revive", () -> Event.class).hasResult();
    public static final EventHandler DESIRE_FULFILLED = GROUP.server("desireFulfilled", () -> Event.class).hasResult();
    public static final EventHandler DESIRE_FAILED = GROUP.server("desireFailed", () -> Event.class).hasResult();
    public static final EventHandler GIFT = GROUP.server("gift", () -> Event.class).hasResult();
    public static final EventHandler EMOTE = GROUP.server("emote", () -> Event.class).hasResult();
    public static final EventHandler CHAT = GROUP.server("chat", () -> Event.class).hasResult();

    private static final Map<String, EventHandler> HANDLERS = Map.of(
            "summon", SUMMON, "death", DEATH, "revive", REVIVE, "desire_fulfilled", DESIRE_FULFILLED,
            "desire_failed", DESIRE_FAILED, "gift", GIFT, "emote", EMOTE, "chat", CHAT);

    /** What scripts receive: the companion, the player (may be null) and a detail (intent, item id...). */
    public static class Event extends EventJS {
        private final LauraEntity laura;
        @Nullable
        private final ServerPlayer player;
        private final String detail;

        public Event(LauraEntity laura, @Nullable ServerPlayer player, String detail) {
            this.laura = laura;
            this.player = player;
            this.detail = detail;
        }

        public LauraEntity getLaura() {
            return laura;
        }

        @Nullable
        public ServerPlayer getPlayer() {
            return player;
        }

        public String getDetail() {
            return detail;
        }
    }

    @Override
    public void init() {
        LauraAPI.setExternalEvents((event, laura, player, detail) -> {
            EventHandler handler = HANDLERS.get(event);
            if (handler == null || !handler.hasListeners()) {
                return false;
            }
            EventResult result = handler.post(new Event(laura, player, detail));
            return result.interruptFalse();
        });
    }

    @Override
    public void registerEvents() {
        GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        event.add("Laura", LauraAPI.class);
        // KubeJS for Minecraft 1.20.1 has no "before scripts" hook: bindings are set up once per
        // load, right before the scripts run.
        if (event.getType() == ScriptType.SERVER) {
            // Scripts run again from scratch: forget what they added last time.
            ScriptData.clear();
            LauraAPI.clearListeners();
        }
    }

    @Override
    public void registerClasses(ScriptType type, ClassFilter filter) {
        filter.allow("com.vyrriox.lauramod.api");
    }

    /** Called once the server scripts are loaded. */
    @Override
    public void onServerReload() {
        ScriptData.requestReload();
    }
}
