package net.fabricmc.example;

import net.fabricmc.api.*;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.block.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.*;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.*;
import net.minecraft.entity.projectile.*;
import net.minecraft.text.*;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import org.lwjgl.system.*;

public class ExampleMod implements ClientModInitializer {
	public static FrustumPos currentFrustum;

	@Override
	public void onInitializeClient() {
		HudRenderCallback.EVENT.register(this::displayBoundingBox);
	}

	private void displayBoundingBox(MatrixStack matrixStack, float tickDelta) {
		MinecraftClient client = MinecraftClient.getInstance();
		captureBoundingBox(matrixStack, client, tickDelta);
	}

	public static void captureBoundingBox(MatrixStack matrixStack, MinecraftClient client, float tickDelta) {
		int width = client.getWindow().getScaledWidth();
		int height = client.getWindow().getScaledHeight();
		Vec3d vector = client.cameraEntity.getRotationVec(tickDelta);
		double fov = client.options.fov;
		double angleSize = fov/height;
		Vector3f verticalRotationAxis = new Vector3f(vector);
		verticalRotationAxis.cross(Vector3f.POSITIVE_Y);
		Vector3f horizontalRotationAxis = new Vector3f(vector);
		horizontalRotationAxis.cross(verticalRotationAxis);

		Vec3d center = map((float) angleSize, vector, horizontalRotationAxis, verticalRotationAxis,
				width/2, height/2, width, height);
		HitResult hit = updateTargetedEntity(client, tickDelta, center);
		int minX = width;
		int maxX = 0;
		int minY = height;
		int maxY = 0;

		for(int x = 0; x < width; x++) {
			for(int y = 0; y < height; y++) {
				HitResult nextHit = updateTargetedEntity(client, tickDelta, map((float) angleSize, vector,
						horizontalRotationAxis, verticalRotationAxis, x, y, width, height));
				if(nextHit.getPos().isInRange(hit.getPos(), 1)) {
					if(minX > x) minX = x;
					if(minY > y) minY = y;
					if(maxX < x) maxX = x;
					if(maxY < y) maxY = y;
				}
			}
		}

		drawHollowFill(matrixStack, minX, minY, maxX - minX, maxY - minY, 5, 0xffff0000);
		client.player.sendMessage(new LiteralText("Bounding: ").append(getLabel(hit)), true);
	}

	private static void drawHollowFill(MatrixStack matrixStack, int x, int y, int height, int width, int stroke, int color) {
		matrixStack.push();
		matrixStack.translate(x, y, 0);
		DrawableHelper.fill(matrixStack, 0, 0, width, stroke, color);
		DrawableHelper.fill(matrixStack, width - stroke, 0, width, height, color);
		DrawableHelper.fill(matrixStack, 0, height - stroke, width, height, color);
		DrawableHelper.fill(matrixStack, 0, 0, stroke, height, color);
		matrixStack.pop();
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

	private static Vec3d map(float anglePerPixel, Vec3d center, Vector3f horizontalRotationAxis,
							 Vector3f verticalRotationAxis, int x, int y, int width, int height) {
		float horizontalRotation = (x - width/2) * anglePerPixel;
		float verticalRotation = (y - height/2) * anglePerPixel;

		final Vector3f temp2 = new Vector3f(center);
		temp2.rotate(verticalRotationAxis.getDegreesQuaternion(verticalRotation));
		temp2.rotate(horizontalRotationAxis.getDegreesQuaternion(horizontalRotation));
		return new Vec3d(temp2);
	}


	public static HitResult updateTargetedEntity(MinecraftClient client, float tickDelta, Vec3d direction) {
		Entity entity = client.getCameraEntity();
		if (entity == null || client.world == null) {
			return null;
		}

		double reachDistance = client.interactionManager.getReachDistance();
		HitResult target = rayTrace(entity, reachDistance, tickDelta, false, direction);
		boolean tooFar = false;
		double extendedReach = reachDistance;
		if (client.interactionManager.hasExtendedReach()) {
			extendedReach = 6.0D;
			reachDistance = extendedReach;
		} else {
			if (reachDistance > 3.0D) {
				tooFar = true;
			}
		}

		Vec3d cameraPos = entity.getCameraPosVec(tickDelta);

		extendedReach = extendedReach * extendedReach;
		if (target != null) {
			extendedReach = target.getPos().squaredDistanceTo(cameraPos);
		}

		Vec3d vec3d3 = entity.getCameraPosVec(tickDelta).add(direction.multiply(reachDistance));
		Box box = entity
				.getBoundingBox()
				.stretch(entity.getRotationVec(1.0F).multiply(reachDistance))
				.expand(1.0D, 1.0D, 1.0D);
		EntityHitResult entityHitResult = ProjectileUtil.rayTrace(
				entity,
				entity.getCameraPosVec(tickDelta),
				vec3d3,
				box,
				(entityx) -> !entityx.isSpectator() && entityx.collides(),
				extendedReach
		);

		if (entityHitResult == null) {
			return target;
		}

		Entity entity2 = entityHitResult.getEntity();
		Vec3d vec3d4 = entityHitResult.getPos();
		double g = cameraPos.squaredDistanceTo(vec3d4);
		if (tooFar && g > 9.0D) {
			target = BlockHitResult.createMissed(vec3d4, Direction.getFacing(direction.x, direction.y, direction.z), new BlockPos(vec3d4));
		} else if (g < extendedReach || target == null) {
			target = entityHitResult;
			if (entity2 instanceof LivingEntity || entity2 instanceof ItemFrameEntity) {
				client.targetedEntity = entity2;
			}
		}

		return target;
	}

	private static HitResult rayTrace(
			Entity entity,
			double maxDistance,
			float tickDelta,
			boolean includeFluids,
			Vec3d direction
	) {
		Vec3d end = entity.getCameraPosVec(tickDelta).add(direction.multiply(maxDistance));
		return entity.world.rayTrace(new RayTraceContext(
				entity.getCameraPosVec(tickDelta),
				end,
				RayTraceContext.ShapeType.OUTLINE,
				includeFluids ? RayTraceContext.FluidHandling.ANY : RayTraceContext.FluidHandling.NONE,
				entity
		));
	}
}
