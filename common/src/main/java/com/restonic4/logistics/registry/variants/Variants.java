package com.restonic4.logistics.registry.variants;

import com.restonic4.logistics.Constants;
import com.restonic4.logistics.registry.entries.BlockEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Entry points for variant-group resource generation that spans multiple blocks — currently the
 * stonecutter interchange graph that lets every block in a group be cut into every other.
 *
 * <p>One {@link #stonecutterGroup()} of N blocks replaces N×(N-1) hand-written recipe JSONs (the
 * wallpaper group alone was 462 files). Recipe ids match the previous hand-authored naming
 * ({@code <result>_from_<ingredient>_stonecutting}).
 */
public final class Variants {
    private Variants() {}

    public static StonecutterGroup stonecutterGroup() {
        return new StonecutterGroup();
    }

    /** Accumulates a set of interchangeable blocks, then emits the full stonecutting graph. */
    public static final class StonecutterGroup {
        private final List<ResourceLocation> members = new ArrayList<>();

        public StonecutterGroup add(BlockEntry<?, ?>... entries) {
            for (BlockEntry<?, ?> entry : entries) {
                members.add(entry.getId());
            }
            return this;
        }

        public StonecutterGroup addAll(Collection<? extends BlockEntry<?, ?>> entries) {
            for (BlockEntry<?, ?> entry : entries) {
                members.add(entry.getId());
            }
            return this;
        }

        /** Schedules a stonecutting recipe for every ordered pair of distinct members. */
        public void build() {
            int count = 0;
            for (ResourceLocation result : members) {
                for (ResourceLocation ingredient : members) {
                    if (result.equals(ingredient)) continue;

                    ResourceLocation path = VariantAssets.recipePath(result, "_from_" + ingredient.getPath() + "_stonecutting");
                    String json = "{\"type\":\"minecraft:stonecutting\","
                            + "\"ingredient\":{\"item\":\"" + ingredient + "\"},"
                            + "\"result\":\"" + result + "\",\"count\":1}";
                    VariantResources.put(PackType.SERVER_DATA, path, json);
                    count++;
                }
            }
            Constants.LOG.info("Generated {} stonecutting recipe(s) for a group of {} blocks.", count, members.size());
        }
    }
}
