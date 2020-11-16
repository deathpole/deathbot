package net.deathpole.deathbot.Services.Impl;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import net.deathpole.deathbot.Dao.IGlobalDao;
import net.deathpole.deathbot.PlayerStatDTO;
import net.deathpole.deathbot.Services.IChartService;
import net.deathpole.deathbot.Services.IHelperService;
import net.deathpole.deathbot.Services.IMessagesService;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

/**
 *
 */
public class ChartServiceImpl implements IChartService {

    private IMessagesService messagesService = new MessagesServiceImpl();
    private IGlobalDao globalDao;
    private IHelperService helperService = new HelperServiceImpl();

    public ChartServiceImpl(IGlobalDao globalDao) {
        this.globalDao = globalDao;
    }

    @Override
    public BufferedImage drawAllPlayersSRChart(List<PlayerStatDTO> playersStats, CommandesServiceImpl commandesService) {
        // Create Chart
        XYChart chart = new XYChart(1000, 800);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
        chart.setTitle("Courbes des SR en fonction du KL");
        chart.setXAxisTitle("KL");
        chart.setYAxisTitle("SR(% du total de médailles)");
        chart.getStyler().setYAxisDecimalPattern("#0.0'%'");
        chart.getStyler().setXAxisDecimalPattern("#");

        createSRSerie(playersStats, chart);

        return BitmapEncoder.getBufferedImage(chart);
    }

    private void createSRSerie(List<PlayerStatDTO> playersStats, XYChart chart) {
        List<Number> srs = new ArrayList<>();
        List<Number> kls = new ArrayList<>();

        for (PlayerStatDTO playerStatDTO : playersStats) {
            srs.add(playerStatDTO.getSrPercentage());
            kls.add(playerStatDTO.getKl());
        }

        XYSeries rawSerie = chart.addSeries("SR", kls, srs);
        generateCommonChartProperties(chart, rawSerie);
    }

    @Override
    public BufferedImage drawSRChart(HashMap<LocalDateTime, BigDecimal> playerSRStats) {
        // Create Chart
        List<Number> srs = new ArrayList<>();
        List<Date> dates = new ArrayList<>();
        DecimalFormat dc = new DecimalFormat("#0.00'%'");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");

        List<LocalDateTime> orderedDates = new ArrayList<>(playerSRStats.keySet());

        Collections.sort(orderedDates);

        Map<Double, Object> yMarkMap = new TreeMap<>();
        Map<Double, Object> xMarkMap = new TreeMap<>();

        for (LocalDateTime date : orderedDates) {
            srs.add(playerSRStats.get(date));
            yMarkMap.put(playerSRStats.get(date).doubleValue(), dc.format(playerSRStats.get(date)));
            Date out = Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
            dates.add(out);
            xMarkMap.put(Double.valueOf(out.getTime()), sdf.format(out));
        }

        XYChart chart = new XYChart(1000, 800);
        chart.setTitle("Evolution du SR dans le temps");
        chart.setXAxisTitle("Dates");
        chart.setYAxisTitle("SR(% du total de médailles)");
        chart.getStyler().setDatePattern("dd/MM");
        chart.getStyler().setDecimalPattern("#0.00'%'");
        chart.setYAxisLabelOverrideMap(yMarkMap);
        chart.setXAxisLabelOverrideMap(xMarkMap);
        XYSeries series = chart.addSeries("SR", dates, srs);
        generateCommonChartProperties(chart, series);

        return BitmapEncoder.getBufferedImage(chart);
    }

    public BufferedImage drawComparisonMedChart(HashMap<LocalDateTime, BigDecimal> playerMedStats, List<Member> compareToMembers, User author, HashMap<Member, Color> colorMap) {
        // Create Chart
        List<Number> medals = new ArrayList<>();

        List<Date> dates = new ArrayList<>();

        if (playerMedStats != null && !playerMedStats.isEmpty()) {
            List<LocalDateTime> orderedDates = new ArrayList<>(playerMedStats.keySet());
            Collections.sort(orderedDates);

            XYChart chart = new XYChart(1000, 800);
            chart.setTitle("Comparaison des médailles");
            chart.setXAxisTitle("Dates");
            chart.setYAxisTitle("Nombre total de médailles");
            chart.getStyler().setDatePattern("dd/MM");

            HashMap<Double, Object> yMarksMap = new HashMap<>();

            fillSerieMedalsByDateDataAndLabels(playerMedStats, medals, dates, orderedDates, yMarksMap, true);
            XYSeries series = chart.addSeries("Médailles de " + author.getName(), dates, medals);
            generateCommonChartProperties(chart, series);
            series.setMarker(SeriesMarkers.CIRCLE);
            series.setLineColor(Color.BLUE);
            series.setMarkerColor(Color.BLUE);
            chart.setYAxisLabelOverrideMap(yMarksMap);

            HashMap<Member, List<Date>> datesForComparedMembers = new HashMap<>();
            HashMap<Member, List<Number>> medalsForComparedMembers = new HashMap<>();

            for (Member memberToCompare : compareToMembers) {

                HashMap<LocalDateTime, BigDecimal> memberMedStats = getMedStatsForPlayer(memberToCompare.getUser().getIdLong());
                if (memberMedStats != null && !memberMedStats.isEmpty()) {

                    List<Date> datesCompare = new ArrayList<>();
                    List<Number> medalsCompare = new ArrayList<>();
                    List<LocalDateTime> orderedDatesComparison = new ArrayList<>(memberMedStats.keySet());
                    Collections.sort(orderedDatesComparison);

                    fillSerieMedalsByDateDataAndLabels(memberMedStats, medalsCompare, datesCompare, orderedDatesComparison, null, true);
                    datesForComparedMembers.put(memberToCompare, datesCompare);
                    medalsForComparedMembers.put(memberToCompare, medalsCompare);
                }
            }

            for (Member memberToCompare : medalsForComparedMembers.keySet()) {
                XYSeries comparisonSeries = chart.addSeries("Médailles de " + memberToCompare.getUser().getName(), datesForComparedMembers.get(memberToCompare),
                        medalsForComparedMembers.get(memberToCompare));
                setRandomColor(comparisonSeries, colorMap.get(memberToCompare));
                comparisonSeries.setMarker(SeriesMarkers.NONE);
                makeSeriesDashed(comparisonSeries);
            }

            return BitmapEncoder.getBufferedImage(chart);
        }
        return null;
    }

    public BufferedImage drawComparisonMedChart2(HashMap<LocalDateTime, BigDecimal> playerMedStats, List<Member> compareToMembers, User author, HashMap<Member, Color> colorMap) {
        // Create Chart
        List<Number> medals = new ArrayList<>();

        List<Date> dates = new ArrayList<>();

        if (playerMedStats != null && !playerMedStats.isEmpty()) {
            List<LocalDateTime> orderedDates = new ArrayList<>(playerMedStats.keySet());
            Collections.sort(orderedDates);

            XYChart chart = new XYChart(1000, 800);
            chart.setTitle("Comparaison des médailles");
            chart.setXAxisTitle("Dates");
            chart.setYAxisTitle("Nombre total de médailles");
            chart.getStyler().setDatePattern("dd/MM");

            HashMap<Double, Object> yMarksMap = new HashMap<>();

            fillSerieMedalsByDateDataAndLabels(playerMedStats, medals, dates, orderedDates, yMarksMap, true);
            XYSeries series = chart.addSeries("Médailles de " + author.getName(), dates, medals);
            generateCommonChartProperties(chart, series);
            series.setMarker(SeriesMarkers.CIRCLE);
            series.setLineColor(Color.BLUE);
            series.setMarkerColor(Color.BLUE);
            chart.setYAxisLabelOverrideMap(yMarksMap);

            HashMap<Member, List<Date>> datesForComparedMembers = new HashMap<>();
            HashMap<Member, List<Number>> medalsForComparedMembers = new HashMap<>();

            for (Member memberToCompare : compareToMembers) {

                HashMap<LocalDateTime, BigDecimal> memberMedStats = getMedStats2ForPlayer(memberToCompare.getUser().getIdLong());
                if (memberMedStats != null && !memberMedStats.isEmpty()) {

                    List<Date> datesCompare = new ArrayList<>();
                    List<Number> medalsCompare = new ArrayList<>();
                    List<LocalDateTime> orderedDatesComparison = new ArrayList<>(memberMedStats.keySet());
                    Collections.sort(orderedDatesComparison);

                    fillSerieMedalsByDateDataAndLabels(memberMedStats, medalsCompare, datesCompare, orderedDatesComparison, null, true);
                    datesForComparedMembers.put(memberToCompare, datesCompare);
                    medalsForComparedMembers.put(memberToCompare, medalsCompare);
                }
            }

            for (Member memberToCompare : medalsForComparedMembers.keySet()) {
                XYSeries comparisonSeries = chart.addSeries("Médailles de " + memberToCompare.getUser().getName(), datesForComparedMembers.get(memberToCompare),
                        medalsForComparedMembers.get(memberToCompare));
                setRandomColor(comparisonSeries, colorMap.get(memberToCompare));
                comparisonSeries.setMarker(SeriesMarkers.NONE);
                makeSeriesDashed(comparisonSeries);
            }

            return BitmapEncoder.getBufferedImage(chart);
        }
        return null;
    }

    private void makeSeriesDashed(XYSeries comparisonSeries) {
        final float dash1[] = {10.0f};
        final BasicStroke dashed = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dash1, 0.0f);
        comparisonSeries.setLineStyle(dashed);
    }

    private BufferedImage drawComparisonSRChart(HashMap<Integer, BigDecimal> playerSRStats, List<Member> compareToMembers, User author, HashMap<Member, Color> colorMap) {
        // Create Chart
        List<Number> srs = new ArrayList<>();
        List<Integer> kls = new ArrayList<>();
        DecimalFormat dc = new DecimalFormat("#");

        if (playerSRStats != null && !playerSRStats.isEmpty()) {
            List<Integer> orderedKLs = new ArrayList<>(playerSRStats.keySet());
            Collections.sort(orderedKLs);

            XYChart chart = new XYChart(1000, 800);
            chart.setTitle("Comparaison du SR");
            chart.setXAxisTitle("KL");
            chart.setYAxisTitle("SR(% du total de médailles)");
            chart.getStyler().setDatePattern("dd/MM");
            chart.getStyler().setYAxisDecimalPattern("#0.00'%'");

            fillSerieSRsDataAndLabelsByDate(srs, kls, dc, null, playerSRStats, orderedKLs);
            XYSeries series = chart.addSeries("SR de " + author.getName(), kls, srs);
            generateCommonChartProperties(chart, series);
            series.setMarker(SeriesMarkers.CIRCLE);
            series.setLineColor(Color.BLUE);
            series.setMarkerColor(Color.BLUE);

            HashMap<Member, List<Integer>> klsForComparedMembers = new HashMap<>();
            HashMap<Member, List<Number>> SRsForComparedMembers = new HashMap<>();

            for (Member memberToCompare : compareToMembers) {

                HashMap<Integer, BigDecimal> memberSRStats = getSRStatsForPlayerForComparison(memberToCompare.getUser().getIdLong());
                if (memberSRStats != null && !memberSRStats.isEmpty()) {
                    List<Number> srsCompare = new ArrayList<>();
                    List<Integer> klsCompare = new ArrayList<>();
                    List<Integer> orderedKLsComparison = new ArrayList<>(memberSRStats.keySet());
                    Collections.sort(orderedKLsComparison);

                    fillSerieSRsDataAndLabelsByDate(srsCompare, klsCompare, dc, null, memberSRStats, orderedKLsComparison);
                    klsForComparedMembers.put(memberToCompare, klsCompare);
                    SRsForComparedMembers.put(memberToCompare, srsCompare);
                }
            }

            for (Member memberToCompare : SRsForComparedMembers.keySet()) {
                XYSeries comparisonSeries = chart.addSeries("SR de " + memberToCompare.getUser().getName(), klsForComparedMembers.get(memberToCompare),
                        SRsForComparedMembers.get(memberToCompare));
                generateCommonChartProperties(chart, comparisonSeries);
                setRandomColor(comparisonSeries, colorMap.get(memberToCompare));
                makeSeriesDashed(comparisonSeries);
                comparisonSeries.setMarker(SeriesMarkers.NONE);
            }

            return BitmapEncoder.getBufferedImage(chart);
        }
        return null;
    }

    private BufferedImage drawComparisonSRChart2(HashMap<Integer, BigDecimal> playerSRStats, List<Member> compareToMembers, User author, HashMap<Member, Color> colorMap) {
        // Create Chart
        List<Number> srs = new ArrayList<>();
        List<Integer> kls = new ArrayList<>();
        DecimalFormat dc = new DecimalFormat("#");

        if (playerSRStats != null && !playerSRStats.isEmpty()) {
            List<Integer> orderedKLs = new ArrayList<>(playerSRStats.keySet());
            Collections.sort(orderedKLs);

            XYChart chart = new XYChart(1000, 800);
            chart.setTitle("Comparaison du SR");
            chart.setXAxisTitle("KL");
            chart.setYAxisTitle("SR(% du total de médailles)");
            chart.getStyler().setDatePattern("dd/MM");
            chart.getStyler().setYAxisDecimalPattern("#0.00'%'");

            fillSerieSRsDataAndLabelsByDate(srs, kls, dc, null, playerSRStats, orderedKLs);
            XYSeries series = chart.addSeries("SR de " + author.getName(), kls, srs);
            generateCommonChartProperties(chart, series);
            series.setMarker(SeriesMarkers.CIRCLE);
            series.setLineColor(Color.BLUE);
            series.setMarkerColor(Color.BLUE);

            HashMap<Member, List<Integer>> klsForComparedMembers = new HashMap<>();
            HashMap<Member, List<Number>> SRsForComparedMembers = new HashMap<>();

            for (Member memberToCompare : compareToMembers) {

                HashMap<Integer, BigDecimal> memberSRStats = getSRStats2ForPlayerForComparison(memberToCompare.getUser().getIdLong());
                if (memberSRStats != null && !memberSRStats.isEmpty()) {
                    List<Number> srsCompare = new ArrayList<>();
                    List<Integer> klsCompare = new ArrayList<>();
                    List<Integer> orderedKLsComparison = new ArrayList<>(memberSRStats.keySet());
                    Collections.sort(orderedKLsComparison);

                    fillSerieSRsDataAndLabelsByDate(srsCompare, klsCompare, dc, null, memberSRStats, orderedKLsComparison);
                    klsForComparedMembers.put(memberToCompare, klsCompare);
                    SRsForComparedMembers.put(memberToCompare, srsCompare);
                }
            }

            for (Member memberToCompare : SRsForComparedMembers.keySet()) {
                XYSeries comparisonSeries = chart.addSeries("SR de " + memberToCompare.getUser().getName(), klsForComparedMembers.get(memberToCompare),
                        SRsForComparedMembers.get(memberToCompare));
                generateCommonChartProperties(chart, comparisonSeries);
                setRandomColor(comparisonSeries, colorMap.get(memberToCompare));
                makeSeriesDashed(comparisonSeries);
                comparisonSeries.setMarker(SeriesMarkers.NONE);
            }

            return BitmapEncoder.getBufferedImage(chart);
        }
        return null;
    }

    private void setRandomColor(XYSeries series, Color color) {
        series.setMarkerColor(color);
        series.setLineColor(color);
    }

    private void fillSerieSRsDataAndLabelsByDate(List<Number> srsCompare, List<Integer> klsCompare, DecimalFormat dc, Map<Double, Object> yMarkMap,
            HashMap<Integer, BigDecimal> memberMedStats, List<Integer> orderedKLsComparison) {
        for (Integer kl : orderedKLsComparison) {
            srsCompare.add(memberMedStats.get(kl));
            if (yMarkMap != null) {
                yMarkMap.put(memberMedStats.get(kl).doubleValue(), dc.format(memberMedStats.get(kl)));
            }
            klsCompare.add(kl);
        }
    }

    public BufferedImage drawComparisonKLChart(HashMap<LocalDateTime, Integer> playerKLStats, List<Member> compareToMembers, User author, HashMap<Member, Color> colorMap) {
        // Create Chart
        List<Number> kls = new ArrayList<>();
        List<Date> dates = new ArrayList<>();

        if (playerKLStats != null && !playerKLStats.isEmpty()) {
            List<LocalDateTime> orderedDates = new ArrayList<>(playerKLStats.keySet());
            Collections.sort(orderedDates);

            XYChart chart = new XYChart(1000, 800);
            chart.setTitle("Comparaison du KL");
            chart.setXAxisTitle("Dates");
            chart.setYAxisTitle("KL");
            chart.getStyler().setDatePattern("dd/MM");

            fillSerieKLsDataAndLabelsByDate(playerKLStats, kls, dates, orderedDates, null);
            XYSeries series = chart.addSeries("KL de " + author.getName(), dates, kls);
            generateCommonChartProperties(chart, series);
            series.setMarker(SeriesMarkers.CIRCLE);
            series.setLineColor(Color.BLUE);
            series.setMarkerColor(Color.BLUE);

            HashMap<Member, List<Date>> datesForComparedMembers = new HashMap<>();
            HashMap<Member, List<Number>> KLsForComparedMembers = new HashMap<>();

            for (Member memberToCompare : compareToMembers) {
                HashMap<LocalDateTime, Integer> memberKLStats = getKLStatsForPlayer(memberToCompare.getUser().getIdLong());
                if (memberKLStats != null && !memberKLStats.isEmpty()) {
                    List<Number> klsCompare = new ArrayList<>();
                    List<Date> datesCompare = new ArrayList<>();
                    List<LocalDateTime> orderedDatesComparison = new ArrayList<>(memberKLStats.keySet());
                    Collections.sort(orderedDatesComparison);

                    fillSerieKLsDataAndLabelsByDate(memberKLStats, klsCompare, datesCompare, orderedDatesComparison, null);
                    datesForComparedMembers.put(memberToCompare, datesCompare);
                    KLsForComparedMembers.put(memberToCompare, klsCompare);
                }
            }

            for (Member memberToCompare : KLsForComparedMembers.keySet()) {
                XYSeries comparisonSeries = chart.addSeries("KL de " + memberToCompare.getUser().getName(), datesForComparedMembers.get(memberToCompare),
                        KLsForComparedMembers.get(memberToCompare));
                generateCommonChartProperties(chart, comparisonSeries);
                setRandomColor(comparisonSeries, colorMap.get(memberToCompare));
                comparisonSeries.setMarker(SeriesMarkers.NONE);
                makeSeriesDashed(comparisonSeries);
            }

            return BitmapEncoder.getBufferedImage(chart);
        }
        return null;
    }

    public BufferedImage drawComparisonKLChart2(HashMap<LocalDateTime, Integer> playerKLStats, List<Member> compareToMembers, User author, HashMap<Member, Color> colorMap) {
        // Create Chart
        List<Number> kls = new ArrayList<>();
        List<Date> dates = new ArrayList<>();

        if (playerKLStats != null && !playerKLStats.isEmpty()) {
            List<LocalDateTime> orderedDates = new ArrayList<>(playerKLStats.keySet());
            Collections.sort(orderedDates);

            XYChart chart = new XYChart(1000, 800);
            chart.setTitle("Comparaison du KL");
            chart.setXAxisTitle("Dates");
            chart.setYAxisTitle("KL");
            chart.getStyler().setDatePattern("dd/MM");

            fillSerieKLsDataAndLabelsByDate(playerKLStats, kls, dates, orderedDates, null);
            XYSeries series = chart.addSeries("KL de " + author.getName(), dates, kls);
            generateCommonChartProperties(chart, series);
            series.setMarker(SeriesMarkers.CIRCLE);
            series.setLineColor(Color.BLUE);
            series.setMarkerColor(Color.BLUE);

            HashMap<Member, List<Date>> datesForComparedMembers = new HashMap<>();
            HashMap<Member, List<Number>> KLsForComparedMembers = new HashMap<>();

            for (Member memberToCompare : compareToMembers) {
                HashMap<LocalDateTime, Integer> memberKLStats = getKLStats2ForPlayer(memberToCompare.getUser().getIdLong());
                if (memberKLStats != null && !memberKLStats.isEmpty()) {
                    List<Number> klsCompare = new ArrayList<>();
                    List<Date> datesCompare = new ArrayList<>();
                    List<LocalDateTime> orderedDatesComparison = new ArrayList<>(memberKLStats.keySet());
                    Collections.sort(orderedDatesComparison);

                    fillSerieKLsDataAndLabelsByDate(memberKLStats, klsCompare, datesCompare, orderedDatesComparison, null);
                    datesForComparedMembers.put(memberToCompare, datesCompare);
                    KLsForComparedMembers.put(memberToCompare, klsCompare);
                }
            }

            for (Member memberToCompare : KLsForComparedMembers.keySet()) {
                XYSeries comparisonSeries = chart.addSeries("KL de " + memberToCompare.getUser().getName(), datesForComparedMembers.get(memberToCompare),
                        KLsForComparedMembers.get(memberToCompare));
                generateCommonChartProperties(chart, comparisonSeries);
                setRandomColor(comparisonSeries, colorMap.get(memberToCompare));
                comparisonSeries.setMarker(SeriesMarkers.NONE);
                makeSeriesDashed(comparisonSeries);
            }

            return BitmapEncoder.getBufferedImage(chart);
        }
        return null;
    }

    private void fillSerieKLsDataAndLabelsByDate(HashMap<LocalDateTime, Integer> playerKLStats, List<Number> kls, List<Date> dates, List<LocalDateTime> orderedDates,
            Map<Double, Object> yMarkMap) {
        for (LocalDateTime date : orderedDates) {
            kls.add(playerKLStats.get(date));
            Date out = Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
            if (yMarkMap != null) {
                yMarkMap.put(Double.valueOf(playerKLStats.get(date)), playerKLStats.get(date));
            }
            dates.add(out);
        }
    }

    private void fillSerieMedalsByDateDataAndLabels(HashMap<LocalDateTime, BigDecimal> medStats, List<Number> medalsData, List<Date> datesData,
            List<LocalDateTime> orderedDatesComparison, Map<Double, Object> yMarkMap, boolean logarithmic) {
        for (LocalDateTime date : orderedDatesComparison) {
            BigDecimal rawMedals = medStats.get(date);
            double medalNumber = logarithmic ? Math.log(rawMedals.doubleValue()) : rawMedals.doubleValue();
            medalsData.add(medalNumber);
            if (yMarkMap != null) {
                yMarkMap.put(medalNumber, helperService.formatBigNumbersToEFFormat(rawMedals));
            }
            Date out = Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
            datesData.add(out);
        }
    }

    @Override
    public BufferedImage drawMedChart(HashMap<LocalDateTime, BigDecimal> playerMedStats) {
        // Create Chart
        List<Number> medals = new ArrayList<>();
        List<Date> dates = new ArrayList<>();

        List<LocalDateTime> orderedDates = new ArrayList<>(playerMedStats.keySet());

        Collections.sort(orderedDates);

        Map<Double, Object> yMarkMap = new TreeMap<Double, Object>();

        fillSerieMedalsByDateDataAndLabels(playerMedStats, medals, dates, orderedDates, yMarkMap, true);

        XYChart chart = new XYChart(1000, 800);
        chart.setTitle("Evolution du total de médailles dans le temps");
        chart.setXAxisTitle("Dates");
        chart.setYAxisTitle("Nombre total de médailles");
        chart.setYAxisLabelOverrideMap(yMarkMap);
        chart.getStyler().setDatePattern("dd/MM");
        chart.getStyler().setDecimalPattern("#0.00'%'");
        XYSeries series = chart.addSeries("Médailles", dates, medals);
        generateCommonChartProperties(chart, series);

        return BitmapEncoder.getBufferedImage(chart);
    }

    private void generateCommonChartProperties(XYChart chart, XYSeries series) {
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setChartTitlePadding(10);
        series.setMarker(SeriesMarkers.CIRCLE);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
        chart.getStyler().setMarkerSize(5);
    }

    @Override
    public HashMap<LocalDateTime, BigDecimal> getSRStatsForPlayer(long idLong) {
        List<PlayerStatDTO> playerStats = globalDao.getStatsForPlayer(idLong, false, null);
        if (!playerStats.isEmpty()) {
            HashMap<LocalDateTime, BigDecimal> results = new HashMap<>();

            for (PlayerStatDTO playerStat : playerStats) {

                float srRatio = (float) 0.9;
                if (playerStat.getSrRatio() != 0) {
                    srRatio = playerStat.getSrRatio();
                }

                BigDecimal fullSR = playerStat.getSr().multiply(new BigDecimal(60L)).multiply(new BigDecimal(4L)).multiply(new BigDecimal(srRatio));
                BigDecimal srPercentage = fullSR.multiply(new BigDecimal(100L)).divide(playerStat.getMedals(), 2, BigDecimal.ROUND_HALF_DOWN);

                results.put(playerStat.getUpdateDate(), srPercentage);
            }

            return results;
        } else {
            return null;
        }
    }

    @Override
    public HashMap<LocalDateTime, BigDecimal> getSRStats2ForPlayer(long idLong) {
        List<PlayerStatDTO> playerStats = globalDao.getStats2ForPlayer(idLong, false, null);
        if (!playerStats.isEmpty()) {
            HashMap<LocalDateTime, BigDecimal> results = new HashMap<>();

            for (PlayerStatDTO playerStat : playerStats) {

                float srRatio = (float) 0.9;
                if (playerStat.getSrRatio() != 0) {
                    srRatio = playerStat.getSrRatio();
                }

                BigDecimal fullSR = playerStat.getSr().multiply(new BigDecimal(60L)).multiply(new BigDecimal(4L)).multiply(new BigDecimal(srRatio));
                BigDecimal srPercentage = fullSR.multiply(new BigDecimal(100L)).divide(playerStat.getMedals(), 2, BigDecimal.ROUND_HALF_DOWN);

                results.put(playerStat.getUpdateDate(), srPercentage);
            }

            return results;
        } else {
            return null;
        }
    }

    private HashMap<Integer, BigDecimal> getSRStatsForPlayerForComparison(long idLong) {
        List<PlayerStatDTO> playerStats = globalDao.getStatsForPlayer(idLong, false, null);
        if (!playerStats.isEmpty()) {
            HashMap<Integer, BigDecimal> results = new HashMap<>();

            for (PlayerStatDTO playerStat : playerStats) {

                float srRatio = (float) 0.9;
                if (playerStat.getSrRatio() != 0) {
                    srRatio = playerStat.getSrRatio();
                }

                BigDecimal fullSR = playerStat.getSr().multiply(new BigDecimal(60L)).multiply(new BigDecimal(4L)).multiply(new BigDecimal(srRatio));
                BigDecimal srPercentage = fullSR.multiply(new BigDecimal(100L)).divide(playerStat.getMedals(), 2, BigDecimal.ROUND_HALF_DOWN);

                results.put(playerStat.getKl(), srPercentage);
            }

            return results;
        } else {
            return null;
        }
    }

    private HashMap<Integer, BigDecimal> getSRStats2ForPlayerForComparison(long idLong) {
        List<PlayerStatDTO> playerStats = globalDao.getStats2ForPlayer(idLong, false, null);
        if (!playerStats.isEmpty()) {
            HashMap<Integer, BigDecimal> results = new HashMap<>();

            for (PlayerStatDTO playerStat : playerStats) {

                float srRatio = (float) 0.9;
                if (playerStat.getSrRatio() != 0) {
                    srRatio = playerStat.getSrRatio();
                }

                BigDecimal fullSR = playerStat.getSr().multiply(new BigDecimal(60L)).multiply(new BigDecimal(4L)).multiply(new BigDecimal(srRatio));
                BigDecimal srPercentage = fullSR.multiply(new BigDecimal(100L)).divide(playerStat.getMedals(), 2, BigDecimal.ROUND_HALF_DOWN);

                results.put(playerStat.getKl(), srPercentage);
            }

            return results;
        } else {
            return null;
        }
    }

    @Override
    public HashMap<LocalDateTime, BigDecimal> getMedStatsForPlayer(long idLong) {
        List<PlayerStatDTO> playerStats = globalDao.getStatsForPlayer(idLong, false, null);
        if (!playerStats.isEmpty()) {
            HashMap<LocalDateTime, BigDecimal> results = new HashMap<>();

            for (PlayerStatDTO playerStat : playerStats) {
                results.put(playerStat.getUpdateDate(), playerStat.getMedals());
            }

            return results;
        } else {
            return null;
        }
    }

    @Override
    public HashMap<LocalDateTime, BigDecimal> getMedStats2ForPlayer(long idLong) {
        List<PlayerStatDTO> playerStats = globalDao.getStats2ForPlayer(idLong, false, null);
        if (!playerStats.isEmpty()) {
            HashMap<LocalDateTime, BigDecimal> results = new HashMap<>();

            for (PlayerStatDTO playerStat : playerStats) {
                results.put(playerStat.getUpdateDate(), playerStat.getMedals());
            }

            return results;
        } else {
            return null;
        }
    }

    @Override
    public BufferedImage drawKLChart(HashMap<LocalDateTime, Integer> playerKLStats) {
        // Create Chart
        List<Number> kls = new ArrayList<>();
        List<Date> dates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");
        List<LocalDateTime> orderedDates = new ArrayList<>(playerKLStats.keySet());

        Collections.sort(orderedDates);
        Map<Double, Object> yMarkMap = new TreeMap<>();

        fillSerieKLsDataAndLabelsByDate(playerKLStats, kls, dates, orderedDates, yMarkMap);

        XYChart chart = new XYChart(1000, 800);
        chart.setTitle("Evolution du KL dans le temps");
        chart.setXAxisTitle("Dates");
        chart.setXAxisTitle("KL");
        XYSeries series = chart.addSeries("KL", dates, kls);
        chart.setYAxisLabelOverrideMap(yMarkMap);
        chart.getStyler().setDecimalPattern("#");
        chart.getStyler().setDatePattern("dd/MM");
        generateCommonChartProperties(chart, series);

        return BitmapEncoder.getBufferedImage(chart);
    }

    @Override
    public HashMap<LocalDateTime, Integer> getKLStatsForPlayer(long idLong) {
        List<PlayerStatDTO> playerStats = globalDao.getStatsForPlayer(idLong, false, null);

        if (!playerStats.isEmpty()) {
            HashMap<LocalDateTime, Integer> results = new HashMap<>();

            for (PlayerStatDTO playerStat : playerStats) {
                results.put(playerStat.getUpdateDate(), playerStat.getKl());
            }
            return results;
        } else {
            return null;
        }
    }

    @Override
    public HashMap<LocalDateTime, Integer> getKLStats2ForPlayer(long idLong) {
        List<PlayerStatDTO> playerStats = globalDao.getStats2ForPlayer(idLong, false, null);

        if (!playerStats.isEmpty()) {
            HashMap<LocalDateTime, Integer> results = new HashMap<>();

            for (PlayerStatDTO playerStat : playerStats) {
                results.put(playerStat.getUpdateDate(), playerStat.getKl());
            }
            return results;
        } else {
            return null;
        }
    }

    @Override
    public List<PlayerStatDTO> getSRStatsForAllPlayersByKL() {
        List<PlayerStatDTO> playerStats = globalDao.getAllStats();

        for (PlayerStatDTO playerStat : playerStats) {

            float srRatio = (float) 0.9;
            if (playerStat.getSrRatio() != 0) {
                srRatio = playerStat.getSrRatio();
            }

            BigDecimal fullSR = playerStat.getSr().multiply(new BigDecimal(60L)).multiply(new BigDecimal(4L)).multiply(new BigDecimal(srRatio));
            BigDecimal srPercentage = fullSR.multiply(new BigDecimal(100L)).divide(playerStat.getMedals(), 2, BigDecimal.ROUND_HALF_DOWN);

            playerStat.setSrPercentage(srPercentage);
        }

        return playerStats;
    }

    @Override
    public List<PlayerStatDTO> getSRStats2ForAllPlayersByKL() {
        List<PlayerStatDTO> playerStats = globalDao.getAllStats2();

        for (PlayerStatDTO playerStat : playerStats) {

            float srRatio = (float) 0.9;
            if (playerStat.getSrRatio() != 0) {
                srRatio = playerStat.getSrRatio();
            }

            BigDecimal fullSR = playerStat.getSr().multiply(new BigDecimal(60L)).multiply(new BigDecimal(4L)).multiply(new BigDecimal(srRatio));
            BigDecimal srPercentage = fullSR.multiply(new BigDecimal(100L)).divide(playerStat.getMedals(), 2, BigDecimal.ROUND_HALF_DOWN);

            playerStat.setSrPercentage(srPercentage);
        }

        return playerStats;
    }

    @Override
    public void drawMultipleComparisons(MessageChannel channel, User author, List<Member> compareToMembers) {
        HashMap<LocalDateTime, BigDecimal> authorMedStats = getMedStatsForPlayer(author.getIdLong());
        HashMap<Integer, BigDecimal> authorSRStats = getSRStatsForPlayerForComparison(author.getIdLong());
        HashMap<LocalDateTime, Integer> authorKLStats = getKLStatsForPlayer(author.getIdLong());

        HashMap<Member, Color> colorMap = generateRandomColorForMembers(compareToMembers);

        BufferedImage medChartImage = drawComparisonMedChart(authorMedStats, compareToMembers, author, colorMap);
        BufferedImage SRChartImage = drawComparisonSRChart(authorSRStats, compareToMembers, author, colorMap);
        BufferedImage KLChartImage = drawComparisonKLChart(authorKLStats, compareToMembers, author, colorMap);

        if (medChartImage != null && SRChartImage != null && KLChartImage != null) {

            // BufferedImage combined = combineChartsImages(medChartImage, SRChartImage, KLChartImage);
            // messagesService.sendBufferedImage(channel, combined, author.getAsMention(), "Comparison.png");

            messagesService.sendBufferedImage(channel, medChartImage, author.getAsMention(), "Med.png");
            messagesService.sendBufferedImage(channel, KLChartImage, "", "KL.png");
            messagesService.sendBufferedImage(channel, SRChartImage, "", "SR.png");
        } else {
            messagesService.sendBotMessage(channel, "Vous n'avez aucune statistique enregistrée ! Pour savoir comment enregistrer vos données, tapez ?stat");
        }
    }

    @Override
    public void drawMultipleComparisons2(MessageChannel channel, User author, List<Member> compareToMembers) {
        HashMap<LocalDateTime, BigDecimal> authorMedStats = getMedStats2ForPlayer(author.getIdLong());
        HashMap<Integer, BigDecimal> authorSRStats = getSRStats2ForPlayerForComparison(author.getIdLong());
        HashMap<LocalDateTime, Integer> authorKLStats = getKLStats2ForPlayer(author.getIdLong());

        HashMap<Member, Color> colorMap = generateRandomColorForMembers(compareToMembers);

        BufferedImage medChartImage = drawComparisonMedChart2(authorMedStats, compareToMembers, author, colorMap);
        BufferedImage SRChartImage = drawComparisonSRChart2(authorSRStats, compareToMembers, author, colorMap);
        BufferedImage KLChartImage = drawComparisonKLChart2(authorKLStats, compareToMembers, author, colorMap);

        if (medChartImage != null && SRChartImage != null && KLChartImage != null) {

            // BufferedImage combined = combineChartsImages(medChartImage, SRChartImage, KLChartImage);
            // messagesService.sendBufferedImage(channel, combined, author.getAsMention(), "Comparison.png");

            messagesService.sendBufferedImage(channel, medChartImage, author.getAsMention(), "Med.png");
            messagesService.sendBufferedImage(channel, KLChartImage, "", "KL.png");
            messagesService.sendBufferedImage(channel, SRChartImage, "", "SR.png");
        } else {
            messagesService.sendBotMessage(channel, "Vous n'avez aucune statistique enregistrée ! Pour savoir comment enregistrer vos données, tapez ?stat");
        }
    }

    private HashMap<Member, Color> generateRandomColorForMembers(List<Member> compareToMembers) {
        HashMap<Member, Color> colorMap = new HashMap<>();

        for (Member compareToMember : compareToMembers) {
            Random rand = new Random();

            float r = rand.nextFloat();
            float g = rand.nextFloat();
            float b = rand.nextFloat();

            Color color = new Color(r, g, b);
            color.darker();
            color.darker();
            color.darker();

            colorMap.put(compareToMember, color);
        }
        return colorMap;
    }

    private BufferedImage combineChartsImages(BufferedImage medChartImage, BufferedImage SRChartImage, BufferedImage KLChartImage) {
        int w = medChartImage.getWidth() * 2 + 10;
        int h = Math.max(medChartImage.getHeight(), SRChartImage.getHeight()) + KLChartImage.getHeight() + 10;
        BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics g = combined.getGraphics();
        g.drawImage(medChartImage, 0, 0, Color.WHITE, null);
        g.drawImage(SRChartImage, medChartImage.getWidth() + 2, 0, Color.WHITE, null);
        g.drawImage(KLChartImage, ((w / 2) - (KLChartImage.getWidth() / 2)) + 2, Math.max(medChartImage.getHeight(), SRChartImage.getHeight()) + 2, Color.WHITE, null);
        return combined;
    }
}
