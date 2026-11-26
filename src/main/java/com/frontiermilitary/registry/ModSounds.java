package com.frontiermilitary.registry;

import com.frontiermilitary.FrontierMilitary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FrontierMilitary.MODID);

    public static final RegistryObject<SoundEvent> PISTOL_SHOT = reg("item.pistol.shot");
    public static final RegistryObject<SoundEvent> PISTOL_DRY = reg("item.pistol.dry");
    public static final RegistryObject<SoundEvent> MAG_OUT = reg("item.pistol.mag_out");
    public static final RegistryObject<SoundEvent> MAG_IN = reg("item.pistol.mag_in");
    public static final RegistryObject<SoundEvent> SLIDE_BACK = reg("item.pistol.slide_back");
    public static final RegistryObject<SoundEvent> SLIDE_FORWARD = reg("item.pistol.slide_forward");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(FrontierMilitary.MODID, name)));
    }

    private ModSounds() {}
}
