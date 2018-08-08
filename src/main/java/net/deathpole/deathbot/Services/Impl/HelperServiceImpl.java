package net.deathpole.deathbot.Services.Impl;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');

        if (value.compareTo(medalsBase) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##a", symbols);
            result = decimalFormat.format(value.divide(factor, 1, BigDecimal.ROUND_HALF_DOWN));
        } else if (value.compareTo(medalsBase.multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##b", symbols);
            result = decimalFormat.format(value.divide(factor).divide(factor, 1, BigDecimal.ROUND_HALF_DOWN));
        } else if (value.compareTo(medalsBase.multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##c", symbols);
            result = decimalFormat.format(value.divide(factor.multiply(factor)).divide(factor, 1, BigDecimal.ROUND_HALF_DOWN));
        } else if (value.compareTo(medalsBase.multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##d", symbols);
            result = decimalFormat.format(value.divide(factor.multiply(factor).multiply(factor)).divide(factor, 1, BigDecimal.ROUND_HALF_DOWN));
        } else if (value.compareTo(medalsBase.multiply(factor).multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##e", symbols);
            result = decimalFormat.format(value.divide(factor.multiply(factor).multiply(factor).multiply(factor)).divide(factor, 1, BigDecimal.ROUND_HALF_DOWN));
        } else if (value.compareTo(medalsBase.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##f", symbols);
            result = decimalFormat.format(value.divide(factor.multiply(factor).multiply(factor).multiply(factor).multiply(factor)).divide(factor, 1, BigDecimal.ROUND_HALF_DOWN));
        } else if (value.compareTo(medalsBase.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##g", symbols);
            result = decimalFormat.format(
                    value.divide(factor.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)).divide(factor, 1, BigDecimal.ROUND_HALF_DOWN));
        } else if (value.compareTo(medalsBase.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##h", symbols);
            result = decimalFormat.format(value.divide(factor.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)).divide(factor,
                    1, BigDecimal.ROUND_HALF_DOWN));
        } else if (value.compareTo(
                medalsBase.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##i", symbols);
            result = decimalFormat.format(
                    value.divide(factor.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)).divide(factor, 1,
                            BigDecimal.ROUND_HALF_DOWN));
        }else if (value.compareTo(
                medalsBase.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##j", symbols);
            result = decimalFormat.format(
                    value.divide(
                            factor.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)).divide(
                                    factor, 1, BigDecimal.ROUND_HALF_DOWN));
        }

        return result;
    }

    @Override
    public BigDecimal convertEFLettersToNumber(String amountWithLetter) {

        String letter = amountWithLetter.substring(amountWithLetter.length() - 1, amountWithLetter.length());
        String amountWithoutLetter = amountWithLetter;

        if (Character.isLetter(amountWithLetter.charAt(amountWithLetter.length() - 1))) {
            amountWithoutLetter = amountWithLetter.substring(0, amountWithLetter.length() - 1);
        }
        BigDecimal amount = new BigDecimal(amountWithoutLetter);

        if (Character.isLetter(letter.charAt(0))) {
            switch (letter) {
                case "k":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "j":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "i":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "h":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "g":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "f":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "e":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "d":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "c":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "b":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "a":
                    amount = amount.multiply(new BigDecimal(1000L));
                    break;
                default:
                    return BigDecimal.ZERO;
            }
        }
        return amount;
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
