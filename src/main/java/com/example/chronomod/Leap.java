package com.example.chronomod;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** 시간 도약: 바라보는 방향으로 순간이동. 지나간 경로의 적대 몹에게 피해를 준다. */
public final class Leap {
    public static final double MAX_DISTANCE = 14.0;
    public static final float DAMAGE = 8.0f;

    private Leap() {}

    public static boolean start(ServerPlayerEntity p) {
        ServerWorld w = (ServerWorld) p.getWorld();
        Vec3d start = p.getPos();
        Vec3d dir = p.getRotationVec(1.0F).normalize();

        Vec3d best = null;
        for (double d = 0.5; d <= MAX_DISTANCE; d += 0.5) {
            Vec3d cand = start.add(dir.multiply(d));
            Box box = p.getBoundingBox().offset(cand.subtract(start));
            if (!w.isSpaceEmpty(p, box)) break;
            if (w.getFluidState(BlockPos.ofFloored(cand)).isIn(FluidTags.LAVA)) break;
            best = cand;
        }
        if (best == null || best.distanceTo(start) < 2.0) {
            p.sendMessage(Text.translatable("msg.chronomod.blocked"), true);
            return false;
        }
        final Vec3d end = best;
        final Vec3d from = start.add(0, 1.0, 0);
        final Vec3d to = end.add(0, 1.0, 0);
        SummonedFx.leap(w, end);
        double len = from.distanceTo(to);

        // 출발점: 포탈 폭발 + 잔상
        ModelFx.burst(w, from, 14, 0.9, 8);
        Fx.ghost(w, start);
        Fx.sound(w, start, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.5f, 1.4f);

        // 경로: 번개 같은 선 + 순차적으로 켜지는 잔상
        Fx.line(w, ParticleTypes.ELECTRIC_SPARK, from, to, 0.35);
        Fx.line(w, ParticleTypes.END_ROD, from, to, 0.8);
        for (double t = 3; t < len; t += 3) {
            final Vec3d ghostPos = start.add(dir.multiply(t));
            Scheduler.later((int) (t / len * 5), () -> Fx.ghost(w, ghostPos));
        }

        p.networkHandler.requestTeleport(end.x, end.y, end.z, p.getYaw(), p.getPitch());
        p.setVelocity(Vec3d.ZERO);
        p.fallDistance = 0;

        // 도착점: 섬광 + 충격파
        ModelFx.impact(w, to, 1.0);
        Fx.sphere(w, ParticleTypes.END_ROD, to, 60, 0.6);
        Fx.shockwave(w, ParticleTypes.ELECTRIC_SPARK, end.add(0, 0.1, 0), 4, 4, 1, 40);
        Fx.sound(w, end, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.9f);
        Fx.sound(w, end, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 1.2f);

        // 경로 위 적대 몹 타격
        Box pathBox = new Box(from, to).expand(2.0);
        List<Entity> victims = w.getOtherEntities(p, pathBox, e -> e instanceof Monster && e.isAlive());
        for (Entity e : victims) {
            Vec3d mid = e.getPos().add(0, e.getHeight() / 2, 0);
            if (distToSegment(mid, from, to) <= 1.8 && e instanceof LivingEntity le) {
                le.damage(w, w.getDamageSources().playerAttack(p), DAMAGE);
                ModelFx.impact(w, mid, 0.85);
            }
        }
        return true;
    }

    private static double distToSegment(Vec3d p, Vec3d a, Vec3d b) {
        Vec3d ab = b.subtract(a);
        double l2 = ab.lengthSquared();
        if (l2 < 1.0E-6) return p.distanceTo(a);
        double t = Math.max(0, Math.min(1, p.subtract(a).dotProduct(ab) / l2));
        return p.distanceTo(a.add(ab.multiply(t)));
    }
}
