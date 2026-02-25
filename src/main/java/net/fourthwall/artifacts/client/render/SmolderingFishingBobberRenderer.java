package net.fourthwall.artifacts.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fourthwall.artifacts.registry.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FishingBobberEntityRenderer;
import net.minecraft.client.render.entity.state.FishingBobberEntityState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class SmolderingFishingBobberRenderer extends EntityRenderer<FishingBobberEntity, SmolderingFishingBobberRenderer.State> {
    private static final Identifier VANILLA_TEXTURE = Identifier.ofVanilla("textures/entity/fishing_hook.png");
    private static final Identifier SMOLDERING_TEXTURE = Identifier.of("evanpack", "textures/entity/smoldering_rod_hook.png");
    private static final RenderLayer VANILLA_LAYER = RenderLayers.entityCutout(VANILLA_TEXTURE);
    private static final RenderLayer SMOLDERING_LAYER = RenderLayers.entityCutout(SMOLDERING_TEXTURE);

    public SmolderingFishingBobberRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(FishingBobberEntity fishingBobberEntity, Frustum frustum, double d, double e, double f) {
        return super.shouldRender(fishingBobberEntity, frustum, d, e, f) && fishingBobberEntity.getPlayerOwner() != null;
    }

    @Override
    public void render(State state, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState) {
        RenderLayer layer = state.useSmolderingTexture ? SMOLDERING_LAYER : VANILLA_LAYER;

        matrixStack.push();
        matrixStack.push();
        matrixStack.scale(0.5F, 0.5F, 0.5F);
        matrixStack.multiply(cameraRenderState.orientation);
        orderedRenderCommandQueue.submitCustom(matrixStack, layer, (matricesEntry, vertexConsumer) -> {
            vertex(vertexConsumer, matricesEntry, state.light, 0.0F, 0, 0, 1);
            vertex(vertexConsumer, matricesEntry, state.light, 1.0F, 0, 1, 1);
            vertex(vertexConsumer, matricesEntry, state.light, 1.0F, 1, 1, 0);
            vertex(vertexConsumer, matricesEntry, state.light, 0.0F, 1, 0, 0);
        });
        matrixStack.pop();

        float x = (float) state.pos.x;
        float y = (float) state.pos.y;
        float z = (float) state.pos.z;
        float lineWidth = MinecraftClient.getInstance().getWindow().getMinimumLineWidth();
        orderedRenderCommandQueue.submitCustom(matrixStack, RenderLayers.lines(), (matricesEntry, vertexConsumer) -> {
            for (int i = 0; i < 16; i++) {
                float a = percentage(i, 16);
                float b = percentage(i + 1, 16);
                renderFishingLine(x, y, z, vertexConsumer, matricesEntry, a, b, lineWidth);
                renderFishingLine(x, y, z, vertexConsumer, matricesEntry, b, a, lineWidth);
            }
        });
        matrixStack.pop();
        super.render(state, matrixStack, orderedRenderCommandQueue, cameraRenderState);
    }

    private Vec3d getHandPos(PlayerEntity player, float handRotation, float tickProgress) {
        int side = FishingBobberEntityRenderer.getArmHoldingRod(player) == Arm.RIGHT ? 1 : -1;
        if (this.dispatcher.gameOptions.getPerspective().isFirstPerson() && player == MinecraftClient.getInstance().player) {
            double fovScale = 960.0 / this.dispatcher.gameOptions.getFov().getValue().intValue();
            Vec3d hand = this.dispatcher.camera.getProjection().getPosition(side * 0.525F, -0.1F)
                    .multiply(fovScale)
                    .rotateY(handRotation * 0.5F)
                    .rotateX(-handRotation * 0.7F);
            return player.getCameraPosVec(tickProgress).add(hand);
        }

        float bodyYaw = MathHelper.lerp(tickProgress, player.lastBodyYaw, player.bodyYaw) * (float) (Math.PI / 180.0);
        double sin = MathHelper.sin(bodyYaw);
        double cos = MathHelper.cos(bodyYaw);
        float scale = player.getScale();
        double sideOffset = side * 0.35 * scale;
        double forwardOffset = 0.8 * scale;
        float sneakOffset = player.isInSneakingPose() ? -0.1875F : 0.0F;
        return player.getCameraPosVec(tickProgress).add(-cos * sideOffset - sin * forwardOffset, sneakOffset - 0.45 * scale, -sin * sideOffset + cos * forwardOffset);
    }

    private static float percentage(int value, int denominator) {
        return (float) value / denominator;
    }

    private static void vertex(VertexConsumer buffer, MatrixStack.Entry matrix, int light, float x, int y, int u, int v) {
        buffer.vertex(matrix, x - 0.5F, y - 0.5F, 0.0F)
                .color(Colors.WHITE)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(matrix, 0.0F, 1.0F, 0.0F);
    }

    private static void renderFishingLine(float x, float y, float z, VertexConsumer buffer, MatrixStack.Entry matrices, float from, float to, float lineWidth) {
        float segX = x * from;
        float segY = y * (from * from + from) * 0.5F + 0.25F;
        float segZ = z * from;
        float dx = x * to - segX;
        float dy = y * (to * to + to) * 0.5F + 0.25F - segY;
        float dz = z * to - segZ;
        float length = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= length;
        dy /= length;
        dz /= length;
        buffer.vertex(matrices, segX, segY, segZ).color(Colors.BLACK).normal(matrices, dx, dy, dz).lineWidth(lineWidth);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void updateRenderState(FishingBobberEntity bobber, State state, float tickProgress) {
        super.updateRenderState(bobber, state, tickProgress);

        PlayerEntity owner = bobber.getPlayerOwner();
        if (owner == null) {
            state.pos = Vec3d.ZERO;
            state.useSmolderingTexture = false;
            return;
        }

        Arm rodArm = FishingBobberEntityRenderer.getArmHoldingRod(owner);
        state.useSmolderingTexture = owner.getStackInArm(rodArm).isOf(ModItems.SMOLDERING_ROD);

        float swing = owner.getHandSwingProgress(tickProgress);
        float handRotation = MathHelper.sin(MathHelper.sqrt(swing) * (float) Math.PI);
        Vec3d handPos = this.getHandPos(owner, handRotation, tickProgress);
        Vec3d bobberPos = bobber.getLerpedPos(tickProgress).add(0.0, 0.25, 0.0);
        state.pos = handPos.subtract(bobberPos);
    }

    @Override
    protected boolean canBeCulled(FishingBobberEntity bobber) {
        return false;
    }

    @Environment(EnvType.CLIENT)
    public static class State extends FishingBobberEntityState {
        public boolean useSmolderingTexture;
    }
}
