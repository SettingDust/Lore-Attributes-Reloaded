package com.settingdust.loreattr;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LoreEvents implements Listener {
    LoreAttributes instance;

    public LoreEvents(LoreAttributes plugin) {
        instance = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
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
    public void applyHealth(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player && !event.isCancelled()) {
            final LivingEntity livingEntity = event.getWhoClicked();
            ItemStack item = event.getCursor();
            if (event.getSlotType().equals(InventoryType.SlotType.ARMOR) && !event.getClick().equals(ClickType.DOUBLE_CLICK)) {
                if (!LoreAttributes.loreManager.canUse((Player) livingEntity, item)) {
                    event.setCancelled(true);
                } else {
                    event.getWhoClicked().setMaxHealth(livingEntity.getMaxHealth()
                            + LoreAttributes.loreManager.getHealth(item));
                    event.getWhoClicked().setHealth(livingEntity.getMaxHealth());
                }

                item = event.getCurrentItem();
                if (LoreAttributes.loreManager.canUse((Player) livingEntity, item)) {
                    event.getWhoClicked().setMaxHealth(livingEntity.getMaxHealth()
                            - LoreAttributes.loreManager.getHealth(item));
                    event.getWhoClicked().setHealth(livingEntity.getMaxHealth());
                }
                item = event.getCurrentItem();
                if (!LoreAttributes.loreManager.canUse((Player) livingEntity, item)) {
                    event.setCancelled(true);
                }
            } else if (event.getClick().equals(ClickType.SHIFT_LEFT)
                    || event.getClick().equals(ClickType.SHIFT_RIGHT)) {
                item = event.getCurrentItem();
                final ItemStack[] armors = livingEntity.getEquipment().getArmorContents();
                final ItemStack fitem = item;
                Bukkit.getScheduler().runTaskLater(instance, new Runnable() {
                    public void run() {
                        ItemStack[] nowArmors = livingEntity.getEquipment().getArmorContents();
                        for (int i = 0; i < armors.length; i++) {
                            if (nowArmors != null
                                    && !nowArmors[i].getType().equals(Material.AIR)
                                    && !LoreAttributes.loreManager.itemIsSimilar(armors[i], nowArmors[i])) {
                                if (!LoreAttributes.loreManager.canUse((Player) livingEntity, fitem)) {
                                    nowArmors[i] = null;
                                    fitem.setAmount(1);
                                    ((Player) livingEntity).getInventory().addItem(fitem);
                                    livingEntity.getEquipment().setArmorContents(armors);
                                } else {
                                    livingEntity.setMaxHealth(livingEntity.getMaxHealth()
                                            + LoreAttributes.loreManager.getHealth(fitem));
                                    livingEntity.setHealth(livingEntity.getMaxHealth());
                                }
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
    /*
    /**
     * A bad health apply
     *
     * @param event
     *
    @EventHandler(priority = EventPriority.NORMAL)
    public void applyHealthClose(InventoryCloseEvent event) {
        LoreAttributes.loreManager.applyHpBonus(event.getPlayer());
    }

    /**
     * A bad health apply2
     *
     * @param event
     *
    @EventHandler(priority = EventPriority.NORMAL)
    public void applyHealthRespawn(PlayerRespawnEvent event) {
        LoreAttributes.loreManager.applyHpBonus(event.getPlayer());
    }

    /**
     * A bad health apply3
     *
     * @param event
     *
    @EventHandler(priority = EventPriority.NORMAL)
    public void applyHealthJoin(PlayerJoinEvent event) {
        final PlayerJoinEvent e = event;
        Bukkit.getScheduler().runTaskLater(instance, new Runnable() {
            public void run() {
                LoreAttributes.loreManager.applyHpBonus(e.getPlayer());
                e.getPlayer().setHealth(e.getPlayer().getMaxHealth());
            }
        }, 0L);
    }

    */

    @EventHandler(priority = EventPriority.NORMAL)
    public void checkArmorRestriction(PlayerInteractEvent event) {
        final Player livingEntity = event.getPlayer();
        final ItemStack[] armors = livingEntity.getEquipment().getArmorContents();
        final ItemStack fitem = event.getItem();
        Bukkit.getScheduler().runTaskLater(instance, new Runnable() {
            public void run() {
                ItemStack[] nowArmors = livingEntity.getEquipment().getArmorContents();
                for (int i = 0; i < armors.length; i++) {
                    if (nowArmors != null
                            && !nowArmors[i].getType().equals(Material.AIR)) {
                        if (!LoreAttributes.loreManager.canUse(livingEntity, fitem) &&
                                !LoreAttributes.loreManager.itemIsSimilar(armors[i], nowArmors[i])) {
                            nowArmors[i] = armors[i];
                            fitem.setAmount(1);
                            livingEntity.getInventory().addItem(fitem);
                            livingEntity.getEquipment().setArmorContents(nowArmors);
                        } else if (LoreAttributes.loreManager.itemIsSimilar(nowArmors[i], fitem)) {
                            LoreAttributes.loreManager.applyHpBonus(livingEntity);
                            livingEntity.setMaxHealth(livingEntity.getMaxHealth()
                                    + LoreAttributes.loreManager.getHealth(fitem));
                            livingEntity.setHealth(livingEntity.getMaxHealth());
                        }
                        if (!LoreAttributes.loreManager.canUse(livingEntity, nowArmors[i])) {
                            livingEntity.getInventory().addItem(nowArmors[i]);
                            nowArmors[i] = null;
                            livingEntity.getEquipment().setArmorContents(nowArmors);
                        }
                    }
                }
            }
        }, 0L);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void applyHatHealth(PlayerCommandPreprocessEvent event) {
        if (event.getMessage().equalsIgnoreCase("/hat")) {
            Player player = event.getPlayer();
            ItemStack item = player.getItemInHand();
            if (LoreAttributes.loreManager.canUse(player, item)) {
                player.setMaxHealth(player.getMaxHealth()
                        - LoreAttributes.loreManager.getHealth(player.getEquipment().getHelmet()));
                player.setMaxHealth(player.getMaxHealth()
                        + LoreAttributes.loreManager.getHealth(item));
                player.setHealth(player.getMaxHealth());
            } else {
                event.setCancelled(true);
            }
        }
    }
}