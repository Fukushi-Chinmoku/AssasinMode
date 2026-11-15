package com.assassinmod.assassin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class AssassinProgression {
    public static final int EXPERIENCE_LEVELS_PER_POINT = 5;

    public record SkillResource(String name, String formula, String color) {}
    public record SkillNode(int index, String id, String name, String branch, String description,
                            int cost, int maxRanks, int[] prerequisites, List<SkillResource> resources) {}
    public record Milestone(String advancement, String title, int points) {}

    public static final List<SkillNode> NODES = List.of(
            new SkillNode(0, "backstab", "Рандеву жизни", "Смертоносность",
                    "Стеклянная пушка: удар в спину наносит ×2 урона, удар в лицо — ×0,5.",
                    1, 1, new int[0], List.of()),
            new SkillNode(1, "evasion", "Увертливость", "Ловкость",
                    "Даёт шанс уклониться от атаки.",
                    1, 5, new int[]{0}, List.of(new SkillResource("Шанс уклонения", "[CurrentPoints*1]%", "#ff4747"))),
            new SkillNode(2, "swift_step", "Проворность", "Ловкость",
                    "Пассивно повышает скорость передвижения.",
                    1, 5, new int[]{0}, List.of(new SkillResource("Скорость передвижения", "[5*(CurrentPoints/2)]%", "#ff4747"))),
            new SkillNode(3, "shadow_echo_hit", "Удар из-подтишка", "Смертоносность",
                    "С шансом появляется одна теневая копия и повторяет вашу атаку. Шанс увеличивается с каждым рангом.",
                    1, 5, new int[]{0}, List.of(new SkillResource("Шанс появления клона", "[1*CurrentPoints]%", "#ff4747"))),
            new SkillNode(4, "shadow_veil", "Теневой покров", "Тень",
                    "На время действия полностью скрывает вас от противников.",
                    1, 5, new int[]{1}, List.of(
                    new SkillResource("Время действия (секунды)", "[CurrentPoints]", "#ffffff"),
                    new SkillResource("Перезарядка (секунды)", "[30-(CurrentPoints*2)]", "#ffffff"))),
            new SkillNode(5, "sprinting", "Срыв голени", "Ловкость",
                    "Способность: скорость передвижения +35% на 3 секунды. Перезарядка уменьшается с рангом.",
                    1, 5, new int[]{2}, List.of(
                    new SkillResource("Скорость передвижения", "[30+(CurrentPoints*5)]%", "#ff4747"),
                    new SkillResource("Время действия (секунды)", "3", "#ffff00"),
                    new SkillResource("Перезарядка (секунды)", "[48-(CurrentPoints*3)]", "#ffffff"))),
            new SkillNode(6, "shadow_twin", "Теневое эхо", "Смертоносность",
                    "Отдельная способность (R): телепорт за спину и атака двумя теневыми клонами.",
                    1, 5, new int[]{3}, List.of(
                    new SkillResource("Урон клонов от урона игрока", "[75+(CurrentPoints*15)]%", "#ffffff"),
                    new SkillResource("Дальность (блоков)", "[CurrentPoints*20]", "#ffffff"),
                    new SkillResource("Перезарядка (секунды)", "[45-(CurrentPoints*5)]", "#ffffff"),
                    new SkillResource("Время действия (секунды)", "[2+(CurrentPoints*0.4)]", "#ffff00"))),
            new SkillNode(7, "quiet_step", "Тихая поступь", "Тень",
                    "Уменьшает дистанцию, с которой существа замечают вас, пока вы не вступили в бой.",
                    1, 3, new int[]{4}, List.of(new SkillResource("Снижение дистанции обнаружения", "[CurrentPoints*15]%", "#a878ff"))),
            new SkillNode(8, "veil_double", "Зеркальная сущность", "Тень",
                    "При активации Теневого покрова оставляет хаотично блуждающего клона-приманку.",
                    1, 1, new int[]{4}, List.of()),
            new SkillNode(9, "shadow_step", "Теневой шаг", "Ловкость",
                    "Способность (Z): телепортирует за спину выбранной цели.",
                    1, 5, new int[]{5}, List.of(
                    new SkillResource("Дальность телепортации (блоки)", "[CurrentPoints*20]", "#ffffff"),
                    new SkillResource("Перезарядка (секунды)", "[45-(CurrentPoints*5)]", "#ffffff"))),
            new SkillNode(10, "mirage", "Мираж", "Обман",
                    "Создаёт несколько иллюзий, кружащих вокруг врага и отвлекающих его.",
                    1, 1, new int[]{8}, List.of(
                    new SkillResource("Время действия (секунды)", "[6*CurrentPoints]", "#ffff00"),
                    new SkillResource("Перезарядка (секунды)", "[60*CurrentPoints]", "#ffffff"))),
            new SkillNode(11, "backstep", "Отшаг", "Ловкость",
                    "Телепортирует назад, помогая уклониться от атаки.",
                    1, 5, new int[]{9}, List.of(
                    new SkillResource("Дальность (блоки)", "[1+CurrentPoints]", "#ffffff"),
                    new SkillResource("Перезарядка (секунды)", "[100-(CurrentPoints*5)]", "#ffffff"))),
            new SkillNode(12, "rapid_assault", "Вихрь", "Смертоносность",
                    "На короткое время увеличивает скорость атаки в 10 раз, но уменьшает урон каждого удара в 10 раз.",
                    1, 1, new int[]{9}, List.of(
                    new SkillResource("Время действия (секунды)", "[5+(CurrentPoints*0)]", "#ffff00"),
                    new SkillResource("Перезарядка (секунды)", "[60+(CurrentPoints*0)]", "#ffffff"))),
            new SkillNode(13, "decoy", "Приманка", "Обман",
                    "После Отшага оставляет на исходном месте иллюзию, отвлекающую врагов.",
                    1, 5, new int[]{11}, List.of(
                    new SkillResource("Время жизни иллюзии (секунды)", "[0.1+(CurrentPoints*0.5)]", "#ffffff"))),
            new SkillNode(14, "perfect_dodge", "Идеальное уклонение", "Ловкость",
                    "Способность (B): гарантированно уклоняется от входящего урона в течение 5 секунд.",
                    1, 1, new int[]{1}, List.of(
                    new SkillResource("Гарантированное уклонение (секунды)", "5", "#ffff00"),
                    new SkillResource("Перезарядка (секунды)", "60", "#ffffff"))),
            new SkillNode(15, "shadow_dance", "Пляс теней", "Смертоносность",
                    "Активная способность: каждый ваш удар создаёт клона на время действия. Длительность равна рангу в секундах.",
                    1, 5, new int[]{3}, List.of(
                    new SkillResource("Клон при каждом ударе", "100%", "#ff4747"),
                    new SkillResource("Длительность (секунды)", "[CurrentPoints]", "#ffff00"),
                    new SkillResource("Перезарядка (секунды)", "90", "#ffffff"))),
            new SkillNode(16, "shadow_echo_followup", "Вторая тень", "Смертоносность",
                    "Пассивное улучшение: после удара первого клона через полсекунды слева появляется второй и повторяет удар.",
                    1, 1, new int[]{15}, List.of(
                    new SkillResource("Задержка после удара (секунды)", "0.5", "#ffff00"),
                    new SkillResource("Дополнительные тени", "1", "#ff4747")))
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
    private static final String RANK_TAG_PREFIX = "assassin_skill_rank_";
    private static final String MILESTONE_TAG_PREFIX = "assassin_milestone_";
    private static final String TREE_REVISION_TAG = "assassin_skill_tree_revision";
    private static final Map<String, Integer> RETIRED_SKILLS = Map.ofEntries(
            Map.entry("far_step", 2), Map.entry("echo_step", 2), Map.entry("executioner", 2),
            Map.entry("twin_rhythm", 2), Map.entry("deep_veil", 2), Map.entry("veil_strike", 2),
            Map.entry("false_horde", 2), Map.entry("lingering_mist", 2), Map.entry("phantom_lunge", 2),
            Map.entry("pursuit", 1), Map.entry("marked_target", 1), Map.entry("shadow_guard", 1));
    public static final int[][] CONNECTIONS = {
            {0, 1}, {0, 2}, {0, 3},
            {1, 4}, {2, 5}, {3, 6},
            {4, 7}, {4, 8}, {5, 9},
            {8, 10}, {9, 11}, {9, 12}, {11, 13}, {1, 14},
            {3, 15}, {15, 16}
    };
    private static final Map<String, Milestone> MILESTONE_BY_ID = MILESTONES.stream()
            .collect(Collectors.toUnmodifiableMap(Milestone::advancement, milestone -> milestone));

    private AssassinProgression() {}

    public static int getPoints(Player player) {
        return player == null ? 0 : player.getPersistentData().getInt(POINTS_TAG);
    }

    public static boolean hasSkill(Player player, String id) {
        return getRank(player, id) > 0;
    }

    public static int getRank(Player player, String id) {
        return player == null ? 0 : player.getPersistentData().getInt(RANK_TAG_PREFIX + id);
    }

    public static int getRank(Player player, int index) {
        return player == null || index < 0 || index >= NODES.size() ? 0 : getRank(player, NODES.get(index).id());
    }

    public static double getResourceValue(Player player, int nodeIndex, int resourceIndex) {
        if (nodeIndex < 0 || nodeIndex >= NODES.size()) return 0.0D;
        return getResourceValueForRank(nodeIndex, resourceIndex, getRank(player, nodeIndex));
    }

    public static double getResourceValueForRank(int nodeIndex, int resourceIndex, int rank) {
        if (nodeIndex < 0 || nodeIndex >= NODES.size()) return 0.0D;
        SkillNode node = NODES.get(nodeIndex);
        if (resourceIndex < 0 || resourceIndex >= node.resources().size()) return 0.0D;
        return evaluateFormula(node.resources().get(resourceIndex).formula(), rank);
    }

    public static String formatResource(Player player, int nodeIndex, int resourceIndex) {
        return formatResourceForRank(nodeIndex, resourceIndex, getRank(player, nodeIndex));
    }

    public static String formatResourceForRank(int nodeIndex, int resourceIndex, int rank) {
        SkillResource resource = NODES.get(nodeIndex).resources().get(resourceIndex);
        double value = getResourceValueForRank(nodeIndex, resourceIndex, rank);
        String number = Math.abs(value - Math.rint(value)) < 0.0001D
                ? Long.toString(Math.round(value))
                : String.format(java.util.Locale.ROOT, "%.1f", value);
        String sign = resource.formula().trim().startsWith("+") ? "+" : "";
        return resource.name() + ": " + sign + number + (resource.formula().contains("%") ? "%" : "");
    }

    private static double evaluateFormula(String formula, int rank) {
        String expression = formula.trim().replace("%", "");
        if (expression.startsWith("[") && expression.endsWith("]")) {
            expression = expression.substring(1, expression.length() - 1);
        }
        expression = expression.replace("CurrentPoints", Integer.toString(rank));
        try {
            FormulaParser parser = new FormulaParser(expression);
            double result = parser.expression();
            return parser.atEnd() && Double.isFinite(result) ? result : 0.0D;
        } catch (IllegalArgumentException exception) {
            return 0.0D;
        }
    }

    private static final class FormulaParser {
        private final String input;
        private int cursor;

        private FormulaParser(String input) {
            this.input = input;
        }

        private double expression() {
            double value = term();
            while (true) {
                skipSpaces();
                if (take('+')) value += term();
                else if (take('-')) value -= term();
                else return value;
            }
        }

        private double term() {
            double value = factor();
            while (true) {
                skipSpaces();
                if (take('*')) value *= factor();
                else if (take('/')) value /= factor();
                else return value;
            }
        }

        private double factor() {
            skipSpaces();
            if (take('+')) return factor();
            if (take('-')) return -factor();
            if (take('(')) {
                double value = expression();
                skipSpaces();
                if (!take(')')) throw new IllegalArgumentException("Unclosed formula group");
                return value;
            }
            int start = cursor;
            while (cursor < input.length()
                    && (Character.isDigit(input.charAt(cursor)) || input.charAt(cursor) == '.')) cursor++;
            if (start == cursor) throw new IllegalArgumentException("Expected formula value");
            return Double.parseDouble(input.substring(start, cursor));
        }

        private boolean take(char expected) {
            if (cursor >= input.length() || input.charAt(cursor) != expected) return false;
            cursor++;
            return true;
        }

        private void skipSpaces() {
            while (cursor < input.length() && Character.isWhitespace(input.charAt(cursor))) cursor++;
        }

        private boolean atEnd() {
            skipSpaces();
            return cursor == input.length();
        }
    }

    public static boolean isUnlocked(Player player, int index) {
        return player != null && index >= 0 && index < NODES.size() && hasSkill(player, NODES.get(index).id());
    }

    public static int getUnlockedMask(Player player) {
        int mask = 0;
        for (SkillNode node : NODES) if (isUnlocked(player, node.index())) mask |= 1 << node.index();
        for (int i = 0; i < MILESTONES.size(); i++) if (hasEarnedMilestone(player, i)) mask |= 1 << (NODES.size() + i);
        return mask;
    }

    public static long getPackedRanks(Player player) {
        long packed = 0L;
        for (SkillNode node : NODES) {
            packed |= (long) Math.min(7, getRank(player, node.index())) << (node.index() * 3);
        }
        return packed;
    }

    public static int[] unpackRanks(long packed) {
        int[] ranks = new int[NODES.size()];
        for (SkillNode node : NODES) ranks[node.index()] = (int) ((packed >>> (node.index() * 3)) & 0x7L);
        return ranks;
    }

    public static boolean hasEarnedMilestone(Player player, int index) {
        if (player == null || index < 0 || index >= MILESTONES.size()) return false;
        String advancement = MILESTONES.get(index).advancement();
        return player.getPersistentData().getBoolean(ledgerKey(advancement));
    }

    public static void awardMilestone(ServerPlayer player, String advancementId) {
        Milestone milestone = MILESTONE_BY_ID.get(advancementId);
        if (player == null || !AssassinAbilityManager.hasClass(player) || milestone == null
                || player.getPersistentData().getBoolean(ledgerKey(advancementId))) return;
        player.getPersistentData().putBoolean(ledgerKey(advancementId), true);
        addPoints(player, milestone.points());
    }

    private static String ledgerKey(String advancement) {
        return MILESTONE_TAG_PREFIX + advancement.replace(':', '_').replace('/', '_');
    }

    public static void addPoints(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) return;
        player.getPersistentData().putInt(POINTS_TAG, getPoints(player) + amount);
        AssassinNetwork.syncAbilityStateNow(player);
    }

    public static boolean buySkillPoint(ServerPlayer player) {
        if (player == null || !AssassinAbilityManager.hasClass(player)) return false;
        if (player.experienceLevel < EXPERIENCE_LEVELS_PER_POINT) {
            return false;
        }
        player.giveExperienceLevels(-EXPERIENCE_LEVELS_PER_POINT);
        addPoints(player, 1);
        return true;
    }

    public static void migrateSkillTree(ServerPlayer player) {
        if (player == null || player.getPersistentData().getInt(TREE_REVISION_TAG) >= 8) return;
        int refunded = 0;
        String retiredDebuffSightRankTag = RANK_TAG_PREFIX + "debuff_sight";
        int debuffSightRank = player.getPersistentData().getInt(retiredDebuffSightRankTag);
        player.getPersistentData().remove(retiredDebuffSightRankTag);
        if (debuffSightRank > 0) {
            refunded += debuffSightRank;
        }
        String retiredDebuffSightTag = NODE_TAG_PREFIX + "debuff_sight";
        boolean hadRetiredDebuffSight = player.getPersistentData().getBoolean(retiredDebuffSightTag);
        player.getPersistentData().remove(retiredDebuffSightTag);
        if (hadRetiredDebuffSight) {
            if (debuffSightRank == 0) refunded++;
        }
        for (var retired : RETIRED_SKILLS.entrySet()) {
            String tag = NODE_TAG_PREFIX + retired.getKey();
            String rankTag = RANK_TAG_PREFIX + retired.getKey();
            int retiredRank = player.getPersistentData().getInt(rankTag);
            player.getPersistentData().remove(rankTag);
            if (retiredRank > 0) {
                refunded += retiredRank;
            }
            boolean hadRetiredSkill = player.getPersistentData().getBoolean(tag);
            player.getPersistentData().remove(tag);
            if (hadRetiredSkill) {
                if (retiredRank == 0) refunded += retired.getValue();
            }
        }
        for (SkillNode node : NODES) {
            String legacy = NODE_TAG_PREFIX + node.id();
            if (player.getPersistentData().getBoolean(legacy)
                    && getRank(player, node.id()) == 0) {
                player.getPersistentData().putInt(RANK_TAG_PREFIX + node.id(), 1);
            }
            player.getPersistentData().remove(legacy);
        }
        if (refunded > 0) {
            player.getPersistentData().putInt(POINTS_TAG, getPoints(player) + refunded);
        }
        player.getPersistentData().putInt(TREE_REVISION_TAG, 8);
        AssassinNetwork.syncAbilityStateNow(player);
    }

    public static boolean unlock(ServerPlayer player, int index) {
        if (player == null || !AssassinAbilityManager.hasClass(player) || index < 0 || index >= NODES.size()) return false;
        SkillNode node = NODES.get(index);
        int currentRank = getRank(player, index);
        if (currentRank >= node.maxRanks()) {
            return false;
        }
        if (!isReachable(player, index)) {
            return false;
        }
        if (getPoints(player) < node.cost()) {
            return false;
        }
        player.getPersistentData().putInt(POINTS_TAG, getPoints(player) - node.cost());
        player.getPersistentData().putInt(RANK_TAG_PREFIX + node.id(), currentRank + 1);
        AssassinNetwork.syncAbilityStateNow(player);
        return true;
    }

    public static boolean isReachable(Player player, int index) {
        if (index < 0 || index >= NODES.size()) return false;
        for (int prerequisite : NODES.get(index).prerequisites()) {
            if (!isUnlocked(player, prerequisite)) return false;
        }
        return true;
    }

    /** Refund and remove all learned branch nodes while preserving achievement claims. */
    public static int respec(ServerPlayer player) {
        if (player == null) return 0;
        int refunded = 0;
        for (SkillNode node : NODES) {
            int rank = getRank(player, node.index());
            if (rank > 0) player.getPersistentData().remove(RANK_TAG_PREFIX + node.id());
            refunded += rank * node.cost();
        }
        if (refunded > 0) player.getPersistentData().putInt(POINTS_TAG, getPoints(player) + refunded);
        AssassinNetwork.syncAbilityStateNow(player);
        return refunded;
    }

    public static void resetForTesting(ServerPlayer player) {
        if (player == null) return;
        player.getPersistentData().remove(POINTS_TAG);
        for (SkillNode node : NODES) {
            player.getPersistentData().remove(NODE_TAG_PREFIX + node.id());
            player.getPersistentData().remove(RANK_TAG_PREFIX + node.id());
        }
        for (Milestone milestone : MILESTONES) player.getPersistentData().remove(ledgerKey(milestone.advancement()));
        for (String id : RETIRED_SKILLS.keySet()) player.getPersistentData().remove(NODE_TAG_PREFIX + id);
        player.getPersistentData().remove(TREE_REVISION_TAG);
        AssassinNetwork.syncAbilityStateNow(player);
    }
}
