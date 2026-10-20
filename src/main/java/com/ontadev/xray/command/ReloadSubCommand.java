package com.ontadev.xray.command;

import com.ontadev.libs.command.SubCommand;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.xray.service.MaterialManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

@Component
public class ReloadSubCommand extends SubCommand {
    private final MaterialManager materialManager;

    public ReloadSubCommand(MaterialManager materialManager) {
        super("reload");
        this.materialManager = materialManager;
    }

    @Override
    public String getPermission(CommandSender sender, Command command, String label, String... args) {
        return "lamp.admin";
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String... args) {
        materialManager.reload();
        sender.sendMessage(ChatColor.GREEN + "Кэш глобального списка материалов перезагружен из БД.");
        sender.sendMessage(ChatColor.GRAY + "config.yml (радиус/cooldown/топливо по умолчанию) требует полного рестарта сервера.");
    }

    @Override
    public void sendUsage(CommandSender sender, Command command, String label, String... args) {
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload");
    }
}
