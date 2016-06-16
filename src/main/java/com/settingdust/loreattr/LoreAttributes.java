package com.settingdust.loreattr;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class LoreAttributes extends JavaPlugin {
    public static LoreManager loreManager;
    public static FileConfiguration config = null;

    public void onEnable() {
        config = getConfig();
        config.options().copyDefaults(true);

        saveConfig();

        if (loreManager == null) {
            loreManager = new LoreManager(this);
        }

        Bukkit.getServer().getPluginManager().registerEvents(new LoreEvents(), this);
    }

    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getLabel().equalsIgnoreCase("hp")) {
            if (!(sender instanceof Player)) {
                return false;
            }
            Player p = (Player) sender;
            p.sendMessage("Health: " + p.getHealth() + "/" + p.getMaxHealth());
            return true;
        }

        if (cmd.getLabel().equalsIgnoreCase("lorestats")) {
            if (!(sender instanceof Player)) {
                return false;
            }
            loreManager.displayLoreStats((Player) sender);
            return true;
        }

        return false;
    }
}