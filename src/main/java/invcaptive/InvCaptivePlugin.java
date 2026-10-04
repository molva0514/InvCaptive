package invcaptive;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class InvCaptivePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("InvCaptive enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("InvCaptive disabled.");
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (command.getName().equalsIgnoreCase("invcaptive")) {
            sender.sendMessage("InvCaptive is working.");
            return true;
        }

        return false;
    }
}