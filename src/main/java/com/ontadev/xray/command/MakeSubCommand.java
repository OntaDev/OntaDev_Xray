package com.ontadev.xray.command;

import com.ontadev.libs.command.SubCommand;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.xray.service.LampItemFactory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MakeSubCommand extends SubCommand {
    private static final List<String> KINDS = List.of("lamp", "fuel");

    private final LampItemFactory lampItemFactory;

    public MakeSubCommand(LampItemFactory lampItemFactory) {
        super("make");
        this.lampItemFactory = lampItemFactory;
    }

    @Override
    public String getPermission(CommandSender sender, Command command, String label, String... args) {
        return "lamp.admin";
    }

    @Override
    public List<String> complete(CommandSender sender, String... args) {
        if (args.length != 1) return Collections.emptyList();

        String typed = args[0].toLowerCase(Locale.ROOT);
        return Stream.of("lamp", "fuel").filter(kind -> kind.startsWith(typed)).collect(Collectors.toList());
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String... args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду можно выполнить только в игре.");
            return;
        }
        if (args.length < 1 || !KINDS.contains(args[0].toLowerCase(Locale.ROOT))) {
            sendUsage(sender, command, label, args);
            return;
        }

        Player player = (Player) sender;
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() == Material.AIR) {
            sender.sendMessage(ChatColor.RED + "Возьмите в руку предмет, который хотите превратить.");
            return;
        }

        if (args[0].equalsIgnoreCase("lamp")) {
            player.getInventory().setItemInMainHand(lampItemFactory.convertToLamp(held));
            sender.sendMessage(ChatColor.GREEN + "Предмет в руке теперь лампа X-ray (заряжена).");
        } else {
            player.getInventory().setItemInMainHand(lampItemFactory.convertToFuel(held));
            sender.sendMessage(ChatColor.GREEN + "Предмет в руке теперь топливо для лампы.");
        }
    }

    @Override
    public void sendUsage(CommandSender sender, Command command, String label, String... args) {
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " make <lamp|fuel>");
    }
}
