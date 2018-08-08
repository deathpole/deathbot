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

    BufferedImage drawComparisonMedChart(HashMap<LocalDateTime, BigDecimal> playerMedStats, List<Member> compareToMembers, User author);

    BufferedImage drawComparisonSRChart(HashMap<LocalDateTime, BigDecimal> playerSRStats, List<Member> compareToMembers, User author);

    BufferedImage drawComparisonKLChart(HashMap<LocalDateTime, Integer> playerKLStats, List<Member> compareToMembers, User author);

    BufferedImage drawMedChart(HashMap<LocalDateTime, BigDecimal> playerMedStats);

    HashMap<LocalDateTime, BigDecimal> getSRStatsForPlayer(long idLong);

    HashMap<LocalDateTime, BigDecimal> getMedStatsForPlayer(long idLong);

    BufferedImage drawKLChart(HashMap<LocalDateTime, Integer> playerKLStats);

    HashMap<LocalDateTime, Integer> getKLStatsForPlayer(long idLong);

    List<PlayerStatDTO> getSRStatsForAllPlayersByKL();

    void drawMultipleComparisons(MessageChannel channel, User author, List<Member> compareToMembers);
}
