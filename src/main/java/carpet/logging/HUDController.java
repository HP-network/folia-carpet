package carpet.logging;

import carpet.CarpetServer;
import carpet.folia.FoliaRuntime;
import carpet.helpers.HopperCounter;
import carpet.logging.logHelpers.PacketCounter;
import carpet.utils.Messenger;
import carpet.utils.SpawnReporter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TimeUtil;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class HUDController
{
    private static final List<Consumer<MinecraftServer>> HUDListeners = new CopyOnWriteArrayList<>();

    private static int hudTick = 0;

    public static void register(Consumer<MinecraftServer> listener)
    {
        HUDListeners.add(listener);
    }

    public static final Map<ServerPlayer, List<Component>> player_huds = new ConcurrentHashMap<>();

    public static final Map<String, Component> scarpet_headers = new ConcurrentHashMap<>();

    public static final Map<String, Component> scarpet_footers = new ConcurrentHashMap<>();

    public static void resetScarpetHUDs() {
        scarpet_headers.clear();
        scarpet_footers.clear();
    }

    public static void addMessage(ServerPlayer player, Component hudMessage)
    {
        if (player == null) return;
        if (!player_huds.containsKey(player))
        {
            player_huds.putIfAbsent(player, new CopyOnWriteArrayList<>());
        }
        else
        {
            player_huds.get(player).add(Component.literal("\n"));
        }
        player_huds.get(player).add(hudMessage);
    }

    public static void clearPlayer(ServerPlayer player)
    {
        FoliaRuntime.sendPacket(player,
                new ClientboundTabListPacket(Component.literal(""), Component.literal("")));
    }

    public static void update_hud(MinecraftServer server, List<ServerPlayer> force)
    {
        if (((++hudTick % 20 != 0) && force == null) || CarpetServer.minecraft_server == null)
            return;

        player_huds.clear();

        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());

        players.forEach(p -> {
            Component scarpetFOoter = scarpet_footers.get(p.getScoreboardName());
            if (scarpetFOoter != null) HUDController.addMessage(p, scarpetFOoter);
        });

        if (LoggerRegistry.__tps)
            LoggerRegistry.getLogger("tps").log(()-> send_tps_display(server));

        if (LoggerRegistry.__mobcaps)
            LoggerRegistry.getLogger("mobcaps").log((option, player) -> {
                ResourceKey<Level> dim = switch (option) {
                    case "overworld" -> Level.OVERWORLD;
                    case "nether" -> Level.NETHER;
                    case "end" -> Level.END;
                    default -> player.level().dimension();
                };
                return new Component[]{SpawnReporter.printMobcapsForDimension(server.getLevel(dim), false).get(0)};
            });

        if(LoggerRegistry.__counter)
            LoggerRegistry.getLogger("counter").log((option)->send_counter_info(server, option));

        if (LoggerRegistry.__packets)
            LoggerRegistry.getLogger("packets").log(HUDController::packetCounter);

        HUDListeners.forEach(l -> l.accept(server));

        Set<ServerPlayer> targets = ConcurrentHashMap.newKeySet();
        targets.addAll(players);
        if (force!= null) targets.addAll(force);
        for (ServerPlayer player: targets)
        {
            FoliaRuntime.runOnPlayer(player, target -> {
                ClientboundTabListPacket packet = new ClientboundTabListPacket(
                        scarpet_headers.getOrDefault(target.getScoreboardName(), Component.literal("")),
                        Messenger.c(player_huds.getOrDefault(target, List.of()).toArray(new Object[0]))
                );
                target.connection.send(packet);
            });
        }
    }
    private static Component [] send_tps_display(MinecraftServer server)
    {
        double MSPT = ((double)server.getAverageTickTimeNanos())/ TimeUtil.NANOSECONDS_PER_MILLISECOND;
        ServerTickRateManager trm = server.tickRateManager();

        double TPS = 1000.0D / Math.max(trm.isSprinting()?0.0:trm.millisecondsPerTick(), MSPT);
        if (trm.isFrozen()) {
            TPS = 0;
        }
        String color = Messenger.heatmap_color(MSPT,trm.millisecondsPerTick());
        return new Component[]{Messenger.c(
                "g TPS: ", String.format(Locale.US, "%s %.1f",color, TPS),
                "g  MSPT: ", String.format(Locale.US,"%s %.1f", color, MSPT))};
    }

    private static Component[] send_counter_info(MinecraftServer server, String colors)
    {
        List <Component> res = new ArrayList<>();
        for (String color : colors.split(","))
        {
            HopperCounter counter = HopperCounter.getCounter(color);
            if (counter != null) res.addAll(counter.format(server, false, true));
        }
        return res.toArray(new Component[0]);
    }
    private static Component [] packetCounter()
    {
        Component [] ret =  new Component[]{
                Messenger.c("w I/" + PacketCounter.totalIn + " O/" + PacketCounter.totalOut),
        };
        PacketCounter.reset();
        return ret;
    }
}
