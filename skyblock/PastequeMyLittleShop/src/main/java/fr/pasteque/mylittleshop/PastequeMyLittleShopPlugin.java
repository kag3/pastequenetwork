package fr.pasteque.mylittleshop;

import fr.pasteque.mylittleshop.command.MyShopAdminCommand;
import fr.pasteque.mylittleshop.command.MyShopCommand;
import fr.pasteque.mylittleshop.economy.PastequeEconomyBridge;
import fr.pasteque.mylittleshop.listener.ShopListener;
import fr.pasteque.mylittleshop.service.ShopManager;
import fr.pasteque.mylittleshop.service.SkyblockIslandBridge;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class PastequeMyLittleShopPlugin extends JavaPlugin {

    private PastequeEconomyBridge economyBridge;
    private SkyblockIslandBridge islandBridge;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.economyBridge = new PastequeEconomyBridge(this);
        this.islandBridge = new SkyblockIslandBridge(this);
        this.shopManager = new ShopManager(this, economyBridge, islandBridge);
        this.shopManager.load();

        MyShopCommand myShopCommand = new MyShopCommand(this, shopManager);
        getCommand("myshop").setExecutor(myShopCommand);
        getCommand("myshop").setTabCompleter(myShopCommand);

        MyShopAdminCommand adminCommand = new MyShopAdminCommand(this, shopManager);
        getCommand("myshopadmin").setExecutor(adminCommand);
        getCommand("myshopadmin").setTabCompleter(adminCommand);

        getServer().getPluginManager().registerEvents(new ShopListener(this, shopManager), this);
    }

    @Override
    public void onDisable() {
        if (shopManager != null) {
            shopManager.save();
        }
    }

    public PastequeEconomyBridge getEconomyBridge() {
        return economyBridge;
    }

    public SkyblockIslandBridge getIslandBridge() {
        return islandBridge;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    public String prefix() {
        return color(getConfig().getString("messages.prefix", "&2Pasteque &dMyLittleShop &7» "));
    }

    public String message(String path) {
        return prefix() + color(getConfig().getString(path, path));
    }
}
