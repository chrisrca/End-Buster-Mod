package com.example;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import net.minecraft.client.render.VertexConsumerProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer.BEAM_TEXTURE;

public class ExampleModClient implements ClientModInitializer {
	public static long worldSeed = 0;
	private List<BlockPos> beaconPositions = new ArrayList<>();
	private EndCityFinder endCityFinder = new EndCityFinder();
	private BlockPos lastCheckedPos = null;
	private long lastCheckTime = 0;
	private static final long CHECK_INTERVAL = 100;
	private Vec3d prevPlayerPos = null;

	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register(this::registerCommands);

		// Register render event
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(this::onRenderWorld);

		// Register world tick event to set beacon position when the player joins a world
		ClientTickEvents.START_WORLD_TICK.register(world -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.player != null && world.getTime() == 1) {
				if (client.player.getWorld().getRegistryKey() == net.minecraft.world.World.END) {
					checkAndUpdateBeacons();
					prevPlayerPos = client.player.getPos();
				}
			}
		});

		// Register player tick event to periodically check player position
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (client.player != null && client.player.getWorld().getRegistryKey() == net.minecraft.world.World.END) {
				long currentTime = client.world.getTime();
				if (currentTime - lastCheckTime >= CHECK_INTERVAL) {
					checkAndUpdateBeacons();
					lastCheckTime = currentTime;
				}
				if (prevPlayerPos == null) {
					prevPlayerPos = client.player.getPos();
				}
			}
		});
	}

	private void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
		System.out.println("registerCommands called"); // Debug message
		dispatcher.register(ClientCommandManager.literal("setseed")
				.then(ClientCommandManager.argument("seed", LongArgumentType.longArg())
						.executes(context -> {
							worldSeed = LongArgumentType.getLong(context, "seed");
							context.getSource().sendFeedback(Text.literal("Seed set to: " + worldSeed));
							System.out.println("Seed set to: " + worldSeed);  // Print to console
							MinecraftClient client = MinecraftClient.getInstance();
							if (client.player != null) {
								prevPlayerPos = client.player.getPos();
								checkAndUpdateBeaconsOverride();
							}
							return 1;
						}))
		);
	}

	private void checkAndUpdateBeacons() {
		MinecraftClient client = MinecraftClient.getInstance();
		Vec3d playerPos = client.player.getPos();
		BlockPos currentPos = new BlockPos((int) playerPos.x, (int) playerPos.y, (int) playerPos.z);

		if (lastCheckedPos == null || !currentPos.isWithinDistance(lastCheckedPos, 300.0)) {
			lastCheckedPos = currentPos;
			int centerX = (int) playerPos.x;
			int centerZ = (int) playerPos.z;

			beaconPositions.clear();  // Clear all existing beacon positions
			EndCity[] cities = endCityFinder.findEndCities(worldSeed, centerX, centerZ, 2500);
			if (cities != null) {
				for (EndCity city : cities) {
					if (city != null) {
						BlockPos cityPos = new BlockPos(city.getX(), 0, city.getZ());
						beaconPositions.add(cityPos);  // Add new beacon position
						System.out.println("End City at (" + city.getX() + ", " + city.getZ() + "), has ship: " + city.hasShip());  // Print to console
					}
				}
			}
		}
	}

	private void checkAndUpdateBeaconsOverride() {
		MinecraftClient client = MinecraftClient.getInstance();
		Vec3d playerPos = client.player.getPos();
		BlockPos currentPos = new BlockPos((int) playerPos.x, (int) playerPos.y, (int) playerPos.z);

		lastCheckedPos = currentPos;
		int centerX = (int) playerPos.x;
		int centerZ = (int) playerPos.z;

		beaconPositions.clear();  // Clear all existing beacon positions
		EndCity[] cities = endCityFinder.findEndCities(worldSeed, centerX, centerZ, 2500);
		if (cities != null) {
			for (EndCity city : cities) {
				if (city != null) {
					BlockPos cityPos = new BlockPos(city.getX(), 0, city.getZ());
					beaconPositions.add(cityPos);  // Add new beacon position
					System.out.println("End City at (" + city.getX() + ", " + city.getZ() + "), has ship: " + city.hasShip());  // Print to console
				}
			}
		}
	}

	private void onRenderWorld(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null) {
			Vec3d currentPlayerPos = client.player.getPos();
			if (prevPlayerPos == null) {
				prevPlayerPos = currentPlayerPos;
			}
			Vec3d interpolatedPlayerPos = interpolate(prevPlayerPos, currentPlayerPos, client.getTickDelta());
			for (BlockPos beaconPos : beaconPositions) {
				renderBeaconBeam(Objects.requireNonNull(context.matrixStack()), context.consumers(), beaconPos, interpolatedPlayerPos);
				renderFloatingText(Objects.requireNonNull(context.matrixStack()), context.consumers(), beaconPos, interpolatedPlayerPos);
			}
			prevPlayerPos = currentPlayerPos;
		}
	}

	private void renderBeaconBeam(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, BlockPos pos, Vec3d interpolatedPlayerPos) {
		MinecraftClient client = MinecraftClient.getInstance();

		if (client.player != null && client.player.getWorld().getRegistryKey() == net.minecraft.world.World.END) {
			long worldTime = Objects.requireNonNull(client.world).getTime();

			// Translate the matrixStack to the beacon position
			matrixStack.push();
			matrixStack.translate(pos.getX() - interpolatedPlayerPos.x, pos.getY() - interpolatedPlayerPos.y, pos.getZ() - interpolatedPlayerPos.z);

			BeaconBlockEntityRenderer.renderBeam(
					matrixStack,
					vertexConsumerProvider,
					BEAM_TEXTURE,
					client.getTickDelta(),
					1.0F,
					worldTime,
					0,
					256,
					new float[]{133.0F, 209.0F, 66.0F},
					0.2F,
					0.25F
			);

			matrixStack.pop();
		}
	}

	private void renderFloatingText(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, BlockPos pos, Vec3d interpolatedPlayerPos) {
		MinecraftClient client = MinecraftClient.getInstance();
		TextRenderer textRenderer = client.textRenderer;

		// Get the player's position
		Vec3d playerPos = interpolatedPlayerPos;

		// Calculate the point 3/4 of the way from the specified position to the player's position
		double factor = 0.85;
		double x = (1 - factor) * (pos.getX() + 0.5) + factor * playerPos.x;
		double y = (1 - factor) * 120 + factor * playerPos.y;
		double z = (1 - factor) * (pos.getZ() + 0.5) + factor * playerPos.z;

		// Calculate the distance from the camera to the new position
		double dx = x - interpolatedPlayerPos.x;
		double dy = y - interpolatedPlayerPos.y;
		double dz = z - interpolatedPlayerPos.z;

		double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
		float scale = 0.0025F * (float) distance;
		y += scale * 2;

		matrixStack.push();
		matrixStack.translate(dx, dy, dz);
		matrixStack.multiply(client.getEntityRenderDispatcher().getRotation());
		matrixStack.scale(-scale, -scale, scale);
		Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
		int color = 0xFFFFFF; // White color for the text

		RenderSystem.disableDepthTest(); // Disable depth test to render text over everything else
		RenderSystem.enableBlend(); // Enable blending for transparent text
		RenderSystem.defaultBlendFunc(); // Use default blend function

		textRenderer.draw("End City", -textRenderer.getWidth("End City") / 2.0F, 0, color, false, matrix4f, vertexConsumerProvider, TextRenderer.TextLayerType.NORMAL, 0, 15728880);

		RenderSystem.disableBlend(); // Disable blending after rendering text
		RenderSystem.enableDepthTest(); // Re-enable depth test

		matrixStack.pop();
	}

	private Vec3d interpolate(Vec3d start, Vec3d end, float delta) {
		double x = start.x + (end.x - start.x) * delta;
		double y = start.y + (end.y - start.y) * delta;
		double z = start.z + (end.z - start.z) * delta;
		return new Vec3d(x, y, z);
	}
}