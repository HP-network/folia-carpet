package carpet.folia;

import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/** Installs the Mixin transformer once the JVM agent has received Instrumentation. */
public final class CarpetMixinBridge
{
    private static volatile boolean installed;
    private static volatile int transformedClasses;
    private static volatile int failedTransformations;
    private static volatile int transformerInvocations;
    private static volatile int serverClassInvocations;
    private static Instrumentation instrumentation;
    private static final Set<String> generatedSyntheticClasses = new HashSet<>();
    private static final Set<JarFile> generatedSyntheticJars = new HashSet<>();

    private CarpetMixinBridge()
    {
    }

    public static synchronized void install(Instrumentation instrumentation)
    {
        if (installed)
        {
            return;
        }
        Object active = MixinEnvironment.getDefaultEnvironment().getActiveTransformer();
        if (!(active instanceof IMixinTransformer transformer))
        {
            throw new IllegalStateException("Mixin transformer was not created");
        }
        CarpetMixinBridge.instrumentation = instrumentation;
        instrumentation.addTransformer(new MixinInstrumentationTransformer(transformer), true);
        installed = true;
        if (Boolean.getBoolean("folia-carpet.retransform-loaded"))
        {
            retransformLoadedClasses(instrumentation);
        }
    }

    static void recordTransformation(boolean transformed)
    {
        if (transformed)
        {
            transformedClasses++;
        }
    }

    static void recordFailure()
    {
        failedTransformations++;
    }

    public static boolean isInstalled()
    {
        return installed;
    }

    public static int transformedClasses()
    {
        return transformedClasses;
    }

    public static int failedTransformations()
    {
        return failedTransformations;
    }

    static void recordInvocation(String className, boolean serverClass)
    {
        transformerInvocations++;
        if (serverClass)
        {
            serverClassInvocations++;
        }
    }

    static synchronized void syncSyntheticClasses(IMixinTransformer transformer)
    {
        if (instrumentation == null)
        {
            return;
        }
        try
        {
            Field registryField = transformer.getClass().getDeclaredField("syntheticClassRegistry");
            registryField.setAccessible(true);
            Object registry = registryField.get(transformer);
            Field classesField = registry.getClass().getDeclaredField("classes");
            classesField.setAccessible(true);
            Set<String> pending = new HashSet<>();
            for (String name : ((Map<String, ?>) classesField.get(registry)).keySet())
            {
                if (!generatedSyntheticClasses.contains(name))
                {
                    pending.add(name);
                }
            }
            if (pending.isEmpty())
            {
                return;
            }
            Path jar = Files.createTempFile("folia-carpet-synthetic-", ".jar");
            int generated = 0;
            try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar)))
            {
                for (String name : pending)
                {
                    byte[] bytes = transformer.generateClass(
                            MixinEnvironment.getCurrentEnvironment(), name.replace('/', '.'));
                    if (bytes == null)
                    {
                        continue;
                    }
                    output.putNextEntry(new JarEntry(name + ".class"));
                    output.write(bytes);
                    output.closeEntry();
                    generatedSyntheticClasses.add(name);
                    generated++;
                }
            }
            if (generated > 0)
            {
                JarFile generatedJar = new JarFile(jar.toFile());
                generatedSyntheticJars.add(generatedJar);
                instrumentation.appendToSystemClassLoaderSearch(generatedJar);
                jar.toFile().deleteOnExit();
            }
            else
            {
                Files.deleteIfExists(jar);
            }
        }
        catch (Throwable error)
        {
            System.err.println("[FoliaCarpet] Unable to publish generated Mixin classes");
            error.printStackTrace(System.err);
        }
    }

    public static int transformerInvocations()
    {
        return transformerInvocations;
    }

    public static int serverClassInvocations()
    {
        return serverClassInvocations;
    }

    public static String mixinSummary()
    {
        try
        {
            Object transformer = MixinEnvironment.getDefaultEnvironment().getActiveTransformer();
            Field processorField = transformer.getClass().getDeclaredField("processor");
            processorField.setAccessible(true);
            Object processor = processorField.get(transformer);
            Field configsField = processor.getClass().getDeclaredField("configs");
            configsField.setAccessible(true);
            Field pendingField = processor.getClass().getDeclaredField("pendingConfigs");
            pendingField.setAccessible(true);
            StringJoiner summary = new StringJoiner(", ");
            Collection<?> configs = (Collection<?>) configsField.get(processor);
            Collection<?> pending = (Collection<?>) pendingField.get(processor);
            for (Object config : configs)
            {
                Field nameField = config.getClass().getDeclaredField("name");
                nameField.setAccessible(true);
                Field mixinsField = config.getClass().getDeclaredField("mixins");
                mixinsField.setAccessible(true);
                summary.add(nameField.get(config) + "/" + ((Collection<?>) mixinsField.get(config)).size());
            }
            StringJoiner pendingSummary = new StringJoiner(", ");
            for (Object config : pending)
            {
                Field nameField = config.getClass().getDeclaredField("name");
                nameField.setAccessible(true);
                Set<?> targets = (Set<?>) config.getClass().getMethod("getTargetsSet").invoke(config);
                pendingSummary.add(nameField.get(config) + "/targets=" + targets.size());
            }
            StringJoiner globalSummary = new StringJoiner(", ");
            for (Object config : org.spongepowered.asm.mixin.Mixins.getConfigs())
            {
                String name = String.valueOf(config.getClass().getMethod("getName").invoke(config));
                Object configEnvironment = config.getClass().getMethod("getEnvironment").invoke(config);
                var configGetter = config.getClass().getDeclaredMethod("get");
                configGetter.setAccessible(true);
                Object mixinConfig = configGetter.invoke(config);
                Field envField = mixinConfig.getClass().getDeclaredField("env");
                envField.setAccessible(true);
                Field selectorField = mixinConfig.getClass().getDeclaredField("selector");
                selectorField.setAccessible(true);
                globalSummary.add(name + "/visited=" + config.getClass().getMethod("isVisited").invoke(config)
                        + "/env=" + System.identityHashCode(configEnvironment)
                        + "/internalEnv=" + System.identityHashCode(envField.get(mixinConfig))
                        + "/selector=" + selectorField.get(mixinConfig));
            }
            Object currentEnvironment = MixinEnvironment.getCurrentEnvironment();
            Object defaultEnvironment = MixinEnvironment.getDefaultEnvironment();
            return "phase=" + MixinEnvironment.getDefaultEnvironment().getPhase()
                    + ", side=" + MixinEnvironment.getDefaultEnvironment().getSide()
                    + ", configs=" + configs.size() + "[" + summary + "]"
                    + ", pending=" + pending.size() + "[" + pendingSummary + "]"
                    + ", global=" + org.spongepowered.asm.mixin.Mixins.getConfigs().size()
                    + ", unvisited=" + org.spongepowered.asm.mixin.Mixins.getUnvisitedCount()
                    + ", currentEnv=" + System.identityHashCode(currentEnvironment)
                    + ", defaultEnv=" + System.identityHashCode(defaultEnvironment)
                    + ", globals=[" + globalSummary + "]";
        }
        catch (Throwable error)
        {
            return "unavailable: " + error.getClass().getSimpleName();
        }
    }

    private static void retransformLoadedClasses(Instrumentation instrumentation)
    {
        for (Class<?> type : instrumentation.getAllLoadedClasses())
        {
            String name = type.getName();
            if (!name.startsWith("net.minecraft.") || !instrumentation.isModifiableClass(type))
            {
                continue;
            }
            try
            {
                instrumentation.retransformClasses(type);
            }
            catch (Throwable ignored)
            {
                // Some bootstrap classes cannot be retransformed on every JVM.
            }
        }
    }
}
