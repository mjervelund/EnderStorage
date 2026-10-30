package codechicken.enderstorage.block;

import codechicken.enderstorage.api.Frequency;
import codechicken.enderstorage.config.EnderStorageConfig;
import codechicken.enderstorage.tile.TileFrequencyOwner;
import codechicken.lib.block.ModularTileBlock;
import codechicken.lib.block.component.DirectionComponent;
import codechicken.lib.block.component.tile.ValueComponent;
import codechicken.lib.colour.EnumColour;
import codechicken.lib.raytracer.RayTracer;
import codechicken.lib.raytracer.SubHitBlockHitResult;
import codechicken.lib.util.ItemUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created by covers1624 on 4/11/2016.
 */
public abstract class BlockEnderStorage<T extends TileFrequencyOwner> extends ModularTileBlock<T> {

    public final DirectionComponent rotation = addComponent(new DirectionComponent(true))
            .withPlacement(DirectionComponent.PLAYER_HORIZONTAL_OPPOSITE);

    // TODO mayhaps?
//    public final ValueComponent<Frequency> frequency = addComponent("frequency",
//            new ValueComponent<>(Frequency.CODEC, Frequency.DEFAULT)
//                    .syncToClient(Frequency.STREAM_CODEC)
//    );

    public BlockEnderStorage(BlockBehaviour.Properties properties, Supplier<BlockEntityType<? extends T>> supplier) {
        super(properties, supplier);

        serverTicks.addTickerFirst((level, pos, state, tile) -> tile.tick());
        clientTicks.addTickerFirst((level, pos, state, tile) -> tile.tick());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player, ItemStack toolStack, boolean willHarvest, FluidState fluid) {
        return willHarvest || super.onDestroyedByPlayer(state, world, pos, player, toolStack, willHarvest, fluid);
    }

    @Override
    public void playerDestroy(ServerLevel worldIn, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity te, ItemStack stack) {
        super.playerDestroy(worldIn, player, pos, state, te, stack);
        worldIn.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>();
        TileFrequencyOwner tile = (TileFrequencyOwner) builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (tile != null) {
            drops.add(createItem(tile.getFrequency()));
            if (EnderStorageConfig.anarchyMode && tile.getFrequency().hasOwner()) {
                drops.add(EnderStorageConfig.getPersonalItem().copy());
            }
        }
        return drops;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        if (level.getBlockEntity(pos) instanceof TileFrequencyOwner tile) {
            return createItem(tile.getFrequency());
        }
        return ItemStack.EMPTY;
    }

    private ItemStack createItem(Frequency freq) {
        if (EnderStorageConfig.anarchyMode) {
            freq = freq.withoutOwner();
        }
        ItemStack stack = new ItemStack(this, 1);
        freq.putComponent(stack);
        return stack;
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult clientHit) {
        if (world.isClientSide()) return InteractionResult.SUCCESS;

        if (!(world.getBlockEntity(pos) instanceof TileFrequencyOwner tile)) return InteractionResult.FAIL;

        //Normal block trace.
        HitResult rawHit = RayTracer.retrace(player);
        if (!(rawHit instanceof SubHitBlockHitResult hit)) return InteractionResult.FAIL;

        if (hit.subHit == 4) {
            ItemStack item = player.getInventory().getSelectedItem();
            if (player.isCrouching() && tile.getFrequency().hasOwner()) {
                if (!player.getAbilities().instabuild && !player.getInventory().add(EnderStorageConfig.getPersonalItem().copy())) {
                    return InteractionResult.FAIL;
                }

                tile.setFreq(tile.getFrequency().withoutOwner());
                return InteractionResult.SUCCESS;
            }
            if (!item.isEmpty() && ItemUtils.areStacksSameType(item, EnderStorageConfig.getPersonalItem())) {
                if (!tile.getFrequency().hasOwner()) {
                    tile.setFreq(tile.getFrequency().withOwner(player));
                    if (!player.getAbilities().instabuild) {
                        item.shrink(1);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        } else if (hit.subHit >= 1 && hit.subHit <= 3) {
            ItemStack item = player.getInventory().getSelectedItem();
            if (!item.isEmpty()) {
                EnumColour dye = EnumColour.fromDyeStack(item);
                if (dye != null) {
                    EnumColour[] colours = { null, null, null };
                    if (colours[hit.subHit - 1] == dye) {
                        return InteractionResult.FAIL;
                    }
                    colours[hit.subHit - 1] = dye;
                    tile.setFreq(tile.getFrequency().withColours(colours));
                    if (!player.getAbilities().instabuild) {
                        item.shrink(1);
                    }
                    return InteractionResult.FAIL;
                }
            }
        }
        return !player.isCrouching() && tile.activate(player, hit.subHit, hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (world.getBlockEntity(pos) instanceof TileFrequencyOwner tile) {
            tile.onPlaced(placer);
        }
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof TileFrequencyOwner tile) {
            return tile.getLightValue();
        }
        return 0;
    }

    @Override
    public boolean shouldRedstoneWireConnectTo(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction side) {
        return world.getBlockEntity(pos) instanceof TileFrequencyOwner tile && tile.redstoneInteraction();
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof TileFrequencyOwner tile ? tile.comparatorOutput() : 0;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public boolean triggerEvent(BlockState state, Level worldIn, BlockPos pos, int eventID, int eventParam) {
        super.triggerEvent(state, worldIn, pos, eventID, eventParam);
        BlockEntity tileentity = worldIn.getBlockEntity(pos);
        return tileentity != null && tileentity.triggerEvent(eventID, eventParam);
    }

}
