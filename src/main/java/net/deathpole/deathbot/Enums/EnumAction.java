package net.deathpole.deathbot.Enums;

/**
 * Created by nicolas on 26/09/17.
 */
public enum EnumAction {
    RANK, ACR, ADD_RANK, REMOVE_RANK, SINGLE, LIST, CHANGE, MAKE, SUDO, HELPFCB, CADAVRE, WITHOUT, WITH, DEACTIVATE, ACTIVATE;

    public static EnumAction fromValue(String v){
        return EnumAction.valueOf(v.toUpperCase());
    }

}
