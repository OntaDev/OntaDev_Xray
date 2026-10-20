package com.ontadev.xray.command;

import com.ontadev.libs.command.SubCommand;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.xray.domain.MaterialEntry;
import com.ontadev.xray.service.MaterialManager;
import com.ontadev.xray.service.PlayerLampSettingsService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class PrefsSubCommand extends SubCommand {
    private static final List<String> ACTIONS = List.of("list", "toggle", "color");

    private final MaterialManager materialManager;
    private final PlayerLampSettingsService settings;

    public PrefsSubCommand(MaterialManager materialManager, PlayerLampSettingsService settings) {
        super("my");
        this.materialManager = materialManager;
        this.settings = settings;
    }

    @Override
    public String getPermission(CommandSender sender, Command command, String label, String... args) {
        return "lamp.gui";
    }

    @Override
    public List<String> complete(CommandSender sender, String... args) {
        if (args.length == 1) return CommandArgs.startingWith(ACTIONS.stream(), args[0]);

        String action = args[0].toLowerCase();
        if (args.length == 2 && (action.equals("toggle") || action.equals("color"))) {
            return CommandArgs.startingWith(listedNames(), args[1]);
        }
        if (args.length == 3 && action.equals("color")) {
            return CommandArgs.startingWith(Stream.concat(CommandArgs.colorNames().stream(), Stream.of("reset")), args[2]);
        }
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, Command command, String label, String... args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду можно выполнить только в игре.");
            return;
        }
        UUID id = ((Player) sender).getUniqueId();

        String action = args.length == 0 ? "list" : args[0].toLowerCase();
        switch (action) {
            case "list":
                list(sender, id);
                break;
            case "toggle":
                toggle(sender, id, command, label, args);
                break;
            case "color":
                color(sender, id, command, label, args);
                break;
            default:
                sendUsage(sender, command, label, args);
        }
    }

    private void list(CommandSender sender, UUID id) {
        sender.sendMessage(ChatColor.GOLD + "Моя подсветка:");
        for (MaterialEntry entry : materialManager.getAll()) {
            boolean enabled = settings.isEnabled(id, entry.getId());
            ChatColor color = settings.colorFor(id, entry.getId(), entry.getColor());
            sender.sendMessage((enabled ? ChatColor.GREEN + " [✓] " : ChatColor.RED + " [✗] ") + ChatColor.WHITE + entry.getMaterial().name()
                    + ChatColor.GRAY + " цвет: " + color + color.name()
                    + (settings.customColor(id, entry.getId()).isPresent() ? ChatColor.DARK_GRAY + " (свой)" : ""));
        }
    }

    private void toggle(CommandSender sender, UUID id, Command command, String label, String[] args) {
        if (args.length < 2) {
            sendUsage(sender, command, label, args);
            return;
        }
        Optional<MaterialEntry> entry = CommandArgs.block(args[1]).flatMap(materialManager::findByMaterial);
        if (entry.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Такого материала нет в списке: " + args[1]);
            return;
        }

        settings.toggle(id, entry.get().getId());
        sender.sendMessage((settings.isEnabled(id, entry.get().getId()) ? ChatColor.GREEN + "Показываю: " : ChatColor.RED + "Скрываю: ")
                + entry.get().getMaterial().name());
    }

    private void color(CommandSender sender, UUID id, Command command, String label, String[] args) {
        if (args.length < 3) {
            sendUsage(sender, command, label, args);
            return;
        }
        Optional<MaterialEntry> entry = CommandArgs.block(args[1]).flatMap(materialManager::findByMaterial);
        if (entry.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Такого материала нет в списке: " + args[1]);
            return;
        }

        if (args[2].equalsIgnoreCase("reset")) {
            settings.resetColor(id, entry.get().getId());
            sender.sendMessage(ChatColor.GREEN + entry.get().getMaterial().name() + ChatColor.GRAY + ": цвет по умолчанию");
            return;
        }

        Optional<ChatColor> color = CommandArgs.color(args[2]);
        if (color.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Неизвестный цвет: " + args[2] + ". Доступно: " + String.join(", ", CommandArgs.colorNames()));
            return;
        }

        settings.setColor(id, entry.get().getId(), color.get());
        sender.sendMessage(ChatColor.GREEN + entry.get().getMaterial().name() + ChatColor.GRAY + " теперь " + color.get() + color.get().name());
    }

    private Stream<String> listedNames() {
        return materialManager.getAll().stream().map(entry -> entry.getMaterial().name().toLowerCase());
    }

    @Override
    public void sendUsage(CommandSender sender, Command command, String label, String... args) {
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " my list" + ChatColor.GRAY + " - мои настройки подсветки");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " my toggle <блок>" + ChatColor.GRAY + " - показать/скрыть материал");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " my color <блок> <цвет|reset>");
    }
}
