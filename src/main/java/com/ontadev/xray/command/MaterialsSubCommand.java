package com.ontadev.xray.command;

import com.ontadev.libs.command.SubCommand;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.xray.domain.MaterialEntry;
import com.ontadev.xray.gui.GlobalMaterialsGui;
import com.ontadev.xray.service.MaterialManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class MaterialsSubCommand extends SubCommand {
    private static final List<String> ACTIONS = List.of("list", "add", "remove", "color");

    private final GlobalMaterialsGui globalMaterialsGui;
    private final MaterialManager materialManager;

    public MaterialsSubCommand(GlobalMaterialsGui globalMaterialsGui, MaterialManager materialManager) {
        super("materials");
        this.globalMaterialsGui = globalMaterialsGui;
        this.materialManager = materialManager;
    }

    @Override
    public String getPermission(CommandSender sender, Command command, String label, String... args) {
        return "lamp.materials";
    }

    @Override
    public List<String> complete(CommandSender sender, String... args) {
        if (args.length == 1) return CommandArgs.startingWith(ACTIONS.stream(), args[0]);

        String action = args[0].toLowerCase();
        if (args.length == 2) {
            if (action.equals("add")) return CommandArgs.blockNames(args[1]);
            if (action.equals("remove") || action.equals("color")) {
                return CommandArgs.startingWith(listedNames(), args[1]);
            }
        }
        if (args.length == 3 && (action.equals("add") || action.equals("color"))) {
            return CommandArgs.startingWith(CommandArgs.colorNames().stream(), args[2]);
        }
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String... args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                globalMaterialsGui.open((Player) sender);
            } else {
                list(sender);
            }
            return;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                list(sender);
                break;
            case "add":
                add(sender, command, label, args);
                break;
            case "remove":
                remove(sender, command, label, args);
                break;
            case "color":
                color(sender, command, label, args);
                break;
            default:
                sendUsage(sender, command, label, args);
        }
    }

    private void list(CommandSender sender) {
        List<MaterialEntry> all = materialManager.getAll();
        if (all.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Список материалов пуст.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "Материалы для подсветки (" + all.size() + "):");
        for (MaterialEntry entry : all) {
            sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + entry.getMaterial().name()
                    + ChatColor.GRAY + " цвет: " + entry.getColor() + entry.getColor().name());
        }
    }

    private void add(CommandSender sender, Command command, String label, String[] args) {
        if (!canEdit(sender)) return;
        if (args.length < 2) {
            sendUsage(sender, command, label, args);
            return;
        }

        Optional<Material> material = CommandArgs.block(args[1]);
        if (material.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Не блок или неизвестный материал: " + args[1]);
            return;
        }

        ChatColor color = ChatColor.WHITE;
        if (args.length >= 3) {
            Optional<ChatColor> parsed = CommandArgs.color(args[2]);
            if (parsed.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Неизвестный цвет: " + args[2] + ". Доступно: " + String.join(", ", CommandArgs.colorNames()));
                return;
            }
            color = parsed.get();
        }

        if (materialManager.addIfAbsent(material.get(), color).isPresent()) {
            sender.sendMessage(ChatColor.GREEN + "Добавлено: " + material.get().name() + ChatColor.GRAY + " (цвет " + color.name() + ")");
        } else {
            sender.sendMessage(ChatColor.YELLOW + material.get().name() + " уже есть в списке.");
        }
    }

    private void remove(CommandSender sender, Command command, String label, String[] args) {
        if (!canEdit(sender)) return;
        if (args.length < 2) {
            sendUsage(sender, command, label, args);
            return;
        }

        Optional<MaterialEntry> entry = listedEntry(args[1]);
        if (entry.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Такого материала нет в списке: " + args[1]);
            return;
        }

        materialManager.remove(entry.get().getId());
        sender.sendMessage(ChatColor.GREEN + "Удалено: " + entry.get().getMaterial().name());
    }

    private void color(CommandSender sender, Command command, String label, String[] args) {
        if (!canEdit(sender)) return;
        if (args.length < 3) {
            sendUsage(sender, command, label, args);
            return;
        }

        Optional<MaterialEntry> entry = listedEntry(args[1]);
        if (entry.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Такого материала нет в списке: " + args[1]);
            return;
        }

        Optional<ChatColor> color = CommandArgs.color(args[2]);
        if (color.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Неизвестный цвет: " + args[2] + ". Доступно: " + String.join(", ", CommandArgs.colorNames()));
            return;
        }

        materialManager.updateColor(entry.get().getId(), color.get());
        sender.sendMessage(ChatColor.GREEN + entry.get().getMaterial().name() + ChatColor.GRAY + " теперь " + color.get() + color.get().name());
    }

    private Optional<MaterialEntry> listedEntry(String name) {
        return CommandArgs.block(name).flatMap(materialManager::findByMaterial);
    }

    private Stream<String> listedNames() {
        return materialManager.getAll().stream().map(entry -> entry.getMaterial().name().toLowerCase());
    }

    private boolean canEdit(CommandSender sender) {
        if (sender.hasPermission("lamp.materials.edit")) return true;
        sender.sendMessage(ChatColor.RED + "У вас нет прав изменять список материалов (lamp.materials.edit).");
        return false;
    }

    @Override
    public void sendUsage(CommandSender sender, Command command, String label, String... args) {
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " materials" + ChatColor.GRAY + " - открыть редактор (в игре) или показать список");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " materials list");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " materials add <блок> [цвет]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " materials remove <блок>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " materials color <блок> <цвет>");
    }
}
