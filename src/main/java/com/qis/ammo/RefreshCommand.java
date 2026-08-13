package com.qis.ammo;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

public final class RefreshCommand {

    private RefreshCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("qisammo")
                .then(Commands.literal("refresh")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            AmmoScanner.RescanResult result = AmmoScanner.rescanAndGenerate();
                            ctx.getSource().getServer().getCommands()
                                    .performPrefixedCommand(ctx.getSource(), "reload");
                            Component msg = Component.literal("[qisammo] rescan done: " + result.total()
                                    + " ammo types tracked (+" + result.added() + " new). Resource pack regenerated at "
                                    + "config/qisammo/qisammo_resources.zip - players must reload it in their resource"
                                    + " pack screen");
                            if (!result.restartRequired().isEmpty()) {
                                msg = msg.copy().append(Component.literal("; restart required for new calibers: "
                                        + String.join(", ", result.restartRequired())));
                            }
                            final Component finalMsg = msg;
                            ctx.getSource().sendSuccess(() -> finalMsg, true);
                            return 1;
                        }))
                .then(Commands.literal("give")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("ammo")
                                .then(Commands.argument("target", EntityArgument.players())
                                        .then(Commands.argument("ammoId", ResourceLocationArgument.id())
                                                .executes(ctx -> {
                                                    return giveAmmo(ctx.getSource(),
                                                            EntityArgument.getPlayers(ctx, "target"),
                                                            ResourceLocationArgument.getId(ctx, "ammoId"));
                                                }))))
                        .then(Commands.literal("stamp")
                                .then(Commands.argument("target", EntityArgument.players())
                                        .then(Commands.argument("ammoId", ResourceLocationArgument.id())
                                                .executes(ctx -> {
                                                    return giveStamp(ctx.getSource(),
                                                            EntityArgument.getPlayers(ctx, "target"),
                                                            ResourceLocationArgument.getId(ctx, "ammoId"));
                                                }))))));
    }

    private static int giveAmmo(CommandSourceStack source, Collection<ServerPlayer> players, ResourceLocation ammoId) {
        Item ammo = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("tacz", "ammo"));
        if (ammo == null || ammo == Items.AIR) {
            source.sendFailure(Component.literal("[qisammo] tacz:ammo item not found"));
            return 0;
        }
        CompoundTag customData = new CompoundTag();
        customData.putString("AmmoId", ammoId.toString());
        ItemStack stack = new ItemStack(ammo);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
        for (ServerPlayer player : players) {
            player.getInventory().add(stack.copy());
        }
        source.sendSuccess(() -> Component.literal(
                "[qisammo] gave ammo " + ammoId + " to " + players.size() + " player(s)"), true);
        return players.size();
    }

    private static int giveStamp(CommandSourceStack source, Collection<ServerPlayer> players, ResourceLocation ammoId) {
        String path = AmmoData.sanitize(ammoId.getPath());
        Item stamp = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(QISAmmoMod.MODID, "stamp_" + path));
        if (stamp == null || stamp == Items.AIR) {
            source.sendFailure(Component.literal("[qisammo] no stamp registered for " + ammoId));
            return 0;
        }
        for (ServerPlayer player : players) {
            player.getInventory().add(new ItemStack(stamp));
        }
        source.sendSuccess(() -> Component.literal(
                "[qisammo] gave stamp for " + ammoId + " to " + players.size() + " player(s)"), true);
        return players.size();
    }
}
