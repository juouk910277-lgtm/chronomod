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

/** 시간 정지 중에도 기본 타격 피해는 즉시 적용되며, 추가 피해는 3초에 걸쳐 적용되는 시간의 검. */
public class TimeSwordItem extends SwordItem {
    private static final float BONUS_DAMAGE_PER_PULSE = 3.0f;
    private static final int[] BONUS_DAMAGE_DELAYS = {0, 20, 40};

    public TimeSwordItem(Settings settings) {
        // 다이아몬드 검과 동일한 기본 공격 수치: 피해량 7, 공격 속도 1.6.
        super(ToolMaterial.DIAMOND, 3.0f, -2.4f, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld() instanceof ServerWorld world && target.isAlive()) {
            // 추가 피해 총 9를 1초 간격으로 3회 적용한다.
            // 시간 정지 중 시간의 검 타격은 TimeStop.onDamage에서 통과하므로 기본 타격은 즉시 들어간다.
            for (int delay : BONUS_DAMAGE_DELAYS) {
                Scheduler.later(delay, () -> {
                    if (!target.isAlive() || target.getWorld() != world) return;
                    if (attacker instanceof PlayerEntity player) {
                        target.damage(world, world.getDamageSources().playerAttack(player), BONUS_DAMAGE_PER_PULSE);
                    } else {
                        target.damage(world, world.getDamageSources().mobAttack(attacker), BONUS_DAMAGE_PER_PULSE);
                    }
                    ModelFx.impact(world, target.getPos().add(0, target.getHeight() * 0.55, 0), 0.55);
                });
            }
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("tooltip.chronomod.time_sword.ability")
                .formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.translatable("tooltip.chronomod.time_sword.instant")
                .formatted(Formatting.GRAY));
    }
}
