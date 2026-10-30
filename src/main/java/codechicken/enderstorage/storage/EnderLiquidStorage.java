package codechicken.enderstorage.storage;

import codechicken.enderstorage.api.AbstractEnderStorage;
import codechicken.enderstorage.api.Frequency;
import codechicken.enderstorage.api.StorageType;
import codechicken.enderstorage.manager.EnderStorageManager;
import net.neoforged.neoforge.fluids.FluidType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

public class EnderLiquidStorage extends AbstractEnderStorage {

    public static final StorageType<EnderLiquidStorage> TYPE = new StorageType<>("liquid");

    // FluidType.BUCKET_VOLUME directly: CCL's FluidUtils creates FluidStacks in its static
    // initializer, which throws "Components not bound yet" during mod construction on 26.3.
    public static final int CAPACITY = 16 * FluidType.BUCKET_VOLUME;

    private final FluidStacksResourceHandler handler = new FluidStacksResourceHandler(1, CAPACITY) {
        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            setDirty();
        }
    };

    public EnderLiquidStorage(EnderStorageManager manager, Frequency freq) {
        super(manager, freq);
    }

    @Override
    public void loadFromTag(ValueInput input) {
        handler.deserialize(input.childOrEmpty("tank"));
    }

    @Override
    public void saveToTag(ValueOutput output) {
        handler.serialize(output.child("tank"));
    }

    @Override
    public String type() {
        return "liquid";
    }

    public ResourceHandler<FluidResource> getHandler() {
        return handler;
    }

    public FluidStack getFluid() {
        return FluidUtil.getStack(handler, 0);
    }
}
