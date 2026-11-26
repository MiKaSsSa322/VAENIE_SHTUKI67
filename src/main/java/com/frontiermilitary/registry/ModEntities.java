package com.frontiermilitary.registry;

import com.frontiermilitary.FrontierMilitary;
import com.frontiermilitary.entity.BulletEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FrontierMilitary.MODID);

    public static final RegistryObject<EntityType<BulletEntity>> BULLET = ENTITIES.register("bullet",
            () -> EntityType.Builder.<BulletEntity>of(BulletEntity::new, MobCategory.MISC)
                    .sized(0.12F, 0.12F)
                    .clientTrackingRange(8)
                    .updateInterval(5)
                    .noSave()
                    .build(FrontierMilitary.MODID + ":bullet"));

    private ModEntities() {}
}
