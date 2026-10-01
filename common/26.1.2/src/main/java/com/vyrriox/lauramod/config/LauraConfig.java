package com.vyrriox.lauramod.config;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.entity.CombatMode;

import java.util.List;

/**
 * Server-side (and singleplayer) settings, stored in {@code config/lauramod/lauramod-common.json}.
 *
 * @author vyrriox
 */
public final class LauraConfig {
    /** What happens when Laura dies. */
    public enum ReviveMode {
        GRAVE, TIMER, NONE
    }

    /** How demanding Laura is. Scales need decay, reminders, complaints and order refusals. */
    public enum Annoyance {
        CHILL(0.5, 0.25, 0.0),
        NORMAL(1.0, 0.6, 0.0),
        NEEDY(1.25, 1.0, 0.15),
        UNBEARABLE(1.5, 1.6, 0.3);

        /** Multiplier applied to need decay speed. */
        public final double decay;
        /** Multiplier applied to how often she nags. */
        public final double nagging;
        /** Chance to refuse an order when she is unhappy. */
        public final double refusal;

        Annoyance(double decay, double nagging, double refusal) {
            this.decay = decay;
            this.nagging = nagging;
            this.refusal = refusal;
        }
    }

    private static ConfigFile file;

    // general
    public static ConfigFile.StringValue defaultName;
    public static ConfigFile.DoubleValue maxHealth;
    public static ConfigFile.DoubleValue regenPerSecond;
    public static ConfigFile.BoolValue invulnerable;
    public static ConfigFile.DoubleValue movementSpeed;
    public static ConfigFile.DoubleValue attackDamage;
    public static ConfigFile.IntValue maxPerPlayer;
    public static ConfigFile.IntValue maxPerWorld;
    public static ConfigFile.StringListValue extraNames;
    public static ConfigFile.EnumValue<ReviveMode> reviveMode;
    public static ConfigFile.IntValue respawnDelaySeconds;
    public static ConfigFile.StringListValue reviveItems;
    public static ConfigFile.IntValue inventoryRows;

    // summoning
    public static ConfigFile.BoolValue chatSummon;
    public static ConfigFile.IntValue summonPermissionLevel;
    public static ConfigFile.IntValue summonCooldownSeconds;

    // follow
    public static ConfigFile.DoubleValue followStartDistance;
    public static ConfigFile.DoubleValue followStopDistance;
    public static ConfigFile.DoubleValue teleportDistance;
    public static ConfigFile.BoolValue teleportWhenStuck;
    public static ConfigFile.BoolValue followAcrossDimensions;
    public static ConfigFile.BoolValue complainWhenStuck;
    public static ConfigFile.IntValue stuckSeconds;

    // home
    public static ConfigFile.IntValue homeRadius;
    public static ConfigFile.IntValue homeTeleportDistance;
    public static ConfigFile.BoolValue sleepAtNight;

    // personality
    public static ConfigFile.IntValue chatterMinutes;
    public static ConfigFile.IntValue loveCheckMinutes;
    public static ConfigFile.IntValue loveAnswerSeconds;
    public static ConfigFile.BoolValue weatherComplaints;
    public static ConfigFile.BoolValue farts;
    public static ConfigFile.IntValue fartMinutes;
    public static ConfigFile.BoolValue playfulHit;
    public static ConfigFile.IntValue playfulHitMinutes;
    public static ConfigFile.BoolValue jealousOfVillagers;
    public static ConfigFile.BoolValue danceToJukebox;
    public static ConfigFile.BoolValue greetOnJoin;
    public static ConfigFile.BoolValue worryWhenOwnerHurt;
    public static ConfigFile.DoubleValue ownerHealthThreshold;
    public static ConfigFile.BoolValue feedOwner;
    public static ConfigFile.BoolValue keepOwnerItemsOnDeath;
    public static ConfigFile.IntValue anniversaryDays;
    public static ConfigFile.IntValue hitsBeforeSulking;
    public static ConfigFile.IntValue sulkMinutes;
    public static ConfigFile.IntValue startAffection;
    public static ConfigFile.BoolValue spontaneousEmotes;
    public static ConfigFile.IntValue spontaneousEmoteSeconds;

    // needs
    public static ConfigFile.BoolValue needsEnabled;
    public static ConfigFile.EnumValue<Annoyance> annoyance;
    public static ConfigFile.IntValue hungerMinutes;
    public static ConfigFile.IntValue energyMinutes;
    public static ConfigFile.IntValue funMinutes;
    public static ConfigFile.IntValue attentionMinutes;
    public static ConfigFile.IntValue hygieneMinutes;
    public static ConfigFile.DoubleValue healWhenFedPerSecond;
    public static ConfigFile.BoolValue starvationHurts;
    public static ConfigFile.BoolValue starvationCanKill;
    public static ConfigFile.BoolValue autoEat;
    public static ConfigFile.BoolValue searchFoodInContainers;
    public static ConfigFile.BoolValue stealFood;
    public static ConfigFile.BoolValue desiresEnabled;
    public static ConfigFile.IntValue desireMinutes;
    public static ConfigFile.IntValue desireDeadlineMinutes;
    public static ConfigFile.BoolValue refuseOrders;
    public static ConfigFile.BoolValue commentActivities;
    public static ConfigFile.BoolValue jealousOfPlayers;
    public static ConfigFile.IntValue afkMinutes;
    public static ConfigFile.IntValue giftCooldownSeconds;

    // combat
    public static ConfigFile.EnumValue<CombatMode> defaultCombatMode;
    public static ConfigFile.BoolValue retaliate;

    // fetch
    public static ConfigFile.BoolValue fetchEnabled;
    public static ConfigFile.IntValue fetchRadius;
    public static ConfigFile.BoolValue fetchFromContainers;
    public static ConfigFile.BoolValue fetchBreakBlocks;
    public static ConfigFile.BoolValue fetchReplantCrops;
    public static ConfigFile.IntValue fetchMaxItems;
    public static ConfigFile.IntValue fetchTimeoutSeconds;

    // work
    public static ConfigFile.BoolValue workEnabled;
    public static ConfigFile.IntValue workRadius;
    public static ConfigFile.IntValue workMaxRadius;
    public static ConfigFile.BoolValue onlyNaturalTrees;
    public static ConfigFile.BoolValue breakLeaves;
    public static ConfigFile.BoolValue replantSaplings;
    public static ConfigFile.IntValue maxLogsPerTree;
    public static ConfigFile.BoolValue plantEmptyFarmland;
    public static ConfigFile.BoolValue useBoneMeal;
    public static ConfigFile.BoolValue depositInChests;
    public static ConfigFile.BoolValue workAtNight;
    public static ConfigFile.IntValue workSpeedPercent;

    // gag
    public static ConfigFile.BoolValue gagEnabled;
    public static ConfigFile.StringListValue gagItems;
    public static ConfigFile.StringListValue ungagItems;
    public static ConfigFile.IntValue gagSeconds;
    public static ConfigFile.IntValue gagAffectionPenalty;
    public static ConfigFile.IntValue dismissAffectionPenalty;

    // dialogue
    public static ConfigFile.IntValue chatRange;
    public static ConfigFile.BoolValue requireNameInChat;
    public static ConfigFile.BoolValue matchAllLanguages;
    public static ConfigFile.StringValue fallbackLanguage;
    public static ConfigFile.StringValue forcedLanguage;
    public static ConfigFile.BoolValue customDialoguesReplace;
    public static ConfigFile.StringValue nameColor;
    public static ConfigFile.BoolValue speechBubbles;

    // skins
    public static ConfigFile.StringValue defaultSkin;
    public static ConfigFile.BoolValue allowUrlSkins;
    public static ConfigFile.StringListValue urlDomainWhitelist;
    public static ConfigFile.BoolValue allowPlayerNameSkins;
    public static ConfigFile.BoolValue allowSkinUploads;
    public static ConfigFile.IntValue maxSkinKb;
    public static ConfigFile.IntValue maxSkinUploadsPerPlayer;
    public static ConfigFile.IntValue maxModelUploadsPerPlayer;
    public static ConfigFile.BoolValue othersCanChangeSkin;

    // models
    public static ConfigFile.BoolValue allowCustomModels;
    public static ConfigFile.BoolValue allowModelUploads;
    public static ConfigFile.IntValue maxModelKb;
    public static ConfigFile.StringValue defaultModel;

    // permissions
    public static ConfigFile.BoolValue othersCanInteract;
    public static ConfigFile.IntValue reloadPermissionLevel;

    private LauraConfig() {
    }

    public static synchronized void load() {
        if (file == null) {
            file = build();
        }
        file.load();
    }

    public static ConfigFile file() {
        if (file == null) {
            load();
        }
        return file;
    }

    private static ConfigFile build() {
        ConfigFile f = new ConfigFile(LauraMod.configDir().resolve("lauramod-common.json"),
                "My Girlfriend Laura - common and server configuration.",
                "Reload in game with /laura reload (operators only).");

        ConfigFile.Section general = f.section("general", "Core settings.");
        defaultName = general.string("defaultName", "Laura", "Name given to a newly summoned Laura. Players can rename her with /laura name or a name tag.");
        maxHealth = general.decimal("maxHealth", 100.0, 1.0, 100000.0, "Maximum health points. Version 1.x used 1000.");
        regenPerSecond = general.decimal("regenPerSecond", 0.0, 0.0, 10000.0, "Free health regenerated every second, without eating. 0 = she must eat to heal (see the needs section). Version 1.x used 20.");
        invulnerable = general.bool("invulnerable", false, "If true, Laura never takes damage.");
        movementSpeed = general.decimal("movementSpeed", 0.4, 0.05, 1.5, "Base walking speed attribute (a player walks at about 0.1, a villager at 0.5).");
        attackDamage = general.decimal("attackDamage", 4.0, 0.0, 1000.0, "Base attack damage, before the weapon she holds.");
        maxPerPlayer = general.integer("maxPerPlayer", 3, 1, 100, "How many companions one player can have. Each one is bound to the player who summoned her.");
        maxPerWorld = general.integer("maxPerWorld", 0, 0, 10000, "Limit for the whole world or server. 0 = no limit. 1 = a single Laura for everybody, like version 1.x.");
        extraNames = general.list("extraNames", List.of("Emma", "Chloe", "Lea", "Jade", "Lina", "Mia", "Zoe", "Luna", "Ines", "Rose"),
                "Names given to your second, third... companion (the first one gets defaultName).");
        reviveMode = general.enumeration("reviveMode", ReviveMode.GRAVE, "What happens when she dies. GRAVE: build a Laura's Gravestone and lay a flower on it to bring her back (default).\nTIMER: she comes back next to you after respawnDelaySeconds. NONE: she is gone for good and drops her things.\nWith GRAVE and TIMER she keeps her memories and her inventory.");
        respawnDelaySeconds = general.integer("respawnDelaySeconds", 30, 0, 3600, "Delay before she comes back in TIMER mode.");
        reviveItems = general.list("reviveItems", List.of("#minecraft:small_flowers", "#minecraft:flowers"), "Items that can be laid on a gravestone to bring her back.");
        inventoryRows = general.integer("inventoryRows", 3, 1, 6, "Rows of 9 slots in her personal inventory.");
        dismissAffectionPenalty = general.integer("dismissAffectionPenalty", 20, 0, 1000, "Affection lost when she is dismissed (she comes back at the next summon).");

        ConfigFile.Section summon = f.section("summoning", "How players get a Laura.");
        chatSummon = summon.bool("chatSummon", true, "Say \"I feel lonely\" (in any supported language) to summon her.");
        summonPermissionLevel = summon.integer("summonPermissionLevel", 0, 0, 4, "Permission level required for /laura summon and the chat summon. 0 = everyone.");
        summonCooldownSeconds = summon.integer("summonCooldownSeconds", 5, 0, 86400, "Cooldown between two summons for the same player.");

        ConfigFile.Section follow = f.section("follow", "Following her partner.");
        followStartDistance = follow.decimal("startDistance", 7.0, 2.0, 64.0, "She starts walking to you when you are further than this (blocks).");
        followStopDistance = follow.decimal("stopDistance", 3.5, 1.0, 32.0, "She stops when she is this close.");
        teleportDistance = follow.decimal("teleportDistance", 128.0, 8.0, 1024.0, "She only teleports next to you when she is further than this (blocks). Closer than that, she walks.");
        teleportWhenStuck = follow.bool("teleportWhenStuck", false, "Also teleport when she has been stuck for stuckSeconds, even closer than teleportDistance.\nWhen off, she only tells you she is stuck; calling her (\"come\") still brings her if no path exists.");
        followAcrossDimensions = follow.bool("followAcrossDimensions", true, "She follows you to the Nether, the End and other dimensions.");
        complainWhenStuck = follow.bool("complainWhenStuck", true, "She tells you when she cannot reach you.");
        stuckSeconds = follow.integer("stuckSeconds", 10, 3, 600, "How long she must be stuck before complaining.");

        ConfigFile.Section home = f.section("home", "The \"stay at home\" mode.");
        homeRadius = home.integer("radius", 12, 2, 64, "How far she wanders around her home.");
        homeTeleportDistance = home.integer("teleportDistance", 48, 8, 100000, "When sent home from further than this, she teleports instead of walking.");
        sleepAtNight = home.bool("sleepAtNight", true, "At home, she lies down in a nearby bed at night.");

        ConfigFile.Section personality = f.section("personality", "Behaviors and random events. Set a timer to 0 to disable it.");
        chatterMinutes = personality.integer("chatterMinutes", 10, 0, 1440, "Average minutes between two random messages.");
        loveCheckMinutes = personality.integer("loveCheckMinutes", 15, 0, 1440, "Average minutes between two \"Do you still love me?\" questions.");
        loveAnswerSeconds = personality.integer("loveAnswerSeconds", 30, 5, 600, "Time you have to answer her.");
        weatherComplaints = personality.bool("weatherComplaints", true, "She complains about rain, thunder and dark nights.");
        farts = personality.bool("farts", true, "Rare fart sound with a smoke puff. Yes, really.");
        fartMinutes = personality.integer("fartMinutes", 20, 1, 1440, "Average minutes between two farts.");
        playfulHit = personality.bool("playfulHit", true, "Now and then she playfully hits you, then apologizes.");
        playfulHitMinutes = personality.integer("playfulHitMinutes", 60, 1, 1440, "Minutes between two playful hits.");
        jealousOfVillagers = personality.bool("jealousOfVillagers", true, "Villagers run away from her when she walks around with you.");
        danceToJukebox = personality.bool("danceToJukebox", true, "She dances when a jukebox plays music nearby.");
        greetOnJoin = personality.bool("greetOnJoin", true, "She greets you when you join the world.");
        worryWhenOwnerHurt = personality.bool("worryWhenOwnerHurt", true, "She worries when your health is low.");
        ownerHealthThreshold = personality.decimal("ownerHealthThreshold", 0.3, 0.05, 0.95, "Health fraction under which she worries.");
        feedOwner = personality.bool("feedOwner", true, "When you are hurt and hungry, she gives you food from her inventory.");
        keepOwnerItemsOnDeath = personality.bool("keepOwnerItemsOnDeath", true, "When you die near her, she collects your items and gives them back.");
        anniversaryDays = personality.integer("anniversaryDays", 7, 0, 3650, "Celebrate every N in-game days together. 0 disables it.");
        hitsBeforeSulking = personality.integer("hitsBeforeSulking", 3, 1, 100, "Hits from her partner before she sulks.");
        sulkMinutes = personality.integer("sulkMinutes", 3, 1, 1440, "How long she sulks if you do not apologize.");
        startAffection = personality.integer("startAffection", 500, 0, 1000, "Affection when she is summoned (0 to 1000).");
        spontaneousEmotes = personality.bool("spontaneousEmotes", true, "Now and then she plays an animation on her own (claps, snaps her fingers, hums, twirls, sneezes...) with its sound.");
        spontaneousEmoteSeconds = personality.integer("spontaneousEmoteSeconds", 90, 10, 3600, "Average seconds between two spontaneous animations.");

        ConfigFile.Section needs = f.section("needs", "Her needs and desires, like a real (and very demanding) partner.\nNeeds go from 100 (satisfied) to 0 (desperate). The minutes below are the time a need takes to go from full to empty.");
        needsEnabled = needs.bool("enabled", true, "Enable hunger, energy, fun, attention and hygiene. If false she never needs anything (and heals with regenPerSecond only).");
        annoyance = needs.enumeration("annoyance", Annoyance.UNBEARABLE, "How demanding she is. CHILL: calm companion. NORMAL. NEEDY. UNBEARABLE: the real experience (default).");
        hungerMinutes = needs.integer("hungerMinutes", 40, 1, 10080, "Hunger: feed her (right click with food) or let her eat from her inventory.");
        energyMinutes = needs.integer("energyMinutes", 60, 1, 10080, "Energy: she recovers it by sleeping.");
        funMinutes = needs.integer("funMinutes", 30, 1, 10080, "Fun: dancing, music, gifts, exploring new places, emotes.");
        attentionMinutes = needs.integer("attentionMinutes", 20, 1, 10080, "Attention: talk to her, hug her, kiss her, compliment her, stay close.");
        hygieneMinutes = needs.integer("hygieneMinutes", 120, 1, 10080, "Hygiene: water, rain or a bath refresh her.");
        healWhenFedPerSecond = needs.decimal("healWhenFedPerSecond", 0.5, 0.0, 1000.0, "Health regenerated every second while her hunger is above 60.");
        starvationHurts = needs.bool("starvationHurts", true, "She slowly loses health when her hunger is at 0.");
        starvationCanKill = needs.bool("starvationCanKill", false, "If false, starvation never takes her below 1 health.");
        autoEat = needs.bool("autoEat", true, "She eats the food stored in her inventory when she is hungry.");
        searchFoodInContainers = needs.bool("searchFoodInContainers", true, "When starving, she looks for food in nearby chests and barrels.");
        stealFood = needs.bool("stealFood", true, "When starving and next to her partner, she steals food from their inventory.");
        desiresEnabled = needs.bool("desires", true, "She regularly wants something: an item, a place, an activity. Fulfil it before the deadline!");
        desireMinutes = needs.integer("desireMinutes", 12, 1, 1440, "Average minutes between two desires.");
        desireDeadlineMinutes = needs.integer("desireDeadlineMinutes", 15, 1, 1440, "Time you have to fulfil a desire.");
        refuseOrders = needs.bool("refuseOrders", true, "When she is unhappy she sometimes refuses an order (asking twice always works). The chance depends on the annoyance level.");
        commentActivities = needs.bool("commentActivities", true, "She comments on what you do: mining, fighting, eating without her, going AFK...");
        jealousOfPlayers = needs.bool("jealousOfPlayers", true, "She gets jealous when you chat with other players.");
        afkMinutes = needs.integer("afkMinutes", 5, 1, 1440, "Minutes without moving before she decides you are ignoring her.");
        giftCooldownSeconds = needs.integer("giftCooldownSeconds", 300, 0, 86400, "The same kind of gift or favorite food makes her fonder only once in this time, per companion.\nIn between she still takes the gift and eats the food, and a wish is still fulfilled, but she gains no affection and gives nothing back. 0 = no limit.");

        ConfigFile.Section combat = f.section("combat", "Fighting.");
        defaultCombatMode = combat.enumeration("defaultMode", CombatMode.PASSIVE, "PASSIVE: she avoids monsters. DEFENSIVE: she protects you. AGGRESSIVE: she also attacks nearby monsters.");
        retaliate = combat.bool("retaliate", true, "She hits back anyone (except her partner) who hits her, unless she is PASSIVE.");

        ConfigFile.Section fetch = f.section("fetch", "Asking Laura to bring you things.");
        fetchEnabled = fetch.bool("enabled", true, "Enable the fetch action.");
        fetchRadius = fetch.integer("radius", 24, 4, 64, "Search radius around her, in blocks.");
        fetchFromContainers = fetch.bool("fromContainers", true, "She may take the item from nearby chests, barrels and other containers.");
        fetchBreakBlocks = fetch.bool("breakBlocks", true, "She may harvest blocks listed in the lauramod:fetch_harvestable block tag (logs, flowers, crops...). Respects the mobGriefing game rule.");
        fetchReplantCrops = fetch.bool("replantCrops", true, "She replants the crops she harvests.");
        fetchMaxItems = fetch.integer("maxItems", 64, 1, 576, "Maximum number of items per request.");
        fetchTimeoutSeconds = fetch.integer("timeoutSeconds", 60, 10, 600, "She gives up after this time.");

        ConfigFile.Section work = f.section("work", "Jobs: lumberjack (fells trees) and farmer (takes care of a field). Both respect the mobGriefing game rule.");
        workEnabled = work.bool("enabled", true, "Enable jobs.");
        workRadius = work.integer("defaultRadius", 10, 3, 64, "Default size of her work area around the place you give the order.");
        workMaxRadius = work.integer("maxRadius", 24, 3, 64, "Largest work area a player can ask for.");
        onlyNaturalTrees = work.bool("onlyNaturalTrees", true, "Only fell trees that have natural leaves, never logs used in builds.");
        breakLeaves = work.bool("breakLeaves", true, "Also clear the leaves of a felled tree (drops saplings, apples and sticks right away).");
        replantSaplings = work.bool("replantSaplings", true, "Plant a sapling where the tree stood.");
        maxLogsPerTree = work.integer("maxLogsPerTree", 200, 1, 2000, "Safety limit: logs cut in one tree.");
        plantEmptyFarmland = work.bool("plantEmptyFarmland", true, "As a farmer, sow empty farmland with seeds from her inventory.");
        useBoneMeal = work.bool("useBoneMeal", true, "As a farmer, use bone meal from her inventory on growing crops.");
        depositInChests = work.bool("depositInChests", true, "Store the harvest in a chest or barrel inside the work area.");
        workAtNight = work.bool("workAtNight", false, "Keep working at night instead of going to bed.");
        workSpeedPercent = work.integer("speedPercent", 100, 10, 1000, "Work speed. 200 = twice as fast.");

        ConfigFile.Section gag = f.section("gag", "Gagging her with hay (right click her with a gag item).");
        gagEnabled = gag.bool("enabled", true, "Enable the gag.");
        gagItems = gag.list("gagItems", List.of("minecraft:hay_block"), "Items that gag her. Item ids or #tags.");
        ungagItems = gag.list("ungagItems", List.of("minecraft:shears"), "Items that remove the gag. Sneaking with an empty hand works too.");
        gagSeconds = gag.integer("durationSeconds", 300, 0, 86400, "She removes the gag herself after this time. 0 = never.");
        gagAffectionPenalty = gag.integer("affectionPenalty", 15, 0, 1000, "Affection lost when gagged.");

        ConfigFile.Section dialogue = f.section("dialogue", "Chat and languages.");
        chatRange = dialogue.integer("chatRange", 64, 4, 512, "Proximity chat: she only hears you, and you only hear her, within this distance in blocks and in the same dimension.\nCommands (/laura come, /laura where) and Laura's Heart still work from anywhere.");
        requireNameInChat = dialogue.bool("requireName", false, "If true, chat orders must contain her name (\"Laura, follow me\").");
        fallbackLanguage = dialogue.string("fallbackLanguage", "en_us", "Language used when a player's language has no dialogue file.");
        matchAllLanguages = dialogue.bool("matchAllLanguages", false, "If false she understands chat in each player's own language plus English (fewer false matches). If true, in every language she knows.");
        forcedLanguage = dialogue.string("forcedLanguage", "", "Force one dialogue language for everyone (for example fr_fr). Empty = each player's own language.");
        customDialoguesReplace = dialogue.bool("customDialoguesReplace", false, "false: files in config/lauramod/dialogues add lines to the built-in ones. true: they replace them.");
        nameColor = dialogue.string("nameColor", "light_purple", "Color of her name in chat (a Minecraft color name or #RRGGBB).");
        speechBubbles = dialogue.bool("speechBubbles", true, "Show what she says in a bubble above her head.");

        ConfigFile.Section skins = f.section("skins", "Skins players can give her.");
        defaultSkin = skins.string("defaultSkin", "builtin:laura", "Skin of a newly summoned Laura. builtin:<name>, server:<file name>, url:<link> or player:<name>.");
        allowUrlSkins = skins.bool("allowUrlSkins", true, "Allow skins from an internet address.");
        urlDomainWhitelist = skins.list("urlDomainWhitelist", List.of("textures.minecraft.net", "i.imgur.com", "s.namemc.com", "namemc.com", "mc-heads.net", "minotar.net", "crafatar.com", "raw.githubusercontent.com", "cdn.discordapp.com", "media.discordapp.net"),
                "Domains allowed for URL skins (sub-domains included). Put \"*\" to allow every domain.");
        allowPlayerNameSkins = skins.bool("allowPlayerNameSkins", true, "Allow copying the skin of any Minecraft account by name.");
        allowSkinUploads = skins.bool("allowUploads", true, "Allow players to upload a skin file to the server (drag and drop in the skin screen).");
        maxSkinKb = skins.integer("maxSkinKb", 256, 8, 4096, "Maximum size of a skin file, in kilobytes.");
        maxSkinUploadsPerPlayer = skins.integer("maxUploadsPerPlayer", 10, 1, 1000, "Skin files one player may keep on the server. Uploading a file with the same name replaces it.");
        othersCanChangeSkin = skins.bool("othersCanChangeSkin", false, "Allow players other than her partner to change her skin.");

        ConfigFile.Section models = f.section("models", "Custom Blockbench models (.bbmodel or .geo.json) from config/lauramod/models.");
        allowCustomModels = models.bool("allowCustomModels", true, "Allow custom models.");
        allowModelUploads = models.bool("allowUploads", false, "Allow players to upload their own model files to the server.");
        maxModelKb = models.integer("maxModelKb", 2048, 16, 16384, "Maximum size of a model file, in kilobytes.");
        maxModelUploadsPerPlayer = models.integer("maxUploadsPerPlayer", 3, 1, 1000, "Model files one player may keep on the server. Uploading a file with the same name replaces it.");
        defaultModel = models.string("defaultModel", "", "Model of a newly summoned Laura. Empty = the player-like default model.");

        ConfigFile.Section permissions = f.section("permissions", "Who can do what.");
        othersCanInteract = permissions.bool("othersCanInteract", false, "Allow other players to open her menu and give her orders.");
        reloadPermissionLevel = permissions.integer("reloadPermissionLevel", 2, 0, 4, "Permission level required for /laura reload and /laura admin.");
        return f;
    }
}
