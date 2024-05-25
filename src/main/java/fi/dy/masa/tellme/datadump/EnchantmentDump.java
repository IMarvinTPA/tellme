package fi.dy.masa.tellme.datadump;

import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import fi.dy.masa.tellme.util.datadump.DataDump;

public class EnchantmentDump
{
    public static List<String> getFormattedEnchantmentDump(DataDump.Format format)
    {
        DataDump enchantmentDump = new DataDump(5, format);
        @SuppressWarnings("resource")
        var reg = Minecraft.getInstance().level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);

        for (Map.Entry<ResourceKey<Enchantment>, Enchantment> entry : ForgeRegistries.ENCHANTMENTS.getEntries())
        {
            Enchantment ench = entry.getValue();

            String regName = entry.getKey().location().toString();
            String name = ench.getDescriptionId() != null ? ench.getDescriptionId() : "<null>";
            String type = ench.category != null ? ench.category.toString() : "<null>";
            Enchantment.Rarity rarity = ench.getRarity();
            String rarityStr = rarity != null ? String.format("%s (%d)", rarity.toString(), rarity.getWeight()) : "<null>";
            //@SuppressWarnings("deprecation")
            int intId = reg.getId(ench);

            enchantmentDump.addData(regName, name, type, rarityStr, String.valueOf(intId));
        }

        enchantmentDump.addTitle("Registry name", "Name", "Type", "Rarity", "ID");
        enchantmentDump.setColumnProperties(4, DataDump.Alignment.RIGHT, true);

        return enchantmentDump.getLines();
    }
}
