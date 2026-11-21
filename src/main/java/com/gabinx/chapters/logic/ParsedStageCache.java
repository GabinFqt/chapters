package com.gabinx.chapters.logic;

import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parsed form of string stage sets (FTB Teams {@code TEAM_STAGES}), reused while the strings are unchanged.
 * Validation compares contents rather than identity because other mods may mutate the stored set in place.
 */
public final class ParsedStageCache<K> {
    private final Map<K, Entry> entries = new ConcurrentHashMap<>();

    /** Unmodifiable stage ids parsed from {@code raw}; invalid ids are skipped. */
    public Set<Identifier> get(K key, Set<String> raw) {
        Entry cached = entries.get(key);
        if (cached != null && cached.raw.equals(raw)) {
            return cached.parsed;
        }
        Set<String> rawCopy = new HashSet<>(raw);
        Set<Identifier> parsed = new LinkedHashSet<>();
        for (String s : rawCopy) {
            Identifier id = Identifier.tryParse(s);
            if (id != null) {
                parsed.add(id);
            }
        }
        Set<Identifier> frozen = Collections.unmodifiableSet(parsed);
        entries.put(key, new Entry(rawCopy, frozen));
        return frozen;
    }

    public void clear() {
        entries.clear();
    }

    private record Entry(Set<String> raw, Set<Identifier> parsed) {
    }
}
