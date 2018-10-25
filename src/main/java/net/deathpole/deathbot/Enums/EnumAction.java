package net.deathpole.deathbot.Enums;

/**
 * Created by nicolas on 26/09/17.
 */
public enum EnumAction {
    ACR,
    ACTIVATE,
    ADD_NOTSINGLE_RANK,
    ANSR,
    // ADD_ONJOIN_RANK,
    ADD_RANK,
    ADD_RANK_TO_RANK,
    // AREM,
    AR,
    // ADD_REMINDER,
    ADD_GLOBAL_RANK,
    // AOJR,
    AGR,
    ADD_VOICE_ROLE,
    AVR,
    // CADAVRE,
    ASSOCIATE,
    CHANGE,
    CHEVALIER,
    DCR,
    DEACTIVATE,
    IGNORE,
    INACTIVITY,
    ISACTIVE,
    LARO,
    LIST,
    LIST_ASSOCIATED,
    LIST_IGNORE,
    // LIST_ONJOIN_RANKS,
    // LOJR,
    // LIST_REMINDERS,
    // LREM,
    LCR,
    MAKE,
    PUNISH,
    RANK,
    REMOVE_GLOBAL_RANK,
    RGR,
    REMOVE_NOTSINGLE_RANK,
    RNSR,
    // REMOVE_ONJOIN_RANK,
    WITHOUT,
    // ROJR,
    REMOVE_ASSOCIATED,
    REMOVE_IGNORE,
    REMOVE_RANK,
    RR,
    // REMOVE_REMINDER,
    // RREM,
    REVIVE,
    SET_WELCOME_MESSAGE,
    SWM,
    SINGLE,
    SUDO,
    STAT,
    STAT2,
    STATCANCEL,
    STAT2CANCEL,
    TIME,
    WITH;


    public static EnumAction fromValue(String v){
        return EnumAction.valueOf(v.toUpperCase());
    }

}
