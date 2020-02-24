package gg.amy.mc.cardboard;

import gg.amy.mc.cardboard.command.BukkitCommandInjector;
import gg.amy.mc.cardboard.command.CardboardCommand;
import gg.amy.mc.cardboard.command.Command;
import gg.amy.mc.cardboard.component.Component;
import gg.amy.mc.cardboard.component.LoadableComponent;
import gg.amy.mc.cardboard.component.Single;
import gg.amy.mc.cardboard.config.Config;
import gg.amy.mc.cardboard.config.ConfigFileLoader;
import gg.amy.mc.cardboard.di.Auto;
import gg.amy.mc.cardboard.di.BukkitPlugin;
import gg.amy.mc.cardboard.util.DirectedGraph;
import gg.amy.mc.cardboard.util.TopologicalSort;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @author amy
 * @since 1/9/20.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Cardboard extends JavaPlugin {
    private final Collection<Class<?>> components = new HashSet<>();
    private final Collection<Class<?>> commands = new HashSet<>();
    private final Map<Class<?>, Object> singletons = new LinkedHashMap<>();
    private final BukkitCommandInjector injector = new BukkitCommandInjector();
    private final ConfigFileLoader loader = new ConfigFileLoader(this);
    private ScanResult graph;
    
    @Override
    public final void onLoad() {
        scan(getClass().getPackage().getName());
    }
    
    @Override
    public final void onEnable() {
        init();
    }
    
    private void scan(final String pkg) {
        graph = new ClassGraph().enableAllInfo().whitelistPackages(pkg).scan();
        loadComponents();
    }
    
    private void loadComponents() {
        singletons.put(getClass(), this);
        final DirectedGraph<Class<?>> singletonGraph = new DirectedGraph<>();
        
        graph.getClassesWithAnnotation(Component.class.getName())
                .getNames()
                .stream()
                .map(c -> {
                    try {
                        return Class.forName(c);
                    } catch(final ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .forEach(c -> {
                    if(c.isAnnotationPresent(Single.class)) {
                        singletonGraph.addNode(c);
                        for(final Class<?> dep : c.getDeclaredAnnotation(Single.class).value()) {
                            if(!c.isAnnotationPresent(Single.class)) {
                                throw new IllegalArgumentException("@Single component " + c.getName() + " listed component "
                                        + dep.getName() + ", but " + dep.getName() + " is not a @Single component!");
                            }
                            singletonGraph.addNode(dep);
                            singletonGraph.addEdge(c, dep);
                        }
                    } else {
                        components.add(c);
                        getLogger().info("Loaded new instanced component: " + c.getName());
                    }
                });
        
        final List<Class<?>> dependencies = TopologicalSort.sort(singletonGraph);
        Collections.reverse(dependencies);
        for(final Class<?> dep : dependencies) {
            try {
                singletons.put(dep, dep.getDeclaredConstructor().newInstance());
                getLogger().info("Loaded new singleton component: " + dep.getName());
            } catch(final InstantiationException | NoSuchMethodException | InvocationTargetException
                    | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void init() {
        initSingletons();
        registerListeners();
        registerCommands();
    }
    
    private void initSingletons() {
        singletons.values().forEach(v -> {
            injectConfig(v);
            injectComponents(v);
            if(v instanceof LoadableComponent) {
                final LoadableComponent l = (LoadableComponent) v;
                if(l.doInit()) {
                    getLogger().info("Loaded component " + getComponentName(v) + ": " + getComponentDescription(v));
                } else {
                    getLogger().warning("Failed loading component " + getComponentName(v) + ": " + getComponentDescription(v));
                }
            }
        });
    }
    
    public final void injectConfig(final Object object) {
        for(final Field f : object.getClass().getDeclaredFields()) {
            if(f.isAnnotationPresent(Config.class)) {
                f.setAccessible(true);
                final Config annotation = f.getDeclaredAnnotation(Config.class);
                final String file = annotation.file();
                final String path = annotation.value();
                final Class<?> type = f.getType();
                final ConfigurationSection config;
                if(file.equalsIgnoreCase("config.yml")) {
                    config = getConfig();
                } else {
                    config = loader.loadFile(file);
                }
                injectValueFromConfig(object, f, annotation, path, type, config);
            }
        }
    }
    
    public final void injectConfigFromFile(final Object object, final String file) {
        for(final Field f : object.getClass().getDeclaredFields()) {
            if(f.isAnnotationPresent(Config.class)) {
                f.setAccessible(true);
                final Config annotation = f.getDeclaredAnnotation(Config.class);
                final String path = annotation.value();
                final Class<?> type = f.getType();
                final ConfigurationSection config = loader.loadFile(file);
                injectValueFromConfig(object, f, annotation, path, type, config);
            }
        }
    }
    
    private void injectValueFromConfig(final Object object, final Field f, final Config annotation, final String path,
                                       final Class<?> type, final ConfigurationSection config) {
        final Object value;
        if(type.equals(Boolean.class) || type.equals(boolean.class)) {
            value = config.getBoolean(path);
        } else if(type.equals(Double.class) || type.equals(double.class)) {
            value = config.getDouble(path);
        } else if(type.equals(Float.class) || type.equals(float.class)) {
            value = (float) config.getDouble(path);
        } else if(type.equals(Byte.class) || type.equals(byte.class)) {
            value = (byte) config.getInt(path);
        } else if(type.equals(Short.class) || type.equals(short.class)) {
            value = (short) config.getInt(path);
        } else if(type.equals(Integer.class) || type.equals(int.class)) {
            value = config.getInt(path);
        } else if(type.equals(Long.class) || type.equals(long.class)) {
            value = config.getLong(path);
        } else if(type.equals(String.class)) {
            if(annotation.coloured()) {
                final String string = config.getString(path);
                if(string != null) {
                    value = ChatColor.translateAlternateColorCodes('&', string);
                } else {
                    value = null;
                }
            } else {
                value = config.getString(path);
            }
        } else {
            value = config.get(path);
        }
        f.setAccessible(true);
        try {
            f.set(object, value);
        } catch(final IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public final void injectComponents(final Object component) {
        injectComponents(component, new HashMap<>());
    }
    
    public final void injectComponents(final Object object, final Map<Class<?>, ?> ctx) {
        for(final Field field : object.getClass().getDeclaredFields()) {
            try {
                if(field.isAnnotationPresent(Auto.class)) {
                    field.setAccessible(true);
                    final Class<?> type = field.getType();
                    final Optional<Class<?>> ctxMatch = ctx.keySet().stream().filter(type::isAssignableFrom).findFirst();
                    if(ctxMatch.isPresent()) {
                        field.set(object, ctx.get(ctxMatch.get()));
                    } else {
                        final Optional<?> located = getComponent(type, ctx);
                        if(located.isPresent()) {
                            field.set(object, located.get());
                        } else {
                            // TODO: Shouldn't this log a warning?
                            // throw new IllegalArgumentException("Couldn't find component for class of type " + type.getName() + '!');
                        }
                    }
                }
                if(field.isAnnotationPresent(BukkitPlugin.class)) {
                    field.setAccessible(true);
                    final String name = field.getDeclaredAnnotation(BukkitPlugin.class).value();
                    final Plugin plugin = getServer().getPluginManager().getPlugin(name);
                    if(plugin == null) {
                        throw new IllegalStateException("Was asked to load plugin " + name + " for " + object.getClass() + '#'
                                + field.getName() + ", but that plugin doesn't exist? Is it loaded?");
                    } else {
                        field.set(object, plugin);
                    }
                }
            } catch(final IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void registerListeners() {
        graph.getClassesImplementing(Listener.class.getName())
                .getNames()
                .stream()
                .map(c -> {
                    try {
                        return Class.forName(c);
                    } catch(final ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .forEach(c -> {
                    try {
                        final Listener listener = (Listener) c.getDeclaredConstructor().newInstance();
                        injectConfig(listener);
                        injectComponents(listener);
                        getServer().getPluginManager().registerEvents(listener, this);
                        getLogger().info("Loaded new Bukkit listener: " + c.getName());
                    } catch(final InstantiationException | NoSuchMethodException | InvocationTargetException
                            | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                });
    }
    
    private void registerCommands() {
        graph.getClassesWithAnnotation(Command.class.getName())
                .getNames()
                .stream()
                .map(c -> {
                    try {
                        return Class.forName(c);
                    } catch(final ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .forEach(c -> {
                    commands.add(c);
                    final Command annotation = c.getDeclaredAnnotation(Command.class);
                    final CardboardCommand wrapper = new CardboardCommand(this, c, annotation.name(), annotation.desc(),
                            annotation.usage(), new ArrayList<>(Arrays.asList(annotation.aliases())), annotation.permissionNode(),
                            annotation.permissionMessage());
                    wrapper.setLabel(annotation.label());
                    wrapper.loadSubcommands();
                    injector.register(wrapper);
                    getLogger().info("Loaded new Bukkit command: " + c.getName());
                });
    }
    
    public final <T> Optional<T> getComponent(final Class<T> cls) {
        return getComponent(cls, new HashMap<>());
    }
    
    @SuppressWarnings({"unchecked", "DuplicatedCode"})
    public final <T> Optional<T> getComponent(final Class<T> cls, final Map<Class<?>, ?> ctx) {
        if(singletons.containsKey(cls)) {
            return Optional.of((T) singletons.get(cls));
        } else {
            final Optional<Class<?>> first = singletons.keySet().stream().filter(cls::isAssignableFrom).findFirst();
            if(first.isPresent()) {
                return Optional.of((T) singletons.get(first.get()));
            } else {
                final Optional<Class<?>> comp = components.stream().filter(cls::isAssignableFrom).findFirst();
                if(comp.isPresent()) {
                    try {
                        final Object instance = comp.get().getDeclaredConstructor().newInstance();
                        injectConfig(instance);
                        injectComponents(instance, ctx);
                        return Optional.of((T) instance);
                    } catch(final InstantiationException | NoSuchMethodException | InvocationTargetException
                            | IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                } else {
                    return Optional.empty();
                }
            }
        }
    }
    
    public final <T> Optional<T> getCommand(final Class<T> cls) {
        return getCommand(cls, new HashMap<>());
    }
    
    @SuppressWarnings({"unchecked", "DuplicatedCode"})
    public final <T> Optional<T> getCommand(final Class<T> cls, final Map<Class<?>, ?> ctx) {
        final Optional<Class<?>> cmd = commands.stream().filter(cls::isAssignableFrom).findFirst();
        if(cmd.isPresent()) {
            try {
                final Object instance = cmd.get().getDeclaredConstructor().newInstance();
                injectConfig(instance);
                injectComponents(instance, ctx);
                return Optional.of((T) instance);
            } catch(final InstantiationException | NoSuchMethodException | InvocationTargetException
                    | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        } else {
            return Optional.empty();
        }
    }
    
    public final <T> String getComponentName(final T component) {
        final Class<?> cls = component.getClass();
        if(!cls.isAnnotationPresent(Component.class)) {
            throw new IllegalArgumentException(cls.getName() + " isn't a @Component!");
        }
        return cls.getAnnotation(Component.class).name();
    }
    
    public final <T> String getComponentDescription(final T component) {
        final Class<?> cls = component.getClass();
        if(!cls.isAnnotationPresent(Component.class)) {
            throw new IllegalArgumentException(cls.getName() + " isn't a @Component!");
        }
        return cls.getAnnotation(Component.class).description();
    }
    
    public final ConfigurationSection loadConfig(final String path) {
        return loader.loadFile(path);
    }
}
