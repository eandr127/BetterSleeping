package be.dezijwegel.bettersleeping.vetolist;

import java.util.Arrays;
import java.util.Optional;

public enum VetoSetting {
    ALWAYS("dontskip", "Not skipping night", true),
    NEVER("alwaysskip", "Always skipping night", false),
    ONE_NIGHT("onenight", "Not skipping just tonight", true);

    static {
        ALWAYS.nextMorningState = ALWAYS;
        NEVER.nextMorningState = NEVER;
        ONE_NIGHT.nextMorningState = NEVER;
    }

    private final String name;
    private final String messageId;
    private final boolean isVeto;

    // Must set after all enums are initialized
    private VetoSetting nextMorningState;

    VetoSetting(String name, String messageId, boolean isVeto) {
        this.name = name;
        this.messageId = messageId;
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

    public String getMessageId(boolean withPlayerPrefix) {
        return (withPlayerPrefix ? "<player>: " : "") + messageId;
    }

    public boolean isVeto() {
        return isVeto;
    }

    public VetoSetting getNextMorningState() {
        return nextMorningState;
    }
}