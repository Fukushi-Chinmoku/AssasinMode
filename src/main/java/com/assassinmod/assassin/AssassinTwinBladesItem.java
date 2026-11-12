package com.assassinmod.assassin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;

public final class AssassinTwinBladesItem extends SwordItem {
    public AssassinTwinBladesItem(Item.Properties properties) {
        super(Tiers.IRON, properties.attributes(SwordItem.createAttributes(Tiers.IRON, 5.0F, -1.9F)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND) {
            if (!AssassinAbilityManager.hasClass(player)) {
                AssassinAbilityManager.giveClass(player);
            }
            if (player.getOffhandItem().isEmpty()) {
                player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(this));
            }
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "The Twin Blades bind you to the Assassin path. Press V to open your skill tree."));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
