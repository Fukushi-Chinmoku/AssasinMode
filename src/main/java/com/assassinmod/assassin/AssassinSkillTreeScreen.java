package com.assassinmod.assassin;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    private static final String[] ICONS = {
            "boots_gold", "eye_green", "potion_cyan", "arrow_diamond",
            "sword_iron", "skull", "glove_iron", "potion_black",
            "potion_indigo", "sword_gold", "eye_green", "treasure_chest",
            "potion_indigo", "arrow_steel"
    };
    private static final double[][] POSITIONS = {
            {250, 0}, {250, -93}, {78, 0}, {250, 93},
            {250, -155}, {-38, 0}, {250, 155},
            {250, -260}, {9, -225}, {-113, -130}, {-233, 0},
            {-113, 130}, {9, 225}, {250, 260}
    };
    private static final int[] BRANCH_COLORS = {0xFF7CCBFF, 0xFFFF737E, 0xFFB483FF, 0xFF86E1C3};
    private static final String[] BRANCH_NAMES = {"ЛОВКОСТЬ", "СМЕРТОНОСНОСТЬ", "ТЕНЬ", "ОБМАН"};
    private static final int GRAPH_WIDTH = 540;
    private static final int GRAPH_HEIGHT = 580;
    private static final long REVEAL_DURATION_MS = 620L;
    private static final int UNLOCK_VFX_FRAME_COUNT = UNLOCK_VFX.length * 2 - 1;
    private static final long UNLOCK_VFX_FRAME_DURATION_MS = 55L;

    private int selectedNode;
    private int hoveredNode = -1;
    private float sceneTime;
    private float zoomMultiplier = 1.0F;
    private float panX;
    private float panY;
    private int panButton = -1;
    private boolean panelOpen = true;
    private int unlockX;
    private int unlockY;
    private int unlockWidth;
    private int unlockHeight;
    private final Set<Integer> pendingUnlocks = new LinkedHashSet<>();
    private int confirmX, confirmY, confirmWidth, actionHeight;
    private int cancelX, cancelWidth;
    private int observedSkillMask;
    private int unlockVfxNode = -1;
    private long unlockVfxStartedAt;
    private final List<Integer> queuedUnlockVfx = new ArrayList<>();
    private int observedVisibleMask;
    private final List<RevealAnimation> revealAnimations = new ArrayList<>();
    private record RevealAnimation(int from, int to, long startAt) {}

    public AssassinSkillTreeScreen() {
        super(Component.literal("Assassin Skill Tree"));
        observedSkillMask = AssassinClientState.getUnlockedSkillsMask();
        observedVisibleMask = visibleMask();
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(AssassinMod.MOD_ID, "textures/passive/" + path);
    }

    private int panelWidth() { return panelOpen ? Math.min(340, Math.max(220, width / 4)) : 0; }
    private int panelX() { return width - panelWidth(); }
    private int graphWidth() { return panelX(); }
    private int panelToggleX() { return width - 32; }
    private float fitZoom() {
        float availableWidth = Math.max(1F, graphWidth() - 24F);
        float availableHeight = Math.max(1F, height - 72F);
        return Math.min(availableWidth / GRAPH_WIDTH, availableHeight / GRAPH_HEIGHT);
    }

    private float zoom() { return fitZoom() * zoomMultiplier; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        sceneTime += Math.max(partialTick, 0.04F);
        float reveal = Mth.clamp(sceneTime / 16F, 0F, 1F);
        detectNewUnlocks();
        if (!isVisible(selectedNode)) selectedNode = 0;
        drawBackdrop(g);
        drawGraph(g, mouseX, mouseY, reveal);
        drawHeader(g, reveal);
        drawDetails(g, reveal);
        drawPanelToggle(g);
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
        int clipRight = graphWidth();
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
        if (index == 0 || index == 1 || index == 4 || index == 7 || index == 10) return 48;
        if (index == 5 || index == 6 || index == 8 || index == 9 || index == 11) return 40;
        return 32;
    }

    private int frameType(int index) {
        if (index == 0 || index == 1 || index == 4 || index == 7 || index == 10) return 0;
        if (index == 5 || index == 6 || index == 8 || index == 9 || index == 11) return 2;
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
            float pulse = 0.5F + 0.5F * (float) Math.sin(sceneTime / 3.0F + index * 0.7F);
            float haloAlpha = 0.18F + pulse * 0.20F;
            int haloSize = (int) (size * (1.35F + pulse * 0.12F));
            int haloOffset = (haloSize - size) / 2;
            g.setColor(red, green, blue, haloAlpha);
            g.blit(frame, x - haloOffset, y - haloOffset, haloSize, haloSize,
                    size * 2, 0, size, size, size * 3, size);
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
        int iconSize = index == 0 || index == 4 || index == 7 || index == 10 ? 20 : 16;
        if (!unlocked) g.setColor(0.48F, 0.49F, 0.53F, 0.9F);
        g.blit(icon, (int) cx - iconSize / 2, (int) cy - iconSize / 2,
                iconSize, iconSize, 0, 0, 16, 16, 16, 16);
        g.setColor(1F, 1F, 1F, 1F);
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
            startAt = Math.max(startAt, animation.startAt() + REVEAL_DURATION_MS);
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
        revealAnimations.removeIf(animation -> now >= animation.startAt() + REVEAL_DURATION_MS);
    }

    private int visibleMask() {
        int mask = 1; // Keep only the entry node visible before progression begins.
        for (int i = 0; i < AssassinProgression.NODES.size(); i++) {
            if (AssassinClientState.isSkillUnlocked(i)) mask |= 1 << i;
        }
        for (int i = 1; i < AssassinProgression.NODES.size(); i++) {
            if (pendingUnlocks.contains(i)) {
                mask |= 1 << i;
                continue;
            }
            if (AssassinClientState.isSkillUnlocked(i)
                    || remainingPoints() < AssassinProgression.NODES.get(i).cost()) continue;
            if (learnedParent(i) >= 0) mask |= 1 << i;
        }
        return mask;
    }

    private int learnedParent(int index) {
        for (int[] edge : AssassinProgression.CONNECTIONS) {
            if (edge[1] == index && AssassinClientState.isSkillUnlocked(edge[0])) return edge[0];
            if (edge[0] == index && AssassinClientState.isSkillUnlocked(edge[1])) return edge[1];
        }
        return -1;
    }

    private boolean isVisible(int index) {
        return (visibleMask() & (1 << index)) != 0;
    }

    private boolean isRevealAnimating(int index) {
        long now = Util.getMillis();
        return revealAnimations.stream().anyMatch(animation -> animation.to() == index
                && now < animation.startAt() + REVEAL_DURATION_MS);
    }

    private boolean isPlayingUnlockVfx(int index) {
        return unlockVfxNode == index
                && Util.getMillis() < unlockVfxStartedAt + UNLOCK_VFX_FRAME_COUNT * UNLOCK_VFX_FRAME_DURATION_MS;
    }

    private void drawRevealAnimations(GuiGraphics g) {
        long now = Util.getMillis();
        revealAnimations.removeIf(animation -> now >= animation.startAt() + REVEAL_DURATION_MS);
        for (RevealAnimation animation : revealAnimations) {
            float progress = Mth.clamp((now - animation.startAt()) / (float) REVEAL_DURATION_MS, 0F, 1F);
            if (progress > 0F && progress < 1F) {
                drawElectricPulse(g, animation.from(), animation.to(), progress, branchColor(animation.to()));
            }
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
        g.fill(0, -3, head, 3, 0x55000000 | rgb);
        g.fill(head - 5, -5, head + 5, 5, 0x88000000 | rgb);
        g.fill(head - 2, -2, head + 3, 3, 0xFFBFF8FF);
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
        return AssassinClientState.isSkillUnlocked(index) || pendingUnlocks.contains(index);
    }

    private boolean canUnlock(int index) {
        var node = AssassinProgression.NODES.get(index);
        if (isLearned(index) || remainingPoints() < node.cost()) return false;
        for (int[] edge : AssassinProgression.CONNECTIONS) {
            if ((edge[0] == index && isLearned(edge[1])) || (edge[1] == index && isLearned(edge[0]))) return true;
        }
        return node.prerequisite() < 0;
    }

    private int remainingPoints() {
        int reserved = pendingUnlocks.stream().mapToInt(i -> AssassinProgression.NODES.get(i).cost()).sum();
        return AssassinClientState.getSkillPoints() - reserved;
    }

    private void drawHeader(GuiGraphics g, float reveal) {
        int y = 14 - (int) ((1F - reveal) * 8);
        g.drawString(font, "АССАСИН // ДРЕВО НАВЫКОВ", 16, y, 0xFFF2E8FF, false);
        int pointsX = panelOpen ? panelX() - 150 : width - 220;
        g.drawString(font, "ОЧКИ НАВЫКОВ  " + AssassinClientState.getSkillPoints(), pointsX, y, 0xFFFCE266, false);
        g.drawString(font, "ТЯНИТЕ ПУСТОЕ МЕСТО — КАМЕРА   КОЛЁСИКО — МАСШТАБ   P — ПАНЕЛЬ   ENTER — ВЫБРАТЬ", 16, height - 14, 0xFFB7ADC9, false);
    }

    private void drawDetails(GuiGraphics g, float reveal) {
        if (!panelOpen) {
            unlockX = -1;
            return;
        }
        int x = panelX();
        int panelWidth = width - x;
        int y = 0;
        int slide = (int) ((1F - reveal) * 22);
        g.pose().pushPose();
        g.pose().translate(slide, 0, 0);
        g.fill(x, y, width, height, 0xDD0B0912);
        g.fill(x, y, x + 1, height, 0xAA7B6798);
        int textX = x + 14;
        int textW = Math.max(80, panelWidth - 28);
        int lineY = 18;
        var node = AssassinProgression.NODES.get(selectedNode);
        int color = BRANCH_COLORS[branchIndex(node.branch())];
        g.drawString(font, "ВЫБРАННЫЙ НАВЫК", textX, lineY, 0xFFAFA4BF, false); lineY += 20;
        g.drawString(font, node.name(), textX, lineY, color, false); lineY += 16;
        g.drawString(font, BRANCH_NAMES[branchIndex(node.branch())] + "  /  ЦЕНА " + node.cost(), textX, lineY,
                0xFFE1D6EE, false); lineY += 20;
        for (var wrapped : font.split(Component.literal(node.description()), textW)) {
            g.drawString(font, wrapped, textX, lineY, 0xFFECE5F3, false);
            lineY += 11;
        }

        lineY += 10;
        boolean unlocked = AssassinClientState.isSkillUnlocked(selectedNode);
        var status = AssassinClientState.isSkillUnlocked(selectedNode) ? "ИЗУЧЕНО"
                : pendingUnlocks.contains(selectedNode) ? "ОЖИДАЕТ ПОДТВЕРЖДЕНИЯ"
                : canUnlock(selectedNode) ? "МОЖНО ИЗУЧИТЬ"
                : !AssassinProgression.isReachable(null, selectedNode) && !hasLearnedNeighbor(selectedNode)
                ? "СВЯЖИТЕ С ИЗУЧЁННЫМ НАВЫКОМ" : "НУЖНО ОЧКОВ: " + Math.max(0, node.cost() - remainingPoints());
        g.drawString(font, status, textX, lineY, unlocked ? 0xFF8CF6B0 : canUnlock(selectedNode) ? 0xFFFCE266 : 0xFFC1B6CF, false);
        lineY += 18;
        unlockX = textX; unlockY = lineY; unlockWidth = textW; unlockHeight = 22;
        boolean available = canUnlock(selectedNode);
        if (!pendingUnlocks.isEmpty()) {
            confirmX = unlockX; confirmY = unlockY; confirmWidth = Math.max(70, (textW - 6) / 2);
            cancelX = confirmX + confirmWidth + 6; cancelWidth = textW - confirmWidth - 6; actionHeight = unlockHeight;
            g.fill(confirmX, confirmY, confirmX + confirmWidth, confirmY + actionHeight, 0xFF57336F);
            g.fill(confirmX, confirmY, confirmX + confirmWidth, confirmY + 1, 0xFFE6B5FF);
            g.drawCenteredString(font, "ПОДТВЕРДИТЬ (" + pendingUnlocks.size() + ")", confirmX + confirmWidth / 2, confirmY + 7, 0xFFFFFFFF);
            g.fill(cancelX, confirmY, cancelX + cancelWidth, confirmY + actionHeight, 0xFF292431);
            g.drawCenteredString(font, "ОТМЕНА", cancelX + cancelWidth / 2, confirmY + 7, 0xFFFFFFFF);
        } else {
            g.fill(unlockX, unlockY, unlockX + unlockWidth, unlockY + unlockHeight, available ? 0xFF57336F : 0xFF292431);
        g.fill(unlockX, unlockY, unlockX + unlockWidth, unlockY + 1, available ? 0xFFE6B5FF : 0xFF61576D);
        g.drawCenteredString(font, AssassinClientState.isSkillUnlocked(selectedNode) ? "ИЗУЧЕНО" : available ? "ИЗУЧИТЬ" : "ЗАКРЫТО",
                unlockX + unlockWidth / 2, unlockY + 7, 0xFFFFFFFF);
        }

        int milestoneY = unlockY + 38;
        g.drawString(font, "ОЧКИ ЗА ДОСТИЖЕНИЯ", textX, milestoneY, 0xFFAA92C6, false); milestoneY += 15;
        for (int i = 0; i < AssassinProgression.MILESTONES.size(); i++) {
            if (milestoneY + 10 >= height - 18) break;
            var milestone = AssassinProgression.MILESTONES.get(i);
            boolean earned = AssassinClientState.hasEarnedMilestone(i);
            String line = (earned ? "✓ " : "· ") + milestone.title() + "  +" + milestone.points();
            g.drawString(font, line, textX, milestoneY, earned ? 0xFF8CF6B0 : 0xFFD0C7D9, false);
            milestoneY += 11;
        }
        g.pose().popPose();
    }

    private void drawPanelToggle(GuiGraphics g) {
        int x = panelToggleX();
        g.fill(x, 10, x + 24, 34, 0xDD17121F);
        g.fill(x, 10, x + 24, 11, 0xAA8E78AC);
        g.drawCenteredString(font, panelOpen ? "×" : "›", x + 12, 17, 0xFFF2E8FF);
    }

    private int branchIndex(String branch) {
        return switch (branch) { case "Ловкость" -> 0; case "Смертоносность" -> 1; case "Тень" -> 2; default -> 3; };
    }

    private boolean hasLearnedNeighbor(int index) {
        for (int[] edge : AssassinProgression.CONNECTIONS) {
            if ((edge[0] == index && isLearned(edge[1])) || (edge[1] == index && isLearned(edge[0]))) return true;
        }
        return false;
    }

    private void confirmPending() {
        for (int index : pendingUnlocks) AssassinNetwork.sendSkillUnlockRequest(index);
        pendingUnlocks.clear();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && mouseX >= panelToggleX() && mouseX <= width
                && mouseY >= 10 && mouseY <= 34) {
            panelOpen = !panelOpen;
            return true;
        }
        if (panelOpen && mouseX >= panelX()) return super.mouseClicked(mouseX, mouseY, button);
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            panButton = button;
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (panelOpen && !pendingUnlocks.isEmpty() && mouseY >= confirmY && mouseY <= confirmY + actionHeight) {
                if (mouseX >= confirmX && mouseX <= confirmX + confirmWidth) confirmPending();
                else if (mouseX >= cancelX && mouseX <= cancelX + cancelWidth) pendingUnlocks.clear();
                return true;
            }
            if (panelOpen && unlockX >= 0 && mouseX >= unlockX && mouseX <= unlockX + unlockWidth && mouseY >= unlockY && mouseY <= unlockY + unlockHeight) {
                if (canUnlock(selectedNode)) pendingUnlocks.add(selectedNode);
                return true;
            }
            double graphMouseX = (mouseX - graphWidth() / 2.0 - panX) / zoom();
            double graphMouseY = (mouseY - height / 2.0 - panY) / zoom();
            int clicked = findNode(graphMouseX, graphMouseY);
            if (clicked >= 0) { selectedNode = clicked; return true; }
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
        double graphMouseX = (mouseX - graphWidth() / 2.0 - panX) / zoom();
        double graphMouseY = (mouseY - height / 2.0 - panY) / zoom();
        zoomMultiplier = Mth.clamp(zoomMultiplier * (deltaY > 0 ? 1.12F : 0.89F), 0.42F, 2.5F);
        panX = (float) (mouseX - graphWidth() / 2.0 - graphMouseX * zoom());
        panY = (float) (mouseY - height / 2.0 - graphMouseY * zoom());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_P) { panelOpen = !panelOpen; return true; }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (!pendingUnlocks.isEmpty()) confirmPending();
            else if (canUnlock(selectedNode)) pendingUnlocks.add(selectedNode);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !pendingUnlocks.isEmpty()) { pendingUnlocks.clear(); return true; }
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
