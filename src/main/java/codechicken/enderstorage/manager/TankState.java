package codechicken.enderstorage.manager;

import codechicken.enderstorage.api.Frequency;
import codechicken.enderstorage.storage.EnderLiquidStorage;
import codechicken.lib.math.MathHelper;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Created by covers1624 on 11/2/25.
 */
public abstract class TankState {

    public FluidStack c_liquid = FluidStack.EMPTY;
    public FluidStack s_liquid = FluidStack.EMPTY;

    public abstract void update();

    protected void fireFluidChange(FluidStack a_liquid, FluidStack b_liquid) {
        if ((b_liquid.getAmount() == 0) != (a_liquid.getAmount() == 0) || !FluidStack.isSameFluidSameComponents(b_liquid, a_liquid)) {
            onLiquidChanged();
        }
    }

    public void onLiquidChanged() {
    }

    public abstract static class Server extends TankState {

        public Frequency frequency = Frequency.DEFAULT;

        public void setFrequency(Frequency frequency) {
            this.frequency = frequency;
        }

        @Override
        public void update() {
            FluidStack b_liquid = s_liquid.copy();
            s_liquid = EnderStorageManager.instance(false)
                    .getStorage(frequency, EnderLiquidStorage.TYPE)
                    .getFluid();
            if (!FluidStack.isSameFluidSameComponents(s_liquid, c_liquid)) {
                sendSyncPacket();
                c_liquid = s_liquid.copy();
            } else if (Math.abs(c_liquid.getAmount() - s_liquid.getAmount()) > 250 || (s_liquid.getAmount() == 0 && c_liquid.getAmount() > 0)) {// Diff grater than 250 Or server no longer has liquid and client does.
                sendSyncPacket();
                c_liquid = s_liquid.copy();
            }

            fireFluidChange(s_liquid, b_liquid);
        }

        public abstract void sendSyncPacket();
    }

    public static class Client extends TankState {

        public FluidStack f_liquid = FluidStack.EMPTY;

        @Override
        public void update() {
            FluidStack b_liquid = c_liquid.copy();

            if (FluidStack.isSameFluidSameComponents(s_liquid, c_liquid) || c_liquid.isEmpty()) {
                int change = MathHelper.approachExpI(c_liquid.getAmount(), s_liquid.getAmount(), 0.1);
                if (c_liquid.isEmpty()) {
                    c_liquid = s_liquid.copyWithAmount(change);
                } else {
                    c_liquid.setAmount(change);
                }
            } else if (c_liquid.getAmount() > 100) {
                c_liquid.setAmount(MathHelper.retreatExpI(c_liquid.getAmount(), 0, f_liquid.getAmount(), 0.1, 1000));
            }

            fireFluidChange(c_liquid, b_liquid);
        }

        public void sync(FluidStack liquid) {
            s_liquid = liquid;
            if (!FluidStack.isSameFluidSameComponents(s_liquid, c_liquid)) {
                f_liquid = c_liquid.copy();
            }
        }
    }
}
