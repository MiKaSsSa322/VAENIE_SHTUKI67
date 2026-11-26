package com.frontiermilitary.item;

import com.frontiermilitary.config.FMConfig;
import com.frontiermilitary.entity.BulletEntity;
import com.frontiermilitary.registry.ModItems;
import com.frontiermilitary.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Служебный пистолет (регистрационное имя сохранено: frontiermilitary:pulse_sidearm).
 * ПКМ — выстрел, R — перезарядка. Патроны: frontiermilitary:pistol_ammo.
 */
public class ServicePistolItem extends Item {
    private static final float BULLET_SPEED = 5.0F;

    public ServicePistolItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        PistolData.setAmmo(stack, PistolData.MAG_SIZE);
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        long now = level.getGameTime();

        if (PistolData.isReloading(stack, now)) {
            return InteractionResultHolder.fail(stack);
        }

        int ammo = PistolData.getAmmo(stack);
        if (ammo <= 0) {
            if (!level.isClientSide) {
                if (hasReserve(player)) {
                    startReload(player, stack, level);
                } else {
                    player.displayClientMessage(Component.translatable("message.frontiermilitary.no_ammo"), true);
                    playAt(level, player, ModSounds.PISTOL_DRY.get(), 0.8F, 1.0F);
                    player.getCooldowns().addCooldown(this, 12);
                }
            }
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide) {
            // Предсказание на клиенте: мгновенная анимация и отдача в камеру, пока сервер не подтвердил.
            PistolData.setAmmo(stack, ammo - 1);
            stack.getOrCreateTag().putLong(PistolData.TAG_LAST_SHOT, now);
            float kick = player.isShiftKeyDown() ? 1.2F : 2.0F;
            player.setXRot(Math.max(-90.0F, player.getXRot() - kick));
            player.setYRot(player.getYRot() + (level.random.nextFloat() - 0.5F) * 0.8F);
            return InteractionResultHolder.consume(stack);
        }

        PistolData.setAmmo(stack, ammo - 1);
        stack.getOrCreateTag().putLong(PistolData.TAG_LAST_SHOT, now);
        fire((ServerLevel) level, player, hand);
        player.getCooldowns().addCooldown(this, PistolData.FIRE_COOLDOWN);
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        return InteractionResultHolder.consume(stack);
    }

    private void fire(ServerLevel level, Player player, InteractionHand hand) {
        Vec3 look = player.getLookAngle();
        float inaccuracy = player.isSprinting() ? 3.0F : (player.isShiftKeyDown() ? 0.3F : 0.9F);

        BulletEntity bullet = new BulletEntity(level, player, FMConfig.PISTOL_DAMAGE.get().floatValue());
        bullet.setPos(player.getEyePosition().add(look.scale(0.6D)).add(0.0D, -0.1D, 0.0D));
        bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, BULLET_SPEED, inaccuracy);
        level.addFreshEntity(bullet);

        // Эффекты у дульного среза (смещён вправо-вперёд от глаз).
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        double side = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT) ? 1.0D : -1.0D;
        Vec3 muzzle = player.getEyePosition().add(look.scale(0.95D)).add(right.scale(0.16D * side)).add(0.0D, -0.14D, 0.0D);
        level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 2, 0.01D, 0.01D, 0.01D, 0.01D);
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 4, 0.03D, 0.03D, 0.03D, 0.012D);
        level.sendParticles(ParticleTypes.POOF, muzzle.x, muzzle.y, muzzle.z, 1, 0.02D, 0.02D, 0.02D, 0.01D);

        playAt(level, player, ModSounds.PISTOL_SHOT.get(), 1.0F, 0.95F + level.random.nextFloat() * 0.1F);
    }

    // ------------------------------------------------------------------ перезарядка

    /** Запускает перезарядку (только на сервере). Возвращает true, если она началась. */
    public static boolean startReload(Player player, ItemStack stack, Level level) {
        if (level.isClientSide) {
            return false;
        }
        long now = level.getGameTime();
        if (PistolData.isReloading(stack, now) || PistolData.getAmmo(stack) >= PistolData.MAG_SIZE) {
            return false;
        }
        if (!hasReserve(player)) {
            player.displayClientMessage(Component.translatable("message.frontiermilitary.no_ammo"), true);
            return false;
        }
        stack.getOrCreateTag().putLong(PistolData.TAG_RELOAD_START, now);
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) {
            return;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(PistolData.TAG_RELOAD_START)) {
            return;
        }
        boolean held = entity instanceof LivingEntity living
                && (living.getMainHandItem() == stack || living.getOffhandItem() == stack);
        long elapsed = level.getGameTime() - tag.getLong(PistolData.TAG_RELOAD_START);
        if (!held || elapsed < 0) {
            tag.remove(PistolData.TAG_RELOAD_START); // убрали оружие из рук — перезарядка отменена
            return;
        }

        if (elapsed == 4) playAt(level, entity, ModSounds.MAG_OUT.get(), 0.8F, 1.0F);
        else if (elapsed == 22) playAt(level, entity, ModSounds.MAG_IN.get(), 0.8F, 1.0F);
        else if (elapsed == 30) playAt(level, entity, ModSounds.SLIDE_BACK.get(), 0.8F, 1.0F);
        else if (elapsed == 36) playAt(level, entity, ModSounds.SLIDE_FORWARD.get(), 0.9F, 1.0F);

        if (elapsed >= PistolData.RELOAD_TICKS) {
            if (entity instanceof Player player) {
                int need = PistolData.MAG_SIZE - PistolData.getAmmo(stack);
                int taken = player.getAbilities().instabuild ? need : consumeAmmo(player, need);
                PistolData.setAmmo(stack, PistolData.getAmmo(stack) + taken);
            }
            tag.remove(PistolData.TAG_RELOAD_START);
        }
    }

    public static boolean hasReserve(Player player) {
        return player.getAbilities().instabuild || countAmmo(player) > 0;
    }

    public static int countAmmo(Player player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ModItems.PISTOL_AMMO.get())) {
                total += s.getCount();
            }
        }
        return total;
    }

    private static int consumeAmmo(Player player, int wanted) {
        int taken = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && taken < wanted; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ModItems.PISTOL_AMMO.get())) {
                int n = Math.min(wanted - taken, s.getCount());
                s.shrink(n);
                taken += n;
            }
        }
        return taken;
    }

    private static void playAt(Level level, Entity at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    // ------------------------------------------------------------------ прочее

    /** Изменение NBT (патроны, анимация) не должно перезапускать анимацию доставания оружия. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.frontiermilitary.pistol.ammo",
                PistolData.getAmmo(stack), PistolData.MAG_SIZE).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.frontiermilitary.pistol.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Третье лицо: двуручный хват (поднятые вперёд руки), как при прицеливании. */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return HumanoidModel.ArmPose.CROSSBOW_HOLD;
            }
        });
    }
}
