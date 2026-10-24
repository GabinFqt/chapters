package com.gabinx.chapters.logic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Per-entry / per-stage toggles for lock behaviours. Every flag defaults to {@code true}
 * so stages without an explicit {@code restrictions} object keep full enforcement.
 * <p>
 * JSON / KubeJS keys describe what is <em>activated</em> while the content stays locked:
 * {@code preventPickup}, {@code dropFromInventory}, {@code preventCraft}, {@code hideInJei},
 * {@code preventTransfer}, {@code preventTravel}.
 */
public record RestrictionFlags(
        boolean preventPickup,
        boolean dropFromInventory,
        boolean preventCraft,
        boolean hideInJei,
        boolean preventTransfer,
        boolean preventTravel
) {
    public static final RestrictionFlags ALL =
            new RestrictionFlags(true, true, true, true, true, true);

    public boolean allows(Restriction restriction) {
        return switch (restriction) {
            case PICKUP -> preventPickup;
            case INVENTORY -> dropFromInventory;
            case CRAFT -> preventCraft;
            case JEI -> hideInJei;
            case TRANSFER -> preventTransfer;
            case TRAVEL -> preventTravel;
        };
    }

    /** OR-merge: a restriction stays enabled if either side enables it. */
    public RestrictionFlags or(RestrictionFlags other) {
        if (other == null) {
            return this;
        }
        return new RestrictionFlags(
                preventPickup || other.preventPickup,
                dropFromInventory || other.dropFromInventory,
                preventCraft || other.preventCraft,
                hideInJei || other.hideInJei,
                preventTransfer || other.preventTransfer,
                preventTravel || other.preventTravel
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
                boolOr(json, "preventPickup", preventPickup),
                boolOr(json, "dropFromInventory", dropFromInventory),
                boolOr(json, "preventCraft", preventCraft),
                boolOr(json, "hideInJei", hideInJei),
                boolOr(json, "preventTransfer", preventTransfer),
                boolOr(json, "preventTravel", preventTravel)
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
