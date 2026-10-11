package com.qis.ammo;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /qisammo refresh} - re-scans the tacz gun pack folder, regenerates the
 * Create recipe JSONs and item model assets, then reloads datapacks.
 * New gun packs that add new ammo types still require a restart (items cannot
 * be registered at runtime).
 */
public final class RefreshCommand {

    private RefreshCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("qisammo")
                .then(Commands.literal("refresh")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            AmmoScanner.rescanAndGenerate();
                            ctx.getSource().getServer().getCommands()
                                    .performPrefixedCommand(ctx.getSource(), "reload");
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "[qisammo] regenerated recipes & models, reloading datapacks"), true);
                            return 1;
                        })));
    }
}
