package fi.dy.masa.tellme.datadump;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import fi.dy.masa.tellme.util.datadump.DataDump;
import fi.dy.masa.tellme.util.datadump.DataDump.Alignment;

public class PotionDump
{
    public static List<String> getFormattedPotionTypeDump(DataDump.Format format)
    {
        DataDump potionTypeDump = new DataDump(3, format);

        for (Identifier id : Registries.POTION.getIds())
        {
            Potion potion = Registries.POTION.get(id);
            String intId = String.valueOf(Registries.POTION.getRawId(potion));

            List<StatusEffectInstance> effects = potion.getEffects();

            potionTypeDump.addData(id.toString(), intId, String.join(", ", getEffectInfoLines(effects)));
        }

        potionTypeDump.addTitle("Registries name", "ID", "Effects");
        potionTypeDump.setColumnProperties(1, Alignment.RIGHT, true); // id

        return potionTypeDump.getLines();
    }

    public static String getEffectInfo(StatusEffect effect)
    {
        String isBad = String.valueOf(effect.getCategory() == StatusEffectCategory.HARMFUL);
        String isBeneficial = String.valueOf(effect.getCategory() == StatusEffectCategory.BENEFICIAL);
        String regName = Registries.STATUS_EFFECT.getId(effect).toString();

        return "Potion:[reg:" + regName + ",name:" + effect.getTranslationKey() + ",isBad:" + isBad + ",isBeneficial:" + isBeneficial + "]";
    }

    public static String getPotionEffectInfo(StatusEffectInstance effect)
    {
        return String.format("PotionEffect:{%s,amplifier:%d,duration:%d,isAmbient:%s}",
                getEffectInfo(effect.getEffectType()),
                effect.getAmplifier(),
                effect.getDuration(),
                effect.isAmbient());
    }

    public static List<String> getEffectInfoLines(List<StatusEffectInstance> effects)
    {
        List<String> effectStrs = new ArrayList<>();

        for (StatusEffectInstance effect : effects)
        {
            effectStrs.add(getPotionEffectInfo(effect));
        }

        return effectStrs;
    }
}
