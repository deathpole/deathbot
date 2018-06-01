package net.deathpole.deathbot.Services.Impl;

import static net.dv8tion.jda.core.MessageBuilder.Formatting.BLOCK;
import static net.dv8tion.jda.core.MessageBuilder.Formatting.BOLD;
import static net.dv8tion.jda.core.MessageBuilder.Formatting.ITALICS;
import static net.dv8tion.jda.core.MessageBuilder.Formatting.UNDERLINE;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import net.deathpole.deathbot.Bot;
import net.deathpole.deathbot.CustomReactionDTO;
import net.deathpole.deathbot.ReminderDTO;
import net.deathpole.deathbot.Dao.IGlobalDao;
import net.deathpole.deathbot.Dao.Impl.GlobalDao;
import net.deathpole.deathbot.Enums.EnumAction;
import net.deathpole.deathbot.Enums.EnumCadavreExquisParams;
import net.deathpole.deathbot.Enums.EnumDynoAction;
import net.deathpole.deathbot.Services.ICommandesService;
import net.deathpole.deathbot.Services.IHelperService;
import net.deathpole.deathbot.Services.IMessagesService;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.managers.GuildController;

/**
 *
 */
public class CommandesServiceImpl implements ICommandesService {

    private static final int NB_REMINDER_PARAMS = 4;
    private static final String PREFIX_TAG = "@";
    private static final String ACTION_ARGS_SEPARATOR = " ";
    private static final String ROLES_SEPARATOR = ",";
    private static final String PARAMETERS_SEPARATOR = ";;";
    private static final String REMINDER_SEPARATOR = ";";
    private static final String RETOUR_LIGNE = "\r\n";
    private static final int DEFAULT_INACTIVITY_LIMIT = 7;
    private IGlobalDao globalDao;
    private IMessagesService messagesService = new MessagesServiceImpl();
    private IHelperService helperService = new HelperServiceImpl();
    private List<String> dynoActions = initDynoActions();

    // Maps used after load
    private HashMap<Guild, Set<Role>> notSingleSelfAssignableRanksByGuild = new HashMap<>();
    private HashMap<Guild, Set<Role>> onJoinRanksByGuild = new HashMap<>();
    private HashMap<Guild, Set<Role>> selfAssignableRanksByGuild = new HashMap<>();
    private HashMap<Guild, String> prefixCmdByGuild = new HashMap<>();
    private HashMap<Guild, HashMap<String, CustomReactionDTO>> mapCustomReactions = new HashMap<>();
    private HashMap<Guild, HashMap<String, String>> mapVoiceRole = new HashMap<>();
    private HashMap<Guild, String> welcomeMessageByGuild = new HashMap<>();
    private HashMap<Guild, Boolean> singleRoleByGuild = new HashMap<>();
    private HashMap<Guild, Boolean> botActivationByGuild = new HashMap<>();

    // Maps used on DB load
    private HashMap<String, Set<String>> notSingleSelfAssignableRanksByIdsByGuild = initNotSingleAssignableRanksbyGuild();
    private HashMap<String, Set<String>> onJoinRanksByIdsByGuild = initOnJoinRanksbyGuild();

    private HashMap<String, Set<String>> selfAssignableRanksByIdsByGuild = initAssignableRanksbyGuild();
    private HashMap<String, HashMap<String, CustomReactionDTO>> mapCustomReactionsByGuild = initMapCustomReactions();
    private HashMap<String, HashMap<String, String>> mapVoiceRoleByGuild = initMapVoiceRoles();

    private List<Member> listCadavreSujet;
    private List<String> listCadavreAction;
    private List<String> listCadavreComplement;
    private List<String> listCadavreAdjectif;

    private static List<String> initDynoActions() {

        List<String> tempDynoActions = new ArrayList<>();

        for (EnumDynoAction action : EnumDynoAction.values()) {
            tempDynoActions.add(action.name());
        }
        return tempDynoActions;
    }

    private HashMap<String, Set<String>> initAssignableRanksbyGuild() {
        if (globalDao == null) {
            globalDao = new GlobalDao();
        }
        return globalDao.initAssignableRanksbyGuild(true);
    }

    private HashMap<String, Set<String>> initNotSingleAssignableRanksbyGuild() {
        if (globalDao == null) {
            globalDao = new GlobalDao();
        }
        return globalDao.initAssignableRanksbyGuild(false);
    }

    private HashMap<String, Set<String>> initOnJoinRanksbyGuild() {
        if (globalDao == null) {
            globalDao = new GlobalDao();
        }
        return globalDao.initOnJoinRanksbyGuild();
    }

    private HashMap<String, HashMap<String, CustomReactionDTO>> initMapCustomReactions() {
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

    @Override
    public void executeAction(MessageReceivedEvent e) {

        GuildController guildController = new GuildController(e.getGuild());
        ChannelType channelType = e.getChannelType();
        if (!ChannelType.PRIVATE.equals(channelType)) {
            Role adminRole = guildController.getGuild().getRolesByName("Admin", true).get(0);
            Role modoRole = guildController.getGuild().getRolesByName("Moderateur", true).get(0);
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
                            commandeComplete = msg.split(prefixCmd, 2)[1];

                            if (commandeComplete != null && !commandeComplete.isEmpty()) {

                                String[] cmdSplit = commandeComplete.split(ACTION_ARGS_SEPARATOR, 2);

                                String[] args = new String[0];
                                String action = null;
                                if (cmdSplit.length > 0) {
                                    action = cmdSplit[0];
                                    if (cmdSplit.length > 1) {
                                        args = cmdSplit[1].split(ACTION_ARGS_SEPARATOR, 1);
                                    }
                                }

                                try {
                                    assert action != null;
                                    if (isBotActivated(guildController.getGuild()) || EnumAction.ACTIVATE.name().equals(action.toUpperCase())) {
                                        if (!dynoActions.contains(action.toUpperCase())) {

                                            HashMap<String, CustomReactionDTO> customReactionsForGuild = getCustomReactionsForGuild(guildController);
                                            List<String> dbActions = new ArrayList<>();

                                            for (EnumAction dbAction : EnumAction.values()) {
                                                dbActions.add(dbAction.name());
                                            }

                                            List<String> customReactions = new ArrayList<>();

                                            for (String customReaction : customReactionsForGuild.keySet()) {
                                                customReactions.add(customReaction);
                                            }

                                            if (lookForSimilarCommandInList(prefixTrimed, channel, action, dynoActions)
                                                    || lookForSimilarCommandInList(prefixTrimed, channel, action, customReactions)
                                                    || lookForSimilarCommandInList(prefixTrimed, channel, action, dbActions)) {
                                                return;
                                            }

                                            if (customReactionsForGuild != null && customReactionsForGuild.keySet().contains(action)) {
                                                String param = null;
                                                if (args.length >= 1) {
                                                    param = args[0];
                                                }
                                                executeCustomReaction(member, e.getMessage(), guildController, channel, param, action);
                                            } else {
                                                List<TextChannel> modChannels = guildController.getGuild().getTextChannelsByName("logs-moderation", true);
                                                MessageChannel modChannel = modChannels.isEmpty() ? null : modChannels.get(0);
                                                executeEmbeddedAction(guildController, adminRole, modoRole, author, member, channel, commandeComplete, args, action, msg,
                                                        e.getMessage(), modChannel);
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

    private boolean lookForSimilarCommandInList(String prefixCmd, MessageChannel channel, String action, List<String> listToSearch) {
        for (String dynoAction : listToSearch) {
            if ((dynoAction + "s").equalsIgnoreCase(action)) {
                messagesService.sendBotMessage(channel, "Cette commande n'existe pas. Pensiez-vous à " + prefixCmd + "" + dynoAction.toLowerCase() + "  ?");
                return true;
            }
        }
        return false;
    }

    private void sendPrivateMessage(User user, String content) {

        user.openPrivateChannel().complete().sendMessage(content).complete();
        // user.openPrivateChannel().queue((channel) -> channel.sendMessage(content).queue());
    }

    @Override
    public void userJoinedGuild(GuildMemberJoinEvent e) {
        Member connectedMember = e.getMember();
        User user = connectedMember.getUser();
        Guild guild = e.getGuild();
        sendWelcomeMessageIfExists(user, guild);
        addOnJoinRankToUserIfExists(connectedMember, guild);
    }

    private void addOnJoinRankToUserIfExists(Member member, Guild guild) {
        if (!onJoinRanksByGuild.isEmpty() && onJoinRanksByGuild.containsKey(guild)) {
            Set<Role> onJoinRanks = onJoinRanksByGuild.get(guild);
            for (Role onJoinRank : onJoinRanks) {

                System.out.println("Guild is : " + guild);
                System.out.println("GuildController is : " + guild.getController());
                System.out.println("Member is : " + member);
                System.out.println("Rank is : " + onJoinRank);

                guild.getController().addSingleRoleToMember(member, onJoinRank).complete();
            }
        }
    }

    private void sendWelcomeMessageIfExists(User user, Guild guild) {
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

    private void executeEmbeddedAction(GuildController guildController, Role adminRole, Role modoRole, User author, Member member, MessageChannel channel, String commandeComplete,
            String[] args, String action, String originalMessage, Message message, MessageChannel modChannel) {

        EnumAction actionEnum = EnumAction.fromValue(action);
        boolean isAdmin = member.getRoles().contains(adminRole);
        boolean isModo = member.getRoles().contains(modoRole);
        Guild guild = guildController.getGuild();

        switch (actionEnum) {
        case SET_WELCOME_MESSAGE:
        case SWM:
            if (isAdmin) {
                setWelcomeMessageForGuild(originalMessage, guild);
                messagesService.sendBotMessage(channel, "Le message privé de bienvenue a bien été mis à jour.");
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case DEACTIVATE:
            if (isAdmin) {
                deactivateBot(channel, guild);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case LARO:
            if (isAdmin || isModo) {
                listRolesForGuild(channel, guild);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case ACTIVATE:
            if (isAdmin) {
                activateBot(channel, guild);

            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case WITHOUT:
            // if (isAdmin || isModo) {
                searchUsersWithoutRole(guildController, channel, args[0]);
            // } else {
            // messagesService.sendMessageNotEnoughRights(channel);
            // }
            break;
        case WITH:
            // if (isAdmin || isModo) {
                searchUsersWithRole(guildController, channel, args[0]);
            // } else {
            // messagesService.sendMessageNotEnoughRights(channel);
            // }
            break;
        // case CADAVRE:
        // if (isAdmin) {
        // manageCadavreExquis(guildController, channel, args[0]);
        // } else {
        // messagesService.sendMessageNotEnoughRights(channel);
        // }
        // break;
        // case AIDE:
        // channel.sendMessage(buildHelpMessage(guild)).queue();
        // break;
        case CHANGE:
            if (isAdmin) {
                changePrefixe(author, channel, commandeComplete, args[0], guild);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case ACR:
            if (isAdmin || isModo) {
                manageAddCustomReaction(guild, channel, args[0]);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case DCR:
            if (isAdmin) {
                manageDeleteCustomReaction(guild, channel, args[0]);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case LCR:
            listCustomReactionsForGuild(guild, channel);
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
        // case LIST_ONJOIN_RANKS:
        // case LOJR:
        // if (isModo || isAdmin) {
        // listOnJoinRanks(guildController, author, channel, commandeComplete);
        // } else {
        // messagesService.sendMessageNotEnoughRights(channel);
        // }
        // break;
        case ADD_RANK:
        case AR:
            if (isAdmin || isModo) {
                addAssignableRanks(author, channel, guildController, commandeComplete, args[0]);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case ADD_NOTSINGLE_RANK:
        case ANSR:
            if (isAdmin || isModo) {
                addNotSingleAssignableRanks(author, channel, guildController, commandeComplete, args[0]);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        // case ADD_ONJOIN_RANK:
        // case AOJR:
        // if (isAdmin || isModo) {
        // addOnJoinRank(author, channel, guildController, commandeComplete, args[0]);
        // } else {
        // messagesService.sendMessageNotEnoughRights(channel);
        // }
        // break;
        case ADD_GLOBAL_RANK:
        case AGR:
            if (isAdmin) {
                addGlobalRank(author, channel, guildController, commandeComplete, args[0], adminRole);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case REMOVE_GLOBAL_RANK:
        case RGR:
            if (isAdmin) {
                removeGlobalRank(author, channel, guildController, commandeComplete, args[0], adminRole);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        // case ADD_REMINDER:
        // case AREM:
        // if (isAdmin) {
        // addReminder(channel, guildController, args);
        // } else {
        // messagesService.sendMessageNotEnoughRights(channel);
        // }
        // break;
        // case REMOVE_REMINDER:
        // case RREM:
        // if (isAdmin) {
        // removeReminder(channel, guildController, args[0]);
        // } else {
        // messagesService.sendMessageNotEnoughRights(channel);
        // }
        // break;
        case TIME:
            if (isAdmin || isModo) {
                LocalDateTime now = LocalDateTime.now();

                ZonedDateTime nowZoned = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Europe/Paris"));
                DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT);

                messagesService.sendBotMessage(channel, "Heure et date en France : " + nowZoned.format(dtf));
                messagesService.sendBotMessage(channel, "Heure et date du serveur Deathbot : " + now.format(dtf));
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        // case LIST_REMINDERS:
        // case LREM:
        // if (isAdmin) {
        // listReminders(channel, guildController);
        // } else {
        // messagesService.sendMessageNotEnoughRights(channel);
        // }
        // break;
        case ADD_VOICE_ROLE:
        case AVR:
            if (isAdmin) {
                addVoiceRole(guild, member, author, channel, guildController, commandeComplete, args[0]);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case REMOVE_RANK:
        case RR:
            if (isAdmin) {
                removeAssignableRanks(author, channel, guildController, commandeComplete, args[0]);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case PUNISH:
            if (isAdmin || isModo) {
                punishUser(channel, guildController, args[0], message, isAdmin, modChannel);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        case REMOVE_NOTSINGLE_RANK:
        case RNSR:
            if (isAdmin) {
                removeNotSingleAssignableRanks(author, channel, guildController, commandeComplete, args[0]);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        // case REMOVE_ONJOIN_RANK:
        // case ROJR:
        // if (isAdmin) {
        // removeOnJoinRank(author, channel, guildController, commandeComplete, args[0]);
        // } else {
        // messagesService.sendMessageNotEnoughRights(channel);
        // }
        // break;
        case CHEVALIER:
            messagesService.sendBotMessage(channel, "La commande correcte est ?rank Chevalier XXX. Comme je suis sympa je l'ai corrigée pour vous !");
            String[] temp = {"Chevalier", args[0]};
            args = temp;
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
            calculateSRandMedals(channel, args);
            break;
        case INACTIVITY:
            if (isAdmin || isModo) {
                System.out.println("DeathbotExecution : Commande non prise en charge");
                listInactiveMember(channel, guildController, args[0]);
            } else {
                messagesService.sendMessageNotEnoughRights(channel);
            }
            break;
        default:
            System.out.println("DeathbotExecution : Commande non prise en charge");
            break;
        }
    }

    private void listInactiveMember(MessageChannel channel, GuildController guildController, String paramStr) {
        System.out.println("Commande ?inactivity lancée.");

        Guild guild = guildController.getGuild();
        int limit;
        String specChannel = null;

        if (!paramStr.isEmpty() && paramStr != null) {
            String[] params = paramStr.split(ACTION_ARGS_SEPARATOR);

            if (params.length == 2) {
                specChannel = params[1];
            }
            String limitStr = params[0];
            limit = Integer.parseInt(limitStr);
        } else {
            limit = DEFAULT_INACTIVITY_LIMIT;
        }

        OffsetDateTime limitDateTime = OffsetDateTime.now().minusDays(limit);

        System.out.println("---> Paramètres récupérés : " + specChannel + ", " + limit);

        List<Member> allMembers = guild.getMembers();
        System.out.println("---> Membres récupérés : " + allMembers.size());

        List<Member> inactiveMembers = new ArrayList<>();


        if (specChannel == null) {

            inactiveMembers.addAll(allMembers);

            List<TextChannel> allChannels = guild.getTextChannels();
            System.out.println("---> Channels récupérés : " + allChannels.size());

            for (TextChannel curChannel : allChannels) {
                List<Member> curMembers = curChannel.getMembers();
                System.out.println("---> Membres récupérés pour le channel : " + curChannel.getName() + " = " + curMembers.size());

                for (Member curMember : curMembers) {
                    if (curMember.getUser().isBot()) {
                        inactiveMembers.remove(curMember);
                        System.out.println("Le membre " + curMember.getEffectiveName() + " est un bot, ignoré.");
                        continue;
                    }
                    if (!getMessagesByUserAfterLimit(curChannel, curMember.getUser(), limitDateTime).isEmpty()) {
                        inactiveMembers.remove(curMember);
                        System.out.println("Le membre " + curMember.getEffectiveName() + " n'est pas inactif.");
                        continue;
                    }
                }
            }
        } else {
            List<TextChannel> specChannels = guild.getTextChannelsByName(specChannel, true);
            System.out.println("---> Channels spécifiques récupérés : " + specChannels.size());

            if (specChannels.isEmpty() || specChannels == null) {
                messagesService.sendBotMessage(channel, "Channel inconnu ! =(");
                return;
            }

            TextChannel specChannelObject = specChannels.get(0);
            List<Member> curMembers = specChannelObject.getMembers();
            inactiveMembers.addAll(curMembers);

            System.out.println("---> Membres récupérés pour le channel : " + specChannelObject.getName() + " = " + curMembers.size());

            for (Member curMember : curMembers) {
                if (curMember.getUser().isBot()) {
                    inactiveMembers.remove(curMember);
                    System.out.println("Le membre " + curMember.getEffectiveName() + " est un bot, ignoré.");
                    continue;
                } else {
                    if (!getMessagesByUserAfterLimit(specChannelObject, curMember.getUser(), limitDateTime).isEmpty()) {
                        inactiveMembers.remove(curMember);
                        System.out.println("Le membre " + curMember.getEffectiveName() + " n'est pas inactif.");
                        continue;
                    }
                }
            }
        }

        if (!inactiveMembers.isEmpty()) {
            StringBuilder sb = new StringBuilder("Les membres actuellement inactifs depuis plus de " + limit + " jours sont : " + RETOUR_LIGNE);
            for (Member member : inactiveMembers) {
                sb.append(member.getEffectiveName()).append(RETOUR_LIGNE);
            }

            messagesService.sendBotMessage(channel, sb.toString());
        } else {
            messagesService.sendBotMessage(channel, "Aucun membre inactif ! Pas mal !");
        }

    }

    private List<Message> getMessagesByUserAfterLimit(MessageChannel channel, User user, OffsetDateTime limitDateTime) {
        return channel.getIterableHistory().stream().filter(m -> m.getAuthor().equals(user)).filter(m -> m.getCreationTime().isAfter(limitDateTime)).collect(Collectors.toList());
    }

    private void punishUser(MessageChannel channel, GuildController guildController, String arg, Message message, boolean isAdmin, MessageChannel modChannel) {

        MessageChannel actualChannel = modChannel != null ? modChannel : channel;

        String[] args;
        String[] parameters;

        String action;
        String user;
        String duration = null;
        String comment;

        args = arg.split(ACTION_ARGS_SEPARATOR, 2);
        action = args[0];
        String parametersString = args[1];

        if ("mute".equals(action.trim()) || "ban".equals(action.trim())) {
            parameters = parametersString.split(PARAMETERS_SEPARATOR, 3);
            if (parameters.length < 3) {
                messagesService.sendBotMessage(actualChannel, "Il manque des paramètres !");
                return;
            }

            user = StringUtils.trim(parameters[0]);
            duration = StringUtils.trim(parameters[1]);
            comment = StringUtils.trim(parameters[2]);

        } else {
            parameters = parametersString.split(PARAMETERS_SEPARATOR, 2);
            if (parameters.length < 2) {
                messagesService.sendBotMessage(actualChannel, "Il manque des paramètres !");
                return;
            }

            user = StringUtils.trim(parameters[0]);
            comment = StringUtils.trim(parameters[1]);

        }
        Role adminRole = guildController.getGuild().getRolesByName("Admin", true).get(0);
        Role modoRole = guildController.getGuild().getRolesByName("Moderateur", true).get(0);

        Member memberToPerformActionOn;

        try {
            memberToPerformActionOn = guildController.getGuild().getMembersByEffectiveName(user.replace("@", ""), true).get(0);

            if (memberToPerformActionOn.getRoles().contains(adminRole) || memberToPerformActionOn.getRoles().contains(modoRole)) {
                messagesService.sendBotMessage(channel, "Hin hin, ce mec, c'est presque Dieu... On lui crache pas au visage ;)");
                return;
            }

            switch (action) {
            case "mute":
                muteUser(duration, comment, guildController, memberToPerformActionOn, actualChannel, message, channel);
                break;
            case "kick":
                kickUser(comment, guildController, memberToPerformActionOn, actualChannel, message, channel);
                break;
            case "warn":
                warnUser(comment, memberToPerformActionOn, actualChannel, message, channel);
                break;
            case "ban":
                if (isAdmin) {
                    banUser(duration, comment, guildController, memberToPerformActionOn, actualChannel, message, channel);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
            case "unban":
                if (isAdmin) {
                    unbanUser(comment, guildController, user, message, actualChannel, channel);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
            case "unmute":
                unmuteUser(comment, guildController, memberToPerformActionOn, message, actualChannel, channel);
                break;
            }
        } catch (IndexOutOfBoundsException e) {
            messagesService.sendBotMessage(actualChannel, "L'utilisateur " + user + " n'a pas été trouvé ! =(");
            return;
        }
    }

    private void banUser(String duration, String comment, GuildController guildController, Member memberToBan, MessageChannel channel, Message message,
            MessageChannel originChannel) {
        if (memberToBan != null) {
            guildController.ban(memberToBan, 0, comment).complete();
            StringBuilder sb = new StringBuilder("L'utilisateur **" + memberToBan.getEffectiveName() + "** a été banni");

            if (!"".equals(duration)) {
                try {
                    long durationLong = Long.parseLong(duration);
                    if (durationLong != 0L) {
                        sb.append(" pour une période de " + durationLong + " minutes");
                        guildController.unban(memberToBan.getUser()).queueAfter(durationLong, TimeUnit.MINUTES);
                    }
                } catch (NumberFormatException e) {

                }
            }

            sb.append(" !");
            messagesService.sendBotMessage(originChannel, sb.toString());
            messagesService.sendBotMessage(channel, sb.toString() + " Raison : " + comment);
            sendPrivateMessage(memberToBan.getUser(), "Vous avez été banni ! " + RETOUR_LIGNE + "La raison : " + comment);

            originChannel.deleteMessageById(message.getId()).complete();
        }
    }

    private void warnUser(String comment, Member memberToWarn, MessageChannel channel, Message message, MessageChannel originChannel) {
        if (memberToWarn != null) {
            StringBuilder sb = new StringBuilder("L'utilisateur **" + memberToWarn.getEffectiveName() + "** a été averti !");
            messagesService.sendBotMessage(originChannel, sb.toString());
            messagesService.sendBotMessage(channel, sb.toString() + " Raison : " + comment);
            sendPrivateMessage(memberToWarn.getUser(), "Ceci est un avertissement ! " + RETOUR_LIGNE + "La raison : " + comment);

            originChannel.deleteMessageById(message.getId()).complete();
        }
    }

    private void kickUser(String comment, GuildController guildController, Member memberToKick, MessageChannel channel, Message message, MessageChannel originChannel) {
        if (memberToKick != null) {
            guildController.kick(memberToKick, comment).complete();

            StringBuilder sb = new StringBuilder("L'utilisateur **" + memberToKick.getEffectiveName() + "** a été kické !");
            messagesService.sendBotMessage(originChannel, sb.toString());
            messagesService.sendBotMessage(channel, sb.toString() + " Raison : " + comment);
            sendPrivateMessage(memberToKick.getUser(), "Vous avez été kické ! " + RETOUR_LIGNE + "La raison : " + comment);

            originChannel.deleteMessageById(message.getId()).complete();
        }
    }

    private void muteUser(String duration, String comment, GuildController guildController, Member memberToMute, MessageChannel channel, Message message,
            MessageChannel originChannel) {
        if (memberToMute != null) {
            guildController.setMute(memberToMute, true).complete();

            List<Role> muted = guildController.getGuild().getRolesByName("Muted", true);
            if (!muted.isEmpty()) {
                Role mutedRole = muted.get(0);
                guildController.addSingleRoleToMember(memberToMute, mutedRole).complete();
                StringBuilder sb = new StringBuilder("L'utilisateur **" + memberToMute.getEffectiveName() + "** a été muté");
                if (!"".equals(duration)) {
                    try {
                        long durationLong = Long.parseLong(duration);

                        sb.append(" pour une période de " + durationLong + " minutes");

                        guildController.removeSingleRoleFromMember(memberToMute, mutedRole).queueAfter(durationLong, TimeUnit.MINUTES);
                    } catch (NumberFormatException e) {

                    }
                    sb.append(" !");
                    messagesService.sendBotMessage(originChannel, sb.toString());
                    messagesService.sendBotMessage(channel, sb.toString() + " Raison : " + comment);
                }

                sendPrivateMessage(memberToMute.getUser(), "Vous avez été muté ! " + RETOUR_LIGNE + "La raison : " + comment);
                originChannel.deleteMessageById(message.getId()).complete();
            }
        }

    }

    private void unbanUser(String comment, GuildController guildController, String user, Message message, MessageChannel channel, MessageChannel originChannel) {
        if (user != null) {
            User userObject = guildController.getJDA().getUsersByName(user, true).get(0);
            guildController.unban(userObject);

            StringBuilder sb = new StringBuilder("L'utilisateur **" + userObject.getName() + "** été \"dé-banni\" !");
            messagesService.sendBotMessage(originChannel, sb.toString());
            messagesService.sendBotMessage(channel, sb.toString() + " Raison : " + comment);
            sendPrivateMessage(userObject, "Vous avez été dé-banni ! " + RETOUR_LIGNE + "La raison : " + comment);

            originChannel.deleteMessageById(message.getId()).complete();
        }
    }

    private void unmuteUser(String comment, GuildController guildController, Member member, Message message, MessageChannel channel, MessageChannel originChannel) {
        if (member != null) {
            List<Role> muted = guildController.getGuild().getRolesByName("Muted", true);
            if (!muted.isEmpty()) {
                Role mutedRole = muted.get(0);
                guildController.setMute(member, false);
                guildController.removeSingleRoleFromMember(member, mutedRole).complete();

                StringBuilder sb = new StringBuilder("L'utilisateur **" + member.getEffectiveName() + "** été \"dé-muté\" !");
                messagesService.sendBotMessage(originChannel, sb.toString());
                messagesService.sendBotMessage(channel, sb.toString() + " Raison : " + comment);
                sendPrivateMessage(member.getUser(), "Vous avez été dé-muté ! " + RETOUR_LIGNE + "La raison : " + comment);
                originChannel.deleteMessageById(message.getId()).complete();
            }
        }
    }

    private void removeGlobalRank(User author, MessageChannel channel, GuildController guildController, String commandeComplete, String arg, Role adminRole) {
        String[] rolesToRemoveTable = arg.split(ROLES_SEPARATOR);

        Set<Role> listRolesToRemove = createListOfRoleFromStringTable(rolesToRemoveTable, guildController, channel);

        for (Member member : guildController.getGuild().getMembers()) {
            if (member.getRoles().contains(adminRole) || member.getUser().isBot()) {
                continue;
            }
            for (Role roleToAdd : listRolesToRemove) {
                guildController.removeSingleRoleFromMember(member, roleToAdd).complete();
            }
        }

        if (!listRolesToRemove.isEmpty()) {
            StringBuilder sb = buildRemoveGlobalRanksMessage(author, commandeComplete, listRolesToRemove);
            messagesService.sendBotMessage(channel, sb.toString());
        }
    }

    private void addGlobalRank(User author, MessageChannel channel, GuildController guildController, String commandeComplete, String arg, Role adminRole) {
        String[] rolesToAddTable = arg.split(ROLES_SEPARATOR);

        Set<Role> listRolesToAdd = createListOfRoleFromStringTable(rolesToAddTable, guildController, channel);

        for (Member member : guildController.getGuild().getMembers()) {
            if (member.getRoles().contains(adminRole) || member.getUser().isBot()) {
                continue;
            }

            for (Role roleToAdd : listRolesToAdd) {
                guildController.addSingleRoleToMember(member, roleToAdd).complete();
            }
        }

        if (!listRolesToAdd.isEmpty()) {
            StringBuilder sb = buildAddGlobalRanksMessage(author, commandeComplete, listRolesToAdd);
            messagesService.sendBotMessage(channel, sb.toString());
        }
    }

    private StringBuilder buildAddGlobalRanksMessage(User author, String commandeComplete, Set<Role> listRolesToAdd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Les rôles suivants ont été assignés à tous les membres du serveur (sauf Admin) : " + RETOUR_LIGNE);

        for (Role role : sortSetOfRolesToList(listRolesToAdd)) {
            sb.append(role.getName()).append(RETOUR_LIGNE);
        }

        System.out.println("DeathbotExecution : Commande " + commandeComplete + " lancée par " + author.getName() + " : " + sb.toString());
        return sb;
    }

    private StringBuilder buildRemoveGlobalRanksMessage(User author, String commandeComplete, Set<Role> listRolesToAdd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Les rôles suivants ont été retirés de tous les membres du serveur (sauf Admin) : " + RETOUR_LIGNE);

        for (Role role : sortSetOfRolesToList(listRolesToAdd)) {
            sb.append(role.getName()).append(RETOUR_LIGNE);
        }

        System.out.println("DeathbotExecution : Commande " + commandeComplete + " lancée par " + author.getName() + " : " + sb.toString());
        return sb;
    }

    private void listOnJoinRanks(GuildController guildController, User author, MessageChannel channel, String commandeComplete) {
        Set<Role> onJoinRanks = new HashSet<>(getOnJoinRanksForGuild(guildController));

        if (onJoinRanks != null && !onJoinRanks.isEmpty()) {

            StringBuilder sb = new StringBuilder();

            sb.append("Les ranks assignés à la connexion sont : " + RETOUR_LIGNE);

            for (Role role : sortSetOfRolesToList(onJoinRanks)) {
                sb.append(role.getName());
                sb.append(RETOUR_LIGNE);
            }
            messagesService.sendBotMessage(channel, sb.toString());
            String message1 = sb.toString();
            System.out.println("DeathbotExecution : Commande " + commandeComplete + " lancée par " + author.getName() + " : " + message1);
        } else {
            String message1 = "Aucun rank assigné à la connexion pour le moment. ";
            System.out.println("DeathbotExecution : Commande " + commandeComplete + " lancée par " + author.getName() + " : " + message1);
            messagesService.sendBotMessage(channel, message1);
        }
    }

    private void removeOnJoinRank(User author, MessageChannel channel, GuildController guildController, String commandeComplete, String arg) {
        String[] rolesToRemoveTable = arg.split(ROLES_SEPARATOR);

        Set<Role> listRolesToRemove = createListOfRoleFromStringTable(rolesToRemoveTable, guildController, channel);

        Guild guild = guildController.getGuild();
        Set<Role> onJoinRanks = getOnJoinRanksForGuild(guildController);

        if (onJoinRanks != null && !onJoinRanks.isEmpty()) {
            onJoinRanks.removeAll(listRolesToRemove);
        } else {
            onJoinRanks = new HashSet<>();
        }

        setOnJoinRanksForGuild(guild, onJoinRanks);
        sendMessageOfRemovedOnJoinRanks(author, channel, commandeComplete, listRolesToRemove);
    }

    private void sendMessageOfRemovedOnJoinRanks(User author, MessageChannel channel, String commandeComplete, Set<Role> listRolesToRemove) {
        if (!listRolesToRemove.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Les rôles suivants ne sont désormais plus assignés à la connexion : " + RETOUR_LIGNE);
            for (Role role : listRolesToRemove) {
                sb.append(role.getName()).append(RETOUR_LIGNE);
            }

            System.out.println("DeathbotExecution : Commande " + commandeComplete + " lancée par " + author.getName() + " : " + sb.toString());
            messagesService.sendBotMessage(channel, sb.toString());
        }
    }

    private void addOnJoinRank(User author, MessageChannel channel, GuildController guildController, String commandeComplete, String arg) {
        String[] rolesToAddTable = arg.split(ROLES_SEPARATOR);
        Guild guild = guildController.getGuild();

        Set<Role> listRolesToAdd = createListOfRoleFromStringTable(rolesToAddTable, guildController, channel);
        Set<Role> onJoinRanks = getOnJoinRanksForGuild(guildController);

        if (onJoinRanks != null && !onJoinRanks.isEmpty()) {
            onJoinRanks.addAll(listRolesToAdd);
        } else {
            onJoinRanks = new HashSet<>(listRolesToAdd);
        }
        setOnJoinRanksForGuild(guild, onJoinRanks);

        if (!listRolesToAdd.isEmpty()) {
            StringBuilder sb = buildNewOnJoinRankMessage(author, commandeComplete, listRolesToAdd);
            messagesService.sendBotMessage(channel, sb.toString());
        }
    }

    private StringBuilder buildNewOnJoinRankMessage(User author, String commandeComplete, Set<Role> listRolesToAdd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Les rôles suivants seront désormais assignés lorsqu'un nouveau membre rejoint le serveur : " + RETOUR_LIGNE);

        for (Role role : sortSetOfRolesToList(listRolesToAdd)) {
            sb.append(role.getName()).append(RETOUR_LIGNE);
        }

        System.out.println("DeathbotExecution : Commande " + commandeComplete + " lancée par " + author.getName() + " : " + sb.toString());
        return sb;
    }

    private void setOnJoinRanksForGuild(Guild guild, Set<Role> onJoinRanks) {
        if (onJoinRanksByGuild == null) {
            onJoinRanksByGuild = new HashMap<>();
        }
        onJoinRanksByGuild.put(guild, onJoinRanks);
        globalDao.saveOnJoinRanksForGuild(guild, onJoinRanks);
        // globalDao.saveAssignableRanksForGuild(guild, onJoinRanks, false);
    }

    private Set<Role> getOnJoinRanksForGuild(GuildController guildController) {
        Guild guild = guildController.getGuild();

        if (onJoinRanksByGuild.isEmpty() && !onJoinRanksByIdsByGuild.isEmpty()) {
            onJoinRanksByGuild = transformRanksIdsToObjects(guildController, onJoinRanksByIdsByGuild);
        }

        return (onJoinRanksByGuild != null && !onJoinRanksByGuild.isEmpty()) ? onJoinRanksByGuild.get(guild) : new HashSet<>();
    }

    private void listCustomReactionsForGuild(Guild guild, MessageChannel channel) {
        HashMap<String, CustomReactionDTO> customReactions = mapCustomReactions.get(guild);

        if (customReactions != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Liste des commandes customs : ").append(RETOUR_LIGNE);

            for (String key : customReactions.keySet()) {
                sb.append(key).append(RETOUR_LIGNE);
            }

            messagesService.sendBotMessage(channel, sb.toString());
        } else {
            messagesService.sendBotMessage(channel, "Aucune commande custom trouvée...");
        }
    }

    private void calculateSRandMedals(MessageChannel channel, String[] args) {

        if (args.length < 1) {
            StringBuilder sb = new StringBuilder("Aide de la commande ?revive : " + RETOUR_LIGNE);
            sb.append("```?revive <STAGE_ATTEINT> <BONUS_DE_MEDAILLES> (<TEMPS_DE_RUN_EN_MIN>) :").append(RETOUR_LIGNE);
            sb.append("\t- Si le temps de run N'EST PAS renseigné : Permet de connaître le nombre de médailles gagnées lors de la résurrection.").append(RETOUR_LIGNE);
            sb.append("\t- Si le temps EST renseigné : Permet de connaître les médailles par minute que génère ce run.```");

            messagesService.sendNormalBotMessage(channel, sb.toString());
            return;
        }

        String[] splittedArgs = args[0].split(" ");

        Integer stage = Integer.valueOf(splittedArgs[0]);

        if (stage % 10 != 0) {
            stage = BigDecimal.valueOf(stage).setScale(-1, BigDecimal.ROUND_HALF_DOWN).intValue();
        }

        Integer medalBonus = Integer.valueOf(splittedArgs[1]);

        BigDecimal medalGain = globalDao.getMedalGainForStage(stage);
        BigDecimal stageGain = medalGain.multiply(BigDecimal.valueOf(medalBonus).divide(new BigDecimal(100), BigDecimal.ROUND_HALF_DOWN));
        String formattedGain = helperService.formatBigNumbersToEFFormat(stageGain);

        if (splittedArgs.length > 2) {
            Integer runDuration = Integer.valueOf(splittedArgs[2]);
            BigDecimal SR = stageGain.divide(BigDecimal.valueOf(runDuration), BigDecimal.ROUND_HALF_DOWN);
            String formattedSR = helperService.formatBigNumbersToEFFormat(SR) + "/min";
            messagesService.sendBotMessage(channel, "SR au stage " + stage + " : " + formattedSR);
        } else {
            messagesService.sendBotMessage(channel, "Gain immédiat au stage " + stage + " : " + formattedGain);
        }
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
        String text = originalMessage.split(ACTION_ARGS_SEPARATOR, 2)[1];
        welcomeMessageByGuild.put(guild, text);
        globalDao.saveWelcomeMessage(guild, text);
    }

    private void manageAddCustomReaction(Guild guild, MessageChannel channel, String arg) {
        String[] fullParams = arg.split(ACTION_ARGS_SEPARATOR, 2);
        String keyWord = fullParams[0];
        String reaction = fullParams[1];
        addCustomReaction(guild, channel, keyWord, reaction);
    }

    private void manageDeleteCustomReaction(Guild guild, MessageChannel channel, String arg) {
        String[] fullParams = arg.split(ACTION_ARGS_SEPARATOR, 1);
        String keyWord = fullParams[0];
        deleteCustomReaction(guild, channel, keyWord);
    }

    @Deprecated
    private void manageCadavreExquis(GuildController guildController, MessageChannel channel, String arg) {
        String[] params = arg.split(ACTION_ARGS_SEPARATOR, 2);
        EnumCadavreExquisParams param = EnumCadavreExquisParams.valueOf(params[0].toUpperCase());
        executeCadavreActions(guildController, channel, params, param);
    }

    private void activateBot(MessageChannel channel, Guild guild) {
        if (!isBotActivated(guild)) {
            activateOrDeactivateBotForGuild(guild, true);
            messagesService.sendBotMessage(channel, "Le bot est à présent activé !");
        } else {
            messagesService.sendBotMessage(channel, "Le bot est déjà activé !");
        }
    }

    private void deactivateBot(MessageChannel channel, Guild guild) {
        if (isBotActivated(guild)) {
            activateOrDeactivateBotForGuild(guild, false);

            String prefixCmd = getPrefixCmdForGuild(guild);

            String prefixTrimed = prefixCmd.replace("[", "").replace("]", "");
            messagesService.sendBotMessage(channel,
                    "Le bot est à présent désactivé !" + RETOUR_LIGNE + "Pour le réactiver tapez \"" + prefixTrimed + "activate\" (commande administrateur)");
        } else {
            messagesService.sendBotMessage(channel, "Le bot est déjà désactivé !");
        }
    }

    private void listRolesForGuild(MessageChannel channel, Guild guild) {
        StringBuilder sb = new StringBuilder();
        List<Role> allRoles = guild.getRoles();

        if (allRoles != null && !allRoles.isEmpty()) {
            sb.append("Les roles du serveur sont : ").append(RETOUR_LIGNE).append(RETOUR_LIGNE);
            allRoles.stream().filter(role -> !role.getName().startsWith("@everyone")).forEach(
                    role -> sb.append(role.getName()).append(" (").append(guild.getMembersWithRoles(role).size()).append(" membres)").append(RETOUR_LIGNE));
        } else {
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
                    sb.append(memberWithRole.getEffectiveName()).append("" + RETOUR_LIGNE);
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
            sb.append("Les membres suivants n'ont pas le rôle **").append(seekRole.getName()).append("** : " + RETOUR_LIGNE);

            for (Member memberWithoutRole : membersWithoutRole) {
                sb.append(memberWithoutRole.getEffectiveName()).append(RETOUR_LIGNE);
            }

            messagesService.sendBotMessage(channel, sb.toString());

        } else {
            messagesService.sendBotMessage(channel, "Le rôle " + roleToSearch + " est inconnu !");
        }
    }

    @Deprecated
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

            String sb = ACTION_ARGS_SEPARATOR + listCadavreAction.get(new Random().nextInt(listCadavreAction.size())) + ACTION_ARGS_SEPARATOR
                    + listCadavreComplement.get(new Random().nextInt(listCadavreComplement.size())) + ACTION_ARGS_SEPARATOR
                    + listCadavreAdjectif.get(new Random().nextInt(listCadavreAdjectif.size()));

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
        CustomReactionDTO customReaction = mapCustomReactions.get(guild).get(action);
        String[] params = (arg == null || arg.isEmpty()) ? new String[0] : arg.trim().split(ACTION_ARGS_SEPARATOR + "+");

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
            if ("aide".equals(action) || "mod".equals(action) || "admin".equals(action)) {
                sendFormattedCustomReaction(message, guildController, channel, customReaction, params);
            } else {
                sendFormattedCustomReactionAndDeleteCommand(message, guildController, channel, customReaction, params);
            }
        }
    }

    private void sendFormattedCustomReactionAndDeleteCommand(Message message, GuildController guildController, MessageChannel channel, CustomReactionDTO customReaction,
            String[] params) {
        sendCustomReaction(guildController, channel, customReaction, params);
        channel.deleteMessageById(message.getId()).complete();
    }

    private void sendCustomReaction(GuildController guildController, MessageChannel channel, CustomReactionDTO customReaction, String[] params) {
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

    private void sendFormattedCustomReaction(Message message, GuildController guildController, MessageChannel channel, CustomReactionDTO customReaction, String[] params) {
        sendCustomReaction(guildController, channel, customReaction, params);
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

        CustomReactionDTO customReaction = new CustomReactionDTO();
        customReaction.setReaction(reaction);
        customReaction.setNumberOfParams(reaction.length() - reaction.replace("$", "").length());

        HashMap<String, CustomReactionDTO> mapCustomReactionsForGuild = mapCustomReactions.get(guild);
        if (mapCustomReactionsForGuild == null) {
            mapCustomReactionsForGuild = new HashMap<>();
        }
        mapCustomReactionsForGuild.put(keyWord, customReaction);
        mapCustomReactions.put(guild, mapCustomReactionsForGuild);

        globalDao.saveCustomReaction(keyWord, customReaction, guild);

        messagesService.sendBotMessage(channel, "Nouvelle réponse ajoutée pour la commande : " + keyWord);
    }

    private void deleteCustomReaction(Guild guild, MessageChannel channel, String keyWord) {
        HashMap<String, CustomReactionDTO> mapCustomReactionsForGuild = mapCustomReactions.get(guild);
        if (mapCustomReactionsForGuild != null) {
            CustomReactionDTO customReaction = mapCustomReactionsForGuild.remove(keyWord);
            if (customReaction != null) {
                boolean deleted = globalDao.deleteCustomReaction(keyWord, guild);
                if (!deleted) {
                    messagesService.sendBotMessage(channel, "La commande custom \"" + keyWord + "\" n'a pas pu être supprimée. Merci de contacter l'administrateur.");
                    return;
                }
                messagesService.sendBotMessage(channel, "La commande custom \"" + keyWord + "\" a été supprimée !");
            } else {
                messagesService.sendBotMessage(channel, "Aucune commande custom trouvée pour le mot-clé : " + keyWord);
            }
        } else {
            messagesService.sendBotMessage(channel, "Aucune commande custom trouvée pour votre serveur !");
        }
    }

    private void sudoMakeMeASandwich(boolean contains, MessageChannel channel, String arg) {
        if ("make me a sandwich".equals(arg)) {
            if (contains) {
                messagesService.sendBotMessage(channel, "Here you go, good Lord !");
            } else {
                messagesService.sendBotMessage(channel, "No way, you're not even Admin !");
            }
        } else {
            messagesService.sendBotMessage(channel, "¯\\_(ツ)_/¯ Apprends à taper sur un clavier...");
        }
    }

    private void makeMeASandwich(MessageChannel channel, String arg) {
        if ("me a sandwich".equals(arg)) {
            messagesService.sendBotMessage(channel, "No, YOU make me a sandwich !");
        } else {
            messagesService.sendBotMessage(channel, "¯\\_(ツ)_/¯ Apprends à taper sur un clavier...");
        }
    }

    private void changePrefixe(User author, MessageChannel channel, String commandeComplete, String arg, Guild guild) {
        if (arg.trim().length() > 1) {
            String message1 = "Le prefixe de commande doit être un caractère unique. ";
            System.out.println("DeathbotExecution : Commande " + commandeComplete + " lancée par " + author.getName() + " : " + message1);
            messagesService.sendBotMessage(channel, message1);
        } else {
            String prefixCmd = "[" + arg + "]";
            setPrefixCmdForGuild(guild, prefixCmd);

            String message1 = "Prefixe de commande modifié pour \"" + arg + "\"";
            System.out.println("DeathbotExecution : Commande " + commandeComplete + " lancée par " + author.getName() + " : " + message1);
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

            sb.append("Les ranks assignables sont : " + RETOUR_LIGNE);

            for (Role role : sortSetOfRolesToList(selfAssignableRanks)) {
                sb.append(role.getName());
                sb.append(RETOUR_LIGNE);
            }
            messagesService.sendBotMessage(channel, sb.toString());
            String message1 = sb.toString();
            System.out.println("DeathbotExecution : Commande " + commandeComplete + " lancée par " + author.getName() + " : " + message1);
        } else {
            String message1 = "Aucun rank assignable pour le moment. ";
            System.out.println("DeathbotExecution : Commande " + commandeComplete + " lancée par " + author.getName() + " : " + message1);
            messagesService.sendBotMessage(channel, message1);
        }
    }

    private List<Role> sortSetOfRolesToList(Set<Role> selfAssignableRanks) {
        List<Role> selfAssignableRanksList = new ArrayList<>(selfAssignableRanks);

        selfAssignableRanksList.sort(Comparator.comparing(Role::getName).thenComparingInt(
                e -> e.getName().contains("Chevalier ") ? Integer.parseInt(e.getName().replace("Chevalier ", "")) : e.getPosition()));

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
            sb.append("Les rôles suivants ne sont désormais plus assignables : " + RETOUR_LIGNE);
            for (Role role : listRolesToRemove) {
                sb.append(role.getName()).append(RETOUR_LIGNE);
            }

            System.out.println("DeathbotExecution : Commande " + commandeComplete + " lancée par " + author.getName() + " : " + sb.toString());
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

        if (notSingleSelfAssignableRanksByGuild.isEmpty() && !notSingleSelfAssignableRanksByIdsByGuild.isEmpty()) {
            notSingleSelfAssignableRanksByGuild = transformRanksIdsToObjects(guildController, notSingleSelfAssignableRanksByIdsByGuild);
        }

        return (notSingleSelfAssignableRanksByGuild != null && !notSingleSelfAssignableRanksByGuild.isEmpty()) ? notSingleSelfAssignableRanksByGuild.get(guild) : new HashSet<>();
    }

    private HashMap<String, CustomReactionDTO> getCustomReactionsForGuild(GuildController guildController) {
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

    private HashMap<Guild, HashMap<String, CustomReactionDTO>> transformCustomReactionsGuildNamesToGuild(GuildController guildController,
            HashMap<String, HashMap<String, CustomReactionDTO>> mapCustomReactionsByGuild) {
        HashMap<Guild, HashMap<String, CustomReactionDTO>> resultMap = new HashMap<>();

        for (String guildName : mapCustomReactionsByGuild.keySet()) {
            Guild guild = guildController.getJDA().getGuildsByName(guildName, true).get(0);

            HashMap<String, CustomReactionDTO> customReactionMap = mapCustomReactionsByGuild.get(guildName);

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

    private void addReminder(MessageChannel channel, GuildController guildController, String[] arg) {

        if (arg.length == 0) {
            messagesService.sendBotMessage(channel, "Il manque des paramètres, désolé. Merci de contacter un administrateur ou de consulter l'aide (argument \"help\").");
            return;
        }

        String[] reminderParams = arg[0].split(REMINDER_SEPARATOR);

        if (reminderParams.length < NB_REMINDER_PARAMS) {
            if (reminderParams.length == 1 && "help".equals(reminderParams[0])) {
                messagesService.sendNormalBotMessage(channel, buildHelpAddReminder().toString());
                return;
            } else {
                messagesService.sendBotMessage(channel, "Il manque des paramètres, désolé. Merci de contacter un administrateur ou de consulter l'aide (argument \"help\").");
                return;
            }
        } else if (reminderParams.length == NB_REMINDER_PARAMS) {
            String title = reminderParams[0];
            String destinationChan = reminderParams[1];
            String reminderText = reminderParams[2];
            String cronTab = reminderParams[3];
            LocalDateTime nextExecutionTime = helperService.generateNextExecutionTime(cronTab, null);
            ReminderDTO reminder = new ReminderDTO(title, reminderText, destinationChan, cronTab, nextExecutionTime);

            Guild guild = guildController.getGuild();
            HashMap<String, ReminderDTO> reminders = Bot.reminderThreadsByGuild.get(guild.getName()).getReminders();
            reminders.put(reminder.getTitle(), reminder);

            globalDao.saveReminder(reminder, guild);
            messagesService.sendBotMessage(channel, "Nouveau reminder ajouté avec le titre \"" + title + "\" et la cronTab \"" + cronTab + "\"");
        } else {
            messagesService.sendBotMessage(channel, "Il y a trop de paramètres, désolé. Merci de contacter un administrateur ou de consulter l'aide (argument \"help\").");
            return;
        }
    }

    private StringBuilder buildHelpAddReminder() {
        StringBuilder sb = new StringBuilder("*** Syntaxe de la commande add_reminder : ***").append(RETOUR_LIGNE);
        sb.append("```?add_reminder <NOM_DU_REMINDER_A_CREER>;<SALON_DE_DESTINATION>;<TEXTE_A_AFFICHER>;<CRONTAB>```").append(RETOUR_LIGNE);
        sb.append(RETOUR_LIGNE);
        sb.append("**__Détails des paramètres__: **").append(RETOUR_LIGNE);
        sb.append(RETOUR_LIGNE);
        sb.append("*<NOM_DU_REMINDER_A_CREER>* : Un titre pour retrouver votre reminder facilement").append(RETOUR_LIGNE);
        sb.append("*<SALON_DE_DESTINATION>* : Le salon dans lequel le reminder sera affiché. :warning: Ne pas mettre le # :warning:").append(RETOUR_LIGNE);
        sb.append("*<TEXTE_A_AFFICHER>* : Le texte qui sera affiché. Il peut contenir des mentions (de membres spécifiques ou de rôles)").append(RETOUR_LIGNE);
        sb.append(RETOUR_LIGNE);
        sb.append("*<CRONTAB>* : La crontab d'exécution du reminder, au format mm HH DD MM YYYY. Elle supporte les \\* et les /").append(RETOUR_LIGNE);
        sb.append("*__Exemple de crontab__ :*").append(RETOUR_LIGNE);
        sb.append("0 9 * * * => L'exécution se fait tous les jours à 9h.").append(RETOUR_LIGNE);
        sb.append(RETOUR_LIGNE);
        sb.append("__**Exemple de création de reminder complet : **__").append(RETOUR_LIGNE);
        sb.append("?add_reminder ToT;annonces;C'est le début de la Tour, messieurs dames du @s3 !;0 9 */3 * *").append(RETOUR_LIGNE);
        sb.append("=> Tous les 3 jours à 9h, dans le salon #annonces, sera posté le message \"C'est le début de la Tour, messieurs dames du @s3 !\"").append(RETOUR_LIGNE);
        return sb;
    }

    private void removeReminder(MessageChannel channel, GuildController guildController, String arg) {

        String[] reminderParams = arg.split(REMINDER_SEPARATOR);

        if (reminderParams.length != 1) {
            messagesService.sendBotMessage(channel,
                    "Le nombre de paramètres n'est pas bon, désolé. Merci de contacter un administrateur ou de consulter l'aide (argument \"help\").");
            return;
        } else {
            if ("help".equals(reminderParams[0])) {
                StringBuilder sb = new StringBuilder("*** Syntaxe de la commande remove_reminder : ***").append(RETOUR_LIGNE);
                sb.append("```?remove_reminder <NOM_DU_REMINDER_A_SUPPRIMER>```").append(RETOUR_LIGNE);
                sb.append("*NB : Les noms peuvent être retrouvés grâce à la commande ?list_reminders*");

                messagesService.sendNormalBotMessage(channel, sb.toString());
                return;
            } else {
                String title = reminderParams[0];

                Guild guild = guildController.getGuild();
                HashMap<String, ReminderDTO> reminders = Bot.reminderThreadsByGuild.get(guild.getName()).getReminders();
                if (!reminders.isEmpty()) {
                    if (reminders.get(title) != null) {
                        reminders.remove(title);
                        Bot.reminderThreadsByGuild.get(guild.getName()).setReminders(reminders);
                        boolean deleted = globalDao.deleteRemindersForGuild(guild, title);
                        if (!deleted) {
                            messagesService.sendBotMessage(channel, "Le reminder \"" + title + "\" n'a pas pu être supprimée. Merci de contacter l'administrateur.");
                            return;
                        }
                        messagesService.sendBotMessage(channel, "Le reminder \"" + title + "\" a été supprimé !");
                    } else {
                        messagesService.sendBotMessage(channel, "Le reminder \"" + title + "\" n'a pas été trouvé ! :thinking:");
                    }
                } else {
                    messagesService.sendBotMessage(channel, "Aucun reminder n'a encore été défini ! :thinking:");
                }
            }
        }
    }

    private void listReminders(MessageChannel channel, GuildController guildController) {

        HashMap<String, ReminderDTO> reminders = Bot.reminderThreadsByGuild.get(guildController.getGuild().getName()).getReminders();

        if (reminders.isEmpty()) {
            messagesService.sendBotMessage(channel, "Aucun reminder mis en place pour le moment !");
            return;
        } else {

            StringBuilder sb = new StringBuilder();
            sb.append("Les reminders suivants sont actuellement actifs : ").append(RETOUR_LIGNE);
            for (ReminderDTO reminder : reminders.values()) {
                sb.append("\t - Nom : ").append(reminder.getTitle()).append(", Salon : ").append(reminder.getChan()).append(", Expression Cron : ").append(
                        reminder.getCronTab()).append(RETOUR_LIGNE);
            }

            messagesService.sendBotMessage(channel, sb.toString());
        }
    }

    private void setNotSingleAssignableRanksForGuild(Guild guild, Set<Role> selfAssignableRanks) {
        if (notSingleSelfAssignableRanksByGuild == null) {
            notSingleSelfAssignableRanksByGuild = new HashMap<>();
        }
        notSingleSelfAssignableRanksByGuild.put(guild, selfAssignableRanks);
        globalDao.saveAssignableRanksForGuild(guild, selfAssignableRanks, false);
    }

    private StringBuilder buildNewlyAssignableRolesListMessage(User author, String commandeComplete, Set<Role> listRolesToAdd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Les rôles suivants sont désormais assignables : " + RETOUR_LIGNE);

        for (Role role : sortSetOfRolesToList(listRolesToAdd)) {
            sb.append(role.getName()).append(RETOUR_LIGNE);
        }

        System.out.println("DeathbotExecution : Commande " + commandeComplete + " lancée par " + author.getName() + " : " + sb.toString());
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
                "', le nom de la commande et les valeurs possible (si besoin).").append(RETOUR_LIGNE);
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
                        "Le rôle **" + rankToAdd + "** n'est pas assignable à soi-même." + RETOUR_LIGNE + "Merci de contacter un admin ou un modérateur pour vous l'ajouter.");
            }
        } else {
            Pattern pattern = Pattern.compile("^Chevalier.*[^0]+$", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(rankToAdd).find()) {
                String kl = rankToAdd.split(ACTION_ARGS_SEPARATOR)[1];
                String chevalier = rankToAdd.split(ACTION_ARGS_SEPARATOR)[0];
                if (kl != null && Integer.valueOf(kl) > 10) {
                    rankToAdd = chevalier + " " + kl.substring(0, kl.length() - 1) + "0";
                } else {
                    rankToAdd = chevalier + " 1";
                }
                messagesService.sendBotMessage(channel, "Les rôles 'Chevalier' vont de 10 en 10 !" + RETOUR_LIGNE + "Comme je suis sympa, j'ai corrigé votre commande par `?rank "
                        + rankToAdd + "`." + RETOUR_LIGNE + "La prochaine fois, pensez à arrondir à la dizaine inférieure ;) ");
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
        sb.append("Le membre ").append(member.getEffectiveName()).append(" a désormais les rôles suivants : " + RETOUR_LIGNE);
        for (Role role : member.getRoles()) {
            sb.append(role.getName()).append(RETOUR_LIGNE);
        }
        System.out.print(sb.toString());
    }

    private void listRanksOfUser(MessageChannel channel, Member member) {
        StringBuilder sb = new StringBuilder();
        sb.append("Vous avez actuellement les rôles suivants : " + RETOUR_LIGNE);
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
