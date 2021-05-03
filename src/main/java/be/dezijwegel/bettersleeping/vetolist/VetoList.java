package be.dezijwegel.bettersleeping.vetolist;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public abstract class VetoList {
    public abstract VetoSetting getVetoStatus(@NotNull OfflinePlayer player);
    public abstract void setVetoStatus(@NotNull OfflinePlayer player, @NotNull VetoSetting setting);
}
