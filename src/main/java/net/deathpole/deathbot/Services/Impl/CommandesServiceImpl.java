package net.deathpole.deathbot.Services.Impl;

import static net.dv8tion.jda.core.MessageBuilder.Formatting.BLOCK;
import static net.dv8tion.jda.core.MessageBuilder.Formatting.BOLD;
import static net.dv8tion.jda.core.MessageBuilder.Formatting.ITALICS;
import static net.dv8tion.jda.core.MessageBuilder.Formatting.UNDERLINE;

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
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
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
    private static final String SEPARATOR_ACTION_ARGS = " ";
    private static final String ROLES_SEPARATOR = ",";
    private static final String RETOUR_LIGNE = "\r\n";

    private List<String> dynoActions = initDynoActions();

    private HashMap<Guild, Set<Role>> selfAssignableRanksByGuild = new HashMap<>();
    private HashMap<Guild, String> prefixCmdByGuild = new HashMap<>();
    private HashMap<Guild, Boolean> singleRoleByGuild = new HashMap<>();
    private HashMap<Guild, Boolean> botActivationByGuild = new HashMap<>();
    private Map<String, CustomReaction> mapCustomReactions;

    private List<Member> listCadavreSujet;
    private List<String> listCadavreAction;
    private List<String> listCadavreComplement;
    private List<String> listCadavreAdjectif;


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

        String prefixCmd = getPrefixCmdForGuild(guildController.getGuild());

        if (!author.isBot()) {
            Member member = e.getMember();
            Message message = e.getMessage();
            MessageChannel channel = e.getChannel();
            String msg = message.getContent();

            String commandeComplete;
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

                        try {
                            assert action != null;
                            if(isBotActivated(guildController.getGuild()) || EnumAction.ACTIVATE.name().equals(action.toUpperCase())) {
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
                messagesService.sendBotMessage(channel, "Un problème est survenu. Merci de contacter l'administrateur.");
            }
        }
    }

    private String getPrefixCmdForGuild(Guild guild) {
        prefixCmdByGuild.putIfAbsent(guild, "[?]");
        return prefixCmdByGuild.get(guild);
    }

    private boolean isBotActivated(Guild guild) {
        botActivationByGuild.putIfAbsent(guild, false);

        return botActivationByGuild.get(guild);
    }

    private void executeEmbeddedAction(GuildController guildController, Role adminRole, User author, Member member, MessageChannel channel, String commandeComplete, String[] args,
            String action) {

        EnumAction actionEnum = EnumAction.fromValue(action);
        boolean isAdmin = member.getRoles().contains(adminRole);
        Guild guild = guildController.getGuild();

        switch (actionEnum) {
        case DEACTIVATE:
            if (isAdmin) {
                if(isBotActivated(guild)) {
                    activateOrDeactivateBotForGuild(guild, false);

                    String prefixCmd = getPrefixCmdForGuild(guild);

                    String prefixTrimed = prefixCmd.replace("[", "").replace("]", "");
                    messagesService.sendBotMessage(channel, "Le bot est à présent désactivé !\r\nPour le réactiver tapez \"" + prefixTrimed + "activate\" (commande administrateur)");
                }else{
                    messagesService.sendBotMessage(channel, "Le bot est déjà désactivé !");
                }
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case LARO:
            if (isAdmin) {
                StringBuilder sb = new StringBuilder();
                List<Role> allRoles = guild.getRoles();

                if (allRoles != null && !allRoles.isEmpty()) {
                    sb.append("Les roles du serveur sont : ").append(RETOUR_LIGNE).append(RETOUR_LIGNE);
                    allRoles.stream().filter(role -> !role.getName().startsWith("@everyone")).forEach(role -> sb.append(role.getName()).append(" (").append(
                            guild.getMembersWithRoles(role).size()).append(" membres)").append(RETOUR_LIGNE));
                }else{
                    sb.append("Aucun rôle sur ce serveur ! Ce n'est pas normal ! ¯\\_(ツ)_/¯");
                }
                messagesService.sendBotMessage(channel, sb.toString());
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case ACTIVATE:
            if (isAdmin) {
                if(!isBotActivated(guild)) {
                    activateOrDeactivateBotForGuild(guild, true);
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
            if (isAdmin) {

                String[] params = args[0].split(SEPARATOR_ACTION_ARGS, 2);
                EnumCadavreExquisParams param = EnumCadavreExquisParams.valueOf(params[0].toUpperCase());
                executeCadavreActions(guildController, channel, params, param);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case HELPFCB:
            channel.sendMessage(buildHelpMessage(guild)).queue();
            break;
        case CHANGE:
            if (isAdmin) {
                changePrefixe(author, channel, commandeComplete, args[0], guild);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case ACR:
            if (isAdmin) {
                String[] fullParams = args[0].split(SEPARATOR_ACTION_ARGS, 2);
                String keyWord = fullParams[0];
                String reaction = fullParams[1];
                addCustomReaction(channel, keyWord, reaction);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case SINGLE:
            if (isAdmin) {
                toggleSingleRank(channel, guild);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;

        case LIST:
            listAssignableRanks(guild, author, channel, commandeComplete);
            break;

        case ADD_RANK:
            if (isAdmin) {
                addAssignableRanks(author, channel, guildController, commandeComplete, args[0]);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case REMOVE_RANK:
            if (isAdmin) {
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
            sudoMakeMeASandwich(isAdmin, channel, args[0]);
            break;
        default:
            System.out.println("Commande non prise en charge");
            break;
        }
    }

    private void activateOrDeactivateBotForGuild(Guild guild, boolean activate) {
        botActivationByGuild.put(guild, activate);
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

            String sb = SEPARATOR_ACTION_ARGS + listCadavreAction.get(new Random().nextInt(listCadavreAction.size())) + SEPARATOR_ACTION_ARGS +
                    listCadavreComplement.get(new Random().nextInt(listCadavreComplement.size())) + SEPARATOR_ACTION_ARGS +
                    listCadavreAdjectif.get(new Random().nextInt(listCadavreAdjectif.size()));

            messagesService.sendBotMessageWithMention(channel, sb, cadavre);
        }
    }

    private void addAdjectifCadavre(String param) {
        if (listCadavreAdjectif == null) {
            listCadavreAdjectif = new ArrayList<>();
        }
        listCadavreAdjectif.add(param);
    }

    private void addComplementCadavre(String param) {
        if (listCadavreComplement == null) {
            listCadavreComplement = new ArrayList<>();
        }
        listCadavreComplement.add(param);
    }

    private void addActionCadavre(String param) {
        if (listCadavreAction == null) {
            listCadavreAction = new ArrayList<>();
        }
        listCadavreAction.add(param);
    }

    private void addSujetCadavre(GuildController guildController, String param) {
        if (param != null) {
            Member cadavreSujet = guildController.getGuild().getMembersByName(param, false).get(0);
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

    private void addCustomReaction(MessageChannel channel, String keyWord, String reaction) {

        if (mapCustomReactions == null) {
            mapCustomReactions = new HashMap<>();
        }

        CustomReaction customReaction = new CustomReaction();
        customReaction.setReaction(reaction);
        customReaction.setNumberOfParams(reaction.length() - reaction.replace("$", "").length());

        mapCustomReactions.put(keyWord, customReaction);

        messagesService.sendBotMessage(channel, "Nouvelle réponse ajoutée pour la commande : " + keyWord);
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

    private void changePrefixe(User author, MessageChannel channel, String commandeComplete, String arg, Guild guild) {
        if (arg.trim().length() > 1) {
            String message1 = "Le prefixe de commande doit être un caractère unique. ";
            System.out.println("Commande " + commandeComplete + " lancée par " + author.getName() + " : " + message1);
            messagesService.sendBotMessage(channel, message1);
        } else {
            String prefixCmd = "[" + arg + "]";
            setPrefixCmdForGuild(guild, prefixCmd);

            String message1 = "Prefixe de commande modifié pour \"" + arg + "\"";
            System.out.println("Commande " + commandeComplete + " lancée par " + author.getName() + " : " + message1);
            messagesService.sendBotMessage(channel, message1);
        }
    }

    private void setPrefixCmdForGuild(Guild guild, String prefixCmd) {
        prefixCmdByGuild.put(guild, prefixCmd);
    }

    private void listAssignableRanks(Guild guild, User author, MessageChannel channel, String commandeComplete) {
        Set<Role> selfAssignableRanks = getSelfAssignableRanksForGuild(guild);
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

        Guild guild = guildController.getGuild();
        Set<Role> selfAssignableRanks = getSelfAssignableRanksForGuild(guild);

        if (selfAssignableRanks != null && !selfAssignableRanks.isEmpty()) {
            selfAssignableRanks.removeAll(listRolesToRemove);
        } else {
            selfAssignableRanks = new HashSet<>();
        }

        addAssignableRanksForGuild(guild, selfAssignableRanks);

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

    private Set<Role> getSelfAssignableRanksForGuild(Guild guild) {
        return selfAssignableRanksByGuild != null ? selfAssignableRanksByGuild.get(guild) : null;
    }

    private void addAssignableRanks(User author, MessageChannel channel, GuildController guildController, String commandeComplete, String arg) {
        String[] rolesToAddTable = arg.split(ROLES_SEPARATOR);
        Guild guild = guildController.getGuild();

        Set<Role> listRolesToAdd = createListOfRoleFromStringTable(rolesToAddTable, guildController, channel);
        Set<Role> selfAssignableRanks = getSelfAssignableRanksForGuild(guild);

        if (selfAssignableRanks != null && !selfAssignableRanks.isEmpty()) {
            selfAssignableRanks.addAll(listRolesToAdd);
        } else {
            selfAssignableRanks = new HashSet<>();
            selfAssignableRanks.addAll(listRolesToAdd);
        }
        addAssignableRanksForGuild(guild, selfAssignableRanks);

        if (!listRolesToAdd.isEmpty()) {
            StringBuilder sb = buildNewlyAssignableRolesListMessage(author, commandeComplete, listRolesToAdd);
            messagesService.sendBotMessage(channel, sb.toString());
        }
    }

    private StringBuilder buildNewlyAssignableRolesListMessage(User author, String commandeComplete, Set<Role> listRolesToAdd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Les rôles suivants sont désormais assignables : \r\n");
        for (Role role : listRolesToAdd) {
            sb.append(role.getName()).append("\r\n");
        }

        System.out.println("Commande " + commandeComplete + " lancée par " + author.getName() + " : " + sb.toString());
        return sb;
    }

    private void addAssignableRanksForGuild(Guild guild, Set<Role> selfAssignableRanks) {
        if(selfAssignableRanksByGuild == null){
            selfAssignableRanksByGuild = new HashMap<>();
        }
        selfAssignableRanksByGuild.put(guild, selfAssignableRanks);
    }

    private Message buildHelpMessage(Guild guild) {
        MessageBuilder messageBuilder = new MessageBuilder();

        String prefixCmd = getPrefixCmdForGuild(guild);

        String prefixTrimed = prefixCmd.replace("[", "").replace("]", "");

        messageBuilder.append("Utilisation", UNDERLINE, ITALICS, BOLD).append(RETOUR_LIGNE);
        messageBuilder.append(RETOUR_LIGNE);
        messageBuilder.append("Toutes les commandes doivent commencer par le symbole '").append(prefixTrimed).append(
                "', le nom de la commande et les valeurs possible (si besoin).").append(
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
            Role roleToAdd;
            try {
                roleToAdd = guildController.getGuild().getRolesByName(roleToAddString, false).get(0);
                listRole.add(roleToAdd);
            } catch (IndexOutOfBoundsException ex) {

                messagesService.sendBotMessage(channel, ("Le role " + roleToAddString + " n'existe pas !"));
            }

        }

        return listRole;
    }

    private void toggleSingleRank(MessageChannel channel, Guild guild) {

        boolean singleRole = getSingleRoleForGuild(guild);

        if (singleRole) {
            toggleSingleRoleForGuild(guild, false);
            messagesService.sendBotMessage(channel, "Les ranks ne sont plus uniques par utilisateur. ");

        } else {
            toggleSingleRoleForGuild(guild, true);
            messagesService.sendBotMessage(channel, "Les ranks sont à présent uniques par utilisateur.");
        }
    }

    private boolean getSingleRoleForGuild(Guild guild) {
        singleRoleByGuild.putIfAbsent(guild, true);
        return singleRoleByGuild.get(guild);
    }

    private void toggleSingleRoleForGuild(Guild guild, boolean singleRole) {
        singleRoleByGuild.put(guild, singleRole);
    }

    private void manageRankCmd(User author, MessageChannel channel, GuildController guildController, String rankToAdd, Member member) {
        List<Role> userRoles = member.getRoles();
        List<Role> userAssignableRoles = findAssignableRole(userRoles, guildController.getGuild());

        if ("".equals(rankToAdd)) {
            listRanksOfUser(channel, member);
        } else {
            List<Role> potentialRolesToAdd = guildController.getJDA().getRolesByName(rankToAdd, false);
            if (!potentialRolesToAdd.isEmpty()) {
                Role roleToAdd = potentialRolesToAdd.get(0);

                Set<Role> selfAssignableRanks = getSelfAssignableRanksForGuild(guildController.getGuild());

                if (selfAssignableRanks != null && selfAssignableRanks.contains(roleToAdd)) {
                    StringBuilder messageBuilder = new StringBuilder();
                    if (!userRoles.contains(roleToAdd)) {

                        if (getSingleRoleForGuild(guildController.getGuild()) && !userAssignableRoles.isEmpty()) {
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
        sb.append("Le membre ").append(member.getEffectiveName()).append(" a désormais les rôles suivants : \r\n");
        for (Role role : member.getRoles()) {
            sb.append(role.getName()).append(RETOUR_LIGNE);
        }
        System.out.print(sb.toString());
    }

    private void listRanksOfUser(MessageChannel channel, Member member) {
        StringBuilder sb = new StringBuilder();
        sb.append("Vous avez actuellement les rôles suivants : \r\n");
        for (Role role : member.getRoles()) {
            sb.append(role.getName()).append(RETOUR_LIGNE);
        }
        messagesService.sendBotMessage(channel, sb.toString());
    }

    private List<Role> findAssignableRole(List<Role> userRoles, Guild guild) {

        List<Role> assignableRoles = new ArrayList<>();
        Set<Role> selfAssignableRanks = getSelfAssignableRanksForGuild(guild);

        assignableRoles.addAll(userRoles.stream().filter(userRole -> selfAssignableRanks != null && selfAssignableRanks.contains(userRole)).collect(Collectors.toList()));

        return assignableRoles;
    }
}
