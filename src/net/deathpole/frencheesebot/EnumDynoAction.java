package net.deathpole.frencheesebot;

/**
 * Created by nicolas on 28/09/17.
 */
public enum EnumDynoAction {

    HELP,
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
    ROLES;

    public static EnumDynoAction fromValue(String v) {
        return EnumDynoAction.valueOf(v.toUpperCase());
    }
}
