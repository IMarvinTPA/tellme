package fi.dy.masa.tellme.config;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import fi.dy.masa.tellme.reference.Reference;

public class TellMeConfigGui extends GuiConfig
{
    public TellMeConfigGui(GuiScreen parent)
    {
        super(parent, getConfigElements(), Reference.MOD_ID, false, false, getTitle(parent));
    }

    private static List<IConfigElement> getConfigElements()
    {
        List<IConfigElement> configElements = new ArrayList<IConfigElement>();

        configElements.addAll(new ConfigElement(Configs.config.getCategory(Configs.CATEGORY_GENERIC)).getChildElements());

        return configElements;
    }

    private static String getTitle(GuiScreen parent)
    {
        return GuiConfig.getAbridgedConfigPath(Configs.configurationFile.toString());
    }
}