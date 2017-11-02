package net.deathpole.deathbot.Enums;

/**
 * Created by nicolas on 26/09/17.
 */
public enum EnumAction {
    RANK,
    ACR,
    ADD_RANK,
    REMOVE_RANK,
    SINGLE,
    LIST,
    CHANGE,
    MAKE,
    SUDO,
    HELPDB,
    CADAVRE,
    WITHOUT,
    WITH,
    DEACTIVATE,
    ACTIVATE,
    LARO,
    ADD_NOTSINGLE_RANK,
    REMOVE_NOTSINGLE_RANK,
    SET_WELCOME_MESSAGE;

    public static EnumAction fromValue(String v){
        return EnumAction.valueOf(v.toUpperCase());
    }

}
