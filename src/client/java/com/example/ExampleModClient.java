package com.example;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
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

import java.util.Objects;

import static net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer.BEAM_TEXTURE;

public class ExampleModClient implements ClientModInitializer {
	public static long worldSeed = 0;
	private BlockPos beaconPos = null;

	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register(this::registerCommands);

		// Register render event
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(this::onRenderWorld);
	}

	private void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
		System.out.println("registerCommands called"); // Debug message
		dispatcher.register(ClientCommandManager.literal("setseed")
				.then(ClientCommandManager.argument("seed", LongArgumentType.longArg())
						.executes(context -> {
							worldSeed = LongArgumentType.getLong(context, "seed");
							context.getSource().sendFeedback(Text.literal("Seed set to: " + worldSeed));
							System.out.println("Seed set to: " + worldSeed);  // Print to console
							return 1;
						})));
		dispatcher.register(ClientCommandManager.literal("beaconbeam")
				.then(ClientCommandManager.argument("x", DoubleArgumentType.doubleArg())
						.then(ClientCommandManager.argument("y", DoubleArgumentType.doubleArg())
								.then(ClientCommandManager.argument("z", DoubleArgumentType.doubleArg())
										.executes(context -> {
											double x = DoubleArgumentType.getDouble(context, "x");
											double y = DoubleArgumentType.getDouble(context, "y");
											double z = DoubleArgumentType.getDouble(context, "z");

											// Set the beacon position
											beaconPos = new BlockPos((int) x, (int) y, (int) z);

											context.getSource().sendFeedback(Text.literal("Beacon beam set at: " + x + ", " + y + ", " + z));
											System.out.println("Beacon beam set at: " + x + ", " + y + ", " + z);  // Print to console

											return 1;
										})))));
	}

	private void onRenderWorld(WorldRenderContext context) {
		if (beaconPos != null) {
			renderBeaconBeam(Objects.requireNonNull(context.matrixStack()), context.consumers(), beaconPos, context.camera().getPos());
		}
	}

	private void renderBeaconBeam(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, BlockPos pos, Vec3d cameraPos) {
		MinecraftClient client = MinecraftClient.getInstance();

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
				new float[]{1.0F, 1.0F, 1.0F},
				0.2F,
				0.25F
		);

		matrixStack.pop();
	}
}
