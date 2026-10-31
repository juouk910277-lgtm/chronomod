package com.example.chronomod;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** 시간 정지 중에도 즉시 피해를 누적시키는 시간의 검. */
public class TimeSwordItem extends SwordItem {
    private static final float BONUS_DAMAGE = 9.0f;

    public TimeSwordItem(Settings settings) {
        // 다이아몬드 검과 동일한 기본 공격 수치: 피해량 7, 공격 속도 1.6.
        super(ToolMaterials.DIAMOND, 3.0f, -2.4f, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld() instanceof ServerWorld world && target.isAlive()) {
            // 기존 3초 분할 피해(3 + 3 + 3)를 한 번의 즉발 추가 피해로 변경.
            // 시간 정지 중에도 TimeStop.onDamage가 시간의 검 타격을 통과시킨다.
            if (attacker instanceof PlayerEntity player) {
                target.damage(world, world.getDamageSources().playerAttack(player), BONUS_DAMAGE);
            } else {
                target.damage(world, world.getDamageSources().mobAttack(attacker), BONUS_DAMAGE);
            }
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0, false, true, true));
            ModelFx.impact(world, target.getPos().add(0, target.getHeight() * 0.55, 0), 0.85);
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
