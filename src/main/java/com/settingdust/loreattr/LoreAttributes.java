package com.settingdust.loreattr;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Matcher;

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

        Bukkit.getServer().getPluginManager().registerEvents(new LoreEvents(this), this);
    }

    public void onDisable() {
        HandlerList.unregisterAll(this);
        loreManager.disable();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getLabel().equalsIgnoreCase("hp")) {
            if (!(sender instanceof Player)) {
                return false;
            }
            Player p = (Player) sender;
            p.sendMessage("生命值: " + p.getHealth() + "/" + p.getMaxHealth());
            return true;
        }

        if (cmd.getLabel().equalsIgnoreCase("lorestats")) {
            if (!(sender instanceof Player)) {
                return false;
            }
            loreManager.displayLoreStats((Player) sender);
            return true;
        }

        if (cmd.getLabel().equalsIgnoreCase("dura")) {
            if (!(sender instanceof Player)) {
                return false;
            }
            Player p = (Player) sender;
            if (loreManager.hasDura(p.getItemInHand())) {
                switch (args.length) {
                    case 0:
                        p.sendMessage(String.valueOf(loreManager.getDura(p.getItemInHand())));
                        break;
                    case 1:
                        if (args[0].matches("\\d+")) {
                            loreManager.addDura(p.getItemInHand(), Integer.parseInt(args[0]));
                            p.sendMessage(ChatColor.GREEN + "耐久已恢复至" + String.valueOf(loreManager.getDura(p.getItemInHand())));
                        }
                        break;
                }
                return true;
            }
        }
        return false;
    }
}