package net.deathpole.deathbot.Services.Impl;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import net.deathpole.deathbot.Services.IHelperService;

/**
 * Created by nicolas on 28/09/17.
 */
class HelperServiceImpl implements IHelperService {

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
}
