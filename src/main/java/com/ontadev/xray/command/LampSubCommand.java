package com.ontadev.xray.command;

import com.ontadev.libs.command.SubCommand;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.xray.domain.MaterialEntry;
import com.ontadev.xray.service.FuelService;
import com.ontadev.xray.service.HandItems;
import com.ontadev.xray.service.LampDataService;
import com.ontadev.xray.service.MaterialManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class LampSubCommand extends SubCommand {
    private static final List<String> ACTIONS = List.of("info", "radius", "max", "cooldown", "forall", "material");

    private final LampDataService lampDataService;
    private final MaterialManager materialManager;
    private final FuelService fuelService;

    public LampSubCommand(LampDataService lampDataService, MaterialManager materialManager, FuelService fuelService) {
        super("lamp");
        this.lampDataService = lampDataService;
        this.materialManager = materialManager;
        this.fuelService = fuelService;
    }

    @Override
    public String getPermission(CommandSender sender, Command command, String label, String... args) {
        return "lamp.use";
    }

    @Override
    public List<String> complete(CommandSender sender, String... args) {
        if (args.length == 1) return CommandArgs.startingWith(ACTIONS.stream(), args[0]);

        String action = args[0].toLowerCase();
        if (args.length == 2 && action.equals("forall")) return CommandArgs.startingWith(List.of("on", "off").stream(), args[1]);
        if (args.length == 2 && action.equals("material")) return CommandArgs.startingWith(List.of("list", "add", "remove").stream(), args[1]);
        if (args.length == 3 && action.equals("material")) {
            return CommandArgs.startingWith(materialManager.getAll().stream().map(e -> e.getMaterial().name().toLowerCase()), args[2]);
        }
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String... args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду можно выполнить только в игре.");
            return;
        }
        Player player = (Player) sender;

        ItemStack lamp = HandItems.heldLamp(lampDataService, player);
        if (lamp == null) {
            sender.sendMessage(ChatColor.RED + "Держите лампу в руке.");
            return;
        }

        String action = args.length == 0 ? "info" : args[0].toLowerCase();
        if (action.equals("info")) {
            info(sender, lamp);
            return;
        }
        if (!sender.hasPermission("lamp.lamp.edit")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав изменять параметры лампы (lamp.lamp.edit).");
            return;
        }

        switch (action) {
            case "radius":
            case "max":
            case "cooldown":
                setNumber(player, lamp, action, command, label, args);
                break;
            case "forall":
                setForAll(player, lamp, command, label, args);
                break;
            case "material":
                material(player, lamp, command, label, args);
                break;
            default:
                sendUsage(sender, command, label, args);
        }
    }

    private void info(CommandSender sender, ItemStack lamp) {
        List<String> names = materialManager.filterExisting(lampDataService.getMaterialIds(lamp)).stream()
                .map(id -> materialManager.get(id).map(entry -> entry.getMaterial().name()).orElse("?"))
                .collect(Collectors.toList());

        sender.sendMessage(ChatColor.GOLD + "Лампа X-ray:");
        sender.sendMessage(ChatColor.GRAY + " радиус: " + ChatColor.WHITE + lampDataService.getRadius(lamp)
                + ChatColor.GRAY + ", макс. блоков: " + ChatColor.WHITE + lampDataService.getMaxHighlightedBlocks(lamp)
                + ChatColor.GRAY + ", перезарядка: " + ChatColor.WHITE + lampDataService.getCooldownSeconds(lamp) + " сек");
        sender.sendMessage(ChatColor.GRAY + " для всех рядом: " + ChatColor.WHITE + (lampDataService.isHighlightForAllPlayers(lamp) ? "да" : "нет")
                + ChatColor.GRAY + ", заряд: " + ChatColor.WHITE + fuelService.chargePercent(lampDataService.getFuel(lamp)) + "%");
        sender.sendMessage(ChatColor.GRAY + " материалы: " + ChatColor.WHITE + (names.isEmpty() ? "нет" : String.join(", ", names)));
    }

    private void setNumber(Player player, ItemStack lamp, String field, Command command, String label, String[] args) {
        Optional<Long> value = args.length >= 2 ? CommandArgs.number(args[1]) : Optional.empty();
        if (value.isEmpty()) {
            sendUsage(player, command, label, args);
            return;
        }

        ItemMeta meta = lamp.getItemMeta();
        long number = value.get();
        switch (field) {
            case "radius":
                lampDataService.setRadius(meta, (int) number);
                break;
            case "max":
                lampDataService.setMaxHighlightedBlocks(meta, (int) number);
                break;
            default:
                lampDataService.setCooldownSeconds(meta, number);
        }
        lamp.setItemMeta(meta);
        HandItems.storeLamp(lampDataService, player, lamp);

        player.sendMessage(ChatColor.GREEN + "Готово: " + field + " = " + readBack(lamp, field));
    }

    private long readBack(ItemStack lamp, String field) {
        switch (field) {
            case "radius":
                return lampDataService.getRadius(lamp);
            case "max":
                return lampDataService.getMaxHighlightedBlocks(lamp);
            default:
                return lampDataService.getCooldownSeconds(lamp);
        }
    }

    private void setForAll(Player player, ItemStack lamp, Command command, String label, String[] args) {
        Optional<Boolean> value = args.length >= 2 ? CommandArgs.toggle(args[1]) : Optional.empty();
        if (value.isEmpty()) {
            sendUsage(player, command, label, args);
            return;
        }

        ItemMeta meta = lamp.getItemMeta();
        lampDataService.setHighlightForAllPlayers(meta, value.get());
        lamp.setItemMeta(meta);
        HandItems.storeLamp(lampDataService, player, lamp);

        player.sendMessage(ChatColor.GREEN + "Подсветка для всех рядом: " + (value.get() ? "включена" : "выключена"));
    }

    private void material(Player player, ItemStack lamp, Command command, String label, String[] args) {
        String action = args.length >= 2 ? args[1].toLowerCase() : "list";
        if (action.equals("list")) {
            info(player, lamp);
            return;
        }
        if (args.length < 3 || !(action.equals("add") || action.equals("remove"))) {
            sendUsage(player, command, label, args);
            return;
        }

        Optional<MaterialEntry> entry = CommandArgs.block(args[2]).flatMap(materialManager::findByMaterial);
        if (entry.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Этого материала нет в общем списке: " + args[2] + " (добавьте его: /xray materials add)");
            return;
        }

        List<Integer> ids = new ArrayList<>(lampDataService.getMaterialIds(lamp));
        Material material = entry.get().getMaterial();
        if (action.equals("add")) {
            if (ids.contains(entry.get().getId())) {
                player.sendMessage(ChatColor.YELLOW + material.name() + " уже подсвечивается этой лампой.");
                return;
            }
            ids.add(entry.get().getId());
        } else if (!ids.remove((Integer) entry.get().getId())) {
            player.sendMessage(ChatColor.YELLOW + material.name() + " и так не выбран в этой лампе.");
            return;
        }

        HandItems.storeLamp(lampDataService, player, lampDataService.setMaterialIds(lamp, ids));
        player.sendMessage(ChatColor.GREEN + (action.equals("add") ? "Добавлено в лампу: " : "Убрано из лампы: ") + material.name());
    }

    @Override
    public void sendUsage(CommandSender sender, Command command, String label, String... args) {
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " lamp info" + ChatColor.GRAY + " - параметры лампы в руке");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " lamp radius|max|cooldown <число>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " lamp forall <on|off>" + ChatColor.GRAY + " - подсветка для всех рядом");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " lamp material <add|remove> <блок>");
    }
}
