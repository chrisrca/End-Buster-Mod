package com.example;

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
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

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
								if (client.player.getWorld().getRegistryKey() == net.minecraft.world.World.END) {
									checkAndUpdateBeacons();
								}
							}
							return 1;
						}))
		);
	}

	private void checkAndUpdateBeacons() {
		MinecraftClient client = MinecraftClient.getInstance();
		Vec3d playerPos = client.player.getPos();
		BlockPos currentPos = new BlockPos((int) playerPos.x, (int) playerPos.y, (int) playerPos.z);

		if (lastCheckedPos == null || currentPos.isWithinDistance(lastCheckedPos, 300.0)) {
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

	private void onRenderWorld(WorldRenderContext context) {
		for (BlockPos beaconPos : beaconPositions) {
			renderBeaconBeam(Objects.requireNonNull(context.matrixStack()), context.consumers(), beaconPos, context.camera().getPos());
		}
	}

	private void renderBeaconBeam(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, BlockPos pos, Vec3d cameraPos) {
		MinecraftClient client = MinecraftClient.getInstance();

		if (client.player != null && client.player.getWorld().getRegistryKey() == net.minecraft.world.World.END) {
			long worldTime = Objects.requireNonNull(client.world).getTime();

			// Translate the matrixStack to the beacon position
			matrixStack.push();
			matrixStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

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
}