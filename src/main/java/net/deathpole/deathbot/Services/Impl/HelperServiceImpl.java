package net.deathpole.deathbot.Services.Impl;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;

import net.deathpole.deathbot.Services.IHelperService;
import net.deathpole.deathbot.Services.IMessagesService;
import net.deathpole.deathbot.ValueLetterVO;

/**
 * Created by nicolas on 28/09/17.
 */
public class HelperServiceImpl implements IHelperService {

    private IMessagesService messagesService = new MessagesServiceImpl();

    @Override
    public String convertMillisToDaysHoursMinSec(long millis) {

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = millis / daysInMilli;
        millis = millis % daysInMilli;

        long elapsedHours = millis / hoursInMilli;
        millis = millis % hoursInMilli;

        long elapsedMinutes = millis / minutesInMilli;
        millis = millis % minutesInMilli;

        long elapsedSeconds = millis / secondsInMilli;

        StringBuilder answer = new StringBuilder();
        answer.append(elapsedDays).append(" jours ").append(elapsedHours).append(" heures ").append(elapsedMinutes).append(" minutes et ").append(elapsedSeconds).append(
                " secondes.");

        return answer.toString();

    }

    @Override
    public String formatBigNumbersToEFFormat(BigDecimal value) {
        String result = null;

        BigDecimal factor = BigDecimal.valueOf(1000);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');

        value = value.divide(factor, 2, BigDecimal.ROUND_HALF_DOWN);

        String letters = "";

        ValueLetterVO vo = new ValueLetterVO();
        vo.setLetters(letters);
        vo.setValue(value);

        boolean firstRun = true;

        while (vo.getValue().compareTo(factor) >= 0) {
            ValueLetterVO letterFromValue = getLetterFromValue(vo.getValue(), factor, firstRun);
            letters += letterFromValue.getLetters();
            vo.setValue(letterFromValue.getValue());
            firstRun = false;
        }

        DecimalFormat decimalFormat = new DecimalFormat("#.##" + letters, symbols);
        result = decimalFormat.format(vo.getValue());

        return result;
    }

    private ValueLetterVO getLetterFromValue(BigDecimal value, BigDecimal factor, boolean firstRun) {
        ValueLetterVO result = new ValueLetterVO();
        String tempLetter = "a";

        while (value.compareTo(factor) >= 0) {
            value = value.divide(factor, 2, BigDecimal.ROUND_HALF_DOWN);
            result.setValue(value);
            int charValue = tempLetter.charAt(0);
            if (charValue > 121) {
                tempLetter = "a";
                result.setValue(value.multiply(factor));
                result.setLetters(tempLetter);
                return result;
            } else {
                tempLetter = String.valueOf((char) (charValue + 1));
            }
        }

        if (!firstRun) {
            result.setLetters(String.valueOf((char) (tempLetter.charAt(0) - 1)));
        } else {
            result.setLetters(tempLetter);
        }

        return result;
    }

    @Override
    public BigDecimal convertEFLettersToNumber(String amountWithLetter) {

        int numberOfLetters = extractNumberOfLetters(amountWithLetter);

        String letters = amountWithLetter.substring(amountWithLetter.length() - numberOfLetters);
        String amountWithoutLetter = amountWithLetter;

        if (Character.isLetter(amountWithLetter.charAt(amountWithLetter.length() - numberOfLetters))) {
            amountWithoutLetter = amountWithLetter.substring(0, amountWithLetter.length() - numberOfLetters);
        }
        BigDecimal amount = new BigDecimal(amountWithoutLetter);

        if (extractNumberOfLetters(letters) > 0) {
            switch (letters) {
                case "ac":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "ab":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "aa":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "z":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "y":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "x":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "w":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "v":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "u":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "t":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "s":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "r":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "q":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "p":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "o":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "n":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "m":
                    amount = amount.multiply(new BigDecimal(1000L));
                case "l":
                    amount = amount.multiply(new BigDecimal(1000L));
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

    private int extractNumberOfLetters(String amountWithLetter) {
        int charCount = 0;
        for (int i = 0; i < amountWithLetter.length(); i++) {
            char temp = amountWithLetter.charAt(i);
            if (Character.isLetter(temp)) {
                charCount++;
            }
        }

        return charCount;
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
