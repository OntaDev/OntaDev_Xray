package com.ontadev.xray.command;

import com.ontadev.libs.command.SubCommand;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.xray.service.LampItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GiveLampSubCommand extends SubCommand {
    private final LampItemFactory lampItemFactory;

    public GiveLampSubCommand(LampItemFactory lampItemFactory) {
        super("give");
        this.lampItemFactory = lampItemFactory;
    }

    @Override
    public String getPermission(CommandSender sender, Command command, String label, String... args) {
        return "lamp.admin";
    }

    @Override
    public List<String> complete(CommandSender sender, String... args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String... args) {
        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Игрок не найден или не в сети: " + args[0]);
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sendUsage(sender, command, label, args);
            return;
        }

        target.getInventory().addItem(lampItemFactory.createLamp());
        sender.sendMessage(ChatColor.GREEN + "Лампа выдана игроку " + target.getName());
    }

    @Override
    public void sendUsage(CommandSender sender, Command command, String label, String... args) {
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " give [игрок]" + ChatColor.GRAY + " - без игрока выдаёт вам");
    }
}
