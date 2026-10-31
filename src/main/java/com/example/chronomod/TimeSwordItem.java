package com.example.chronomod;

import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * 시간의 검: 전투 탭에 유지되는 도구형 아이템.
 * 기본 공격력 보정으로 공격 피해를 0으로 만들며, 추가 피해 효과는 없다.
 */
public class TimeSwordItem extends SwordItem {
    public TimeSwordItem(Settings settings) {
        // 플레이어 기본 공격력 1 + 다이아 검 공격력 3 - 4 = 0
        super(ToolMaterial.DIAMOND, -4.0f, -2.4f, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.literal("공격 피해를 주지 않는 도구형 아이템입니다.")
                .formatted(Formatting.GRAY));
    }
}
