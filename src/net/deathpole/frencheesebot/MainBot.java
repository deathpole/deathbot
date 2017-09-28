package net.deathpole.frencheesebot;

import static net.dv8tion.jda.core.MessageBuilder.Formatting.BLOCK;
import static net.dv8tion.jda.core.MessageBuilder.Formatting.BOLD;
import static net.dv8tion.jda.core.MessageBuilder.Formatting.ITALICS;
import static net.dv8tion.jda.core.MessageBuilder.Formatting.UNDERLINE;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.security.auth.login.LoginException;

import com.sun.deploy.util.StringUtils;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.IMentionable;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.GuildController;

public class MainBot extends ListenerAdapter {

    private static final String SEPARATOR_ACTION_ARGS = " ";
    private static final String ROLES_SEPARATOR = ",";
    private static final String RETOUR_LIGNE = "\r\n";
    private static JDA jda;
    Set<Role> selfAssignableRanks;
    List<String> dynoActions = initDynoActions();
    private String prefixCmd = "[?]";
    private boolean singleRole = true;

    public static void main(String[] args) {
        try {
            jda = new JDABuilder(AccountType.BOT).setToken(args[0]).addEventListener(new MainBot()).buildBlocking();
            initDynoActions();
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

    private static List<String> initDynoActions() {
        List<String> tempDynoActions = new ArrayList<>();

        for (EnumDynoAction action : EnumDynoAction.values()) {
            tempDynoActions.add(action.name());
        }
        return tempDynoActions;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {

        User author = e.getAuthor();
        Member member = e.getMember();
        Message message = e.getMessage();
        MessageChannel channel = e.getChannel();

        String msg = message.getContent();
        GuildController guildController = new GuildController(e.getGuild());
        Role adminRole = guildController.getGuild().getRolesByName("Admin", false).get(0);

        if (!author.isBot()) {

            String commandeComplete = null;
            try {

                if (msg.matches("^" + prefixCmd + ".*")) {
                    commandeComplete = msg.split(prefixCmd)[1];

                    if (commandeComplete != null && !commandeComplete.isEmpty()) {

                        String[] cmdSplit = commandeComplete.split(SEPARATOR_ACTION_ARGS, 2);

                        String[] args = new String[0];
                        String action = null;
                        if (cmdSplit.length > 0) {
                            action = cmdSplit[0];
                            if (cmdSplit.length > 1) {
                                args = cmdSplit[1].split(SEPARATOR_ACTION_ARGS, 1);
                            }
                        }

                        EnumAction actionEnum = null;
                        try {

                            if (!dynoActions.contains(action.toUpperCase())) {

                                actionEnum = EnumAction.fromValue(action);

                                switch (actionEnum) {
                                case HELPFCB:
                                    channel.sendMessage(buildHelpMessage()).queue();
                                case CHANGE:
                                    if (member.getRoles().contains(adminRole)) {
                                        changePrefixe(author, channel, commandeComplete, args[0]);
                                    } else {
                                        sendMessageNotEnoughRights(channel);
                                    }
                                    break;
                                case ACC:
                                    if (member.getRoles().contains(adminRole)) {
                                        addCustomCommand(author, channel, commandeComplete);
                                    } else {
                                        sendMessageNotEnoughRights(channel);
                                    }
                                    break;
                                case SINGLE:
                                    if (member.getRoles().contains(adminRole)) {
                                        toggleSingleRank(channel);
                                    } else {
                                        sendMessageNotEnoughRights(channel);
                                    }
                                    break;

                                case LIST:
                                    listAssignableRanks(author, channel, commandeComplete);
                                    break;

                                case ADD_RANK:
                                    if (member.getRoles().contains(adminRole)) {
                                        addAssignableRanks(author, channel, guildController, commandeComplete, args[0]);
                                    } else {
                                        sendMessageNotEnoughRights(channel);
                                    }
                                    break;
                                case REMOVE_RANK:
                                    if (member.getRoles().contains(adminRole)) {
                                        removeAssignableRanks(author, channel, guildController, commandeComplete, args[0]);
                                    } else {
                                        sendMessageNotEnoughRights(channel);
                                    }
                                    break;
                                case RANK:
                                    String roleStr = StringUtils.join(new ArrayList<>(Arrays.asList(args)), " ");
                                    manageRankCmd(author, channel, guildController, roleStr, member);
                                    break;
                                case MAKE:
                                    makeMeASandwich(channel, args[0]);
                                    break;
                                case SUDO:
                                    sudoMakeMeASandwich(member.getRoles().contains(adminRole), channel, args[0]);
                                    break;
                                default:
                                    System.out.println("Commande non prise en charge");
                                    break;
                                }
                            }
                        } catch (IllegalArgumentException ex) {
                            sendBotMessage(channel, "Commande inconnue.");
                        }
                    }
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
            }
        }
    }

    private void addCustomCommand(User author, MessageChannel channel, String commandeComplete) {
        String message1 = "Commande non implémentée pour le moment. ";
        System.out.println("Commande " + commandeComplete + " lancée par " + author.getName() + " : " + message1);
        sendBotMessage(channel, message1);
    }

    private void sudoMakeMeASandwich(boolean contains, MessageChannel channel, String arg) {
        if ("make me a sandwich".equals(arg)) {
            if (contains) {
                sendBotMessage(channel, "Here you go, good Lord !");
            } else {
                sendBotMessage(channel, "No way, you're not even Admin !");
            }
        } else {
            sendBotMessage(channel, "/shrug");
        }
    }

    private void makeMeASandwich(MessageChannel channel, String arg) {
        if ("me a sandwich".equals(arg)) {
            sendBotMessage(channel, "No, YOU make me a sandwich !");
        } else {
            sendBotMessage(channel, "/shrug");
        }
    }

    private void changePrefixe(User author, MessageChannel channel, String commandeComplete, String arg) {
        if (arg.trim().length() > 1) {
            String message1 = "Le prefixe de commande doit être un caractère unique. ";
            System.out.println("Commande " + commandeComplete + " lancée par " + author.getName() + " : " + message1);
            sendBotMessage(channel, message1);
        } else {
            prefixCmd = "[" + arg + "]";
            String message1 = "Prefixe de commande modifié pour \"" + arg + "\"";
            System.out.println("Commande " + commandeComplete + " lancée par " + author.getName() + " : " + message1);
            sendBotMessage(channel, message1);
        }
    }

    private void listAssignableRanks(User author, MessageChannel channel, String commandeComplete) {
        if (selfAssignableRanks != null && !selfAssignableRanks.isEmpty()) {

            StringBuilder sb = new StringBuilder();

            sb.append("Les ranks assignables sont : \r\n");
            for (Role role : selfAssignableRanks) {
                sb.append(role.getName());
                sb.append("\r\n");
            }
            sendBotMessage(channel, sb.toString());
            String message1 = sb.toString();
            System.out.println("Commande " + commandeComplete + " lancée par " + author.getName() + " : " + message1);
        } else {
            String message1 = "Aucun rank assignable pour le moment. ";
            System.out.println("Commande " + commandeComplete + " lancée par " + author.getName() + " : " + message1);
            sendBotMessage(channel, message1);
        }
    }

    private void removeAssignableRanks(User author, MessageChannel channel, GuildController guildController, String commandeComplete, String arg) {
        String[] rolesToRemoveTable = arg.split(ROLES_SEPARATOR);

        Set<Role> listRolesToRemove = createListOfRoleFromStringTable(rolesToRemoveTable, guildController, channel);

        if (selfAssignableRanks != null && !selfAssignableRanks.isEmpty()) {
            selfAssignableRanks.removeAll(listRolesToRemove);
        } else {
            selfAssignableRanks = new HashSet<>();
        }

        if (!listRolesToRemove.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Les rôles suivants ne sont désormais plus assignables : \r\n");
            for (Role role : listRolesToRemove) {
                sb.append(role.getName()).append("\r\n");
            }

            System.out.println("Commande " + commandeComplete + " lancée par " + author.getName() + " : " + sb.toString());
            sendBotMessage(channel, sb.toString());
        }
    }

    private void addAssignableRanks(User author, MessageChannel channel, GuildController guildController, String commandeComplete, String arg) {
        String[] rolesToAddTable = arg.split(ROLES_SEPARATOR);

        Set<Role> listRolesToAdd = createListOfRoleFromStringTable(rolesToAddTable, guildController, channel);

        if (selfAssignableRanks != null && !selfAssignableRanks.isEmpty()) {
            selfAssignableRanks.addAll(listRolesToAdd);
        } else {
            selfAssignableRanks = new HashSet<>();
            selfAssignableRanks.addAll(listRolesToAdd);
        }
        if (!listRolesToAdd.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Les rôles suivants sont désormais assignables : \r\n");
            for (Role role : listRolesToAdd) {
                sb.append(role.getName()).append("\r\n");
            }

            System.out.println("Commande " + commandeComplete + " lancée par " + author.getName() + " : " + sb.toString());
            sendBotMessage(channel, sb.toString());
        }
    }

    private Message buildHelpMessage() {
        MessageBuilder messageBuilder = new MessageBuilder();

        String prefixTrimed = prefixCmd.replace("[", "").replace("]", "");

        messageBuilder.append("Utilisation", UNDERLINE, ITALICS, BOLD).append(RETOUR_LIGNE);
        messageBuilder.append(RETOUR_LIGNE);
        messageBuilder.append("Toutes les commandes doivent commencer par le symbole '" + prefixTrimed + "', le nom de la commande et les valeurs possible (si besoin).").append(
                RETOUR_LIGNE);
        messageBuilder.append(prefixTrimed + "<COMMANDE>", BLOCK).append(RETOUR_LIGNE);
        messageBuilder.append(prefixTrimed + "<COMMANDE> <VALEUR_1>", BLOCK).append(RETOUR_LIGNE);
        messageBuilder.append(RETOUR_LIGNE);
        messageBuilder.append("Exemple :", ITALICS).append(RETOUR_LIGNE);
        messageBuilder.append("Se donne le rôle de Chevalier 60 : ").append(RETOUR_LIGNE);
        messageBuilder.append(prefixTrimed + "rank Chevalier 60", BLOCK).append(RETOUR_LIGNE);
        messageBuilder.append(RETOUR_LIGNE);
        messageBuilder.append("Liste de commandes utiles", UNDERLINE, ITALICS).append(RETOUR_LIGNE);
        messageBuilder.append(prefixTrimed + "rank <NOM_DU_ROLE>", BLOCK).append(" : S'attribuer ou révoquer un rôle.").append(RETOUR_LIGNE);
        messageBuilder.append(prefixTrimed + "list", BLOCK).append(" : Lister les rôles assignables.").append(RETOUR_LIGNE);
        messageBuilder.append(prefixTrimed + "helpfcb", BLOCK).append(" : Afficher cette aide.").append(RETOUR_LIGNE);
        messageBuilder.append(prefixTrimed + "help", BLOCK).append(" : Recevoir l'aide Dyno.").append(RETOUR_LIGNE);
        messageBuilder.append(RETOUR_LIGNE);

        messageBuilder.append("Liste des commandes réservées aux administrateurs", UNDERLINE, ITALICS).append(RETOUR_LIGNE);
        messageBuilder.append(prefixTrimed + "add_rank <NOM_DU_ROLE>", BLOCK).append(" : Ajouter un rôle à la liste des rôles assignables.").append(RETOUR_LIGNE).append(
                "(Vous pouvez en ajouter plusieur en les séparant par ','").append(RETOUR_LIGNE);
        messageBuilder.append("Exemple :", ITALICS).append(RETOUR_LIGNE);
        messageBuilder.append("Ajouter les rôles Chevalier 160 et Chevalier 162 en tant que rôle assignables").append(RETOUR_LIGNE);
        messageBuilder.append(prefixTrimed + "add Chevalier 160,Chevalier 162", BLOCK).append(RETOUR_LIGNE);
        messageBuilder.append(RETOUR_LIGNE);

        messageBuilder.append(prefixTrimed + "remove_rank <NOM_DU_ROLE>", BLOCK).append(" : Enlever un rôle de la liste des rôles assignables.").append(RETOUR_LIGNE);
        messageBuilder.append(prefixTrimed + "single", BLOCK).append(" : Basculer en mode “un seul rôle” (et inversement).").append(RETOUR_LIGNE);
        messageBuilder.append(prefixTrimed + "change <PREFIXE>", BLOCK).append(" : Changer de préfixe de commande __pour FrencheeseBot__.").append(RETOUR_LIGNE);
        messageBuilder.append("(A utiliser essentiellement en cas de conflit avec un autre bot)", ITALICS).append(RETOUR_LIGNE);
        messageBuilder.append(RETOUR_LIGNE);
        messageBuilder.append("Exemple :", ITALICS).append(RETOUR_LIGNE);
        messageBuilder.append(prefixTrimed + "change .", BLOCK).append(RETOUR_LIGNE);
        messageBuilder.append("Toutes les commandes commenceront à présent par '.'").append(RETOUR_LIGNE);

        return messageBuilder.build();
    }

    private void sendMessageNotEnoughRights(MessageChannel channel) {
        sendBotMessage(channel, "Désolé, vous n'avez pas les droits pour exécuter cette commande. ");
    }

    private Set<Role> createListOfRoleFromStringTable(String[] rolesToAddTable, GuildController guildController, MessageChannel channel) {

        Set<Role> listRole = new HashSet<>();

        for (String roleToAddString : rolesToAddTable) {
            Role roleToAdd = null;
            try {
                roleToAdd = guildController.getGuild().getRolesByName(roleToAddString, false).get(0);
                listRole.add(roleToAdd);
            } catch (IndexOutOfBoundsException ex) {
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setColor(Color.MAGENTA);

                StringBuilder sb = new StringBuilder();

                sb.append("Le role " + roleToAddString + " n'existe pas !");
                embedBuilder.setDescription(sb.toString());

                MessageBuilder messageBuilder = new MessageBuilder();
                messageBuilder.setEmbed(embedBuilder.build());
                channel.sendMessage(messageBuilder.build()).queue();
            }

        }

        return listRole;
    }

    private void toggleSingleRank(MessageChannel channel) {
        if (singleRole) {
            singleRole = false;
            sendBotMessage(channel, "Les ranks ne sont plus uniques par utilisateur. ");

        } else {
            singleRole = true;
            sendBotMessage(channel, "Les ranks sont à présent uniques par utilisateur.");
        }
    }

    private void sendBotMessage(MessageChannel channel, String message) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.MAGENTA);

        StringBuilder sb = new StringBuilder();

        sb.append(message);
        embedBuilder.setDescription(sb.toString());

        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.setEmbed(embedBuilder.build());
        channel.sendMessage(messageBuilder.build()).queue();
    }

    private void sendBotMessageWithMention(MessageChannel channel, String message, IMentionable mentionable) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.MAGENTA);

        StringBuilder sb = new StringBuilder();

        sb.append(message);
        embedBuilder.setDescription(sb.toString());

        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.append(mentionable);
        messageBuilder.setEmbed(embedBuilder.build());
        channel.sendMessage(messageBuilder.build()).queue();
    }

    private void manageRankCmd(User author, MessageChannel channel, GuildController guildController, String rankToAdd, Member member) {
        List<Role> userRoles = member.getRoles();
        List<Role> userAssignableRoles = findAssignableRole(userRoles);

        if ("".equals(rankToAdd)) {
            listRanksOfUser(channel, member);
        } else {
            List<Role> potentialRolesToAdd = jda.getRolesByName(rankToAdd, false);
            if (!potentialRolesToAdd.isEmpty()) {
                Role roleToAdd = potentialRolesToAdd.get(0);
                if (selfAssignableRanks != null && selfAssignableRanks.contains(roleToAdd)) {
                    StringBuilder messageBuilder = new StringBuilder();
                    if (!userRoles.contains(roleToAdd)) {

                        if (singleRole && !userAssignableRoles.isEmpty()) {
                            guildController.removeRolesFromMember(member, userAssignableRoles).complete();
                            member = guildController.getGuild().getMember(member.getUser());
                        }

                        guildController.addSingleRoleToMember(member, roleToAdd).complete();
                        member = guildController.getGuild().getMember(member.getUser());
                        messageBuilder.append("Vous êtes passé **").append(rankToAdd).append("** !");
                    } else {
                        guildController.removeSingleRoleFromMember(member, roleToAdd).complete();
                        member = guildController.getGuild().getMember(member.getUser());
                        messageBuilder.append("Vous n'êtes plus **").append(rankToAdd).append("** !");
                    }
                    sendBotMessageWithMention(channel, messageBuilder.toString(), author);

                    // listRanksOfUser(channel, member);
                    logRolesOfMember(member);

                } else {
                    sendBotMessage(channel, "Le rôle **" + rankToAdd + "** n'est pas assignable à soi-même.\r\nMerci de contacter un admin ou un modérateur pour vous l'ajouter.");
                }
            } else {
                sendBotMessage(channel, "Le rôle **" + rankToAdd + "** n'existe pas.");
            }
        }
    }

    private void logRolesOfMember(Member member) {
        StringBuilder sb = new StringBuilder();
        sb.append("Le membre " + member.getEffectiveName() + " a désormais les rôles suivants : \r\n");
        for (Role role : member.getRoles()) {
            sb.append(role.getName() + "\r\n");
        }
        System.out.print(sb.toString());
    }

    private void listRanksOfUser(MessageChannel channel, Member member) {
        StringBuilder sb = new StringBuilder();
        sb.append("Vous avez actuellement les rôles suivants : \r\n");
        for (Role role : member.getRoles()) {
            sb.append(role.getName() + "\r\n");
        }
        sendBotMessage(channel, sb.toString());
    }

    private List<Role> findAssignableRole(List<Role> userRoles) {

        List<Role> assignableRoles = new ArrayList<>();

        for (Role userRole : userRoles) {
            if (selfAssignableRanks != null && selfAssignableRanks.contains(userRole)) {
                assignableRoles.add(userRole);
            }
        }

        return assignableRoles;
    }

    public List<String> getDynoActions() {
        return dynoActions;
    }

    public void setDynoActions(List<String> dynoActions) {
        this.dynoActions = dynoActions;
    }

    private class Reponse {

        EnumAction action;

        String response;

        List<String> parameters;

        public EnumAction getAction() {
            return action;
        }

        public void setAction(EnumAction action) {
            this.action = action;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public List<String> getParameters() {
            return parameters;
        }

        public void setParameters(List<String> parameters) {
            this.parameters = parameters;
        }
    }

    private class Commande {

        EnumAction action;

        List<String> parameters;

        public EnumAction getAction() {
            return action;
        }

        public void setAction(EnumAction action) {
            this.action = action;
        }

        public List<String> getParameters() {
            return parameters;
        }

        public void setParameters(List<String> parameters) {
            this.parameters = parameters;
        }
    }
}