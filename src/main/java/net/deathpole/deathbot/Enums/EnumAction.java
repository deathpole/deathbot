package net.deathpole.deathbot.Enums;

/**
 * Created by nicolas on 26/09/17.
 */
public enum EnumAction {
    ACR,
    ACTIVATE,
    ADD_NOTSINGLE_RANK,
    ADD_RANK,
    ADD_VOICE_ROLE,
    // AIDE,
    CADAVRE,
    CHANGE,
    DEACTIVATE,
    LARO,
    LIST,
    MAKE,
    RANK,
    REMOVE_NOTSINGLE_RANK,
    REMOVE_RANK,
    REVIVE,
    SET_WELCOME_MESSAGE,
    SINGLE,
    SUDO,
    WITH,
    WITHOUT;

    public static EnumAction fromValue(String v){
        return EnumAction.valueOf(v.toUpperCase());
    }

}
