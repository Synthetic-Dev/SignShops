package me.syntheticdev.signshops;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Item;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class SignShopsManager {
    private ArrayList<Shop> shops;
    private Material[] containerTypes = new Material[]{Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL};

    public SignShopsManager() {
        this.shops = new ArrayList<>();
    }

    public boolean isValidContainer(Block block) {
        return Arrays.stream(containerTypes).anyMatch((material) -> material.equals(block.getType()));
    }

    private boolean isShopRaw(Block block) {
        return this.shops.stream().anyMatch((shop) ->
                shop.getContainer().getLocation().equals(block.getLocation())
                        || shop.getSign().getLocation().equals(block.getLocation()));
    }

    public boolean isShop(Block block) {
        if (!(this.isValidContainer(block) || Tag.SIGNS.isTagged(block.getType()))) return false;

        boolean is = this.isShopRaw(block);
        // Handle double chests
        if (!is && block.getType().equals(Material.CHEST) && ((Chest)block.getState()).getInventory() instanceof DoubleChestInventory) {
            DoubleChest doubleChest = (DoubleChest)((Chest)block.getState()).getInventory().getHolder();
            is = this.isShopRaw(((Chest)doubleChest.getLeftSide()).getBlock());
            if (!is) is = this.isShopRaw(((Chest)doubleChest.getRightSide()).getBlock());
        }
        return is;
    }

    public boolean isShopSignFormat(String[] lines) {
        if (!lines[0].trim().equalsIgnoreCase("[Shop]")) return false;

        short sellingAmount;
        short paymentAmount;
        try {
            sellingAmount = Short.parseShort(lines[1].trim());
            paymentAmount = Short.parseShort(lines[2].trim());
        } catch (NumberFormatException err) {
            return false;
        }

        if (sellingAmount > 0 && paymentAmount > 0 && lines[3].trim().isEmpty()) {
            return true;
        }
        return false;
    }

    public boolean isShopItem(ItemStack shopItem, ItemStack otherItem) {
        Map<Enchantment, Integer> shopItemEnchants = getEnchants(shopItem);
        Map<Enchantment, Integer> otherEnchants = getEnchants(otherItem);

        if ((shopItemEnchants == null) != (otherEnchants == null)) return false;
        if (shopItemEnchants != null && otherEnchants != null && !shopItemEnchants.equals(otherEnchants)) return false;

        ItemMeta shopItemMeta = shopItem.getItemMeta();
        ItemMeta otherMeta = otherItem.getItemMeta();
        boolean sellingIsDamageable = shopItemMeta instanceof Damageable;
        boolean otherIsDamageable = otherMeta instanceof Damageable;
        if (sellingIsDamageable != otherIsDamageable) return false;
        if (sellingIsDamageable && otherIsDamageable && ((Damageable)shopItemMeta).getDamage() != ((Damageable)otherMeta).getDamage()) return false;

        return true;
    }

    public ArrayList<Shop> getShops() {
        return this.shops;
    }

    @Nullable
    private Shop getShopRaw(Block block) {
        return this.shops.stream().filter((shop) ->
                shop.getContainer().getLocation().equals(block.getLocation())
                        || shop.getSign().getLocation().equals(block.getLocation())).findFirst().orElse(null);
    }

    @Nullable
    public Shop getShop(Block block) {
        Shop shop = this.getShopRaw(block);
        // Handle double chests
        if (shop == null && block.getType().equals(Material.CHEST) && ((Chest)block.getState()).getInventory() instanceof DoubleChestInventory) {
            DoubleChest doubleChest = (DoubleChest)((Chest)block.getState()).getInventory().getHolder();
            shop = this.getShopRaw(((Chest)doubleChest.getLeftSide()).getBlock());
            if (shop == null) shop = this.getShopRaw(((Chest)doubleChest.getRightSide()).getBlock());
        }
        return shop;
    }

    @Nullable
    public static Map<Enchantment, Integer> getEnchants(ItemStack item) {
        Map<Enchantment, Integer> enchants = null;
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta) {
            enchants = ((EnchantmentStorageMeta)meta).getStoredEnchants();
        }
        if (enchants == null) enchants = item.getEnchantments();
        return enchants != null && !enchants.isEmpty() ? enchants : null;
    }

    public static TextComponent getItemTextComponent(ItemStack item) {
        String itemName = Utils.toDisplayCase(item.getType().toString().replaceAll("_", " "));
        TextComponent text = new TextComponent(itemName);
        text.setColor(net.md_5.bungee.api.ChatColor.AQUA);
        text.setBold(true);

        Map<Enchantment, Integer> enchants = getEnchants(item);
        ItemMeta meta = item.getItemMeta();

        ComponentBuilder toolTip = new ComponentBuilder();
        toolTip.append(meta.hasDisplayName() ? meta.getDisplayName() : itemName);
        if (enchants != null && !meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS)) {
            toolTip.color(net.md_5.bungee.api.ChatColor.AQUA);
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                Enchantment enchant = entry.getKey();
                String name = Utils.toDisplayCase(enchant.getKey().getKey().replaceAll("_", " "));
                if (enchant.getMaxLevel() > 1) {
                    name += " " + Utils.toRomanNumerials(entry.getValue());
                }
                toolTip.append("\n" + name).color(net.md_5.bungee.api.ChatColor.GRAY);
            }
        }

        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable)meta;
            if (damageable.hasDamage()) {
                int maxDurability = item.getType().getMaxDurability();
                toolTip.append("\n\nDurability: " + (maxDurability - damageable.getDamage()) + " / " + maxDurability).reset().color(net.md_5.bungee.api.ChatColor.WHITE);
            }
        }

        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(toolTip.create()));
        text.setHoverEvent(hoverEvent);
        return text;
    }

    public void load() {
        File file = new File(SignShopsPlugin.getPlugin().getDataFolder().getAbsolutePath(), "shops.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (file.length() != 0L) {
            List<?> list = config.getList("shops");
            if (list instanceof ArrayList) {
                ArrayList<Shop> shops = (ArrayList<Shop>)list;
                boolean removed = shops.removeIf(shop -> shop == null || shop.getContainer() == null || shop.getSign() == null);
                this.shops = shops;

                if (!removed) return;
                try {
                    config.set("shops", this.shops);
                    config.save(file);
                } catch (IOException err) {
                    err.printStackTrace();
                }
            }
        }
    }

    @Nullable
    public Shop createShop(SignChangeEvent event) {
        Block signBlock = event.getBlock();
        Directional directional = (Directional)signBlock.getBlockData();
        Block placedAgainst = signBlock.getRelative(directional.getFacing().getOppositeFace());
        if (!this.isValidContainer(placedAgainst)) return null;

        Container container = (Container)placedAgainst.getState();
        Sign sign = (Sign)signBlock.getState();

        Player player = event.getPlayer();
        Inventory inventory = container.getInventory();
        ItemStack itemSelling = inventory.getItem(0);
        ItemStack itemPayment = inventory.getItem(1);
        if (itemSelling == null) {
            player.sendMessage(ChatColor.RED + "[Shop] Could not find item to sell!");
            return null;
        }
        if (itemPayment == null) {
            player.sendMessage(ChatColor.RED + "[Shop] Could not find item for payment!");
            return null;
        }

        String[] lines = event.getLines();

        itemSelling = itemSelling.clone();
        itemPayment = itemPayment.clone();
        int sellingAmount = Short.parseShort(lines[1].trim());
        int paymentAmount = Short.parseShort(lines[2].trim());
        itemSelling.setAmount(sellingAmount);
        itemPayment.setAmount(paymentAmount);

        event.setLine(0, ChatColor.BOLD + "[Shop]");
        event.setLine(1, ChatColor.GREEN + "Selling: " + sellingAmount);
        event.setLine(2, ChatColor.YELLOW + "Cost: " + paymentAmount);
        event.setLine(3, player.getDisplayName());

        Shop shop = new Shop(player, container, sign, itemSelling, itemPayment);

        try {
            File file = new File(SignShopsPlugin.getPlugin().getDataFolder().getAbsolutePath(), "shops.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            this.shops.add(shop);
            config.set("shops", this.shops);
            config.save(file);
        } catch (IOException err) {
            err.printStackTrace();
        }

        return shop;
    }

    public boolean destroyShop(BlockBreakEvent event, Shop shop) throws IOException {
        Player player = event.getPlayer();
        if (!shop.isOwner(player) && !player.hasPermission("signshops.admin")) {
            player.sendMessage(ChatColor.RED + "[Shop] This is not your shop.");
            return false;
        }

        Logger logger = SignShopsPlugin.getPlugin().getLogger();
        Sign sign = shop.getSign();
        Container container = shop.getContainer();

        if (!(container.getLocation().equals(event.getBlock().getLocation())
                || sign.getLocation().equals(event.getBlock().getLocation()))) {
            //logger.info("Broke non-primary container");
            return true;
        }

        if (sign != null) {
            for (int i = 0; i < 4; i++) {
                sign.setLine(i, "");
            }
            sign.setLine(1, "[Broken Shop]");
            sign.update();
        }

        File file = new File(SignShopsPlugin.getPlugin().getDataFolder().getAbsolutePath(), "shops.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        this.shops.remove(shop);

        config.set("shops", this.shops);
        config.save(file);
        return true;
    }

    public void handleSignClick(PlayerInteractEvent event, Shop shop) {
        Player player = event.getPlayer();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            ItemStack itemSelling = shop.getItemSelling();
            ItemStack itemPayment = shop.getItemPayment();
            AbstractMap.Entry<ChatColor, String> status = shop.getStatusAsEntry();
            ComponentBuilder builder = new ComponentBuilder("[Shop] Selling " + itemSelling.getAmount() + " ")
                    .color(net.md_5.bungee.api.ChatColor.YELLOW)
                    .append(getItemTextComponent(itemSelling))
                    .append(" for " + itemPayment.getAmount() + " ")
                    .reset().color(net.md_5.bungee.api.ChatColor.YELLOW)
                    .append(getItemTextComponent(itemPayment))
                    .append(". ")
                    .reset().color(net.md_5.bungee.api.ChatColor.YELLOW)
                    .append(status.getValue())
                    .color(net.md_5.bungee.api.ChatColor.of(status.getKey().name()));
            player.spigot().sendMessage(builder.create());
            return;
        }

        if (shop.isOwner(player)) {
            this.handleShopOpen(event, shop);
        } else {
            shop.makeDeal(event);
        }
    }

    public void handleShopOpen(PlayerInteractEvent event, @Nullable Shop shop) {
        Player player = event.getPlayer();
        if (player.isSneaking()) return;

        Block block = event.getClickedBlock();
        if (shop == null) shop = this.getShop(block);
        if (shop == null) return;

        if (!shop.isOwner(player) && !player.hasPermission("signshops.admin")) {
            if (this.isValidContainer(block)) {
                player.sendMessage(ChatColor.RED + "[Shop] This is not your shop.");
                player.playSound(block.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 1f, 1f);
            }
            return;
        }

        player.openInventory(shop.getContainer().getInventory());
    }
}
