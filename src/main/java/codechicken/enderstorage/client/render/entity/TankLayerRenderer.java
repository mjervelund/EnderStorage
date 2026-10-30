package codechicken.enderstorage.client.render.entity;

import codechicken.enderstorage.client.render.tile.RenderTileEnderTank;
import codechicken.lib.math.MathHelper;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.util.ClientUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.covers1624.quack.collection.FastStream;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Quaternionf;

import java.util.Set;
import java.util.UUID;

import static codechicken.enderstorage.EnderStorage.MOD_ID;

/**
 * Created by covers1624 on 15/12/2016.
 */
public class TankLayerRenderer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public static final ContextKey<UUID> PLAYER_UUID = new ContextKey<>(Identifier.fromNamespaceAndPath(MOD_ID, "player_uuid"));

    private static final Set<UUID> UUIDS = FastStream.of(
                    "c85f3fd3-1754-45ec-ab3d-a33d6312dfef",
                    "c501d550-7e3c-463e-8a95-256f86d9a47d",
                    "cf3e2c7e-d703-48e0-808e-f139bf26ff9d",
                    "44ba40ef-fd8a-446f-834b-5aea42119c92"
            )
            .map(UUID::fromString)
            .toSet();

    public TankLayerRenderer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int packedLight, AvatarRenderState state, float yRot, float xRot) {
        if (!UUIDS.contains(state.getRenderData(PLAYER_UUID))) return;

        pose.pushPose();
        pose.rotate(new Quaternionf().rotateX((float) (MathHelper.torad * 180)));
        pose.scale(0.5F, 0.5F, 0.5F);
        if (state.isCrouching) {
            pose.translate(0, -0.5, 0);
        }
        if (state.isFallFlying) {
            xRot = -45;
        }
        pose.rotate(new Quaternionf().rotateY((float) -(yRot * MathHelper.torad)));
        pose.rotate(new Quaternionf().rotateX((float) (xRot * MathHelper.torad)));
        pose.translate(-0.5, 1, -0.5);

        RenderTileEnderTank.RenderState tankState = new RenderTileEnderTank.RenderState();
        tankState.lightCoords = packedLight;
        tankState.valveRotation = (float) (MathHelper.torad * 90F);
        tankState.fluid = new FluidStack(
                Fluids.WATER,
                (int) MathHelper.map(
                        0.45F + RenderUtils.getPearlBob(ClientUtils.getRenderTime()) * 2,
                        0.2,
                        0.6,
                        1000,
                        14000
                )
        );
        RenderTileEnderTank.doSubmit(tankState, pose, collector, null);
        pose.popPose();
    }
}
