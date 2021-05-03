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

    private void initializeList() {
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            scoreboard.registerNewObjective(objectiveName, "dummy", "Veto List", RenderType.HEARTS);
        }
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
    public VetoSetting getVetoStatus(@NotNull OfflinePlayer player) {
        Score score = getScore(player);
        if (!score.isScoreSet()) {
            score.setScore(0);
        }

        int scoreValue = score.getScore();
        if (scoreValue < 0) {
            return VetoSetting.ALWAYS;
        } else if (scoreValue > 0) {
            // This might change to number of nights to not skip
            return VetoSetting.ONE_NIGHT;
        } else {
            return VetoSetting.NEVER;
        }
    }

    @Override
    public void setVetoStatus(@NotNull OfflinePlayer player, @NotNull VetoSetting veto) {
        Objects.requireNonNull(veto);

        int scoreValue;
        switch(veto) {
            case ALWAYS:
                scoreValue = -1;
                break;
            case NEVER:
                scoreValue = 0;
                break;
            case ONE_NIGHT:
                scoreValue = 1;
                break;
            default:
                throw new UnsupportedOperationException("Unknown veto settting: " + veto);
        }
        getScore(player).setScore(scoreValue);
    }
}
