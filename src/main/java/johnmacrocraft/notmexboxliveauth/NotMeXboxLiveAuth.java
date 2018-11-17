/*
 *
 * NotMeXboxLiveAuth
 *
 * Copyright © 2018 Johnmacrocraft
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 */

package johnmacrocraft.notmexboxliveauth;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerKickEvent;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;

import java.io.File;
import java.util.Set;

public class NotMeXboxLiveAuth extends PluginBase implements Listener {

	public Config xboxlist;
	public Config prefixes;

	@Override
	public void onEnable() {
		if(!this.getDataFolder().isDirectory()) {
			this.getDataFolder().mkdir();
		}
		if(!new File(this.getDataFolder() + "/config.yml").isFile()) {
			this.saveDefaultConfig();
		}
		Boolean invert = this.useInvert();
		if(this.getServer().getPropertyBoolean("xbox-auth") == invert) {
			this.getLogger().warning("To use NotMeXboxLiveAuth, you must " +
					(invert ? "disable (invert mode enabled)" : "enable (invert mode disabled)") +
					" online mode in server.properties. Set value of xbox-auth to " +
					(invert ? "false" : "true") + " to " + (invert ? "disable" : "enable") + " online mode."
			);
			this.getServer().getPluginManager().disablePlugin(this);
			return;
		}
		this.xboxlist = new Config(this.getDataFolder() + "/xbox-list.txt", Config.ENUM);
		this.prefixes = new Config(this.getDataFolder() + "/prefixes.txt", Config.ENUM);
		this.getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		switch(command.getName()) {
			case "xboxlist":
				if(args.length == 0 || args.length > 3) {
					sender.sendMessage(new TranslationContainer("commands.generic.usage", command.getUsage()));
					return true;
				}

				if(args.length == 1) {
					if(this.badPerm(sender, args[0].toLowerCase())) {
						return true;
					}
					switch(args[0].toLowerCase()) {
						case "add":
						case "remove":
							sender.sendMessage(new TranslationContainer("commands.generic.usage", "/xboxlist " + args[0].toLowerCase() + " <player>"));
							return true;

						case "invert":
							sender.sendMessage(new TranslationContainer("commands.generic.usage", "/xboxlist invert <bool|state>"));
							return true;

						case "prefix":
							sender.sendMessage(new TranslationContainer("commands.generic.usage", "/xboxlist prefix <add|remove|list|reload> [prefix]"));
							return true;

						case "list":
							Set<String> entries = this.xboxlist.getAll().keySet();
							sender.sendMessage(TextFormat.AQUA + "There are " + entries.size() + " xboxlisted players:");
							sender.sendMessage(String.join(", ", entries));
							return true;

						case "reload":
							this.reloadXboxlist();
							sender.sendMessage(TextFormat.GREEN + "Reloaded the xboxlist");
							return true;

						default:
							sender.sendMessage(new TranslationContainer("commands.generic.usage", command.getUsage()));
							return true;
					}
				} else if(args.length == 2) {
					if(this.badPerm(sender, args[0].toLowerCase())) {
						return true;
					}
					switch(args[0].toLowerCase()) {
						case "add":
							this.addXboxlist(args[1]);
							sender.sendMessage(TextFormat.GREEN + "Added " + args[1] + " to the xboxlist");
							return true;

						case "remove":
							this.removeXboxlist(args[1]);
							sender.sendMessage(TextFormat.GREEN + "Removed " + args[1] + " from the xboxlist");
							return true;

						case "invert":
							Boolean invert = this.isBoolean(args[1]); //Use homemade method to match PHP filter_var
							if(invert != null) {
								this.setInvert(invert);
								sender.sendMessage(TextFormat.GREEN + (invert ? "Enabled" : "Disabled") + " invert mode - please " + (invert ? "disable" : "enable") + " online mode.");
							} else if(args[1].equals("state")) {
								sender.sendMessage(TextFormat.AQUA + "Invert mode is currently " + (this.useInvert() ? "enabled" : "disabled") + ".");
							} else {
								sender.sendMessage(new TranslationContainer("commands.generic.usage", "/xboxlist invert <bool|state>"));
							}
							return true;

						case "prefix":
							switch(args[1].toLowerCase()) {
								case "add":
								case "remove":
									sender.sendMessage(new TranslationContainer("commands.generic.usage", "/xboxlist prefix " + args[1].toLowerCase() + " <prefix>"));
									return true;

								case "list":
									Set<String> prefixes = this.prefixes.getAll().keySet();
									sender.sendMessage(TextFormat.AQUA + "There are " + prefixes.size() + " guest prefixes:");
									sender.sendMessage(String.join(", ", prefixes));
									return true;

								case "reload":
									this.reloadPrefixes();
									sender.sendMessage(TextFormat.GREEN + "Reloaded the guest prefix list");
									return true;

								default:
									sender.sendMessage(new TranslationContainer("commands.generic.usage", "/xboxlist prefix <add|remove|list> [prefix]"));
									return true;
							}
					}
				} else if(args.length == 3) {
					if(this.badPerm(sender, args[0].toLowerCase())) {
						return true;
					}
					switch(args[0].toLowerCase()) {
						case "prefix":
							if(this.useInvert()) {
								sender.sendMessage(TextFormat.YELLOW + "Please disable invert mode before trying to use guest prefix");
							}
							switch(args[1].toLowerCase()) {
								case "add":
									this.addPrefix(args[2]);
									sender.sendMessage(TextFormat.GREEN + "Added " + args[2] + " to the guest prefix list");
									return true;

								case "remove":
									this.removePrefix(args[2]);
									sender.sendMessage(TextFormat.GREEN + "Removed " + args[2] + " from the guest prefix list");
									return true;
							}
					}
				}
		}
		return true;
	}

	private Boolean isBoolean(String str) {
		switch(str.toLowerCase()) {
			case "true":
			case "yes":
			case "on":
			case "1":
				return true;
			case "false":
			case "no":
			case "off":
			case "0":
				return false;
			default:
				return null;
		}
	}

	private boolean badPerm(CommandSender sender, String perm) {
		if(!sender.hasPermission("notmexboxliveauth.command.xboxlist." + perm)) {
			sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
			return true;
		}
		return false;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerKick(PlayerKickEvent event) {
		String name = event.getPlayer().getName().toLowerCase();
		if((!event.getPlayer().getLoginChainData().isXboxAuthed() && !this.useInvert()) && (this.xboxlist.exists(name) || this.startsWithPrefix(name))) {
			event.setCancelled();
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		if(!event.getPlayer().getLoginChainData().isXboxAuthed() && this.useInvert() && this.xboxlist.exists(event.getPlayer().getName().toLowerCase())) {
			event.getPlayer().kick("disconnectionScreen.notAuthenticated", false);
		}
	}

	public void addXboxlist(String name) {
		this.xboxlist.set(name.toLowerCase(), true);
		this.xboxlist.save();
	}

	public void removeXboxlist(String name) {
		this.xboxlist.remove(name.toLowerCase());
		this.xboxlist.save();
	}

	public void reloadXboxlist() {
		this.xboxlist.reload();
	}

	public void setInvert(boolean value) {
		this.getConfig().set("invert", value);
		this.getConfig().save();
	}

	public boolean useInvert() {
		return this.getConfig().getBoolean("invert");
	}

	public void addPrefix(String prefix) {
		this.prefixes.set(prefix.toLowerCase(), true);
		this.prefixes.save();
	}

	public void removePrefix(String prefix) {
		this.prefixes.remove(prefix.toLowerCase());
		this.prefixes.save();
	}

	public void reloadPrefixes() {
		this.prefixes.reload();
	}

	public boolean startsWithPrefix(String name) {
		for(String prefixes : this.prefixes.getAll().keySet()) {
			if(name.indexOf(prefixes) == 0) {
				return true;
			}
		}
		return false;
	}
}