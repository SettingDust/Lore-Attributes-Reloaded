package com.settingdust.loreattr;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LoreEvents implements Listener {
    LoreAttributes instance;

    public LoreEvents(LoreAttributes plugin) {
        instance = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            public void run() {
                List<Player> players = new ArrayList<Player>(Bukkit.getServer().getOnlinePlayers());
                for (Player player : players) {
                    Inventory inv = player.getInventory();
                    ItemStack[] items = inv.getContents();
                    for (int i = 0; i < items.length; i++) {
                        int regen = LoreAttributes.loreManager.getDuraRegen(items[i]);
                        if (regen != 0 && items[i].getDurability() > 0) {
                            items[i].setDurability((short) (items[i].getDurability() - regen));
                            inv.setItem(i, items[i]);
                        }
                    }
                    items = player.getEquipment().getArmorContents();
                    for (int i = 0; i < items.length; i++) {
                        int regen = LoreAttributes.loreManager.getDuraRegen(items[i]);
                        if (regen != 0 && items[i].getDurability() > 0) {
                            items[i].setDurability((short) (items[i].getDurability() - regen));
                        }
                    }
                }
            }
        }, 0L, 60L);
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

    @EventHandler(priority = EventPriority.NORMAL)
    public void checkArmorRestriction(InventoryClickEvent event) {
        ItemStack item = event.getCursor();
        if (event.getWhoClicked() instanceof Player) {
            if (event.getSlotType().equals(InventoryType.SlotType.ARMOR)) {
                event.setCancelled(
                        !LoreAttributes.loreManager.canUse((Player) event.getWhoClicked(), item)
                );
            } else if (event.getClick().equals(ClickType.SHIFT_LEFT)
                    || event.getClick().equals(ClickType.SHIFT_RIGHT)) {
                final LivingEntity livingEntity = event.getWhoClicked();
                item = event.getCurrentItem();
                final ItemStack[] armors = livingEntity.getEquipment().getArmorContents();
                final InventoryClickEvent e = event;
                final ItemStack fitem = item;
                Bukkit.getScheduler().runTaskLater(instance, new Runnable() {
                    public void run() {
                        ItemStack[] nowAromors = livingEntity.getEquipment().getArmorContents();
                        for (int i = 0; i < armors.length; i++) {
                            if (nowAromors != null
                                    && !nowAromors[i].getType().equals(Material.AIR)
                                    && !LoreAttributes.loreManager.itemIsSimilar(armors[i], nowAromors[i])
                                    && !LoreAttributes.loreManager.canUse((Player) e.getWhoClicked(), fitem)) {
                                nowAromors[i] = null;
                                fitem.setAmount(1);
                                ((Player) livingEntity).getInventory().addItem(fitem);
                                livingEntity.getEquipment().setArmorContents(nowAromors);
                            }
                        }
                    }
                }, 0L);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void applyHealth(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            final LivingEntity livingEntity = event.getWhoClicked();
            ItemStack item = event.getCursor();
            if (event.getSlotType().equals(InventoryType.SlotType.ARMOR) && !event.getClick().equals(ClickType.DOUBLE_CLICK)) {
                event.getWhoClicked().setMaxHealth(livingEntity.getMaxHealth()
                        + LoreAttributes.loreManager.getHealth(item));
                event.getWhoClicked().setHealth(livingEntity.getMaxHealth());
                item = event.getCurrentItem();
                event.getWhoClicked().setMaxHealth(livingEntity.getMaxHealth()
                        - LoreAttributes.loreManager.getHealth(item));
                event.getWhoClicked().setHealth(livingEntity.getMaxHealth());
            } else if (event.getClick().equals(ClickType.SHIFT_LEFT)
                    || event.getClick().equals(ClickType.SHIFT_RIGHT)) {
                item = event.getCurrentItem();
                final ItemStack[] armors = livingEntity.getEquipment().getArmorContents();
                final InventoryClickEvent e = event;
                final ItemStack fitem = item;
                Bukkit.getScheduler().runTaskLater(instance, new Runnable() {
                    public void run() {
                        for (int i = 0; i < armors.length; i++) {
                            if (livingEntity.getEquipment().getArmorContents()[i] != null
                                    && !livingEntity.getEquipment().getArmorContents()[i].getType().equals(Material.AIR)
                                    && !LoreAttributes.loreManager.itemIsSimilar(armors[i],
                                    livingEntity.getEquipment().getArmorContents()[i])) {
                                e.getWhoClicked().setMaxHealth(livingEntity.getMaxHealth()
                                        + LoreAttributes.loreManager.getHealth(fitem));
                                e.getWhoClicked().setHealth(livingEntity.getMaxHealth());
                            }
                        }
                    }
                }, 0L);
            }
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
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void applyUnlimitDura(PlayerItemDamageEvent event) {
        if (LoreAttributes.loreManager.isUnlimitDura(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void applyExp(PlayerExpChangeEvent event) {
        if (event.getAmount() > 0) {
            int exp = event.getAmount() + LoreAttributes.loreManager.getExp(event.getPlayer()) / 100 * event.getAmount();
            event.setAmount(exp);
        }
    }
}