package com.vyrriox.lauramod.skin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Rules for the files a client keeps from a server ({@code lauramod/cache}). Everything a server
 * sends is untrusted: the hash it announces names the cached file, so it must be a real SHA-1 and
 * the file must stay inside the cache folder, and only content that matches its hash is kept.
 * <p>
 * No client class is used here, so the self tests can check these rules on a dedicated server.
 *
 * @author vyrriox
 */
public final class AssetCache {
    /** Length of a SHA-1 written in hexadecimal. */
    public static final int SHA1_LENGTH = 40;
    /** Files a model archive may hold (a model, its animations and its textures). */
    public static final int MAX_ZIP_ENTRIES = 256;

    private AssetCache() {
    }

    /** True for 40 lower case hexadecimal characters, the only form the server writes. */
    public static boolean isSha1(String hash) {
        if (hash == null || hash.length() != SHA1_LENGTH) {
            return false;
        }
        for (int i = 0; i < SHA1_LENGTH; i++) {
            char c = hash.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'f')) {
                return false;
            }
        }
        return true;
    }

    /**
     * The cache file for a hash, or null when the hash is not a SHA-1 or the file would not sit
     * directly in {@code dir}. Nothing is read or written here.
     */
    public static Path file(Path dir, String sha1, String extension) {
        if (dir == null || !isSha1(sha1)) {
            return null;
        }
        Path base = dir.toAbsolutePath().normalize();
        Path target;
        try {
            target = base.resolve(sha1 + extension).normalize();
        } catch (RuntimeException e) {
            return null;
        }
        return base.equals(target.getParent()) ? target : null;
    }

    /** True if the bytes are the skin the hash stands for. */
    public static boolean matchesSkin(String sha1, byte[] png) {
        return isSha1(sha1) && png != null && sha1.equals(ServerAssetStore.sha1(png));
    }

    /**
     * Reads a model archive in memory, entry names as written by the server, in their order.
     * Stops with an error past {@code maxBytes} of content or {@link #MAX_ZIP_ENTRIES} files, so
     * a small archive cannot expand into a huge one.
     */
    public static Map<String, byte[]> unzip(byte[] zip, long maxBytes) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        long total = 0;
        byte[] buffer = new byte[8192];
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (files.size() >= MAX_ZIP_ENTRIES) {
                    throw new IOException("too many files in the model");
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int n;
                while ((n = in.read(buffer)) > 0) {
                    total += n;
                    if (total > maxBytes) {
                        throw new IOException("model too large");
                    }
                    out.write(buffer, 0, n);
                }
                if (files.put(entry.getName(), out.toByteArray()) != null) {
                    throw new IOException("the model holds two files with the same name");
                }
            }
        }
        return files;
    }

    /**
     * The hash the server gives a model: every file in order, its name then its content. Same
     * computation as the server's scan of its models folder.
     */
    public static String modelDigest(Map<String, byte[]> files) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            digest.update(file.getKey().getBytes(StandardCharsets.UTF_8));
            digest.update(file.getValue());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** True if the unpacked archive is the model the hash stands for. */
    public static boolean matchesModel(String sha1, Map<String, byte[]> files) {
        return isSha1(sha1) && files != null && sha1.equals(modelDigest(files));
    }
}
