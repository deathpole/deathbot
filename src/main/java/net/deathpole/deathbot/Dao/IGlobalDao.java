package net.deathpole.deathbot.Dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.deathpole.deathbot.CustomReactionDTO;
import net.deathpole.deathbot.PlayerStatDTO;
import net.deathpole.deathbot.ReminderDTO;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

/**
 * Created by nicolas on 24/10/17.
 */
public interface IGlobalDao {

    Connection getConnectionToDB();

    HashMap<String, Set<String>> initAssignableRanksbyGuild(boolean single);

    void saveAssignableRanksForGuild(Guild guild, Set<Role> selfAssignableRanks, boolean single);

    void saveWelcomeMessage(Guild guild, String message);

    void saveActivationState(Guild guild, boolean activated);

    boolean getActivationState(Guild guild);

    String getWelcomeMessage(Guild guild);

    HashMap<String, HashMap<String, CustomReactionDTO>> initMapCustomReactions();

    HashMap<String, List<String>> initMapDynoActions();

    void saveCustomReaction(String keyWord, CustomReactionDTO customReaction, Guild guild);

    void addDynoAction(String action, Guild guild);

    boolean deleteDynoAction(String action, Guild guild);

    List<PlayerStatDTO> getStatsForPlayer(long playerId, boolean lastStatOnly, Integer limit);

    HashMap<String, ReminderDTO> getRemindersForGuild(Guild guild);

    boolean deleteRemindersForGuild(Guild guild, String title);

    void saveReminder(ReminderDTO reminder, Guild guild);

    boolean deleteCustomReaction(String keyWord, Guild guild);

    HashMap<String, HashMap<String, String>> initMapVoiceRoles();

    void saveVoiceRole(String channelName, String roleName, Guild guild);

    BigDecimal getMedalGainForStage(Integer stage);

    HashMap<String, Set<String>> initOnJoinRanksbyGuild();

    void saveOnJoinRanksForGuild(Guild guild, Set<Role> onJoinRanks);

    void updateExecutedTime(Guild guild, ReminderDTO reminder);

    void createRankLink(Guild guild, Role role, Set<Role> linkedRoles);

    HashMap<String, HashMap<String, Set<String>>> initLinkedRanksbyGuild();

    void deleteRankLink(Guild guild, Role role);

    List<PlayerStatDTO> getStats2ForPlayer(long playerId, boolean lastStatOnly, Integer limit);

    PlayerStatDTO getStatById(Integer id);

    PlayerStatDTO getStat2ById(Integer id);

    void savePlayerStats(PlayerStatDTO playerStatDTO);

    void savePlayerStats2(PlayerStatDTO playerStatDTO);

    void cancelLastPlayerStats(long playerId);

    void cancelLastPlayerStats2(long playerId);

    int cancelStatById(Integer statId);

    int cancelStat2ById(Integer statId);

    List<PlayerStatDTO> getAllStats();

    List<PlayerStatDTO> getAllStats2();

    List<PlayerStatDTO> getAllStats2ForSameKL(int kl);
}
