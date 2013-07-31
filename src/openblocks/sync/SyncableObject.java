package openblocks.sync;

import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.tileentity.TileEntity;

public abstract class SyncableObject implements ISyncableObject {

	protected Object value;
	protected boolean hasChanged = false;
	private int ticksSinceChanged = 0;
	public WeakHashMap<TileEntity, Void> tiles;

	public SyncableObject(Object value) {
		this.value = value;
		tiles = new WeakHashMap<TileEntity, Void>();
	}

	public void clear() {
		value = null;
	}

	public void setValue(Object newValue) {
		if (!equals(newValue)) {
			setHasChanged();
			this.value = newValue;
		}
	}

	public int ticksSinceChanged() {
		return ticksSinceChanged;
	}

	public void setHasChanged() {
		hasChanged = true;
		ticksSinceChanged = 0;
	}

	public boolean equals(Object otherValue) {
		return value == otherValue;
	}

	public Object getValue() {
		return value;
	}

	public boolean hasChanged() {
		return hasChanged;
	}

	public void resetChangeStatus() {
		hasChanged = false;
		ticksSinceChanged++;
	}

	@Override
	public void registerTile(TileEntity tile) {
		tiles.put(tile, null);
	}

	@Override
	public void unregisterTile(TileEntity tile) {
		tiles.remove(tile);
	}

}
