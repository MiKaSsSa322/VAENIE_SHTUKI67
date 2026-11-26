package com.frontiermilitary.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Общие (клиент+сервер) константы и чтение/запись состояния пистолета из NBT предмета. */
public final class PistolData {
    public static final int MAG_SIZE = 12;
    public static final int RELOAD_TICKS = 46;
    public static final int FIRE_COOLDOWN = 6;

    public static final String TAG_AMMO = "Ammo";
    public static final String TAG_LAST_SHOT = "LastShot";
    public static final String TAG_RELOAD_START = "ReloadStart";

    private PistolData() {}

    /** Предмет без тега (например, выданный командой) считается заряженным. */
    public static int getAmmo(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_AMMO)) {
            return MAG_SIZE;
        }
        return Math.max(0, Math.min(MAG_SIZE, tag.getInt(TAG_AMMO)));
    }

    public static void setAmmo(ItemStack stack, int ammo) {
        stack.getOrCreateTag().putInt(TAG_AMMO, Math.max(0, Math.min(MAG_SIZE, ammo)));
    }

    public static boolean isReloading(ItemStack stack, long now) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_RELOAD_START)) {
            return false;
        }
        long elapsed = now - tag.getLong(TAG_RELOAD_START);
        return elapsed < RELOAD_TICKS;
    }

    /** Сколько тиков прошло с начала перезарядки, или -1 если не перезаряжается. */
    public static long reloadElapsed(ItemStack stack, long now) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_RELOAD_START)) {
            return -1;
        }
        return Math.max(0, now - tag.getLong(TAG_RELOAD_START));
    }

    /** Сколько тиков прошло с последнего выстрела, или -1. */
    public static long sinceShot(ItemStack stack, long now) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_LAST_SHOT)) {
            return -1;
        }
        return Math.max(0, now - tag.getLong(TAG_LAST_SHOT));
    }

    /**
     * Номер состояния 3D-модели (соответствует файлам models/item/pistol/state_N.json):
     * 0 покой, 1-2 отдача, 3 затвор на задержке (пусто), 4-6 смена магазина, 7-8 передёргивание затвора.
     */
    public static int gunState(ItemStack stack, long now) {
        long reload = reloadElapsed(stack, now);
        if (reload >= 0 && reload < RELOAD_TICKS) {
            if (reload < 4) return getAmmo(stack) <= 0 ? 3 : 0;
            if (reload < 7) return 4;
            if (reload < 19) return 5;
            if (reload < 23) return 6;
            if (reload < 30) return 0;
            if (reload < 35) return 7;
            if (reload < 40) return 8;
            return 0;
        }
        long shot = sinceShot(stack, now);
        if (shot == 0) return 1;
        if (shot == 1) return 2;
        return getAmmo(stack) <= 0 ? 3 : 0;
    }

    /** Поза рук для данного состояния модели (hands_N.json). */
    public static int handIndex(int gunState) {
        switch (gunState) {
            case 4: return 1;
            case 5: return 2;
            case 6: return 3;
            case 7: return 4;
            case 8: return 5;
            default: return 0;
        }
    }
}
