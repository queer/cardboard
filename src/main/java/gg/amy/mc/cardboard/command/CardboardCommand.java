package gg.amy.mc.cardboard.command;

import gg.amy.mc.cardboard.Cardboard;
import gg.amy.mc.cardboard.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author amy
 * @since 1/9/20.
 */
public final class CardboardCommand extends Command {
    private final Cardboard cardboard;
    private final Class<?> src;
    private final String permission;
    private final String permissionMessage;
    private final Map<String, CardboardSubcommand> subcommands = new HashMap<>();
    private Method defaultSubcommand = null;
    
    public CardboardCommand(final Cardboard cardboard, final Class<?> src, final String name, final String description,
                            final String usageMessage, final List<String> aliases, final String permission,
                            final String permissionMessage) {
        super(name, description, usageMessage, aliases);
        this.cardboard = cardboard;
        this.src = src;
        this.permission = permission;
        this.permissionMessage = permissionMessage;
    }
    
    @SuppressWarnings("UnusedReturnValue")
    public final CardboardCommand loadSubcommands() {
        for(final Method m : src.getDeclaredMethods()) {
            if(m.isAnnotationPresent(Subcommand.class)) {
                m.setAccessible(true);
                final Subcommand annotation = m.getAnnotation(Subcommand.class);
                for(final String name : annotation.value()) {
                    final String lowerName = name.toLowerCase();
                    if(subcommands.containsKey(lowerName)) {
                        throw new IllegalStateException("Attempted to register subcommand '" + lowerName + "' for class "
                                + src.getName() + ", but it's already been registered!");
                    }
                    subcommands.put(lowerName, new CardboardSubcommand(lowerName, annotation.desc(),
                            annotation.usage(), annotation.permissionNode(), m));
                }
            }
            if(m.isAnnotationPresent(Default.class)) {
                if(defaultSubcommand != null) {
                    throw new IllegalStateException("Attempted to register default command for class " + src.getName()
                            + ", but it's already been registered!");
                }
                m.setAccessible(true);
                defaultSubcommand = m;
            }
        }
        return this;
    }
    
    @Override
    public final boolean execute(final CommandSender commandSender, final String s, final String[] strings) {
        if(commandSender.hasPermission(permission) || commandSender.isOp()) {
            final boolean ret;
            try {
                ret = executeCommand(commandSender, s, strings);
                if(!ret) {
                    MessageUtil.sendMessage(commandSender, usageMessage);
                }
            } catch(final InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        } else {
            MessageUtil.sendMessage(commandSender, permissionMessage);
        }
        return true;
    }
    
    private boolean executeCommand(final CommandSender sender, final String cmd, final String[] args) throws InvocationTargetException, IllegalAccessException {
        // TODO: Instead of passing String[] args, parse out quotes etc. for the end-user.
        final Map<Class<?>, Object> ctx = new HashMap<>();
        ctx.put(CommandSender.class, sender);
        if(sender instanceof Player p) {
            ctx.put(Player.class, p);
        }
        final Optional<?> maybeCommand = cardboard.getCommand(src, ctx);
        if(maybeCommand.isPresent()) {
            final Object instance = maybeCommand.get();
            if(args.length == 0) {
                defaultSubcommand.invoke(instance, cmd, args);
            } else {
                final String[] realArgs = new String[args.length - 1];
                System.arraycopy(args, 1, realArgs, 0, realArgs.length);
                final CardboardSubcommand sub = subcommands.get(args[0]);
                if(sub != null) {
                    if(sender.hasPermission(sub.permissionNode) || sender.isOp()) {
                        sub.method.invoke(instance, String.join(" ", Arrays.asList(realArgs)), realArgs);
                    } else {
                        MessageUtil.sendMessage(sender, permissionMessage);
                    }
                } else {
                    defaultSubcommand.invoke(instance, cmd, args);
                }
            }
        } else {
            MessageUtil.sendMessage(sender, usageMessage);
            throw new IllegalStateException("Asked to execute command of type " + src.getName()
                    + ", but somehow we don't have a component for it!?");
        }
        return true;
    }
    
    private static final class CardboardSubcommand {
        private final String name;
        private final String desc;
        private final String usage;
        private final String permissionNode;
        private final Method method;
        
        private CardboardSubcommand(final String name, final String desc, final String usage, final String permissionNode, final Method method) {
            this.name = name;
            this.desc = desc;
            this.usage = usage;
            this.permissionNode = permissionNode;
            this.method = method;
        }
    }
}
