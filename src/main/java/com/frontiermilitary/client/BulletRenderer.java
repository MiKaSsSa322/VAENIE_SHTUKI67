package com.frontiermilitary.client;

import com.frontiermilitary.FrontierMilitary;
import com.frontiermilitary.entity.BulletEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Трассер пули: две пересекающиеся светящиеся полоски вдоль траектории. */
public class BulletRenderer extends EntityRenderer<BulletEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(FrontierMilitary.MODID, "textures/entity/bullet_tracer.png");
    private static final float LENGTH = 1.4F;
    private static final float HALF_WIDTH = 0.035F;

    public BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BulletEntity bullet, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (bullet.tickCount < 2) {
            return; // не рисуем в первые тики, чтобы трассер не «бил» в камеру стрелка
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, bullet.yRotO, bullet.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, bullet.xRotO, bullet.getXRot())));

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        int light = LightTexture.FULL_BRIGHT;
        // плоскость XY
        quad(vc, pose, light, HALF_WIDTH, 0.0F);
        // плоскость XZ
        quad(vc, pose, light, 0.0F, HALF_WIDTH);

        poseStack.popPose();
        super.render(bullet, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, int light, float hy, float hz) {
        float x0 = -LENGTH;
        float x1 = 0.0F;
        vertex(vc, pose, x0, -hy, -hz, 0.0F, 0.0F, light);
        vertex(vc, pose, x1, -hy, -hz, 1.0F, 0.0F, light);
        vertex(vc, pose, x1, hy, hz, 1.0F, 1.0F, light);
        vertex(vc, pose, x0, hy, hz, 0.0F, 1.0F, light);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int light) {
        vc.vertex(pose.pose(), x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(BulletEntity bullet) {
        return TEXTURE;
    }
}
