package com.frontiermilitary.client;

import com.frontiermilitary.item.PistolData;
import com.frontiermilitary.item.ServicePistolItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Счётчик патронов в правом нижнем углу, пока пистолет в руках. */
public final class PistolHud {
    public static final IGuiOverlay OVERLAY = PistolHud::render;

    private static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || mc.level == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem().getItem() instanceof ServicePistolItem
                ? player.getMainHandItem()
                : (player.getOffhandItem().getItem() instanceof ServicePistolItem ? player.getOffhandItem() : ItemStack.EMPTY);
        if (stack.isEmpty()) {
            return;
        }

        Font font = mc.font;
        int ammo = PistolData.getAmmo(stack);
        int reserve = player.getAbilities().instabuild ? -1 : ServicePistolItem.countAmmo(player);
        long reload = PistolData.reloadElapsed(stack, mc.level.getGameTime());
        boolean reloading = reload >= 0 && reload < PistolData.RELOAD_TICKS;

        int right = width - 12;
        int y = height - 46;

        String main = String.valueOf(ammo);
        String sub = " / " + (reserve < 0 ? "\u221E" : String.valueOf(reserve));
        int mainColor = ammo <= 3 ? 0xFFE04030 : 0xFFF2E6B8;

        int subW = font.width(sub);
        g.drawString(font, sub, right - subW, y + 8, 0xFFA8A8A0, true);
        g.pose().pushPose();
        g.pose().translate(right - subW - font.width(main) * 2, y, 0);
        g.pose().scale(2.0F, 2.0F, 1.0F);
        g.drawString(font, main, 0, 0, mainColor, true);
        g.pose().popPose();

        // магазин в виде столбиков
        int bx = right - 12 * 5;
        int by = y + 22;
        for (int i = 0; i < PistolData.MAG_SIZE; i++) {
            int color = i < ammo ? 0xFFD8B04A : 0x55303030;
            g.fill(bx + i * 5, by, bx + i * 5 + 3, by + 6, color);
        }

        if (reloading) {
            float p = Math.min(1.0F, reload / (float) PistolData.RELOAD_TICKS);
            Component text = Component.translatable("hud.frontiermilitary.reloading");
            g.drawString(font, text, right - font.width(text), by + 10, 0xFFE0E0D0, true);
            g.fill(bx, by + 21, bx + 12 * 5 - 2, by + 23, 0x66000000);
            g.fill(bx, by + 21, bx + (int) ((12 * 5 - 2) * p), by + 23, 0xFFD8B04A);
        }
    }

    private PistolHud() {}
}
