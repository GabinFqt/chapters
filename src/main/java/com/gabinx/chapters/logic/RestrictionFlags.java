package com.gabinx.chapters.logic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Per-entry / per-stage toggles for lock behaviours. Every flag defaults to {@code true}
 * so legacy datapacks keep the previous full-enforcement behaviour.
 */
public record RestrictionFlags(
        boolean pickup,
        boolean inventory,
        boolean craft,
        boolean jei,
        boolean transfer,
        boolean travel
) {
    public static final RestrictionFlags ALL =
            new RestrictionFlags(true, true, true, true, true, true);

    public boolean allows(Restriction restriction) {
        return switch (restriction) {
            case PICKUP -> pickup;
            case INVENTORY -> inventory;
            case CRAFT -> craft;
            case JEI -> jei;
            case TRANSFER -> transfer;
            case TRAVEL -> travel;
        };
    }

    /** OR-merge: a restriction stays enabled if either side enables it. */
    public RestrictionFlags or(RestrictionFlags other) {
        if (other == null) {
            return this;
        }
        return new RestrictionFlags(
                pickup || other.pickup,
                inventory || other.inventory,
                craft || other.craft,
                jei || other.jei,
                transfer || other.transfer,
                travel || other.travel
        );
    }

    /**
     * Applies optional boolean overrides from a JSON object (stage defaults or entry object).
     * Missing keys leave the base value unchanged.
     */
    public RestrictionFlags withOverrides(JsonObject json) {
        if (json == null) {
            return this;
        }
        return new RestrictionFlags(
                boolOr(json, "pickup", pickup),
                boolOr(json, "inventory", inventory),
                boolOr(json, "craft", craft),
                boolOr(json, "jei", jei),
                boolOr(json, "transfer", transfer),
                boolOr(json, "travel", travel)
        );
    }

    public static RestrictionFlags fromJson(JsonObject json) {
        return ALL.withOverrides(json);
    }

    private static boolean boolOr(JsonObject json, String key, boolean fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        JsonElement el = json.get(key);
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isBoolean()) {
            return fallback;
        }
        return el.getAsBoolean();
    }
}
