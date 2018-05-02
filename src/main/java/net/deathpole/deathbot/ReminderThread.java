package net.deathpole.deathbot;

import static java.time.temporal.ChronoUnit.MINUTES;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;

import net.deathpole.deathbot.Dao.IGlobalDao;
import net.deathpole.deathbot.Dao.Impl.GlobalDao;
import net.deathpole.deathbot.Services.IHelperService;
import net.deathpole.deathbot.Services.IMessagesService;
import net.deathpole.deathbot.Services.Impl.HelperServiceImpl;
import net.deathpole.deathbot.Services.Impl.MessagesServiceImpl;
import net.dv8tion.jda.core.entities.Guild;

public class ReminderThread extends Thread {

    private static final String PREFIX_TAG = "@";
    public HashMap<String, ReminderDTO> reminders = new HashMap<>();
    private IMessagesService messagesService = new MessagesServiceImpl();
    private IHelperService helperService = new HelperServiceImpl();
    private IGlobalDao globalDao = new GlobalDao();
    private Guild guild;

    public ReminderThread(Guild guild) {
        super("ReminderThread_" + guild.getName());
        this.guild = guild;
    }

    public void run() {
        try {
            while (true) {
                for (ReminderDTO reminder : this.reminders.values()) {
                    LocalDateTime nextExecutionTime = reminder.getNextExecutionTime();
                    boolean doItNow = isDoItNow(nextExecutionTime);
                    if (doItNow) {
                        String text = reminder.getText();
                        ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Europe/Paris"));
                        System.out.println("DeathbotExecution : Reminder crontab matched, sending bot message at " + now.toString());

                        LocalDateTime newNextExecutionTime = helperService.generateNextExecutionTime(reminder.getCronTab(), nextExecutionTime);

                        reminder.setNextExecutionTime(newNextExecutionTime);

                        globalDao.updateExecutedTime(guild, reminder);

                        messagesService.sendBotMessageWithMentions(this.guild.getTextChannelsByName(reminder.getChan(), true).get(0), text, this.guild);
                    }
                }

                Thread.currentThread().sleep(60 * 1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean isDoItNow(LocalDateTime nextExecutionTime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextExecTime = ZonedDateTime.of(nextExecutionTime, ZoneId.of("Europe/Paris")).toLocalDateTime();

        return MINUTES.between(now, nextExecTime) < 1;
    }

    public HashMap<String, ReminderDTO> getReminders() {
        return reminders;
    }

    public void setReminders(HashMap<String, ReminderDTO> reminders) {
        this.reminders = reminders;
    }
}