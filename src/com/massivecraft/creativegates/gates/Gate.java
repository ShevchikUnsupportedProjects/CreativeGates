package com.massivecraft.creativegates.gates;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import com.massivecraft.creativegates.Conf;
import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.Lang;
import com.massivecraft.creativegates.util.BlockUtil;
import com.massivecraft.creativegates.zcore.util.TextUtil;

public class Gate implements Comparable<Gate> {

	private static transient final Set<BlockFace> expandFacesWE = new HashSet<>();
	private static transient final Set<BlockFace> expandFacesNS = new HashSet<>();
	static {
		expandFacesWE.add(BlockFace.UP);
		expandFacesWE.add(BlockFace.DOWN);
		expandFacesWE.add(BlockFace.WEST);
		expandFacesWE.add(BlockFace.EAST);

		expandFacesNS.add(BlockFace.UP);
		expandFacesNS.add(BlockFace.DOWN);
		expandFacesNS.add(BlockFace.NORTH);
		expandFacesNS.add(BlockFace.SOUTH);
	}

	private final WorldCoord sourceCoord;

	public Gate() {
		this(null);
	}

	public Gate(WorldCoord sourceCoord) {
		this.sourceCoord = sourceCoord;
	}

	private transient Set<WorldCoord> contentCoords = new HashSet<>();
	private transient Set<WorldCoord> frameCoords = new HashSet<>();
	private transient Set<BlockData> frameBlocksData = new HashSet<>();
	private transient boolean frameDirIsNS; // True means NS direction. false means WE direction.
	private transient boolean open;

	public boolean isOpen() {
		return open;
	}

	public boolean isFrameCoord(WorldCoord coord) {
		return this.frameCoords.contains(coord);
	}

	public boolean isContentCoord(WorldCoord coord) {
		return contentCoords.contains(coord);
	}

	public boolean isAtWorld(World world) {
		return world.getName().equals(sourceCoord.worldName);
	}

	public void open() throws GateOpenException {
		if (this.isOpen()) {
			return;
		}

		Block sourceBlock = sourceCoord.getBlock();

		if ((sourceBlock == null) || (sourceBlock.getType() != Conf.getInstance().blockMaterial)) {
			throw new GateOpenException(CreativeGates.getInstance().txt.parse(Lang.openFailWrongSourceMaterial, TextUtil.getMaterialName(Conf.getInstance().blockMaterial)));
		}

		if (!this.dataPopulate()) {
			throw new GateOpenException(CreativeGates.getInstance().txt.parse(Lang.openFailNoFrame));
		}

		open = true;

		// Finally we set the content blocks material to water
		this.fill();
	}

	public void close() {
		empty();
		contentCoords.clear();
		frameCoords.clear();
		frameBlocksData.clear();
		open = false;
	}

	public void remove() {
		close();
		Gates.INSTANCE.removeGate(this);
	}

	/**
	 * This method populates the "data" (coords and material ids). It will return false if there was no possible frame.
	 */
	private boolean dataPopulate() {
		Block sourceBlock = sourceCoord.getBlock();

		// Search for content WE and NS
		Block floodStartBlock = sourceBlock.getRelative(BlockFace.UP);
		Set<Block> contentBlocksWE = getFloodBlocks(floodStartBlock, new HashSet<Block>(), expandFacesWE);
		Set<Block> contentBlocksNS = getFloodBlocks(floodStartBlock, new HashSet<Block>(), expandFacesNS);

		// Figure out dir and content... or throw no frame fail.
		Set<Block> contentBlocks;

		if ((contentBlocksWE == null) && (contentBlocksNS == null)) {
			return false;
		}

		if (contentBlocksNS == null) {
			contentBlocks = contentBlocksWE;
			frameDirIsNS = false;
		} else if (contentBlocksWE == null) {
			contentBlocks = contentBlocksNS;
			frameDirIsNS = true;
		} else if (contentBlocksWE.size() > contentBlocksNS.size()) {
			contentBlocks = contentBlocksNS;
			frameDirIsNS = true;
		} else {
			contentBlocks = contentBlocksWE;
			frameDirIsNS = false;
		}

		// Find the frame blocks
		Set<Block> frameBlocks = new HashSet<>();
		Set<BlockFace> expandFaces = frameDirIsNS ? expandFacesNS : expandFacesWE;
		for (Block currentBlock : contentBlocks) {
			for (BlockFace face : expandFaces) {
				Block potentialBlock = currentBlock.getRelative(face);
				if (!contentBlocks.contains(potentialBlock)) {
					frameBlocks.add(potentialBlock);
				}
			}
		}

		// Now we add the frame and content blocks as world coords to the lookup maps.
		for (Block frameBlock : frameBlocks) {
			this.frameCoords.add(new WorldCoord(frameBlock));
			if (frameBlock != sourceBlock) {
				frameBlocksData.add(frameBlock.getBlockData());
			}
		}
		for (Block contentBlock : contentBlocks) {
			this.contentCoords.add(new WorldCoord(contentBlock));
		}

		return true;
	}

	// ----------------------------------------------//
	// Find Target Gate And Location
	// ----------------------------------------------//

	/*
	 * This method finds the place where this gates goes to. We pick the next gate in the network chain that has a non blocked exit.
	 */
	public Gate getMyTargetGate() {
		List<Gate> networkGatePath = this.getNetworkGatePath();

		if (networkGatePath.size() == 1) {
			return null;
		}

		int myIndex = networkGatePath.indexOf(this);

		if (myIndex < (networkGatePath.size() - 1)) {
			return networkGatePath.get(myIndex + 1);
		} else {
			return networkGatePath.get(0);
		}
	}

	/*
	 * Find all the gates on the network of this gate (including this gate itself). The gates on the same network are those with the same frame materials.
	 */
	public List<Gate> getNetworkGatePath() {
		ArrayList<Gate> networkGatePath = new ArrayList<>();

		for (Gate gate : Gates.INSTANCE.get()) {
			if (gate.isOpen() && this.frameBlocksData.equals(gate.frameBlocksData)) {
				networkGatePath.add(gate);
			}
		}

		return networkGatePath;
	}

	/*
	 * If someone arrives to this gate, where should we place them? This method returns a Location telling us just that. It might also return null if the gate exit is blocked.
	 */
	public Location getMyOwnExitLocation() {
		Block overSourceBlock = sourceCoord.getBlock().getRelative(BlockFace.UP);
		Location firstChoice;
		Location secondChoice;

		if (frameDirIsNS) {
			firstChoice = overSourceBlock.getRelative(BlockFace.EAST).getLocation();
			firstChoice.setYaw(270);

			secondChoice = overSourceBlock.getRelative(BlockFace.WEST).getLocation();
			secondChoice.setYaw(90);
		} else {
			firstChoice = overSourceBlock.getRelative(BlockFace.NORTH).getLocation();
			firstChoice.setYaw(180);

			secondChoice = overSourceBlock.getRelative(BlockFace.SOUTH).getLocation();
			secondChoice.setYaw(0);
		}

		// We want to stand in the middle of the block. Not in the corner.
		firstChoice.add(0.5, 0, 0.5);
		secondChoice.add(0.5, 0, 0.5);

		firstChoice.setPitch(0);
		secondChoice.setPitch(0);

		if (BlockUtil.canPlayerStandInBlock(firstChoice.getBlock())) {
			return firstChoice;
		} else if (BlockUtil.canPlayerStandInBlock(secondChoice.getBlock())) {
			return secondChoice;
		}

		return null;
	}

	// ----------------------------------------------//
	// Gate information
	// ----------------------------------------------//

	public String getInfoMsgMaterial() {
		ArrayList<String> names = new ArrayList<>();

		for (BlockData frameBlockData : frameBlocksData) {
			names.add(CreativeGates.getInstance().txt.parse("<h>") + frameBlockData.getAsString());
		}

		String materials = TextUtil.implode(names, CreativeGates.getInstance().txt.parse("<i>, "));

		return CreativeGates.getInstance().txt.parse(Lang.infoMaterials, materials);
	}

	public String getInfoMsgNetwork() {
		return CreativeGates.getInstance().txt.parse(Lang.infoGateCount, this.getNetworkGatePath().size());
	}

	public void informPlayer(Player player) {
		player.sendMessage("");
		player.sendMessage(this.getInfoMsgMaterial());
		player.sendMessage(this.getInfoMsgNetwork());
	}

	// ----------------------------------------------//
	// Content management
	// ----------------------------------------------//

	public void fill() {
		for (WorldCoord coord : this.contentCoords) {
			Block block = coord.getBlock();
			if (block != null) {
				block.setType(Material.WATER);
			}
		}
	}

	public void empty() {
		for (WorldCoord coord : this.contentCoords) {
			Block block = coord.getBlock();
			if (block != null) {
				block.setType(Material.AIR);
			}
		}
	}

	// ----------------------------------------------//
	// Flood
	// ----------------------------------------------//

	public static Set<Block> getFloodBlocks(Block startBlock, Set<Block> foundBlocks, Set<BlockFace> expandFaces) {
		if (foundBlocks == null) {
			return null;
		}

		if (foundBlocks.size() > Conf.getInstance().maxarea) {
			return null;
		}

		if (foundBlocks.contains(startBlock)) {
			return foundBlocks;
		}

		if ((startBlock.getType() == Material.AIR) || (startBlock.getType() == Material.WATER)) {
			// ... We found a block :D ...
			foundBlocks.add(startBlock);

			// ... And flood away !
			for (BlockFace face : expandFaces) {
				Block potentialBlock = startBlock.getRelative(face);
				foundBlocks = getFloodBlocks(potentialBlock, foundBlocks, expandFaces);
			}
		}

		return foundBlocks;
	}

	@Override
	public int compareTo(Gate o) {
		return this.sourceCoord.toString().compareTo(o.sourceCoord.toString());
	}

	@Override
	public int hashCode() {
		return sourceCoord.hashCode();
	}

	@Override
	public boolean equals(Object otherObj) {
		if (!(otherObj instanceof Gate)) {
			return false;
		}
		Gate other = (Gate) otherObj;
		return sourceCoord.equals(other.sourceCoord);
	}

}
