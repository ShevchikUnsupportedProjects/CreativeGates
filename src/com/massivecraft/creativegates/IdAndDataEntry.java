package com.massivecraft.creativegates;

public class IdAndDataEntry implements Comparable<IdAndDataEntry> {

	private int id;
	private int data;

	public IdAndDataEntry(int id, int data) {
		this.id = id;
		this.data = data;
	}

	public int getId() {
		return id;
	}

	public int getData() {
		return data;
	}

	@Override
	public int hashCode() {
		return id << 4 | data;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof IdAndDataEntry)) {
			return false;
		}
		IdAndDataEntry entry = (IdAndDataEntry) other;
		return entry.id == id && entry.data == data;
	}

	@Override
	public int compareTo(IdAndDataEntry o) {
		if (id > o.id) {
			return 1;
		} else if (id < o.id) {
			return -1;
		} else {
			if (data > o.data) {
				return 1;
			} else if (data < o.data) {
				return -1;
			} else {
				return 0;
			}
		}
	}

}
