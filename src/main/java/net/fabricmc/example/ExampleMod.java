package net.fabricmc.example;

import net.fabricmc.api.*;
import net.fabricmc.fabric.api.client.keybinding.v1.*;
import net.fabricmc.fabric.api.event.client.*;
import net.minecraft.block.*;
import net.minecraft.client.*;
import net.minecraft.client.options.*;
import net.minecraft.text.*;
import net.minecraft.util.*;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import org.lwjgl.glfw.*;

public class ExampleMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		KeyBinding binding1 = KeyBindingHelper.registerKeyBinding(
				new KeyBinding(
						"key.modid.capture",
						GLFW.GLFW_KEY_R,
						"key.category.all"
				)
		);

		ClientTickCallback.EVENT.register(client -> {
			while (binding1.wasPressed()) {
				captureBoundingBox(client);
			}
		});
	}

	public static void captureBoundingBox(MinecraftClient client) {
		HitResult hit = client.crosshairTarget;

		client.player.sendMessage(new LiteralText("Bounding: ").append(getLabel(hit)), false);
	}

	private static Text getLabel(HitResult hit) {
		if(hit == null) return new LiteralText("null");

		switch (hit.getType()) {
			case BLOCK:
				return getLabelBlock((BlockHitResult) hit);
			case ENTITY:
				return getLabelEntity((EntityHitResult) hit);
			case MISS:
			default:
				return new LiteralText("null");
		}
	}

	private static Text getLabelEntity(EntityHitResult hit) {
		return hit.getEntity().getDisplayName();
	}

	private static Text getLabelBlock(BlockHitResult hit) {
		BlockPos blockPos = hit.getBlockPos();
		BlockState blockState = MinecraftClient.getInstance().world.getBlockState(blockPos);
		Block block = blockState.getBlock();
		return block.getName();
	}
}
