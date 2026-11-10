package com.assassinmod.assassin;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class AssassinNetwork {
    public static final CustomPacketPayload.Type<AbilityRequest> ABILITY_REQUEST_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AssassinMod.MOD_ID, "ability_request"));

    private AssassinNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(ABILITY_REQUEST_TYPE, AbilityRequest.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                context.enqueueWork(() -> handleAbilityRequest(player, payload));
            }
        });
    }

    public static void sendAbilityRequest(int ability, int targetId) {
        PacketDistributor.sendToServer(new AbilityRequest(ability, targetId));
    }

    private static void handleAbilityRequest(net.minecraft.server.level.ServerPlayer player, AbilityRequest request) {
        if (!AssassinAbilityManager.hasClass(player)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("You are not an Assassin."));
            return;
        }
        switch (request.ability()) {
            case 0 -> {
                var target = player.level().getEntity(request.targetId());
                if (target != null) AssassinAbilityManager.performShadowStep(player, target);
            }
            case 1 -> AssassinAbilityManager.activateShadowVeil(player);
            case 2 -> {
                var target = player.level().getEntity(request.targetId());
                if (target instanceof net.minecraft.world.entity.LivingEntity living) {
                    AssassinAbilityManager.activateMirage(player, living);
                }
            }
            default -> AssassinMod.LOGGER.warn("Ignoring unknown Assassin ability request {}", request.ability());
        }
    }

    public record AbilityRequest(int ability, int targetId) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, AbilityRequest> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, AbilityRequest::ability,
                ByteBufCodecs.VAR_INT, AbilityRequest::targetId,
                AbilityRequest::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ABILITY_REQUEST_TYPE;
        }
    }
}
