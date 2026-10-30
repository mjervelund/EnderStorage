package codechicken.enderstorage.client.render.item;

import codechicken.enderstorage.api.Frequency;
import codechicken.enderstorage.client.render.tile.RenderTileEnderTank;
import codechicken.enderstorage.client.render.tile.RenderTileEnderTank.RenderState;
import codechicken.enderstorage.manager.ClientTankSynchronizer;
import codechicken.lib.math.MathHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by covers1624 on 4/27/2016.
 */
public class EnderTankItemRender implements SpecialModelRenderer<RenderState> {

    @Override
    public RenderState extractArgument(ItemStack stack) {
        RenderState state = new RenderState();
        state.valveRotation = (float) (MathHelper.torad * 90F);
        state.frequency = Frequency.getComponentOrEmpty(stack);
        state.fluid = ClientTankSynchronizer.getClientLiquid(state.frequency);
        return state;
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        RenderTileEnderTank.getExtents(output);
    }

    @Override
    public void submit(@Nullable RenderState state, PoseStack pose, SubmitNodeCollector collector, int packedLight, int packedOverlay, boolean hasFoil, int outlineColor) {
        if (state == null) return;

        state.lightCoords = packedLight;
        state.overlayCoords = packedOverlay;
        RenderTileEnderTank.doSubmit(state, pose, collector, null);
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<RenderState> {

        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<RenderState>> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<RenderState> bake(BakingContext context) {
            return new EnderTankItemRender();
        }
    }
}
