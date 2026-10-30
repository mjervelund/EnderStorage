package codechicken.enderstorage.client.render.item;

import codechicken.enderstorage.api.Frequency;
import codechicken.enderstorage.client.render.tile.RenderTileEnderChest;
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
public class EnderChestItemRender implements SpecialModelRenderer<RenderTileEnderChest.RenderState> {

    @Override
    public RenderTileEnderChest.RenderState extractArgument(ItemStack stack) {
        var state = new RenderTileEnderChest.RenderState();
        state.frequency = Frequency.getComponentOrEmpty(stack);
        return state;
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        RenderTileEnderChest.getExtents(output);
    }

    @Override
    public void submit(@Nullable RenderTileEnderChest.RenderState state, PoseStack pose, SubmitNodeCollector collector, int packedLight, int packedOverlay, boolean hasFoil, int outlineColor) {
        if (state == null) return;

        state.lightCoords = packedLight;
        state.overlayCoords = packedOverlay;
        RenderTileEnderChest.doSubmit(state, pose, collector, null);
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<RenderTileEnderChest.RenderState> {

        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<RenderTileEnderChest.RenderState>> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<RenderTileEnderChest.RenderState> bake(BakingContext context) {
            return new EnderChestItemRender();
        }
    }
}
