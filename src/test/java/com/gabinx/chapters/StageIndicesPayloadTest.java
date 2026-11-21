package com.gabinx.chapters;

import com.gabinx.chapters.logic.StageBook;
import com.gabinx.chapters.network.ClientboundStageIndicesPayload;
import com.gabinx.chapters.stage.StageDefinition;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.gabinx.chapters.TutorialFixtures.DIAMOND;
import static com.gabinx.chapters.TutorialFixtures.EMERALD;
import static com.gabinx.chapters.TutorialFixtures.HYDROGEN;
import static com.gabinx.chapters.TutorialFixtures.LAVA;
import static com.gabinx.chapters.TutorialFixtures.WATER;
import static com.gabinx.chapters.TutorialFixtures.id;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageIndicesPayloadTest {
    @Test
    void tutorialIndicesRoundTrip() throws IOException {
        assertRoundTrip(TutorialFixtures.loadTutorialBook());
    }

    @Test
    void flagSubsetsRoundTripPerStage() {
        Identifier hard = id("tutorial:hard");
        Identifier soft = id("tutorial:soft");

        JsonObject hardJson = new JsonObject();
        hardJson.add("items", array(DIAMOND.toString(), EMERALD.toString()));
        hardJson.add("fluids", array(WATER.toString()));
        hardJson.add("chemicals", array(HYDROGEN.toString()));
        hardJson.add("recipes", array("minecraft:diamond_pickaxe"));

        JsonObject softJson = new JsonObject();
        JsonObject restrictions = new JsonObject();
        restrictions.addProperty("hideInJei", false);
        restrictions.addProperty("preventTransfer", false);
        softJson.add("restrictions", restrictions);
        softJson.add("items", array(DIAMOND.toString()));
        softJson.add("fluids", array(WATER.toString(), LAVA.toString()));
        softJson.add("chemicals", array(HYDROGEN.toString()));
        softJson.add("recipes", array("minecraft:diamond_pickaxe"));

        Map<Identifier, StageDefinition> defs = new LinkedHashMap<>();
        defs.put(hard, StageDefinition.fromJson(hard, hardJson));
        defs.put(soft, StageDefinition.fromJson(soft, softJson));
        StageBook book = new StageBook(TutorialFixtures.tutorialCatalog());
        book.replaceDatapack(defs);

        ClientboundStageIndicesPayload decoded = assertRoundTrip(book);
        assertEquals(Set.of(hard, soft), decoded.itemStages().get(DIAMOND));
        assertEquals(Set.of(hard), decoded.itemJeiStages().get(DIAMOND));
        assertEquals(Set.of(hard), decoded.fluidTransferStages().get(WATER));
        assertFalse(decoded.fluidJeiStages().containsKey(LAVA));
        assertFalse(decoded.fluidTransferStages().containsKey(LAVA));
        assertTrue(decoded.fluidStages().containsKey(LAVA));
    }

    private static ClientboundStageIndicesPayload assertRoundTrip(StageBook book) {
        ClientboundStageIndicesPayload payload = new ClientboundStageIndicesPayload(
                book.itemStagesIndexView(),
                book.fluidStagesIndexView(),
                book.chemicalStagesIndexView(),
                book.recipeStagesIndexView(),
                book.itemJeiStagesIndexView(),
                book.fluidJeiStagesIndexView(),
                book.chemicalJeiStagesIndexView(),
                book.recipeJeiStagesIndexView(),
                book.fluidTransferStagesIndexView(),
                book.chemicalTransferStagesIndexView());
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        ClientboundStageIndicesPayload.STREAM_CODEC.encode(buf, payload);
        ClientboundStageIndicesPayload decoded = ClientboundStageIndicesPayload.STREAM_CODEC.decode(buf);
        assertEquals(0, buf.readableBytes());
        assertEquals(payload, decoded);
        return decoded;
    }

    private static JsonArray array(String... values) {
        JsonArray out = new JsonArray();
        for (String v : values) {
            out.add(v);
        }
        return out;
    }
}
