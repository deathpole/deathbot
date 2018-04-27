package net.deathpole.deathbot.Services;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Created by nicolas on 28/09/17.
 */
public interface IHelperService {

    public String formatBigNumbersToEFFormat(BigDecimal value);

    LocalDateTime generateNextExecutionTime(String cronTab, LocalDateTime nextExecutionTime);
}
