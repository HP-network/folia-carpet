package carpet.mixins;

import carpet.fakes.TicketsFetcherInterface;
import carpet.folia.MixinCompat;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.Ticket;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(DistanceManager.class)
public abstract class DistanceManager_scarpetMixin implements TicketsFetcherInterface
{
    @Override
    public Long2ObjectOpenHashMap<List<Ticket>>  getTicketsByPosition()
    {
        return MixinCompat.ticketManager_getTicketsByPosition((DistanceManager) (Object) this);
    }

}
