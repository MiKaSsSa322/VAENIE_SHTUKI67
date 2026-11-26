package com.frontiermilitary.client;

import com.frontiermilitary.FrontierMilitary;
import com.frontiermilitary.item.PistolData;
import com.frontiermilitary.item.ServicePistolItem;
import com.frontiermilitary.network.ModNetwork;
import com.frontiermilitary.network.ReloadPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Клиентские события (FORGE-шина): клавиша перезарядки и собственная отрисовка рук с пистолетом
 * от первого лица (отдача, смена магазина). Для леворуких игроков остаётся ванильная отрисовка предмета.
 */
@Mod.EventBusSubscriber(modid = FrontierMilitary.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        while (ModKeys.RELOAD.consumeClick()) {
            if (player != null && mc.screen == null && holdsPistol(player)) {
                ModNetwork.CHANNEL.sendToServer(new ReloadPacket());
            }
        }
    }

    private static boolean holdsPistol(LocalPlayer player) {
        return player.getMainHandItem().getItem() instanceof ServicePistolItem
                || player.getOffhandItem().getItem() instanceof ServicePistolItem;
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ServicePistolItem)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        boolean rightSide = (event.getHand() == InteractionHand.MAIN_HAND) == (player.getMainArm() == HumanoidArm.RIGHT);
        if (!rightSide) {
            return; // леворукий режим — стандартная отрисовка предмета
        }

        event.setCanceled(true);
        PoseStack ps = event.getPoseStack();
        float pt = event.getPartialTick();
        long now = mc.level.getGameTime();

        ps.pushPose();
        // базовая позиция предмета в руке (как в ванили) + опускание при доставании
        ps.translate(0.56F, -0.52F + event.getEquipProgress() * -0.6F, -0.72F);
        applyAnimation(ps, stack, now, pt);

        ItemRenderer renderer = mc.getItemRenderer();
        ItemStack hands = stack.copy();
        hands.getOrCreateTag().putBoolean(ClientSetup.TAG_FP_HANDS, true);

        renderer.renderStatic(player, hands, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, false, ps,
                event.getMultiBufferSource(), mc.level, event.getPackedLight(), OverlayTexture.NO_OVERLAY, 0);
        renderer.renderStatic(player, stack, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, false, ps,
                event.getMultiBufferSource(), mc.level, event.getPackedLight(), OverlayTexture.NO_OVERLAY, 0);
        ps.popPose();
    }

    private static void applyAnimation(PoseStack ps, ItemStack stack, long now, float pt) {
        // --- отдача: рывок на игрока и подброс ствола, затухает за ~8 тиков
        long since = PistolData.sinceShot(stack, now);
        if (since >= 0 && since < 12) {
            float t = since + pt;
            float k = Math.max(0.0F, 1.0F - t / 8.0F);
            k = k * k;
            ps.translate(0.0F, 0.015F * k, 0.11F * k);
            ps.mulPose(Axis.XP.rotationDegrees(9.0F * k));
        }

        // --- перезарядка: оружие чуть опускается и кренится, пока руки меняют магазин
        long reload = PistolData.reloadElapsed(stack, now);
        if (reload >= 0 && reload < PistolData.RELOAD_TICKS) {
            float p = Mth.clamp((reload + pt) / (float) PistolData.RELOAD_TICKS, 0.0F, 1.0F);
            float s = Mth.sin(p * (float) Math.PI);
            s = Math.min(1.0F, s * 1.6F);
            ps.translate(-0.02F * s, -0.08F * s, 0.02F * s);
            ps.mulPose(Axis.XP.rotationDegrees(-9.0F * s));
            ps.mulPose(Axis.ZP.rotationDegrees(-13.0F * s));
            ps.mulPose(Axis.YP.rotationDegrees(7.0F * s));
        }
    }

    private ClientForgeEvents() {}
}
