package com.ontadev.xray.command;

import com.ontadev.libs.command.AbstractCommand;
import com.ontadev.libs.ioc.annotation.stereotype.RegisterCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RegisterCommand
public class XrayCommand extends AbstractCommand {
    public XrayCommand(
            GiveLampSubCommand giveLampSubCommand,
            GiveFuelSubCommand giveFuelSubCommand,
            MakeSubCommand makeSubCommand,
            LampSubCommand lampSubCommand,
            PrefsSubCommand prefsSubCommand,
            MaterialsSubCommand materialsSubCommand,
            ReloadSubCommand reloadSubCommand
    ) {
        registerSubCommand(giveLampSubCommand);
        registerSubCommand(giveFuelSubCommand);
        registerSubCommand(makeSubCommand);
        registerSubCommand(lampSubCommand);
        registerSubCommand(prefsSubCommand);
        registerSubCommand(materialsSubCommand);
        registerSubCommand(reloadSubCommand);
    }

    @Override
    public @NotNull String getName() {
        return "xray";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("ontadevxray");
    }

    @Override
    protected void handle(CommandSender sender, org.bukkit.command.Command command, String commandLabel, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "OntaDev_Xray" + ChatColor.GRAY + " - используйте:");
        sender.sendMessage(ChatColor.YELLOW + "/xray give [игрок]" + ChatColor.GRAY + " - выдать заряженную лампу (себе, если без игрока)");
        sender.sendMessage(ChatColor.YELLOW + "/xray givefuel [игрок] [количество]" + ChatColor.GRAY + " - выдать топливо (себе 1, если без аргументов)");
        sender.sendMessage(ChatColor.YELLOW + "/xray make <lamp|fuel>" + ChatColor.GRAY + " - сделать предмет в руке лампой или топливом");
        sender.sendMessage(ChatColor.YELLOW + "/xray materials [list|add|remove|color]" + ChatColor.GRAY + " - общий список материалов (без аргументов - редактор)");
        sender.sendMessage(ChatColor.YELLOW + "/xray lamp <info|radius|max|cooldown|forall|material>" + ChatColor.GRAY + " - параметры лампы в руке");
        sender.sendMessage(ChatColor.YELLOW + "/xray my <list|toggle|color>" + ChatColor.GRAY + " - мои цвета и скрытые материалы");
        sender.sendMessage(ChatColor.YELLOW + "/xray reload" + ChatColor.GRAY + " - перезагрузить конфиг и кэш материалов");
    }
}
