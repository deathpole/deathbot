package net.deathpole.deathbot;

/**
 * Created by nicolas on 04/10/17.
 */
public class CustomReactionDTO {

    private String reaction;

    private int numberOfParams;

    public String getReaction() {
        return reaction;
    }

    public void setReaction(String reaction) {
        this.reaction = reaction;
    }

    public int getNumberOfParams() {
        return numberOfParams;
    }

    public void setNumberOfParams(int numberOfParams) {
        this.numberOfParams = numberOfParams;
    }
}
