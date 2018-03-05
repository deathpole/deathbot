package net.deathpole.deathbot.Services.Impl;

import static net.dv8tion.jda.core.MessageBuilder.Formatting.BLOCK;
import static net.dv8tion.jda.core.MessageBuilder.Formatting.BOLD;
import static net.dv8tion.jda.core.MessageBuilder.Formatting.ITALICS;
import static net.dv8tion.jda.core.MessageBuilder.Formatting.UNDERLINE;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import net.deathpole.deathbot.CustomReaction;
import net.deathpole.deathbot.Dao.IGlobalDao;
import net.deathpole.deathbot.Dao.Impl.GlobalDao;
import net.deathpole.deathbot.Enums.EnumAction;
import net.deathpole.deathbot.Enums.EnumCadavreExquisParams;
import net.deathpole.deathbot.Enums.EnumDynoAction;
import net.deathpole.deathbot.Services.ICommandesService;
import net.deathpole.deathbot.Services.IMessagesService;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
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
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    private List<String> dynoActions = initDynoActions();

    private HashMap<Guild, Set<Role>> selfAssignableRanksByGuild = new HashMap<>();
    private HashMap<Guild, Set<Role>> notSingleSelfAssignableRanksByGuild = new HashMap<>();

    // Maps used after
    private HashMap<Guild, String> prefixCmdByGuild = new HashMap<>();
    private HashMap<String, Set<String>> notSingleSelfAssignableRanksByIdsByGuild = initNotSingleAssignableRanksbyGuild();
    private HashMap<Guild, HashMap<String, CustomReaction>> mapCustomReactions = new HashMap<>();
    private HashMap<Guild, HashMap<String, String>> mapVoiceRole = new HashMap<>();
    private IGlobalDao globalDao;
    private HashMap<Guild, Boolean> singleRoleByGuild = new HashMap<>();
    private HashMap<Guild, Boolean> botActivationByGuild = new HashMap<>();
    private HashMap<Guild, String> welcomeMessageByGuild = new HashMap<>();
    // Maps used on DB load
    private HashMap<String, Set<String>> selfAssignableRanksByIdsByGuild = initAssignableRanksbyGuild();
    private HashMap<String, HashMap<String, CustomReaction>> mapCustomReactionsByGuild = initMapCustomReactions();
    private HashMap<String, HashMap<String, String>> mapVoiceRoleByGuild = initMapVoiceRoles();

    private List<Member> listCadavreSujet;
    private List<String> listCadavreAction;
    private List<String> listCadavreComplement;
    private List<String> listCadavreAdjectif;


    private IMessagesService messagesService = new MessagesServiceImpl();

    private HashMap<String, Set<String>> initAssignableRanksbyGuild() {
        if (globalDao == null) {
            globalDao = new GlobalDao();
        }
        return globalDao.initAssignableRanksbyGuild(true);
    }

    private HashMap<String,Set<String>> initNotSingleAssignableRanksbyGuild() {
        if (globalDao == null) {
            globalDao = new GlobalDao();
        }
        return globalDao.initAssignableRanksbyGuild(false);
    }

    private HashMap<String, HashMap<String, CustomReaction>> initMapCustomReactions() {
        if (globalDao == null) {
            globalDao = new GlobalDao();
        }
        return globalDao.initMapCustomReactions();
    }

    private HashMap<String, HashMap<String, String>> initMapVoiceRoles() {
        if (globalDao == null) {
            globalDao = new GlobalDao();
        }
        return globalDao.initMapVoiceRoles();
    }

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
        ChannelType channelType = e.getChannelType();
        if (!ChannelType.PRIVATE.equals(channelType)) {
            Role adminRole = guildController.getGuild().getRolesByName("Admin", true).get(0);
            User author = e.getAuthor();

            String prefixCmd = getPrefixCmdForGuild(guildController.getGuild());

            if (!author.isBot()) {
                Member member = e.getMember();
                Message message = e.getMessage();
                MessageChannel channel = e.getChannel();
                String msg = message.getContent();
                String prefixTrimed = prefixCmd.replace("[", "").replace("]", "");
                if (!prefixTrimed.equals(msg.trim())) {
                String commandeComplete;
                try {
                        if (msg.matches("^" + prefixCmd + "[^?]\\p{all}+")) {
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
                                    if (isBotActivated(guildController.getGuild()) || EnumAction.ACTIVATE.name().equals(action.toUpperCase())) {
                                        if (!dynoActions.contains(action.toUpperCase())) {
                                            HashMap<String, CustomReaction> customReactionsForGuild = getCustomReactionsForGuild(guildController);
                                            if (customReactionsForGuild != null && customReactionsForGuild.keySet().contains(action)) {
                                                String param = null;
                                                if (args.length >= 1) {
                                                    param = args[0];
                                                }
                                                executeCustomReaction(member, e.getMessage(), guildController, channel, param, action);
                                            } else {
                                                executeEmbeddedAction(guildController, adminRole, author, member, channel, commandeComplete, args, action, msg);
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
        }
    }

    private void sendPrivateMessage(User user, String content) {
        user.openPrivateChannel().queue((channel) -> channel.sendMessage(content).queue());
    }

    @Override
    public void userJoinedGuild(GuildMemberJoinEvent e) {
        Member connectedMember = e.getMember();
        User user = connectedMember.getUser();
        Guild guild = e.getGuild();
        String welcomeMessage = welcomeMessageByGuild.get(guild);
        if (welcomeMessage == null) {
            welcomeMessage = globalDao.getWelcomeMessage(guild);
        }
        if (welcomeMessage != null && !welcomeMessage.isEmpty()) {
            String[] toSendList = welcomeMessage.split("(?<=\\G.{2000})");
            for (String toSend : toSendList) {
                sendPrivateMessage(user, toSend);
            }
        }
    }

    @Override
    public void userJoinedVoiceChannel(VoiceChannel channel, Member connectedMember, Guild guild) {
        // User user = connectedMember.getUser();
        GuildController guildController = new GuildController(guild);

        HashMap<String, String> voiceRoles = getVoiceRolesForGuild(guildController);

        if (voiceRoles.keySet().contains(channel.getName())) {
            String rankToAdd = voiceRoles.get(channel.getName());
            List<Role> potentialRolesToAdd = guildController.getGuild().getRolesByName(rankToAdd, true);

            if (!potentialRolesToAdd.isEmpty()) {
                guildController.addSingleRoleToMember(connectedMember, potentialRolesToAdd.get(0)).complete();
            }
        }
    }

    @Override
    public void userLeftVoiceChannel(VoiceChannel channel, Member connectedMember, Guild guild) {
        // User user = connectedMember.getUser();
        GuildController guildController = new GuildController(guild);
        // Récupérer la liste des rôles/channel

        HashMap<String, String> voiceRoles = getVoiceRolesForGuild(guildController);

        if (voiceRoles.keySet().contains(channel.getName())) {
            String rankToAdd = voiceRoles.get(channel.getName());
            List<Role> potentialRolesToDelete = guildController.getGuild().getRolesByName(rankToAdd, true);

            if (!potentialRolesToDelete.isEmpty()) {
                guildController.removeSingleRoleFromMember(connectedMember, potentialRolesToDelete.get(0)).complete();
            }
        }
    }

    private String getPrefixCmdForGuild(Guild guild) {
        prefixCmdByGuild.putIfAbsent(guild, "[?]");
        return prefixCmdByGuild.get(guild);
    }

    private boolean isBotActivated(Guild guild) {
        boolean state = globalDao.getActivationState(guild);
        botActivationByGuild.putIfAbsent(guild, state);

        return botActivationByGuild.get(guild);
    }

    private void executeEmbeddedAction(GuildController guildController, Role adminRole, User author, Member member, MessageChannel channel, String commandeComplete, String[] args,
                                       String action, String originalMessage) {

        EnumAction actionEnum = EnumAction.fromValue(action);
        boolean isAdmin = member.getRoles().contains(adminRole);
        Guild guild = guildController.getGuild();

        switch (actionEnum) {
        case SET_WELCOME_MESSAGE:
                if (isAdmin) {
                setWelcomeMessageForGuild(originalMessage, guild);
                messagesService.sendBotMessage(channel, "Le message privé de bienvenue a bien été mis à jour.");
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;case DEACTIVATE:
            if (isAdmin) {
                deactivateBot(channel,guild);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case LARO:
            if (isAdmin) {
                listRolesForGuild(channel, guild);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case ACTIVATE:
            if (isAdmin) {
                activateBot(channel,guild);

            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case WITHOUT:
            searchUsersWithoutRole(guildController, channel, args[0]);
            break;
        case WITH:
            searchUsersWithRole(guildController, channel, args[0]);
            break;
        case CADAVRE:
            if (isAdmin) {

                manageCadavreExquis(guildController, channel, args[0]);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case HELPDB:
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
                manageAddCustomReaction(guild, channel, args[0]);
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
            listAssignableRanks(guildController, author, channel, commandeComplete);
            break;

        case ADD_RANK:
            if (isAdmin) {
                addAssignableRanks(author, channel, guildController, commandeComplete, args[0]);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case ADD_NOTSINGLE_RANK:
            if(isAdmin) {
                addNotSingleAssignableRanks(author, channel, guildController, commandeComplete, args[0]);
            }else{
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case ADD_VOICE_ROLE:
            if (isAdmin) {
                addVoiceRole(guild, member, author, channel, guildController, commandeComplete, args[0]);
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
        case REMOVE_NOTSINGLE_RANK:
            if (isAdmin) {
                removeNotSingleAssignableRanks(author, channel, guildController, commandeComplete, args[0]);
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
        case REVIVE:
            calculateSRandMedals(channel, guildController, args[0]);
        default:
            System.out.println("Commande non prise en charge");
            break;
        }
    }

    private void calculateSRandMedals(MessageChannel channel, GuildController guildController, String args) {

        String[] splittedArgs = args.split(" ");

        if (splittedArgs.length < 1) {
            messagesService.sendBotMessage(channel, "Il faut mettre des paramètres, hein...");
            return;
        }

        Integer stage = Integer.valueOf(splittedArgs[0]);

        if (stage % 10 != 0) {
            stage = Math.toIntExact(Math.round(Double.valueOf(stage / 10)) * 10);
        }

        Integer medalBonus = Integer.valueOf(splittedArgs[1]);

        BigDecimal medalGain = globalDao.getMedalGainForStage(stage);
        BigDecimal stageGain = medalGain.multiply(BigDecimal.valueOf(medalBonus));
        String formattedGain = formatBigNumbersToEFFormat(stageGain);

        if (splittedArgs.length > 2) {
            Integer runDuration = Integer.valueOf(splittedArgs[2]);
            BigDecimal SR = stageGain.divide(BigDecimal.valueOf(runDuration), BigDecimal.ROUND_HALF_DOWN);
            String formattedSR = formatBigNumbersToEFFormat(SR) + "/min";
            messagesService.sendBotMessage(channel, "SR : " + formattedSR);
        } else {
            messagesService.sendBotMessage(channel, "Gain immédiat : " + formattedGain);
        }
    }

    private String formatBigNumbersToEFFormat(BigDecimal value) {

        String result = null;

        BigDecimal medalsBase = BigDecimal.valueOf(1000000);
        BigDecimal factor = BigDecimal.valueOf(1000);

        if (value.compareTo(medalsBase) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##a");
            result = decimalFormat.format(value.divide(factor, BigDecimal.ROUND_HALF_DOWN));
        } else if (value.compareTo(medalsBase.multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##b");
            result = decimalFormat.format(value.divide(factor).divide(factor, BigDecimal.ROUND_HALF_DOWN).multiply(BigDecimal.TEN));

        } else if (value.compareTo(medalsBase.multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##c");
            result = decimalFormat.format(value.divide(factor.multiply(factor)).divide(factor, BigDecimal.ROUND_HALF_DOWN).multiply(BigDecimal.TEN));
        } else if (value.compareTo(medalsBase.multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##d");
            result = decimalFormat.format(value.divide(factor.multiply(factor).multiply(factor)).divide(factor, BigDecimal.ROUND_HALF_DOWN).multiply(BigDecimal.TEN));
        } else if (value.compareTo(medalsBase.multiply(factor).multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##e");
            result = decimalFormat.format(
                    value.divide(factor.multiply(factor).multiply(factor).multiply(factor)).divide(factor, BigDecimal.ROUND_HALF_DOWN).multiply(BigDecimal.TEN));
        } else if (value.compareTo(medalsBase.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##f");
            result = decimalFormat.format(
                    value.divide(factor.multiply(factor).multiply(factor).multiply(factor).multiply(factor)).divide(factor, BigDecimal.ROUND_HALF_DOWN).multiply(BigDecimal.TEN));
        } else if (value.compareTo(medalsBase.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##g");
            result = decimalFormat.format(
                    value.divide(factor.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)).divide(factor, BigDecimal.ROUND_HALF_DOWN).multiply(
                            BigDecimal.TEN));
        } else if (value.compareTo(medalsBase.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##h");
            result = decimalFormat.format(value.divide(factor.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)).divide(factor,
                    BigDecimal.ROUND_HALF_DOWN).multiply(BigDecimal.TEN));
        } else if (value.compareTo(
                medalsBase.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)) < 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##i");
            result = decimalFormat.format(
                    value.divide(factor.multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor).multiply(factor)).divide(factor,
                            BigDecimal.ROUND_HALF_DOWN).multiply(BigDecimal.TEN));
        }

        return result;
    }

    private void addVoiceRole(Guild guild, Member member, User author, MessageChannel channel, GuildController guildController, String commandeComplete, String arg) {
        VoiceChannel actualVoiceChannel = member.getVoiceState().getChannel();

        if (actualVoiceChannel == null) {
            messagesService.sendBotMessage(channel, "Vous devez être connecté sur un salon vocal pour exécuter cette commande !");
        } else {

            String channelName = actualVoiceChannel.getName();
            HashMap<String, String> voiceRoles = mapVoiceRole.get(guild);
            if (voiceRoles == null) {
                voiceRoles = new HashMap<>();
            }

            String roleName = arg;
            if (!guildController.getGuild().getRolesByName(roleName, true).isEmpty()) {
                voiceRoles.put(channelName, roleName);
                mapVoiceRole.put(guild, voiceRoles);
                globalDao.saveVoiceRole(channelName, roleName, guild);
                messagesService.sendBotMessage(channel, "Le rôle " + roleName + " sera à présent affecté lors de la connection au salon vocal : " + channelName);
            } else {
                messagesService.sendBotMessage(channel, "Le rôle " + roleName + " n'existe pas !");
            }
        }

    }

    private void setWelcomeMessageForGuild(String originalMessage, Guild guild) {
        String text = originalMessage.split(SEPARATOR_ACTION_ARGS, 2)[1];
        welcomeMessageByGuild.put(guild, text);
        globalDao.saveWelcomeMessage(guild, text);
    }

    private void manageAddCustomReaction(Guild guild, MessageChannel channel, String arg) {
        String[] fullParams = arg.split(SEPARATOR_ACTION_ARGS, 2);
        String keyWord = fullParams[0];
        String reaction = fullParams[1];
        addCustomReaction(guild, channel, keyWord, reaction);
    }

    private void manageCadavreExquis(GuildController guildController, MessageChannel channel, String arg) {
        String[] params = arg.split(SEPARATOR_ACTION_ARGS, 2);
        EnumCadavreExquisParams param = EnumCadavreExquisParams.valueOf(params[0].toUpperCase());
        executeCadavreActions(guildController, channel, params, param);
    }

    private void activateBot(MessageChannel channel, Guild guild) {
        if(!isBotActivated(guild)) {
            activateOrDeactivateBotForGuild(guild, true);
            messagesService.sendBotMessage(channel, "Le bot est à présent activé !");
        }else{
            messagesService.sendBotMessage(channel, "Le bot est déjà activé !");
        }
    }

    private void deactivateBot(MessageChannel channel, Guild guild) {
        if(isBotActivated(guild)) {
            activateOrDeactivateBotForGuild(guild, false);

            String prefixCmd = getPrefixCmdForGuild(guild);

            String prefixTrimed = prefixCmd.replace("[", "").replace("]", "");
            messagesService.sendBotMessage(channel, "Le bot est à présent désactivé !\r\nPour le réactiver tapez \"" + prefixTrimed + "activate\" (commande administrateur)");
        }else{
            messagesService.sendBotMessage(channel, "Le bot est déjà désactivé !");
        }
    }

    private void listRolesForGuild(MessageChannel channel, Guild guild) {
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
    }

    private void activateOrDeactivateBotForGuild(Guild guild, boolean activate) {
        botActivationByGuild.put(guild, activate);
        globalDao.saveActivationState(guild, activate);
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

    private void executeCustomReaction(Member member, Message message, GuildController guildController, MessageChannel channel, String arg, String action) {
        Guild guild = guildController.getGuild();
        CustomReaction customReaction = mapCustomReactions.get(guild).get(action);
        String[] params = (arg == null || arg.isEmpty()) ? new String[0] : arg.trim().split(SEPARATOR_ACTION_ARGS + "+");

        if (params.length != customReaction.getNumberOfParams()) {
            messagesService.sendBotMessage(channel, "Le nombre d'argument n'est pas le bon ! Try again !");
        } else {
            if ("princesse".equals(action)) {
                List<Role> rolesPrincesse = guild.getRolesByName("princesse", true);
                if (rolesPrincesse.isEmpty()) {
                    messagesService.sendBotMessage(channel, "Le rôle de princesse n'existe pas ici, dommage =(");
                    return;
                } else {
                    Role rolePrincesse = rolesPrincesse.get(0);
                    if (guild.getMembersWithRoles(rolesPrincesse).contains(member)) {
                        sendFormattedCustomReactionAndDeleteCommand(message, guildController, channel, customReaction, params);
                        return;
                    } else {
                        messagesService.sendBotMessage(channel, "Non, toi t'es pas une princesse. Au boulot ! Va farmer tes médailles !");
                        return;
                    }
                }
            }
            sendFormattedCustomReactionAndDeleteCommand(message, guildController, channel, customReaction, params);
        }
    }

    private void sendFormattedCustomReactionAndDeleteCommand(Message message, GuildController guildController, MessageChannel channel, CustomReaction customReaction,
            String[] params) {
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

        channel.deleteMessageById(message.getId()).complete();
        messagesService.sendBotMessage(channel, reactionReplaced);
    }

    private void addCustomReaction(Guild guild, MessageChannel channel, String keyWord, String reaction) {

        if (mapCustomReactions == null) {
            mapCustomReactions = new HashMap<>();
        }

        for (EnumAction action : EnumAction.values()) {
            if (keyWord.equalsIgnoreCase(action.name())) {
                messagesService.sendBotMessage(channel, "La commande " + keyWord + " existe déjà dans les commandes natives de Deathbot. Merci d'utiliser un autre mot-clé.");
                return;
            }
        }

        CustomReaction customReaction = new CustomReaction();
        customReaction.setReaction(reaction);
        customReaction.setNumberOfParams(reaction.length() - reaction.replace("$", "").length());

        HashMap<String, CustomReaction> mapCustomReactionsForGuild = mapCustomReactions.get(guild);
        if (mapCustomReactionsForGuild == null) {
            mapCustomReactionsForGuild = new HashMap<>();
        }
        mapCustomReactionsForGuild.put(keyWord, customReaction);
        mapCustomReactions.put(guild, mapCustomReactionsForGuild);

        globalDao.saveCustomReaction(keyWord, customReaction, guild);

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

    private void listAssignableRanks(GuildController guildController, User author, MessageChannel channel, String commandeComplete) {
        Set<Role> selfAssignableRanks = new HashSet<>(getSelfAssignableRanksForGuild(guildController));
        Set<Role> notSingleSelfAssignableRanksForGuild = getNotSingleSelfAssignableRanksForGuild(guildController);
        if (selfAssignableRanks != null && notSingleSelfAssignableRanksForGuild != null) {
            selfAssignableRanks.addAll(notSingleSelfAssignableRanksForGuild);
        }

        if (selfAssignableRanks != null && !selfAssignableRanks.isEmpty()) {

            StringBuilder sb = new StringBuilder();

            sb.append("Les ranks assignables sont : \r\n");

            for (Role role : sortSetOfRolesToList(selfAssignableRanks)) {
                sb.append(role.getName());
                sb.append(RETOUR_LIGNE);
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

    private List<Role> sortSetOfRolesToList(Set<Role> selfAssignableRanks) {
        List<Role> selfAssignableRanksList = new ArrayList<>(selfAssignableRanks);

        selfAssignableRanksList.sort(Comparator.comparing(Role::getName)
                .thenComparingInt(e -> e.getName().contains("Chevalier ") ?
                        Integer.parseInt(e.getName().replace("Chevalier ", "")) :
                        e.getPosition()));

        return selfAssignableRanksList;
    }

    private void removeAssignableRanks(User author, MessageChannel channel, GuildController guildController, String commandeComplete, String arg) {
        String[] rolesToRemoveTable = arg.split(ROLES_SEPARATOR);

        Set<Role> listRolesToRemove = createListOfRoleFromStringTable(rolesToRemoveTable, guildController, channel);

        Guild guild = guildController.getGuild();
        Set<Role> selfAssignableRanks = getSelfAssignableRanksForGuild(guildController);

        if (selfAssignableRanks != null && !selfAssignableRanks.isEmpty()) {
            selfAssignableRanks.removeAll(listRolesToRemove);
        } else {
            selfAssignableRanks = new HashSet<>();
        }

        setAssignableRanksForGuild(guild, selfAssignableRanks);
        sendMessageOfRemovedRanks(author, channel, commandeComplete, listRolesToRemove);
    }

    private void removeNotSingleAssignableRanks(User author, MessageChannel channel, GuildController guildController, String commandeComplete, String arg) {
        String[] rolesToRemoveTable = arg.split(ROLES_SEPARATOR);

        Set<Role> listRolesToRemove = createListOfRoleFromStringTable(rolesToRemoveTable, guildController, channel);

        Guild guild = guildController.getGuild();
        Set<Role> selfAssignableRanks = getNotSingleSelfAssignableRanksForGuild(guildController);

        if (selfAssignableRanks != null && !selfAssignableRanks.isEmpty()) {
            selfAssignableRanks.removeAll(listRolesToRemove);
        } else {
            selfAssignableRanks = new HashSet<>();
        }

        setNotSingleAssignableRanksForGuild(guild, selfAssignableRanks);
        sendMessageOfRemovedRanks(author, channel, commandeComplete, listRolesToRemove);
    }

    private void sendMessageOfRemovedRanks(User author, MessageChannel channel, String commandeComplete, Set<Role> listRolesToRemove) {
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

    private Set<Role> getSelfAssignableRanksForGuild(GuildController guildController) {

        Guild guild = guildController.getGuild();

        if (selfAssignableRanksByGuild.isEmpty() && !selfAssignableRanksByIdsByGuild.isEmpty()) {
            selfAssignableRanksByGuild = transformRanksIdsToObjects(guildController, selfAssignableRanksByIdsByGuild);
        }

        return (selfAssignableRanksByGuild != null && !selfAssignableRanksByGuild.isEmpty()) ? selfAssignableRanksByGuild.get(guild) : new HashSet<>();
    }

    private Set<Role> getNotSingleSelfAssignableRanksForGuild(GuildController guildController) {

        Guild guild = guildController.getGuild();

        if(notSingleSelfAssignableRanksByGuild.isEmpty() && !notSingleSelfAssignableRanksByIdsByGuild.isEmpty()){
            notSingleSelfAssignableRanksByGuild = transformRanksIdsToObjects(guildController, notSingleSelfAssignableRanksByIdsByGuild);
        }

        return (notSingleSelfAssignableRanksByGuild != null && !notSingleSelfAssignableRanksByGuild.isEmpty()) ? notSingleSelfAssignableRanksByGuild.get(guild) : new HashSet<>();
    }

    private HashMap<String, CustomReaction> getCustomReactionsForGuild(GuildController guildController) {
        Guild guild = guildController.getGuild();

        if (mapCustomReactions.isEmpty() && !mapCustomReactionsByGuild.isEmpty()) {
            mapCustomReactions = transformCustomReactionsGuildNamesToGuild(guildController, mapCustomReactionsByGuild);
        }
        return (mapCustomReactions != null && !mapCustomReactions.isEmpty()) ? mapCustomReactions.get(guild) : new HashMap<>();
    }

    private HashMap<String, String> getVoiceRolesForGuild(GuildController guildController) {
        Guild guild = guildController.getGuild();

        if (mapVoiceRole.isEmpty() && !mapVoiceRoleByGuild.isEmpty()) {
            mapVoiceRole = transformVoiceRolesGuildNamesToGuild(guildController, mapVoiceRoleByGuild);
        }
        return (mapVoiceRole != null && !mapVoiceRole.isEmpty()) ? mapVoiceRole.get(guild) : new HashMap<>();
    }

    private HashMap<Guild, HashMap<String, String>> transformVoiceRolesGuildNamesToGuild(GuildController guildController,
            HashMap<String, HashMap<String, String>> mapVoiceRoleByGuild) {
        HashMap<Guild, HashMap<String, String>> resultMap = new HashMap<>();

        for (String guildName : mapVoiceRoleByGuild.keySet()) {
            Guild guild = guildController.getJDA().getGuildsByName(guildName, true).get(0);

            HashMap<String, String> voiceRolesMap = mapVoiceRoleByGuild.get(guildName);

            resultMap.put(guild, voiceRolesMap);
        }
        return resultMap;
    }

    private HashMap<Guild, HashMap<String, CustomReaction>> transformCustomReactionsGuildNamesToGuild(GuildController guildController,
            HashMap<String, HashMap<String, CustomReaction>> mapCustomReactionsByGuild) {
        HashMap<Guild, HashMap<String, CustomReaction>> resultMap = new HashMap<>();

        for (String guildName : mapCustomReactionsByGuild.keySet()) {
            Guild guild = guildController.getJDA().getGuildsByName(guildName, true).get(0);

            HashMap<String, CustomReaction> customReactionMap = mapCustomReactionsByGuild.get(guildName);

            resultMap.put(guild, customReactionMap);
        }
        return resultMap;
    }

    private HashMap<Guild, Set<Role>> transformRanksIdsToObjects(GuildController guildController, HashMap<String, Set<String>> ranksByIdsByGuild) {

        HashMap<Guild, Set<Role>> resultMap = new HashMap<>();

        for (String guildName : ranksByIdsByGuild.keySet()) {
            List<Guild> guilds = guildController.getJDA().getGuildsByName(guildName, true);
            if (!guilds.isEmpty()) {
                Guild guild = guildController.getJDA().getGuildsByName(guildName, true).get(0);

                Set<String> roleNames = ranksByIdsByGuild.get(guildName);
                Set<Role> roles = new HashSet<>();
                for (String roleName : roleNames) {
                    Role role = guildController.getGuild().getRolesByName(roleName, true).get(0);
                    roles.add(role);
                }

                resultMap.put(guild, roles);
            }
        }

        return resultMap;
    }

    private void addAssignableRanks(User author, MessageChannel channel, GuildController guildController, String commandeComplete, String arg) {
        String[] rolesToAddTable = arg.split(ROLES_SEPARATOR);
        Guild guild = guildController.getGuild();

        Set<Role> listRolesToAdd = createListOfRoleFromStringTable(rolesToAddTable, guildController, channel);
        Set<Role> selfAssignableRanks = getSelfAssignableRanksForGuild(guildController);

        if (selfAssignableRanks != null && !selfAssignableRanks.isEmpty()) {
            selfAssignableRanks.addAll(listRolesToAdd);
        } else {
            selfAssignableRanks = new HashSet<>(listRolesToAdd);
        }
        setAssignableRanksForGuild(guild, selfAssignableRanks);

        if (!listRolesToAdd.isEmpty()) {
            StringBuilder sb = buildNewlyAssignableRolesListMessage(author, commandeComplete, listRolesToAdd);
            messagesService.sendBotMessage(channel, sb.toString());
        }
    }

    private void addNotSingleAssignableRanks(User author, MessageChannel channel, GuildController guildController, String commandeComplete, String arg) {
        String[] rolesToAddTable = arg.split(ROLES_SEPARATOR);
        Guild guild = guildController.getGuild();

        Set<Role> listRolesToAdd = createListOfRoleFromStringTable(rolesToAddTable, guildController, channel);
        Set<Role> selfAssignableRanks = getNotSingleSelfAssignableRanksForGuild(guildController);

        if (selfAssignableRanks != null && !selfAssignableRanks.isEmpty()) {
            selfAssignableRanks.addAll(listRolesToAdd);
        } else {
            selfAssignableRanks = new HashSet<>(listRolesToAdd);
        }
        setNotSingleAssignableRanksForGuild(guild, selfAssignableRanks);

        if (!listRolesToAdd.isEmpty()) {
            StringBuilder sb = buildNewlyAssignableRolesListMessage(author, commandeComplete, listRolesToAdd);
            messagesService.sendBotMessage(channel, sb.toString());
        }
    }

    private void setNotSingleAssignableRanksForGuild(Guild guild, Set<Role> selfAssignableRanks) {
        if(notSingleSelfAssignableRanksByGuild == null){
            notSingleSelfAssignableRanksByGuild = new HashMap<>();
        }
        notSingleSelfAssignableRanksByGuild.put(guild, selfAssignableRanks);
        globalDao.saveAssignableRanksForGuild(guild, selfAssignableRanks, false);
    }

    private StringBuilder buildNewlyAssignableRolesListMessage(User author, String commandeComplete, Set<Role> listRolesToAdd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Les rôles suivants sont désormais assignables : \r\n");

        for (Role role : sortSetOfRolesToList(listRolesToAdd)) {
            sb.append(role.getName()).append("\r\n");
        }

        System.out.println("Commande " + commandeComplete + " lancée par " + author.getName() + " : " + sb.toString());
        return sb;
    }

    private void setAssignableRanksForGuild(Guild guild, Set<Role> selfAssignableRanks) {
        if (selfAssignableRanksByGuild == null) {
            selfAssignableRanksByGuild = new HashMap<>();
        }
        selfAssignableRanksByGuild.put(guild, selfAssignableRanks);
        globalDao.saveAssignableRanksForGuild(guild, selfAssignableRanks, true);

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
        messageBuilder.append(prefixTrimed + "helpdb", BLOCK).append(" : Afficher cette aide.").append(RETOUR_LIGNE);
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
                roleToAdd = guildController.getGuild().getRolesByName(roleToAddString, true).get(0);
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
        List<Role> userAssignableRoles = findAssignableRole(userRoles, guildController);

        if ("".equals(rankToAdd)) {
            listRanksOfUser(channel, member);
        } else {
            addOrRemoveRankToUser(author, channel, guildController, rankToAdd, member, userRoles, userAssignableRoles);
        }
    }

    private void addOrRemoveRankToUser(User author, MessageChannel channel, GuildController guildController, String rankToAdd, Member member, List<Role> userRoles,
            List<Role> userAssignableRoles) {
        List<Role> potentialRolesToAdd = guildController.getGuild().getRolesByName(rankToAdd, true);
        if (!potentialRolesToAdd.isEmpty()) {
            Role roleToAdd = potentialRolesToAdd.get(0);

            Set<Role> selfAssignableRanks = getSelfAssignableRanksForGuild(guildController);
            Set<Role> notSingleSelfAssignableRanks = getNotSingleSelfAssignableRanksForGuild(guildController);

            boolean isSingle = selfAssignableRanks != null && selfAssignableRanks.contains(roleToAdd);
            boolean isNotSingle = notSingleSelfAssignableRanks != null && notSingleSelfAssignableRanks.contains(roleToAdd);

            if (isSingle || isNotSingle) {
                StringBuilder messageBuilder = new StringBuilder();
                if (!userRoles.contains(roleToAdd)) {
                    member = addRankToUser(guildController, member, userAssignableRoles, roleToAdd, messageBuilder, isSingle);
                } else {
                    member = removeRankToUser(guildController, member, roleToAdd, messageBuilder);
                }
                messagesService.sendBotMessageWithMention(channel, messageBuilder.toString(), author);

                // listRanksOfUser(channel, member);
                logRolesOfMember(member);

            } else {
                messagesService.sendBotMessage(channel,
                        "Le rôle **" + rankToAdd + "** n'est pas assignable à soi-même.\r\nMerci de contacter un admin ou un modérateur pour vous l'ajouter.");
            }
        } else {
            Pattern pattern = Pattern.compile("^Chevalier.*[^0]+$", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(rankToAdd).find()) {
                String kl = rankToAdd.split(SEPARATOR_ACTION_ARGS)[1];
                String chevalier = rankToAdd.split(SEPARATOR_ACTION_ARGS)[0];
                if (kl != null && Integer.valueOf(kl) > 10) {
                    rankToAdd = chevalier + " " + kl.substring(0, kl.length() - 1) + "0";
                } else {
                    rankToAdd = chevalier + " 1";
                }
                messagesService.sendBotMessage(channel, "Les rôles 'Chevalier' vont de 10 en 10 !\r\nComme je suis sympa, j'ai corrigé votre commande par `?rank " + rankToAdd
                        + "`.\r\nLa prochaine fois, pensez à arrondir à la dizaine inférieure ;) ");
                addOrRemoveRankToUser(author, channel, guildController, rankToAdd, member, userRoles, userAssignableRoles);
            } else {
                messagesService.sendBotMessage(channel, "Le rôle **" + rankToAdd + "** n'existe pas.");
            }
        }
    }

    private Member removeRankToUser(GuildController guildController, Member member, Role roleToAdd, StringBuilder messageBuilder) {
        guildController.removeSingleRoleFromMember(member, roleToAdd).complete();
        member = guildController.getGuild().getMember(member.getUser());
        messageBuilder.append("Vous n'êtes plus **").append(roleToAdd.getName()).append("** !");
        return member;
    }

    private Member addRankToUser(GuildController guildController, Member member, List<Role> userAssignableRoles, Role roleToAdd, StringBuilder messageBuilder, boolean isSingle) {

        if (isSingle && getSingleRoleForGuild(guildController.getGuild()) && !userAssignableRoles.isEmpty()) {
            guildController.removeRolesFromMember(member, userAssignableRoles).complete();
            member = guildController.getGuild().getMember(member.getUser());
        }

        guildController.addSingleRoleToMember(member, roleToAdd).complete();
        member = guildController.getGuild().getMember(member.getUser());
        messageBuilder.append("Vous êtes passé **").append(roleToAdd.getName()).append("** !");
        return member;
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

    private List<Role> findAssignableRole(List<Role> userRoles, GuildController guildController) {

        Set<Role> selfAssignableRanks = new HashSet<>(getSelfAssignableRanksForGuild(guildController));

        return userRoles.stream().filter(userRole -> selfAssignableRanks != null && selfAssignableRanks.contains(userRole)).collect(Collectors.toList());
    }

}
