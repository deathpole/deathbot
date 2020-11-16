package net.deathpole.deathbot.Services;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Created by nicolas on 28/09/17.
 */
public interface IHelperService {

    String convertMillisToDaysHoursMinSec(long millis);

    public String formatBigNumbersToEFFormat(BigDecimal value);

    BigDecimal convertEFLettersToNumber(String amountWithLetter);

    LocalDateTime generateNextExecutionTime(String cronTab, LocalDateTime nextExecutionTime);
}
