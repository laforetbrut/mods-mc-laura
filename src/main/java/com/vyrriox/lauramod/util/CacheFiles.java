package com.vyrriox.lauramod.util;

import com.vyrriox.lauramod.LauraMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Upkeep of a download cache folder (skins and models a client fetched): files are kept while they
 * are used, old ones are deleted and the folder stays under a size limit.
 *
 * @author vyrriox
 */
public final class CacheFiles {
    private CacheFiles() {
    }

    private record Cached(Path path, long modified, long size) {
    }

    /**
     * Deletes the files of the folder (not of its sub-folders) last used more than {@code maxAgeMs}
     * ago, then the oldest ones until the folder holds at most {@code maxBytes}. Returns how many
     * files were deleted.
     */
    public static int prune(Path dir, long maxAgeMs, long maxBytes) {
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        List<Cached> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            for (Path path : stream.toList()) {
                if (Files.isRegularFile(path)) {
                    files.add(new Cached(path, Files.getLastModifiedTime(path).toMillis(), Files.size(path)));
                }
            }
        } catch (IOException e) {
            LauraMod.LOGGER.debug("Could not list cache folder {}: {}", dir, e.getMessage());
            return 0;
        }
        files.sort(Comparator.comparingLong(Cached::modified));
        long now = System.currentTimeMillis();
        long total = 0;
        for (Cached file : files) {
            total += file.size();
        }
        int deleted = 0;
        for (Cached file : files) {
            if (now - file.modified() <= maxAgeMs && total <= maxBytes) {
                break;
            }
            try {
                Files.deleteIfExists(file.path());
                total -= file.size();
                deleted++;
            } catch (IOException e) {
                LauraMod.LOGGER.debug("Could not delete cached file {}: {}", file.path(), e.getMessage());
            }
        }
        return deleted;
    }

    /** Marks a cached file as used now, so {@link #prune} keeps it. */
    public static void touch(Path file) {
        try {
            Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
        } catch (IOException e) {
            LauraMod.LOGGER.debug("Could not touch cached file {}: {}", file, e.getMessage());
        }
    }
}
