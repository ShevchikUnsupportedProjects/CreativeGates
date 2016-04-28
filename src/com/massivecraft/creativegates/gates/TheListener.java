package com.massivecraft.creativegates.gates;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.massivecraft.creativegates.Conf;
import com.massivecraft.creativegates.CreativeGates;
import com.massivecraft.creativegates.Lang;
import com.massivecraft.creativegates.Permission;
import com.massivecraft.creativegates.event.CreativeGatesTeleportEvent;

public class TheListener implements Listener {

	// -------------------------------------------- //
	// META
	// -------------------------------------------- //

	public TheListener() {
		Bukkit.getServer().getPluginManager().registerEvents(this, CreativeGates.getInstance());
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockFromTo(BlockFromToEvent event) {
		Block blockFrom = event.getBlock();

		if (blockFrom.getType() != Material.STATIONARY_WATER) {
			return;
		}

		if (Gates.INSTANCE.findFrom(blockFrom) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockBreakNormal(BlockBreakEvent event) {
		Gate gate = Gates.INSTANCE.findFromFrame(event.getBlock());
		if (gate == null) {
			return;
		}

		// A player is attempting to destroy a gate. Can he?
		if (!Permission.DESTROY.has(event.getPlayer(), true)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBreakMonitor(BlockBreakEvent event) {
		Gate gate = Gates.INSTANCE.findFromFrame(event.getBlock());
		if (gate != null) {
			gate.close();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerGateTeleport(CreativeGatesTeleportEvent event) {
		Player player = event.getPlayerMoveEvent().getPlayer();

		// For now we do not handle vehicles
		if (player.isInsideVehicle()) {
			player.leaveVehicle();
		}

		player.teleport(event.getLocation());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (event.getFrom().getBlock().equals(event.getTo().getBlock())) {
			return;
		}

		// We look one up due to half blocks.
		Block blockToTest = event.getTo().getBlock().getRelative(BlockFace.UP);

		// Fast material check
		if (blockToTest.getType() != Material.STATIONARY_WATER) {
			return;
		}

		// Find the gate if there is one
		Gate gateFrom = Gates.INSTANCE.findFromContent(blockToTest);
		if (gateFrom == null) {
			return;
		}

		// Can the player use gates?
		if (!Permission.USE.has(event.getPlayer(), true)) {
			return;
		}

		// Find the target location
		Gate gateTo = gateFrom.getMyTargetGate();
		Location targetLocation = gateTo == null ? null : gateTo.getMyOwnExitLocation();
		if (targetLocation == null) {
			event.getPlayer().sendMessage(CreativeGates.getInstance().txt.parse(Lang.useFailNoTargetLocation));
			return;
		}

		CreativeGatesTeleportEvent gateevent = new CreativeGatesTeleportEvent(event, targetLocation, gateFrom, gateTo);
		Bukkit.getPluginManager().callEvent(gateevent);
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		// We are only interested in clicks on a block with a wand
		if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		if (event.getItem().getTypeId() != Conf.getInstance().wand) {
			return;
		}

		Block clickedBlock = event.getClickedBlock();
		Player player = event.getPlayer();

		// Did we hit an existing gate?
		// In such case send information.
		Gate gate = Gates.INSTANCE.findFrom(clickedBlock);
		if (gate != null) {
			gate.informPlayer(player);
			return;
		}

		// Did we hit a diamond block?
		if (clickedBlock.getTypeId() == Conf.getInstance().block) {
			// create a gate if the player has the permission
			if (Permission.CREATE.has(player, true)) {
				Gates.INSTANCE.open(new WorldCoord(clickedBlock), player);
			}
		}
	}

}
