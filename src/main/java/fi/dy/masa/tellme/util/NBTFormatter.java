package fi.dy.masa.tellme.util;

import java.util.IllegalFormatException;
import java.util.List;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;

import malilib.util.game.wrap.NbtWrap;
import fi.dy.masa.tellme.TellMe;
import fi.dy.masa.tellme.reference.Constants;

public class NBTFormatter
{
    public static final String[] TAG_NAMES = new String[] {"TAG_End", "TAG_Byte", "TAG_Short",
        "TAG_Int", "TAG_Long", "TAG_Float", "TAG_Double", "TAG_Byte_Array", "TAG_String", "TAG_List", "TAG_Compound", "TAG_Int_Array"};

    private static String getTagName(int id)
    {
        if (id >= 0 && id < TAG_NAMES.length)
        {
            return TAG_NAMES[id];
        }

        return "";
    }

    private static String getTagDescription(int id, String name)
    {
        return getTagName(id) + String.format(" (%d) ('%s')", id, name);
    }

    private static void getPrettyFormattedLine(List<String> lines, NBTBase nbt, String name, int depth)
    {
        int len;
        String line;
        String pre = "";
        String pre2 = "";
        int typeId = NbtWrap.getTypeId(nbt);

        try
        {
            String fmt = String.format("%%-%ds", (depth * 4));
            if (depth > 0) { pre  = String.format(fmt, ""); }
            fmt = String.format("%%%ds", (depth * 4 + 3));
            pre2 = String.format(fmt, "");
        }
        catch(IllegalFormatException e)
        {
            TellMe.LOGGER.warn("Error while printing NBT data");
        }

        switch (typeId)
        {
            case Constants.NBT.TAG_END:
                lines.add(pre + "}");
                break;

            case Constants.NBT.TAG_BYTE:
                lines.add(pre + getTagDescription(typeId, name) + ": " + ((NBTTagByte)nbt).getByte());
                break;

            case Constants.NBT.TAG_SHORT:
                lines.add(pre + getTagDescription(typeId, name) + ": " + ((NBTTagShort)nbt).getShort());
                break;

            case Constants.NBT.TAG_INT:
                lines.add(pre + getTagDescription(typeId, name) + ": " + ((NBTTagInt)nbt).getInt());
                break;

            case Constants.NBT.TAG_LONG:
                lines.add(pre + getTagDescription(typeId, name) + ": " + ((NBTTagLong)nbt).getLong());
                break;

            case Constants.NBT.TAG_FLOAT:
                lines.add(pre + getTagDescription(typeId, name) + ": " + ((NBTTagFloat)nbt).getFloat());
                break;

            case Constants.NBT.TAG_DOUBLE:
                lines.add(pre + getTagDescription(typeId, name) + ": " + ((NBTTagDouble)nbt).getDouble());
                break;

            case Constants.NBT.TAG_BYTE_ARRAY:
                byte[] arrByte = ((NBTTagByteArray) nbt).getByteArray();
                len = arrByte.length;
                lines.add(pre + getTagDescription(typeId, name) + " (" + len + " values)");

                // For short arrays, print one value per line, it is easier to read
                if (len <= 16)
                {
                    for (int pos = 0; pos < len; ++pos)
                    {
                        lines.add(pre2 + String.format("    %2d: 0x%02X (%4d)", pos, arrByte[pos], arrByte[pos]));
                    }
                }
                else
                {
                    for (int pos = 0; pos < len; )
                    {
                        StringBuilder sb = new StringBuilder(256);
                        line = pre2 + String.format("    %4d:", pos);
                        sb.append(line);

                        for (int i = 0; i < 8 && pos < len; ++i, ++pos)
                        {
                            sb.append(String.format(" 0x%02X (%4d)", arrByte[pos], arrByte[pos]));
                        }

                        lines.add(sb.toString());
                    }
                }
                break;

            case Constants.NBT.TAG_STRING:
                lines.add(pre + getTagDescription(typeId, name) + ": " + nbt);
                break;

            case Constants.NBT.TAG_LIST:
                NBTTagList tagList = (NBTTagList)nbt;
                int tagCount = NbtWrap.getListSize(tagList);
                int tagType = NbtWrap.getListStoredType(tagList);

                lines.add(pre + getTagDescription(typeId, name) + " (" + tagCount + " entries of " + getTagName(tagType) + " (" + tagType + "))");
                lines.add(pre + "{");

                for (int i = 0; i < tagCount; ++i)
                {
                    getPrettyFormattedLine(lines, tagList.get(i), "", depth + 1);
                }

                lines.add(pre + "}");
                break;

            case Constants.NBT.TAG_COMPOUND:
                lines.add(pre + getTagDescription(typeId, name));
                lines.add(pre + "{");

                NBTTagCompound tag = (NBTTagCompound) nbt;

                for (String key : NbtWrap.getKeys(tag))
                {
                    getPrettyFormattedLine(lines, NbtWrap.getTag(tag, key), key, depth + 1);
                }

                lines.add(pre + "}");
                break;

            case Constants.NBT.TAG_INT_ARRAY:
                int[] arrInt = ((NBTTagIntArray) nbt).getIntArray();
                len = arrInt.length;
                lines.add(pre + getTagDescription(typeId, name) + " (" + len + " values)");

                // For short arrays, print one value per line, it is easier to read
                if (len <= 16)
                {
                    for (int pos = 0; pos < len; ++pos)
                    {
                        lines.add(pre2 + String.format("    %2d: 0x%08X (%11d)", pos, arrInt[pos], arrInt[pos]));
                    }
                }
                else
                {
                    for (int pos = 0; pos < len; )
                    {
                        StringBuilder sb = new StringBuilder(256);
                        line = pre2 + String.format("    %4d:", pos);
                        sb.append(line);

                        for (int i = 0; i < 4 && pos < len; ++i, ++pos)
                        {
                            sb.append(String.format(" 0x%08X (%11d)", arrInt[pos], arrInt[pos]));
                        }

                        lines.add(sb.toString());
                    }
                }
                break;

            default:
        }
    }

    public static void getPrettyFormattedNBT(List<String> lines, NBTTagCompound nbt)
    {
        if (nbt != null)
        {
            getPrettyFormattedLine(lines, nbt, "", 0);
        }
    }
}
