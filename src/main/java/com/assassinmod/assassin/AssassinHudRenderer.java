package com.assassinmod.assassin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

public final class AssassinHudRenderer {

    private AssassinHudRenderer() {
    }

    public static void render(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !AssassinAbilityManager.hasClass(player)) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int startX = screenWidth - 90;
        int startY = screenHeight - 42;

        renderAbility(guiGraphics, startX, startY, "Z", "Shadow Step", AssassinAbilityManager.TAG_SHADOW_STEP_CD, 0xFF6C63FF);
        renderAbility(guiGraphics, startX + 40, startY, "X", "Shadow Veil", AssassinAbilityManager.TAG_SHADOW_VEIL_CD, 0xFF00D1FF);
        renderAbility(guiGraphics, startX + 80, startY, "C", "Mirage", AssassinAbilityManager.TAG_MIRAGE_CD, 0xFFFFB84D);
    }

    private static void renderAbility(GuiGraphics guiGraphics, int x, int y, String key, String label, String cooldownTag, int color) {
        int boxWidth = 32;
        int boxHeight = 32;
        int cooldownTicks = getCooldownTicks(Minecraft.getInstance().player, cooldownTag);
        float cooldownRatio = cooldownTicks > 0 ? Math.min(1.0F, cooldownTicks / 200.0F) : 0.0F;

        guiGraphics.fill(x, y, x + boxWidth, y + boxHeight, 0xAA111111);
        guiGraphics.fill(x + 1, y + 1, x + boxWidth - 1, y + boxHeight - 1, 0xFF222222);
        guiGraphics.fill(x + 1, y + 1, x + boxWidth - 1, y + (int) (boxHeight * (1.0F - cooldownRatio)) - 1, 0x66000000 | (color & 0x00FFFFFF));

        guiGraphics.drawString(Minecraft.getInstance().font, key, x + 11, y + 10, 0xFFFFFFFF, false);

        if (cooldownTicks > 0) {
            guiGraphics.drawString(Minecraft.getInstance().font, String.valueOf((int) Math.ceil(cooldownTicks / 20.0F)), x + 8, y + 22, 0xFFFFFFFF, false);
        }

        if (AssassinAbilityManager.hasClass(Minecraft.getInstance().player)) {
            guiGraphics.renderOutline(x, y, boxWidth, boxHeight, color | 0xAA000000);
        }
    }

    private static int getCooldownTicks(Player player, String cooldownTag) {
        if (player == null || !player.getPersistentData().contains(cooldownTag)) {
            return 0;
        }
        return player.getPersistentData().getInt(cooldownTag);
    }
}
