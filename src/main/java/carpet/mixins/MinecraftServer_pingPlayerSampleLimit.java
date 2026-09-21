package carpet.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import carpet.CarpetSettings;
import net.minecraft.server.MinecraftServer;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServer_pingPlayerSampleLimit
{

	@Redirect(method = "buildPlayerStatus", at = @At(value = "FIELD",
			target = "Lorg/spigotmc/SpigotConfig;playerSample:I"))
	private int modifyPlayerSampleLimit()
	{
		return CarpetSettings.pingPlayerListLimit;
	}
}
