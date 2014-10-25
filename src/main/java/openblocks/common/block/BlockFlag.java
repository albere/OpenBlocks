package openblocks.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import openblocks.common.tileentity.TileEntityFlag;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockFlag extends OpenBlock {

	public static final int[] COLORS;

	private static void setColor(int index, int red, int green, int blue) {
		COLORS[index] = red % 256 << 16 | green % 256 << 8 | blue % 256;
	}

	static {
		COLORS = new int[16];
		setColor(0, 20, 198, 0);
		setColor(1, 41, 50, 156);
		setColor(2, 221, 0, 0);
		setColor(3, 255, 174, 201);
		setColor(4, 185, 122, 87);
		setColor(5, 181, 230, 29);
		setColor(6, 0, 162, 232);
		setColor(7, 128, 0, 64);
		setColor(8, 255, 242, 0);
		setColor(9, 255, 127, 39);
		setColor(10, 255, 45, 45);
		setColor(11, 255, 23, 151);
		setColor(12, 195, 195, 195);
		setColor(13, 163, 73, 164);
		setColor(14, 0, 0, 0);
		setColor(15, 255, 255, 255);
	}

	public BlockFlag() {
		super(Material.circuits);
		setupDimensionsFromCenter(0.5f, 0f, 0.5f, 1 / 16f, 1f, 1 / 16f);
		setRotationMode(BlockRotationMode.SIX_DIRECTIONS);
		setPlacementMode(BlockPlacementMode.SURFACE);
		setInventoryRenderRotation(ForgeDirection.DOWN);
		setRenderMode(RenderMode.TESR_ONLY);
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean isBlockSolid(IBlockAccess world, int x, int y, int z, int side) {
		return false;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World par1World, int par2, int par3, int par4) {
		return null;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		TileEntityFlag flag = getTileEntity(world, x, y, z, TileEntityFlag.class);
		if (flag != null) {
			ForgeDirection onSurface = flag.getSurfaceDirection();
			if (onSurface == ForgeDirection.DOWN) {
				setupDimensionsFromCenter(0.5f, 0f, 0.5f, 1 / 16f, 1f, 1 / 16f);
			} else if (onSurface == ForgeDirection.EAST || onSurface == ForgeDirection.WEST) {
				setupDimensionsFromCenter(0.5f, 0f, 0.5f, 5 / 16f, 1f, 1 / 16f);
			} else {
				setupDimensionsFromCenter(0.5f, 0f, 0.5f, 1 / 16f, 1f, 5 / 16f);
			}
		}
	}

	@Override
	public boolean canPlaceBlockOnSide(World world, int x, int y, int z, ForgeDirection side) {
		if (side == ForgeDirection.DOWN) {
			Block belowBlock = world.getBlock(x, y - 1, z);
			if (belowBlock != null) {
				if (belowBlock == Blocks.fence) {
					return true;
				} else if (belowBlock == this) {
					TileEntityFlag flag = getTileEntity(world, x, y - 1, z, TileEntityFlag.class);
					if (flag != null && flag.getSurfaceDirection().equals(ForgeDirection.DOWN)) { return true; }
				}
			}
		} else if (side == ForgeDirection.UP) { return false; }
		return isNeighborBlockSolid(world, x, y, z, side);
	}

	@Override
	public boolean canRotateWithTool() {
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister registry) {
		blockIcon = registry.registerIcon("planks_oak");
	}

}
