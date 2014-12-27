package com.massivecraft.creativegates;

import com.massivecraft.creativegates.zcore.*;

public class CreativeGates extends MPlugin {
	// Our single plugin instance
	public static CreativeGates instance;

	// Listeners
	public TheListener theListener;

	public CreativeGates() {
		instance = this;
	}

	public void onEnable() {
		if (!preEnable())
			return;

		// TODO fix config auto update routine... ?
		Conf.load();

		Gates.i.loadFromDisc();
		Gates.i.openAllOrDetach();

		// Register events
		this.theListener = new TheListener(this);

		postEnable();
	}

	public void onDisable() {
		Gates.i.emptyAll();
		super.onDisable();
	}
}
