package net.deathpole.deathbot.Services.Impl;

import static net.dv8tion.jda.core.MessageBuilder.Formatting.BLOCK;
import static net.dv8tion.jda.core.MessageBuilder.Formatting.BOLD;
import static net.dv8tion.jda.core.MessageBuilder.Formatting.ITALICS;
import static net.dv8tion.jda.core.MessageBuilder.Formatting.UNDERLINE;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import net.deathpole.deathbot.CustomReaction;
import net.deathpole.deathbot.Enums.EnumAction;
import net.deathpole.deathbot.Enums.EnumCadavreExquisParams;
import net.deathpole.deathbot.Enums.EnumDynoAction;
import net.deathpole.deathbot.Services.ICommandesService;
import net.deathpole.deathbot.Services.IMessagesService;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.managers.GuildController;

/**
 * Created by nicolas on 28/09/17.
 */
public class CommandesServiceImpl implements ICommandesService {

    private static final String PREFIX_TAG = "@";
    Map<String, CustomReaction> mapCustomReactions;

    private static final String SEPARATOR_ACTION_ARGS = " ";
    private static final String ROLES_SEPARATOR = ",";
    private static final String RETOUR_LIGNE = "\r\n";
    Set<Role> selfAssignableRanks;
    List<String> dynoActions = initDynoActions();
    private String prefixCmd = "[?]";
    private boolean singleRole = true;
    List<Member> listCadavreSujet;
    List<String> listCadavreAction;
    List<String> listCadavreComplement;
    List<String> listCadavreAdjectif;
    private boolean botActivated = false;
    private IMessagesService messagesService = new MessagesServiceImpl();

    private static List<String> initDynoActions() {
        List<String> tempDynoActions = new ArrayList<>();

        for (EnumDynoAction action : EnumDynoAction.values()) {
            tempDynoActions.add(action.name());
        }
        return tempDynoActions;
    }

    @Override
    public void executeAction(MessageReceivedEvent e) {

        GuildController guildController = new GuildController(e.getGuild());
        Role adminRole = guildController.getGuild().getRolesByName("Admin", false).get(0);
        User author = e.getAuthor();

        if (!author.isBot()) {
            Member member = e.getMember();
            Message message = e.getMessage();
            MessageChannel channel = e.getChannel();
            String msg = message.getContent();

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
                            if(botActivated || EnumAction.ACTIVATE.name().equals(action.toUpperCase())) {
                                if (!dynoActions.contains(action.toUpperCase())) {
                                    if (mapCustomReactions != null && mapCustomReactions.keySet().contains(action)) {
                                        executeCustomReaction(guildController, channel, args[0], action);
                                    } else {
                                        executeEmbeddedAction(guildController, adminRole, author, member, channel, commandeComplete, args, action);
                                    }
                                }
                            }
                        } catch (IllegalArgumentException ex) {
                            messagesService.sendBotMessage(channel, "Commande inconnue.");
                        }
                    }
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
            }
        }
    }

    private void executeEmbeddedAction(GuildController guildController, Role adminRole, User author, Member member, MessageChannel channel, String commandeComplete, String[] args,
            String action) {
        EnumAction actionEnum;
        actionEnum = EnumAction.fromValue(action);

        switch (actionEnum) {
        case DEACTIVATE:
            if (member.getRoles().contains(adminRole)) {
                if(botActivated) {
                    botActivated = false;
                    String prefixTrimed = prefixCmd.replace("[", "").replace("]", "");
                    messagesService.sendBotMessage(channel, "Le bot est à présent désactivé !\r\nPour le réactiver tapez \"" + prefixTrimed + "activate\" (commande administrateur)");
                }else{
                    messagesService.sendBotMessage(channel, "Le bot est déjà désactivé !");
                }
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case ACTIVATE:
            if (member.getRoles().contains(adminRole)) {
                if(!botActivated) {
                    botActivated = true;
                    messagesService.sendBotMessage(channel, "Le bot est à présent activé !");
                }else{
                    messagesService.sendBotMessage(channel, "Le bot est déjà activé !");
                }
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case WITHOUT:
            String seekWithoutRoleStr = args[0];
            searchUsersWithoutRole(guildController, channel, seekWithoutRoleStr);
            break;
        case WITH:
            String seekWithRoleStr = args[0];
            searchUsersWithRole(guildController, channel, seekWithRoleStr);
            break;
        case CADAVRE:
            if (member.getRoles().contains(adminRole)) {

                String[] params = args[0].split(SEPARATOR_ACTION_ARGS, 2);
                EnumCadavreExquisParams param = EnumCadavreExquisParams.valueOf(params[0].toUpperCase());
                executeCadavreActions(guildController, channel, params, param);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case HELPFCB:
            channel.sendMessage(buildHelpMessage()).queue();
        case CHANGE:
            if (member.getRoles().contains(adminRole)) {
                changePrefixe(author, channel, commandeComplete, args[0]);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case ACR:
            if (member.getRoles().contains(adminRole)) {
                String[] fullParams = args[0].split(SEPARATOR_ACTION_ARGS, 2);
                String keyWord = fullParams[0];
                String reaction = fullParams[1];
                addCustomReaction(author, channel, keyWord, reaction);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case SINGLE:
            if (member.getRoles().contains(adminRole)) {
                toggleSingleRank(channel);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;

        case LIST:
            listAssignableRanks(author, channel, commandeComplete);
            break;

        case ADD_RANK:
            if (member.getRoles().contains(adminRole)) {
                addAssignableRanks(author, channel, guildController, commandeComplete, args[0]);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case REMOVE_RANK:
            if (member.getRoles().contains(adminRole)) {
                removeAssignableRanks(author, channel, guildController, commandeComplete, args[0]);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
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

    private void searchUsersWithRole(GuildController guildController, MessageChannel channel, String roleToSearch) {
        List<Role> foundWithRoles = guildController.getGuild().getRolesByName(roleToSearch, true);
        if (foundWithRoles != null && !foundWithRoles.isEmpty()) {
            Role seekRole = foundWithRoles.get(0);
            List<Member> membersWithRole = guildController.getGuild().getMembersWithRoles(seekRole);
            StringBuilder sb = new StringBuilder();
            if (membersWithRole != null && !membersWithRole.isEmpty()) {
                sb.append("Les membres suivants ont le rôle **").append(seekRole.getName()).append("** : \r\n");

                for (Member memberWithRole : membersWithRole) {
                    sb.append(memberWithRole.getEffectiveName()).append("\r\n");
                }

            } else {
                sb.append("Aucun membre n'a le rôle **").append(seekRole.getName()).append("** !");
            }

            messagesService.sendBotMessage(channel, sb.toString());

        } else {
            messagesService.sendBotMessage(channel, "Le rôle " + roleToSearch + " est inconnu !");
        }
    }

    private void searchUsersWithoutRole(GuildController guildController, MessageChannel channel, String roleToSearch) {
        List<Role> foundWithoutRoles = guildController.getGuild().getRolesByName(roleToSearch, true);
        if (foundWithoutRoles != null && !foundWithoutRoles.isEmpty()) {
            Role seekRole = foundWithoutRoles.get(0);
            List<Member> membersWithRole = guildController.getGuild().getMembersWithRoles(seekRole);
            List<Member> allMembers = guildController.getGuild().getMembers();

            List<Member> membersWithoutRole = allMembers.stream().filter(guildMember -> !membersWithRole.contains(guildMember)).collect(Collectors.toList());

            StringBuilder sb = new StringBuilder();
            sb.append("Les membres suivants n'ont pas le rôle **").append(seekRole.getName()).append("** : \r\n");

            for (Member memberWithoutRole : membersWithoutRole) {
                sb.append(memberWithoutRole.getEffectiveName()).append("\r\n");
            }

            messagesService.sendBotMessage(channel, sb.toString());

        } else {
            messagesService.sendBotMessage(channel, "Le rôle " + roleToSearch + " est inconnu !");
        }

        return;
    }

    private void executeCadavreActions(GuildController guildController, MessageChannel channel, String[] params, EnumCadavreExquisParams param) {
        switch (param) {
        case ADD_SUJET:
            addSujetCadavre(guildController, params[1]);
            break;
        case ADD_ACTION:
            addActionCadavre(params[1]);
            break;
        case ADD_COMPLEMENT:
            addComplementCadavre(params[1]);
            break;
        case ADD_ADJECTIF:
            addAdjectifCadavre(params[1]);
            break;
        case EXQUIS:
            displayCadavreExquis(channel);
            break;
        }
    }

    private void displayCadavreExquis(MessageChannel channel) {
        if (!listCadavreSujet.isEmpty() && !listCadavreAction.isEmpty() && !listCadavreComplement.isEmpty() && !listCadavreAdjectif.isEmpty()) {
            Member cadavre = listCadavreSujet.get(new Random().nextInt(listCadavreSujet.size()));

            StringBuilder sb = new StringBuilder();
            sb.append(SEPARATOR_ACTION_ARGS).append(listCadavreAction.get(new Random().nextInt(listCadavreAction.size()))).append(SEPARATOR_ACTION_ARGS).append(
                    listCadavreComplement.get(new Random().nextInt(listCadavreComplement.size()))).append(SEPARATOR_ACTION_ARGS).append(
                            listCadavreAdjectif.get(new Random().nextInt(listCadavreAdjectif.size())));

            messagesService.sendBotMessageWithMention(channel, sb.toString(), cadavre);
        }
    }

    private void addAdjectifCadavre(String param) {
        String adjectifCadavre = param;
        if (listCadavreAdjectif == null) {
            listCadavreAdjectif = new ArrayList<>();
        }
        listCadavreAdjectif.add(adjectifCadavre);
    }

    private void addComplementCadavre(String param) {
        String complementCadavre = param;
        if (listCadavreComplement == null) {
            listCadavreComplement = new ArrayList<>();
        }
        listCadavreComplement.add(complementCadavre);
    }

    private void addActionCadavre(String param) {
        String actionCadavre = param;
        if (listCadavreAction == null) {
            listCadavreAction = new ArrayList<>();
        }
        listCadavreAction.add(actionCadavre);
    }

    private void addSujetCadavre(GuildController guildController, String param) {
        String user = param;
        if (user != null) {
            Member cadavreSujet = guildController.getGuild().getMembersByName(user, false).get(0);
            if (listCadavreSujet == null) {
                listCadavreSujet = new ArrayList<>();
            }

            listCadavreSujet.add(cadavreSujet);
        }
    }

    private void executeCustomReaction(GuildController guildController, MessageChannel channel, String arg, String action) {
        CustomReaction customReaction = mapCustomReactions.get(action);
        String[] params = arg.trim().split(SEPARATOR_ACTION_ARGS + "+");

        if (params.length != customReaction.getNumberOfParams()) {
            messagesService.sendBotMessage(channel, "Le nombre d'argument n'est pas le bon ! Try again !");
        } else {
            String reactionReplaced = customReaction.getReaction();
            for (String param : params) {

                if (param.startsWith(PREFIX_TAG)) {
                    String effectiveName = param.replace(PREFIX_TAG, "");
                    List<Member> membersFound = guildController.getGuild().getMembersByEffectiveName(effectiveName, false);
                    if (membersFound != null && !membersFound.isEmpty()) {
                        Member memberFound = membersFound.get(0);
                        param = memberFound.getAsMention();
                    }
                }

                reactionReplaced = reactionReplaced.replaceFirst("\\$[0-9]+", param);
            }

            messagesService.sendBotMessage(channel, reactionReplaced);
        }
    }

    private void addCustomReaction(User author, MessageChannel channel, String keyWord, String reaction) {

        if (mapCustomReactions == null) {
            mapCustomReactions = new HashMap<>();
        }

        CustomReaction customReaction = new CustomReaction();
        customReaction.setReaction(reaction);
        customReaction.setNumberOfParams(reaction.length() - reaction.replace("$", "").length());

        mapCustomReactions.put(keyWord, customReaction);

        StringBuilder sb = new StringBuilder();
        sb.append("Nouvelle réponse ajoutée pour la commande : " + keyWord);

        messagesService.sendBotMessage(channel, sb.toString());
    }

    private void sudoMakeMeASandwich(boolean contains, MessageChannel channel, String arg) {
        if ("make me a sandwich".equals(arg)) {
            if (contains) {
                messagesService.sendBotMessage(channel, "Here you go, good Lord !");
            } else {
                messagesService.sendBotMessage(channel, "No way, you're not even Admin !");
            }
        } else {
            messagesService.sendBotMessage(channel, "/shrug");
        }
    }

    private void makeMeASandwich(MessageChannel channel, String arg) {
        if ("me a sandwich".equals(arg)) {
            messagesService.sendBotMessage(channel, "No, YOU make me a sandwich !");
        } else {
            messagesService.sendBotMessage(channel, "/shrug");
        }
    }

    private void changePrefixe(User author, MessageChannel channel, String commandeComplete, String arg) {
        if (arg.trim().length() > 1) {
            String message1 = "Le prefixe de commande doit être un caractère unique. ";
            System.out.println("Commande " + commandeComplete + " lancée par " + author.getName() + " : " + message1);
            messagesService.sendBotMessage(channel, message1);
        } else {
            prefixCmd = "[" + arg + "]";
            String message1 = "Prefixe de commande modifié pour \"" + arg + "\"";
            System.out.println("Commande " + commandeComplete + " lancée par " + author.getName() + " : " + message1);
            messagesService.sendBotMessage(channel, message1);
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
            messagesService.sendBotMessage(channel, sb.toString());
            String message1 = sb.toString();
            System.out.println("Commande " + commandeComplete + " lancée par " + author.getName() + " : " + message1);
        } else {
            String message1 = "Aucun rank assignable pour le moment. ";
            System.out.println("Commande " + commandeComplete + " lancée par " + author.getName() + " : " + message1);
            messagesService.sendBotMessage(channel, message1);
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
            messagesService.sendBotMessage(channel, sb.toString());
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
            messagesService.sendBotMessage(channel, sb.toString());
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
            messagesService.sendBotMessage(channel, "Les ranks ne sont plus uniques par utilisateur. ");

        } else {
            singleRole = true;
            messagesService.sendBotMessage(channel, "Les ranks sont à présent uniques par utilisateur.");
        }
    }

    private void manageRankCmd(User author, MessageChannel channel, GuildController guildController, String rankToAdd, Member member) {
        List<Role> userRoles = member.getRoles();
        List<Role> userAssignableRoles = findAssignableRole(userRoles);

        if ("".equals(rankToAdd)) {
            listRanksOfUser(channel, member);
        } else {
            List<Role> potentialRolesToAdd = guildController.getJDA().getRolesByName(rankToAdd, false);
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
                    messagesService.sendBotMessageWithMention(channel, messageBuilder.toString(), author);

                    // listRanksOfUser(channel, member);
                    logRolesOfMember(member);

                } else {
                    messagesService.sendBotMessage(channel,
                            "Le rôle **" + rankToAdd + "** n'est pas assignable à soi-même.\r\nMerci de contacter un admin ou un modérateur pour vous l'ajouter.");
                }
            } else {
                messagesService.sendBotMessage(channel, "Le rôle **" + rankToAdd + "** n'existe pas.");
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
        messagesService.sendBotMessage(channel, sb.toString());
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

    public List<Member> getListCadavreSujet() {
        return listCadavreSujet;
    }

    public void setListCadavreSujet(List<Member> listCadavreSujet) {
        this.listCadavreSujet = listCadavreSujet;
    }

    public List<String> getListCadavreAction() {
        return listCadavreAction;
    }

    public void setListCadavreAction(List<String> listCadavreAction) {
        this.listCadavreAction = listCadavreAction;
    }

    public List<String> getListCadavreComplement() {
        return listCadavreComplement;
    }

    public void setListCadavreComplement(List<String> listCadavreComplement) {
        this.listCadavreComplement = listCadavreComplement;
    }

    public List<String> getListCadavreAdjectif() {
        return listCadavreAdjectif;
    }

    public void setListCadavreAdjectif(List<String> listCadavreAdjectif) {
        this.listCadavreAdjectif = listCadavreAdjectif;
    }
}
