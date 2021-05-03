package be.dezijwegel.bettersleeping.vetolist;

import java.util.Arrays;
import java.util.Optional;

public enum VetoSetting {
    ALWAYS("dontskip", true),
    NEVER("alwaysskip", false),
    ONE_NIGHT("onenight", true);

    static {
        ALWAYS.nextMorningState = ALWAYS;
        NEVER.nextMorningState = NEVER;
        ONE_NIGHT.nextMorningState = NEVER;
    }

    private final String name;
    private final boolean isVeto;

    // Must set after all enums are initialized
    private VetoSetting nextMorningState;

    VetoSetting(String name, boolean isVeto) {
        this.name = name;
        this.isVeto = isVeto;
    }

    public static Optional<VetoSetting> settingFromString(String str) {
        return Arrays.stream(VetoSetting.values())
                .filter(setting -> setting.name.equals(str))
                .findAny();
    }

    public String getName() {
        return name;
    }

    public boolean isVeto() {
        return isVeto;
    }

    public VetoSetting getNextMorningState() {
        return nextMorningState;
    }
}