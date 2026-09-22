package carpet.folia;

import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Small agent entrypoint used to connect JVM instrumentation to the plugin classloader. */
public final class FoliaCarpetAgent
{
    private static volatile Instrumentation premainInstrumentation;
    private static volatile JarFile supportJarHandle;

    public static void premain(String args, Instrumentation instrumentation)
    {
        premainInstrumentation = instrumentation;
        appendSupportJar(resolvePluginJar(args));
    }

    public static Instrumentation getPremainInstrumentation()
    {
        return premainInstrumentation;
    }

    public static void agentmain(String args, Instrumentation instrumentation)
    {
        install(args, instrumentation);
    }

    private static Path resolvePluginJar(String args)
    {
        try
        {
            Path source = Path.of(FoliaCarpetAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(source))
            {
                return source;
            }
            if (args != null && args.startsWith("plugin="))
            {
                return Path.of(args.substring("plugin=".length()));
            }
            return source;
        }
        catch (Exception error)
        {
            throw new IllegalStateException("Unable to locate FoliaCarpet agent source", error);
        }
    }

    private static void appendSupportJar(Path pluginJar)
    {
        if (!Files.isRegularFile(pluginJar))
        {
            throw new IllegalStateException("Agent source is not a jar: " + pluginJar);
        }
        try (JarFile jar = new JarFile(pluginJar.toFile()))
        {
            JarEntry embedded = jar.getJarEntry("META-INF/folia-carpet/folia-carpet-support.jar");
            if (embedded == null)
            {
                throw new IllegalStateException("Embedded FoliaCarpet support jar is missing");
            }
            Path supportJar = Files.createTempFile("folia-carpet-support-", ".jar");
            try (var input = jar.getInputStream(embedded))
            {
                Files.copy(input, supportJar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            supportJarHandle = new JarFile(supportJar.toFile());
            premainInstrumentation.appendToSystemClassLoaderSearch(supportJarHandle);
            supportJar.toFile().deleteOnExit();
        }
        catch (Exception error)
        {
            throw new IllegalStateException("Unable to expose FoliaCarpet support classes", error);
        }
    }

    private static void install(String args, Instrumentation instrumentation)
    {
        if (args != null && !args.isBlank())
        {
            try
            {
                instrumentation.appendToSystemClassLoaderSearch(new JarFile(args));
            }
            catch (java.io.IOException error)
            {
                throw new IllegalStateException("Unable to expose FoliaCarpet classes to the server loader", error);
            }
        }
        // The support jar is the single shared copy of Mixin and MixinExtras.
        // The plugin loader still owns the bootstrap classes, but transformed
        // server classes and the agent must resolve the same package classes.
        for (Class<?> loaded : instrumentation.getAllLoadedClasses())
        {
            if (!loaded.getName().equals("carpet.folia.CarpetMixinBridge"))
            {
                continue;
            }
            try
            {
                loaded.getMethod("install", Instrumentation.class).invoke(null, instrumentation);
            }
            catch (ReflectiveOperationException error)
            {
                throw new IllegalStateException("Unable to connect the Folia Mixin bridge", error);
            }
            return;
        }
        // The bootstrap uses one attach to expose the support classes before
        // Mixin is initialized, then a second attach after the bridge exists.
    }

    private FoliaCarpetAgent()
    {
    }
}
