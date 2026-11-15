package com.assassinmod.assassin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class AssassinHudRenderer {
    private static final int ICON_SIZE = 32;
    private static final int GAP = 4;
    private static final int RIGHT_MARGIN = 8;
    private static final int BOTTOM_MARGIN = 8;
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

    private AssassinHudRenderer() {}

    private static ResourceLocation abilityTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(AssassinMod.MOD_ID, "textures/gui/abilities/" + name + ".png");
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !AssassinClientState.isAssassin()) return;

        int totalWidth = ICON_SIZE * 8 + GAP * 7;
        int startX = minecraft.getWindow().getGuiScaledWidth() - RIGHT_MARGIN - totalWidth;
        int y = minecraft.getWindow().getGuiScaledHeight() - BOTTOM_MARGIN - ICON_SIZE;

        // Slots left-to-right follow the active abilities in the skill tree.
        renderAbility(graphics, startX, y, 0, ICON_SHADOW_STEP,
                AssassinKeyMappings.SHADOW_STEP.getTranslatedKeyMessage().getString(),
                800, 9);
        renderAbility(graphics, startX + (ICON_SIZE + GAP), y, 1, ICON_SHADOW_TWIN,
                AssassinKeyMappings.SHADOW_TWIN.getTranslatedKeyMessage().getString(), 800, 6);
        renderAbility(graphics, startX + (ICON_SIZE + GAP) * 2, y, 2, ICON_SHADOW_VEIL,
                AssassinKeyMappings.SHADOW_VEIL.getTranslatedKeyMessage().getString(), 560, 4);
        renderAbility(graphics, startX + (ICON_SIZE + GAP) * 3, y, 3, ICON_MIRAGE,
                AssassinKeyMappings.MIRAGE.getTranslatedKeyMessage().getString(),
                1200, 10);
        renderAbility(graphics, startX + (ICON_SIZE + GAP) * 4, y, 4, ICON_RAPID_ASSAULT,
                AssassinKeyMappings.RAPID_ASSAULT.getTranslatedKeyMessage().getString(), 1200, 12, 16);
        renderAbility(graphics, startX + (ICON_SIZE + GAP) * 5, y, 5, ICON_SPRINT,
                AssassinKeyMappings.SPRINT.getTranslatedKeyMessage().getString(), 1140, 5);
        renderAbility(graphics, startX + (ICON_SIZE + GAP) * 6, y, 6, ICON_BACKSTEP,
                AssassinKeyMappings.BACKSTEP.getTranslatedKeyMessage().getString(), 1900, 11);
        renderAbility(graphics, startX + (ICON_SIZE + GAP) * 7, y, 7, ICON_PERFECT_DODGE,
                AssassinKeyMappings.PERFECT_DODGE.getTranslatedKeyMessage().getString(), 1200, 14);
    }

    private static void renderAbility(GuiGraphics graphics, int x, int y,
                                      int cooldownIndex, ResourceLocation icon, String key,
                                      int cooldownLength, int skillIndex) {
        renderAbility(graphics, x, y, cooldownIndex, icon, key, cooldownLength, skillIndex, 32);
    }

    private static void renderAbility(GuiGraphics graphics, int x, int y,
                                      int cooldownIndex, ResourceLocation icon, String key,
                                      int cooldownLength, int skillIndex, int iconSourceSize) {
        drawFrame(graphics, x, y);
        graphics.blit(icon, x + 5, y + 5, 26, 26,
                0, 0, iconSourceSize, iconSourceSize, iconSourceSize, iconSourceSize);

        boolean unlocked = AssassinClientState.isSkillUnlocked(skillIndex);
        int cooldown = unlocked ? AssassinClientState.getCooldown(cooldownIndex) : 0;
        if (cooldown > 0) {
            int coveredHeight = Math.min(28, (int) Math.ceil(28.0D * cooldown / cooldownLength));
            graphics.fill(x + 4, y + 4, x + 32, y + 4 + coveredHeight, 0xA9000611);

            String seconds = Integer.toString((int) Math.ceil(cooldown / 20.0D));
            int textX = x + (ICON_SIZE - Minecraft.getInstance().font.width(seconds)) / 2;
            graphics.drawString(Minecraft.getInstance().font, seconds, textX + 1, y + 13, 0xFF10091A, false);
            graphics.drawString(Minecraft.getInstance().font, seconds, textX, y + 12, 0xFFFFFFFF, false);
        } else if (!unlocked) {
            graphics.fill(x + 4, y + 4, x + 32, y + 32, 0xB9000611);
            graphics.drawCenteredString(Minecraft.getInstance().font, "LOCK", x + ICON_SIZE / 2, y + 13, 0xFFE3D4F0);
        }

        drawKeyBadge(graphics, x, y, key);
    }

    private static void drawKeyBadge(GuiGraphics graphics, int x, int y, String assignedKey) {
        var font = Minecraft.getInstance().font;
        String label = assignedKey.toUpperCase(java.util.Locale.ROOT);
        while (font.width(label) > 18 && label.length() > 2) {
            label = label.substring(0, label.length() - 1);
        }
        int badgeWidth = Math.max(12, font.width(label) + 4);
        int badgeX = x + ICON_SIZE - badgeWidth - 3;
        int badgeY = y + ICON_SIZE - 14;
        graphics.fill(badgeX - 1, badgeY - 1, badgeX + badgeWidth + 1, badgeY + 11, 0xFFB887E8);
        graphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + 10, 0xFF160F24);
        graphics.drawString(font, label, badgeX + (badgeWidth - font.width(label)) / 2,
                badgeY + 1, 0xFFFFFFFF, true);
    }

    private static void drawFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 2, y + 3, x + ICON_SIZE + 2, y + ICON_SIZE + 3, 0x99000000);
        graphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0xFF110D19);
        graphics.fill(x + 2, y + 2, x + ICON_SIZE - 2, y + ICON_SIZE - 2, 0xFFDCC8F6);
        graphics.fill(x + 4, y + 4, x + ICON_SIZE - 4, y + ICON_SIZE - 4, 0xFF24172F);

        graphics.fill(x + 2, y + 2, x + ICON_SIZE - 2, y + 3, 0xFFFFF0FF);
        graphics.fill(x + 2, y + 2, x + 3, y + ICON_SIZE - 2, 0xFFFFF0FF);
        graphics.fill(x + ICON_SIZE - 3, y + 3, x + ICON_SIZE - 2, y + ICON_SIZE - 2, 0xFF80649E);
        graphics.fill(x + 3, y + ICON_SIZE - 3, x + ICON_SIZE - 2, y + ICON_SIZE - 2, 0xFF80649E);
    }
}
