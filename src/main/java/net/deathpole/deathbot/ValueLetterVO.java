package net.deathpole.deathbot;

import java.math.BigDecimal;

/**
 * Created by nicolas on 04/10/17.
 */
public class ValueLetterVO {

    private BigDecimal value;

    private String letters;

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getLetters() {
        return letters;
    }

    public void setLetters(String letters) {
        this.letters = letters;
    }
}
