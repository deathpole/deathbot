package net.deathpole.deathbot.Enums;

/**
 * Created by nicolas on 28/09/17.
 */
public enum EnumDynoAction {

    HELP,
    AIDE,
    OW,
    AFK,
    AVATAR,
    GOOGLE,
    INFO,
    MEMBERCOUNT,
    RANDOMCOLOR,
    ROLL,
    RPS,
    SERVERINVITE,
    UPTIME,
    WHOIS,
    BAN,
    CLEAN,
    DIAGNOSE,
    IGNORED,
    KICK,
    MODLOGS,
    MUTE,
    SOFTBAN,
    UNBAN,
    UNDEAFEN,
    UNMUTE,
    WARN,
    WARNINGS,
    RANKS,
    ROLEINFO,
    ELF,
    ORC,
    HUMAN,
    UNDEAD,
    GEM,
    TOUR,
    PET,
    REZ,
    PUSH,
    SR,
    UNIT1,
    UNIT2,
    RAID,
    GVG,
    ARTEFACTS,
    TRANS,
    PETUNIT,
    SHEETS,
    PETSHEET,
    QUOTA,
    KL,
    FION,
    PURGE,
    NINJA,
    DYNO,
    ROLES;

    public static EnumDynoAction fromValue(String v) {
        return EnumDynoAction.valueOf(v.toUpperCase());
    }
}
