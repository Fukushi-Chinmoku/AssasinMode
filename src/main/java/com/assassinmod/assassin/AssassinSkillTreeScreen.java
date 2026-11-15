package com.assassinmod.assassin;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

/** Assassin adaptation of the Passive Skill Tree node canvas and pan/zoom presentation. */
public final class AssassinSkillTreeScreen extends Screen {
    private static final ResourceLocation STARFIELD = texture("screen/skill_tree_background.png");
    private static final ResourceLocation[] UNLOCK_VFX = new ResourceLocation[12];
    static {
        for (int i = 0; i < UNLOCK_VFX.length; i++) UNLOCK_VFX[i] = texture("screen/vfx2/frame_" + (i + 1) + ".png");
    }
    private static final ResourceLocation[] NODE_FRAMES = {
            texture("icons/background/class.png"), texture("icons/background/keystone.png"),
            texture("icons/background/notable.png"), texture("icons/background/lesser.png")
    };
    private static final ResourceLocation[] BLOOD_REVEAL_VFX = new ResourceLocation[30];
    static {
        for (int i = 0; i < BLOOD_REVEAL_VFX.length; i++)
            BLOOD_REVEAL_VFX[i] = texture("screen/blood_reveal/frame_" + (i + 1) + ".png");
    }
    private static final String[] ICONS = {
            "sword_gold", "eye_green", "boots_gold", "twin_daggers",
            "potion_black", "boots_gold", "twin_daggers", "ghost",
            "ghost", "arrow_steel", "potion_indigo", "arrow_diamond",
            "sword_iron", "ghost", "ghost", "ghost", "twin_daggers"
    };
    // Icons cut from the dark ability pack are 32x32, the rest of the set is 16x16.
    private static final java.util.Set<String> ICONS_32 = java.util.Set.of("twin_daggers", "ghost");
    private static final double[][] POSITIONS = {
            {0, -270}, {-84, -170}, {0, -170}, {84, -170},
            {-126, -70}, {0, -70}, {126, -70},
            {-168, 30}, {-126, 30}, {0, 30},
            {-168, 130}, {-42, 130}, {0, 130}, {-42, 230}, {-210, -70},
            {168, -170}, {168, -70}
    };
    private static final int[] BRANCH_COLORS = {0xFF7CCBFF, 0xFFFF737E, 0xFFB483FF, 0xFF86E1C3};
    private static final int GRAPH_WIDTH = 460;
    private static final int GRAPH_HEIGHT = 600;
    private static final long REVEAL_DURATION_MS = 620L;
    private static final long BLOOD_REVEAL_FRAME_DURATION_MS = 20L;
    // Pack 2 peaks at frame 10 of 30: the icon must pop exactly under the biggest blob.
    private static final float BLOOD_REVEAL_NODE_APPEAR_PROGRESS = 0.33F;
    // Start the burst slightly before the spark lands so the droplet is already growing.
    private static final long BLOOD_REVEAL_LEAD_MS = 100L;
    private static final int UNLOCK_VFX_FRAME_COUNT = UNLOCK_VFX.length * 2 - 1;
    private static final long UNLOCK_VFX_FRAME_DURATION_MS = 55L;

    private int selectedNode = -1;
    private int hoveredNode = -1;
    private float sceneTime;
    private float zoomMultiplier = 1.0F;
    private float panX;
    private float panY;
    private int panButton = -1;
    private int observedSkillMask;
    private int unlockVfxNode = -1;
    private long unlockVfxStartedAt;
    private final List<Integer> queuedUnlockVfx = new ArrayList<>();
    private int observedVisibleMask;
    private final List<RevealAnimation> revealAnimations = new ArrayList<>();

    private static final class RevealAnimation {
        final int from;
        final int to;
        final long startAt;
        RevealAnimation(int from, int to, long startAt) {
            this.from = from;
            this.to = to;
            this.startAt = startAt;
        }
    }

    public AssassinSkillTreeScreen() {
        super(Component.literal("Assassin Skill Tree"));
        observedSkillMask = AssassinClientState.getUnlockedSkillsMask();
        observedVisibleMask = visibleMask();
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(AssassinMod.MOD_ID, "textures/passive/" + path);
    }

    private float fitZoom() {
        float availableWidth = Math.max(1F, width - 24F);
        float availableHeight = Math.max(1F, height - 72F);
        return Math.min(availableWidth / GRAPH_WIDTH, availableHeight / GRAPH_HEIGHT);
    }

    private float zoom() { return fitZoom() * zoomMultiplier; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        sceneTime += Math.max(partialTick, 0.04F);
        float reveal = Mth.clamp(sceneTime / 16F, 0F, 1F);
        detectNewUnlocks();
        if (selectedNode >= 0 && !isVisible(selectedNode)) selectedNode = -1;
        drawBackdrop(g);
        drawGraph(g, mouseX, mouseY, reveal);
        drawHeader(g, reveal);
        drawHoverTooltip(g, mouseX, mouseY);
    }

    private void drawBackdrop(GuiGraphics g) {
        // Scale the square starfield to cover the whole screen. A fixed 2048px
        // quad left black strips on ultrawide and high-resolution displays.
        int size = Math.max(width, height);
        g.blit(STARFIELD, (width - size) / 2, (height - size) / 2, 0,
                0F, 0F, size, size, size, size);
        g.fill(0, 0, width, height, 0x20070710);
    }

    private void drawGraph(GuiGraphics g, int mouseX, int mouseY, float reveal) {
        int clipRight = width;
        g.enableScissor(0, 0, clipRight, height);
        double graphMouseX = (mouseX - clipRight / 2.0 - panX) / zoom();
        double graphMouseY = (mouseY - height / 2.0 - panY) / zoom();
        hoveredNode = findNode(graphMouseX, graphMouseY);

        var pose = g.pose();
        pose.pushPose();
        pose.translate(Math.round(clipRight / 2.0 + panX), Math.round(height / 2.0 + panY), 0);
        pose.scale(zoom(), zoom(), 1F);

        drawConnections(g);
        drawRevealAnimations(g);
        for (var node : AssassinProgression.NODES) drawNode(g, node.index());
        drawBloodBursts(g);
        drawUnlockVfx(g);
        pose.popPose();
        g.disableScissor();
    }

    private int findNode(double x, double y) {
        for (int i = 0; i < POSITIONS.length; i++) {
            if (!isVisible(i) || isRevealAnimating(i)) continue;
            int size = nodeSize(i);
            double radius = size * 0.64;
            if (Math.abs(x - POSITIONS[i][0]) <= radius && Math.abs(y - POSITIONS[i][1]) <= radius) return i;
        }
        return -1;
    }

    private void drawConnections(GuiGraphics g) {
        for (int[] edge : AssassinProgression.CONNECTIONS) {
            int a = edge[0], b = edge[1];
            if (!isVisible(a) || !isVisible(b)) continue;
            // Hide the static connection while the reveal spark is still travelling to it.
            if (isRevealAnimating(a) || isRevealAnimating(b)) continue;
            boolean learned = isLearned(a) && isLearned(b);
            boolean available = (isLearned(a) || isLearned(b)) && (canUnlock(a) || canUnlock(b));
            float pulse = (float) (0.5 + 0.5 * Math.sin(sceneTime / 4F));
            float alpha = learned ? 0.75F + pulse * 0.25F : available ? 0.30F + pulse * 0.22F : 0.14F;
            drawConnection(g, POSITIONS[a][0], POSITIONS[a][1], POSITIONS[b][0], POSITIONS[b][1],
                    learned || available ? branchColor(b) : 0xFF777982, alpha, learned);
        }
    }

    private void drawConnection(GuiGraphics g, double x1, double y1, double x2, double y2,
                                int color, float alpha, boolean learned) {
        double length = Math.hypot(x2 - x1, y2 - y1);
        float angle = (float) Math.atan2(y2 - y1, x2 - x1);
        g.pose().pushPose();
        g.pose().translate(x1, y1, 0);
        g.pose().mulPose(Axis.ZP.rotation(angle));
        int rgb = color & 0x00FFFFFF;
        if (learned) g.fill(0, -5, (int) length, 5, ((int) (alpha * 0.22F * 255F) << 24) | rgb);
        g.fill(0, -1, (int) length, 2, ((int) (alpha * 255F) << 24) | rgb);
        g.pose().popPose();
    }

    private int nodeSize(int index) {
        if (index == 0) return 56;
        if (index == 1 || index == 2 || index == 3 || index == 4 || index == 5 || index == 6) return 44;
        if (index == 8 || index == 9 || index == 10 || index == 11 || index == 12 || index == 13) return 40;
        return 32;
    }

    private int frameType(int index) {
        if (index == 0) return 0;
        if (index == 1 || index == 2 || index == 3) return 1;
        if (index == 4 || index == 5 || index == 6 || index == 8 || index == 9
                || index == 10 || index == 11 || index == 12 || index == 13) return 2;
        return 3;
    }

    private void drawNode(GuiGraphics g, int index) {
        if (!isVisible(index) || isRevealAnimating(index) || isPlayingUnlockVfx(index)) return;
        int size = nodeSize(index);
        int type = frameType(index);
        ResourceLocation frame = NODE_FRAMES[type];
        double cx = POSITIONS[index][0];
        double cy = POSITIONS[index][1];
        int x = (int) Math.round(cx - size / 2.0);
        int y = (int) Math.round(cy - size / 2.0);
        boolean unlocked = isLearned(index);
        boolean canUnlock = canUnlock(index);
        int color = branchColor(index);
        float red = ((color >> 16) & 0xFF) / 255F;
        float green = ((color >> 8) & 0xFF) / 255F;
        float blue = (color & 0xFF) / 255F;
        if (unlocked) {
            g.setColor(red, green, blue, 0.9F);
            g.blit(frame, x, y, size, size, size * 2, 0, size, size, size * 3, size);
        } else {
            // Keep every unlearned icon subdued, including currently available nodes.
            g.setColor(0.42F, 0.43F, 0.47F, 0.88F);
            g.blit(frame, x, y, size, size, 0, 0, size, size, size * 3, size);
            g.blit(frame, x, y, size, size, size, 0, size, size, size * 3, size);
        }
        g.setColor(1F, 1F, 1F, 1F);

        ResourceLocation icon = texture("icons/" + ICONS[index] + ".png");
        int iconSize = index == 0 || index == 4 || index == 5 || index == 6 || index == 9
                || index == 12 || index == 14 ? 20 : 16;
        int iconSource = ICONS_32.contains(ICONS[index]) ? 32 : 16;
        if (!unlocked) g.setColor(0.48F, 0.49F, 0.53F, 0.9F);
        g.blit(icon, (int) cx - iconSize / 2, (int) cy - iconSize / 2,
                iconSize, iconSize, 0, 0, iconSource, iconSource, iconSource, iconSource);
        g.setColor(1F, 1F, 1F, 1F);

        // Soul-fire ring: a small tongue runs around available skills, a large one
        // around the selected skill, as if the ring itself were a burning trail.
        if (index == selectedNode) {
            drawFlameRing(g, cx, cy, size * 0.78, true);
        } else if (canUnlock && !unlocked) {
            drawFlameRing(g, cx, cy, size * 0.70, false);
        }
    }

    /** Pixel soul-fire ring with a flame tongue travelling along the circumference. */
    private void drawFlameRing(GuiGraphics g, double cx, double cy, double radius, boolean large) {
        // Quiet dotted track the tongue runs along.
        int trackDots = large ? 36 : 24;
        int trackColor = (large ? 0x55 : 0x33) << 24 | 0x9FD8F2;
        for (int i = 0; i < trackDots; i++) {
            double angle = (Math.PI * 2 * i) / trackDots;
            double px = cx + Math.cos(angle) * radius;
            double py = cy + Math.sin(angle) * radius;
            g.fill((int) Math.round(px), (int) Math.round(py),
                    (int) Math.round(px) + 1, (int) Math.round(py) + 1, trackColor);
        }

        // The tongue itself: white-hot head fading into a blue tail.
        float head = sceneTime * (large ? 1.1F : 0.8F);
        int segments = large ? 26 : 14;
        float span = (float) (Math.PI * (large ? 1.45 : 0.85));
        for (int i = 0; i < segments; i++) {
            float t = i / (float) segments;
            float angle = head - t * span;
            float fade = 1F - t;
            fade *= fade;
            int alpha = (int) (fade * (large ? 235 : 195));
            int dot = Math.max(1, Math.round((large ? 5F : 3F) * (1F - 0.6F * t)));
            // Tail licks slightly outward, like a flame leaning away from the track.
            double r = radius + t * (large ? 2.5 : 1.2);
            double px = cx + Math.cos(angle) * r;
            double py = cy + Math.sin(angle) * r;
            int color = t < 0.22F ? 0xF6FDFF : t < 0.55F ? 0x9FF3FF : 0x4FA8E8;
            int half = dot / 2;
            g.fill((int) Math.round(px) - half, (int) Math.round(py) - half,
                    (int) Math.round(px) + half + 1, (int) Math.round(py) + half + 1,
                    (alpha << 24) | color);
        }
    }

    private int branchColor(int index) {
        if (index == 0) return 0xFFFFD45A;
        return BRANCH_COLORS[branchIndex(AssassinProgression.NODES.get(index).branch())];
    }

    private void detectNewUnlocks() {
        int currentMask = AssassinClientState.getUnlockedSkillsMask();
        int added = currentMask & ~observedSkillMask;
        observedSkillMask = currentMask;
        if (added != 0) {
            for (int i = 0; i < AssassinProgression.NODES.size(); i++) {
                if ((added & (1 << i)) != 0) {
                    if (unlockVfxNode < 0) {
                        unlockVfxNode = i;
                        unlockVfxStartedAt = Util.getMillis();
                    } else {
                        queuedUnlockVfx.add(i);
                    }
                }
            }
        }
        detectNewlyVisibleNodes();
    }

    private void detectNewlyVisibleNodes() {
        int currentMask = visibleMask();
        int added = currentMask & ~observedVisibleMask;
        observedVisibleMask = currentMask;
        long startAt = Util.getMillis();
        for (RevealAnimation animation : revealAnimations) {
            startAt = Math.max(startAt, animation.startAt + REVEAL_DURATION_MS);
        }
        for (int i = 1; i < AssassinProgression.NODES.size(); i++) {
            if ((added & (1 << i)) == 0) continue;
            int parent = learnedParent(i);
            if (parent >= 0) {
                revealAnimations.add(new RevealAnimation(parent, i, startAt));
                startAt += 110L;
            }
        }
        long now = Util.getMillis();
        // The entry dies the moment its node appears — nothing plays after the icon.
        revealAnimations.removeIf(animation -> now >= burstStart(animation) + bloodRevealAppearDelay());
    }

    private static long bloodBurstDuration() {
        return BLOOD_REVEAL_VFX.length * BLOOD_REVEAL_FRAME_DURATION_MS;
    }

    private static long bloodRevealAppearDelay() {
        return (long) (bloodBurstDuration() * BLOOD_REVEAL_NODE_APPEAR_PROGRESS);
    }

    private static long burstStart(RevealAnimation animation) {
        return animation.startAt + REVEAL_DURATION_MS - BLOOD_REVEAL_LEAD_MS;
    }

    private int visibleMask() {
        int mask = 1; // Keep only the entry node visible before progression begins.
        for (int i = 0; i < AssassinProgression.NODES.size(); i++) {
            if (AssassinClientState.isSkillUnlocked(i)) mask |= 1 << i;
        }
        for (int i = 1; i < AssassinProgression.NODES.size(); i++) {
            if (canUnlock(i)) mask |= 1 << i;
        }
        return mask;
    }

    private int learnedParent(int index) {
        int[] prerequisites = AssassinProgression.NODES.get(index).prerequisites();
        return prerequisites.length == 0 ? -1 : prerequisites[0];
    }

    private boolean isVisible(int index) {
        return (visibleMask() & (1 << index)) != 0;
    }

    private boolean isRevealAnimating(int index) {
        long now = Util.getMillis();
        // Hidden while the spark travels and until the burst reaches its peak; the node
        // then pops exactly under the biggest blob and the burst stops right there.
        return revealAnimations.stream().anyMatch(animation -> animation.to == index
                && now < burstStart(animation) + bloodRevealAppearDelay());
    }

    private boolean isPlayingUnlockVfx(int index) {
        return unlockVfxNode == index
                && Util.getMillis() < unlockVfxStartedAt + UNLOCK_VFX_FRAME_COUNT * UNLOCK_VFX_FRAME_DURATION_MS;
    }

    private void drawRevealAnimations(GuiGraphics g) {
        long now = Util.getMillis();
        for (RevealAnimation animation : revealAnimations) {
            float progress = Mth.clamp((now - animation.startAt) / (float) REVEAL_DURATION_MS, 0F, 1F);
            if (progress > 0F && progress < 1F) {
                drawElectricPulse(g, animation.from, animation.to, progress, branchColor(animation.to));
            }
        }
    }

    /** The burst starts just before the spark lands and ends the instant the node appears. */
    private void drawBloodBursts(GuiGraphics g) {
        long now = Util.getMillis();
        for (RevealAnimation animation : revealAnimations) {
            long since = now - burstStart(animation);
            // Nothing plays past the appear point — no stray frames after the icon.
            if (since < 0 || since >= bloodRevealAppearDelay()) continue;
            int frame = (int) (since / BLOOD_REVEAL_FRAME_DURATION_MS);
            // Pack 2's blob only fills ~30% of the frame, so scale it up harder than other packs.
            int size = nodeSize(animation.to) * 4;
            int x = (int) Math.round(POSITIONS[animation.to][0] - size / 2.0);
            int y = (int) Math.round(POSITIONS[animation.to][1] - size / 2.0);
            g.blit(BLOOD_REVEAL_VFX[frame], x, y, size, size, 0, 0, 100, 100, 100, 100);
        }
    }

    private void drawElectricPulse(GuiGraphics g, int from, int to, float progress, int color) {
        double x1 = POSITIONS[from][0], y1 = POSITIONS[from][1];
        double dx = POSITIONS[to][0] - x1, dy = POSITIONS[to][1] - y1;
        double length = Math.hypot(dx, dy);
        float angle = (float) Math.atan2(dy, dx);
        int head = (int) (length * progress);
        int rgb = color & 0x00FFFFFF;
        g.pose().pushPose();
        g.pose().translate(x1, y1, 0);
        g.pose().mulPose(Axis.ZP.rotation(angle));
        // Trail left behind the spark: the line that stays and connects the two skills.
        g.fill(0, -5, head, 5, 0x33000000 | rgb);
        g.fill(0, -1, head, 2, 0xAA000000 | rgb);
        // Fading glow just behind the spark head.
        g.fill(Math.max(0, head - 12), -4, head, 4, 0x66000000 | rgb);
        // The spark itself, drawn last so it always rides on top of the trail.
        g.fill(head - 2, -3, head + 4, 3, 0xFFDFFAFF);
        g.fill(head - 1, -2, head + 2, 2, 0xFFFFFFFF);
        g.pose().popPose();
    }

    private void drawUnlockVfx(GuiGraphics g) {
        if (unlockVfxNode < 0) return;
        int sequenceFrame = (int) ((Util.getMillis() - unlockVfxStartedAt) / UNLOCK_VFX_FRAME_DURATION_MS);
        if (sequenceFrame >= UNLOCK_VFX_FRAME_COUNT) {
            if (queuedUnlockVfx.isEmpty()) {
                unlockVfxNode = -1;
                return;
            }
            unlockVfxNode = queuedUnlockVfx.remove(0);
            unlockVfxStartedAt = Util.getMillis();
            sequenceFrame = 0;
        }
        int frame = sequenceFrame < UNLOCK_VFX.length
                ? sequenceFrame
                : UNLOCK_VFX_FRAME_COUNT - sequenceFrame - 1;
        int size = 148;
        int x = (int) Math.round(POSITIONS[unlockVfxNode][0] - size / 2.0);
        int y = (int) Math.round(POSITIONS[unlockVfxNode][1] - size / 2.0);
        g.blit(UNLOCK_VFX[frame], x, y, size, size, 0, 0, 128, 128, 128, 128);
    }

    private boolean isLearned(int index) {
        return effectiveRank(index) > 0;
    }

    private boolean canUnlock(int index) {
        var node = AssassinProgression.NODES.get(index);
        if (effectiveRank(index) >= node.maxRanks() || AssassinClientState.getSkillPoints() < node.cost()) return false;
        for (int prerequisite : node.prerequisites()) {
            if (effectiveRank(prerequisite) < 1) return false;
        }
        return true;
    }

    private int effectiveRank(int index) {
        return AssassinClientState.getSkillRank(index);
    }

    private void drawHoverTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (hoveredNode < 0 || !isVisible(hoveredNode)) return;
        var node = AssassinProgression.NODES.get(hoveredNode);
        int currentRank = effectiveRank(hoveredNode);
        int nextRank = Math.min(node.maxRanks(), currentRank + 1);
        int tooltipWidth = Math.min(310, Math.max(220, width / 3));
        int textWidth = tooltipWidth - 20;
        List<net.minecraft.util.FormattedCharSequence> description =
                font.split(Component.literal(node.description()), textWidth);
        List<String> stats = new ArrayList<>();
        for (int i = 0; i < node.resources().size(); i++) {
            String current = AssassinProgression.formatResourceForRank(hoveredNode, i, currentRank);
            if (nextRank > currentRank) {
                String next = AssassinProgression.formatResourceForRank(hoveredNode, i, nextRank);
                current += "  →  " + next.substring(next.indexOf(':') + 2);
            }
            stats.add(current);
        }
        int descriptionHeight = 14 + Math.max(1, description.size()) * 10;
        int statsHeight = 18 + Math.max(1, stats.size()) * 11;
        int tooltipHeight = 42 + descriptionHeight + statsHeight;
        int x = mouseX + 14;
        int y = mouseY + 12;
        if (x + tooltipWidth > width) x = mouseX - tooltipWidth - 12;
        if (x < 4) x = 4;
        if (y + tooltipHeight > height) y = height - tooltipHeight - 4;
        if (y < 4) y = 4;
        g.fill(x, y, x + tooltipWidth, y + tooltipHeight, 0xF20B0912);
        g.fill(x, y, x + tooltipWidth, y + 1, branchColor(hoveredNode));
        g.fill(x, y, x + 1, y + tooltipHeight, branchColor(hoveredNode));
        g.drawString(font, node.name(), x + 9, y + 7, branchColor(hoveredNode), false);
        g.drawString(font, "РАНГ " + currentRank + "/" + node.maxRanks() + "   ·   ЦЕНА " + node.cost(),
                x + 9, y + 19, 0xFFFCE266, false);
        int lineY = y + 38;
        g.fill(x + 7, lineY - 3, x + tooltipWidth - 7, lineY - 2, 0x665E526C);
        g.drawString(font, "ОПИСАНИЕ", x + 9, lineY, 0xFFB7ADC9, false);
        lineY += 12;
        for (var line : description) {
            g.drawString(font, line, x + 9, lineY, 0xFFECE5F3, false);
            lineY += 10;
        }
        lineY += 5;
        int statsBlockTop = lineY - 4;
        int statsBlockHeight = 16 + Math.max(1, stats.size()) * 11;
        g.fill(x + 5, statsBlockTop, x + tooltipWidth - 5, statsBlockTop + statsBlockHeight, 0xAA21182D);
        g.fill(x + 5, statsBlockTop, x + tooltipWidth - 5, statsBlockTop + 1, 0xAA8E78AC);
        String statHeading = nextRank > currentRank
                ? "ХАРАКТЕРИСТИКИ  ·  СЛЕДУЮЩИЙ РАНГ" : "ХАРАКТЕРИСТИКИ  ·  МАКСИМУМ";
        g.drawString(font, statHeading, x + 9, lineY, 0xFFD8B6FF, false);
        lineY += 12;
        if (stats.isEmpty()) {
            g.drawString(font, "Пассивный навык", x + 9, lineY, 0xFFC8B9D7, false);
        } else {
            for (String stat : stats) {
                g.drawString(font, stat, x + 9, lineY, 0xFFB9E6FF, false);
                lineY += 11;
            }
        }
    }

    private void drawHeader(GuiGraphics g, float reveal) {
        int y = 14 - (int) ((1F - reveal) * 8);
        g.drawString(font, "АССАСИН // ДРЕВО НАВЫКОВ", 16, y, 0xFFF2E8FF, false);
        g.drawString(font, "ОЧКИ  " + AssassinClientState.getSkillPoints(), width - 194, y, 0xFFFCE266, false);
        int buyX = width - 194;
        g.fill(buyX, 28, width - 12, 49, 0xDD241733);
        g.fill(buyX, 28, width - 12, 29, 0xFFB887E8);
        g.drawCenteredString(font, "КУПИТЬ ОЧКО · " + AssassinProgression.EXPERIENCE_LEVELS_PER_POINT + " ур.",
                (buyX + width - 12) / 2, 34, 0xFFFFFFFF);
        if (selectedNode >= 0) {
            var selected = AssassinProgression.NODES.get(selectedNode);
            g.drawString(font, "ВЫБРАНО: " + selected.name() + "  " + effectiveRank(selectedNode)
                            + "/" + selected.maxRanks(),
                    16, height - 26, 0xFFD8B6FF, false);
        } else {
            g.drawString(font, "ВЫБЕРИТЕ НАВЫК ЛЕВОЙ КНОПКОЙ МЫШИ",
                    16, height - 26, 0xFFD8B6FF, false);
        }
        g.drawString(font, "ЛКМ — выбрать  ·  повторный ЛКМ — прокачать  ·  перетаскивание — обзор  ·  колёсико — масштаб",
                16, height - 14, 0xFFB7ADC9, false);
    }

    private int branchIndex(String branch) {
        return switch (branch) { case "Ловкость" -> 0; case "Смертоносность" -> 1; case "Тень" -> 2; default -> 3; };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int buyX = width - 194;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && mouseX >= buyX && mouseX <= width - 12
                && mouseY >= 28 && mouseY <= 49) {
            AssassinNetwork.sendBuySkillPointRequest();
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            panButton = button;
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            double graphMouseX = (mouseX - width / 2.0 - panX) / zoom();
            double graphMouseY = (mouseY - height / 2.0 - panY) / zoom();
            int clicked = findNode(graphMouseX, graphMouseY);
            if (clicked >= 0) {
                if (clicked != selectedNode) {
                    selectedNode = clicked;
                } else if (canUnlock(clicked)) {
                    AssassinNetwork.sendSkillUnlockRequest(clicked);
                }
                return true;
            }
            panButton = button;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == panButton) {
            panX = Mth.clamp(panX + (float) dragX, -width, width);
            panY = Mth.clamp(panY + (float) dragY, -height, height);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == panButton) {
            panButton = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double graphMouseX = (mouseX - width / 2.0 - panX) / zoom();
        double graphMouseY = (mouseY - height / 2.0 - panY) / zoom();
        zoomMultiplier = Mth.clamp(zoomMultiplier * (deltaY > 0 ? 1.12F : 0.89F), 0.42F, 2.5F);
        panX = (float) (mouseX - width / 2.0 - graphMouseX * zoom());
        panY = (float) (mouseY - height / 2.0 - graphMouseY * zoom());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (selectedNode >= 0 && canUnlock(selectedNode)) {
                AssassinNetwork.sendSkillUnlockRequest(selectedNode);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_DOWN) {
            selectNextVisible(1); return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_UP) {
            selectNextVisible(-1); return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void selectNextVisible(int direction) {
        for (int step = 1; step <= AssassinProgression.NODES.size(); step++) {
            int candidate = Math.floorMod(selectedNode + direction * step, AssassinProgression.NODES.size());
            if (isVisible(candidate) && !isRevealAnimating(candidate)) {
                selectedNode = candidate;
                return;
            }
        }
    }

    @Override public boolean isPauseScreen() { return false; }
}
