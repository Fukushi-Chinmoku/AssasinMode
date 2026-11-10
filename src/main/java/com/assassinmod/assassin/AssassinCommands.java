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
        event.getDispatcher().register(root);
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
