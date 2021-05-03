package be.dezijwegel.bettersleeping.vetolist;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public abstract class VetoList {
    public abstract void initializeList();
    public abstract boolean getVetoStatus(@NotNull OfflinePlayer player);
    public abstract void setVetoStatus(@NotNull OfflinePlayer player, boolean veto);
}
