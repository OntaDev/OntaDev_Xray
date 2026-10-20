package com.ontadev.xray.command;

import com.ontadev.libs.command.SubCommand;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.xray.service.LampItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GiveFuelSubCommand extends SubCommand {
    private final LampItemFactory lampItemFactory;

    public GiveFuelSubCommand(LampItemFactory lampItemFactory) {
        super("givefuel");
        this.lampItemFactory = lampItemFactory;
    }

    @Override
    public String getPermission(CommandSender sender, Command command, String label, String... args) {
        return "lamp.admin";
    }

    @Override
    public List<String> complete(CommandSender sender, String... args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        if (args.length == 2) {
            return Arrays.asList("1", "8", "16", "32", "64");
        }
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String... args) {
        Player target;
        int amount = 1;

        if (args.length == 0) {
            target = senderAsPlayer(sender);
        } else if (args.length == 1 && isNumber(args[0])) {
            target = senderAsPlayer(sender);
            amount = Integer.parseInt(args[0]);
        } else {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Игрок не найден или не в сети: " + args[0]);
                return;
            }
            if (args.length >= 2) {
                if (!isNumber(args[1])) {
                    sender.sendMessage(ChatColor.RED + "Количество должно быть числом: " + args[1]);
                    return;
                }
                amount = Integer.parseInt(args[1]);
            }
        }

        if (target == null) {
            sendUsage(sender, command, label, args);
            return;
        }

        if (amount <= 0) {
            sender.sendMessage(ChatColor.RED + "Количество должно быть больше нуля");
            return;
        }

        target.getInventory().addItem(lampItemFactory.createFuel(amount));
        sender.sendMessage(ChatColor.GREEN + "Выдано " + amount + " ед. топлива игроку " + target.getName());
    }

    private Player senderAsPlayer(CommandSender sender) {
        return sender instanceof Player ? (Player) sender : null;
    }

    private boolean isNumber(String text) {
        try {
            Integer.parseInt(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void sendUsage(CommandSender sender, Command command, String label, String... args) {
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " givefuel [игрок] [количество]"
                + ChatColor.GRAY + " - без аргументов выдаёт вам 1 топливо");
    }
}
