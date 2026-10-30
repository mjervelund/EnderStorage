package codechicken.enderstorage.item;

import codechicken.enderstorage.api.Frequency;
import codechicken.enderstorage.tile.TileFrequencyOwner;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ItemEnderStorage extends BlockItem {

    public ItemEnderStorage(Block block, Item.Properties properties) {
        super(block, properties);
    }

    public Frequency getFreq(ItemStack stack) {
        return Frequency.getComponentOrEmpty(stack);
    }

    @Override
    public void onBlockPlacedBy(BlockPlaceContext context, Level world, BlockPos pos, BlockState state, @Nullable Player player, ItemStack stack) {
        super.onBlockPlacedBy(context, world, pos, state, player, stack);
        if (world.getBlockEntity(pos) instanceof TileFrequencyOwner tile) {
            tile.setFreq(getFreq(stack));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        Frequency frequency = Frequency.getComponentOrEmpty(stack);
        frequency.ownerName().ifPresent(tooltipAdder);
        tooltipAdder.accept(frequency.getTooltip());
    }

    //    private EnderLiquidStorage getLiquidStorage(ItemStack stack) {
    //        return (EnderLiquidStorage) EnderStorageManager.instance(FMLCommonHandler.instance().getSide().isClient()).getStorage(getFreq(stack), "liquid");
    //    }
    //
    //    @Override
    //    public ICapabilityProvider initCapabilities(final ItemStack stack, NBTTagCompound nbt) {
    //        if (getMetadata(stack) == 1) {
    //            return new ICapabilityProvider() {
    //                @Override
    //                public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
    //
    //                    return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
    //                }
    //
    //                @Override
    //                public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
    //
    //                    return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY ? getLiquidStorage(stack) : null);
    //                }
    //            };
    //        }
    //        return null;
    //    }
}
