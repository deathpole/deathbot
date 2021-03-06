package net.deathpole.deathbot.Services.Impl;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.imageio.ImageIO;

import net.deathpole.deathbot.Services.IMessagesService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

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
        // embedBuilder.setFooter("Deathbot créé par @deathpole#9686",
        // "https://pbs.twimg.com/profile_images/632567125308272640/i1dfsYr2_400x400.jpg");

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

    @Override
    public void sendImageByURL(MessageChannel channel, String urlStr, String title, String fileName) {

        channel.sendTyping().queue();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.RED);
        embedBuilder.setDescription(title);

        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.setEmbed(embedBuilder.build());

        Message messageStr = messageBuilder.build();
        try {
            URL url = new URL(urlStr);
            BufferedImage img = ImageIO.read(url);
            File file = new File(fileName);
            ImageIO.write(img, "png", file);
            channel.sendFile(file, messageStr.getContentDisplay()).queue();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendBufferedImage(MessageChannel channel, BufferedImage image, String title, String fileName) {

        channel.sendTyping().queue();
        MessageBuilder messageBuilder = new MessageBuilder();

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.RED);
        embedBuilder.setDescription(title);

        messageBuilder.setEmbed(embedBuilder.build());

        Message messageStr = messageBuilder.build();
        try {
            File outPuteFile = new File(fileName);
            ImageIO.write(image, "png", outPuteFile);
            channel.sendFile(outPuteFile, messageStr.getContentDisplay()).queue();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            } else {
                message = message.replaceFirst("\\{\\#[a-zA-Z0-9_-]*\\}", channelName);
            }
        }
        return message;
    }
}

