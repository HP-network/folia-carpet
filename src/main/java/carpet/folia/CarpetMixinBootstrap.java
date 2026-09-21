package carpet.folia;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;

import com.sun.tools.attach.VirtualMachine;

import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.nio.file.Path;

public final class CarpetMixinBootstrap {

    private static boolean initialized = false;

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        try {
            System.setProperty("mixin.hotSwap", "true");
            // Put the shared Mixin classes on the server loader before any of
            // their types are resolved by the plugin bootstrap class.
            attachAgent();
            MixinBootstrap.init();
            MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.SERVER);
            advanceToDefaultPhase();
            createTransformer();
            Mixins.addConfiguration("carpet.mixins.json", null);
            // Keep the bridge visible to the agent class loader before attachment.
            Class.forName("carpet.folia.CarpetMixinBridge");
            attachAgent();
            MixinExtrasBootstrap.init();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to initialize carpet mixins", t);
        }
    }

    private static void advanceToDefaultPhase() throws Exception
    {
        var method = MixinEnvironment.class.getDeclaredMethod(
                "gotoPhase", MixinEnvironment.Phase.class);
        method.setAccessible(true);
        method.invoke(null, MixinEnvironment.Phase.DEFAULT);
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.SERVER);
    }

    private static void createTransformer() throws Exception
    {
        if (MixinEnvironment.getDefaultEnvironment().getActiveTransformer() instanceof IMixinTransformer)
        {
            return;
        }
        Class<?> factoryType = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer$Factory");
        var constructor = factoryType.getDeclaredConstructor();
        constructor.setAccessible(true);
        var factory = (org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory) constructor.newInstance();
        IMixinTransformer transformer = factory.createTransformer();
        MixinEnvironment.getCurrentEnvironment().setActiveTransformer(transformer);
        MixinEnvironment.getDefaultEnvironment().setActiveTransformer(transformer);
    }

    private static void attachAgent() throws Exception
    {
        System.setProperty("jdk.attach.allowAttachSelf", "true");
        String source = System.getProperty("folia-carpet.source");
        if (source == null || source.isBlank())
        {
            source = CarpetMixinBootstrap.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString();
        }
        Path pluginJar = source.startsWith("file:")
                ? Path.of(java.net.URI.create(source))
                : Path.of(source);
        if (!java.nio.file.Files.isRegularFile(pluginJar))
        {
            throw new IllegalStateException("Agent source is not a jar: " + pluginJar);
        }
        Path supportJar = createSupportJar(pluginJar);
        Path agentJar = createAgentJar();
        String pid = Long.toString(ProcessHandle.current().pid());
        try
        {
            VirtualMachine vm = VirtualMachine.attach(pid);
            try
            {
                vm.loadAgent(agentJar.toString(), supportJar.toString());
            }
            finally
            {
                vm.detach();
            }
        }
        catch (java.io.IOException selfAttachFailure)
        {
            Path java = Path.of(System.getProperty("java.home"), "bin", "java");
            Process helper = new ProcessBuilder(
                    java.toString(),
                    "-cp", pluginJar.toString(),
                    FoliaCarpetAttacher.class.getName(),
                    pid,
                    agentJar.toString(),
                    supportJar.toString())
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            if (helper.waitFor() != 0)
            {
                throw new java.io.IOException("External agent attach failed", selfAttachFailure);
            }
        }
    }

    private static Path createAgentJar() throws Exception
    {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue("Agent-Class", FoliaCarpetAgent.class.getName());
        attributes.putValue("Can-Redefine-Classes", "true");
        attributes.putValue("Can-Retransform-Classes", "true");
        Path agentJar = java.nio.file.Files.createTempFile("folia-carpet-agent-", ".jar");
        try (JarOutputStream output = new JarOutputStream(java.nio.file.Files.newOutputStream(agentJar), manifest);
             InputStream input = FoliaCarpetAgent.class.getResourceAsStream("/carpet/folia/FoliaCarpetAgent.class"))
        {
            if (input == null)
            {
                throw new IllegalStateException("Folia agent class is missing from the plugin");
            }
            output.putNextEntry(new JarEntry("carpet/folia/FoliaCarpetAgent.class"));
            input.transferTo(output);
            output.closeEntry();
        }
        agentJar.toFile().deleteOnExit();
        return agentJar;
    }

    private static Path createSupportJar(Path pluginJar) throws Exception
    {
        Path supportJar = java.nio.file.Files.createTempFile("folia-carpet-support-", ".jar");
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(pluginJar.toFile()))
        {
            JarEntry embedded = jar.getJarEntry("META-INF/folia-carpet/folia-carpet-support.jar");
            if (embedded != null)
            {
                try (InputStream input = jar.getInputStream(embedded))
                {
                    java.nio.file.Files.copy(input, supportJar,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                supportJar.toFile().deleteOnExit();
                return supportJar;
            }
        }
        try (java.util.zip.ZipInputStream input = new java.util.zip.ZipInputStream(
                     java.nio.file.Files.newInputStream(pluginJar));
             JarOutputStream output = new JarOutputStream(java.nio.file.Files.newOutputStream(supportJar)))
        {
            java.util.zip.ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = input.getNextEntry()) != null)
            {
                String name = entry.getName();
                if (entry.isDirectory() || !isServerSupportEntry(name))
                {
                    continue;
                }
                output.putNextEntry(new JarEntry(name));
                int count;
                while ((count = input.read(buffer)) >= 0)
                {
                    output.write(buffer, 0, count);
                }
                output.closeEntry();
            }
        }
        supportJar.toFile().deleteOnExit();
        return supportJar;
    }

    private static boolean isServerSupportEntry(String name)
    {
        if (name.startsWith("carpet/"))
        {
            if (name.startsWith("carpet/folia/CarpetFolia")
                    || name.startsWith("carpet/folia/FoliaCarpet")
                    || name.startsWith("carpet/folia/MixinInstrumentation")
                    || name.startsWith("carpet/folia/MixinService")
                    || name.startsWith("carpet/folia/CarpetMixin"))
            {
                return false;
            }
            return true;
        }
        if (name.startsWith("assets/carpet/"))
        {
            return true;
        }
        if (name.equals("carpet.mixins.json") || name.endsWith(".refmap.json"))
        {
            return true;
        }
        return name.startsWith("com/llamalad7/mixinextras/")
                || name.startsWith("org/spongepowered/asm/")
                || name.startsWith("org/objectweb/asm/")
                || name.startsWith("META-INF/services/");
    }

    private CarpetMixinBootstrap() {
    }
}
