package codechicken.enderstorage.tile;

import codechicken.enderstorage.init.EnderStorageModContent;
import codechicken.enderstorage.manager.EnderStorageManager;
import codechicken.enderstorage.manager.TankState;
import codechicken.enderstorage.network.EnderStorageNetwork;
import codechicken.enderstorage.storage.EnderLiquidStorage;
import codechicken.lib.capability.CapabilityCache;
import codechicken.lib.fluid.FluidUtils;
import codechicken.lib.math.MathHelper;
import net.covers1624.quack.util.SneakyUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public class TileEnderTank extends TileFrequencyOwner {

    private @Nullable TankState liquid_state;
    public final PressureState pressure_state = new PressureState();
    private final CapabilityCache capCache = new CapabilityCache();

    private @Nullable ResourceHandler<FluidResource> fluidHandler;

    private boolean described;

    public TileEnderTank(BlockPos pos, BlockState state) {
        super(EnderStorageModContent.ENDER_TANK_TILE.get(), pos, state);
    }

    public TankState getTankState() {
        return requireNonNull(liquid_state);
    }

    @Override
    public void tick() {
        super.tick();
        assert level != null;
        pressure_state.update(level.isClientSide());
        if (!level.isClientSide() && pressure_state.a_pressure) {
            ejectLiquid();
        }

        getTankState().update();
    }

    @Override
    public void setLevel(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            capCache.setLevelPos(serverLevel, getBlockPos());
            var tankState = new ServerTankState();
            liquid_state = tankState;
            tankState.setFrequency(frequency);
        } else {
            liquid_state = new ClientTankState();
        }
        super.setLevel(level);
    }

    private void ejectLiquid() {
        var source = getFluidHandler();

        for (Direction side : Direction.BY_3D_DATA) {
            var dest = capCache.getCapability(Capabilities.Fluid.BLOCK, side);
            if (dest == null) continue;

            ResourceHandlerUtil.move(source, dest, SneakyUtils.trueP(), 100, null);
            if (source.getAmountAsInt(0) == 0) return;
        }
    }

    @Override
    public void onFrequencySet() {
        if (level == null) {
            return;
        }
        if (getTankState() instanceof ServerTankState tankState) {
            tankState.setFrequency(frequency);
        }
        invalidateCapabilities();
        fluidHandler = null;
    }

    @Override
    public EnderLiquidStorage getStorage() {
        assert level != null;
        return EnderStorageManager.instance(level.isClientSide()).getStorage(frequency, EnderLiquidStorage.TYPE);
    }

    @Override
    public void onPlaced(@Nullable LivingEntity entity) {
        assert level != null;
        pressure_state.b_rotate = pressure_state.a_rotate = pressure_state.approachRotate();
        if (!level.isClientSide()) {
            sendUpdatePacket();
        }
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("ir", pressure_state.invert_redstone);
        if (getTankState() instanceof ServerTankState tankState) {
            output.store("s_liquid", FluidStack.OPTIONAL_CODEC, tankState.s_liquid);
        }
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        if (liquid_state instanceof ServerTankState tankState) tankState.setFrequency(frequency);
        pressure_state.invert_redstone = input.getBooleanOr("ir", false);
        if (liquid_state instanceof ClientTankState tankState) {
            tankState.s_liquid = input.read("s_liquid", FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY);
        }
    }

    @Override
    public void writeToPacket(RegistryFriendlyByteBuf packet) {
        super.writeToPacket(packet);
        packet.cc$writeWithRegistryCodec(FluidStack.OPTIONAL_STREAM_CODEC, getTankState().s_liquid);
        packet.writeBoolean(pressure_state.a_pressure);
    }

    @Override
    public void readFromPacket(RegistryFriendlyByteBuf packet) {
        super.readFromPacket(packet);
        if (getTankState() instanceof ServerTankState tankState) {
            tankState.setFrequency(frequency);
        }
        getTankState().s_liquid = packet.cc$readWithRegistryCodec(FluidStack.OPTIONAL_STREAM_CODEC);
        pressure_state.a_pressure = packet.readBoolean();
        if (!described) {
            getTankState().c_liquid = getTankState().s_liquid;
            pressure_state.b_rotate = pressure_state.a_rotate = pressure_state.approachRotate();
        }
        described = true;
    }

    @Override
    public boolean activate(Player player, int subHit, InteractionHand hand) {
        if (subHit == 4) {
            pressure_state.invert();
            return true;
        }
        return FluidUtil.interactWithFluidHandler(player, hand, getBlockPos(), getFluidHandler(), null);
    }

    @Override
    public int getLightValue() {
        if (getTankState().s_liquid.getAmount() > 0) {
            return FluidUtils.getLuminosity(getTankState().c_liquid, getTankState().s_liquid.getAmount() / 16D);
        }

        return 0;
    }

    @Override
    public boolean redstoneInteraction() {
        return true;
    }

    @Override
    public int comparatorOutput() {
        return ResourceHandlerUtil.getRedstoneSignalFromResourceHandler(getFluidHandler());
    }

    public ResourceHandler<FluidResource> getFluidHandler() {
        if (fluidHandler == null) {
            fluidHandler = getStorage().getHandler();
        }
        return fluidHandler;
    }

    public class ServerTankState extends TankState.Server {

        @Override
        public void sendSyncPacket() {
            var packet = EnderStorageNetwork.LIQUID_SYNC.toClient(TileEnderTank.this);
            packet.writeBlockPos(getBlockPos());
            packet.cc$writeWithRegistryCodec(FluidStack.OPTIONAL_STREAM_CODEC, s_liquid);
            packet.sendToChunk(TileEnderTank.this);
        }

        @Override
        public void onLiquidChanged() {
            assert level != null;
            level.getChunkSource().getLightEngine().checkBlock(worldPosition);
        }
    }

    public class ClientTankState extends TankState.Client {

        @Override
        public void onLiquidChanged() {
            assert level != null;
            level.getChunkSource().getLightEngine().checkBlock(worldPosition);
        }
    }

    public class PressureState {

        public boolean invert_redstone;
        public boolean a_pressure;
        public boolean b_pressure;

        public double a_rotate;
        public double b_rotate;

        public void update(boolean client) {
            assert level != null;
            if (client) {
                b_rotate = a_rotate;
                a_rotate = MathHelper.approachExp(a_rotate, approachRotate(), 0.5, 20);
            } else {
                b_pressure = a_pressure;
                a_pressure = level.hasNeighborSignal(getBlockPos()) != invert_redstone;
                if (a_pressure != b_pressure) {
                    sendSyncPacket();
                }
            }
        }

        public double approachRotate() {
            return a_pressure ? -90 : 90;
        }

        private void sendSyncPacket() {
            var packet = EnderStorageNetwork.PRESSURE_SYNC.toClient(TileEnderTank.this);
            packet.writeBlockPos(getBlockPos());
            packet.writeBoolean(a_pressure);
            packet.sendToChunk(TileEnderTank.this);
        }

        public void invert() {
            assert level != null;
            invert_redstone = !invert_redstone;
            level.blockEntityChanged(getBlockPos());
        }
    }
}
