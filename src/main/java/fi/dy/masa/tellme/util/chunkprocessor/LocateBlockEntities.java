package fi.dy.masa.tellme.util.chunkprocessor;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.Sets;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import fi.dy.masa.tellme.TellMe;
import fi.dy.masa.tellme.util.BlockInfo;
import fi.dy.masa.tellme.util.WorldUtils;
import fi.dy.masa.tellme.util.datadump.DataDump;

public class LocateBlockEntities extends LocateBase
{
    protected final Set<BlockEntityType<?>> filters;

    protected LocateBlockEntities(DataDump.Format format, List<String> filterStrings) throws CommandSyntaxException
    {
        super(format);

        this.filters = this.generateBlockEntityFilters(filterStrings);
    }

    protected Set<BlockEntityType<?>> generateBlockEntityFilters(List<String> filterStrings) throws CommandSyntaxException
    {
        Set<BlockEntityType<?>> set = Sets.newIdentityHashSet();
        @SuppressWarnings("resource")
        var reg = Minecraft.getInstance().level.registryAccess().registryOrThrow(Registries.BLOCK_ENTITY_TYPE);

        for (String name : filterStrings)
        {
            try
            {
                ResourceLocation key = new ResourceLocation(name);
                //@SuppressWarnings("deprecation")
                Optional<BlockEntityType<?>> type = reg.getOptional(key);

                if (type.isPresent())
                {
                    set.add(type.get());
                    continue;
                }
            }
            catch (Exception ignore) {}

            TellMe.logger.warn("Invalid BlockEntity name '{}'", name);
            throw INVALID_NAME_EXCEPTION.create(name);
        }

        return set;
    }

    @Override
    public void processChunks(Collection<LevelChunk> chunks, BlockPos posMin, BlockPos posMax)
    {
        final long timeBefore = System.nanoTime();
        Set<BlockEntityType<?>> filters = this.filters;
        int count = 0;

        for (LevelChunk chunk : chunks)
        {
            if (this.data.size() >= 100000)
            {
                TellMe.logger.warn("Over 100 000 BlockEntities found already, aborting...");
                break;
            }

            ChunkPos chunkPos = chunk.getPos();
            final String dim = WorldUtils.getDimensionId(chunk.getLevel());
            final int xMin = Math.max(chunkPos.x << 4, posMin.getX());
            final int yMin = Math.max(chunk.getMinBuildHeight(), posMin.getY());
            final int zMin = Math.max(chunkPos.z << 4, posMin.getZ());
            final int xMax = Math.min((chunkPos.x << 4) + 15, posMax.getX());
            final int yMax = Math.min(chunk.getMaxBuildHeight() - 1, posMax.getY());
            final int zMax = Math.min((chunkPos.z << 4) + 15, posMax.getZ());
            BoundingBox box = new BoundingBox(xMin, yMin, zMin, xMax, yMax, zMax);

            for (BlockEntity te : chunk.getBlockEntities().values())
            {
                BlockPos pos = te.getBlockPos();
                BlockEntityType<?> type = te.getType();
                //System.out.printf("plop @ %s - box: %s\n", pos, box);

                if (filters.contains(type) && box.isInside(pos))
                {
                    String name = BlockInfo.getBlockEntityNameFor(type);
                    this.data.add(LocationData.of(name, dim, new Vec3(pos.getX(), pos.getY(), pos.getZ())));
                    count++;
                }
            }
        }

        final long timeAfter = System.nanoTime();
        TellMe.logger.info(String.format(Locale.US, "Located %d BlockEntities in %d chunks in %.3f seconds.",
                                         count, chunks.size(), (timeAfter - timeBefore) / 1000000000D));
    }
}
