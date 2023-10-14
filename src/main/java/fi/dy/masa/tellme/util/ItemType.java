package fi.dy.masa.tellme.util;

import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;

/**
 * Wrapper class for ItemStack, which implements equals()
 * for the item, damage and NBT, but not stackSize.
 */
public class ItemType
{
    private final ItemStack stack;

    public ItemType(@Nonnull ItemStack stack)
    {
        this.stack = stack.copy();
    }

    public ItemStack getStack()
    {
        return this.stack;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        //result = prime * result + ((stack == null) ? 0 : stack.hashCode());
        result = prime * result + this.stack.getItem().hashCode();
        result = prime * result + (this.stack.getNbt() != null ? this.stack.getNbt().hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;

        if (obj == null || this.getClass() != obj.getClass())
            return false;

        ItemType other = (ItemType) obj;

        return ItemStack.canCombine(this.stack, other.stack);
    }
}
