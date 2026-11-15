package com.assassinmod.assassin;

/** Small client HUD state synchronized from the authoritative server player data. */
public final class AssassinClientState {
    private static boolean assassin;
    private static int shadowStepCooldown;
    private static int shadowTwinCooldown;
    private static int shadowVeilCooldown;
    private static int mirageCooldown;
    private static int rapidAssaultCooldown;
    private static int sprintCooldown;
    private static int backstepCooldown;
    private static int perfectDodgeCooldown;
    private static int shadowDanceCooldown;
    private static int skillPoints;
    private static int unlockedSkillsMask;
    private static int[] skillRanks = new int[AssassinProgression.NODES.size()];

    private AssassinClientState() {}

    public static void update(boolean isAssassin, int shadowStep, int shadowTwin, int shadowVeil, int mirage,
                              int rapidAssault, int sprint, int backstep, int perfectDodge,
                              int shadowDance,
                              int points, int unlockedMask, long packedRanks) {
        assassin = isAssassin;
        shadowStepCooldown = Math.max(0, shadowStep);
        shadowTwinCooldown = Math.max(0, shadowTwin);
        shadowVeilCooldown = Math.max(0, shadowVeil);
        mirageCooldown = Math.max(0, mirage);
        rapidAssaultCooldown = Math.max(0, rapidAssault);
        sprintCooldown = Math.max(0, sprint);
        backstepCooldown = Math.max(0, backstep);
        perfectDodgeCooldown = Math.max(0, perfectDodge);
        shadowDanceCooldown = Math.max(0, shadowDance);
        skillPoints = Math.max(0, points);
        unlockedSkillsMask = unlockedMask;
        skillRanks = AssassinProgression.unpackRanks(packedRanks);
    }

    public static boolean isAssassin() {
        return assassin;
    }

    public static int getCooldown(int index) {
        return switch (index) {
            case 0 -> shadowStepCooldown;
            case 1 -> shadowTwinCooldown;
            case 2 -> shadowVeilCooldown;
            case 3 -> mirageCooldown;
            case 4 -> rapidAssaultCooldown;
            case 5 -> sprintCooldown;
            case 6 -> backstepCooldown;
            case 7 -> perfectDodgeCooldown;
            case 8 -> shadowDanceCooldown;
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
        return index >= 0 && index < AssassinProgression.NODES.size()
                && (unlockedSkillsMask & (1 << index)) != 0;
    }

    public static int getSkillRank(int index) {
        return index >= 0 && index < skillRanks.length ? skillRanks[index] : 0;
    }

    public static boolean hasEarnedMilestone(int index) {
        int bit = AssassinProgression.NODES.size() + index;
        return index >= 0 && index < AssassinProgression.MILESTONES.size()
                && (unlockedSkillsMask & (1 << bit)) != 0;
    }
}
