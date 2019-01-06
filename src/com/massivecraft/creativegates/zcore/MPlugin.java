package com.massivecraft.creativegates.zcore;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.massivecraft.creativegates.zcore.util.Perm;
import com.massivecraft.creativegates.zcore.util.TextUtil;

public abstract class MPlugin extends JavaPlugin {

	// Some utils
	public TextUtil txt;
	public Perm perm;

	// Persist related
	public Gson gson;

	// Listeners
	// private MPluginSecretPlayerListener mPluginSecretPlayerListener;
	// private MPluginSecretServerListener mPluginSecretServerListener;

	// Our stored base commands
	private final List<MCommand<?>> baseCommands = new ArrayList<MCommand<?>>();

	public List<MCommand<?>> getBaseCommands() {
		return this.baseCommands;
	}

	// -------------------------------------------- //
	// ENABLE
	// -------------------------------------------- //
	private long timeEnableStart;

	public boolean preEnable() {
		log("=== ENABLE START ===");
		timeEnableStart = System.currentTimeMillis();

		// Ensure basefolder exists!
		this.getDataFolder().mkdirs();

		// Create Utility Instances
		this.perm = new Perm(this);

		this.gson = this.getGsonBuilder().create();

		this.txt = new TextUtil();

		return true;
	}

	public void postEnable() {
		log("=== ENABLE DONE (Took " + (System.currentTimeMillis() - timeEnableStart) + "ms) ===");
	}

	// -------------------------------------------- //
	// Some inits...
	// You are supposed to override these in the plugin if you aren't satisfied with the defaults
	// The goal is that you always will be satisfied though.
	// -------------------------------------------- //

	public GsonBuilder getGsonBuilder() {
		return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.VOLATILE);
	}

	// -------------------------------------------- //
	// COMMAND HANDLING
	// -------------------------------------------- //

	public boolean handleCommand(CommandSender sender, String commandString, boolean testOnly) {
		boolean noSlash = false;
		if (commandString.startsWith("/")) {
			noSlash = true;
			commandString = commandString.substring(1);
		}

		for (MCommand<?> command : this.getBaseCommands()) {
			if (noSlash && !command.allowNoSlashAccess) {
				continue;
			}

			for (String alias : command.aliases) {
				if (commandString.startsWith(alias + " ") || commandString.equals(alias)) {
					List<String> args = new ArrayList<String>(Arrays.asList(commandString.split("\\s+")));
					args.remove(0);
					if (testOnly) {
						return true;
					}
					command.execute(sender, args);
					return true;
				}
			}
		}
		return false;
	}

	public boolean handleCommand(CommandSender sender, String commandString) {
		return this.handleCommand(sender, commandString, false);
	}

	// -------------------------------------------- //
	// LOGGING
	// -------------------------------------------- //
	public void log(Object msg) {
		log(Level.INFO, msg);
	}

	public void log(Level level, Object msg) {
		Logger.getLogger("Minecraft").log(level, "[" + this.getDescription().getFullName() + "] " + msg);
	}

}
