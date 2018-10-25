package net.deathpole.deathbot.Services;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import net.deathpole.deathbot.PlayerStatDTO;
import net.deathpole.deathbot.Services.Impl.CommandesServiceImpl;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

/**
 * Created by nicolas on 28/09/17.
 */
public interface IChartService {

    BufferedImage drawAllPlayersSRChart(List<PlayerStatDTO> playersStats, CommandesServiceImpl commandesService);

    BufferedImage drawSRChart(HashMap<LocalDateTime, BigDecimal> playerSRStats);

    BufferedImage drawMedChart(HashMap<LocalDateTime, BigDecimal> playerMedStats);

    HashMap<LocalDateTime, BigDecimal> getSRStatsForPlayer(long idLong);

    HashMap<LocalDateTime, BigDecimal> getSRStats2ForPlayer(long idLong);

    HashMap<LocalDateTime, BigDecimal> getMedStatsForPlayer(long idLong);

    HashMap<LocalDateTime, BigDecimal> getMedStats2ForPlayer(long idLong);

    BufferedImage drawKLChart(HashMap<LocalDateTime, Integer> playerKLStats);

    HashMap<LocalDateTime, Integer> getKLStatsForPlayer(long idLong);

    HashMap<LocalDateTime, Integer> getKLStats2ForPlayer(long idLong);

    List<PlayerStatDTO> getSRStatsForAllPlayersByKL();

    List<PlayerStatDTO> getSRStats2ForAllPlayersByKL();

    void drawMultipleComparisons(MessageChannel channel, User author, List<Member> compareToMembers);

    void drawMultipleComparisons2(MessageChannel channel, User author, List<Member> compareToMembers);
}
