package codechicken.enderstorage.client.render;

import codechicken.lib.math.MathHelper;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.covers1624.quack.util.CrashLock;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Random;

import static codechicken.enderstorage.EnderStorage.MOD_ID;

public class RenderCustomEndPortal {

    private static final CrashLock LOCK = new CrashLock("Already Initialized");

    public static final RenderPipeline SKY_PIPELINE = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/starfield_sky"))
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .build();

    public static final RenderPipeline PORTAL_PIPELINE = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/portal_sky"))
            .withVertexShader("core/position_tex_color")
            .withFragmentShader("core/position_tex_color")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withDepthStencilState(new DepthStencilState(com.mojang.renderpearl.api.pipeline.CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .build();

    private static final RenderType SKY_TYPE = RenderType.create(
            MOD_ID + ":starfield_sky",
            RenderSetup.builder(SKY_PIPELINE)
                    .withTexture("Sampler0", AbstractEndPortalRenderer.END_SKY_LOCATION)
                    .createRenderSetup()
    );

    private static final RenderType PORTAL_TYPE = RenderType.create(
            MOD_ID + ":starfield_portal",
            RenderSetup.builder(PORTAL_PIPELINE)
                    .withTexture("Sampler0", AbstractEndPortalRenderer.END_PORTAL_LOCATION)
                    .createRenderSetup()
    );

    public static void init(IEventBus modBus) {
        LOCK.lock();

        modBus.addListener(RenderCustomEndPortal::onRegisterPipelines);
    }

    private static void onRegisterPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(SKY_PIPELINE);
        event.registerPipeline(PORTAL_PIPELINE);
    }

    private final double surfaceY;
    private final double surfaceX1;
    private final double surfaceX2;
    private final double surfaceZ1;
    private final double surfaceZ2;

    private final Matrix4f textureMatrix = new Matrix4f();
    private final Vector4f tex = new Vector4f();

    public RenderCustomEndPortal(double y, double x1, double x2, double z1, double z2) {
        surfaceY = y;
        surfaceX1 = x1;
        surfaceX2 = x2;
        surfaceZ1 = z1;
        surfaceZ2 = z2;
    }

    public void submit(PoseStack pose, BlockPos pos, SubmitNodeCollector collector, CameraRenderState camera) {
        Random random = new Random(31100L);
        float tileRelX = (float) (pos.getX() - camera.pos.x);
        float tileRelZ = (float) (pos.getZ() - camera.pos.z);

        float parallaxBase = (float) (camera.pos.y - pos.getY() - surfaceY);

        collector.submitCustomGeometry(pose, SKY_TYPE, (p, cons) -> {
            renderLayer(camera, p, cons, 0, parallaxBase, random, tileRelX, tileRelZ);
        });

        collector.submitCustomGeometry(pose, PORTAL_TYPE, (p, cons) -> {
            for (int layer = 1; layer < 16; layer++) {
                renderLayer(camera, p, cons, layer, parallaxBase, random, tileRelX, tileRelZ);
            }
        });
    }

    private void renderLayer(CameraRenderState camera, PoseStack.Pose p, VertexConsumer cons, int layer, float parallaxBase, Random random, float tileRelX, float tileRelZ) {
        float layerDepth = 16F - layer;
        float scale = 1F / 16F;
        float layerIntensity = 1F / (layerDepth + 1F);
        if (layer == 0) {
            layerIntensity = 0.1F;
            layerDepth = 65F;
            scale = 0.125F;
        } else if (layer == 1) {
            scale = 0.5F;
        }

        float q = -parallaxBase / (parallaxBase + layerDepth);

        computeTextureMatrix(layer, scale, camera);

        float r = (random.nextFloat() * 0.5F + 0.1F) * layerIntensity;
        float g = (random.nextFloat() * 0.5F + 0.4F) * layerIntensity;
        float b = (random.nextFloat() * 0.5F + 0.5F) * layerIntensity;
        if (layer == 0) {
            r = layerIntensity;
            g = layerIntensity;
            b = layerIntensity;
        }

        bufferVertex(cons, p, surfaceX1, surfaceY, surfaceZ1, tileRelX, tileRelZ, q, r, g, b);
        bufferVertex(cons, p, surfaceX1, surfaceY, surfaceZ2, tileRelX, tileRelZ, q, r, g, b);
        bufferVertex(cons, p, surfaceX2, surfaceY, surfaceZ2, tileRelX, tileRelZ, q, r, g, b);
        bufferVertex(cons, p, surfaceX2, surfaceY, surfaceZ1, tileRelX, tileRelZ, q, r, g, b);
    }

    private void computeTextureMatrix(int layer, float scale, CameraRenderState camera) {
        textureMatrix.identity();
        textureMatrix.translate(0, System.currentTimeMillis() % 700000L / 700000F, 0);
        textureMatrix.scale(scale);
        textureMatrix.translate(0.5F, 0.5F, 0F);
        textureMatrix.rotateZ((float) (((layer * layer * 4321 + layer * 9) * 2F) * MathHelper.torad));
        textureMatrix.translate(-0.5F, -0.5F, 0F);
        textureMatrix.translate((float) -camera.pos.x, (float) -camera.pos.z, (float) -camera.pos.y);
        textureMatrix.translate(0, 0, (float) -camera.pos.y);
    }

    private void bufferVertex(VertexConsumer consumer, PoseStack.Pose pose,
            double x, double y, double z,
            float tileRelX, float tileRelZ,
            float q,
            float red, float green, float blue
    ) {
        tex.set(
                (float) x + tileRelX,
                (float) z + tileRelZ,
                1.0f,
                q
        );
        tex.mul(textureMatrix);
        float u = tex.x / tex.w;
        float v = tex.y / tex.w;

        consumer.addVertex(pose, (float) x, (float) y, (float) z).setUv(u, v).setColor(red, green, blue, 1.0F);
    }
}
