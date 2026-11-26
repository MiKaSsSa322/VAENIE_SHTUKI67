package com.frontiermilitary.network;

import com.frontiermilitary.item.ServicePistolItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Клиент -> сервер: игрок нажал клавишу перезарядки. */
public class ReloadPacket {
    public ReloadPacket() {}

    public void encode(FriendlyByteBuf buf) {
        // без данных
    }

    public static ReloadPacket decode(FriendlyByteBuf buf) {
        return new ReloadPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();
            if (main.getItem() instanceof ServicePistolItem) {
                ServicePistolItem.startReload(player, main, player.level());
            } else if (off.getItem() instanceof ServicePistolItem) {
                ServicePistolItem.startReload(player, off, player.level());
            }
        });
        context.setPacketHandled(true);
    }
}
