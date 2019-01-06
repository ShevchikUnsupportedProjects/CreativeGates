package com.massivecraft.creativegates;

import org.bukkit.command.CommandSender;

public enum Permission {
	USE("use"), CREATE("create"), DESTROY("destroy"),;

	public final String node;

	Permission(final String node) {
		this.node = "creativegates." + node;
	}

	public boolean has(CommandSender sender, boolean informSenderIfNot) {
		return CreativeGates.getInstance().perm.has(sender, this.node, informSenderIfNot);
	}

	public boolean has(CommandSender sender) {
		return CreativeGates.getInstance().perm.has(sender, this.node, false);
	}

}
