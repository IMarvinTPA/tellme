package fi.dy.masa.tellme.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import fi.dy.masa.tellme.TellMe;
import fi.dy.masa.tellme.command.SubCommand;
import fi.dy.masa.tellme.datadump.DataDump;

public class ItemInfo
{
    public static boolean areItemStacksEqual(@Nonnull ItemStack stack1, @Nonnull ItemStack stack2)
    {
        if (stack1.isEmpty() || stack2.isEmpty())
        {
            return stack1.isEmpty() == stack2.isEmpty();
        }

        return stack1.isItemEqual(stack2) && ItemStack.areItemStackTagsEqual(stack1, stack2);
    }

    private static List<String> getBasicItemInfo(@Nonnull ItemStack stack)
    {
        List<String> lines = new ArrayList<String>();
        String name = Item.REGISTRY.getNameForObject(stack.getItem()).toString();
        String dname = stack.getDisplayName();
        String nbtInfo;

        if (stack.hasTagCompound())
        {
            nbtInfo = "has NBT data";
        }
        else
        {
            nbtInfo = "no NBT data";
        }

        String fmt = "%s (%s - %d:%d) %s";
        lines.add(String.format(fmt, dname, name, Item.getIdFromItem(stack.getItem()), stack.getItemDamage(), nbtInfo));

        return lines;
    }

    private static List<String> getFullItemInfo(@Nonnull ItemStack stack)
    {
        List<String> lines = getBasicItemInfo(stack);
        if (stack.hasTagCompound() == false)
        {
            return lines;
        }

        lines.add("");
        lines.add(stack.getTagCompound().toString());
        lines.add("");
        NBTFormatter.getPrettyFormattedNBT(lines, stack.getTagCompound());

        return lines;
    }

    public static void printBasicItemInfoToChat(EntityPlayer player, @Nonnull ItemStack stack)
    {
        for (String line : getBasicItemInfo(stack))
        {
            player.sendMessage(new TextComponentString(line));
        }
    }

    public static void printItemInfoToConsole(@Nonnull ItemStack stack)
    {
        List<String> lines = getFullItemInfo(stack);

        for (String line : lines)
        {
            TellMe.logger.info(line);
        }
    }

    public static void dumpItemInfoToFile(EntityPlayer player, @Nonnull ItemStack stack)
    {
        File file = DataDump.dumpDataToFile("item_data", getFullItemInfo(stack));
        SubCommand.sendClickableLinkMessage(player, "Output written to file %s", file);
    }

    public static void printItemInfo(EntityPlayer player, @Nonnull ItemStack stack, boolean dumpToFile)
    {
        printBasicItemInfoToChat(player, stack);

        if (dumpToFile)
        {
            dumpItemInfoToFile(player, stack);
        }
        else
        {
            printItemInfoToConsole(stack);
        }
    }
}
