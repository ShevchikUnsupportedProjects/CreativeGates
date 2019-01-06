package com.massivecraft.creativegates;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.bukkit.Material;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class Conf {

	public transient Material wandMaterial;
	public transient Material blockMaterial;

	private final String wand = Material.CLOCK.toString();
	private final String block = Material.WET_SPONGE.toString();
	public int maxarea = 200;
	public boolean effects = true;

	private static transient Conf instance = new Conf();

	public static Conf getInstance() {
		return instance;
	}

	private static File getFile() {
		return new File(CreativeGates.getInstance().getDataFolder(), "conf.json");
	}

	public static void load() {
		try {
			if (getFile().exists()) {
				instance = CreativeGates.getInstance().gson.fromJson(new InputStreamReader(new FileInputStream(getFile())), Conf.class);
				instance.wandMaterial = Material.getMaterial(instance.wand);
				instance.blockMaterial = Material.getMaterial(instance.block);
			} else {
				save();
			}
		} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
		}
	}

	public static void save() {
		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(getFile()))) {
			CreativeGates.getInstance().gson.toJson(instance, writer);
		} catch (IOException e) {
		}
	}

}
