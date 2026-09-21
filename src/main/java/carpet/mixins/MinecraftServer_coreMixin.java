package carpet.mixins;

import carpet.CarpetServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServer_coreMixin
{

    @Inject(method = "loadLevel", at = @At("HEAD"))
    private void serverLoaded(CallbackInfo ci)
    {
        CarpetServer.onServerLoaded((MinecraftServer) (Object) this);
    }

    @Inject(method = "loadLevel", at = @At("RETURN"))
    private void serverLoadedWorlds(CallbackInfo ci)
    {
        CarpetServer.onServerLoadedWorlds((MinecraftServer) (Object) this);
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void serverClosed(CallbackInfo ci)
    {
        CarpetServer.onServerClosed((MinecraftServer) (Object) this);
    }

    @Inject(method = "stopServer", at = @At("TAIL"))
    private void serverDoneClosed(CallbackInfo ci)
    {
        CarpetServer.onServerDoneClosing((MinecraftServer) (Object) this);
    }

    @Shadow
    public abstract ServerLevel overworld();
}
