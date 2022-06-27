package me.syntheticdev.signshops;

import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Logger;

public class Shop implements ConfigurationSerializable {
    private OfflinePlayer owner;
    private ItemStack itemSelling;
    private ItemStack itemPayment;
    private @Nullable Container container;
    private @Nullable Sign sign;

    public Shop(OfflinePlayer owner, Container container, Sign sign, ItemStack itemSelling, ItemStack itemPayment) {
        this.owner = owner;
        this.container = container;
        this.sign = sign;
        this.itemSelling = itemSelling;
        this.itemPayment = itemPayment;
    }

    public Shop(OfflinePlayer owner, Location container, Location sign, ItemStack itemSelling, ItemStack itemPayment) {
        Block containerBlock = container.getBlock();
        if (SignShopsPlugin.getManager().isValidContainer(containerBlock)) {
            this.container = (Container)containerBlock.getState();
        }
        Block signBlock = sign.getBlock();
        if (Tag.SIGNS.isTagged(signBlock.getType())) {
            this.sign = (Sign)signBlock.getState();
        }
        this.owner = owner;
        this.itemSelling = itemSelling;
        this.itemPayment = itemPayment;
    }

//    public OfflinePlayer getOwner() {
//        return this.owner;
//    }

    @Nullable
    public Container getContainer() {
        return this.container;
    }

    @Nullable
    public Sign getSign() { return this.sign; }

    public void setContainer(Container container) {
        this.container = container;
    }

    public boolean isOwner(Player player) {
        return this.owner.getUniqueId().equals(player.getUniqueId());
    }

    public ItemStack getItemSelling() {
        return this.itemSelling.clone();
    }

    public ItemStack getItemPayment() {
        return this.itemPayment.clone();
    }

    public Inventory getInventory() {
        Inventory inventory = this.container.getInventory();
        if (inventory.getHolder() instanceof DoubleChest) {
            return ((DoubleChest)inventory.getHolder()).getInventory();
        }
        return this.container.getInventory();
    }

    public int getStock() {
        int itemSellingAmount = 0;

        Inventory inventory = this.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (item == null || !item.getType().equals(this.itemSelling.getType())) continue;
            if (!SignShopsPlugin.getManager().isShopItem(this.itemSelling, item)) continue;

            itemSellingAmount += item.getAmount();
        }

        return (int)Math.floor((double)itemSellingAmount / (double)this.itemSelling.getAmount());
    }

    public AbstractMap.Entry<ChatColor, String> getStatusAsEntry() {
        if (!this.hasSpace()) return new AbstractMap.SimpleEntry(ChatColor.RED, "Full");
        if (this.getStock() > 0) return new AbstractMap.SimpleEntry(ChatColor.GREEN, "Open");
        return new AbstractMap.SimpleEntry(ChatColor.RED, "Out of Stock");
    }

    public String getStatus() {
        Map.Entry<ChatColor, String> entry = this.getStatusAsEntry();
        return entry.getKey() + entry.getValue();
    }

    public boolean hasSpace() {
        int itemPaymentFreeSpace = 0;

        Inventory inventory = this.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (item == null || !item.getType().equals(this.itemPayment.getType())) continue;
            if (!SignShopsPlugin.getManager().isShopItem(this.itemPayment, item)) continue;

            itemPaymentFreeSpace += item.getMaxStackSize() - item.getAmount();
            if (itemPaymentFreeSpace >= this.itemPayment.getAmount()) return true;
        }
        return inventory.firstEmpty() != -1;
    }

    public boolean canMakeDeal() {
        return this.hasSpace() && this.getStock() > 0;
    }

    public boolean canPlayerAfford(PlayerInteractEvent event) {
        int paymentItemAmount = 0;
        ItemStack handItem = event.getItem();
        if (handItem == null || !handItem.getType().equals(this.itemPayment.getType())) return false;

        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getStorageContents()) {
            if (item == null || !item.getType().equals(this.itemPayment.getType())) continue;
            if (!SignShopsPlugin.getManager().isShopItem(this.itemPayment, item)) continue;
            paymentItemAmount += item.getAmount();
            if (paymentItemAmount >= this.itemPayment.getAmount()) return true;
        }
        return false;
    }

    public void makeDeal(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!this.canPlayerAfford(event)) {
            ComponentBuilder builder = new ComponentBuilder("[Shop] You don't have enough ").color(net.md_5.bungee.api.ChatColor.RED)
                .append(SignShopsManager.getItemTextComponent(this.itemPayment))
                .append(" to pay!").reset().color(net.md_5.bungee.api.ChatColor.RED);
            player.spigot().sendMessage(builder.create());
            return;
        }

        if (!this.canMakeDeal()) {
            player.sendMessage(ChatColor.RED + "[Shop] Sorry, the shop is: " + this.getStatus());
        }

        {
            int paymentAmountToTake = this.itemPayment.getAmount();
            ItemStack handItem = event.getItem();
            PlayerInventory inventory = player.getInventory();
            if (paymentAmountToTake < handItem.getAmount()) {
                handItem.setAmount(handItem.getAmount() - paymentAmountToTake);
            } else {
                paymentAmountToTake -= handItem.getAmount();
                inventory.setItem(event.getHand(), null);

                for (int i = 0; i < inventory.getSize(); i++) {
                    ItemStack item = inventory.getItem(i);
                    if (item == null || !item.getType().equals(this.itemPayment.getType())) continue;
                    if (!SignShopsPlugin.getManager().isShopItem(this.itemPayment, item)) continue;

                    if (paymentAmountToTake < item.getAmount()) {
                        item.setAmount(item.getAmount() - paymentAmountToTake);
                        break;
                    }

                    paymentAmountToTake -= item.getAmount();
                    inventory.setItem(i, null);
                    if (paymentAmountToTake == 0) break;
                }
            }

            ItemStack itemsToGive = this.itemSelling.clone();
            HashMap<Integer, ItemStack> result = inventory.addItem(itemsToGive);
            if (!result.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "[Shop] Your inventory is full, dropping excess items.");

                World world = player.getWorld();
                for (ItemStack item : result.values()) {
                    world.dropItem(player.getLocation(), item);
                }
            }
        }

        {
            int sellingAmountToTake = this.itemSelling.getAmount();
            Inventory inventory = this.getInventory();

            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item == null || !item.getType().equals(this.itemSelling.getType())) continue;
                if (!SignShopsPlugin.getManager().isShopItem(this.itemSelling, item)) continue;

                if (sellingAmountToTake < item.getAmount()) {
                    item.setAmount(item.getAmount() - sellingAmountToTake);
                    break;
                }

                sellingAmountToTake -= item.getAmount();
                inventory.setItem(i, null);
                if (sellingAmountToTake == 0) break;
            }

            ItemStack itemsToAdd = this.itemPayment.clone();
            inventory.addItem(itemsToAdd);
        }
    }

    @Override
    public Map<String, Object> serialize() {
        HashMap<String, Object> serializedMap = new HashMap();
        serializedMap.put("owner", this.owner.getUniqueId().toString());
        serializedMap.put("container", this.container.getLocation());
        serializedMap.put("sign", this.sign.getLocation());
        serializedMap.put("itemSelling", this.itemSelling);
        serializedMap.put("itemPayment", this.itemPayment);
        return serializedMap;
    }

    public static Shop deserialize(Map<String, Object> serializedMap) {
        String ownerUUID = (String)serializedMap.get("owner");
        OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));

        Location containerLocation = (Location)serializedMap.get("container");
        Location signLocation = (Location)serializedMap.get("sign");

        ItemStack itemSelling = (ItemStack)serializedMap.get("itemSelling");
        ItemStack itemPayment = (ItemStack)serializedMap.get("itemPayment");

        Shop deserialized = new Shop(owner, containerLocation, signLocation, itemSelling, itemPayment);
        return deserialized;
    }
}
