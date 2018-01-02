package net.deathpole.deathbot;

import javax.security.auth.login.LoginException;

import net.deathpole.deathbot.Services.ICommandesService;
import net.deathpole.deathbot.Services.Impl.CommandesServiceImpl;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class Bot extends ListenerAdapter {

    private ICommandesService commandesService = new CommandesServiceImpl();

    public static void main(String[] args) {
        try {
            String token = System.getenv().get("TOKEN");
            JDA jda = new JDABuilder(AccountType.BOT).setToken(token).setGame(Game.of("Made for EF FR")).addEventListener(new Bot()).buildBlocking();

            jda.setAutoReconnect(true);
        } catch (LoginException | IllegalArgumentException | InterruptedException | RateLimitedException e) {
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
}