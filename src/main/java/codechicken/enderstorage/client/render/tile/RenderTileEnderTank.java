package codechicken.enderstorage.client.render.tile;

import codechicken.enderstorage.api.Frequency;
import codechicken.enderstorage.block.BlockEnderTank;
import codechicken.enderstorage.client.model.ButtonModelLibrary;
import codechicken.enderstorage.client.render.RenderCustomEndPortal;
import codechicken.enderstorage.init.EnderStorageModContent;
import codechicken.enderstorage.tile.TileEnderTank;
import codechicken.lib.colour.EnumColour;
import codechicken.lib.fluid.FluidUtils;
import codechicken.lib.math.MathHelper;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCModelLibrary;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.RenderUtils;
import codechicken.lib.render.buffer.ExtentsConsumer;
import codechicken.lib.render.model.OBJParser;
import codechicken.lib.util.ClientUtils;
import codechicken.lib.vec.*;
import codechicken.lib.vec.uv.UVTranslation;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
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
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.Map;
import java.util.function.Consumer;

import static codechicken.enderstorage.EnderStorage.MOD_ID;

public class RenderTileEnderTank implements BlockEntityRenderer<TileEnderTank, RenderTileEnderTank.RenderState> {

    private static final RenderType baseType = RenderTypes.entityCutout(Identifier.fromNamespaceAndPath(MOD_ID, "textures/endertank.png"));
    private static final RenderType buttonType = RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(MOD_ID, "textures/buttons.png"));
    private static final RenderType pearlType = CCModelLibrary.getIcos7RenderType(Identifier.fromNamespaceAndPath(MOD_ID, "textures/hedronmap.png"));

    public static final CCModel tankModel;
    public static final CCModel valveModel;
    public static final CCModel[] buttons;
    public static final RenderCustomEndPortal renderEndPortal = new RenderCustomEndPortal(0.1205, 0.24, 0.76, 0.24, 0.76);

    static {
        Map<String, CCModel> models = new OBJParser(Identifier.fromNamespaceAndPath(MOD_ID, "models/endertank.obj"))
                .quads()
                .swapYZ()
                .parse();
        Transformation fix = new Translation(-0.0099 - 0.5, 0, -0.0027 - 0.5);
        valveModel = models.remove("Valve").apply(fix).computeNormals();
        tankModel = CCModel.combine(models.values()).apply(fix).computeNormals().shrinkUVs(0.004);

        buttons = new CCModel[3];
        for (int i = 0; i < 3; i++) {
            buttons[i] = ButtonModelLibrary.button.copy().apply(BlockEnderTank.buttonT[i].with(new Translation(-0.5, 0, -0.5)));
        }
    }

    public RenderTileEnderTank(BlockEntityRendererProvider.Context context) {
    }

    public static void getExtents(Consumer<Vector3fc> output) {
        Matrix4 mat = new Matrix4();
        var ccrs = CCRenderState.instance();
        ccrs.reset();
        mat.translate(0.5, 0, 0.5);
        ccrs.bind(new ExtentsConsumer(output), DefaultVertexFormat.POSITION);
        tankModel.render(ccrs, mat);
        valveModel.render(ccrs, mat);
        for (int i = 0; i < 3; i++) {
            buttons[i].render(ccrs, mat);
        }
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(TileEnderTank tile, RenderState state, float partialTick, Vec3 camera, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, camera, breakProgress);

        state.rotation = EnderStorageModContent.ENDER_CHEST_BLOCK.get()
                .rotation
                .get(tile.getBlockState());
        state.frequency = tile.getFrequency();
        state.valveRotation = (float) MathHelper.interpolate(tile.pressure_state.b_rotate, tile.pressure_state.a_rotate, partialTick) * 0.01745F;
        state.pearlOffset = RenderUtils.getTimeOffset(tile.getBlockPos());
        state.fluid = tile.getTankState().c_liquid.copy();
    }

    @Override
    public void submit(RenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        doSubmit(state, pose, collector, camera);
    }

    public static void doSubmit(RenderState state, PoseStack pose, SubmitNodeCollector collector, @Nullable CameraRenderState camera) {
        if (camera != null) {
            renderEndPortal.submit(pose, state.blockPos, collector, camera);
        }
        Matrix4 mat = new Matrix4(pose);
        mat.translate(0.5, 0, 0.5);
        mat.rotate((-state.rotation.toYRot() + 180F) * MathHelper.torad, Vector3.Y_POS);

        collector.cc$submitCCRS(mat, baseType, (baseMat, ccrs) -> {
            ccrs.brightness = state.lightCoords;
            ccrs.overlay = state.overlayCoords;

            tankModel.render(ccrs, mat);
            Matrix4 valveMat = mat.copy().apply(new Rotation(state.valveRotation, Vector3.Z_POS).at(new Vector3(0, 0.4165, 0)));
            valveModel.render(ccrs, valveMat, new UVTranslation(0, state.frequency.hasOwner() ? 13 / 64D : 0));
        });

        collector.cc$submitCCRS(mat, buttonType, (buttonMat, ccrs) -> {
            ccrs.brightness = state.lightCoords;
            ccrs.overlay = state.overlayCoords;

            EnumColour[] colours = state.frequency.toArray();
            for (int i = 0; i < 3; i++) {
                //noinspection IntegerDivisionInFloatingPointContext
                buttons[i].render(ccrs, mat, new UVTranslation(0.25 * (colours[i].getWoolMeta() % 4), 0.25 * (colours[i].getWoolMeta() / 4)));
            }
        });

        collector.cc$submitCCRS(mat, pearlType, (pearlMat, ccrs) -> {
            ccrs.brightness = 15728880;
            ccrs.overlay = state.overlayCoords;

            double time = ClientUtils.getRenderTime() + state.pearlOffset;
            CCModelLibrary.icosahedron7.render(
                    ccrs,
                    RenderUtils.getMatrix(pearlMat, new Vector3(0, 0.45 + RenderUtils.getPearlBob(time) * 2, 0), new Rotation(time / 3, Vector3.Y_POS), 0.04)
            );
        });

        if (!state.fluid.isEmpty()) {
            collector.cc$submitCCRS(pose, RenderTypes.translucentMovingBlock(), (m, ccrs) -> {
                RenderUtils.renderFluidCuboid(
                        ccrs,
                        m,
                        state.fluid,
                        new Cuboid6(0.22, 0.12, 0.22, 0.78, 0.121 + 0.63, 0.78),
                        state.fluid.getAmount() / (16D * FluidUtils.B),
                        0.75
                );
            });
        }
    }

    public static class RenderState extends BlockEntityRenderState {

        public Direction rotation = Direction.SOUTH;
        public Frequency frequency = Frequency.DEFAULT;
        public float valveRotation;
        public int pearlOffset;
        public FluidStack fluid = FluidStack.EMPTY;

        public int overlayCoords = OverlayTexture.NO_OVERLAY;
    }
}
