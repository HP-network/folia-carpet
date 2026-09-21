package carpet.mixins;

import carpet.fakes.MinecraftServerInterface;
import carpet.CarpetServer;
import carpet.script.CarpetScriptServer;
import net.minecraft.util.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import java.util.Map;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServer_scarpetMixin extends ReentrantBlockableEventLoop<TickTask> implements MinecraftServerInterface
{
    private CarpetScriptServer scriptServer;

    public MinecraftServer_scarpetMixin(String string_1)
    {
        super(string_1);
    }

    @Shadow @Final protected LevelStorageSource.LevelStorageAccess storageSource;

    @Shadow @Final private Map<ResourceKey<Level>, ServerLevel> levels;

    @Shadow private MinecraftServer.ReloadableResources resources;

    @Shadow public abstract RegistryAccess.Frozen registryAccess();

    @Shadow public abstract PlayerList getPlayerList();

    @Shadow @Final private ServerFunctionManager functionManager;

    @Shadow @Final private StructureTemplateManager structureTemplateManager;

    @Shadow public abstract ServerTickRateManager tickRateManager();

    @Shadow private long nextTickTimeNanos;

    @Shadow private long lastOverloadWarningNanos;

    @Override
    public void forceTick(BooleanSupplier isAhead)
    {
        if (isAhead == null || isAhead.getAsBoolean())
        {
            // A Folia server has no single main-world tick to invoke here.
            // Preserve Scarpet's logical tick callbacks without touching a
            // different region from the caller's region thread.
            CarpetServer.tick((MinecraftServer) (Object) this);
        }
    }

    @Override
    public LevelStorageSource.LevelStorageAccess getCMSession()
    {
        return storageSource;
    }

    @Override
    public Map<ResourceKey<Level>, ServerLevel> getCMWorlds()
    {
        return levels;
    }

    @Override
    public void reloadAfterReload(RegistryAccess newRegs)
    {
        resources.managers().updateStaticRegistryTags();
        getPlayerList().saveAll();
        getPlayerList().reloadResources();
        functionManager.replaceLibrary(this.resources.managers().getFunctionLibrary());
        structureTemplateManager.onResourceManagerReload(this.resources.resourceManager());
    }

    @Override
    public MinecraftServer.ReloadableResources getResourceManager()
    {
        return resources;
    }

    @Override
    public void addScriptServer(final CarpetScriptServer scriptServer)
    {
        this.scriptServer = scriptServer;
    }

    @Override
    public CarpetScriptServer getScriptServer()
    {
        return scriptServer;
    }
}
