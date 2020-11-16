package net.deathpole.deathbot;

import java.util.HashMap;

import javax.security.auth.login.LoginException;

import net.deathpole.deathbot.Services.ICommandesService;
import net.deathpole.deathbot.Services.Impl.CommandesServiceImpl;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Bot extends ListenerAdapter {

    public static HashMap<String, ReminderThread> reminderThreadsByGuild = new HashMap<>();
    private ICommandesService commandesService = new CommandesServiceImpl();

    public static void main(String[] args) {
        try {
            String token = System.getenv().get("TOKEN");
            JDA jda = JDABuilder.create(token, GatewayIntent.GUILD_MESSAGES).addEventListeners(new Bot()).build();
            jda.setAutoReconnect(true);
        } catch (LoginException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        commandesService.executeAction(e);
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent e) {
        commandesService.userJoinedGuild(e);
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent e) {
        commandesService.userJoinedVoiceChannel(e.getChannelJoined(), e.getMember(), e.getGuild());
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent e) {
        commandesService.userLeftVoiceChannel(e.getChannelLeft(), e.getMember(), e.getGuild());
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent e) {
        commandesService.userLeftVoiceChannel(e.getChannelLeft(), e.getMember(), e.getGuild());
        commandesService.userJoinedVoiceChannel(e.getChannelJoined(), e.getMember(), e.getGuild());
    }
}