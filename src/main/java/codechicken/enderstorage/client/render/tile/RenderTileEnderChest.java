package codechicken.enderstorage.client.render.tile;

import codechicken.enderstorage.api.Frequency;
import codechicken.enderstorage.block.BlockEnderChest;
import codechicken.enderstorage.client.model.ButtonModelLibrary;
import codechicken.enderstorage.client.model.EnderChestModel;
import codechicken.enderstorage.client.render.RenderCustomEndPortal;
import codechicken.enderstorage.init.EnderStorageModContent;
import codechicken.enderstorage.tile.TileEnderChest;
import codechicken.lib.colour.EnumColour;
import codechicken.lib.math.MathHelper;
import codechicken.lib.render.CCModelLibrary;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.util.ClientUtils;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.uv.UVTranslation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.function.Consumer;

import static codechicken.enderstorage.EnderStorage.MOD_ID;

/**
 * Created by covers1624 on 4/12/2016.
 */
public class RenderTileEnderChest implements BlockEntityRenderer<TileEnderChest, RenderTileEnderChest.RenderState> {

    private static final RenderType buttonType = RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(MOD_ID, "textures/buttons.png"));
    private static final RenderType pearlType = CCModelLibrary.getIcos7RenderType(Identifier.fromNamespaceAndPath(MOD_ID, "textures/hedronmap.png"));
    private static final RenderCustomEndPortal renderEndPortal = new RenderCustomEndPortal(0.626, 0.188, 0.812, 0.188, 0.812);

    private static final EnderChestModel model = new EnderChestModel();

    public RenderTileEnderChest(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(TileEnderChest tile, RenderState state, float partialTick, Vec3 camera, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, camera, breakProgress);
        state.rotation = EnderStorageModContent.ENDER_CHEST_BLOCK.get()
                .rotation
                .get(tile.getBlockState());
        state.frequency = tile.getFrequency();
        state.lidAngle = (float) tile.getRadianLidAngle(partialTick);
        state.pearlOffset = RenderUtils.getTimeOffset(tile.getBlockPos());
    }

    @Override
    public void submit(RenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        doSubmit(state, pose, collector, camera);
    }

    public static void getExtents(Consumer<Vector3fc> output) {
        model.root().getExtentsForGui(new PoseStack(), output);
    }

    public static void doSubmit(RenderState state, PoseStack pose, SubmitNodeCollector collector, @Nullable CameraRenderState camera) {
        if (camera != null) {
            renderEndPortal.submit(pose, state.blockPos, collector, camera);
        }
        pose.pushPose();
        pose.translate(0, 1.0, 1.0);
        pose.scale(1.0F, -1.0F, -1.0F);
        pose.translate(0.5, 0.5, 0.5);
        pose.rotateDegrees(Axis.YP, state.rotation.toYRot());
        pose.translate(-0.5, -0.5, -0.5);

        // Render chest
        var modelState = new EnderChestModel.State(state.lidAngle, state.frequency.hasOwner());
        collector.submitModel(
                model,
                modelState,
                pose,
                EnderChestModel.RENDER_TYPE,
                state.lightCoords,
                state.overlayCoords,
                -1,
                null,
                0
        );
        if (state.breakProgress != null) {
            collector.order(1).submitCrumblingOverlay(
                    model,
                    modelState,
                    pose,
                    EnderChestModel.RENDER_TYPE,
                    state.lightCoords,
                    state.overlayCoords,
                    -1,
                    state.breakProgress
            );
        }
        pose.popPose();

        // Buttons
        Matrix4 mat = new Matrix4(pose);
        mat.translate(0.5, 0, 0.5);
        collector.cc$submitCCRS(mat, buttonType, (buttonCommon, ccrs) -> {
            ccrs.brightness = state.lightCoords;
            ccrs.overlay = state.overlayCoords;

            buttonCommon.rotate(state.rotation.toYRot() * MathHelper.torad, Vector3.Y_POS);
            buttonCommon.apply(new Rotation(state.lidAngle, 1, 0, 0).at(new Vector3(-8 / 16D, 9D / 16D, -7 / 16D)));

            EnumColour[] colours = state.frequency.toArray();
            for (int i = 0; i < 3; i++) {
                Matrix4 buttonMat = buttonCommon.copy();
                buttonMat.apply(BlockEnderChest.buttonT[i]);
                ButtonModelLibrary.button.render(ccrs, buttonMat, new UVTranslation(0.25 * (colours[i].getWoolMeta() % 4), 0.25 * (colours[i].getWoolMeta() / 4)));
            }
        });
        // Pearl
        if (state.lidAngle != 0) {
            collector.cc$submitCCRS(mat, pearlType, (pearlMat, ccrs) -> {
                ccrs.brightness = 15728880;
                ccrs.overlay = state.overlayCoords;

                double time = ClientUtils.getRenderTime() + state.pearlOffset;
                CCModelLibrary.icosahedron7.render(
                        ccrs,
                        RenderUtils.getMatrix(pearlMat, new Vector3(0, 0.2 + state.lidAngle * -0.5 + RenderUtils.getPearlBob(time), 0), new Rotation(time / 3, new Vector3(0, 1, 0)), 0.04)
                );
            });
        }
    }

    public static class RenderState extends BlockEntityRenderState {

        public Direction rotation = Direction.SOUTH;
        public Frequency frequency = Frequency.DEFAULT;
        public float lidAngle;
        public int pearlOffset;

        public int overlayCoords = OverlayTexture.NO_OVERLAY;
    }
}
