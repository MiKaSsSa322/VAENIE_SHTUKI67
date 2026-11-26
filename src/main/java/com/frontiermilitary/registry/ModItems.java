package com.frontiermilitary.registry;

import com.frontiermilitary.FrontierMilitary;
import com.frontiermilitary.item.ServicePistolItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FrontierMilitary.MODID);

    /** Регистрационное имя pulse_sidearm сохранено ради совместимости с существующими мирами и ссылками. */
    public static final RegistryObject<Item> PULSE_SIDEARM = ITEMS.register("pulse_sidearm",
            () -> new ServicePistolItem(new Item.Properties().stacksTo(1).durability(256)));
    public static final RegistryObject<Item> PISTOL_AMMO = ITEMS.register("pistol_ammo",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FIELD_RATION = ITEMS.register("field_ration",
            () -> new Item(new Item.Properties().food(
                    new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(6).saturationMod(0.6F).build())));
    public static final RegistryObject<Item> REINFORCED_FIBER = ITEMS.register("reinforced_fiber",
            () -> new Item(new Item.Properties()));

    private ModItems() {}
}
