package openblocks.integration;

import java.lang.ref.WeakReference;

import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import openblocks.Config;
import openblocks.common.entity.EntityMagnet;
import openblocks.common.entity.EntityMagnet.IOwner;
import openperipheral.api.*;

import com.google.common.base.Preconditions;

import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;

@PeripheralTypeId("openblocks_magnet")
public class MagnetControlAdapter implements ITickingTurtle, IWorldProvider {

	public class Owner implements IOwner {

		private final Vec3 target;

		public Owner() {
			this.target = Vec3.createVectorHelper(0, 0, 0);
		}

		public synchronized void setTarget(double x, double y, double z) {
			target.xCoord = x;
			target.yCoord = y;
			target.zCoord = z;
		}

		public synchronized Vec3 getTarget(Vec3 pos, ForgeDirection side) {
			pos.yCoord += target.yCoord;
			switch (side) {
				case NORTH:
					pos.xCoord += target.zCoord;
					pos.zCoord -= target.xCoord;
					break;
				case SOUTH:
					pos.xCoord -= target.zCoord;
					pos.zCoord += target.xCoord;
					break;
				case WEST:
					pos.xCoord -= target.xCoord;
					pos.zCoord -= target.zCoord;
					break;
				case EAST:
					pos.xCoord += target.xCoord;
					pos.zCoord += target.zCoord;
					break;
				default:
					break;
			}

			return pos.addVector(0.5, 0.5, 0.5);
		}

		@Override
		public boolean isValid(EntityMagnet magnet) {
			return turtle != null && turtle.getWorld() != null;
		}

		@Override
		public Vec3 getTarget() {
			return getTarget(getTurtlePosition(), getTurtleFacing());
		}
	}

	private final TurtleSide side;

	private final ITurtleAccess turtle;

	private WeakReference<EntityMagnet> magnet = new WeakReference<EntityMagnet>(null);
	private Owner magnetOwner;

	public MagnetControlAdapter(ITurtleAccess turtle, TurtleSide side) {
		this.turtle = turtle;
		this.side = side;
	}

	public enum SpawnSide {
		Left,
		Right;
	}

	private int fuelTick = 0;

	@Override
	public World getWorld() {
		return turtle.getWorld();
	}

	protected boolean consumeFuel(int amount) {
		return turtle.consumeFuel(amount);
	}

	protected SpawnSide getSpawnSide() {
		switch (side) {
			case Left:
				return SpawnSide.Left;
			case Right:
			default:
				return SpawnSide.Right;
		}
	}

	protected ForgeDirection getTurtleFacing() {
		return ForgeDirection.getOrientation(turtle.getDirection());
	}

	protected Vec3 getTurtlePosition() {
		ChunkCoordinates coord = turtle.getPosition();
		return Vec3.createVectorHelper(coord.posX, coord.posY, coord.posZ);
	}

	@LuaCallable(description = "Activate magnet")
	public void activate() {
		EntityMagnet magnet = this.magnet.get();
		Preconditions.checkState(magnet == null || magnet.isDead, "Magnet already active");
		World world = getWorld();
		Preconditions.checkNotNull(world, "Trying to spawn magnet, but turtle is unloaded");
		Preconditions.checkState(canSpawn(world), "Can't deploy magnet");
		Preconditions.checkState(consumeFuel(5), "No fuel");

		magnetOwner = new Owner();
		magnetOwner.target.zCoord = getSpawnSide() == SpawnSide.Left? -1 : 1;
		magnet = new EntityMagnet(world, magnetOwner, true);
		world.spawnEntityInWorld(magnet);

		magnet.playSound("mob.endermen.portal", 1, 1);
		this.magnet = new WeakReference<EntityMagnet>(magnet);
	}

	@LuaCallable(description = "Deactive magnet")
	public void deactivate() {
		despawnMagnet(true);
	}

	@LuaCallable(description = "Set target for magnet")
	public void setTarget(@Arg(name = "x") double x,
			@Arg(name = "y") double y,
			@Arg(name = "z") double z) {
		Preconditions.checkNotNull(magnetOwner, "Magnet not active");
		Preconditions.checkArgument(checkTargetRange(x, y, z), "Target out of range");
		magnetOwner.setTarget(x, y, z);
	}

	@LuaCallable(returnTypes = { LuaReturnType.NUMBER, LuaReturnType.NUMBER, LuaReturnType.NUMBER },
			description = "Get turtle position")
	public IMultiReturn getPosition() {
		EntityMagnet magnet = getMagnet();
		Vec3 rotated = getRelativeDistance(magnet);
		return MultiReturn.wrap(rotated.xCoord, rotated.yCoord, rotated.zCoord);
	}

	@LuaCallable(returnTypes = LuaReturnType.BOOLEAN, description = "Is magnet above grabbable entity")
	public boolean isAboveEntity() {
		return getMagnet().isAboveTarget();
	}

	@Alias("toggle")
	@LuaCallable(returnTypes = LuaReturnType.BOOLEAN, description = "Grab or release entity/block under magnet")
	public boolean toggleMagnet() {
		return getMagnet().toggleMagnet();
	}

	@LuaCallable(returnTypes = LuaReturnType.BOOLEAN, description = "Is magnet currently grabbing block or entity")
	public boolean isGrabbing() {
		return getMagnet().isLocked();
	}

	@Alias("distance")
	@LuaCallable(returnTypes = { LuaReturnType.NUMBER, LuaReturnType.NUMBER, LuaReturnType.NUMBER })
	public IMultiReturn getDistanceToTarget() {
		EntityMagnet magnet = getMagnet();
		Vec3 current = getRelativeDistance(magnet);
		Vec3 target = magnetOwner.target;
		return MultiReturn.wrap(current.xCoord - target.xCoord,
				current.yCoord - target.yCoord,
				current.zCoord - target.zCoord);
	}

	@Override
	public void onPeripheralTick() {
		EntityMagnet magnet = this.magnet.get();
		if (magnet != null && !magnet.isDead) {
			if (++fuelTick >= 20) {
				fuelTick = 0;
				int fuel = magnet.isLocked()? 2 : 1;
				if (!consumeFuel(fuel)) despawnMagnet(false);

			}
		}
	}

	private static boolean checkTargetRange(double x, double y, double z) {
		return Math.abs(x) <= Config.turtleMagnetRange
				&& Math.abs(y) <= Config.turtleMagnetRange
				&& Math.abs(z) <= Config.turtleMagnetRange;
	}

	private Vec3 getRelativeDistance(EntityMagnet magnet) {
		Vec3 magnetPos = Vec3.createVectorHelper(magnet.posX, magnet.posY, magnet.posZ);
		Vec3 turtlePos = getTurtlePosition().addVector(0.5, 0.5, 0.5);

		Vec3 dist = turtlePos.subtract(magnetPos);

		ForgeDirection side = getTurtleFacing();

		switch (side) {
			case NORTH:
				return Vec3.createVectorHelper(-dist.zCoord, dist.yCoord, dist.xCoord);
			case SOUTH:
				return Vec3.createVectorHelper(dist.zCoord, dist.yCoord, -dist.xCoord);
			case EAST:
				return Vec3.createVectorHelper(dist.xCoord, dist.yCoord, dist.zCoord);
			case WEST:
				return Vec3.createVectorHelper(-dist.xCoord, dist.yCoord, -dist.zCoord);
			default:
				return dist;
		}
	}

	private boolean canSpawn(World world) {
		ForgeDirection facing = getTurtleFacing();
		Vec3 position = getTurtlePosition();
		SpawnSide side = getSpawnSide();

		ForgeDirection spawnSide = facing.getRotation((side == SpawnSide.Left)? ForgeDirection.DOWN : ForgeDirection.UP);
		int x = MathHelper.floor_double(position.xCoord) + spawnSide.offsetX;
		int y = MathHelper.floor_double(position.yCoord) + spawnSide.offsetY;
		int z = MathHelper.floor_double(position.zCoord) + spawnSide.offsetZ;

		return world.isAirBlock(x, y, z);
	}

	private EntityMagnet getMagnet() {
		EntityMagnet magnet = this.magnet.get();
		Preconditions.checkState(magnet != null && !magnet.isDead, "Magnet not active");
		return magnet;
	}

	private void despawnMagnet(boolean checkPosition) {
		EntityMagnet magnet = this.magnet.get();
		Preconditions.checkNotNull(magnet, "Magnet not active");

		Vec3 magnetPos = Vec3.createVectorHelper(magnet.posX, magnet.posY, magnet.posZ);
		Vec3 turtlePos = getTurtlePosition().addVector(0.5, 0.5, 0.5);

		Preconditions.checkState(!checkPosition || canOperateOnMagnet(magnetPos, turtlePos), "Magnet too far");

		magnet.playSound("mob.endermen.portal", 1, 1);
		magnet.setDead();
		this.magnet.clear();
		magnetOwner = null;
	}

	private static boolean canOperateOnMagnet(Vec3 magnetPos, Vec3 turtlePos) {
		return magnetPos.squareDistanceTo(turtlePos) <= Config.turtleMagnetRangeDeactivate * Config.turtleMagnetRangeDeactivate;
	}

	@Override
	public boolean isValid() {
		EntityMagnet magnet = this.magnet.get();
		return magnet != null && magnet.addedToChunk;
	}
}
