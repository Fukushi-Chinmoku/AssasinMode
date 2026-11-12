package com.assassinmod.assassin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class AssassinProgression {
    public record SkillNode(int index, String id, String name, String branch, String description,
                            int cost, int prerequisite) {}
    public record Milestone(String advancement, String title, int points) {}

    public static final List<SkillNode> NODES = List.of(
            new SkillNode(0, "swift_step", "Лёгкий шаг", "Ловкость", "Скорость передвижения ассасина повышается на 8%.", 1, -1),
            new SkillNode(1, "shadow_step", "Теневой шаг", "Ловкость", "Открывает телепортацию за спину цели.", 1, 0),
            new SkillNode(2, "far_step", "Дальняя тень", "Ловкость", "Перезарядка Теневого шага сокращается на 2 секунды.", 2, 1),
            new SkillNode(3, "echo_step", "Повторный шаг", "Ловкость", "Перезарядка Теневого шага сокращается до 3 секунд.", 2, 2),
            new SkillNode(4, "backstab", "Удар в спину", "Смертоносность", "Удар со спины наносит двойной урон.", 1, 0),
            new SkillNode(5, "executioner", "Палач", "Смертоносность", "Удар в спину наносит в 2,5 раза больше урона.", 2, 4),
            new SkillNode(6, "twin_rhythm", "Ритм клинков", "Смертоносность", "Парные клинки наносят на 15% больше урона.", 2, 5),
            new SkillNode(7, "shadow_veil", "Покров тени", "Тень", "Открывает невидимость и усиливающий удар из тени.", 1, 0),
            new SkillNode(8, "deep_veil", "Глубокая тень", "Тень", "Невидимость длится на 4 секунды дольше.", 2, 7),
            new SkillNode(9, "veil_strike", "Удар из тени", "Тень", "Первый удар из невидимости получает усиленный бонус урона.", 2, 8),
            new SkillNode(10, "mirage", "Мираж", "Обман", "Открывает рой приманок и туман у земли.", 1, 9),
            new SkillNode(11, "false_horde", "Ложная толпа", "Обман", "Мираж создаёт ещё 20 приманок.", 2, 10),
            new SkillNode(12, "lingering_mist", "Дымовая завеса", "Обман", "Мираж и ослепление длятся на 5 секунд дольше.", 2, 11),
            new SkillNode(13, "phantom_lunge", "Призрачный рывок", "Обман", "Мираж может настигнуть цель на расстоянии до 45 блоков.", 2, 12)
    );

    public static final List<Milestone> MILESTONES = List.of(
            new Milestone("minecraft:adventure/kill_a_mob", "Охотник на монстров", 1),
            new Milestone("minecraft:story/mine_diamond", "Алмазы!", 1),
            new Milestone("minecraft:nether/obtain_blaze_rod", "В огонь", 1),
            new Milestone("minecraft:nether/loot_bastion", "Былые времена", 1),
            new Milestone("minecraft:nether/obtain_ancient_debris", "Сокрытое в глубинах", 2),
            new Milestone("minecraft:adventure/adventuring_time", "Путешественник", 2),
            new Milestone("minecraft:adventure/kill_all_mobs", "Истребитель монстров", 3),
            new Milestone("minecraft:end/kill_dragon", "Освободить Край", 3),
            new Milestone("minecraft:nether/create_beacon", "Высоты иссушителя", 2)
    );

    private static final String POINTS_TAG = "assassin_skill_points";
    private static final String NODE_TAG_PREFIX = "assassin_skill_";
    private static final String MILESTONE_TAG_PREFIX = "assassin_milestone_";
    // Three semicircular branches share a start. Shadow and deception skills
    // continue along the outer arc, matching the third branch in the UI.
    public static final int[][] CONNECTIONS = {
            {0, 1}, {1, 2}, {2, 3},
            {0, 4}, {4, 5}, {5, 6},
            {0, 7}, {7, 8}, {8, 9}, {9, 10}, {10, 11}, {11, 12}, {12, 13}
    };
    private static final Map<String, Milestone> MILESTONE_BY_ID = MILESTONES.stream()
            .collect(Collectors.toUnmodifiableMap(Milestone::advancement, milestone -> milestone));

    private AssassinProgression() {}

    public static int getPoints(Player player) {
        return player == null ? 0 : player.getPersistentData().getInt(POINTS_TAG);
    }

    public static boolean hasSkill(Player player, String id) {
        return player != null && player.getPersistentData().getBoolean(NODE_TAG_PREFIX + id);
    }

    public static boolean isUnlocked(Player player, int index) {
        return player != null && index >= 0 && index < NODES.size() && hasSkill(player, NODES.get(index).id());
    }

    public static int getUnlockedMask(Player player) {
        int mask = 0;
        for (SkillNode node : NODES) if (isUnlocked(player, node.index())) mask |= 1 << node.index();
        for (int i = 0; i < MILESTONES.size(); i++) if (hasEarnedMilestone(player, i)) mask |= 1 << (14 + i);
        return mask;
    }

    public static boolean hasEarnedMilestone(Player player, int index) {
        if (player == null || index < 0 || index >= MILESTONES.size()) return false;
        String advancement = MILESTONES.get(index).advancement();
        return player.getPersistentData().getBoolean(ledgerKey(advancement));
    }

    public static void awardMilestone(ServerPlayer player, String advancementId) {
        Milestone milestone = MILESTONE_BY_ID.get(advancementId);
        if (player == null || milestone == null || player.getPersistentData().getBoolean(ledgerKey(advancementId))) return;
        player.getPersistentData().putBoolean(ledgerKey(advancementId), true);
        addPoints(player, milestone.points());
        player.sendSystemMessage(Component.literal("Assassin skill point +" + milestone.points()
                + " for achievement: " + milestone.title()));
    }

    private static String ledgerKey(String advancement) {
        return MILESTONE_TAG_PREFIX + advancement.replace(':', '_').replace('/', '_');
    }

    public static void addPoints(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) return;
        player.getPersistentData().putInt(POINTS_TAG, getPoints(player) + amount);
        AssassinNetwork.syncAbilityStateNow(player);
    }

    public static boolean unlock(ServerPlayer player, int index) {
        if (player == null || !AssassinAbilityManager.hasClass(player) || index < 0 || index >= NODES.size()) return false;
        SkillNode node = NODES.get(index);
        if (isUnlocked(player, index)) {
            player.sendSystemMessage(Component.literal(node.name() + " is already unlocked."));
            return false;
        }
        if (!isReachable(player, index)) {
            player.sendSystemMessage(Component.literal("Unlock a connected skill first."));
            return false;
        }
        if (getPoints(player) < node.cost()) {
            player.sendSystemMessage(Component.literal("You need " + node.cost() + " skill point(s) for " + node.name() + "."));
            return false;
        }
        player.getPersistentData().putInt(POINTS_TAG, getPoints(player) - node.cost());
        player.getPersistentData().putBoolean(NODE_TAG_PREFIX + node.id(), true);
        player.sendSystemMessage(Component.literal("Skill unlocked: " + node.name()));
        AssassinNetwork.syncAbilityStateNow(player);
        return true;
    }

    public static boolean isReachable(Player player, int index) {
        for (int[] edge : CONNECTIONS) {
            if ((edge[0] == index && isUnlocked(player, edge[1]))
                    || (edge[1] == index && isUnlocked(player, edge[0]))) return true;
        }
        return NODES.get(index).prerequisite() < 0;
    }

    /** Refund and remove all learned branch nodes while preserving achievement claims. */
    public static int respec(ServerPlayer player) {
        if (player == null) return 0;
        int refunded = 0;
        for (SkillNode node : NODES) {
            if (isUnlocked(player, node.index())) {
                player.getPersistentData().remove(NODE_TAG_PREFIX + node.id());
                refunded += node.cost();
            }
        }
        if (refunded > 0) player.getPersistentData().putInt(POINTS_TAG, getPoints(player) + refunded);
        AssassinNetwork.syncAbilityStateNow(player);
        return refunded;
    }

    public static void resetForTesting(ServerPlayer player) {
        if (player == null) return;
        player.getPersistentData().remove(POINTS_TAG);
        for (SkillNode node : NODES) player.getPersistentData().remove(NODE_TAG_PREFIX + node.id());
        for (Milestone milestone : MILESTONES) player.getPersistentData().remove(ledgerKey(milestone.advancement()));
        AssassinNetwork.syncAbilityStateNow(player);
    }
}
