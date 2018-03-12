package net.deathpole.deathbot.Dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Set;

import net.deathpole.deathbot.CustomReaction;
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

    HashMap<String, HashMap<String, CustomReaction>> initMapCustomReactions();

    void saveCustomReaction(String keyWord, CustomReaction customReaction, Guild guild);

    boolean deleteCustomReaction(String keyWord, Guild guild);

    HashMap<String, HashMap<String, String>> initMapVoiceRoles();

    void saveVoiceRole(String channelName, String roleName, Guild guild);

    BigDecimal getMedalGainForStage(Integer stage);
}
