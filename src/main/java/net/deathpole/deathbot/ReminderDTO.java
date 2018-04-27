package net.deathpole.deathbot;

import java.time.LocalDateTime;

/**
 * Created by nicolas on 04/10/17.
 */
public class ReminderDTO {

    private String title;

    private String text;

    private String chan;

    private String cronTab;

    private LocalDateTime nextExecutionTime;

    public ReminderDTO() {
    }

    public ReminderDTO(String title, String text, String chan, String cronTab, LocalDateTime nextExecutionTime) {
        this.title = title;
        this.text = text;
        this.chan = chan;
        this.cronTab = cronTab;
        this.nextExecutionTime = nextExecutionTime;

    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getChan() {
        return chan;
    }

    public void setChan(String chan) {
        this.chan = chan;
    }

    public String getCronTab() {
        return cronTab;
    }

    public void setCronTab(String cronTab) {
        this.cronTab = cronTab;
    }

    public LocalDateTime getNextExecutionTime() {
        return nextExecutionTime;
    }

    public void setNextExecutionTime(LocalDateTime nextExecutionTime) {
        this.nextExecutionTime = nextExecutionTime;
    }
}
