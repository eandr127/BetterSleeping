package be.dezijwegel.bettersleeping.commands;

import be.dezijwegel.bettersleeping.commands.bscommands.BsCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TabCompleter implements org.bukkit.command.TabCompleter {


    private final Map<String, BsCommand> playerCommands;    // Every Player subcommand mapped to its BsCommand object
    private final Map<String, BsCommand> consoleCommands;   // Every console subcommand mapped to its BsCommand object


    public TabCompleter(Map<String, BsCommand> playerCommands, Map<String, BsCommand> consoleCommands)
    {
        this.playerCommands = playerCommands;
        this.consoleCommands = consoleCommands;
    }


    /**
     * This will return a sorted list of type list, containing all allowed commands for a CommandSender
     *
     * @param cs which CommandSender needs to be checked
     * @param partialMatch enforces each command to start with the given String
     * @return a list of possible commands
     */
    private List<String> getAllowedCommands( @NotNull  Map<String, BsCommand> commands, @NotNull CommandSender cs, @Nullable String partialMatch )
    {
        List<String> options = new ArrayList<>();

        // Get the allowed commands for this CommandSender
        for(Map.Entry<String, BsCommand> entry : commands.entrySet())
        {
            String cmdName = entry.getKey();
            BsCommand command = entry.getValue();

            if (cs.hasPermission( command.getPermission() ))
                options.add( cmdName );
        }


        List<String> matches = new ArrayList<>();
        // Only keep the matches
        if (partialMatch != null)
        {
            StringUtil.copyPartialMatches( partialMatch, options, matches );
        }


        // Sort the result
        Collections.sort(matches);

        return matches;
    }


    /**
     * This will return a sorted list of type list, containing all allowed commands for a CommandSender
     *
     * @param cs which CommandSender needs to be checked
     * @return a list of possible commands
     */
    private List<String> getAllowedCommands( @NotNull Map<String, BsCommand> commands, @NotNull CommandSender cs)
    {
        return getAllowedCommands(commands, cs, null);
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String alias, @NotNull String[] arguments)
    {
        arguments = CommandHandler.handleAlias(alias, arguments);

        // Only support console and player
        if ( !(commandSender instanceof Player) && !(commandSender instanceof ConsoleCommandSender))
            return null;

        // Get the right commands
        Map<String, BsCommand> commands;
        commands = commandSender instanceof Player ? playerCommands : consoleCommands;

        // Return the correct list of possible commands
        if (arguments.length == 0)

            return getAllowedCommands(commands, commandSender);

        else {

            List<String> allowedCommands = getAllowedCommands(commands, commandSender, arguments[0]);

            if (allowedCommands.size() == 1 && allowedCommands.get(0).equals(arguments[0])) {
                BsCommand cmd = commands.get(allowedCommands.get(0));

                if(cmd instanceof org.bukkit.command.TabCompleter) {
                    org.bukkit.command.TabCompleter tabCompleter = (org.bukkit.command.TabCompleter) cmd;
                    return tabCompleter.onTabComplete(commandSender, command, alias, arguments);
                }
            }

            if (arguments.length == 1) {
                return allowedCommands;
            } else {
                return new ArrayList<>();
            }
        }
    }

}
