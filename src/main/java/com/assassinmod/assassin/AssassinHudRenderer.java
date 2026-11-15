package com.assassinmod.assassin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class AssassinHudRenderer {
    private static final int ICON_SIZE = 18;
    private static final int GAP = 3;
    private static final int HEALTH_HUD_OFFSET = 78;
    private static final int ABILITY_COUNT = 9;
    private static final ResourceLocation ICON_SHADOW_STEP = abilityTexture("shadow_step");
    private static final ResourceLocation ICON_SHADOW_TWIN = abilityTexture("shadow_twin");
    private static final ResourceLocation ICON_SHADOW_VEIL = abilityTexture("shadow_veil");
    private static final ResourceLocation ICON_MIRAGE = abilityTexture("mirage");
    private static final ResourceLocation ICON_RAPID_ASSAULT = ResourceLocation.fromNamespaceAndPath(
            AssassinMod.MOD_ID, "textures/passive/icons/sword_gold.png");
    private static final ResourceLocation ICON_SPRINT = ResourceLocation.fromNamespaceAndPath(
            AssassinMod.MOD_ID, "textures/passive/icons/boots_gold.png");
    private static final ResourceLocation ICON_BACKSTEP = ResourceLocation.fromNamespaceAndPath(
            AssassinMod.MOD_ID, "textures/passive/icons/arrow_diamond.png");
    private static final ResourceLocation ICON_PERFECT_DODGE = abilityTexture("shadow_veil");
    private static final ResourceLocation ICON_SHADOW_DANCE = abilityTexture("mirage");

    private AssassinHudRenderer() {}

    private static ResourceLocation abilityTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(AssassinMod.MOD_ID, "textures/gui/abilities/" + name + ".png");
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !AssassinClientState.isAssassin()) return;

        int totalWidth = ICON_SIZE * ABILITY_COUNT + GAP * (ABILITY_COUNT - 1);
        int startX = (minecraft.getWindow().getGuiScaledWidth() - totalWidth) / 2;
        int y = minecraft.getWindow().getGuiScaledHeight() - HEALTH_HUD_OFFSET;

        renderAbility(graphics, startX, y, 0, ICON_SHADOW_STEP, 9, 800);
        renderAbility(graphics, startX + ICON_SIZE + GAP, y, 1, ICON_SHADOW_TWIN, 6, 800);
        renderAbility(graphics, startX + (ICON_SIZE + GAP) * 2, y, 2, ICON_SHADOW_VEIL, 4, 560);
        renderAbility(graphics, startX + (ICON_SIZE + GAP) * 3, y, 3, ICON_MIRAGE, 10, 1200);
        renderAbility(graphics, startX + (ICON_SIZE + GAP) * 4, y, 4, ICON_RAPID_ASSAULT, 12, 1200);
        renderAbility(graphics, startX + (ICON_SIZE + GAP) * 5, y, 5, ICON_SPRINT, 5, 1140);
        renderAbility(graphics, startX + (ICON_SIZE + GAP) * 6, y, 6, ICON_BACKSTEP, 11, 1900);
        renderAbility(graphics, startX + (ICON_SIZE + GAP) * 7, y, 7, ICON_PERFECT_DODGE, 14, 1200);
        renderAbility(graphics, startX + (ICON_SIZE + GAP) * 8, y, 8, ICON_SHADOW_DANCE, 15, 1800);
    }

    private static void renderAbility(GuiGraphics graphics, int x, int y,
                                      int cooldownIndex, ResourceLocation icon,
                                      int skillIndex, int cooldownLength) {
        drawFrame(graphics, x, y);
        graphics.blit(icon, x + 2, y + 2, 14, 14, 0, 0, 32, 32, 32, 32);

        boolean unlocked = AssassinClientState.isSkillUnlocked(skillIndex);
        int cooldown = unlocked ? AssassinClientState.getCooldown(cooldownIndex) : 0;
        if (cooldown > 0) {
            int coveredHeight = Math.min(14, (int) Math.ceil(14.0D * cooldown / cooldownLength));
            graphics.fill(x + 2, y + 2, x + ICON_SIZE - 2, y + 2 + coveredHeight, 0xA9000611);

            String seconds = Integer.toString((int) Math.ceil(cooldown / 20.0D));
            graphics.drawCenteredString(Minecraft.getInstance().font, seconds,
                    x + ICON_SIZE / 2, y + 5, 0xFFFFFFFF);
        } else if (!unlocked) {
            graphics.fill(x + 2, y + 2, x + ICON_SIZE - 2, y + ICON_SIZE - 2, 0xB9000611);
        }
    }

    private static void drawFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 1, y + 2, x + ICON_SIZE + 1, y + ICON_SIZE + 2, 0x99000000);
        graphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0xFF110D19);
        graphics.fill(x + 1, y + 1, x + ICON_SIZE - 1, y + ICON_SIZE - 1, 0xFFDCC8F6);
        graphics.fill(x + 2, y + 2, x + ICON_SIZE - 2, y + ICON_SIZE - 2, 0xFF24172F);
    }
}
