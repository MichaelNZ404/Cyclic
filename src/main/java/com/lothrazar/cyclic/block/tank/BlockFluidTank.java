package com.lothrazar.cyclic.block.tank;

import com.lothrazar.cyclic.ModCyclic;
import com.lothrazar.cyclic.base.BlockBase;
import com.lothrazar.cyclic.capability.FluidHandlerCapabilityStack;
import com.lothrazar.cyclic.registry.TileRegistry;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.client.registry.ClientRegistry;

public class BlockFluidTank extends BlockBase {

  public static final BooleanProperty TANK_ABOVE = BooleanProperty.create("above");
  public static final BooleanProperty TANK_BELOW = BooleanProperty.create("below");
  public static final int MAXHEIGHT = 16;

  public BlockFluidTank(Properties properties) {
    super(properties.harvestTool(ToolType.PICKAXE).hardnessAndResistance(1.2F).notSolid());
    this.setHasFluidInteract();
  }

  @Override
  @Deprecated
  public float getAmbientOcclusionLightValue(BlockState state, IBlockReader worldIn, BlockPos pos) {
    return 1.0f;
  }

  @Override
  @OnlyIn(Dist.CLIENT)
  public boolean isSideInvisible(BlockState state, BlockState adjacentBlockState, Direction side) {
    return false;
  }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
    super.fillStateContainer(builder);
    builder.add(TANK_ABOVE, TANK_BELOW);
  }

  @Override
  public BlockState updatePostPlacement(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos) {
    boolean tileAbove = world.getTileEntity(pos.up()) instanceof TileTank;
    boolean tileBelow = world.getTileEntity(pos.down()) instanceof TileTank;
    return state
        .with(TANK_ABOVE, tileAbove)
        .with(TANK_BELOW, tileBelow);
  }

  @Override
  public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos) {
    return true;
  }

  @Override
  public boolean hasTileEntity(BlockState state) {
    return true;
  }

  @Override
  public TileEntity createTileEntity(BlockState state, IBlockReader world) {
    return new TileTank();
  }

  @Override
  public boolean shouldDisplayFluidOverlay(BlockState state, IBlockDisplayReader world, BlockPos pos, FluidState fluidState) {
    return true;
  }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void registerClient() {
    RenderTypeLookup.setRenderLayer(this, RenderType.getTranslucent());
    ClientRegistry.bindTileEntityRenderer(TileRegistry.tank, RenderTank::new);
  }

  @Override
  public List<ItemStack> getDrops(BlockState state, net.minecraft.loot.LootContext.Builder builder) {
    //because harvestBlock manually forces a drop 
    return new ArrayList<>();
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    try {
      IFluidHandlerItem storage = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null).orElse(null);
      TileEntity container = world.getTileEntity(pos);
      if (storage != null && container != null) {
        IFluidHandler storageTile = container.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null).orElse(null);
        if (storageTile != null) {
          storageTile.fill(storage.getFluidInTank(0), FluidAction.EXECUTE);
        }
      }
    }
    catch (Exception e) {
      //
      ModCyclic.LOGGER.error("Error during fill from item ", e);
    }
    //set default state
    state = state.with(TANK_ABOVE, false).with(TANK_BELOW, false);
    world.setBlockState(pos, state);
  }

  @Override
  public void harvestBlock(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable TileEntity ent, ItemStack stack) {
    super.harvestBlock(world, player, pos, state, ent, stack);
    ItemStack tankStack = new ItemStack(this);
    if (ent != null) {
      IFluidHandler fluidInTile = ent.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null).orElse(null);
      // note a DIFFERENT cap here for the item
      IFluidHandler fluidInStack = tankStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null).orElse(null);
      if (fluidInStack != null) {
        //now give 
        FluidStack fs = fluidInTile.getFluidInTank(0);
        ((FluidHandlerCapabilityStack) fluidInStack).setFluid(fs);
      }
    }
    if (world.isRemote == false) {
      world.addEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), tankStack));
    }
  }
}
