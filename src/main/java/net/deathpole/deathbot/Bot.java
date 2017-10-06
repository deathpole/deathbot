package net.deathpole.deathbot;

import javax.security.auth.login.LoginException;

import net.deathpole.deathbot.Services.ICommandesService;
import net.deathpole.deathbot.Services.Impl.CommandesServiceImpl;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class Bot extends ListenerAdapter {

    private ICommandesService commandesService = new CommandesServiceImpl();
    private static JDA jda;

    public static void main(String[] args) {
        try {
            jda = new JDABuilder(AccountType.BOT).setToken(args[0]).addEventListener(new Bot()).buildBlocking();
            jda.setAutoReconnect(true);
        } catch (LoginException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (RateLimitedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        commandesService.executeAction(e);
    }
}