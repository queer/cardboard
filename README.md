# cardboard

A magical modularisation system for your Bukkit plugins.

cardboard automatically takes care of commands, listeners, components, and
dependency injection.

cardboard requires Spigot 1.13.2 or higher.

## Features

- Classes that implement the Bukkit listener interface are automatically loaded
  and registered.
- Individual components can be registered with `@Component`, and singleton
  components can be registered with `@Single @Component`.
- Commands are automatically registered with `@Component` - no `plugin.yml`
  needed
- Default commands (`@Default`) and subcommands (`@Command`)
- Full permissions for all commands and subcommands
- Configuration can be automatically injected into components and commands with
  `@Config("config.yml.path.here")` (primitives only, no lists etc.).
- Components can extend `LoadableComponent` for more complicated configuration
  and initialization needs (not required!).
- Components and other data can be automatically injected into command and
  component fields via `@Auto`, ex `@Auto private Player player` for a command,
  or `@Auto private MyComponent component`. This means you can have things like
  your database interface as a component that gets injected automatically.

## Example usage

config.yml:
```YAML
test-block:
  value: 1234

test-block-2:
  value: "test string"
```
MyPlugin.java:
```Java
package me.whatever.plugin;

import gg.amy.mc.cardboard.Cardboard;

public class MyPlugin extends Cardboard {
}
```
TestComponent.java:
```Java
package me.whatever.plugin;

import gg.amy.mc.cardboard.component.Component;

@Component(name = "Test", description = "Test description")
public class TestComponent {
}
```
TestCommand.java:
```Java
package me.whatever.plugin;

import gg.amy.mc.cardboard.command.Command;
import gg.amy.mc.cardboard.command.Default;
import gg.amy.mc.cardboard.command.Subcommand;
import gg.amy.mc.cardboard.config.Config;
import gg.amy.mc.cardboard.di.Auto;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "test", permissionNode = "cardboard.test")
public class TestCommand {
    @Auto
    private Player player;
    @Auto
    private CommandSender sender;
    @Auto
    private TestComponent component;
    
    @Config("test-block.value")
    private int number;
    @Config("test-block-2.value")
    private String thing;
    
    @Default
    @Subcommand(value = "di", permissionNode = "cardboard.test.di")
    public void base(final String cmd, final String[] args) {
        sender.sendMessage("Sender = " + sender + ", player = " + player + ", component = " + component);
    }
    
    @Subcommand(value = "config", permissionNode = "cardboard.test.config")
    public void config(final String cmd, final String[] args) {
        sender.sendMessage("number = " + number + ", thing = " + thing);
    }
}
```
plugin.yml
```YAML
name: "MyPlugin"
main: "me.whatever.plugin.MyPlugin"
version: "0.1.0"
api-version: "1.13"
```
