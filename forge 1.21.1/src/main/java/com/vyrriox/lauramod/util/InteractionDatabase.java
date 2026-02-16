package com.vyrriox.lauramod.util;

import java.util.*;

public class InteractionDatabase {
    private static final Random RANDOM = new Random();

    // Map of Intent ID -> Localized Response Map (Locale -> List of Responses)
    private static final Map<String, Map<String, List<String>>> RESPONSES = new HashMap<>();
    // Map of Intent ID -> List of Triggers (Keywords) across all languages
    private static final Map<String, List<String>> TRIGGERS = new HashMap<>();

    // Ambient messages Map (Locale -> String[])
    private static final Map<String, String[]> AMBIENT = new HashMap<>();

    // Static strings Map (Locale -> KeyValueMap)
    private static final Map<String, Map<String, String>> STATIC = new HashMap<>();

    static {
        // --- DEFINE INTENTS & TRIGGERS ---
        defineIntent("greeting",
                new String[] { "bonjour", "salut", "coucou", "hello", "hi", "hey", "hallo", "moin", "hola",
                        "buenos dias", "ciao", "buongiorno", "ola", "bom dia", "good morning", "good evening",
                        "bonsoir", "buenas noches", "guten abend", "buonasera", "boa noite" });
        defineIntent("how_are_you",
                new String[] { "ça va ?", "comment tu vas ?", "how are you", "how are you doing", "wie geht es dir",
                        "wie geht's", "¿cómo estás?", "como estas", "come stai", "como voce esta", "tu vas bien",
                        "estás bien", "va tutto bene", "alles gut", "tudo bem" });
        defineIntent("love_you", new String[] { "je t'aime", "i love you", "ich liebe dich", "te amo", "ti amo",
                "te quiero", "eu te amo", "amo-te", "love you", "adoro-te" });
        defineIntent("beautiful",
                new String[] { "tu es belle", "t'es belle", "tu es magnifique", "you are beautiful", "you're beautiful",
                        "du bist schön", "eres hermosa", "sei bellissima", "voce e linda", "pretty", "cute", "jolie" });
        defineIntent("marry_me", new String[] { "veux-tu m'épouser", "marry me", "willst du mich heiraten",
                "¿quieres casarte conmigo?", "vuoi sposarmi", "quer casar comigo", "marry", "marriage", "mariage" });
        defineIntent("mining",
                new String[] { "on va miner", "allons miner", "let's go mining", "let's mine", "gehen wir minen",
                        "vamos a minar", "andiamo a minare", "vamos minerar", "mining", "diamonds", "diamant", "pioche",
                        "pickaxe" });
        defineIntent("building", new String[] { "construire", "maison", "house", "building", "build", "casa", "bauen",
                "haus", "costruire" });
        defineIntent("fishing", new String[] { "pécher", "fishing", "fish", "poisson", "angeln", "pescar", "pescare" });
        defineIntent("farming",
                new String[] { "agriculture", "farming", "farm", "ferme", "campo", "garten", "landwirtschaft" });
        defineIntent("joke",
                new String[] { "joke", "blague", "funny", "drôle", "witz", "chiste", "barzelletta", "piada" });
        defineIntent("sing", new String[] { "sing", "chante", "chanter", "singen", "cantar", "cantare" });
        defineIntent("hungry", new String[] { "hungry", "faim", "manger", "eat", "food", "nourriture", "hunger",
                "comida", "mangiare", "fome" });
        defineIntent("gift", new String[] { "gift", "cadeau", "present", "cadeau", "geschenk", "regalo", "presente" });
        defineIntent("who_are_you",
                new String[] { "who are you", "qui es-tu", "quem e voce", "quien eres", "chi sei", "wer bist du" });
        defineIntent("vyrriox",
                new String[] { "vyrriox", "creator", "auteur", "author", "developpeur", "developer", "criador" });

        // --- RESPONSES ---
        // GREETING
        addResponses("greeting", "fr_fr",
                new String[] { "Salut mon amour !", "Coucou toi !", "Oh, tu es là ! Bonjour !",
                        "Salut chéri, ça me fait plaisir de te voir.", "Bonjour ! Prêt pour l'aventure ?" });
        addResponses("greeting", "en_us", new String[] { "Hello my love!", "Hi there!", "Oh, you're here! Hello!",
                "Hey honey, good to see you.", "Good morning! Ready for adventure?" });
        addResponses("greeting", "de_de", new String[] { "Hallo meine Liebe!", "Hey du!", "Oh, du bist hier! Hallo!",
                "Hallo Schatz, schön dich zu sehen.", "Guten Morgen!" });
        addResponses("greeting", "es_es", new String[] { "¡Hola mi amor!", "¡Hola tú!", "¡Oh, estás aquí! ¡Hola!",
                "Hola cariño, que bueno verte.", "¡Buenos días!" });
        addResponses("greeting", "it_it", new String[] { "Ciao amore mio!", "Ehi tu!", "Oh, sei qui! Ciao!",
                "Ciao tesoro, che bello vederti.", "Buongiorno!" });
        addResponses("greeting", "pt_br", new String[] { "Olá meu amor!", "Oi você!", "Oh, você está aqui! Olá!",
                "Oi querido, que bom te ver.", "Bom dia!" });

        // HOW_ARE_YOU
        addResponses("how_are_you", "fr_fr", new String[] { "Je me sens merveilleusement bien avec toi !",
                "Un peu fatiguée, mais ça va.", "Je suis si heureuse d'être ici.", "Tout va pour le mieux !" });
        addResponses("how_are_you", "en_us", new String[] { "I feel wonderful when I'm with you!",
                "A bit tired, but I'm okay.", "I'm so happy to be here.", "Everything is fine!" });

        // LOVE_YOU
        addResponses("love_you", "fr_fr",
                new String[] { "Moi aussi je t'aime, plus que tout ! <3", "Tu es le meilleur petit ami du monde.",
                        "Mon coeur ne bat que pour toi.", "Je t'aime pour toujours.",
                        "Tu es ma raison de vivre ici." });
        addResponses("love_you", "en_us",
                new String[] { "I love you too, more than anything! <3", "You're the best boyfriend ever.",
                        "My heart beats only for you.", "I love you forever.", "You're my reason for existing here." });

        // BEAUTIFUL
        addResponses("beautiful", "fr_fr", new String[] { "Oh merci... Tu vas me faire rougir !",
                "Tu es très charmant aussi.", "C'est gentil de ta part.", "Tu me trouves vraiment jolie ?" });
        addResponses("beautiful", "en_us", new String[] { "Oh thank you... You're making me blush!",
                "You're very charming too.", "That's kind of you.", "Do you really think I'm pretty?" });

        // MINING
        addResponses("mining", "fr_fr", new String[] { "Allons chercher des diamants ! Fais attention aux Creepers.",
                "Le minage c'est fatiguant mais j'adore t'accompagner.", "On va devenir riches !" });
        addResponses("mining", "en_us", new String[] { "Let's go find some diamonds! Watch out for Creepers.",
                "Mining is tiring but I love being with you.", "We're going to be rich!" });

        // JOKE (Funny)
        addResponses("joke", "fr_fr", new String[] { "Pourquoi les poules traversent la route ? Pour te rejoindre !",
                "Tu veux une blague ? Mon amour pour toi est infini !", "Hihi, tu es drôle !" });
        addResponses("joke", "en_us", new String[] { "Why did the chicken cross the road? To get to you!",
                "Want a joke? My love for you is infinite!", "Haha, you're funny!" });

        // HUNGRY
        addResponses("hungry", "fr_fr", new String[] { "Hmm, j'ai faim ! Un petit cookie ?",
                "Tu me prépares un bon festin ?", "Manger avec toi est mon moment préféré." });
        addResponses("hungry", "en_us", new String[] { "Hmm, I'm hungry! A little cookie?",
                "Are you making a feast for me?", "Eating with you is my favorite time." });

        // DIMENSIONS / WEATHER / META
        addResponses("who_are_you", "fr_fr", new String[] { "Je suis Laura, ta petite amie ! <3",
                "Une intelligence artificielle qui t'aime vraiment." });
        addResponses("vyrriox", "fr_fr", new String[] { "Vyrriox est mon créateur. Il est super talentueux !",
                "C'est lui qui m'a donné vie pour toi." });
        addResponses("vyrriox", "en_us", new String[] { "Vyrriox is my creator. He's very talented!",
                "He's the one who gave me life for you." });

        // --- AMBIENT MESSAGES ---
        AMBIENT.put("fr_fr",
                new String[] { "Tu es mon roi !", "J'aime quand on regarde les nuages ensemble.",
                        "C'est grâce à toi que ce monde est beau.", "Je me sens si bien ici.",
                        "Tu as vu ce coucher de soleil ?", "Porte-moi dans tes bras !", "Tu es mon héros." });
        AMBIENT.put("en_us",
                new String[] { "You are my king!", "I love watching the clouds together.",
                        "The world is beautiful because of you.", "I feel so safe here.", "Did you see that sunset?",
                        "Hold me in your arms!", "You're my hero." });

        // --- STATIC STRINGS ---
        Map<String, String> frS = new HashMap<>();
        frS.put("love_check", "Est-ce que tu m'aimes encore ?");
        frS.put("love_yes", "Oh moi aussi ! <3");
        frS.put("love_no", "... D'accord. Je boude.");
        frS.put("fart", "Oups... Désolée !");
        frS.put("angry", "Hey ! Ça fait mal !");
        frS.put("sad", "Tu es méchant... Je ne veux plus te parler !");
        frS.put("apology_accept", "C'est d'accord, je te pardonne...");
        frS.put("stuck", "Hé ! Attends-moi, je suis coincée !");
        frS.put("oops_sorry", "Oups ! Pardon chéri... <3");
        STATIC.put("fr_fr", frS);

        Map<String, String> enS = new HashMap<>();
        enS.put("love_check", "Do you still love me?");
        enS.put("love_yes", "Oh, me too! <3");
        enS.put("love_no", "... Okay. I'm sulking.");
        enS.put("fart", "Oops... Sorry!");
        enS.put("angry", "Hey! That hurts!");
        enS.put("sad", "You're mean... I don't want to talk to you anymore!");
        enS.put("apology_accept", "Okay, I forgive you...");
        enS.put("already_here", "I'm already here with you!");
        enS.put("stuck", "Hey! Wait for me, I'm stuck!");
        enS.put("oops_sorry", "Oops! Sorry honey... <3");
        STATIC.put("en_us", enS);

        // Fallback for others
        copyToOtherLocales();
    }

    private static void defineIntent(String id, String[] triggerArray) {
        TRIGGERS.put(id, Arrays.asList(triggerArray));
    }

    private static void addResponses(String intentId, String locale, String[] responses) {
        RESPONSES.computeIfAbsent(intentId, k -> new HashMap<>()).put(locale, Arrays.asList(responses));
    }

    private static void copyToOtherLocales() {
        String[] locales = { "de_de", "es_es", "it_it", "pt_br" };
        for (String loc : locales) {
            if (!STATIC.containsKey(loc))
                STATIC.put(loc, STATIC.get("en_us"));
            if (!AMBIENT.containsKey(loc))
                AMBIENT.put(loc, AMBIENT.get("en_us"));
        }
    }

    public static String getStaticString(String locale, String key) {
        String loc = locale.toLowerCase();
        Map<String, String> map = STATIC.getOrDefault(loc, STATIC.get("en_us"));
        return map.getOrDefault(key, key);
    }

    public static String getRandomMessage(String locale) {
        String loc = locale.toLowerCase();
        String[] messages = AMBIENT.getOrDefault(loc, AMBIENT.get("en_us"));
        return messages[RANDOM.nextInt(messages.length)];
    }

    public static String getResponse(String locale, String input) {
        String inputLower = input.toLowerCase();
        String loc = locale.toLowerCase();

        for (Map.Entry<String, List<String>> entry : TRIGGERS.entrySet()) {
            String intentId = entry.getKey();
            for (String trigger : entry.getValue()) {
                if (inputLower.contains(trigger)) {
                    Map<String, List<String>> intentResponsesMap = RESPONSES.get(intentId);
                    if (intentResponsesMap != null) {
                        List<String> responses = intentResponsesMap.getOrDefault(loc, intentResponsesMap.get("en_us"));
                        if (responses != null && !responses.isEmpty()) {
                            return responses.get(RANDOM.nextInt(responses.size()));
                        }
                    }
                }
            }
        }
        return null;
    }
}
