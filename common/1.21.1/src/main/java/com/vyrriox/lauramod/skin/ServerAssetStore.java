package com.vyrriox.lauramod.skin;

import com.vyrriox.lauramod.LauraMod;
import com.vyrriox.lauramod.config.LauraConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Skins and models hosted by the server in {@code config/lauramod/skins} and
 * {@code config/lauramod/models}. In singleplayer this is the player's own folder, so local files
 * just work; on a server, clients download them on demand.
 *
 * @author vyrriox
 */
public final class ServerAssetStore {
    /** One skin or model. {@code files} are relative to {@code root}. */
    public record Entry(AssetKind kind, String name, String sha1, long size, Path root, List<Path> files) {
    }

    private static final Map<AssetKind, Map<String, Entry>> ENTRIES = new EnumMap<>(AssetKind.class);

    /** Most bytes of skins and model archives kept in memory between two scans. */
    private static final long PAYLOAD_CACHE_BYTES = 32L * 1024 * 1024;
    /** What {@link #read} returned, least recently used first. */
    private static final Map<String, byte[]> PAYLOADS = new LinkedHashMap<>(16, 0.75f, true);
    private static long payloadBytes;
    private static long diskReads;

    private ServerAssetStore() {
    }

    public static synchronized void init() {
        try {
            Path skins = dir(AssetKind.SKIN);
            Path models = dir(AssetKind.MODEL);
            Files.createDirectories(skins.resolve("uploads"));
            Files.createDirectories(models.resolve("uploads"));
            Path skinReadme = skins.resolve("README.txt");
            if (!Files.exists(skinReadme)) {
                Files.writeString(skinReadme, SKIN_README, StandardCharsets.UTF_8);
            }
            Path modelReadme = models.resolve("README.txt");
            if (!Files.exists(modelReadme)) {
                Files.writeString(modelReadme, MODEL_README, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            LauraMod.LOGGER.warn("Could not prepare the skin and model folders: {}", e.getMessage());
        }
        rescan();
    }

    public static Path dir(AssetKind kind) {
        return LauraMod.configDir().resolve(kind.folder);
    }

    public static synchronized void rescan() {
        scans++;
        for (AssetKind kind : AssetKind.values()) {
            Map<String, Entry> found = new LinkedHashMap<>();
            Path root = dir(kind);
            if (Files.isDirectory(root)) {
                try (Stream<Path> stream = Files.walk(root, 3)) {
                    List<Path> paths = stream.sorted().toList();
                    for (Path path : paths) {
                        Entry entry = kind == AssetKind.SKIN ? skinEntry(root, path) : modelEntry(root, path);
                        if (entry != null && !found.containsKey(entry.name())) {
                            found.put(entry.name(), entry);
                        }
                    }
                } catch (IOException e) {
                    LauraMod.LOGGER.warn("Could not scan {}: {}", root, e.getMessage());
                }
            }
            ENTRIES.put(kind, found);
        }
        // Files may have changed: what was read before this scan is not served again.
        PAYLOADS.clear();
        payloadBytes = 0;
        LauraMod.LOGGER.info("Server assets: {} skins, {} models", ENTRIES.get(AssetKind.SKIN).size(), ENTRIES.get(AssetKind.MODEL).size());
    }

    private static Entry skinEntry(Path root, Path path) throws IOException {
        if (!Files.isRegularFile(path) || !path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
            return null;
        }
        long size = Files.size(path);
        if (size > LauraConfig.maxSkinKb.getInt() * 1024L) {
            LauraMod.LOGGER.warn("Skin {} is larger than skins.maxSkinKb and is ignored", path.getFileName());
            return null;
        }
        String name = relativeName(root, path, ".png");
        if (name == null) {
            return null;
        }
        byte[] bytes = Files.readAllBytes(path);
        if (!isValidSkinPng(bytes)) {
            LauraMod.LOGGER.warn("Skin {} is not a valid skin PNG (64x64, 64x32 or an HD multiple)", path.getFileName());
            return null;
        }
        return new Entry(AssetKind.SKIN, name, sha1(bytes), size, root, List.of(root.relativize(path)));
    }

    private static Entry modelEntry(Path root, Path path) throws IOException {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        List<Path> files = new ArrayList<>();
        String name;
        Path base;
        if (Files.isDirectory(path) && !path.equals(root) && !path.getFileName().toString().equals("uploads")) {
            try (Stream<Path> children = Files.list(path)) {
                children.filter(Files::isRegularFile).filter(ServerAssetStore::isModelFile).sorted().forEach(files::add);
            }
            if (files.stream().noneMatch(p -> p.getFileName().toString().endsWith(".bbmodel") || p.getFileName().toString().endsWith(".geo.json"))) {
                return null;
            }
            name = relativeName(root, path, "");
            base = path;
        } else if (Files.isRegularFile(path) && fileName.endsWith(".bbmodel") && isTopLevelModelFile(root, path)) {
            files.add(path);
            name = relativeName(root, path, ".bbmodel");
            base = path.getParent();
        } else if (Files.isRegularFile(path) && fileName.endsWith(".geo.json") && isTopLevelModelFile(root, path)) {
            String stem = path.getFileName().toString().substring(0, path.getFileName().toString().length() - ".geo.json".length());
            files.add(path);
            Path anim = path.resolveSibling(stem + ".animation.json");
            Path texture = path.resolveSibling(stem + ".png");
            if (Files.isRegularFile(anim)) {
                files.add(anim);
            }
            if (Files.isRegularFile(texture)) {
                files.add(texture);
            }
            name = relativeName(root, path, ".geo.json");
            base = path.getParent();
        } else {
            return null;
        }
        if (name == null) {
            return null;
        }
        long size = 0;
        MessageDigest digest = digest();
        List<Path> relative = new ArrayList<>();
        for (Path f : files) {
            size += Files.size(f);
            digest.update(f.getFileName().toString().getBytes(StandardCharsets.UTF_8));
            digest.update(Files.readAllBytes(f));
            relative.add(base.relativize(f));
        }
        if (size > LauraConfig.maxModelKb.getInt() * 1024L) {
            LauraMod.LOGGER.warn("Model {} is larger than models.maxModelKb and is ignored", name);
            return null;
        }
        return new Entry(AssetKind.MODEL, name, HexFormat.of().formatHex(digest.digest()), size, base, relative);
    }

    /** Only files directly in models/ or models/uploads/ are models on their own; files inside a model folder belong to it. */
    private static boolean isTopLevelModelFile(Path root, Path path) {
        Path parent = path.getParent();
        return parent.equals(root) || (parent.getFileName().toString().equals("uploads") && parent.getParent().equals(root));
    }

    private static boolean isModelFile(Path p) {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".bbmodel") || n.endsWith(".json") || n.endsWith(".png");
    }

    private static String relativeName(Path root, Path path, String extension) {
        String rel = root.relativize(path).toString().replace('\\', '/');
        if (!extension.isEmpty() && rel.toLowerCase(Locale.ROOT).endsWith(extension)) {
            rel = rel.substring(0, rel.length() - extension.length());
        }
        return isSafeName(rel) ? rel : null;
    }

    /** Asset names travel over the network, keep them boring. */
    public static boolean isSafeName(String name) {
        if (name == null || name.isEmpty() || name.length() > 96 || name.contains("..") || name.startsWith("/")) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '/' || c == '.' || c == ' ')) {
                return false;
            }
        }
        return true;
    }

    public static synchronized Entry get(AssetKind kind, String name) {
        Map<String, Entry> map = ENTRIES.get(kind);
        return map == null ? null : map.get(name);
    }

    public static synchronized List<Entry> list(AssetKind kind) {
        Map<String, Entry> map = ENTRIES.get(kind);
        if (map == null) {
            return Collections.emptyList();
        }
        List<Entry> out = new ArrayList<>(map.values());
        out.sort(Comparator.comparing(Entry::name));
        return out;
    }

    /**
     * Skin: the PNG bytes. Model: a zip of its files. The result is kept until the next scan (up
     * to {@link #PAYLOAD_CACHE_BYTES} in total), so a request never costs more than one disk read
     * per file. Callers must not modify the array.
     */
    public static synchronized byte[] read(Entry entry) {
        String key = entry.kind().name() + "|" + entry.name() + "#" + entry.sha1();
        byte[] cached = PAYLOADS.get(key);
        if (cached != null) {
            return cached;
        }
        byte[] bytes = readFromDisk(entry);
        if (bytes != null && bytes.length <= PAYLOAD_CACHE_BYTES) {
            PAYLOADS.put(key, bytes);
            payloadBytes += bytes.length;
            var oldest = PAYLOADS.entrySet().iterator();
            while (payloadBytes > PAYLOAD_CACHE_BYTES && oldest.hasNext()) {
                payloadBytes -= oldest.next().getValue().length;
                oldest.remove();
            }
        }
        return bytes;
    }

    /** Number of times an asset was really read from disk since the game started. */
    public static synchronized long diskReads() {
        return diskReads;
    }

    private static byte[] readFromDisk(Entry entry) {
        diskReads++;
        try {
            if (entry.kind() == AssetKind.SKIN) {
                return Files.readAllBytes(entry.root().resolve(entry.files().get(0)));
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                for (Path rel : entry.files()) {
                    ZipEntry ze = new ZipEntry(rel.getFileName().toString());
                    ze.setTime(0L);
                    zip.putNextEntry(ze);
                    zip.write(Files.readAllBytes(entry.root().resolve(rel)));
                    zip.closeEntry();
                }
            }
            return out.toByteArray();
        } catch (IOException e) {
            LauraMod.LOGGER.warn("Could not read asset {}: {}", entry.name(), e.getMessage());
            return null;
        }
    }

    /** True if the uploads folder already holds a file with this stem (it will be replaced, not added). */
    public static synchronized boolean hasUpload(AssetKind kind, String fileStem) {
        return Files.isRegularFile(dir(kind).resolve("uploads").resolve(fileStem + uploadExtension(kind)));
    }

    /** Number of uploaded files whose name starts with the prefix (the uploads of one player). */
    public static synchronized int countUploads(AssetKind kind, String prefix) {
        Path dir = dir(kind).resolve("uploads");
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        String extension = uploadExtension(kind);
        try (java.util.stream.Stream<Path> files = Files.list(dir)) {
            return (int) files.map(p -> p.getFileName().toString()).filter(n -> n.startsWith(prefix) && n.endsWith(extension)).count();
        } catch (IOException e) {
            LauraMod.LOGGER.warn("Could not list uploads: {}", e.getMessage());
            // Unknown count: refuse rather than let the folder grow without limit.
            return Integer.MAX_VALUE;
        }
    }

    private static String uploadExtension(AssetKind kind) {
        return kind == AssetKind.SKIN ? ".png" : ".bbmodel";
    }

    /**
     * Saves an uploaded file in the uploads folder and returns its entry. Only that one entry is
     * built (no scan of the folders), and nothing is written when the stored file is already the same.
     */
    public static synchronized Entry storeUpload(AssetKind kind, String fileStem, byte[] data) throws IOException {
        Path root = dir(kind);
        Path dir = root.resolve("uploads");
        Files.createDirectories(dir);
        String fileName = fileStem + uploadExtension(kind);
        Path target = dir.resolve(fileName);
        String name = "uploads/" + fileStem;
        Entry known = get(kind, name);
        if (known != null && known.sha1().equals(uploadHash(kind, fileName, data)) && Files.isRegularFile(target)) {
            return known;
        }
        Files.write(target, data);
        storeWrites++;
        Entry entry = kind == AssetKind.SKIN ? skinEntry(root, target) : modelEntry(root, target);
        Map<String, Entry> entries = ENTRIES.computeIfAbsent(kind, k -> new LinkedHashMap<>());
        if (entry == null) {
            entries.remove(name);
        } else {
            entries.put(entry.name(), entry);
        }
        return entry;
    }

    /** The hash a rescan would give this upload: the bytes of a skin, the file name and the bytes of a model. */
    private static String uploadHash(AssetKind kind, String fileName, byte[] data) {
        if (kind == AssetKind.SKIN) {
            return sha1(data);
        }
        MessageDigest digest = digest();
        digest.update(fileName.getBytes(StandardCharsets.UTF_8));
        digest.update(data);
        return HexFormat.of().formatHex(digest.digest());
    }

    private static int storeWrites;
    private static int scans;

    /** Upload files written since the game started (read by the self tests). */
    public static synchronized int storeWrites() {
        return storeWrites;
    }

    /** Folder scans done since the game started (read by the self tests). */
    public static synchronized int scans() {
        return scans;
    }

    public static boolean isValidSkinPng(byte[] data) {
        if (data.length < 24) {
            return false;
        }
        byte[] magic = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) {
                return false;
            }
        }
        int width = readInt(data, 16);
        int height = readInt(data, 20);
        return width >= 64 && width <= 1024 && width % 64 == 0 && (height == width || height * 2 == width);
    }

    private static int readInt(byte[] b, int offset) {
        return ((b[offset] & 0xFF) << 24) | ((b[offset + 1] & 0xFF) << 16) | ((b[offset + 2] & 0xFF) << 8) | (b[offset + 3] & 0xFF);
    }

    public static String sha1(byte[] data) {
        return HexFormat.of().formatHex(digest().digest(data));
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final String SKIN_README = """
            My Girlfriend Laura - server skins
            ===================================

            Drop Minecraft skin files (PNG, 64x64, 64x32 or HD multiples like 128x128) in this folder.
            Players pick them in Laura's skin screen, or with /laura skin file <name>.
            Every client downloads the file from the server, so everybody sees the same Laura.

            Sub-folders are allowed (their path becomes part of the name).
            The uploads folder receives the skins players drag and drop into the skin screen
            (can be disabled with skins.allowUploads in lauramod-common.json).
            Reload with /laura reload.
            """;

    private static final String MODEL_README = """
            My Girlfriend Laura - custom models (Blockbench)
            ================================================

            Supported formats:
              - <name>.bbmodel            Blockbench project (textures and animations are embedded)
              - <name>.geo.json           Bedrock / GeckoLib geometry, with optional
                <name>.animation.json     animations and
                <name>.png                texture next to it
              - <name>/                   a folder holding one of the above

            Players pick a model in Laura's menu or with /laura model <name>.
            Clients download models from the server, so everybody sees the same Laura.

            Animation names are matched by their last part: "animation.laura.walk" is used for walking.
            Recognized names: idle, walk, sit, sleep, swim, sad, angry, happy, hungry, tired, gagged, carry,
            carry_walk, eat, and every emote of the emote wheel (wave, hug, kiss, dance, clap, laugh, cry,
            blush, facepalm, jump, bow, think, shrug, stomp, yawn, celebrate, snap, twirl, hum, stretch...).
            Missing animations fall back to idle or are skipped. A model with no animation at all borrows
            the default ones, and its arms and legs swing when it walks.

            Bones are found by name, in English or not (head/tete/kopf, body/corps, right_arm/bras_d,
            left_leg/jambe_g...). Left and right are read from where the bones are, not from their names.
            They are used for the hay gag (head), the bag on her back (body) and held items (right_hand /
            left_hand bones if the model has them, otherwise the arms).
            See docs/MODELS.md in the project repository for the full guide.
            """;
}
