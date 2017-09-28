package net.deathpole.frencheesebot;

/**
 * Created by nicolas on 26/09/17.
 */
public enum EnumAction {
    RANK, ACC, ADD_RANK, REMOVE_RANK, SINGLE, LIST, CHANGE, MAKE, SUDO, HELPFCB;

    public static EnumAction fromValue(String v){
        return EnumAction.valueOf(v.toUpperCase());
    }

}
