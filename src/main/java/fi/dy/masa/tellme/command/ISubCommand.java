package fi.dy.masa.tellme.command;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;

import malilib.util.position.BlockPos;

public interface ISubCommand
{
    /* Returns the command name */
    String getName();

    /* Processes the command */
    void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException;

    /* Adds the tab completion options */
    List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos);

    /* Gets the sub commands for this (sub) command.*/
    List<String> getSubCommands();

    /* Gets the sub command help string. */
    ITextComponent getHelp();
}
