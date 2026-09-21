package carpet.folia;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;

/** Registers Carpet's bytecode hooks during the Folia bootstrap phase. */
public final class CarpetFoliaBootstrap implements PluginBootstrap
{
    @Override
    public void bootstrap(BootstrapContext context)
    {
        context.getLogger().info("Preparing Carpet mixins before Folia loads the server");
        System.setProperty("folia-carpet.source", context.getPluginSource().toString());
        CarpetMixinBootstrap.initialize();
    }
}
