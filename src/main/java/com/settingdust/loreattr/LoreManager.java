package com.settingdust.loreattr;

import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class LoreManager {
    private Pattern healthRegex;
    private Pattern negHealthRegex;
    private Pattern regenRegex;
    private Pattern attackSpeedRegex;
    private Pattern damageValueRegex;
    private Pattern negitiveDamageValueRegex;
    private Pattern damageRangeRegex;
    private Pattern dodgeRegex;
    private Pattern critChanceRegex;
    private Pattern critDamageRegex;
    private Pattern lifestealRegex;
    private Pattern armorRegex;
    private Pattern restrictionRegex;
    private Pattern levelRegex;
    private Map<String, Timestamp> attackLog;
    private boolean attackSpeedEnabled;
    private Random generator;

    public LoreManager() {
        this.generator = new Random();

        this.attackSpeedEnabled = false;

        if (LoreAttributes.config.getBoolean("lore.attack-speed.enabled")) {
            this.attackSpeedEnabled = true;
            this.attackLog = new HashMap<String, Timestamp>();
        }

        this.healthRegex = Pattern.compile("[+](\\d+)[ ](" + LoreAttributes.config.getString("lore.health.keyword").toLowerCase() + ")");
        this.negHealthRegex = Pattern.compile("[-](\\d+)[ ](" + LoreAttributes.config.getString("lore.health.keyword").toLowerCase() + ")");
        this.regenRegex = Pattern.compile("[+](\\d+)[ ](" + LoreAttributes.config.getString("lore.regen.keyword").toLowerCase() + ")");
        this.attackSpeedRegex = Pattern.compile("[+](\\d+)[ ](" + LoreAttributes.config.getString("lore.attack-speed.keyword").toLowerCase() + ")");
        this.damageValueRegex = Pattern.compile("[+](\\d+)[ ](" + LoreAttributes.config.getString("lore.damage.keyword").toLowerCase() + ")");
        this.negitiveDamageValueRegex = Pattern.compile("[-](\\d+)[ ](" + LoreAttributes.config.getString("lore.damage.keyword").toLowerCase() + ")");
        this.damageRangeRegex = Pattern.compile("(\\d+)(-)(\\d+)[ ](" + LoreAttributes.config.getString("lore.damage.keyword").toLowerCase() + ")");
        this.dodgeRegex = Pattern.compile("[+](\\d+)[%][ ](" + LoreAttributes.config.getString("lore.dodge.keyword").toLowerCase() + ")");
        this.critChanceRegex = Pattern.compile("[+](\\d+)[%][ ](" + LoreAttributes.config.getString("lore.critical-chance.keyword").toLowerCase() + ")");
        this.critDamageRegex = Pattern.compile("[+](\\d+)[ ](" + LoreAttributes.config.getString("lore.critical-damage.keyword").toLowerCase() + ")");
        this.lifestealRegex = Pattern.compile("[+](\\d+)[ ](" + LoreAttributes.config.getString("lore.life-steal.keyword").toLowerCase() + ")");
        this.armorRegex = Pattern.compile("[+](\\d+)[ ](" + LoreAttributes.config.getString("lore.armor.keyword").toLowerCase() + ")");
        this.restrictionRegex = Pattern.compile("(" + LoreAttributes.config.getString("lore.restriction.keyword").toLowerCase() + ": )(\\w*)");
        this.levelRegex = Pattern.compile(LoreAttributes.config.getString("lore.level.keyword").toLowerCase() + "[ ](\\d+)");
    }

    public void disable() {
        this.attackSpeedEnabled = false;
        if (this.attackLog != null)
            this.attackLog.clear();
    }

    public void handleArmorRestriction(Player player) {
        if (!canUse(player, player.getInventory().getBoots())) {
            if (player.getInventory().firstEmpty() >= 0)
                player.getInventory().addItem(player.getInventory().getBoots());
            else {
                player.getWorld().dropItem(player.getLocation(), player.getInventory().getBoots());
            }
            player.getInventory().setBoots(new ItemStack(Material.AIR));
        }

        if (!canUse(player, player.getInventory().getChestplate())) {
            if (player.getInventory().firstEmpty() >= 0)
                player.getInventory().addItem(player.getInventory().getChestplate());
            else {
                player.getWorld().dropItem(player.getLocation(), player.getInventory().getChestplate());
            }
            player.getInventory().setChestplate(new ItemStack(Material.AIR));
        }

        if (!canUse(player, player.getInventory().getHelmet())) {
            if (player.getInventory().firstEmpty() >= 0)
                player.getInventory().addItem(player.getInventory().getHelmet());
            else {
                player.getWorld().dropItem(player.getLocation(), player.getInventory().getHelmet());
            }
            player.getInventory().setHelmet(new ItemStack(Material.AIR));
        }

        if (!canUse(player, player.getInventory().getLeggings())) {
            if (player.getInventory().firstEmpty() >= 0)
                player.getInventory().addItem(player.getInventory().getLeggings());
            else {
                player.getWorld().dropItem(player.getLocation(), player.getInventory().getLeggings());
            }
            player.getInventory().setLeggings(new ItemStack(Material.AIR));
        }
        applyHpBonus(player);
    }

    public boolean canUse(Player player, ItemStack item) {
        if ((item != null) &&
                (item.hasItemMeta()) &&
                (item.getItemMeta().hasLore()) &&
                !player.getGameMode().equals(GameMode.CREATIVE)) {
            List lore = item.getItemMeta().getLore();
            String allLore = lore.toString().toLowerCase();
            Matcher valueMatcher = this.levelRegex.matcher(allLore);
            if (valueMatcher.find()) {
                if (player.getLevel() < Integer.valueOf(valueMatcher.group(1))) {
                    player.sendMessage(LoreAttributes.config.getString("lore.level.message"));
                    return false;
                }
            }
            valueMatcher = this.restrictionRegex.matcher(allLore);
            if (valueMatcher.find()) {
                if (player.hasPermission("loreattributes." + valueMatcher.group(2))) {
                    return true;
                }
                if (LoreAttributes.config.getBoolean("lore.restriction.display-message")) {
                    player.sendMessage(LoreAttributes.config.getString("lore.restriction.message").replace("%itemname%", item.getType().toString()));
                }
                return false;
            }
        }

        return true;
    }

    public int getDodgeBonus(LivingEntity entity) {
        Integer dodgeBonus = 0;
        for (ItemStack item : entity.getEquipment().getArmorContents()) {
            if ((item != null) &&
                    (item.hasItemMeta()) &&
                    (item.getItemMeta().hasLore())) {
                List lore = item.getItemMeta().getLore();
                String allLore = lore.toString().toLowerCase();

                Matcher valueMatcher = this.dodgeRegex.matcher(allLore);
                if (valueMatcher.find()) {
                    dodgeBonus = dodgeBonus + Integer.valueOf(valueMatcher.group(1));
                }

            }

        }

        ItemStack item = entity.getEquipment().getItemInHand();
        if ((item != null) &&
                (item.hasItemMeta()) &&
                (item.getItemMeta().hasLore())) {
            List lore = item.getItemMeta().getLore();
            String allLore = lore.toString().toLowerCase();

            Matcher valueMatcher = this.dodgeRegex.matcher(allLore);
            if (valueMatcher.find()) {
                dodgeBonus = dodgeBonus + Integer.valueOf(valueMatcher.group(1));
            }

        }

        return dodgeBonus;
    }

    public boolean dodgedAttack(LivingEntity entity) {
        if (!entity.isValid()) {
            return false;
        }
        Integer chance = getDodgeBonus(entity);

        Integer roll = this.generator.nextInt(100) + 1;

        return chance >= roll;
    }

    private int getCritChance(LivingEntity entity) {
        Integer chance = 0;

        for (ItemStack item : entity.getEquipment().getArmorContents()) {
            if ((item != null) &&
                    (item.hasItemMeta()) &&
                    (item.getItemMeta().hasLore())) {
                List lore = item.getItemMeta().getLore();
                String allLore = lore.toString().toLowerCase();

                Matcher valueMatcher = this.critChanceRegex.matcher(allLore);
                if (valueMatcher.find()) {
                    chance = chance + Integer.valueOf(valueMatcher.group(1));
                }

            }

        }

        ItemStack item = entity.getEquipment().getItemInHand();
        if ((item != null) &&
                (item.hasItemMeta()) &&
                (item.getItemMeta().hasLore())) {
            List lore = item.getItemMeta().getLore();
            String allLore = lore.toString().toLowerCase();

            Matcher valueMatcher = this.critChanceRegex.matcher(allLore);
            if (valueMatcher.find()) {
                chance = chance + Integer.valueOf(valueMatcher.group(1));
            }

        }

        return chance;
    }

    private boolean critAttack(LivingEntity entity) {
        if (!entity.isValid()) {
            return false;
        }
        Integer chance = getCritChance(entity);

        Integer roll = this.generator.nextInt(100) + 1;

        return chance >= roll;
    }

    public int getArmorBonus(LivingEntity entity) {
        Integer armor = 0;

        for (ItemStack item : entity.getEquipment().getArmorContents()) {
            if ((item != null) &&
                    (item.hasItemMeta()) &&
                    (item.getItemMeta().hasLore())) {
                List lore = item.getItemMeta().getLore();
                String allLore = lore.toString().toLowerCase();

                Matcher valueMatcher = this.armorRegex.matcher(allLore);
                if (valueMatcher.find()) {
                    armor = armor + Integer.valueOf(valueMatcher.group(1));
                }

            }

        }

        ItemStack item = entity.getEquipment().getItemInHand();
        if ((item != null) &&
                (item.hasItemMeta()) &&
                (item.getItemMeta().hasLore())) {
            List lore = item.getItemMeta().getLore();
            String allLore = lore.toString().toLowerCase();

            Matcher valueMatcher = this.armorRegex.matcher(allLore);
            if (valueMatcher.find()) {
                armor = armor + Integer.valueOf(valueMatcher.group(1));
            }

        }

        return armor;
    }

    public int getLifeSteal(LivingEntity entity) {
        Integer steal = 0;

        for (ItemStack item : entity.getEquipment().getArmorContents()) {
            if ((item != null) &&
                    (item.hasItemMeta()) &&
                    (item.getItemMeta().hasLore())) {
                List lore = item.getItemMeta().getLore();
                String allLore = lore.toString().toLowerCase();

                Matcher valueMatcher = this.lifestealRegex.matcher(allLore);
                if (valueMatcher.find()) {
                    steal = steal + Integer.valueOf(valueMatcher.group(1));
                }

            }

        }

        ItemStack item = entity.getEquipment().getItemInHand();
        if ((item != null) &&
                (item.hasItemMeta()) &&
                (item.getItemMeta().hasLore())) {
            List lore = item.getItemMeta().getLore();
            String allLore = lore.toString().toLowerCase();

            Matcher valueMatcher = this.lifestealRegex.matcher(allLore);
            if (valueMatcher.find()) {
                steal = steal + Integer.valueOf(valueMatcher.group(1));
            }

        }

        return steal;
    }

    public int getCritDamage(LivingEntity entity) {
        if (!critAttack(entity)) {
            return 0;
        }
        Integer damage = 0;

        for (ItemStack item : entity.getEquipment().getArmorContents()) {
            if ((item != null) &&
                    (item.hasItemMeta()) &&
                    (item.getItemMeta().hasLore())) {
                List lore = item.getItemMeta().getLore();
                String allLore = lore.toString().toLowerCase();

                Matcher valueMatcher = this.critDamageRegex.matcher(allLore);
                if (valueMatcher.find()) {
                    damage = damage + Integer.valueOf(valueMatcher.group(1));
                }

            }

        }

        ItemStack item = entity.getEquipment().getItemInHand();
        if ((item != null) &&
                (item.hasItemMeta()) &&
                (item.getItemMeta().hasLore())) {
            List lore = item.getItemMeta().getLore();
            String allLore = lore.toString().toLowerCase();

            Matcher valueMatcher = this.critDamageRegex.matcher(allLore);
            if (valueMatcher.find()) {
                damage = damage + Integer.valueOf(valueMatcher.group(1));
            }

        }

        return damage;
    }

    private double getAttackCooldown(Player player) {
        if (!this.attackSpeedEnabled) {
            return 0.0D;
        }
        return LoreAttributes.config.getDouble("lore.attack-speed.base-delay") * 0.1D - getAttackSpeed(player) * 0.1D;
    }

    public void addAttackCooldown(String playerName) {
        if (!this.attackSpeedEnabled) {
            return;
        }
        Timestamp able = new Timestamp((long) (new Date().getTime() + getAttackCooldown(Bukkit.getPlayer(playerName)) * 1000.0D));

        this.attackLog.put(playerName, able);
    }

    public boolean canAttack(String playerName) {
        if (!this.attackSpeedEnabled) {
            return true;
        }
        if (!this.attackLog.containsKey(playerName)) {
            return true;
        }
        Date now = new Date();
        return now.after(this.attackLog.get(playerName));
    }

    private double getAttackSpeed(Player player) {
        if (player == null) {
            return 1.0D;
        }

        double speed = 1.0D;

        for (ItemStack item : player.getEquipment().getArmorContents()) {
            if ((item != null) &&
                    (item.hasItemMeta()) &&
                    (item.getItemMeta().hasLore())) {
                List lore = item.getItemMeta().getLore();
                String allLore = lore.toString().toLowerCase();

                Matcher valueMatcher = this.attackSpeedRegex.matcher(allLore);
                if (valueMatcher.find()) {
                    speed += Integer.valueOf(valueMatcher.group(1));
                }

            }

        }

        ItemStack item = player.getEquipment().getItemInHand();
        if ((item != null) &&
                (item.hasItemMeta()) &&
                (item.getItemMeta().hasLore())) {
            List lore = item.getItemMeta().getLore();
            String allLore = lore.toString().toLowerCase();

            Matcher valueMatcher = this.attackSpeedRegex.matcher(allLore);
            if (valueMatcher.find()) {
                speed += Integer.valueOf(valueMatcher.group(1));
            }

        }

        return speed;
    }

    public void applyHpBonus(LivingEntity entity) {
        if (!entity.isValid()) {
            return;
        }
        Integer hpToAdd = getHpBonus(entity);
        //entity.setMaxHealth(entity.getMaxHealth() + hpToAdd);
        entity.setMaxHealth(20 + hpToAdd);
        //entity.setHealth(entity.getHealth() + hpToAdd);
    }

    public int getHpBonus(LivingEntity entity) {
        Integer hpToAdd = 0;
        for (ItemStack item : entity.getEquipment().getArmorContents()) {
            if ((item != null) &&
                    (item.hasItemMeta()) &&
                    (item.getItemMeta().hasLore())) {
                hpToAdd = hpToAdd + this.getHealth(item);
            }
            if (hpToAdd < 0) hpToAdd = 0;
        }
        return hpToAdd;
    }

    public int getRegenBonus(LivingEntity entity) {
        if (!entity.isValid()) {
            return 0;
        }
        Integer regenBonus = 0;
        for (ItemStack item : entity.getEquipment().getArmorContents()) {
            if ((item != null) &&
                    (item.hasItemMeta()) &&
                    (item.getItemMeta().hasLore())) {
                List lore = item.getItemMeta().getLore();
                String allLore = lore.toString().toLowerCase();

                Matcher matcher = this.regenRegex.matcher(allLore);
                if (matcher.find()) {
                    regenBonus = regenBonus + Integer.valueOf(matcher.group(1));
                }
            }

        }

        return regenBonus;
    }

    public int getDamageBonus(LivingEntity entity) {
        if (!entity.isValid()) {
            return 0;
        }
        Integer damageMin = 0;
        Integer damageMax = 0;
        Integer damageBonus = 0;
        for (ItemStack item : entity.getEquipment().getArmorContents()) {
            if ((item != null) &&
                    (item.hasItemMeta()) &&
                    (item.getItemMeta().hasLore())) {
                List lore = item.getItemMeta().getLore();
                String allLore = lore.toString().toLowerCase();

                Matcher rangeMatcher = this.damageRangeRegex.matcher(allLore);
                Matcher valueMatcher = this.damageValueRegex.matcher(allLore);

                if (rangeMatcher.find()) {
                    damageMin = damageMin + Integer.valueOf(rangeMatcher.group(1));
                    damageMax = damageMax + Integer.valueOf(rangeMatcher.group(3));
                }
                if (valueMatcher.find()) {
                    damageBonus = damageBonus + Integer.valueOf(valueMatcher.group(1));
                }

            }

        }

        ItemStack item = entity.getEquipment().getItemInHand();
        if ((item != null) &&
                (item.hasItemMeta()) &&
                (item.getItemMeta().hasLore())) {
            List lore = item.getItemMeta().getLore();
            String allLore = lore.toString().toLowerCase();
            Matcher negValueMatcher = this.negitiveDamageValueRegex.matcher(allLore);
            Matcher rangeMatcher = this.damageRangeRegex.matcher(allLore);
            Matcher valueMatcher = this.damageValueRegex.matcher(allLore);
            if (rangeMatcher.find()) {
                damageMin = damageMin + Integer.valueOf(rangeMatcher.group(1));
                damageMax = damageMax + Integer.valueOf(rangeMatcher.group(3));
            }
            if (valueMatcher.find()) {
                damageBonus = damageBonus + Integer.valueOf(valueMatcher.group(1));
                if (negValueMatcher.find()) {
                    damageBonus = damageBonus - Integer.valueOf(negValueMatcher.group(1));
                }
            }

        }

        if (damageMax < 1) {
            damageMax = 1;
        }
        if (damageMin < 1) {
            damageMin = 1;
        }
        return (int) Math.round(Math.random() * (damageMax - damageMin) + damageMin + damageBonus + getCritDamage(entity));
    }

    public boolean useRangeOfDamage(LivingEntity entity) {
        if (!entity.isValid()) {
            return false;
        }
        for (ItemStack item : entity.getEquipment().getArmorContents()) {
            if ((item != null) &&
                    (item.hasItemMeta()) &&
                    (item.getItemMeta().hasLore())) {
                List lore = item.getItemMeta().getLore();
                String allLore = lore.toString().toLowerCase();

                Matcher rangeMatcher = this.damageRangeRegex.matcher(allLore);
                if (rangeMatcher.find()) {
                    return true;
                }
            }

        }

        ItemStack item = entity.getEquipment().getItemInHand();
        if ((item != null) &&
                (item.hasItemMeta()) &&
                (item.getItemMeta().hasLore())) {
            List lore = item.getItemMeta().getLore();
            String allLore = lore.toString().toLowerCase();

            Matcher rangeMatcher = this.damageRangeRegex.matcher(allLore);
            if (rangeMatcher.find()) {
                return true;
            }

        }

        return false;
    }

    public void displayLoreStats(Player sender) {
        HashSet<String> message = new HashSet<String>();

        if (getHpBonus(sender) != 0)
            message.add(ChatColor.GRAY + LoreAttributes.config.getString("lore.health.keyword") + ": " + ChatColor.WHITE + getHpBonus(sender));
        if (getRegenBonus(sender) != 0)
            message.add(ChatColor.GRAY + LoreAttributes.config.getString("lore.regen.keyword") + ": " + ChatColor.WHITE + getRegenBonus(sender));
        if (LoreAttributes.config.getBoolean("lore.attack-speed.enabled"))
            message.add(ChatColor.GRAY + LoreAttributes.config.getString("lore.attack-speed.keyword") + ": " + ChatColor.WHITE + getAttackSpeed(sender));
        if (getDamageBonus(sender) != 0)
            message.add(ChatColor.GRAY + LoreAttributes.config.getString("lore.damage.keyword") + ": " + ChatColor.WHITE + getDamageBonus(sender));
        if (getDodgeBonus(sender) != 0)
            message.add(ChatColor.GRAY + LoreAttributes.config.getString("lore.dodge.keyword") + ": " + ChatColor.WHITE + getDodgeBonus(sender) + "%");
        if (getCritChance(sender) != 0)
            message.add(ChatColor.GRAY + LoreAttributes.config.getString("lore.critical-chance.keyword") + ": " + ChatColor.WHITE + getCritChance(sender) + "%");
        if (getCritDamage(sender) != 0)
            message.add(ChatColor.GRAY + LoreAttributes.config.getString("lore.critical-damage.keyword") + ": " + ChatColor.WHITE + getCritDamage(sender));
        if (getLifeSteal(sender) != 0)
            message.add(ChatColor.GRAY + LoreAttributes.config.getString("lore.life-steal.keyword") + ": " + ChatColor.WHITE + getLifeSteal(sender));
        if (getArmorBonus(sender) != 0)
            message.add(ChatColor.GRAY + LoreAttributes.config.getString("lore.armor.keyword") + ": " + ChatColor.WHITE + getArmorBonus(sender));

        String newMessage = "";
        for (String toSend : message) {
            newMessage = newMessage + "     " + toSend;
            if (newMessage.length() > 40) {
                sender.sendMessage(newMessage);
                newMessage = "";
            }
        }
        if (newMessage.length() > 0) {
            sender.sendMessage(newMessage);
        }
        message.clear();
    }

    public int getHealth(ItemStack item) {
        int health = 0;
        if (item != null
                && item.hasItemMeta()
                && !item.getType().equals(Material.AIR)
                && item.getItemMeta().hasLore()) {
            List lore = item.getItemMeta().getLore();
            String allLore = lore.toString().toLowerCase();
            Matcher matcher = this.healthRegex.matcher(allLore);
            Matcher nematcher = this.negHealthRegex.matcher(allLore);
            if (matcher.find())
                health = health + Integer.valueOf(matcher.group(1));
            if (nematcher.find())
                health = health - Integer.valueOf(matcher.group(1));
        }
        return health;
    }

    public boolean itemIsSimilar(ItemStack item1, ItemStack item2) {
        boolean similar = false;
        if (item1 != null
                && !item1.getType().equals(Material.AIR)
                && item2 != null
                && !item2.getType().equals(Material.AIR)) {
            similar = item1.getDurability() == item2.getDurability()
                    && item1.getType().equals(item2.getType());
            if (item1.hasItemMeta() && item2.hasItemMeta())
                similar = similar && item1.getItemMeta().equals(item2.getItemMeta());

        }
        return similar;
    }
}