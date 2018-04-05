package net.deathpole.deathbot.Services;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.IMentionable;
import net.dv8tion.jda.core.entities.MessageChannel;

/**
 * Created by nicolas on 28/09/17.
 */
public interface IMessagesService {

    void sendBotMessageWithMention(MessageChannel channel, String message, IMentionable mentionable);

    void sendBotMessage(MessageChannel channel, String message);

    void sendBotMessageWithMentions(MessageChannel channel, String message, Guild guild);

    void sendNormalBotMessage(MessageChannel channel, String message);

    void sendMessageNotEnoughRights(MessageChannel channel);
}
