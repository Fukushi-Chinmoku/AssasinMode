package com.assassinmod.assassin;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class AssassinNetwork {
    public static final CustomPacketPayload.Type<AbilityRequest> ABILITY_REQUEST_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AssassinMod.MOD_ID, "ability_request"));
    public static final CustomPacketPayload.Type<AbilityState> ABILITY_STATE_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AssassinMod.MOD_ID, "ability_state"));
    public static final CustomPacketPayload.Type<SkillUnlockRequest> SKILL_UNLOCK_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AssassinMod.MOD_ID, "skill_unlock"));
    public static final CustomPacketPayload.Type<BuySkillPointRequest> BUY_SKILL_POINT_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AssassinMod.MOD_ID, "buy_skill_point"));
    public static final CustomPacketPayload.Type<OpenSkillTreeRequest> OPEN_SKILL_TREE_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AssassinMod.MOD_ID, "open_skill_tree"));
    public static final CustomPacketPayload.Type<OpenSkillTreeResult> OPEN_SKILL_TREE_RESULT_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AssassinMod.MOD_ID, "open_skill_tree_result"));
    public static final CustomPacketPayload.Type<ShadowStepCamera> SHADOW_STEP_CAMERA_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AssassinMod.MOD_ID, "shadow_step_camera"));

    private AssassinNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(ABILITY_REQUEST_TYPE, AbilityRequest.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) context.enqueueWork(() -> handleAbilityRequest(player, payload));
        });
        registrar.playToServer(SKILL_UNLOCK_TYPE, SkillUnlockRequest.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) context.enqueueWork(() -> AssassinProgression.unlock(player, payload.nodeIndex()));
        });
        registrar.playToServer(BUY_SKILL_POINT_TYPE, BuySkillPointRequest.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                context.enqueueWork(() -> AssassinProgression.buySkillPoint(player));
            }
        });
        registrar.playToServer(OPEN_SKILL_TREE_TYPE, OpenSkillTreeRequest.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) context.enqueueWork(() -> PacketDistributor.sendToPlayer(
                    player, new OpenSkillTreeResult(AssassinAbilityManager.hasClass(player))));
        });
        registrar.playToClient(ABILITY_STATE_TYPE, AbilityState.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> AssassinClientState.update(
                        payload.assassin(), payload.shadowStepCooldown(), payload.shadowTwinCooldown(),
                        payload.shadowVeilCooldown(), payload.mirageCooldown(), payload.rapidAssaultCooldown(),
                        payload.sprintCooldown(), payload.backstepCooldown(), payload.perfectDodgeCooldown(),
                        payload.skillPoints(), payload.unlockedSkillsMask(), payload.packedRanks())));
        registrar.playToClient(OPEN_SKILL_TREE_RESULT_TYPE, OpenSkillTreeResult.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                        AssassinClient.handleSkillTreeOpen(payload.allowed());
                    }
                }));
        registrar.playToClient(SHADOW_STEP_CAMERA_TYPE, ShadowStepCamera.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                        AssassinClient.handleShadowStepCamera(payload.yaw(), payload.pitch());
                    }
                }));
    }

    public static void syncAbilityState(ServerPlayer player) {
        if (player == null || player.level().getGameTime() % 2L != 0L) return;
        syncAbilityStateNow(player);
    }

    public static void syncAbilityStateNow(ServerPlayer player) {
        if (player == null) return;
        PacketDistributor.sendToPlayer(player, new AbilityState(
                AssassinAbilityManager.hasClass(player),
                AssassinAbilityManager.getCooldown(player, AssassinAbilityManager.TAG_SHADOW_STEP_CD),
                AssassinAbilityManager.getCooldown(player, AssassinAbilityManager.TAG_SHADOW_TWIN_CD),
                AssassinAbilityManager.getCooldown(player, AssassinAbilityManager.TAG_SHADOW_VEIL_CD),
                AssassinAbilityManager.getCooldown(player, AssassinAbilityManager.TAG_MIRAGE_CD),
                AssassinAbilityManager.getCooldown(player, AssassinAbilityManager.TAG_RAPID_ASSAULT_CD),
                AssassinAbilityManager.getCooldown(player, AssassinAbilityManager.TAG_SPRINT_CD),
                AssassinAbilityManager.getCooldown(player, AssassinAbilityManager.TAG_BACKSTEP_CD),
                AssassinAbilityManager.getCooldown(player, AssassinAbilityManager.TAG_PERFECT_DODGE_CD),
                AssassinProgression.getPoints(player), AssassinProgression.getUnlockedMask(player),
                AssassinProgression.getPackedRanks(player)));
    }

    public static void sendAbilityRequest(int ability, int targetId) {
        PacketDistributor.sendToServer(new AbilityRequest(ability, targetId));
    }

    public static void sendSkillUnlockRequest(int nodeIndex) {
        PacketDistributor.sendToServer(new SkillUnlockRequest(nodeIndex));
    }

    public static void sendBuySkillPointRequest() {
        PacketDistributor.sendToServer(new BuySkillPointRequest());
    }

    public static void sendOpenSkillTreeRequest() {
        PacketDistributor.sendToServer(new OpenSkillTreeRequest());
    }

    public static void sendShadowStepCamera(ServerPlayer player, float yaw, float pitch) {
        PacketDistributor.sendToPlayer(player, new ShadowStepCamera(yaw, pitch));
    }

    private static void handleAbilityRequest(ServerPlayer player, AbilityRequest request) {
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
                if (target instanceof net.minecraft.world.entity.LivingEntity living) AssassinAbilityManager.activateMirage(player, living);
            }
            case 3 -> {
                var target = player.level().getEntity(request.targetId());
                if (target != null) AssassinAbilityManager.performShadowStep(player, target, true);
            }
            case 4 -> AssassinAbilityManager.activateRapidAssault(player);
            case 5 -> AssassinAbilityManager.activateSprint(player);
            case 6 -> AssassinAbilityManager.activateBackstep(player);
            case 7 -> AssassinAbilityManager.activatePerfectDodge(player);
            default -> AssassinMod.LOGGER.warn("Ignoring unknown Assassin ability request {}", request.ability());
        }
    }

    public record AbilityRequest(int ability, int targetId) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, AbilityRequest> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, AbilityRequest::ability,
                ByteBufCodecs.VAR_INT, AbilityRequest::targetId,
                AbilityRequest::new);
        @Override public Type<? extends CustomPacketPayload> type() { return ABILITY_REQUEST_TYPE; }
    }

    public record AbilityState(boolean assassin, int shadowStepCooldown, int shadowTwinCooldown,
                               int shadowVeilCooldown, int mirageCooldown, int rapidAssaultCooldown,
                               int sprintCooldown, int backstepCooldown, int perfectDodgeCooldown,
                               int skillPoints, int unlockedSkillsMask, long packedRanks) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, AbilityState> STREAM_CODEC = StreamCodec.of(
                (buffer, state) -> {
                    buffer.writeBoolean(state.assassin());
                    buffer.writeVarInt(state.shadowStepCooldown());
                    buffer.writeVarInt(state.shadowTwinCooldown());
                    buffer.writeVarInt(state.shadowVeilCooldown());
                    buffer.writeVarInt(state.mirageCooldown());
                    buffer.writeVarInt(state.rapidAssaultCooldown());
                    buffer.writeVarInt(state.sprintCooldown());
                    buffer.writeVarInt(state.backstepCooldown());
                    buffer.writeVarInt(state.perfectDodgeCooldown());
                    buffer.writeVarInt(state.skillPoints());
                    buffer.writeVarInt(state.unlockedSkillsMask());
                    buffer.writeLong(state.packedRanks());
                },
                buffer -> new AbilityState(buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readLong()));
        @Override public Type<? extends CustomPacketPayload> type() { return ABILITY_STATE_TYPE; }
    }

    public record SkillUnlockRequest(int nodeIndex) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, SkillUnlockRequest> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SkillUnlockRequest::nodeIndex, SkillUnlockRequest::new);
        @Override public Type<? extends CustomPacketPayload> type() { return SKILL_UNLOCK_TYPE; }
    }

    public record BuySkillPointRequest() implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, BuySkillPointRequest> STREAM_CODEC =
                StreamCodec.unit(new BuySkillPointRequest());
        @Override public Type<? extends CustomPacketPayload> type() { return BUY_SKILL_POINT_TYPE; }
    }

    public record OpenSkillTreeRequest() implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenSkillTreeRequest> STREAM_CODEC = StreamCodec.unit(new OpenSkillTreeRequest());
        @Override public Type<? extends CustomPacketPayload> type() { return OPEN_SKILL_TREE_TYPE; }
    }

    public record OpenSkillTreeResult(boolean allowed) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenSkillTreeResult> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, OpenSkillTreeResult::allowed, OpenSkillTreeResult::new);
        @Override public Type<? extends CustomPacketPayload> type() { return OPEN_SKILL_TREE_RESULT_TYPE; }
    }

    public record ShadowStepCamera(float yaw, float pitch) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, ShadowStepCamera> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, ShadowStepCamera::yaw,
                ByteBufCodecs.FLOAT, ShadowStepCamera::pitch,
                ShadowStepCamera::new);
        @Override public Type<? extends CustomPacketPayload> type() { return SHADOW_STEP_CAMERA_TYPE; }
    }

}
