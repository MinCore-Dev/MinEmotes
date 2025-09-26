package dev.minemotes.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.minemotes.config.ConfigManager;
import dev.minemotes.core.EmoteService;
import dev.minemotes.core.EmoteService.Result;
import dev.minemotes.core.EmoteService.Toggle;
import dev.minemotes.core.EmoteType;
import dev.minemotes.perms.Perms;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Registers Fabric commands for all emotes and admin actions. */
public final class CommandRegistrar {
  private CommandRegistrar() {}

  public static void registerAll(EmoteService service, ConfigManager configManager) {
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, environment) -> {
          dispatcher.register(crawlCommand(service));
          dispatcher.register(seatCommand("sit", EmoteType.SIT, service));
          dispatcher.register(seatCommand("chair", EmoteType.CHAIR, service));
          dispatcher.register(seatCommand("lay", EmoteType.LAY, service));
          dispatcher.register(seatCommand("belly", EmoteType.BELLY, service));
          dispatcher.register(rootCommand(configManager));
        });
  }

  private static LiteralArgumentBuilder<ServerCommandSource> crawlCommand(EmoteService service) {
    return CommandManager.literal("crawl")
        .executes(ctx -> executeCrawl(ctx, service, Toggle.TOGGLE))
        .then(
            CommandManager.argument("state", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                  builder.suggest("on");
                  builder.suggest("off");
                  builder.suggest("toggle");
                  return builder.buildFuture();
                })
                .executes(
                    ctx -> {
                      Toggle toggle = Toggle.fromArgument(StringArgumentType.getString(ctx, "state"));
                      return executeCrawl(ctx, service, toggle);
                    }));
  }

  private static LiteralArgumentBuilder<ServerCommandSource> seatCommand(
      String literal, EmoteType type, EmoteService service) {
    return CommandManager.literal(literal).executes(ctx -> executeSeat(ctx, service, type));
  }

  private static LiteralArgumentBuilder<ServerCommandSource> rootCommand(ConfigManager configManager) {
    return CommandManager.literal("minemotes")
        .then(
            CommandManager.literal("reload")
                .executes(
                    ctx -> {
                      ServerCommandSource source = ctx.getSource();
                      ServerPlayerEntity player =
                          source.getEntity() instanceof ServerPlayerEntity playerEntity
                              ? playerEntity
                              : null;
                      if (player != null) {
                        if (!Perms.check(
                            player,
                            configManager.current().permissions().admin(),
                            configManager.current().permissions().fallbackLevel(EmoteType.ADMIN))) {
                          source.sendError(Text.translatable("commands.generic.unknown"));
                          return 0;
                        }
                      } else if (!source.hasPermissionLevel(
                          configManager.current().permissions().fallbackLevel(EmoteType.ADMIN))) {
                        source.sendError(Text.translatable("commands.generic.unknown"));
                        return 0;
                      }
                      configManager.reloadNow();
                      source.sendFeedback(() -> Text.translatable("minemotes.reload.ok"), true);
                      return 1;
                    }));
  }

  private static int executeCrawl(
      CommandContext<ServerCommandSource> ctx, EmoteService service, Toggle toggle)
      throws CommandSyntaxException {
    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
    Result result = service.handleCrawl(player, toggle);
    player.sendMessage(result.message(), false);
    return result.success() ? 1 : 0;
  }

  private static int executeSeat(CommandContext<ServerCommandSource> ctx, EmoteService service, EmoteType type)
      throws CommandSyntaxException {
    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
    Result result = service.handleSeat(player, type);
    player.sendMessage(result.message(), false);
    return result.success() ? 1 : 0;
  }
}
