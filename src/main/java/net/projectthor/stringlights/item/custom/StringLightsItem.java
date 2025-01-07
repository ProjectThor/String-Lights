package net.projectthor.stringlights.item.custom;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.projectthor.stringlights.StringLights;
import net.projectthor.stringlights.entity.LightKnotEntity;
import net.projectthor.stringlights.light.LightLink;
import net.projectthor.stringlights.tag.ModTags;

import java.util.List;


public class StringLightsItem extends Item {
    public StringLightsItem(Settings settings) {
        super(settings);
    }

    public static ActionResult lightUseEvent(PlayerEntity player, World world, Hand hand, BlockHitResult blockHitResult) {
        if (player == null || player.isSneaking()) return ActionResult.PASS;
        ItemStack stack = player.getStackInHand(hand);
        BlockPos blockPos = blockHitResult.getBlockPos();
        BlockState blockState= world.getBlockState(blockPos);

        if (!LightKnotEntity.canAttachTo(blockState)) {
            return ActionResult.PASS;
        } else if (world.isClient) {
            ItemStack handItem = player.getStackInHand(hand);
            if (handItem.isIn(ModTags.LIGHT_ITEMS)) {
                return ActionResult.SUCCESS;
            }

            if (!LightKnotEntity.getHeldLightsInRange(player, blockPos).isEmpty()) {
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        }

        LightKnotEntity knot = LightKnotEntity.getKnotAt(world, blockPos);
        if (knot != null) {
            if (knot.interact(player, hand) == ActionResult.CONSUME) {
                return ActionResult.CONSUME;
            }
            return ActionResult.PASS;
        }

        List<LightLink> attachableLights = LightKnotEntity.getHeldLightsInRange(player, blockPos);

        Item knotType = stack.getItem();

        if (attachableLights.isEmpty() && !stack.isIn(ModTags.LIGHT_ITEMS)) {
            return ActionResult.PASS;
        }

        if (!stack.isIn(ModTags.LIGHT_ITEMS)) {
            knotType = attachableLights.getFirst().getSourceItem();
        }

        knot = new LightKnotEntity(world, blockPos, knotType);
        knot.setGraceTicks((byte) 0);
        world.spawnEntity(knot);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return knot.interact(player, hand);
    }

    @Environment(EnvType.CLIENT)
    public static void infoToolTip(ItemStack itemStack, Item.TooltipContext tooltipContext, TooltipType tooltipType, List<Text> texts) {
        if (StringLights.runtimeConfig.doShowToolTip()) {
            if (itemStack.isIn(ModTags.LIGHT_ITEMS)) {
                if (Screen.hasShiftDown()) {
                    texts.add(1, Text.translatable("message.stringlights.string_lights_detailed").formatted(Formatting.AQUA));
                } else {
                    texts.add(1, Text.translatable("message.stringlights.string_lights").formatted(Formatting.YELLOW));
                }
            }
        }
    }
}
