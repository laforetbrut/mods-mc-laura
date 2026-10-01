package com.vyrriox.lauramod.model;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Finds which bones of a custom model are the head, body, arms and legs, from their names in the
 * usual conventions (Bedrock "bipedHead", "right_arm", "RightArm"...) and in several languages
 * ("tete", "bras_d", "jambe_gauche", "kopf", "brazo_derecho"...). Models made by players rarely
 * follow one naming scheme, and these parts are what the default animations, the walking legs,
 * the head looking around and the held items need.
 *
 * <p>Names are compared without case, accents, spaces, dashes or underscores. When several bones
 * match, the one closest to the top of the hierarchy wins ("tete" before "tete_peau"). Names tell
 * arms from legs; which one is the right one is then read from their positions.
 *
 * @author vyrriox
 */
public final class HumanoidParts {
    public static final String HEAD = "head";
    public static final String BODY = "body";
    public static final String RIGHT_ARM = "right_arm";
    public static final String LEFT_ARM = "left_arm";
    public static final String RIGHT_LEG = "right_leg";
    public static final String LEFT_LEG = "left_leg";
    public static final String ROOT = "root";

    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        alias(HEAD, "head", "bipedhead", "tete", "kopf", "cabeza", "testa", "cabeca", "hoofd", "glowa", "huvud", "kafa", "kepala");
        alias(BODY, "body", "bipedbody", "torso", "chest", "corps", "torse", "korper", "rumpf", "cuerpo", "corpo", "tronco",
                "lichaam", "romp", "cialo", "tulwa", "kropp", "govde", "badan");
        alias(RIGHT_ARM, "rightarm", "armright", "bipedrightarm", "armr", "rarm", "rightupperarm",
                "brasd", "brasdroit", "rechterarm", "armrechts", "rechterarm", "brazod", "brazoderecho", "brazoder",
                "bracciod", "bracciodestro", "bracod", "bracodireito", "prawareka", "rekaprawa", "hogerarm", "sagkol", "lengankanan");
        alias(LEFT_ARM, "leftarm", "armleft", "bipedleftarm", "arml", "larm", "leftupperarm",
                "brasg", "brasgauche", "linkerarm", "armlinks", "brazoi", "brazoizquierdo", "brazoizq",
                "braccios", "bracciosinistro", "bracoe", "bracoesquerdo", "lewareka", "rekalewa", "vansterarm", "solkol", "lengankiri");
        alias(RIGHT_LEG, "rightleg", "legright", "bipedrightleg", "legr", "rleg", "rightupperleg",
                "jambed", "jambedroite", "rechterbeen", "beinrechts", "rechtesbein", "piernad", "piernaderecha", "piernader",
                "gambad", "gambadestra", "pernad", "pernadireita", "prawanoga", "noga prawa", "hogerben", "sagbacak", "kakikanan");
        alias(LEFT_LEG, "leftleg", "legleft", "bipedleftleg", "legl", "lleg", "leftupperleg",
                "jambeg", "jambegauche", "linkerbeen", "beinlinks", "linkesbein", "piernai", "piernaizquierda", "piernaizq",
                "gambas", "gambasinistra", "pernae", "pernaesquerda", "lewanoga", "noga lewa", "vansterben", "solbacak", "kakikiri");
        alias(ROOT, "root", "racine", "raiz", "radice", "wurzel", "wortel", "korzen");
    }

    private HumanoidParts() {
    }

    private static void alias(String part, String... names) {
        for (String name : names) {
            ALIASES.putIfAbsent(normalize(name), part);
        }
    }

    /** Lower case, no accent, letters and digits only: "Tête_Peau" becomes "tetepeau". */
    public static String normalize(String name) {
        String plain = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** The part a bone name stands for, or null. */
    public static String partOf(String boneName) {
        return ALIASES.get(normalize(boneName));
    }

    /** Part name to bone, for the parts this model has. */
    public static Map<String, ModelData.Bone> find(List<ModelData.Bone> roots) {
        Map<String, ModelData.Bone> found = new HashMap<>();
        // Breadth first, so a group wins over the sub groups that share its name.
        Deque<ModelData.Bone> queue = new ArrayDeque<>(roots);
        while (!queue.isEmpty()) {
            ModelData.Bone bone = queue.poll();
            String part = partOf(bone.name);
            if (part != null) {
                found.putIfAbsent(part, bone);
            }
            queue.addAll(bone.children);
        }
        bySide(found, RIGHT_ARM, LEFT_ARM);
        bySide(found, RIGHT_LEG, LEFT_LEG);
        return Map.copyOf(found);
    }

    /**
     * Left and right are decided by where the bones are, not by their names: a model faces north,
     * so its right side is the one with the larger X pivot. Many models label the sides the other
     * way round (Java and Bedrock files put the right arm at negative X, a Blockbench project does
     * not), and they would otherwise wave and hold items with the wrong hand.
     */
    private static void bySide(Map<String, ModelData.Bone> found, String right, String left) {
        ModelData.Bone r = found.get(right);
        ModelData.Bone l = found.get(left);
        if (r != null && l != null && r.pivotX < l.pivotX) {
            found.put(right, l);
            found.put(left, r);
        }
    }
}
