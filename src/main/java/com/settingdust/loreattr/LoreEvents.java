package com.settingdust.loreattr;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LoreEvents implements Listener {

    public LoreEvents(LoreAttributes plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            public void run() {
                List<Player> players = new ArrayList<Player>(Bukkit.getServer().getOnlinePlayers());
                for (Player player : players) {
                    Inventory inv = player.getInventory();
                    ItemStack[] items = inv.getContents();
                    for (int i = 0; i < items.length; i++) {
                        int regen = LoreAttributes.loreManager.getDuraRegen(items[i]);
                        if (regen != 0) {
                            items[i].setDurability((short) (items[i].getDurability() + regen));
                        }
                    }
                    inv.setContents(items);
                }
            }
        }, 20L, 0L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void modifyEntityDamage(EntityDamageByEntityEvent event) {
        if ((event.isCancelled()) || (!(event.getEntity() instanceof LivingEntity))) {
            return;
        }

        if (LoreAttributes.loreManager.dodgedAttack((LivingEntity) event.getEntity())) {
            event.setDamage(0.0D);
            event.setCancelled(true);
            return;
        }

        if ((event.getDamager() instanceof LivingEntity)) {
            LivingEntity damager = (LivingEntity) event.getDamager();

            if ((damager instanceof Player)) {
                if (LoreAttributes.loreManager.canAttack(damager.getName())) {
                    LoreAttributes.loreManager.addAttackCooldown(damager.getName());
                } else {
                    if (!LoreAttributes.config.getBoolean("lore.attack-speed.display-message")) {
                        event.setCancelled(true);
                        return;
                    }
                    damager.sendMessage(LoreAttributes.config.getString("lore.attack-speed.message"));
                    event.setCancelled(true);
                    return;
                }

            }

            if (LoreAttributes.loreManager.useRangeOfDamage(damager))
                event.setDamage(Math.max(0, LoreAttributes.loreManager.getDamageBonus(damager) - LoreAttributes.loreManager.getArmorBonus((LivingEntity) event.getEntity())));
            else {
                event.setDamage(Math.max(0.0D, event.getDamage() + LoreAttributes.loreManager.getDamageBonus(damager) - LoreAttributes.loreManager.getArmorBonus((LivingEntity) event.getEntity())));
            }

            damager.setHealth(Math.min(damager.getMaxHealth(), damager.getHealth() + Math.min(LoreAttributes.loreManager.getLifeSteal(damager), event.getDamage())));
        } else if ((event.getDamager() instanceof Arrow)) {
            Arrow arrow = (Arrow) event.getDamager();
            if ((arrow.getShooter() != null) && ((arrow.getShooter() instanceof LivingEntity))) {
                LivingEntity damager = (LivingEntity) arrow.getShooter();

                if ((damager instanceof Player)) {
                    if (LoreAttributes.loreManager.canAttack(damager.getName())) {
                        LoreAttributes.loreManager.addAttackCooldown(damager.getName());
                    } else {
                        if (!LoreAttributes.config.getBoolean("lore.attack-speed.display-message")) {
                            event.setCancelled(true);
                            return;
                        }
                        damager.sendMessage(LoreAttributes.config.getString("lore.attack-speed.message"));
                        event.setCancelled(true);
                        return;
                    }

                }

                if (LoreAttributes.loreManager.useRangeOfDamage(damager))
                    event.setDamage(Math.max(0, LoreAttributes.loreManager.getDamageBonus(damager) - LoreAttributes.loreManager.getArmorBonus((LivingEntity) event.getEntity())));
                else {
                    event.setDamage(Math.max(0.0D, event.getDamage() + LoreAttributes.loreManager.getDamageBonus(damager)) - LoreAttributes.loreManager.getArmorBonus((LivingEntity) event.getEntity()));
                }

                damager.setHealth(Math.min(damager.getMaxHealth(), damager.getHealth() + Math.min(LoreAttributes.loreManager.getLifeSteal(damager), event.getDamage())));
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void applyHealthRegen(EntityRegainHealthEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (((event.getEntity() instanceof Player)) &&
                (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED)) {
            event.setAmount(event.getAmount() + LoreAttributes.loreManager.getRegenBonus((LivingEntity) event.getEntity()));

            if (event.getAmount() <= 0.0D)
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void checkBowRestriction(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (!LoreAttributes.loreManager.canUse((Player) event.getEntity(), event.getBow()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void checkCraftRestriction(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        for (ItemStack item : event.getInventory().getContents())
            if (!LoreAttributes.loreManager.canUse((Player) event.getWhoClicked(), item)) {
                event.setCancelled(true);
                return;
            }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void checkWeaponRestriction(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!LoreAttributes.loreManager.canUse((Player) event.getDamager(), ((Player) event.getDamager()).getItemInHand())) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void applyUnlimitDura(PlayerItemBreakEvent event) {
        if (LoreAttributes.loreManager.isUnlimitDura(event.getBrokenItem())) {
            Inventory inv = event.getPlayer().getInventory();
            ItemStack[] items = inv.getContents();
            ItemStack item = event.getBrokenItem();
            for (int i = 0; i < items.length; i++) {
                if (items[i] == item) {
                    items[i].setDurability((short) 1);
                    inv.setContents(items);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void applyExp(PlayerExpChangeEvent event) {
        if (event.getAmount() > 0) {
            event.setAmount(event.getAmount() + LoreAttributes.loreManager.getExp(event.getPlayer()) / 100 * event.getAmount());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void checkItemBound(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        String name = LoreAttributes.loreManager.getBound(item);
        if (name != null
                && name != event.getPlayer().getName()) {
            Player player = event.getPlayer();
            player.sendMessage(ChatColor.RED + "此物品只可由" + name + "使用！");
            event.setCancelled(true);
        }
    }
}