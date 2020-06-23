package fi.dy.masa.tellme.util.chunkprocessor;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import fi.dy.masa.tellme.TellMe;
import fi.dy.masa.tellme.util.BiomeLocator;
import fi.dy.masa.tellme.util.datadump.DataDump;
import fi.dy.masa.tellme.util.datadump.DataDump.Alignment;
import fi.dy.masa.tellme.util.datadump.DataDump.Format;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

public class BiomeStats
{
    private final Object2LongOpenHashMap<Biome> biomeCounts = new Object2LongOpenHashMap<Biome>();
    private int totalCount;
    private boolean append;

    public void setAppend(boolean append)
    {
        this.append = append;
    }

    public void getFullBiomeDistribution(BiomeAccess biomeAccess, BlockPos posMin, BlockPos posMax)
    {
        Object2LongOpenHashMap<Biome> counts = new Object2LongOpenHashMap<>();
        BlockPos.Mutable posMutable = new BlockPos.Mutable();
        final long timeBefore = System.nanoTime();
        final int xMin = posMin.getX();
        final int zMin = posMin.getZ();
        final int xMax = posMax.getX();
        final int zMax = posMax.getZ();
        final int width = xMax - xMin + 1;
        final int length = zMax - zMin + 1;
        final long count = width * length;

        for (int x = xMin; x <= xMax; ++x)
        {
            for (int z = zMin; z <= zMax; ++z)
            {
                posMutable.set(x, 0, z);
                Biome biome = biomeAccess.getBiome(posMutable);
                counts.addTo(biome, 1);
            }
        }

        final long timeAfter = System.nanoTime();
        TellMe.logger.info(String.format(Locale.US, "Counted the biome for %d xz-locations in %.3f seconds", count, (timeAfter - timeBefore) / 1000000000D));

        this.addData(counts, count);
    }

    public void getSampledBiomeDistribution(BiomeAccess biomeAccess, int centerX, int centerZ, int sampleInterval, int sampleRadius)
    {
        Object2LongOpenHashMap<Biome> counts = new Object2LongOpenHashMap<>();
        BlockPos.Mutable posMutable = new BlockPos.Mutable();
        final long timeBefore = System.nanoTime();
        final int endX = centerX + sampleRadius * sampleInterval;
        final int endZ = centerZ + sampleRadius * sampleInterval;
        long count = 0;

        for (int z = centerZ - sampleRadius * sampleInterval; z <= endZ; z += sampleInterval)
        {
            for (int x = centerX - sampleRadius * sampleInterval; x <= endX; x += sampleInterval)
            {
                posMutable.set(x, 0, z);
                Biome biome = biomeAccess.getBiome(posMutable);
                counts.addTo(biome, 1);
                ++count;
            }
        }

        final long timeAfter = System.nanoTime();
        TellMe.logger.info(String.format(Locale.US, "Counted the biome for %d xz-locations in %.3f seconds", count, (timeAfter - timeBefore) / 1000000000D));

        this.addData(counts, count);
    }

    private void addData(Object2LongOpenHashMap<Biome> counts, long count)
    {
        if (this.append == false)
        {
            this.biomeCounts.clear();
            this.totalCount = 0;
        }

        for (Map.Entry<Biome, Long> entry : counts.object2LongEntrySet())
        {
            if (this.append)
            {
                this.biomeCounts.addTo(entry.getKey(), entry.getValue());
            }
            else
            {
                this.biomeCounts.put(entry.getKey(), (long) entry.getValue());
            }
        }

        this.totalCount += count;
    }

    private void addFilteredData(DataDump dump, List<String> filters)
    {
        for (String filter : filters)
        {
            int firstSemi = filter.indexOf(":");

            if (firstSemi == -1)
            {
                filter = "minecraft:" + filter;
            }

            Identifier key = null;
            Biome biome = null;

            try
            {
                key = new Identifier(filter);
            }
            catch (Exception e)
            {
            }

            biome = key != null ? Registry.BIOME.get(key) : null;

            if (biome == null)
            {
                TellMe.logger.warn("Invalid biome name '{}'", filter);
                continue;
            }

            for (Map.Entry<Biome, Long> entry : this.biomeCounts.object2LongEntrySet())
            {
                if (entry.getKey() == biome)
                {
                    int id = Registry.BIOME.getRawId(biome);
                    long count = entry.getValue();

                    dump.addData(
                            key.toString(),
                            BiomeLocator.getBiomeDisplayName(biome),
                            String.valueOf(id),
                            String.valueOf(count),
                            String.format("%.2f %%", (double) count * 100D / (double) this.totalCount));
                    break;
                }
            }
        }
    }

    public List<String> queryAll(Format format)
    {
        return this.query(format, null);
    }

    public List<String> query(Format format, @Nullable List<String> filters)
    {
        DataDump dump = new DataDump(5, format);

        if (filters != null)
        {
            this.addFilteredData(dump, filters);
        }
        else
        {
            for (Map.Entry<Biome, Long> entry : this.biomeCounts.object2LongEntrySet())
            {
                Biome biome = entry.getKey();

                if (biome == null)
                {
                    TellMe.logger.warn("Null biome '{}' with count {} ?!", biome, entry.getValue());
                    continue;
                }

                Identifier key = Registry.BIOME.getId(biome);
                int id = Registry.BIOME.getRawId(biome);
                long count = entry.getValue();

                dump.addData(
                        key != null ? key.toString() : "<null>",
                        BiomeLocator.getBiomeDisplayName(biome),
                        String.valueOf(id),
                        String.valueOf(count),
                        String.format("%.2f", (double) count * 100D / (double) this.totalCount));
            }
        }

        dump.addTitle("Registry name", "Name", "ID", "Count", "%");

        dump.setColumnProperties(2, Alignment.RIGHT, true); // Biome ID
        dump.setColumnProperties(3, Alignment.RIGHT, true); // count
        dump.setColumnProperties(4, Alignment.RIGHT, true); // count %

        return dump.getLines();
    }
}
