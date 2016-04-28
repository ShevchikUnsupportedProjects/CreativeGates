package com.massivecraft.creativegates;

import org.bukkit.Bukkit;

import com.massivecraft.creativegates.gates.Gates;
import com.massivecraft.creativegates.gates.TheListener;
import com.massivecraft.creativegates.zcore.*;

public class CreativeGates extends MPlugin {

	// Our single plugin instance
	private static CreativeGates instance;

	public static CreativeGates getInstance() {
		return instance;
	}

	public CreativeGates() {
		instance = this;
	}

	protected boolean loaded;

	@Override
	public void onEnable() {
		loaded = false;

		if (!preEnable())
			return;

		Conf.load();

		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run() {
				Gates.INSTANCE.load();
				Gates.INSTANCE.openAll();
				loaded = true;
			}
		}, 20);

		// Register events
		new TheListener();

		postEnable();
	}

	@Override
	public void onDisable() {
		Conf.save();
		if (loaded) {
			Gates.INSTANCE.emptyAll();
			Gates.INSTANCE.save();
		}
	}

}
