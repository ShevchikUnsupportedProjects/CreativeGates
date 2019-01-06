package com.massivecraft.creativegates.util;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class BlockUtil {

	public static boolean canPlayerStandInBlock(Block block) {
		return block.isPassable() && block.getRelative(BlockFace.UP).isPassable();
	}

}
