package be.dezijwegel.bettersleeping.commands.bscommands;

import be.dezijwegel.bettersleeping.messaging.Messenger;
import be.dezijwegel.bettersleeping.messaging.MsgEntry;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.*;

public class VetoCommand extends BsCommand {


    public VetoCommand(Messenger messenger)
    {
        super( messenger );
    }


    @Override
    public boolean execute(CommandSender commandSender, Command command, String alias, String[] arguments)
    {
        if ( ! commandSender.hasPermission( getPermission() ))
        {
            messenger.sendMessage(commandSender, "no_permission", true, new MsgEntry("<var>", "/bs " + arguments[0]));
            return true;
        }

        if (arguments.length < 2)
        {
            final char CHECKMARK = '\u2714', X = '\u2718';
            messenger.sendMessage(commandSender, "veto_status", true, new MsgEntry("<var>",
                    String.valueOf(Math.random() > 0.5 ? CHECKMARK : X)));
        }
        else
        {
            Optional<VetoSetting> setting = VetoSetting.settingFromString(arguments[1].toLowerCase());

            setting.ifPresentOrElse(x -> commandSender.sendMessage("Setting veto status to " + x.value),
                    () -> commandSender.sendMessage(ChatColor.RED + "The unknown option '" + arguments[1] + "'. Execute /bs veto [on/off]"));
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
        return new ArrayList<String>() {{
            add("Sets whether player must be sleeping to skip night");
        }};
    }

    @Override
    public String getDescriptionAsString() {
        return "Sets whether player must be sleeping to skip night";
    }

    private enum VetoSetting {
        ON("on", true),
        OFF("off", false);

        final String str;
        final boolean value;

        VetoSetting(String str, boolean value) {
            this.str = str;
            this.value = value;
        }

        static Optional<VetoSetting> settingFromString(String str) {
            return Arrays.stream(VetoSetting.values())
                    .filter(setting -> setting.str.equals(str))
                    .findAny();
        }
    }
}
