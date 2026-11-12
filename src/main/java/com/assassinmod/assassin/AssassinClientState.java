package com.assassinmod.assassin;

/** Small client HUD state synchronized from the authoritative server player data. */
public final class AssassinClientState {
    private static boolean assassin;
    private static int shadowStepCooldown;
    private static int shadowVeilCooldown;
    private static int mirageCooldown;
    private static int skillPoints;
    private static int unlockedSkillsMask;

    private AssassinClientState() {}

    public static void update(boolean isAssassin, int shadowStep, int shadowVeil, int mirage,
                              int points, int unlockedMask) {
        assassin = isAssassin;
        shadowStepCooldown = Math.max(0, shadowStep);
        shadowVeilCooldown = Math.max(0, shadowVeil);
        mirageCooldown = Math.max(0, mirage);
        skillPoints = Math.max(0, points);
        unlockedSkillsMask = unlockedMask;
    }

    public static boolean isAssassin() {
        return assassin;
    }

    public static int getCooldown(int index) {
        return switch (index) {
            case 0 -> shadowStepCooldown;
            case 1 -> shadowVeilCooldown;
            case 2 -> mirageCooldown;
            default -> 0;
        };
    }

    public static int getSkillPoints() {
        return skillPoints;
    }

    public static int getUnlockedSkillsMask() {
        return unlockedSkillsMask;
    }

    public static boolean isSkillUnlocked(int index) {
        return index >= 0 && index < 14 && (unlockedSkillsMask & (1 << index)) != 0;
    }

    public static boolean hasEarnedMilestone(int index) {
        int bit = 14 + index;
        return index >= 0 && index < 19 && (unlockedSkillsMask & (1 << bit)) != 0;
    }
}
