package com.vyrriox.lauramod.util;

import java.util.*;

public class InteractionDatabase {
    private static final Random RANDOM = new Random();

    // Map of Intent ID -> Localized Response Map (Locale -> Response)
    private static final Map<String, Map<String, String>> RESPONSES = new HashMap<>();
    // Map of Intent ID -> List of Triggers (Keywords) across all languages
    private static final Map<String, List<String>> TRIGGERS = new HashMap<>();

    // Ambient messages Map (Locale -> String[])
    private static final Map<String, String[]> AMBIENT = new HashMap<>();

    // Static strings Map (Locale -> KeyValueMap)
    private static final Map<String, Map<String, String>> STATIC = new HashMap<>();

    static {
        // --- DEFINE INTENTS & TRIGGERS ---
        // GREETING
        defineIntent("greeting", new String[] {
                "bonjour", "salut", "coucou", "hello", "hi", "hey", "hallo", "moin", "hola", "buenos dias", "ciao",
                "buongiorno", "ola", "bom dia"
        });
        // HOW_ARE_YOU
        defineIntent("how_are_you", new String[] {
                "ça va ?", "comment tu vas ?", "how are you", "how are you doing", "wie geht es dir", "wie geht's",
                "¿cómo estás?", "como estas", "come stai", "como voce esta"
        });
        // LOVE_YOU
        defineIntent("love_you", new String[] {
                "je t'aime", "i love you", "ich liebe dich", "te amo", "ti amo", "te quiero"
        });
        // BEAUTIFUL
        defineIntent("beautiful", new String[] {
                "tu es belle", "t'es belle", "tu es magnifique", "you are beautiful", "you're beautiful",
                "du bist schön", "eres hermosa", "sei bellissima", "voce e linda"
        });
        // MARRY_ME
        defineIntent("marry_me", new String[] {
                "veux-tu m'épouser", "marry me", "willst du mich heiraten", "¿quieres casarte conmigo?",
                "vuoi sposarmi", "quer casar comigo"
        });
        // MINING
        defineIntent("mining", new String[] {
                "on va miner", "allons miner", "let's go mining", "let's mine", "gehen wir minen", "vamos a minar",
                "andiamo a minare", "vamos minerar"
        });

        // --- RESPONSES ---
        addResponse("greeting", "fr_fr", "Coucou toi ! Comment se passe ta journée ?");
        addResponse("greeting", "en_us", "Hi there! How is your day going?");
        addResponse("greeting", "de_de", "Hallo! Wie läuft dein Tag so?");
        addResponse("greeting", "es_es", "¡Hola! ¿Cómo va tu día?");
        addResponse("greeting", "it_it", "Ciao! Come sta andando la tua giornata?");
        addResponse("greeting", "pt_br", "Oi! Como está sendo o seu dia?");

        addResponse("how_are_you", "fr_fr", "Je me sens merveilleusement bien avec toi !");
        addResponse("how_are_you", "en_us", "I feel wonderful when I'm with you!");
        addResponse("how_are_you", "de_de", "Ich fühle mich wundervoll, wenn ich bei dir bin!");
        addResponse("how_are_you", "es_es", "¡Me siento maravillosamente cuando estoy contigo!");
        addResponse("how_are_you", "it_it", "Mi sento divinamente quando sono con te!");
        addResponse("how_are_you", "pt_br", "Eu me sinto maravilhosa quando estou com você!");

        addResponse("love_you", "fr_fr", "Moi aussi je t'aime, plus que tout ! <3");
        addResponse("love_you", "en_us", "I love you too, more than anything! <3");
        addResponse("love_you", "de_de", "Ich liebe dich auch, mehr als alles andere! <3");
        addResponse("love_you", "es_es", "¡Yo también te amo, más que a nada! <3");
        addResponse("love_you", "it_it", "Anch'io ti amo, più di ogni altra cosa! <3");
        addResponse("love_you", "pt_br", "Eu também te amo, mais do que tudo! <3");

        addResponse("beautiful", "fr_fr", "Oh merci... Tu vas me faire rougir !");
        addResponse("beautiful", "en_us", "Oh thank you... You're making me blush!");
        addResponse("beautiful", "de_de", "Oh danke... du machst mich ganz verlegen!");
        addResponse("beautiful", "es_es", "¡Oh gracias... me vas a hacer sonrojar!");
        addResponse("beautiful", "it_it", "Oh grazie... mi fai arrossire!");
        addResponse("beautiful", "pt_br", "Ah, obrigada... você está me fazendo corar!");

        addResponse("marry_me", "fr_fr", "Oh oui ! Je veux passer le reste de ma vie avec toi !");
        addResponse("marry_me", "en_us", "Oh yes! I want to spend the rest of my life with you!");
        addResponse("marry_me", "de_de", "Oh ja! Ich möchte den Rest meines Lebens mit dir verbringen!");
        addResponse("marry_me", "es_es", "¡Oh, sí! ¡Quiero pasar le reste de mi vida contigo!");
        addResponse("marry_me", "it_it", "Oh sì! Voglio passare il resto della mia vita con te!");
        addResponse("marry_me", "pt_br", "Ah, sim! Eu quero passar o resto da minha vida com você!");

        addResponse("mining", "fr_fr", "Allons chercher des diamants !");
        addResponse("mining", "en_us", "Let's go find some diamonds!");
        addResponse("mining", "de_de", "Lass uns ein paar Diamanten finden!");
        addResponse("mining", "es_es", "¡Vamos a buscar diamantes!");
        addResponse("mining", "it_it", "Andiamo a cercare dei diamanti!");
        addResponse("mining", "pt_br", "Vamos encontrar alguns diamantes!");

        // --- AMBIENT MESSAGES ---
        AMBIENT.put("fr_fr", new String[] { "Tu es mon roi !", "J'aime quand on regarde les nuages ensemble.",
                "C'est grâce à toi que ce monde est beau." });
        AMBIENT.put("en_us", new String[] { "You are my king!", "I love watching the clouds together.",
                "The world is beautiful because of you." });
        AMBIENT.put("de_de", new String[] { "Du bist mein König!", "Ich liebe es, gemeinsam die Wolken zu beobachten.",
                "Die Welt ist schön, weil es dich gibt." });
        AMBIENT.put("es_es", new String[] { "¡Eres mi rey!", "Me encanta mirar las nubes juntos.",
                "El mundo es hermoso gracias a ti." });
        AMBIENT.put("it_it", new String[] { "Sei il mio re!", "Amo guardare le nuvole insieme.",
                "Il mondo è bellissimo grazie a te." });
        AMBIENT.put("pt_br",
                new String[] { "Você é meu rei!", "Eu amo olhar as nuvens juntos.", "O mundo é lindo por sua causa." });

        // --- STATIC STRINGS ---
        Map<String, String> frS = new HashMap<>();
        frS.put("love_check", "Est-ce que tu m'aimes encore ?");
        frS.put("love_yes", "Oh moi aussi ! <3");
        frS.put("love_no", "... D'accord. Je boude.");
        frS.put("fart", "Oups... Désolée !");
        frS.put("angry", "Hey ! Ça fait mal !");
        frS.put("sad", "Tu es méchant... Je ne veux plus te parler !");
        frS.put("apology_accept", "C'est d'accord, je te pardonne...");
        frS.put("already_here", "Je suis déjà là avec toi !");
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
        STATIC.put("en_us", enS);

        Map<String, String> deS = new HashMap<>();
        deS.put("love_check", "Liebst du mich noch?");
        deS.put("love_yes", "Oh, ich dich auch! <3");
        deS.put("love_no", "... Okay. Ich schmolle.");
        deS.put("fart", "Ups... Entschuldigung!");
        deS.put("angry", "Hey! Das tut weh!");
        deS.put("sad", "Du bist gemein... ich will nicht mehr mit dir reden!");
        deS.put("apology_accept", "In Ordnung, ich verzeihe dir...");
        deS.put("already_here", "Ich bin schon hier bei dir!");
        STATIC.put("de_de", deS);

        Map<String, String> esS = new HashMap<>();
        esS.put("love_check", "¿Todavía me amas?");
        esS.put("love_yes", "¡Oh, yo también! <3");
        esS.put("love_no", "... De acuerdo. Estoy deprimida.");
        esS.put("fart", "¡Ups... lo siento!");
        esS.put("angry", "¡Oye! ¡Eso duele!");
        esS.put("sad", "Eres malo... ¡ya no quiero hablar contigo!");
        esS.put("apology_accept", "Está bien, te perdono...");
        esS.put("already_here", "¡Ya estoy aquí contigo!");
        STATIC.put("es_es", esS);

        Map<String, String> itS = new HashMap<>();
        itS.put("love_check", "Mi ami ancora?");
        itS.put("love_yes", "Oh, anch'io! <3");
        itS.put("love_no", "... Va bene. Tengo il muso.");
        itS.put("fart", "Ops... scusa!");
        itS.put("angry", "Ehi! Fa male!");
        itS.put("sad", "Sei cattivo... non voglio più parlarti!");
        itS.put("apology_accept", "Va bene, ti perdono...");
        itS.put("already_here", "Sono già qui con te!");
        STATIC.put("it_it", itS);

        Map<String, String> ptS = new HashMap<>();
        ptS.put("love_check", "Você ainda me ama?");
        ptS.put("love_yes", "Oh, eu também! <3");
        ptS.put("love_no", "... Tudo bem. Estou de bico.");
        ptS.put("fart", "Ops... desculpe!");
        ptS.put("angry", "Ei! Isso dói!");
        ptS.put("sad", "Você é mau... não quero mais falar com você!");
        ptS.put("apology_accept", "Tudo bem, eu te perdoo...");
        ptS.put("already_here", "Eu já estou aqui com você!");
        STATIC.put("pt_br", ptS);

        // (Other languages omitted for brevity in code but would be fully populated)
        copyToOtherLocales();
    }

    private static void defineIntent(String id, String[] triggerArray) {
        TRIGGERS.put(id, Arrays.asList(triggerArray));
    }

    private static void addResponse(String intentId, String locale, String response) {
        RESPONSES.computeIfAbsent(intentId, k -> new HashMap<>()).put(locale, response);
    }

    private static void copyToOtherLocales() {
        // Fallback static strings for DE, ES, IT, PT if not explicitly set
        String[] locales = { "de_de", "es_es", "it_it", "pt_br" };
        for (String loc : locales) {
            if (!STATIC.containsKey(loc)) {
                STATIC.put(loc, STATIC.get("en_us"));
            }
            if (!AMBIENT.containsKey(loc)) {
                AMBIENT.put(loc, AMBIENT.get("en_us"));
            }
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
                    Map<String, String> intentResponses = RESPONSES.get(intentId);
                    if (intentResponses != null) {
                        return intentResponses.getOrDefault(loc, intentResponses.get("en_us"));
                    }
                }
            }
        }
        return null;
    }
}
