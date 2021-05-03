package be.dezijwegel.bettersleeping.vetolist;

import org.bukkit.OfflinePlayer;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ObjectiveVetoList extends VetoList {
    private final String objectiveName;
    private final Scoreboard scoreboard;

    public ObjectiveVetoList(@NotNull String objectiveName, @NotNull Scoreboard scoreboard) {
        this.objectiveName = objectiveName;
        this.scoreboard = scoreboard;
    }

    @NotNull
    private Score getScore(@NotNull OfflinePlayer player) {
        initializeList();

        Objects.requireNonNull(player);
        Objects.requireNonNull(player.getName());

        Objective objective = Objects.requireNonNull(scoreboard.getObjective(objectiveName));

        return objective.getScore(player.getName());
    }

    @Override
    public void initializeList() {
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            scoreboard.registerNewObjective(objectiveName, "dummy", "Veto List", RenderType.HEARTS);
        }
    }

    @Override
    public boolean getVetoStatus(@NotNull OfflinePlayer player) {
        Score score = getScore(player);
        if (!score.isScoreSet()) {
            score.setScore(0);
        }

        return score.getScore() != 0;
    }

    @Override
    public void setVetoStatus(@NotNull OfflinePlayer player, boolean veto) {
        getScore(player).setScore(veto ? 1 : 0);
    }
}
