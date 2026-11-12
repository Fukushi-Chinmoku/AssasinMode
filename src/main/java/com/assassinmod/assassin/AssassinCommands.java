package com.assassinmod.assassin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class AssassinCommands {
    private AssassinCommands() {
    }

    public static void register(final RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("assassin")
                .then(Commands.literal("give").executes(context -> giveClass(context)))
                .then(Commands.literal("cooldown").then(Commands.literal("reset").executes(context -> resetCooldowns(context))))
                .then(Commands.literal("test").then(Commands.literal("backstab").executes(context -> testBackstab(context)))
                        .then(Commands.literal("shadowstep").executes(context -> testShadowStep(context)))
                        .then(Commands.literal("veil").executes(context -> testVeil(context)))
                        .then(Commands.literal("mirage").executes(context -> testMirage(context))));
        root.then(Commands.literal("blades").requires(source -> source.hasPermission(2))
                .executes(context -> giveTwinBlades(context)));
        root.then(Commands.literal("tree")
                .then(Commands.literal("rewards").executes(context -> showMilestones(context)))
                .then(Commands.literal("testpoints")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                                .executes(context -> grantTestPoints(context))))
                .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> resetSkillTree(context)))
                .then(Commands.literal("respec").executes(context -> respecSkillTree(context))));
        event.getDispatcher().register(root);
    }

    private static int giveTwinBlades(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        player.getInventory().add(new ItemStack(AssassinItems.TWIN_BLADES.get()));
        context.getSource().sendSuccess(() -> Component.literal("Twin Blades added to your inventory."), false);
        return 1;
    }

    private static int showMilestones(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("Assassin skill points come only from these milestones:"), false);
        for (AssassinProgression.Milestone milestone : AssassinProgression.MILESTONES) {
            context.getSource().sendSuccess(() -> Component.literal("• " + milestone.title() + " — "
                    + milestone.advancement() + " (" + milestone.points() + " point(s))"), false);
        }
        return AssassinProgression.MILESTONES.size();
    }

    private static int grantTestPoints(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;
        int amount = IntegerArgumentType.getInteger(context, "amount");
        AssassinProgression.addPoints(player, amount);
        player.sendSystemMessage(Component.literal("[Skill Tree Test] Added " + amount + " temporary skill points."));
        return amount;
    }

    private static int resetSkillTree(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;
        AssassinProgression.resetForTesting(player);
        player.sendSystemMessage(Component.literal("[Skill Tree Test] Points, unlocked skills and milestone claims reset."));
        return 1;
    }

    private static int respecSkillTree(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;
        int refunded = AssassinProgression.respec(player);
        player.sendSystemMessage(Component.literal("Skill tree reset. Refunded " + refunded
                + " point(s); achievement rewards remain claimed."));
        return 1;
    }

    private static int giveClass(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        AssassinAbilityManager.giveClass(player);
        context.getSource().sendSuccess(() -> Component.literal("Assassin class granted."), true);
        return 1;
    }

    private static int resetCooldowns(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }
        AssassinAbilityManager.resetCooldowns(player);
        context.getSource().sendSuccess(() -> Component.literal("Cooldowns reset."), true);
        return 1;
    }

    private static int testBackstab(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;
        AssassinAbilityManager.giveClass(player);
        player.sendSystemMessage(Component.literal("Backstab test ready. Attack from behind to trigger the passive."));
        return 1;
    }

    private static int testShadowStep(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;
        Entity target = null;
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(100.0D))) {
            if (entity instanceof LivingEntity && !(entity == player) && !(entity instanceof ServerPlayer)) {
                target = entity;
                break;
            }
        }
        if (target == null) {
            player.sendSystemMessage(Component.literal("No nearby valid target found for Shadow Step."));
            return 0;
        }
        AssassinAbilityManager.performShadowStep(player, target);
        return 1;
    }

    private static int testVeil(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;
        AssassinAbilityManager.activateShadowVeil(player);
        return 1;
    }

    private static int testMirage(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;
        LivingEntity target = null;
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(30.0D))) {
            if (entity instanceof LivingEntity living && !(entity == player)) {
                target = living;
                break;
            }
        }
        if (target == null) {
            player.sendSystemMessage(Component.literal("No valid target found for Mirage."));
            return 0;
        }
        AssassinAbilityManager.activateMirage(player, target);
        return 1;
    }
}
