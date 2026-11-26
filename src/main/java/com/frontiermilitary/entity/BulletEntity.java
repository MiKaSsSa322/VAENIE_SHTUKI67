package com.frontiermilitary.entity;

import com.frontiermilitary.FrontierMilitary;
import com.frontiermilitary.registry.ModEntities;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/** Пистолетная пуля: быстрый снаряд с проверкой столкновений по лучу за тик, лёгким падением и замедлением в воде. */
public class BulletEntity extends Projectile {
    public static final ResourceKey<DamageType> BULLET_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(FrontierMilitary.MODID, "bullet"));
    private static final int MAX_LIFE_TICKS = 30;

    private float damage = 6.0F;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
    }

    public BulletEntity(Level level, LivingEntity owner, float damage) {
        this(ModEntities.BULLET.get(), level);
        this.setOwner(owner);
        this.damage = damage;
        this.setPos(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
    }

    @Override
    protected void defineSynchedData() {
        // нечего синхронизировать
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > MAX_LIFE_TICKS) {
            this.discard();
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            if (this.level().isClientSide) {
                this.discard();
                return;
            }
            if (!ForgeEventFactory.onProjectileImpact(this, hit)) {
                this.onHit(hit);
                if (this.isRemoved()) {
                    return;
                }
            }
        }

        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
        this.updateRotation();

        Vec3 next = motion;
        if (this.isInWater()) {
            next = next.scale(0.6D);
        }
        this.setDeltaMovement(next.x, next.y - 0.004D, next.z);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) {
            return;
        }
        Entity target = result.getEntity();
        float dealt = this.damage;
        if (target instanceof LivingEntity living && result.getLocation().y >= living.getEyeY() - 0.2D) {
            dealt *= 1.5F; // попадание в голову
        }
        target.invulnerableTime = 0; // пули не должны гаситься кадрами неуязвимости предыдущей пули
        target.hurt(this.bulletSource(this.getOwner()), dealt);
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel server) {
            BlockState state = server.getBlockState(result.getBlockPos());
            Vec3 loc = result.getLocation();
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    loc.x, loc.y, loc.z, 10, 0.06D, 0.06D, 0.06D, 0.12D);
            server.sendParticles(ParticleTypes.CRIT, loc.x, loc.y, loc.z, 3, 0.04D, 0.04D, 0.04D, 0.25D);
            server.sendParticles(ParticleTypes.SMOKE, loc.x, loc.y, loc.z, 2, 0.03D, 0.03D, 0.03D, 0.01D);
            server.playSound(null, loc.x, loc.y, loc.z, state.getSoundType().getBreakSound(),
                    SoundSource.BLOCKS, 0.55F, 1.45F);
        }
        this.discard();
    }

    private DamageSource bulletSource(@Nullable Entity owner) {
        try {
            Holder<DamageType> type = this.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BULLET_DAMAGE);
            return new DamageSource(type, this, owner);
        } catch (RuntimeException e) {
            return this.damageSources().thrown(this, owner);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
