package com.massivecraft.creativegates.gates;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.massivecraft.creativegates.CreativeGates;

public class Gates {

	public static final Gates INSTANCE = new Gates();

	private Gates() {
	}

	private final HashMap<String, Gate> gates = new HashMap<String, Gate>();

	private int nextId;

	private File getFile() {
		return new File(CreativeGates.getInstance().getDataFolder(), "gate.json");
	}

	@SuppressWarnings("serial")
	private final Type typeToken = new TypeToken<Map<String, Gate>>() {
	}.getType();

	@SuppressWarnings("unchecked")
	public void load() {
		try {
			gates.clear();
			if (getFile().exists()) {
				gates.putAll((Map<? extends String, ? extends Gate>) CreativeGates.getInstance().gson.fromJson(new InputStreamReader(new FileInputStream(getFile())), typeToken));
				for (Entry<String, Gate> entry : gates.entrySet()) {
					entry.getValue().setup(entry.getKey());
					nextId = Math.max(nextId, Integer.parseInt(entry.getKey()));
				}
			}
		} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
		}
	}

	public void save() {
		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(getFile()))) {
			CreativeGates.getInstance().gson.toJson(gates, typeToken, writer);
		} catch (IOException e) {
		}
	}

	public Collection<Gate> get() {
		return new ArrayList<Gate>(gates.values());
	}

	// -------------------------------------------- //
	// Find gate from block or coord.
	// -------------------------------------------- //

	public Gate findFromContent(WorldCoord coord) {
		for (Gate gate : this.get()) {
			if (gate.contentCoords.contains(coord)) {
				return gate;
			}
		}
		return null;
	}

	public Gate findFromContent(Block block) {
		return findFromContent(new WorldCoord(block));
	}

	public Gate findFromFrame(WorldCoord coord) {
		for (Gate gate : this.get()) {
			if (gate.frameCoords.contains(coord)) {
				return gate;
			}
		}
		return null;
	}

	public Gate findFromFrame(Block block) {
		return findFromFrame(new WorldCoord(block));
	}

	public Gate findFrom(WorldCoord coord) {
		Gate gate = findFromContent(coord);

		if (gate != null) {
			return gate;
		}

		return findFromFrame(coord);
	}

	public Gate findFrom(Block block) {
		return findFrom(new WorldCoord(block));
	}

	// -------------------------------------------- //
	// Mass Content Management
	// -------------------------------------------- //

	public void emptyAll() {
		for (Gate gate : this.get()) {
			gate.empty();
		}
	}

	public void openAll() {
		for (Gate gate : this.get()) {
			try {
				gate.open();
			} catch (GateOpenException e) {
			}
		}
	}

	// -------------------------------------------- //
	// Gate Factory
	// -------------------------------------------- //

	public Gate open(WorldCoord sourceCoord, Player player) {
		Gate gate = new Gate();
		gate.setup(String.valueOf(++nextId));
		gate.sourceCoord = sourceCoord;

		try {
			gate.open();
			addGate(gate);
			if (player != null) {
				gate.informPlayer(player);
			}
			return gate;
		} catch (GateOpenException e) {
			if (player == null) {
				CreativeGates.getInstance().log(e.getMessage());
			} else {
				player.sendMessage(e.getMessage());
			}
			return null;
		}
	}

	public Gate open(WorldCoord sourceCoord) {
		return this.open(sourceCoord, null);
	}

	public void addGate(Gate gate) {
		gates.put(gate.id, gate);
	}

	public void removeGate(Gate gate) {
		gates.remove(gate.id);
	}

}
