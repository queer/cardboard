package gg.amy.mc.cardboard;

import gg.amy.mc.cardboard.command.BukkitCommandInjector;
import gg.amy.mc.cardboard.command.CardboardCommand;
import gg.amy.mc.cardboard.command.Command;
import gg.amy.mc.cardboard.component.Component;
import gg.amy.mc.cardboard.component.LoadableComponent;
import gg.amy.mc.cardboard.component.Single;
import gg.amy.mc.cardboard.config.Config;
import gg.amy.mc.cardboard.di.Auto;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.bukkit.event.Listener;
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
    private final Map<Class<?>, Object> singletons = new HashMap<>();
    private final BukkitCommandInjector injector = new BukkitCommandInjector();
    private ScanResult graph;
    
    @Override
    public void onLoad() {
        scan(getClass().getPackage().getName());
    }
    
    @Override
    public void onEnable() {
        init();
    }
    
    private void scan(final String pkg) {
        graph = new ClassGraph().enableAllInfo().whitelistPackages(pkg).scan();
        loadComponents();
    }
    
    private void loadComponents() {
        singletons.put(getClass(), this);
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
                    try {
                        if(c.isAnnotationPresent(Single.class)) {
                            singletons.put(c, c.getConstructor().newInstance());
                        } else {
                            components.add(c);
                            getLogger().info("Loaded new instanced component: " + c.getName());
                        }
                    } catch(final NullPointerException | IllegalAccessException | NoSuchMethodException
                            | InvocationTargetException | InstantiationException e) {
                        e.printStackTrace();
                    }
                });
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
    
    private void injectConfig(final Object object) {
        for(final Field f : object.getClass().getDeclaredFields()) {
            if(f.isAnnotationPresent(Config.class)) {
                f.setAccessible(true);
                final String path = f.getDeclaredAnnotation(Config.class).value();
                final Class<?> type = f.getType();
                final Object value;
                if(type.equals(Boolean.class) || type.equals(boolean.class)) {
                    value = getConfig().getBoolean(path);
                } else if(type.equals(Double.class) || type.equals(double.class)) {
                    value = getConfig().getDouble(path);
                } else if(type.equals(Float.class) || type.equals(float.class)) {
                    value = (float) getConfig().getDouble(path);
                } else if(type.equals(Byte.class) || type.equals(byte.class)) {
                    value = (byte) getConfig().getInt(path);
                } else if(type.equals(Short.class) || type.equals(short.class)) {
                    value = (short) getConfig().getInt(path);
                } else if(type.equals(Integer.class) || type.equals(int.class)) {
                    value = getConfig().getInt(path);
                } else if(type.equals(Long.class) || type.equals(long.class)) {
                    value = getConfig().getLong(path);
                } else if(type.equals(String.class)) {
                    value = getConfig().getString(path);
                } else {
                    value = getConfig().get(path);
                }
                f.setAccessible(true);
                try {
                    f.set(object, value);
                } catch(final IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }
    
    private void injectComponents(final Object component) {
        injectComponents(component, new HashMap<>());
    }
    
    private void injectComponents(final Object object, final Map<Class<?>, ?> ctx) {
        for(final Field field : object.getClass().getDeclaredFields()) {
            if(field.isAnnotationPresent(Auto.class)) {
                field.setAccessible(true);
                final Class<?> type = field.getType();
                final Optional<Class<?>> ctxMatch = ctx.keySet().stream().filter(type::isAssignableFrom).findFirst();
                if(ctxMatch.isPresent()) {
                    try {
                        field.set(object, ctx.get(ctxMatch.get()));
                    } catch(final IllegalAccessException e) {
                        // e.printStackTrace();
                    }
                } else {
                    final Optional<?> located = getComponent(type, ctx);
                    if(located.isPresent()) {
                        try {
                            field.set(object, located.get());
                        } catch(final IllegalAccessException e) {
                            // e.printStackTrace();
                        }
                    } else {
                        // throw new IllegalArgumentException("Couldn't find component for class of type " + type.getName() + '!');
                    }
                }
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
    
    public <T> Optional<T> getComponent(final Class<T> cls) {
        return getComponent(cls, new HashMap<>());
    }
    
    @SuppressWarnings({"unchecked", "DuplicatedCode"})
    public <T> Optional<T> getComponent(final Class<T> cls, final Map<Class<?>, ?> ctx) {
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
    
    public <T> Optional<T> getCommand(final Class<T> cls) {
        return getCommand(cls, new HashMap<>());
    }
    
    @SuppressWarnings({"unchecked", "DuplicatedCode"})
    public <T> Optional<T> getCommand(final Class<T> cls, final Map<Class<?>, ?> ctx) {
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
    
    public <T> String getComponentName(final T component) {
        final Class<?> cls = component.getClass();
        if(!cls.isAnnotationPresent(Component.class)) {
            throw new IllegalArgumentException(cls.getName() + " isn't a @Component!");
        }
        return cls.getAnnotation(Component.class).name();
    }
    
    public <T> String getComponentDescription(final T component) {
        final Class<?> cls = component.getClass();
        if(!cls.isAnnotationPresent(Component.class)) {
            throw new IllegalArgumentException(cls.getName() + " isn't a @Component!");
        }
        return cls.getAnnotation(Component.class).description();
    }
}
