package net.deathpole.deathbot.Services.Impl;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;

import net.deathpole.deathbot.Services.IHelperService;
import net.deathpole.deathbot.Services.IMessagesService;

/**
 * Created by nicolas on 28/09/17.
 */
public class HelperServiceImpl implements IHelperService {

    private IMessagesService messagesService = new MessagesServiceImpl();

    @Override
    public String formatBigNumbersToEFFormat(BigDecimal value) {
        String result = null;

        BigDecimal medalsBase = BigDecimal.valueOf(1000000);
        BigDecimal factor = BigDecimal.valueOf(1000);

        if (value.compareTo(medalsBase) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##a");
            result = decimalFormat.format(value.divide(factor, BigDecimal.ROUND_HALF_DOWN));
        } else if (value.compareTo(medalsBase.multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##b");
            result = decimalFormat.format(value.divide(factor).divide(factor, BigDecimal.ROUND_HALF_DOWN));
        } else if (value.compareTo(medalsBase.multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##c");
            result = decimalFormat.format(value.divide(factor.multiply(factor)).divide(factor, BigDecimal.ROUND_HALF_DOWN));
        } else if (value.compareTo(medalsBase.multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##d");
            result = decimalFormat.format(value.divide(factor.multiply(factor).multiply(factor)).divide(factor, BigDecimal.ROUND_HALF_DOWN));
        } else if (value.compareTo(medalsBase.multiply(factor).multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##e");
            result = decimalFormat.format(value.divide(factor.multiply(factor).multiply(factor).multiply(factor)).divide(factor, BigDecimal.ROUND_HALF_DOWN));
        } else if (value.compareTo(medalsBase.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##f");
            result = decimalFormat.format(value.divide(factor.multiply(factor).multiply(factor).multiply(factor).multiply(factor)).divide(factor, BigDecimal.ROUND_HALF_DOWN));
        } else if (value.compareTo(medalsBase.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##g");
            result = decimalFormat.format(
                    value.divide(factor.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)).divide(factor, BigDecimal.ROUND_HALF_DOWN));
        } else if (value.compareTo(medalsBase.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##h");
            result = decimalFormat.format(value.divide(factor.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)).divide(factor,
                    BigDecimal.ROUND_HALF_DOWN));
        } else if (value.compareTo(
                medalsBase.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##i");
            result = decimalFormat.format(
                    value.divide(factor.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)).divide(factor,
                            BigDecimal.ROUND_HALF_DOWN));
        }

        return result;
    }

    @Override
    public LocalDateTime generateNextExecutionTime(String cronTab, LocalDateTime actualNextExecutionTime) {
        // 6 13 */3

        LocalDateTime nextExecutionTime = LocalDateTime.now();

        String[] values = cronTab.trim().split(" ");
        String minutes = values[0];
        String hours = values[1];
        String days = values[2];

        if (days.contains("*")) {
            if (days.contains("/")) {
                if (actualNextExecutionTime != null) {
                    nextExecutionTime = LocalDateTime.from(actualNextExecutionTime);
                    nextExecutionTime = nextExecutionTime.plusDays(Long.parseLong(days.split("/")[1]));
                }
            } else {
                LocalDateTime tempTime = LocalDateTime.from(nextExecutionTime);
                if ("*".equals(hours)) {
                    if ("*".equals(minutes)) {
                        nextExecutionTime = nextExecutionTime.plusMinutes(1L);
                    } else {
                        tempTime = tempTime.withMinute(Integer.parseInt(minutes));

                        if (tempTime.isBefore(nextExecutionTime)) {
                            nextExecutionTime = nextExecutionTime.withMinute(Integer.parseInt(minutes));
                            nextExecutionTime = nextExecutionTime.plusHours(1L);
                        }
                    }
                } else {
                    if ("*".equals(minutes)) {
                        tempTime = tempTime.withHour(Integer.parseInt(hours));

                        if (tempTime.isBefore(nextExecutionTime)) {
                            nextExecutionTime = nextExecutionTime.withHour(Integer.parseInt(hours));
                            nextExecutionTime = nextExecutionTime.plusDays(1L);
                        }
                    } else {
                        tempTime = tempTime.withHour(Integer.parseInt(hours));
                        tempTime = tempTime.withMinute(Integer.parseInt(minutes));

                        if (tempTime.isBefore(nextExecutionTime)) {
                            nextExecutionTime = nextExecutionTime.withHour(Integer.parseInt(hours));
                            nextExecutionTime = nextExecutionTime.withMinute(Integer.parseInt(minutes));
                            nextExecutionTime = nextExecutionTime.plusDays(1L);
                        }
                    }
                }
            }
        } else {
            if (nextExecutionTime.isAfter(LocalDateTime.from(nextExecutionTime).withDayOfMonth(Integer.parseInt(days)))) {
                nextExecutionTime = nextExecutionTime.withDayOfMonth(Integer.parseInt(days)).plusMonths(1L);
            } else {
                nextExecutionTime = nextExecutionTime.withDayOfMonth(Integer.parseInt(days));
            }
        }

        if (!minutes.contains("*")) {
            if (nextExecutionTime.isAfter(LocalDateTime.from(nextExecutionTime).withMinute(Integer.parseInt(minutes)))) {
                nextExecutionTime = nextExecutionTime.withMinute(Integer.parseInt(minutes)).plusHours(1L);
            } else {
                nextExecutionTime = nextExecutionTime.withMinute(Integer.parseInt(minutes));
            }
        }

        if (!hours.contains("*")) {
            if (nextExecutionTime.isAfter(LocalDateTime.from(nextExecutionTime).withHour(Integer.parseInt(hours)))) {
                nextExecutionTime = nextExecutionTime.withHour(Integer.parseInt(hours)).plusDays(1L);
            } else {
                nextExecutionTime = nextExecutionTime.withHour(Integer.parseInt(hours));
            }
        }

        return nextExecutionTime;
    }
}
