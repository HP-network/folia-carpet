package carpet.folia;

import org.bukkit.plugin.Plugin;

import java.util.concurrent.atomic.AtomicInteger;

/** State shared by the Paper plugin loader and transformed server classes. */
public final class FoliaRuntime
{
    private static final AtomicInteger TICK = new AtomicInteger();
    private static volatile Plugin plugin;

    private FoliaRuntime()
    {
    }

    public static void bind(Plugin value)
    {
        plugin = value;
    }

    public static Plugin plugin()
    {
        return plugin;
    }

    public static int tick()
    {
        return TICK.get();
    }

    public static int advanceTick()
    {
        return TICK.incrementAndGet();
    }
}
