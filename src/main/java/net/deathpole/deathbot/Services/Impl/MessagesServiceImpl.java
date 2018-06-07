package net.deathpole.deathbot.Services.Impl;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import net.deathpole.deathbot.Services.IMessagesService;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.IMentionable;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;

/**
 * Created by nicolas on 28/09/17.
 */
public class MessagesServiceImpl implements IMessagesService {

    private static final String PREFIX_TAG = "@";

    @Override
    public void sendBotMessageWithMention(MessageChannel channel, String message, IMentionable mentionable) {
        message = replaceDateAndTime(message);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.RED);

        embedBuilder.setDescription(message);

        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.append(mentionable);
        messageBuilder.setEmbed(embedBuilder.build());
        channel.sendMessage(messageBuilder.build()).queue();
    }

    @Override public void sendBotMessage(MessageChannel channel, String message) {
        message = replaceDateAndTime(message);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.RED);

        embedBuilder.setDescription(message);

        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.setEmbed(embedBuilder.build());
        channel.sendMessage(messageBuilder.build()).queue();
    }

    @Override
    public void sendBotMessageWithMentions(MessageChannel channel, String message, Guild guild) {
        message = replaceDateAndTime(message);

        MessageBuilder messageBuilder = new MessageBuilder();

        while (message.contains(PREFIX_TAG)) {
            int mentionStart = message.indexOf(PREFIX_TAG);
            int mentionEnd = message.indexOf(" ", mentionStart);

            messageBuilder.append(message.substring(0, mentionStart));

            String name = message.substring(mentionStart + 1, mentionEnd);
            List<Member> membersFound = guild.getMembersByEffectiveName(name, false);
            if (membersFound != null && !membersFound.isEmpty()) {
                Member memberFound = membersFound.get(0);
                messageBuilder.append(memberFound);
                message = message.substring(mentionEnd, message.length());
            } else {
                List<Role> rolesFound = guild.getRolesByName(name, false);
                if (rolesFound != null && !rolesFound.isEmpty()) {
                    Role roleFound = rolesFound.get(0);
                    messageBuilder.append(roleFound);
                    message = message.substring(mentionEnd, message.length());
                }
            }
        }
        messageBuilder.append(message);
        channel.sendMessage(messageBuilder.build()).queue();
    }

    private String replaceDateAndTime(String message) {
        LocalDateTime now = LocalDateTime.now();

        if (message.contains("{time}")) {
            message = message.replace("{time}", DateTimeFormatter.ofPattern("HH:mm").format(now));
        }

        if (message.contains("{date}")) {
            message = message.replace("{date}", DateTimeFormatter.ofPattern("dd MMMM yyyy").format(now));
        }
        return message;
    }

    @Override
    public void sendNormalBotMessage(MessageChannel channel, String message) {
        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.append(replaceDateAndTime(message));
        channel.sendMessage(messageBuilder.build()).queue();
    }

    @Override public void sendMessageNotEnoughRights(MessageChannel channel) {
        sendBotMessage(channel, "Désolé, vous n'avez pas les droits pour exécuter cette commande. ");
    }

    @Override
    public String replaceChannel(String message, Guild guild) {
        while (message.contains("{#")) {
            String channelName = message.substring(message.indexOf("{#") + 2, message.indexOf("}", message.indexOf("{#")));
            List<TextChannel> channels = guild.getTextChannelsByName(channelName, true);
            if (channels != null && !channels.isEmpty()) {
                TextChannel channel = channels.get(0);
                message = message.replaceFirst("\\{\\#[a-zA-Z0-9_-]*\\}", channel.getAsMention());
            }
        }
        return message;
    }
}

