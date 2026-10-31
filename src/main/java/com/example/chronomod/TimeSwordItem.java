package com.example.chronomod;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** 시간의 검: 기본 피해 3 + 추가 피해 6으로 총 9의 피해를 준다. */
public class TimeSwordItem extends SwordItem {
    private static final float BONUS_DAMAGE_PER_PULSE = 2.0f;
    private static final int[] BONUS_DAMAGE_DELAYS = {20, 40, 60};
    private static final ThreadLocal<Boolean> APPLYING_BONUS =
            ThreadLocal.withInitial(() -> false);

    public TimeSwordItem(Settings settings) {
        // 플레이어 기본 공격력 1 + 다이아 검 공격력 3 - 보정 1 = 기본 피해 3
        super(ToolMaterial.DIAMOND, -1.0f, -2.4f, settings);
    }

    /** 시간 정지 피해 가로채기에서 추가 피해를 통과시키기 위한 표시. */
    public static boolean isApplyingBonusDamage() {
        return APPLYING_BONUS.get();
    }

    private static void applyBonus(LivingEntity target, LivingEntity attacker,
                                   ServerWorld world, float amount) {
        if (!target.isAlive() || target.getWorld() != world) return;

        // 기본 타격 직후에도 추가 피해가 무적 시간에 막히지 않도록 초기화한다.
        target.timeUntilRegen = 0;
        APPLYING_BONUS.set(true);
        try {
            if (attacker instanceof PlayerEntity player) {
                target.damage(world, world.getDamageSources().playerAttack(player), amount);
            } else {
                target.damage(world, world.getDamageSources().mobAttack(attacker), amount);
            }
        } finally {
            APPLYING_BONUS.set(false);
        }
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld() instanceof ServerWorld world && target.isAlive()) {
            if (TimeStop.isTargetFrozen(target)) {
                // 시간 정지 중에는 기본 피해 3에 추가 피해 6을 더해 즉시 총 9 피해.
                applyBonus(target, attacker, world,
                        BONUS_DAMAGE_PER_PULSE * BONUS_DAMAGE_DELAYS.length);
                AbilityClockFx.redImpact(world,
                        target.getPos().add(0, target.getHeight(), 0), 12, true);
            } else {
                // 평상시에는 기본 피해 3 + 1초 간격 추가 피해 2씩 세 번 = 총 9 피해.
                for (int delay : BONUS_DAMAGE_DELAYS) {
                    Scheduler.later(delay, () -> {
                        applyBonus(target, attacker, world, BONUS_DAMAGE_PER_PULSE);
                        if (target.isAlive()) {
                            AbilityClockFx.redImpact(world,
                                    target.getPos().add(0, target.getHeight(), 0), 8, false);
                        }
                    });
                }
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("tooltip.chronomod.time_sword.ability")
                .formatted(Formatting.LIGHT_PURPLE));
    }
}
