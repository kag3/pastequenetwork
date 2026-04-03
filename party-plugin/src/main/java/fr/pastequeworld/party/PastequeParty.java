package fr.pastequeworld.party;

import fr.pastequeworld.party.command.PartyCommand;
import fr.pastequeworld.party.data.PartyManager;
import fr.pastequeworld.party.listener.PartyListener;

import net.md_5.bungee.api.plugin.Plugin;

public class PastequeParty extends Plugin {

    public static final String CHANNEL = "PastequeParty";

    private PartyManager partyManager;

    @Override
    public void onEnable() {
        partyManager = new PartyManager();

        // Register plugin messaging channel for LabyRoyale integration
        getProxy().registerChannel(CHANNEL);

        getProxy().getPluginManager().registerCommand(this, new PartyCommand(this));
        getProxy().getPluginManager().registerListener(this, new PartyListener(this));

        getLogger().info("========================================");
        getLogger().info("  PastequeParty v" + getDescription().getVersion());
        getLogger().info("  Système de groupe - PastequeWorld Network");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getProxy().unregisterChannel(CHANNEL);
        getLogger().info("PastequeParty désactivé.");
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }
}
