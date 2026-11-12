package com.assassinmod.assassin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class AssassinHudRenderer {
    private static final int ICON_SIZE = 36;
    private static final int GAP = 5;
    private static final int RIGHT_MARGIN = 8;
    private static final int BOTTOM_MARGIN = 8;
    private static final String[][] ICONS = {
            {
                    "......12....", ".....1221...", "....122.21..", "...122..21..",
                    "..122...21..", ".122....211.", ".12.....221.", "..12....221.",
                    "...12...221.", "....122221..", ".....1221...", "......21...."
            },
            {
                    "....2222....", "...233332...", "..23333332..", ".2333333332.",
                    ".2333333332.", ".2343333432.", ".2343333432.", "..23333332..",
                    "...233332...", "....2222....", "...22..22...", "..22....22.."
            },
            {
                    "...22..22...", "..23322332..", "..23322332..", "...22..22...",
                    ".22..22..22.", "2332.22.2332", "2332.22.2332", ".22..22..22.",
                    "..22....22..", ".22......22.", "22........22", "............"
            }
    };

    private AssassinHudRenderer() {}

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !AssassinClientState.isAssassin()) return;

        int totalWidth = ICON_SIZE * 3 + GAP * 2;
        int startX = minecraft.getWindow().getGuiScaledWidth() - RIGHT_MARGIN - totalWidth;
        int y = minecraft.getWindow().getGuiScaledHeight() - BOTTOM_MARGIN - ICON_SIZE;

        renderAbility(graphics, startX, y, 0,
                AssassinKeyMappings.SHADOW_STEP.getTranslatedKeyMessage().getString(),
                AssassinClientState.isSkillUnlocked(2) ? 80 : 120, 1);
        renderAbility(graphics, startX + ICON_SIZE + GAP, y, 1,
                AssassinKeyMappings.SHADOW_VEIL.getTranslatedKeyMessage().getString(), 240, 6);
        renderAbility(graphics, startX + (ICON_SIZE + GAP) * 2, y, 2,
                AssassinKeyMappings.MIRAGE.getTranslatedKeyMessage().getString(), 300, 9);
    }

    private static void renderAbility(GuiGraphics graphics, int x, int y,
                                      int iconIndex, String key, int cooldownLength, int skillIndex) {
        drawFrame(graphics, x, y);
        drawPixelIcon(graphics, x + 6, y + 6, ICONS[iconIndex]);

        boolean unlocked = AssassinClientState.isSkillUnlocked(skillIndex);
        int cooldown = unlocked ? AssassinClientState.getCooldown(iconIndex) : 0;
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

    private static void drawPixelIcon(GuiGraphics graphics, int x, int y, String[] pixels) {
        int scale = 2;
        for (int row = 0; row < pixels.length; row++) {
            String line = pixels[row];
            for (int column = 0; column < line.length(); column++) {
                int color = switch (line.charAt(column)) {
                    case '1' -> 0xFF6A27B8;
                    case '2' -> 0xFFB34CFF;
                    case '3' -> 0xFFE6B9FF;
                    default -> 0;
                };
                if (color != 0) {
                    graphics.fill(x + column * scale, y + row * scale,
                            x + (column + 1) * scale, y + (row + 1) * scale, color);
                }
            }
        }
    }
}
