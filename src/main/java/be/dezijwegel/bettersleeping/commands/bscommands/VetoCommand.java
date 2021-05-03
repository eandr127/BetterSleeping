package be.dezijwegel.bettersleeping.commands.bscommands;

import be.dezijwegel.bettersleeping.messaging.Messenger;
import be.dezijwegel.bettersleeping.messaging.MsgEntry;
import be.dezijwegel.bettersleeping.vetolist.VetoList;
import be.dezijwegel.bettersleeping.vetolist.VetoSetting;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VetoCommand extends BsCommand implements TabCompleter {

    private final VetoList vetoList;
    private final ShoutCommand beg;

    public VetoCommand(Messenger messenger, VetoList vetoList)
    {
        super( messenger );

        this.vetoList = vetoList;
        this.beg = new ShoutCommand(messenger);
    }


    @Override
    public boolean execute(CommandSender commandSender, Command command, String alias, String[] arguments)
    {
        if ( ! commandSender.hasPermission( getPermission() ))
        {
            messenger.sendMessage(commandSender, "no_permission", true, new MsgEntry("<var>", "/bs " + arguments[0]));
            return true;
        }

        Player playerSender = (Player)commandSender;

        String options = "/ns [" +
            Stream.concat(Arrays.stream(VetoSetting.values())
                .map(VetoSetting::getName), Stream.of("get", "beg", "help"))
                .collect(Collectors.joining("/")) + "]";

        String flag;
        if (arguments.length < 2)
        {
            flag = VetoSetting.ONE_NIGHT.getName();
        }
        else {
            flag = arguments[1];
        }

        switch (flag) {
            case "get":
                VetoSetting vetoStatus = vetoList.getVetoStatus(playerSender);

                if (arguments.length < 3) {
                    messenger.sendMessage(commandSender, vetoStatus.getMessageId(false), true);
                } else if (arguments[2].equals("all")) {
                    for (Player p : playerSender.getWorld().getPlayers()) {
                        messenger.sendMessage(commandSender, vetoList.getVetoStatus(p).getMessageId(true), true,
                                new MsgEntry("<player>", p.getDisplayName()));
                    }
                } else {
                    commandSender.sendMessage(ChatColor.RED + "The unknown option '" + arguments[2] + "'. Execute /bs veto get [all]");
                }
                break;
            case "beg":
                beg.execute(commandSender, command, alias, new String[]{arguments[1]});
                break;
            case "help":
                commandSender.sendMessage(ChatColor.GREEN + "Usage: " + options);
                break;
            default:
                Optional<VetoSetting> setting = VetoSetting.settingFromString(flag.toLowerCase());

                setting.ifPresentOrElse(x -> {
                    // Using shorthand shouldn't remove existing veto
                    if (arguments.length < 2 && vetoList.getVetoStatus(playerSender).isVeto()) {
                        commandSender.sendMessage(ChatColor.RED + "Already vetoing");
                        return;
                    }

                    messenger.sendMessage(commandSender, x.getMessageId(false), true);
                    vetoList.setVetoStatus(playerSender, x);
                }, () -> commandSender.sendMessage(ChatColor.RED + "The unknown option '" + arguments[1] + "'. Execute " + options));
                break;
        }

        return true;
    }


    @Override
    public String getPermission()
    {
        return "bukkit.command.help";
    }


    @Override
    public List<String> getDescription()
    {
        return new ArrayList<>() {{
            add("Sets whether player must be sleeping to skip night");
        }};
    }

    @Override
    public String getDescriptionAsString() {
        return "Sets whether player must be sleeping to skip night";
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length < 2) {
            return new ArrayList<>();
        } else if (args.length == 2) {
            List<String> matches = new ArrayList<>();
            StringUtil.copyPartialMatches(args[1], Stream.concat(Stream.of("", "get", "beg", "help"), Arrays.stream(VetoSetting.values()).map(VetoSetting::getName)).collect(Collectors.toList()), matches);
            return matches;
        } else if (args.length == 3) {
            if (args[1].equals("get")) {
                if("all".startsWith(args[2])) {
                    return List.of("all");
                }
            }
        }

        return List.of();
    }
}
