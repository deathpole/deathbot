package net.deathpole.deathbot.Enums;

/**
 * Created by nicolas on 28/09/17.
 */
public enum EnumDynoAction {

    AFK,
    // ADMIN,
    AVATAR,
    // BAN,
    CLEAN,
    DIAGNOSE,
    FLIP,
    GOOGLE,
    HELP,
    IGNORED,
    INFO,
    // KICK,
    MEMBERCOUNT,
    // MOD,
    MODLOGS,
    // MUTE,
    OW,
    PING,
    PURGE,
    RANDOMCOLOR,
    RANKS,
    ROLEINFO,
    ROLES,
    ROLL,
    RPS,
    SERVERINVITE,
    SERVERINFO,
    SOFTBAN,
    // UNBAN,
    // UNDEAFEN,
    // UNMUTE,
    UPTIME,
    // WARN,
    // WARNINGS,
    WHOIS;

    public static EnumDynoAction fromValue(String v) {
        return EnumDynoAction.valueOf(v.toUpperCase());
    }
}
