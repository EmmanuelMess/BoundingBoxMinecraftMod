package net.fabricmc.example;

import net.fabricmc.api.*;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.block.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.util.math.*;
import net.minecraft.entity.*;
import net.minecraft.text.*;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;

public class ExampleMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HudRenderCallback.EVENT.register(this::displayBoundingBox);
	}

	private void displayBoundingBox(MatrixStack matrixStack, float tickDelta) {
		MinecraftClient client = MinecraftClient.getInstance();
		//drawHollowFill(matrixStack, 5, 5, 100, 100, 5, 0xffff0000);

		captureBoundingBox(client);
	}

	private void drawHollowFill(MatrixStack matrixStack, int x, int y, int height, int width, int stroke, int color) {
		DrawableHelper.fill(matrixStack, x, y, x + width, y + stroke, color);
		DrawableHelper.fill(matrixStack, x + width - stroke, y, x + width, y + height, color);
		DrawableHelper.fill(matrixStack, x, y + height - stroke, x + width, y + height, color);
		DrawableHelper.fill(matrixStack, x, y, x + stroke, y + height, color);
	}

	public static void captureBoundingBox(MinecraftClient client) {
		HitResult hit = client.crosshairTarget;

		client.player.sendMessage(new LiteralText("Bounding: ").append(getLabel(hit)), true);
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

	private static HitResult rayTrace(Entity entity, double maxDistance, float tickDelta, boolean includeFluids) {
		Vec3d vec3d = entity.getCameraPosVec(tickDelta);
		Vec3d vec3d2 = entity.getRotationVec(tickDelta);
		Vec3d vec3d3 = vec3d.add(vec3d2.x * maxDistance, vec3d2.y * maxDistance, vec3d2.z * maxDistance);
		return entity.world.rayTrace(new RayTraceContext(
				vec3d,
				vec3d3,
				RayTraceContext.ShapeType.OUTLINE,
				includeFluids ? RayTraceContext.FluidHandling.ANY : RayTraceContext.FluidHandling.NONE,
				entity
		));
	}
}
