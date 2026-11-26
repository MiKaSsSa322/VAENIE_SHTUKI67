package com.frontiermilitary.client;

import com.frontiermilitary.FrontierMilitary;
import com.frontiermilitary.item.PistolData;
import com.frontiermilitary.registry.ModEntities;
import com.frontiermilitary.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import javax.annotation.Nullable;

/** Клиентская регистрация (MOD-шина): свойства моделей, рендерер пули, клавиша, HUD. */
@Mod.EventBusSubscriber(modid = FrontierMilitary.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    /** Тег-маркер «рисуем только руки» (ставится на временную копию стека в ClientForgeEvents). */
    public static final String TAG_FP_HANDS = "FpHands";

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.PULSE_SIDEARM.get(),
                new ResourceLocation(FrontierMilitary.MODID, "gun_state"),
                (stack, level, entity, seed) -> modelState(stack, level)));
    }

    /**
     * 0..8 — состояние самого пистолета (state_N), 100..105 — только руки (hands_N).
     * Значения соответствуют predicate в models/item/pulse_sidearm.json.
     */
    static float modelState(ItemStack stack, @Nullable ClientLevel level) {
        Level lv = level != null ? level : Minecraft.getInstance().level;
        if (lv == null) {
            return 0.0F;
        }
        int state = PistolData.gunState(stack, lv.getGameTime());
        if (stack.hasTag() && stack.getTag().getBoolean(TAG_FP_HANDS)) {
            return 100 + PistolData.handIndex(state);
        }
        return state;
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BULLET.get(), BulletRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeys.RELOAD);
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("pistol_ammo", PistolHud.OVERLAY);
    }

    private ClientSetup() {}
}
