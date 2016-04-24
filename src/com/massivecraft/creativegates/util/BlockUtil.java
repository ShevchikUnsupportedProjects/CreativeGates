package com.massivecraft.creativegates.util;

import java.util.*;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class BlockUtil {

	private final static HashSet<Material> standable = new HashSet<Material>();
	static {
		standable.add(Material.AIR);
		standable.add(Material.BROWN_MUSHROOM);
		standable.add(Material.CROPS);
		standable.add(Material.DEAD_BUSH);
		standable.add(Material.DETECTOR_RAIL);
		standable.add(Material.DIODE_BLOCK_OFF);
		standable.add(Material.DIODE_BLOCK_ON);
		standable.add(Material.LADDER);
		standable.add(Material.LEVER);
		standable.add(Material.LONG_GRASS);
		standable.add(Material.RAILS);
		standable.add(Material.RED_MUSHROOM);
		standable.add(Material.RED_ROSE);
		standable.add(Material.REDSTONE_TORCH_OFF);
		standable.add(Material.REDSTONE_TORCH_ON);
		standable.add(Material.REDSTONE_WIRE);
		standable.add(Material.SAPLING);
		standable.add(Material.STATIONARY_WATER);
		standable.add(Material.STONE_BUTTON);
		standable.add(Material.SUGAR_CANE_BLOCK);
		standable.add(Material.TORCH);
		standable.add(Material.WALL_SIGN);
		standable.add(Material.WATER);
		standable.add(Material.YELLOW_FLOWER);
		standable.add(Material.SNOW);
	}

	public static boolean isMaterialStandable(Material material) {
		return standable.contains(material);
	}

	public static boolean canPlayerStandInBlock(Block block) {
		return isMaterialStandable(block.getType()) && isMaterialStandable(block.getRelative(BlockFace.UP).getType());
	}

}
