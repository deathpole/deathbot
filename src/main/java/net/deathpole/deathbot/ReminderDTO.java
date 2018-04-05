package net.deathpole.deathbot;

/**
 * Created by nicolas on 04/10/17.
 */
public class ReminderDTO {

    private String title;

    private String text;

    private String chan;

    private String cronTab;

    public ReminderDTO() {
    }

    public ReminderDTO(String title, String text, String chan, String cronTab) {
        this.title = title;
        this.text = text;
        this.chan = chan;
        this.cronTab = cronTab;
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
}
