package fi.dy.masa.tellme.util.chunkprocessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ArrayListMultimap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

import fi.dy.masa.tellme.TellMe;
import fi.dy.masa.tellme.command.CommandUtils;
import fi.dy.masa.tellme.util.BlockInfo;
import fi.dy.masa.tellme.util.datadump.DataDump;
import fi.dy.masa.tellme.util.datadump.DataDump.Alignment;
import fi.dy.masa.tellme.util.datadump.DataDump.Format;

public class BlockStats extends ChunkProcessorAllChunks
{
    private final HashMap<BlockState, BlockStateCount> blockStats = new HashMap<>();
    private int chunkCount;
    private boolean append;

    public void setAppend(boolean append)
    {
        this.append = append;
    }

    @Override
    public void processChunks(Collection<WorldChunk> chunks, BlockPos posMin, BlockPos posMax)
    {
        final long timeBefore = System.nanoTime();
        Object2LongOpenHashMap<BlockState> counts = new Object2LongOpenHashMap<>();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        final BlockState air = Blocks.AIR.getDefaultState();
        int count = 0;

        for (Chunk chunk : chunks)
        {
            ChunkPos chunkPos = chunk.getPos();
            final int xMin = Math.max(chunkPos.x << 4, posMin.getX());
            final int zMin = Math.max(chunkPos.z << 4, posMin.getZ());
            final int xMax = Math.min((chunkPos.x << 4) + 15, posMax.getX());
            final int zMax = Math.min((chunkPos.z << 4) + 15, posMax.getZ());
            final int topY = chunk.getHighestNonEmptySectionYOffset() + 15;
            final int yMin = Math.max(chunk.getBottomY(), posMin.getY());
            final int yMax = Math.min(topY, posMax.getY());

            for (int y = yMin; y <= yMax; ++y)
            {
                for (int z = zMin; z <= zMax; ++z)
                {
                    for (int x = xMin; x <= xMax; ++x)
                    {
                        pos.set(x, y, z);
                        BlockState state = chunk.getBlockState(pos);

                        counts.addTo(state, 1);
                        count++;
                    }
                }
            }

            // Add the amount of air that would be in non-existing chunk sections within the given volume
            if (topY < posMax.getY())
            {
                counts.addTo(air, (long) (posMax.getY() - Math.max(topY, posMin.getY() - 1)) * (xMax - xMin + 1) * (zMax - zMin + 1));
            }
        }

        this.chunkCount = this.append ? this.chunkCount + chunks.size() : chunks.size();

        TellMe.logger.info(String.format(Locale.US, "Counted %d blocks in %d chunks in %.4f seconds.",
                count, chunks.size(), (System.nanoTime() - timeBefore) / 1000000000D));

        this.addParsedData(counts);
    }

    private void addParsedData(Object2LongOpenHashMap<BlockState> counts)
    {
        if (this.append == false)
        {
            this.blockStats.clear();
        }

        for (final BlockState state : counts.keySet())
        {
            try
            {
                final Block block = state.getBlock();
                final Identifier id = Registries.BLOCK.getId(block);
                final long amount = counts.getLong(state);

                if (id == null)
                {
                    TellMe.logger.warn("Non-registered block: class = {}, state = {}", block.getClass().getName(), state);
                }

                BlockStateCount info = this.blockStats.computeIfAbsent(state, (s) -> new BlockStateCount(state, id, 0));

                if (this.append)
                {
                    info.addToCount(amount);
                }
                else
                {
                    info.setCount(amount);
                }
            }
            catch (Exception e)
            {
                TellMe.logger.error("Caught an exception while getting block names", e);
            }
        }
    }

    private List<BlockStateCount> getFilteredData(List<String> filters, RegistryWrapper<Block> registryWrapper) throws CommandSyntaxException
    {
        ArrayList<BlockStateCount> list = new ArrayList<>();
        ArrayListMultimap<Block, BlockStateCount> infoByBlock = ArrayListMultimap.create();
        DynamicCommandExceptionType exception = new DynamicCommandExceptionType((type) -> Text.literal("Invalid block state filter: '" + type + "'"));

        for (BlockStateCount info : this.blockStats.values())
        {
            infoByBlock.put(info.state.getBlock(), info);
        }

        for (String filter : filters)
        {
            BlockArgumentParser.BlockResult result = BlockArgumentParser.block(registryWrapper, filter, false);
            BlockState state = result.blockState();

            if (state == null)
            {
                throw exception.create(filter);
            }

            Block block = state.getBlock();
            Map<Property<?>, Comparable<?>> parsedProperties = result.properties();

            // No block state properties specified, get all states for this block
            if (parsedProperties.size() == 0)
            {
                list.addAll(infoByBlock.get(block));
            }
            // Exact state specified, only add that state
            else if (parsedProperties.size() == state.getProperties().size())
            {
                BlockStateCount info = this.blockStats.get(state);

                if (info != null)
                {
                    list.add(info);
                }
            }
            // Some properties specified, filter by those
            else
            {
                List<BlockStateCount> listIn = infoByBlock.get(block);

                // Accept states whose properties are not being filtered, or the value matches the filter
                for (BlockStateCount info : listIn)
                {
                    if (BlockInfo.statePassesFilter(info.state, parsedProperties))
                    {
                        list.add(info);
                    }
                }
            }
        }

        return list;
    }

    public List<String> queryAll(Format format, CommandUtils.BlockStateGrouping grouping,
                                 boolean sortByCount, RegistryWrapper<Block> registryWrapper) throws CommandSyntaxException
    {
        return this.query(format, grouping, sortByCount, null, registryWrapper);
    }

    public List<String> query(Format format, CommandUtils.BlockStateGrouping grouping,
                              boolean sortByCount, @Nullable List<String> filters,
                              RegistryWrapper<Block> registryWrapper) throws CommandSyntaxException
    {
        DataDump dump = new DataDump(3, format);
        List<BlockStateCount> list = new ArrayList<>();

        if (filters != null)
        {
            list.addAll(this.getFilteredData(filters, registryWrapper));
        }
        else
        {
            list.addAll(this.blockStats.values());
        }

        if (grouping == CommandUtils.BlockStateGrouping.BY_BLOCK)
        {
            IdentityHashMap<Block, BlockStateCount> map = new IdentityHashMap<>();

            for (final BlockStateCount info : list)
            {
                BlockStateCount combined = map.computeIfAbsent(info.state.getBlock(), (b) -> new BlockStateCount(info.state, info.id, 0));
                combined.addToCount(info.count);
            }

            list.clear();
            list.addAll(map.values());
        }

        list.sort(sortByCount ? BlockStateCount.getCountComparator() : BlockStateCount.getAlphabeticComparator());
        long total = 0L;

        for (BlockStateCount info : list)
        {
            if (grouping == CommandUtils.BlockStateGrouping.BY_STATE)
            {
                dump.addData(BlockInfo.blockStateToString(info.state), info.displayName, String.valueOf(info.count));
            }
            else
            {
                dump.addData(info.registryName, info.displayName, String.valueOf(info.count));
            }

            if (info.state.isAir() == false)
            {
                total += info.count;
            }
        }

        dump.addTitle("Registry name", "Display name", "Count");
        dump.addFooter(String.format("Block stats from an area touching %d chunks", this.chunkCount));
        dump.addFooter(String.format("The listed output contains %d non-air blocks", total));

        dump.setColumnProperties(2, Alignment.RIGHT, true); // count
        dump.setSort(sortByCount == false);

        return dump.getLines();
    }

    private static class BlockStateCount
    {
        public final BlockState state;
        public final Identifier id;
        public final String registryName;
        public final String displayName;
        public long count;

        public BlockStateCount(BlockState state, Identifier id, long count)
        {
            Block block = state.getBlock();
            ItemStack stack = new ItemStack(block);
            String displayName = stack.isEmpty() == false ? stack.getName().getString() : (Text.translatable(block.getTranslationKey())).getString();

            this.state = state;
            this.id = id;
            this.registryName = id.toString();
            this.displayName = displayName;
            this.count = count;
        }

        public void addToCount(long amount)
        {
            this.count += amount;
        }

        public void setCount(long amount)
        {
            this.count = amount;
        }

        public String getRegistryName()
        {
            return this.registryName;
        }

        public long getCount()
        {
            return this.count;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.registryName == null) ? 0 : this.registryName.hashCode());
            result = prime * result + ((this.state == null) ? 0 : this.state.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (this.getClass() != obj.getClass())
                return false;
            BlockStateCount other = (BlockStateCount) obj;
            if (this.registryName == null)
            {
                if (other.registryName != null)
                    return false;
            }
            else if (!this.registryName.equals(other.registryName))
                return false;
            if (this.state == null)
            {
                return other.state == null;
            }
            else return this.state.equals(other.state);
        }

        public static Comparator<BlockStateCount> getAlphabeticComparator()
        {
            return Comparator.comparing(BlockStateCount::getRegistryName);
        }

        public static Comparator<BlockStateCount> getCountComparator()
        {
            return Comparator.comparingLong(BlockStateCount::getCount).reversed().thenComparing(BlockStateCount::getRegistryName);
        }
    }
}
