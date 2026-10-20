package com.ontadev.xray.service;

import com.ontadev.libs.ioc.annotation.stereotype.Service;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TeamManager {
    private static final String TEAM_PREFIX = "ox_";
    private static final String LEGACY_TEAM_PREFIX = "ontadev_xray_";

    private final Scoreboard scoreboard;
    private final Map<ChatColor, Team> teamsByColor = new EnumMap<>(ChatColor.class);

    public TeamManager(JavaPlugin plugin) {
        ScoreboardManager manager = plugin.getServer().getScoreboardManager();
        this.scoreboard = manager != null ? manager.getMainScoreboard() : null;
        removeLegacyTeams();
    }

    private void removeLegacyTeams() {
        if (scoreboard == null) return;

        for (Team team : new java.util.ArrayList<>(scoreboard.getTeams())) {
            if (team.getName().startsWith(LEGACY_TEAM_PREFIX)) team.unregister();
        }
    }

    public synchronized Team teamFor(ChatColor color) {
        return teamsByColor.computeIfAbsent(color, this::createTeam);
    }

    private Team createTeam(ChatColor color) {
        String name = TEAM_PREFIX + color.name().toLowerCase();

        Team existing = scoreboard.getTeam(name);
        if (existing != null) {
            existing.setColor(color);
            return existing;
        }

        Team team = scoreboard.registerNewTeam(name);
        team.setColor(color);
        team.setCanSeeFriendlyInvisibles(false);
        return team;
    }

    public void addEntry(ChatColor color, UUID fakeEntityId) {
        teamFor(color).addEntry(fakeEntityId.toString());
    }

    public void removeEntry(ChatColor color, UUID fakeEntityId) {
        Team team = teamsByColor.get(color);
        if (team != null) {
            team.removeEntry(fakeEntityId.toString());
        }
    }
}
