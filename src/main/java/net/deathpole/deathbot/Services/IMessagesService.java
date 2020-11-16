package net.deathpole.deathbot.Services;

import java.awt.image.BufferedImage;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.MessageChannel;

/**
 * Created by nicolas on 28/09/17.
 */
public interface IMessagesService {

    void sendBotMessageWithMention(MessageChannel channel, String message, IMentionable mentionable);

    void sendBotMessage(MessageChannel channel, String message);

    void sendBotMessageWithMentions(MessageChannel channel, String message, Guild guild);

    void sendNormalBotMessage(MessageChannel channel, String message);

    void sendImageByURL(MessageChannel channel, String url, String title, String fileName);

    void sendBufferedImage(MessageChannel channel, BufferedImage image, String title, String fileName);

    void sendMessageNotEnoughRights(MessageChannel channel);

    String replaceChannel(String reactionReplaced, Guild guild);
}
