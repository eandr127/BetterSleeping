package be.dezijwegel.bettersleeping.commands.bscommands;

import be.dezijwegel.bettersleeping.messaging.Messenger;
import be.dezijwegel.bettersleeping.messaging.MsgEntry;
import be.dezijwegel.bettersleeping.vetolist.VetoList;
import be.dezijwegel.bettersleeping.vetolist.VetoSetting;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class VetoCommand extends BsCommand {

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

        String flag;
        if (arguments.length < 2)
        {
            flag = VetoSetting.ONE_NIGHT.getName();
        }
        else {
            flag = arguments[1];
        }

        if (flag.equals("get")) {
            boolean isVetoed = vetoList.getVetoStatus(playerSender).isVeto();

            final char CHECKMARK = '\u2714', X = '\u2718';

            if (arguments.length < 3) {
                messenger.sendMessage(commandSender, "veto_status", true, new MsgEntry("<var>",
                        String.valueOf(isVetoed ? CHECKMARK : X)));
            }
            else if(arguments[2].equals("all")) {
                for (Player p : playerSender.getWorld().getPlayers()) {
                    boolean playerVetoed = vetoList.getVetoStatus(p).isVeto();
                    messenger.sendMessage(commandSender, p.getDisplayName() + ": <var>", true, new MsgEntry("<var>",
                            String.valueOf(playerVetoed ? CHECKMARK : X)));
                }
            } else {
                commandSender.sendMessage(ChatColor.RED + "The unknown option '" + arguments[2] + "'. Execute /bs veto get [all]");
            }
        } else if (flag.equals("beg")) {
            beg.execute(commandSender, command, alias, new String[]{arguments[1]});
        }
        else {
            Optional<VetoSetting> setting = VetoSetting.settingFromString(flag.toLowerCase());

            setting.ifPresentOrElse(x -> {
                // Using shorthand shouldn't remove existing veto
                if (arguments.length < 2 && vetoList.getVetoStatus(playerSender).isVeto()) {
                    commandSender.sendMessage(ChatColor.RED + "Already vetoing");
                    return;
                }

                commandSender.sendMessage("Setting veto status to " + x.getName());
                vetoList.setVetoStatus(playerSender, x);
            }, () -> commandSender.sendMessage(ChatColor.RED + "The unknown option '" + arguments[1] + "'. Execute /bs veto [" +
                    Arrays.stream(VetoSetting.values())
                            .map(VetoSetting::getName)
                            .collect(Collectors.joining("/")) + "]"));
        }

        return true;
    }


    @Override
    public String getPermission()
    {
        return "bettersleeping.veto";
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
}
