package net.deathpole.deathbot;

import java.time.ZonedDateTime;
import java.util.HashMap;

import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import net.deathpole.deathbot.Services.IMessagesService;
import net.deathpole.deathbot.Services.Impl.MessagesServiceImpl;
import net.dv8tion.jda.core.entities.Guild;

public class ReminderThread extends Thread {

    private static final String PREFIX_TAG = "@";
    public HashMap<String, ReminderDTO> reminders = new HashMap<>();
    private IMessagesService messagesService = new MessagesServiceImpl();
    private Guild guild;

    public ReminderThread(Guild guild) {
        super("ReminderThread_" + guild.getName());
        this.guild = guild;
    }

    public void run() {
        try {
            while (true) {

                Thread.currentThread().sleep(60 * 1000);

                // List<ReminderDTO> reminders = new ArrayList<>();
                //
                // ReminderDTO fakeReminder = new ReminderDTO();
                // fakeReminder.setChan("raid");
                // fakeReminder.setCronTab("15 16 * * *");
                // fakeReminder.setText("Ceci est un test !");
                //
                // reminders.add(fakeReminder);

                for (ReminderDTO reminder : this.reminders.values()) {
                    boolean doItNow = isDoItNow(reminder.getCronTab());
                    if (doItNow) {
                        String text = reminder.getText();
                        messagesService.sendBotMessageWithMentions(this.guild.getTextChannelsByName(reminder.getChan(), true).get(0), text, this.guild);
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean isDoItNow(String cronTab) {
        ZonedDateTime now = ZonedDateTime.now();
        CronDefinition cronDefinition = CronDefinitionBuilder.defineCron().withMinutes().and().withHours().and().withDayOfMonth().and().withMonth().and().withYear().and().instance();
        CronParser parser = new CronParser(cronDefinition);
        ExecutionTime executionTime = ExecutionTime.forCron(parser.parse(cronTab));

        return executionTime.isMatch(now);
    }

    public HashMap<String, ReminderDTO> getReminders() {
        return reminders;
    }

    public void setReminders(HashMap<String, ReminderDTO> reminders) {
        this.reminders = reminders;
    }
}