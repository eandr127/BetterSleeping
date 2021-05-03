package be.dezijwegel.bettersleeping.runnables;

import be.dezijwegel.bettersleeping.events.custom.TimeSetToDayEvent;
import be.dezijwegel.bettersleeping.interfaces.SleepersNeededCalculator;
import be.dezijwegel.bettersleeping.messaging.Messenger;
import be.dezijwegel.bettersleeping.messaging.MsgEntry;
import be.dezijwegel.bettersleeping.messaging.ScreenMessenger;
import be.dezijwegel.bettersleeping.sleepersneeded.AbsoluteNeeded;
import be.dezijwegel.bettersleeping.timechange.TimeChanger;
import be.dezijwegel.bettersleeping.util.Debugger;
import be.dezijwegel.bettersleeping.util.SleepStatus;
import be.dezijwegel.bettersleeping.vetolist.VetoList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class SleepersRunnable extends BukkitRunnable {

    // Final data
    private final World world;
    private final Set<UUID> sleepers;
    private final Set<UUID> customSleepers;
    private final HashMap<UUID, Long> bedLeaveTracker;

    // Utility
    private final SleepersNeededCalculator sleepersCalculator;
    private final TimeChanger timeChanger;
    private final Messenger messenger;
    private final ScreenMessenger screenMessenger;
    private final VetoList vetoList;

    // Variables for internal working
    private int numNeeded;
    private long oldTime;
    private boolean areAllPlayersSleeping = false;

    private final Debugger debugger = new Debugger();

    /**
     * A runnable that will detect time changes and its cause
     */
    public SleepersRunnable(World world, Messenger messenger, ScreenMessenger screenMessenger, TimeChanger timeChanger, SleepersNeededCalculator sleepersCalculator, VetoList vetoList) {
        this.world = world;
        this.messenger = messenger;
        this.screenMessenger = screenMessenger;
        this.oldTime = world.getTime();
        this.timeChanger = timeChanger;
        this.sleepersCalculator = sleepersCalculator;
        this.numNeeded = sleepersCalculator.getNumNeeded(world);
        this.sleepers = new HashSet<>();
        this.customSleepers = new HashSet<>();
        this.bedLeaveTracker = new HashMap<>();
        this.vetoList = vetoList;
    }

    /**
     * Fakes a player entering their bed
     * This allows non-sleeping players to fake sleeping
     *
     * @param player the player who should count as a sleeping player
     */
    public void playerCustomEnterBed(Player player)
    {
        this.customSleepers.add( player.getUniqueId() );
        this.playerEnterBed( player );
    }

    /**
     * Fakes a player leaving their bed
     * This allows non-sleeping players to fake getting out of bed
     *
     * @param player the player who should no longer count as a sleeping player
     */
    public void playerCustomLeaveBEd(Player player)
    {
        this.customSleepers.remove( player.getUniqueId() );
        this.playerLeaveBed( player );
    }

    /**
     * Mark a player as sleeping
     * A reference to the player will be stored
     * @param player the now sleeping player
     */
    public void playerEnterBed(Player player) {
        this.sleepers.add(player.getUniqueId());
        this.bedLeaveTracker.remove(player.getUniqueId());

        // Check whether all players are sleeping
        if (this.sleepers.size() == this.world.getPlayers().size()) {
            this.areAllPlayersSleeping = true;
        }

        this.numNeeded = this.sleepersCalculator.getNumNeeded(this.world);

        int remaining = Math.max(this.numNeeded - this.sleepers.size() , 0);

        this.messenger.sendMessage(
            player,"bed_enter_message", false,
            new MsgEntry("<num_sleeping>", "" + this.sleepers.size()),
            new MsgEntry("<needed_sleeping>", "" + this.numNeeded),
            new MsgEntry("<remaining_sleeping>", "" + remaining)
        );

        boolean noVetoedSleep = this.world.getPlayers()
                .stream()
                .noneMatch(p -> vetoList.getVetoStatus(p).isVeto() && !this.sleepers.contains(p.getUniqueId()));

        boolean isEnoughSleepingEmpty = false;
        if (this.sleepers.size() == this.numNeeded && noVetoedSleep) {
            List<Player> players = this.world.getPlayers();
            players.removeIf(x -> vetoList.getVetoStatus(x).isVeto() || sleepers.contains(x.getUniqueId()) );

            screenMessenger.sendMessage(players, "At least one player is sleeping. Type /ns if you wish stay up for this night.", false);

            isEnoughSleepingEmpty = ! this.messenger.sendMessage(
                this.world.getPlayers(), "enough_sleeping", false,
                new MsgEntry("<player>", ChatColor.stripColor(player.getName())),
                new MsgEntry("<num_sleeping>", "" + this.sleepers.size()),
                new MsgEntry("<needed_sleeping>", "" + this.numNeeded),
                new MsgEntry("<remaining_sleeping>", "" + remaining)
            );
        }

        if (this.sleepers.size() < this.numNeeded || !noVetoedSleep) {
            List<Player> players = this.world.getPlayers();
            players.removeIf( x -> !vetoList.getVetoStatus(x).isVeto() || sleepers.contains(x.getUniqueId()) );
            screenMessenger.sendMessage(players, "At least one player is sleeping, but your preference is set to stay up during the night.", false);
        }

        if (isEnoughSleepingEmpty || this.sleepers.size() < this.numNeeded) {
            List<Player> players = this.world.getPlayers();
            players.remove( player );
            messenger.sendMessage(
                players, "bed_enter_broadcast", false,
                new MsgEntry("<player>", ChatColor.stripColor(player.getName())),
                new MsgEntry("<num_sleeping>", "" + this.sleepers.size()),
                new MsgEntry("<needed_sleeping>", "" + this.numNeeded),
                new MsgEntry("<remaining_sleeping>", "" + remaining)
            );
        }
    }

    public SleepStatus getSleepStatus() {
        int set = this.sleepersCalculator.getSetting();
        String setting = ((this.sleepersCalculator instanceof AbsoluteNeeded)
            ? "An absolute amount of players has to sleep: " + set
            : set + "% of players needs to sleep"
        );

        return new SleepStatus(this.sleepers.size(), this.numNeeded, this.world, this.timeChanger.getType(), setting);
    }

    /**
     * Mark a player as awake
     * The player's reference will be deleted
     * @param player the now awake player
     */
    public void playerLeaveBed(Player player)
    {

        int previousSize = this.sleepers.size();
        this.sleepers.remove(player.getUniqueId());
        this.bedLeaveTracker.put(player.getUniqueId(), this.world.getTime());

        this.numNeeded = this.sleepersCalculator.getNumNeeded(this.world);

        // Don't send cancelled messages when the time is not right
        if (this.world.getTime() < 20 || this.world.getTime() > 23450) {
            return;
        } else
        {
            this.areAllPlayersSleeping = false;
        }

        // Check if enough players WERE sleeping but now not anymore
        boolean tooFewSleepers =
                this.sleepers.size() < previousSize &&
                previousSize >= this.numNeeded &&
                this.sleepers.size() < this.numNeeded;
        boolean vetoedSleeperLeft = vetoList.getVetoStatus(player).isVeto();
        if ((tooFewSleepers || vetoedSleeperLeft) && !this.timeChanger.removedStorm(false)
        ) {
            int remaining = this.numNeeded - this.sleepers.size();
            this.messenger.sendMessage(
                this.world.getPlayers(), "skipping_canceled", false,
                new MsgEntry("<player>", ChatColor.stripColor(player.getDisplayName())),
                new MsgEntry("<num_sleeping>", "" + this.sleepers.size()),
                new MsgEntry("<needed_sleeping>", "" + this.numNeeded),
                new MsgEntry("<remaining_sleeping>", "" + remaining)
            );
        }
    }

    /**
     * Delete the player from all internal lists
     * @param player the player to be deleted
     */
    public void playerLogout(Player player) {
        this.playerLeaveBed(player);
        this.customSleepers.remove( player.getUniqueId() );
        this.bedLeaveTracker.remove(player.getUniqueId());

        // Update the needed count when players leave their bed so that the count is adjusted
        this.numNeeded = this.sleepersCalculator.getNumNeeded(this.world);
    }

    @Override
    public void run() {

        // Time check subsystem: detect time set to day
        long currentTime = this.world.getTime();

        // Beds can now be used in any weather
        if (currentTime == 12542)
        {
            messenger.sendMessage( this.world.getPlayers(), "sleep_possible_now", false, new MsgEntry("<needed_sleeping>", "" + this.numNeeded) );
        }
        // Beds can be used in any weather, one minute from now
        else if (currentTime == 11342)
        {
            messenger.sendMessage( this.world.getPlayers(), "sleep_possible_soon", false, new MsgEntry("<needed_sleeping>", "" + this.numNeeded) );
        }

        // True if time is set to day OR the storm was skipped
        if (
            (currentTime < 10 && currentTime < this.oldTime) ||
            (this.timeChanger.removedStorm(false))
        ) {
            // Find players who slept
            if (this.areAllPlayersSleeping)
            {
                // Filter NPC's from this list
                List<Player> sleepers = this.world.getPlayers().stream()
                        .filter(player -> !player.hasMetadata("NPC") )
                        .collect(Collectors.toList());

                sleepers.forEach(player -> this.sleepers.add( player.getUniqueId() ));
            }
            else
            {
                for (Map.Entry<UUID, Long> entry : this.bedLeaveTracker.entrySet()) {
                    UUID uuid = entry.getKey();
                    Player player = Bukkit.getPlayer( uuid );
                    if ( (entry.getValue() < 10 || entry.getValue() >= 23450) && player != null && !player.hasMetadata("NPC") ) {
                        this.sleepers.add(entry.getKey());
                    }
                }
            }

            // Find the skip cause
            TimeSetToDayEvent.Cause cause;
            if (this.timeChanger.removedStorm( true )) {
                cause = TimeSetToDayEvent.Cause.SLEEPING;
            } else if (timeChanger.wasTimeSetToDay()) { // Caused by BetterSleeping?
                cause = TimeSetToDayEvent.Cause.SLEEPING;
            } else if (areAllPlayersSleeping) { // Caused by all players in a world sleeping -> Time is set to day instantly
                cause = TimeSetToDayEvent.Cause.SLEEPING;
            } else if (currentTime == 0 && oldTime == 23999) { // Natural passing of time?
                cause = TimeSetToDayEvent.Cause.NATURAL;
            } else { // Caused by some time setter?
                cause = TimeSetToDayEvent.Cause.OTHER;
            }

            debugger.debug( "Night skip detected in world " + this.world.getName() + ". Cause: " + cause.toString(), Debugger.DebugLevel.INFORMATIVE);

            // Doesn't consider players that logged out, but that should be ok
            for (Player p : this.world.getPlayers()) {
                vetoList.setVetoStatus(p, vetoList.getVetoStatus(p).getNextMorningState());
            }

            if (cause != TimeSetToDayEvent.Cause.NATURAL) {
                // Send good morning, only when the players slept
                messenger.sendMessage(this.world.getPlayers(), "morning_message", false);
            }

            // Throw event for other devs to handle (and to handle buffs internally)
            List<Player> nonSleepers = this.world.getPlayers();
            nonSleepers.removeIf( player -> this.sleepers.contains( player.getUniqueId() ));
            List<Player> actualSleepers = new ArrayList<>();
            this.sleepers.forEach( uuid -> actualSleepers.add( Bukkit.getPlayer( uuid ) ) );
            Event timeSetToDayEvent = new TimeSetToDayEvent(world, cause, actualSleepers, nonSleepers);
            Bukkit.getPluginManager().callEvent(timeSetToDayEvent);

            // Reset state
            this.areAllPlayersSleeping = false;
            this.bedLeaveTracker.clear();
            this.sleepers.clear();
        }

        this.oldTime = currentTime;

        // SLEEP HANDLER

        // Find all players that are no longer sleeping and remove them from the list (Also remove NPCs)
        List<UUID> awakePlayers = new ArrayList<>();
        for (UUID uuid : this.sleepers)
        {
            Player player = Bukkit.getPlayer( uuid );
            if (player == null || !player.isSleeping() || player.hasMetadata("NPC"))
                // Ignore custom sleeping players
                if (player == null || !this.customSleepers.contains( player.getUniqueId() ))
                    awakePlayers.add( uuid );
        }

        this.sleepers.removeAll( awakePlayers );

        boolean noVetoedSleep = this.world.getPlayers()
                .stream()
                .noneMatch(p -> vetoList.getVetoStatus(p).isVeto() && !this.sleepers.contains(p.getUniqueId()));

        if (this.sleepers.size() >= this.numNeeded && noVetoedSleep) {
            this.timeChanger.tick(this.sleepers.size(), this.numNeeded);
        }
    }
}
