package net.deathpole.deathbot.Services;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Created by nicolas on 28/09/17.
 */
public interface ICommandesService {

    void executeAction(MessageReceivedEvent e);

    void userJoinedGuild(GuildMemberJoinEvent e);

    void userJoinedVoiceChannel(VoiceChannel e, Member member, Guild guild);

    void userLeftVoiceChannel(VoiceChannel channel, Member connectedMember, Guild guild);
}
