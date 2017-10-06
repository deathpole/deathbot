package net.deathpole.deathbot;

public class Main {

    public static void main(String[] args) {
        try {
           Bot.main(args);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
}