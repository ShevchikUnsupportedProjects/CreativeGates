package com.massivecraft.creativegates.gates;

import java.util.*;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.massivecraft.creativegates.Conf;
import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.IdAndDataEntry;
import com.massivecraft.creativegates.Lang;
import com.massivecraft.creativegates.util.BlockUtil;
import com.massivecraft.creativegates.zcore.util.*;

public class Gate implements Comparable<Gate> {

	public transient Set<WorldCoord> contentCoords;
	public transient Set<WorldCoord> frameCoords;
	public transient Set<IdAndDataEntry> frameMaterialIds;
	public transient String id;
	public WorldCoord sourceCoord;
	public transient boolean frameDirIsNS; // True means NS direction. false means WE direction.

	private static transient final Set<BlockFace> expandFacesWE = new HashSet<BlockFace>();
	private static transient final Set<BlockFace> expandFacesNS = new HashSet<BlockFace>();
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

	public Gate() {
	}

	public void setup(String id) {
		contentCoords = new HashSet<WorldCoord>();
		frameCoords = new HashSet<WorldCoord>();
		frameMaterialIds = new TreeSet<IdAndDataEntry>();
	}

	/**
	 * Is this gate open right now?
	 */
	public boolean isOpen() {
		return Gates.INSTANCE.findFrom(sourceCoord) != null;
	}

	@SuppressWarnings("deprecation")
	public void open() throws GateOpenException {
		Block sourceBlock = sourceCoord.getBlock();

		if (this.isOpen()) {
			return;
		}

		if (sourceBlock == null || sourceBlock.getTypeId() != Conf.getInstance().block) {
			throw new GateOpenException(CreativeGates.getInstance().txt.parse(Lang.openFailWrongSourceMaterial, TextUtil.getMaterialName(Conf.getInstance().block)));
		}

		if (!this.dataPopulate()) {
			throw new GateOpenException(CreativeGates.getInstance().txt.parse(Lang.openFailNoFrame));
		}

		// Finally we set the content blocks material to water
		this.fill();
	}

	public void close() {
		this.empty();
		Gates.INSTANCE.removeGate(this);
	}

	/**
	 * This method clears the "data" (coords and material ids).
	 */
	public void dataClear() {
		contentCoords.clear();
		frameCoords.clear();
		frameMaterialIds.clear();
	}

	/**
	 * This method populates the "data" (coords and material ids). It will return false if there was no possible frame.
	 */
	@SuppressWarnings("deprecation")
	public boolean dataPopulate() {
		this.dataClear();
		Block sourceBlock = sourceCoord.getBlock();

		// Search for content WE and NS
		Block floodStartBlock = sourceBlock.getRelative(BlockFace.UP);
		Set<Block> contentBlocksWE = getFloodBlocks(floodStartBlock, new HashSet<Block>(), expandFacesWE);
		Set<Block> contentBlocksNS = getFloodBlocks(floodStartBlock, new HashSet<Block>(), expandFacesNS);

		// Figure out dir and content... or throw no frame fail.
		Set<Block> contentBlocks;

		if (contentBlocksWE == null && contentBlocksNS == null) {
			// throw new Exception("There is no frame, or it is broken, or it is to large.");
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

		// Find the frame blocks and materials
		Set<Block> frameBlocks = new HashSet<Block>();
		Set<BlockFace> expandFaces = frameDirIsNS ? expandFacesNS : expandFacesWE;
		for (Block currentBlock : contentBlocks) {
			for (BlockFace face : expandFaces) {
				Block potentialBlock = currentBlock.getRelative(face);
				if (!contentBlocks.contains(potentialBlock)) {
					frameBlocks.add(potentialBlock);
					if (potentialBlock != sourceBlock) {
						frameMaterialIds.add(new IdAndDataEntry(potentialBlock.getTypeId(), potentialBlock.getData()));
					}
				}
			}
		}

		// Now we add the frame and content blocks as world coords to the lookup maps.
		for (Block frameBlock : frameBlocks) {
			this.frameCoords.add(new WorldCoord(frameBlock));
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
		ArrayList<Gate> networkGatePath = this.getNetworkGatePath();
		int myIndex = networkGatePath.indexOf(this);

		if (networkGatePath.size() == 1) {
			return null;
		}

		if (myIndex < networkGatePath.size() - 1) {
			return networkGatePath.get(myIndex + 1);
		} else {
			return networkGatePath.get(0);
		}
	}

	/*
	 * Find all the gates on the network of this gate (including this gate itself). The gates on the same network are those with the same frame materials.
	 */
	public ArrayList<Gate> getNetworkGatePath() {
		ArrayList<Gate> networkGatePath = new ArrayList<Gate>();

		// We put the gates in a tree set to sort them after gate location.
		TreeSet<Gate> gates = new TreeSet<Gate>();
		gates.addAll(Gates.INSTANCE.get());

		for (Gate gate : gates) {
			if (this.frameMaterialIds.equals(gate.frameMaterialIds)) {
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

	@SuppressWarnings("deprecation")
	public String getInfoMsgMaterial() {
		ArrayList<String> materialNames = new ArrayList<String>();
		for (IdAndDataEntry frameMaterialId : this.frameMaterialIds) {
			materialNames.add(CreativeGates.getInstance().txt.parse("<h>") + TextUtil.getMaterialName(Material.getMaterial(frameMaterialId.getId()))+":"+frameMaterialId.getData());
		}

		String materials = TextUtil.implode(materialNames, CreativeGates.getInstance().txt.parse("<i>, "));

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
			coord.getBlock().setType(Material.STATIONARY_WATER);
		}
	}

	public void empty() {
		for (WorldCoord coord : this.contentCoords) {
			coord.getBlock().setType(Material.AIR);
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

		if (startBlock.getType() == Material.AIR || startBlock.getType() == Material.WATER || startBlock.getType() == Material.STATIONARY_WATER) {
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

	// ----------------------------------------------//
	// Comparable
	// ----------------------------------------------//

	@Override
	public int compareTo(Gate o) {
		return this.sourceCoord.toString().compareTo(o.sourceCoord.toString());
	}

}
