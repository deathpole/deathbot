package net.deathpole.deathbot.Services;

import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/**
 * Created by nicolas on 28/09/17.
 */
public interface ICommandesService {

    void executeAction(MessageReceivedEvent e);

    void userJoinedGuild(GuildMemberJoinEvent e);
}
