package codechicken.enderstorage.tile;

import codechicken.enderstorage.api.Frequency;
import codechicken.enderstorage.config.EnderStorageConfig;
import codechicken.enderstorage.init.EnderStorageModContent;
import codechicken.enderstorage.manager.EnderStorageManager;
import codechicken.enderstorage.storage.EnderItemStorage;
import codechicken.lib.block.component.tile.ValueComponent;
import codechicken.lib.math.MathHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.sounds.SoundEvents.*;

public class TileEnderChest extends TileFrequencyOwner {

//    protected final ValueComponent<Frequency>.Data frequency = getData(EnderStorageModContent.ENDER_CHEST_BLOCK.get().frequency);

    public double a_lidAngle;
    public double b_lidAngle;
    public int c_numOpen;

    private @Nullable ResourceHandler<ItemResource> resourceHandler;

    public TileEnderChest(BlockPos pos, BlockState state) {
        super(EnderStorageModContent.ENDER_CHEST_TILE.get(), pos, state);
    }

    @Override
    public void tick() {
        super.tick();

        assert level != null;
        if (!level.isClientSide() && (level.getGameTime() % 20 == 0 || c_numOpen != getStorage().getNumOpen())) {
            c_numOpen = getStorage().getNumOpen();
            level.blockEvent(getBlockPos(), getBlockState().getBlock(), 1, c_numOpen);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }

        b_lidAngle = a_lidAngle;
        a_lidAngle = MathHelper.approachLinear(a_lidAngle, c_numOpen > 0 ? 1 : 0, 0.1);

        if (b_lidAngle >= 0.5 && a_lidAngle < 0.5) {
            level.playSound(null, getBlockPos(), EnderStorageConfig.useVanillaEnderChestSounds ? ENDER_CHEST_CLOSE : CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        } else if (b_lidAngle == 0 && a_lidAngle > 0) {
            level.playSound(null, getBlockPos(), EnderStorageConfig.useVanillaEnderChestSounds ? ENDER_CHEST_OPEN : CHEST_OPEN, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        }
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            c_numOpen = type;
            return true;
        }
        return false;
    }

    public double getRadianLidAngle(float frame) {
        double a = MathHelper.interpolate(b_lidAngle, a_lidAngle, frame);
        a = 1.0F - a;
        a = 1.0F - a * a * a;
        return a * 3.141593 * -0.5;
    }

    @Override
    public EnderItemStorage getStorage() {
        assert level != null;
        return EnderStorageManager.instance(level.isClientSide()).getStorage(frequency, EnderItemStorage.TYPE);
    }

    @Override
    public void onFrequencySet() {
        invalidateCapabilities();
        resourceHandler = null;
    }

    @Override
    public void writeToPacket(RegistryFriendlyByteBuf packet) {
        super.writeToPacket(packet);
    }

    @Override
    public void readFromPacket(RegistryFriendlyByteBuf packet) {
        super.readFromPacket(packet);
    }

    @Override
    public void onPlaced(@Nullable LivingEntity entity) {
        assert level != null;
        onFrequencySet();
        if (!level.isClientSide()) {
            sendUpdatePacket();
        }
    }

    @Override
    public boolean activate(Player player, int subHit, InteractionHand hand) {
        getStorage().openContainer((ServerPlayer) player, Component.translatable(getBlockState().getBlock().asItem().getDescriptionId()));
        return true;
    }

    @Override
    public int comparatorOutput() {
        return ResourceHandlerUtil.getRedstoneSignalFromResourceHandler(getItemHandler());
    }

    public ResourceHandler<ItemResource> getItemHandler() {
        if (resourceHandler == null) {
            resourceHandler = VanillaContainerWrapper.of(getStorage());
        }
        return resourceHandler;
    }
}
