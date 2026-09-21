package carpet.folia;

import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/** Adapts Mixin's transformer to the JVM instrumentation API used by Folia. */
public final class MixinInstrumentationTransformer implements ClassFileTransformer
{
    private final IMixinTransformer transformer;
    private static int diagnosticFailures;

    public MixinInstrumentationTransformer(IMixinTransformer transformer)
    {
        this.transformer = transformer;
    }

    @Override
    public byte[] transform(Module module, ClassLoader loader, String className, Class<?> classBeingRedefined,
                             ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException
    {
        boolean serverClass = className != null && isServerClass(className);
        CarpetMixinBridge.recordInvocation(className, serverClass);
        if (className == null || classfileBuffer == null || !serverClass)
        {
            return null;
        }
        try
        {
            String dottedName = className.replace('/', '.');
            byte[] transformed = transformer.transformClassBytes(dottedName, dottedName, classfileBuffer);
            CarpetMixinBridge.syncSyntheticClasses(transformer);
            CarpetMixinBridge.recordTransformation(transformed != classfileBuffer);
            return transformed;
        }
        catch (Throwable error)
        {
            if (diagnosticFailures++ < 12)
            {
                System.err.println("[FoliaCarpet] Mixin transformation failed for " + className
                        + " (loader=" + loader + ")");
                error.printStackTrace(System.err);
            }
            CarpetMixinBridge.recordFailure();
            return null;
        }
    }


    private static boolean isServerClass(String name)
    {
        return name.startsWith("net/minecraft/") || name.startsWith("org/bukkit/craftbukkit/");
    }
}
