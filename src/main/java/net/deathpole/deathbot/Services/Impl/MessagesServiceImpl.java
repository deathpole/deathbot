package net.deathpole.deathbot.Services.Impl;

import java.awt.*;

import net.deathpole.deathbot.Services.IMessagesService;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.IMentionable;
import net.dv8tion.jda.core.entities.MessageChannel;

/**
 * Created by nicolas on 28/09/17.
 */
public class MessagesServiceImpl implements IMessagesService {

    private static final String SEPARATOR_ACTION_ARGS = " ";
    private static final String ROLES_SEPARATOR = ",";
    private static final String RETOUR_LIGNE = "\r\n";

    @Override
    public void sendBotMessageWithMention(MessageChannel channel, String message, IMentionable mentionable) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.RED);

        StringBuilder sb = new StringBuilder();

        sb.append(message);
        embedBuilder.setDescription(sb.toString());

        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.append(mentionable);
        messageBuilder.setEmbed(embedBuilder.build());
        channel.sendMessage(messageBuilder.build()).queue();
    }

    @Override public void sendBotMessage(MessageChannel channel, String message) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.RED);

        StringBuilder sb = new StringBuilder();

        sb.append(message);
        embedBuilder.setDescription(sb.toString());

        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.setEmbed(embedBuilder.build());
        channel.sendMessage(messageBuilder.build()).queue();
    }

    @Override public void sendMessageNotEnoughRights(MessageChannel channel) {
        sendBotMessage(channel, "Désolé, vous n'avez pas les droits pour exécuter cette commande. ");
    }
}

