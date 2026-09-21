package carpet.folia;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinServiceAbstract;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Mixin host used when Folia is launched without Fabric or ModLauncher. */
public final class MixinServiceFolia extends MixinServiceAbstract
        implements IClassProvider, IClassBytecodeProvider, ITransformerProvider, IGlobalPropertyService
{
    private final IClassTracker classTracker = new ClassTracker();
    private final IMixinAuditTrail auditTrail = new AuditTrail();
    private final ITransformerProvider transformerProvider = new TransformerProvider();
    private final java.util.Map<String, IPropertyKey> propertyKeys = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<IPropertyKey, Object> properties = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public String getName()
    {
        return "Folia";
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public void prepare()
    {
    }

    @Override
    public void init()
    {
    }

    @Override
    public IClassProvider getClassProvider()
    {
        return this;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider()
    {
        return this;
    }

    @Override
    public ITransformerProvider getTransformerProvider()
    {
        return transformerProvider;
    }

    @Override
    public IClassTracker getClassTracker()
    {
        return classTracker;
    }

    @Override
    public IMixinAuditTrail getAuditTrail()
    {
        return auditTrail;
    }

    @Override
    public Collection<String> getPlatformAgents()
    {
        return Collections.emptyList();
    }

    @Override
    public IContainerHandle getPrimaryContainer()
    {
        try
        {
            URL source = MixinServiceFolia.class.getProtectionDomain().getCodeSource().getLocation();
            return new ContainerHandleURI(URI.create(source.toURI().toString()));
        }
        catch (Exception ignored)
        {
            return new org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual("FoliaCarpet");
        }
    }

    @Override
    public IPropertyKey resolveKey(String name)
    {
        return propertyKeys.computeIfAbsent(name, ignored -> new PropertyKey(name));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(IPropertyKey key)
    {
        return (T) properties.get(key);
    }

    @Override
    public void setProperty(IPropertyKey key, Object value)
    {
        if (value == null)
        {
            properties.remove(key);
        }
        else
        {
            properties.put(key, value);
        }
    }

    @Override
    public <T> T getProperty(IPropertyKey key, T defaultValue)
    {
        T value = getProperty(key);
        return value == null ? defaultValue : value;
    }

    @Override
    public String getPropertyString(IPropertyKey key, String defaultValue)
    {
        Object value = properties.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    @Override
    public InputStream getResourceAsStream(String name)
    {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        InputStream stream = context == null ? null : context.getResourceAsStream(name);
        if (stream != null)
        {
            return stream;
        }
        return MixinServiceFolia.class.getClassLoader().getResourceAsStream(name);
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel()
    {
        return MixinEnvironment.CompatibilityLevel.JAVA_17;
    }

    @Override
    public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel()
    {
        return MixinEnvironment.CompatibilityLevel.JAVA_21;
    }

    @Override
    public URL[] getClassPath()
    {
        return new URL[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException
    {
        return findClass(name, true);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException
    {
        ClassLoader loader = MixinServiceFolia.class.getClassLoader();
        return Class.forName(name, initialize, loader);
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException
    {
        return findClass(name, initialize);
    }

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException
    {
        return getClassNode(name, true);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException
    {
        return getClassNode(name, runTransformers, ClassReader.EXPAND_FRAMES);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags)
            throws ClassNotFoundException, IOException
    {
        String resource = name.replace('.', '/') + ".class";
        InputStream stream = getResourceAsStream(resource);
        if (stream == null)
        {
            throw new ClassNotFoundException(name);
        }
        try (stream)
        {
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, readerFlags);
            return node;
        }
    }

    @Override
    public Collection<ITransformer> getTransformers()
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<ITransformer> getDelegatedTransformers()
    {
        return Collections.emptyList();
    }

    @Override
    public void addTransformerExclusion(String name)
    {
    }

    @Override
    protected ILogger createLogger(String name)
    {
        return super.createLogger("FoliaCarpet/" + name);
    }

    private static final class TransformerProvider implements ITransformerProvider
    {
        @Override
        public Collection<ITransformer> getTransformers()
        {
            return Collections.emptyList();
        }

        @Override
        public Collection<ITransformer> getDelegatedTransformers()
        {
            return Collections.emptyList();
        }

        @Override
        public void addTransformerExclusion(String name)
        {
        }
    }

    private static final class ClassTracker implements IClassTracker
    {
        @Override
        public void registerInvalidClass(String name)
        {
        }

        @Override
        public boolean isClassLoaded(String name)
        {
            return false;
        }

        @Override
        public String getClassRestrictions(String name)
        {
            return "";
        }
    }

    private static final class AuditTrail implements IMixinAuditTrail
    {
        @Override
        public void onApply(String mixin, String target)
        {
        }

        @Override
        public void onPostProcess(String target)
        {
        }

        @Override
        public void onGenerate(String className, String generator)
        {
        }
    }

    private record PropertyKey(String name) implements IPropertyKey
    {
    }
}
