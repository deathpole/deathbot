package net.deathpole.deathbot.Dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Set;

import net.deathpole.deathbot.CustomReactionDTO;
import net.deathpole.deathbot.ReminderDTO;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

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

    void saveCustomReaction(String keyWord, CustomReactionDTO customReaction, Guild guild);

    HashMap<String, ReminderDTO> getRemindersForGuild(Guild guild);

    boolean deleteRemindersForGuild(Guild guild, String title);

    void saveReminder(ReminderDTO reminder, Guild guild);

    boolean deleteCustomReaction(String keyWord, Guild guild);

    HashMap<String, HashMap<String, String>> initMapVoiceRoles();

    void saveVoiceRole(String channelName, String roleName, Guild guild);

    BigDecimal getMedalGainForStage(Integer stage);

    HashMap<String, Set<String>> initOnJoinRanksbyGuild();

    void saveOnJoinRanksForGuild(Guild guild, Set<Role> onJoinRanks);
}
