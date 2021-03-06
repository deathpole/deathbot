package net.deathpole.deathbot.Services.Impl;

import static net.dv8tion.jda.api.MessageBuilder.Formatting.BLOCK;
import static net.dv8tion.jda.api.MessageBuilder.Formatting.BOLD;
import static net.dv8tion.jda.api.MessageBuilder.Formatting.ITALICS;
import static net.dv8tion.jda.api.MessageBuilder.Formatting.UNDERLINE;

import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
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
import net.deathpole.deathbot.Dao.IGlobalDao;
import net.deathpole.deathbot.Dao.Impl.GlobalDao;
import net.deathpole.deathbot.Enums.EnumAction;
import net.deathpole.deathbot.Enums.EnumCadavreExquisParams;
import net.deathpole.deathbot.Enums.EnumDynoAction;
import net.deathpole.deathbot.PlayerStatDTO;
import net.deathpole.deathbot.ReminderDTO;
import net.deathpole.deathbot.Services.IChartService;
import net.deathpole.deathbot.Services.ICommandesService;
import net.deathpole.deathbot.Services.IHelperService;
import net.deathpole.deathbot.Services.IMessagesService;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 *
 */
public class CommandesServiceImpl implements ICommandesService {

    private static final int NB_REMINDER_PARAMS = 4;
    private static final String PREFIX_TAG = "@";
    private static final String ACTION_ARGS_SEPARATOR = "\\s+";
    private static final String SINGLE_SPACE = " ";
    private static final String ROLES_SEPARATOR = ",";
    private static final String PARAMETERS_SEPARATOR = ";;";
    private static final String REMINDER_SEPARATOR = ";";
    private static final String RETOUR_LIGNE = "\r\n";
    private static final int DEFAULT_INACTIVITY_LIMIT = 7;
    private static final String BALISE_BLOCK_CODE = "```";
    private static final String BALISE_CODE = "`";
    private static final String TAB = "\t";
    private static final String SPACE = " ";
    private IGlobalDao globalDao;
    private IMessagesService messagesService = new MessagesServiceImpl();
    private IHelperService helperService = new HelperServiceImpl();
    private IChartService chartService;
    private List<String> dynoActions = initDynoActions();

    // Maps used after load
    private HashMap<Guild, Set<Role>> notSingleSelfAssignableRanksByGuild = new HashMap<>();
    private HashMap<Guild, Set<Role>> onJoinRanksByGuild = new HashMap<>();
    private HashMap<Guild, HashMap<Role, Set<Role>>> linkedRolesByGuild = new HashMap<>();
    private HashMap<Guild, List<String>> mapDynoActions = new HashMap<>();
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
    private HashMap<String, HashMap<String, Set<String>>> linkedRanksByIdsByGuild = initLinkedRanksByGuild();

    private HashMap<String, HashMap<String, Set<String>>> initLinkedRanksByGuild() {
        if (globalDao == null) {
            globalDao = new GlobalDao();
            if (chartService == null) {
                chartService = new ChartServiceImpl(this.globalDao);
            }
        }
        return globalDao.initLinkedRanksbyGuild();
    }

    private HashMap<String, Set<String>> selfAssignableRanksByIdsByGuild = initAssignableRanksbyGuild();
    private HashMap<String, HashMap<String, CustomReactionDTO>> mapCustomReactionsByGuild = initMapCustomReactions();
    private HashMap<String, List<String>> mapDynoActionsByGuild = initMapDynoActions();
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
            if (chartService == null) {
                chartService = new ChartServiceImpl(this.globalDao);
            }
        }
        return globalDao.initAssignableRanksbyGuild(true);
    }

    private HashMap<String, Set<String>> initNotSingleAssignableRanksbyGuild() {
        if (globalDao == null) {
            globalDao = new GlobalDao();
            if (chartService == null) {
                chartService = new ChartServiceImpl(this.globalDao);
            }
        }
        return globalDao.initAssignableRanksbyGuild(false);
    }

    private HashMap<String, Set<String>> initOnJoinRanksbyGuild() {
        if (globalDao == null) {
            globalDao = new GlobalDao();
            if (chartService == null) {
                chartService = new ChartServiceImpl(this.globalDao);
            }
        }
        return globalDao.initOnJoinRanksbyGuild();
    }

    private HashMap<String, HashMap<String, CustomReactionDTO>> initMapCustomReactions() {
        if (globalDao == null) {
            globalDao = new GlobalDao();
            if (chartService == null) {
                chartService = new ChartServiceImpl(this.globalDao);
            }
        }
        return globalDao.initMapCustomReactions();
    }

    private HashMap<String, List<String>> initMapDynoActions() {
        if (globalDao == null) {
            globalDao = new GlobalDao();
            if (chartService == null) {
                chartService = new ChartServiceImpl(this.globalDao);
            }
        }
        return globalDao.initMapDynoActions();
    }

    private HashMap<String, HashMap<String, String>> initMapVoiceRoles() {
        if (globalDao == null) {
            globalDao = new GlobalDao();
            if (chartService == null) {
                chartService = new ChartServiceImpl(this.globalDao);
            }
        }
        return globalDao.initMapVoiceRoles();
    }

    @Override
    public void executeAction(MessageReceivedEvent e) {

        Guild guild = e.getGuild();
        ChannelType channelType = e.getChannelType();
        if (!ChannelType.PRIVATE.equals(channelType)) {
            List<Role> adminRoles = guild.getRolesByName("Admin", true);
            Role adminRole = null;
            if (adminRoles != null && !adminRoles.isEmpty()) {
                adminRole = adminRoles.get(0);
            }
            List<Role> moderateurRoles = guild.getRolesByName("Moderateur", true);
            Role modoRole = null;
            if (moderateurRoles != null && !moderateurRoles.isEmpty()) {
                modoRole = moderateurRoles.get(0);
            }
            User author = e.getAuthor();

            String prefixCmd = getPrefixCmdForGuild(guild);

            if (!author.isBot()) {
                Member member = e.getMember();
                Message message = e.getMessage();
                MessageChannel channel = e.getChannel();
                String msg = message.getContentRaw();
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
                                    if (isBotActivated(guild) || EnumAction.ACTIVATE.name().equals(action.toUpperCase())) {
                                        List<String> dynoActionsForGuild = getDynoActionsForGuild(guild);
                                        if (!(dynoActions.contains(action.toUpperCase()) || (dynoActionsForGuild != null && dynoActionsForGuild.contains(
                                                action.toUpperCase().toLowerCase())))) {

                                            HashMap<String, CustomReactionDTO> customReactionsForGuild = getCustomReactionsForGuild(guild);
                                            List<String> dbActions = new ArrayList<>();

                                            for (EnumAction dbAction : EnumAction.values()) {
                                                dbActions.add(dbAction.name());
                                            }

                                            List<String> customReactions = new ArrayList<>();

                                            if (customReactionsForGuild != null) {
                                                for (String customReaction : customReactionsForGuild.keySet()) {
                                                    customReactions.add(customReaction);
                                                }
                                            }

                                            if (lookForSimilarCommandInList(prefixTrimed, channel, action, dynoActions)
                                                    || lookForSimilarCommandInList(prefixTrimed, channel, action, customReactions)
                                                    || lookForSimilarCommandInList(prefixTrimed, channel, action, dynoActionsForGuild)
                                                    || lookForSimilarCommandInList(prefixTrimed, channel, action, dbActions)) {
                                                return;
                                            }

                                            if (customReactionsForGuild != null && customReactionsForGuild.keySet().contains(action)) {
                                                String param = null;
                                                if (args.length >= 1) {
                                                    param = args[0];
                                                }
                                                executeCustomReaction(member, e.getMessage(), guild, channel, param, action, adminRole, modoRole);
                                            } else {
                                                List<TextChannel> modChannels = guild.getTextChannelsByName("logs-moderation", true);
                                                MessageChannel modChannel = modChannels.isEmpty() ? null : modChannels.get(0);
                                                executeEmbeddedAction(guild, adminRole, modoRole, author, member, channel, commandeComplete, args, action, msg, e.getMessage(),
                                                        modChannel);
                                            }
                                        }
                                    }
                                } catch (IllegalArgumentException ex) {
                                    messagesService.sendBotMessage(channel, "Commande inconnue.");
                                    System.out.println("Exception : " + ex);
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
        if (listToSearch != null && !listToSearch.isEmpty()) {
            for (String dynoAction : listToSearch) {
                if ((dynoAction + "s").equalsIgnoreCase(action)) {
                    messagesService.sendBotMessage(channel, "Cette commande n'existe pas. Pensiez-vous à " + prefixCmd + "" + dynoAction.toLowerCase() + "  ?");
                    return true;
                }
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
                System.out.println("Member is : " + member);
                System.out.println("Rank is : " + onJoinRank);

                guild.addRoleToMember(member, onJoinRank).complete();
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

        HashMap<String, String> voiceRoles = getVoiceRolesForGuild(guild);

        if (voiceRoles.keySet().contains(channel.getName())) {
            String rankToAdd = voiceRoles.get(channel.getName());
            List<Role> potentialRolesToAdd = guild.getRolesByName(rankToAdd, true);

            if (!potentialRolesToAdd.isEmpty()) {
                guild.addRoleToMember(connectedMember, potentialRolesToAdd.get(0)).complete();
            }
        }
    }

    @Override
    public void userLeftVoiceChannel(VoiceChannel channel, Member connectedMember, Guild guild) {
        // User user = connectedMember.getUser();
        // Récupérer la liste des rôles/channel

        HashMap<String, String> voiceRoles = getVoiceRolesForGuild(guild);

        if (voiceRoles.keySet().contains(channel.getName())) {
            String rankToAdd = voiceRoles.get(channel.getName());
            List<Role> potentialRolesToDelete = guild.getRolesByName(rankToAdd, true);

            if (!potentialRolesToDelete.isEmpty()) {
                guild.removeRoleFromMember(connectedMember, potentialRolesToDelete.get(0)).complete();
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

    public static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        String positive = String.format("%d heure(s) et %02d minute(s)", absSeconds / 3600, (absSeconds % 3600) / 60);
        return seconds < 0 ? "-" + positive : positive;
    }

    private void executeEmbeddedAction(Guild guild, Role adminRole, Role modoRole, User author, Member member, MessageChannel channel, String commandeComplete, String[] args,
            String action, String originalMessage, Message message, MessageChannel modChannel) {

        EnumAction actionEnum = EnumAction.fromValue(action);
        boolean isAdmin = adminRole != null ? member.getRoles().contains(adminRole) : member.isOwner();
        boolean isModo = modoRole != null ? member.getRoles().contains(modoRole) : member.isOwner();
        boolean isCmds = "cmds".equals(channel.getName());
        boolean isStatsCmds = "stats-cmds".equals(channel.getName());

        switch (actionEnum) {
            case STATUS:
                RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
                long uptime = rb.getUptime();
                messagesService.sendBotMessage(channel, "Le bot est lancé depuis " + helperService.convertMillisToDaysHoursMinSec(uptime));
                break;
            case ADD_RANK_TO_RANK:
                if (isAdmin) {
                    addRankToRank(guild, channel, args[0]);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
            case ASSOCIATE:
                if (isAdmin) {
                    associateRanksAction(guild, channel, args[0]);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
            case REMOVE_ASSOCIATED:
                if (isAdmin) {
                    removeRankLinkAction(guild, channel, args[0]);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
            case LIST_ASSOCIATED:
                if (isAdmin || isModo) {
                    listAssociatedRanksAction(guild, channel);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
            case IGNORE:
                if (isAdmin || isModo) {
                    manageAddDynoAction(guild, channel, args[0]);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
            case REMOVE_IGNORE:
                if (isAdmin || isModo) {
                    manageDeleteDynoAction(guild, channel, args[0]);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
            case LIST_IGNORE:
                if (isAdmin || isModo) {
                    listDynoActionsForGuild(guild, channel);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
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
                if (isCmds || isAdmin || isModo) {
                    searchUsersWithoutRole(guild, channel, args[0]);
                } else if (isStatsCmds || isModo || isAdmin) {
                    messagesService.sendBotMessage(channel,
                            "Cette commande est interdite dans ce salon ! Merci d'aller dans le salon " + guild.getTextChannelsByName("cmds", true).get(0).getAsMention());
                }
                break;
            case WITH:
                if (isCmds || isAdmin || isModo) {
                    searchUsersWithRole(guild, channel, args[0]);
                } else if (isStatsCmds || isModo || isAdmin) {
                    messagesService.sendBotMessage(channel,
                            "Cette commande est interdite dans ce salon ! Merci d'aller dans le salon " + guild.getTextChannelsByName("cmds", true).get(0).getAsMention());
                }
                break;
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
                if (isModo || isAdmin) {
                    listCustomReactionsForGuild(guild, channel);
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
                if (isCmds || isAdmin || isModo) {
                    listAssignableRanks(guild, author, channel, commandeComplete);
                } else if (isStatsCmds || isModo || isAdmin) {
                    messagesService.sendBotMessage(channel,
                            "Cette commande est interdite dans ce salon ! Merci d'aller dans le salon " + guild.getTextChannelsByName("cmds", true).get(0).getAsMention());
                }
                break;
            case ADD_RANK:
            case AR:
                if (isAdmin || isModo) {
                    addAssignableRanks(author, channel, guild, commandeComplete, args[0]);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
            case ADD_NOTSINGLE_RANK:
            case ANSR:
                if (isAdmin || isModo) {
                    addNotSingleAssignableRanks(author, channel, guild, commandeComplete, args[0]);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
            case ADD_GLOBAL_RANK:
            case AGR:
                if (isAdmin) {
                    addGlobalRank(author, channel, guild, commandeComplete, args[0], adminRole);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
            case REMOVE_GLOBAL_RANK:
            case RGR:
                if (isAdmin) {
                    removeGlobalRank(author, channel, guild, commandeComplete, args[0], adminRole);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
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
            case ADD_VOICE_ROLE:
            case AVR:
                if (isAdmin) {
                    addVoiceRole(guild, member, author, channel, commandeComplete, args[0]);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
            case REMOVE_RANK:
            case RR:
                if (isAdmin) {
                    removeAssignableRanks(author, channel, guild, commandeComplete, args[0]);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
            case PUNISH:
                if (isAdmin || isModo) {
                    punishUser(channel, guild, args[0], message, isAdmin, modChannel);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
            case REMOVE_NOTSINGLE_RANK:
            case RNSR:
                if (isAdmin) {
                    removeNotSingleAssignableRanks(author, channel, guild, commandeComplete, args[0]);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
            case CHEVALIER:
                if (isCmds || isAdmin || isModo) {
                    messagesService.sendBotMessage(channel, "La commande correcte est ?rank Chevalier XXX. Comme je suis sympa je l'ai corrigée pour vous !");
                    String[] temp = (args.length >= 1) ? new String[]{"Chevalier", args[0]} : new String[]{};
                    args = temp;
                } else if (isStatsCmds || isModo || isAdmin) {
                    messagesService.sendBotMessage(channel,
                            "Cette commande est interdite dans ce salon ! Merci d'aller dans le salon " + guild.getTextChannelsByName("cmds", true).get(0).getAsMention());
                    break;
                }
            case RANK:
                if (isCmds || isAdmin || isModo) {
                    String roleStr = StringUtils.join(new ArrayList<>(Arrays.asList(args)), " ");
                    manageRankCmd(author, channel, guild, roleStr, member, true);
                } else if (isStatsCmds || isModo || isAdmin) {
                    messagesService.sendBotMessage(channel,
                            "Cette commande est interdite dans ce salon ! Merci d'aller dans le salon " + guild.getTextChannelsByName("cmds", true).get(0).getAsMention());
                }
                break;
            case MAKE:
                if (isCmds || isAdmin || isModo) {
                    makeMeASandwich(channel, args[0]);
                } else if (isStatsCmds || isModo || isAdmin) {
                    messagesService.sendBotMessage(channel,
                            "Cette commande est interdite dans ce salon ! Merci d'aller dans le salon " + guild.getTextChannelsByName("cmds", true).get(0).getAsMention());
                }
                break;
            case SUDO:
                if (isCmds || isAdmin || isModo) {
                    sudoMakeMeASandwich(isAdmin, channel, args[0]);
                } else if (isStatsCmds || isModo || isAdmin) {
                    messagesService.sendBotMessage(channel,
                            "Cette commande est interdite dans ce salon ! Merci d'aller dans le salon " + guild.getTextChannelsByName("cmds", true).get(0).getAsMention());
                }
                break;
            case REVIVE:
                if (isCmds || isAdmin || isModo) {
                    calculateSRandMedals(channel, args);
                } else if (isStatsCmds || isModo || isAdmin) {
                    messagesService.sendBotMessage(channel,
                            "Cette commande est interdite dans ce salon ! Merci d'aller dans le salon " + guild.getTextChannelsByName("cmds", true).get(0).getAsMention());
                }
                break;
            case INACTIVITY:
                if (isAdmin || isModo) {
                    listInactiveMember(channel, guild, args[0]);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
            case ISACTIVE:
                if (isAdmin || isModo) {
                    isMemberActive(channel, guild, args[0]);
                } else {
                    messagesService.sendMessageNotEnoughRights(channel);
                }
                break;
            case STAT:
                if (isStatsCmds || isAdmin || isModo) {
                calculateStatsForPlayer(channel, author, args, guild, member, isAdmin || isModo, isAdmin);
                } else if (isCmds) {
                    List<TextChannel> statCmdsChannels = guild.getTextChannelsByName("stats-cmds", true);
                    if (!statCmdsChannels.isEmpty()) {
                        messagesService.sendBotMessage(channel, "Cette commande est interdite dans ce salon ! Merci d'aller dans le salon " + statCmdsChannels.get(0).getAsMention());
                    } else {
                    calculateStatsForPlayer(channel, author, args, guild, member, isAdmin || isModo, isAdmin);
                    }
                }
                break;
        case STAT2:
            if (isStatsCmds || isAdmin || isModo) {
                calculateStats2ForPlayer(channel, author, args, guild, member, isAdmin || isModo, isAdmin);
            } else if (isCmds) {
                List<TextChannel> statCmdsChannels = guild.getTextChannelsByName("stats-cmds", true);
                if (!statCmdsChannels.isEmpty()) {
                    messagesService.sendBotMessage(channel, "Cette commande est interdite dans ce salon ! Merci d'aller dans le salon " + statCmdsChannels.get(0).getAsMention());
                } else {
                    calculateStats2ForPlayer(channel, author, args, guild, member, isAdmin || isModo, isAdmin);
                }
            }
            break;
            default:
                System.out.println("DeathbotExecution : Commande non prise en charge");
                break;
        }
    }

    private void calculateStatsForPlayer(MessageChannel channel, User author, String[] arg, Guild guild, Member member, boolean isSuperUser, boolean isAdmin) {
        List<PlayerStatDTO> playerStatsResult = globalDao.getStatsForPlayer(author.getIdLong(), true, 1);

        PlayerStatDTO actualStats = null;
        if (!playerStatsResult.isEmpty()) {
            actualStats = playerStatsResult.get(0);
        }

        if (arg.length < 1 || (arg.length == 1 && "help".equals(arg[0]))) {
            String message = buildStatCommandHelp();
            messagesService.sendBotMessage(channel, message);
            return;
        }

        String[] params = arg[0].split(ACTION_ARGS_SEPARATOR);

        if (params.length <= 2) {
            if (params.length == 1) {
                String keyWord = params[0];
                manageStatSingleCommands(channel, author, guild, member, keyWord);
            } else if (params.length == 2) {
                String keyWord = params[0];
                if ("cancel".equals(keyWord)) {
                    String idStr = params[1];
                    Integer id = Integer.valueOf(idStr);
                    PlayerStatDTO statById = globalDao.getStatById(id);
                    if (statById.getPlayerId() == author.getIdLong() || isSuperUser) {
                        deleteStatById(channel, id, statById);
                    } else {
                        messagesService.sendBotMessage(channel,
                                "Cette statistiques n'est pas à vous, vous ne pouvez donc pas la supprimer ! Pour savoir quelles sont vos statistiques, tapez ?stat history");
                    }
                } else {
                    String message = buildStatCommandHelp();
                    messagesService.sendBotMessage(channel, message);
                    return;
                }
            }
        } else {
            PlayerStatDTO newStats = new PlayerStatDTO();
            newStats.setPlayerId(author.getIdLong());
            newStats.setKl(Integer.valueOf(params[0]));
            newStats.setMedals(helperService.convertEFLettersToNumber(params[1]));
            newStats.setSr(helperService.convertEFLettersToNumber(params[2]));
            newStats.setUpdateDate(LocalDateTime.now());
            newStats.setPlayerInstantName(guild.retrieveMember(author).complete().getEffectiveName());

            boolean isRatioProvided = false;
            boolean isPreviousRatioPresent = false;

            String srRatio = "0.9";
            if (params.length == 4) {
                isRatioProvided = true;
                srRatio = params[3];
                newStats.setSrRatio(Float.parseFloat(srRatio));
            } else if (actualStats != null && actualStats.getSrRatio() != 0) {
                isPreviousRatioPresent = true;
                srRatio = String.format(java.util.Locale.US, "%.2f", actualStats.getSrRatio());
                newStats.setSrRatio(Float.parseFloat(srRatio));
            } else {
                newStats.setSrRatio(Float.parseFloat("0"));
            }

            Object[] srResults = calculateSrStat(newStats, channel, srRatio, isRatioProvided, isPreviousRatioPresent);

            BigDecimal srPercentage = (BigDecimal) srResults[0];
            StringBuilder srSb = (StringBuilder) srResults[1];

            if (srPercentage.compareTo(new BigDecimal(40)) < 0) {

                if (actualStats != null) {
                    compareStats(actualStats, newStats, channel, guild, author);
                } else {
                    messagesService.sendBotMessage(channel,
                            "Bonjour " + guild.retrieveMember(author).complete().getEffectiveName() + ", j'ai enregistré vos informations. A la prochaine !" + RETOUR_LIGNE);
                }

                manageRankCmd(author, channel, guild, "Chevalier " + params[0], guild.retrieveMember(author).complete(), false);

                if (!isAdmin) {
                    if (member.getNickname() != null && member.getNickname().contains("\uD83C\uDFC6")) {
                        String originalNickname = member.getNickname().split("\uD83C\uDFC6")[0];
                        String newNickname = originalNickname.trim() + " \uD83C\uDFC6 " + params[0];
                        guild.modifyNickname(member, newNickname).complete();
                    } else {
                        String oldNickName;
                        if (member.getNickname() == null) {
                            oldNickName = member.getEffectiveName();
                        } else {
                            oldNickName = member.getNickname().trim();
                        }

                        String newNickname = oldNickName + " \uD83C\uDFC6 " + params[0];
                        guild.modifyNickname(member, newNickname).complete();
                    }
                }

                messagesService.sendBotMessage(channel, srSb.toString());

                globalDao.savePlayerStats(newStats);
            } else {

                StringBuilder sb = new StringBuilder("Votre SR est énoooorme (it's over 9000 !), êtes vous sûr des informations rentrées ?");
                messagesService.sendBotMessage(channel, sb.toString());
            }
        }
    }

    private void calculateStats2ForPlayer(MessageChannel channel, User author, String[] arg, Guild guild, Member member, boolean isSuperUser, boolean isAdmin) {
        List<PlayerStatDTO> playerStatsResult = globalDao.getStats2ForPlayer(author.getIdLong(), true, 1);

        PlayerStatDTO actualStats = null;
        if (!playerStatsResult.isEmpty()) {
            actualStats = playerStatsResult.get(0);
        }

        if (arg.length < 1 || (arg.length == 1 && "help".equals(arg[0]))) {
            String message = buildStatCommandHelp();
            messagesService.sendBotMessage(channel, message);
            return;
        }

        String[] params = arg[0].split(ACTION_ARGS_SEPARATOR);

        if (params.length <= 2) {
            if (params.length == 1) {
                String keyWord = params[0];
                manageStat2SingleCommands(channel, author, guild, member, keyWord);
            } else if (params.length == 2) {
                String keyWord = params[0];
                if ("cancel".equals(keyWord)) {
                    String idStr = params[1];
                    Integer id = Integer.valueOf(idStr);
                    PlayerStatDTO statById = globalDao.getStat2ById(id);
                    if (statById.getPlayerId() == author.getIdLong() || isSuperUser) {
                        deleteStat2ById(channel, id, statById);
                    } else {
                        messagesService.sendBotMessage(channel,
                                "Cette statistiques n'est pas à vous, vous ne pouvez donc pas la supprimer ! Pour savoir quelles sont vos statistiques, tapez ?stat history");
                    }
                } else {
                    String message = buildStatCommandHelp();
                    messagesService.sendBotMessage(channel, message);
                    return;
                }
            }
        } else {
            PlayerStatDTO newStats = new PlayerStatDTO();
            newStats.setPlayerId(author.getIdLong());
            newStats.setKl(Integer.valueOf(params[0]));
            newStats.setMedals(helperService.convertEFLettersToNumber(params[1]));
            newStats.setSr(helperService.convertEFLettersToNumber(params[2]));
            newStats.setUpdateDate(LocalDateTime.now());
            newStats.setPlayerInstantName(guild.retrieveMember(author).complete().getEffectiveName());

            boolean isRatioProvided = false;
            boolean isPreviousRatioPresent = false;

            String srRatio = "0.9";
            if (params.length == 4) {
                isRatioProvided = true;
                srRatio = params[3];
                newStats.setSrRatio(Float.parseFloat(srRatio));
            } else if (actualStats != null && actualStats.getSrRatio() != 0) {
                isPreviousRatioPresent = true;
                srRatio = String.format(java.util.Locale.US, "%.2f", actualStats.getSrRatio());
                newStats.setSrRatio(Float.parseFloat(srRatio));
            } else {
                newStats.setSrRatio(Float.parseFloat("0"));
            }

            Object[] srResults = calculateSrStat(newStats, channel, srRatio, isRatioProvided, isPreviousRatioPresent);

            BigDecimal srPercentage = (BigDecimal) srResults[0];
            StringBuilder srSb = (StringBuilder) srResults[1];

            if (srPercentage.compareTo(new BigDecimal(200)) < 0) {

                if (actualStats != null) {
                    compareStats(actualStats, newStats, channel, guild, author);
                } else {
                    messagesService.sendBotMessage(channel,
                            "Bonjour " + guild.retrieveMember(author).complete().getEffectiveName() + ", j'ai enregistré vos informations. A la prochaine !" + RETOUR_LIGNE);
                }

                // manageRankCmd(author, channel, guild.getController(), "Chevalier " + params[0],
                // guild.retrieveMember(author).complete(), false);

                // if (!isAdmin) {
                // if (member.getNickname() != null && member.getNickname().contains("\uD83C\uDFC6")) {
                // String originalNickname = member.getNickname().split("\uD83C\uDFC6")[0];
                // String newNickname = originalNickname.trim() + " \uD83C\uDFC6 " + params[0];
                // guild.getController().setNickname(member, newNickname).complete();
                // } else {
                // String oldNickName;
                // if (member.getNickname() == null) {
                // oldNickName = member.getEffectiveName();
                // } else {
                // oldNickName = member.getNickname().trim();
                // }
                //
                // String newNickname = oldNickName + " \uD83C\uDFC6 " + params[0];
                // guild.getController().setNickname(member, newNickname).complete();
                // }
                // }

                messagesService.sendBotMessage(channel, srSb.toString());

                globalDao.savePlayerStats2(newStats);
            } else {

                StringBuilder sb = new StringBuilder("Votre SR est énoooorme (it's over 9000 !), êtes vous sûr des informations rentrées ?");
                messagesService.sendBotMessage(channel, sb.toString());
            }
        }
    }

    private void deleteStatById(MessageChannel channel, Integer id, PlayerStatDTO statById) {
        int deleted = globalDao.cancelStatById(statById.getId());
        if (deleted != 0) {
            messagesService.sendBotMessage(channel, "La statistique ayant pour id : " + id + " a bien été supprimée !");
        } else {
            messagesService.sendBotMessage(channel, "La statistique avec l'id " + id + " n'a pas pu être supprimée. Veuillez contacter un administrateur.");
        }
    }

    private void deleteStat2ById(MessageChannel channel, Integer id, PlayerStatDTO statById) {
        int deleted = globalDao.cancelStat2ById(statById.getId());
        if (deleted != 0) {
            messagesService.sendBotMessage(channel, "La statistique ayant pour id : " + id + " a bien été supprimée !");
        } else {
            messagesService.sendBotMessage(channel, "La statistique avec l'id " + id + " n'a pas pu être supprimée. Veuillez contacter un administrateur.");
        }
    }

    private void manageStatSingleCommands(MessageChannel channel, User author, Guild guild, Member member, String keyWord) {
        BufferedImage image;
        switch (keyWord) {
        case "graph":
            List<PlayerStatDTO> playersSRStats = chartService.getSRStatsForAllPlayersByKL();
            if (playersSRStats != null && !playersSRStats.isEmpty()) {
                image = chartService.drawAllPlayersSRChart(playersSRStats, this);
                messagesService.sendBufferedImage(channel, image, author.getAsMention(), "KL.png");
            } else {
                messagesService.sendBotMessage(channel, "Aucune donnée trouvée ! Pour savoir comment enregistrer vos données, tapez ?stat");
            }
            break;
        case "cancel":
            globalDao.cancelLastPlayerStats(author.getIdLong());
            messagesService.sendBotMessage(channel, "Votre dernière statistique a été supprimée !");
            break;
        case "history":
            sendStatsHistoryForPlayer(channel, author);
            break;
        case "kl":
            HashMap<LocalDateTime, Integer> playerKLStats = chartService.getKLStatsForPlayer(author.getIdLong());
            if (playerKLStats != null && !playerKLStats.isEmpty()) {
                image = chartService.drawKLChart(playerKLStats);
                messagesService.sendBufferedImage(channel, image, author.getAsMention(), "KL.png");
            } else {
                messagesService.sendBotMessage(channel, "Aucune donnée trouvée ! Pour savoir comment enregistrer vos données, tapez ?stat");
            }
            break;
        case "sr":
            HashMap<LocalDateTime, BigDecimal> playerSRStats = chartService.getSRStatsForPlayer(author.getIdLong());
            if (playerSRStats != null && !playerSRStats.isEmpty()) {
                image = chartService.drawSRChart(playerSRStats);
                messagesService.sendBufferedImage(channel, image, author.getAsMention(), "KL.png");
            } else {
                messagesService.sendBotMessage(channel, "Aucune donnée trouvée ! Pour savoir comment enregistrer vos données, tapez ?stat");
            }
            break;
        case "med":
        case "medals":
            HashMap<LocalDateTime, BigDecimal> playerMedStats = chartService.getMedStatsForPlayer(author.getIdLong());
            if (playerMedStats != null && !playerMedStats.isEmpty()) {
                image = chartService.drawMedChart(playerMedStats);
                messagesService.sendBufferedImage(channel, image, author.getAsMention(), "Med.png");
            } else {
                messagesService.sendBotMessage(channel, "Aucune donnée trouvée ! Pour savoir comment enregistrer vos données, tapez ?stat");
            }
            break;
        case "compare":
            List<Role> roles = member.getRoles();
            List<Role> usefulRoles = new ArrayList<>();
            boolean found = false;
            for (Role role : roles) {
                if (role.getName().startsWith("Chevalier")) {
                    usefulRoles.add(role);
                    found = true;
                }
            }
            if (found) {
                List<Member> compareToMembers = guild.getMembersWithRoles(usefulRoles);
                compareToMembers.remove(member);
                if (!compareToMembers.isEmpty()) {
                    chartService.drawMultipleComparisons(channel, author, compareToMembers);
                } else {
                    messagesService.sendBotMessage(channel, "Aucun joueur trouvé correspondant à votre niveau, désolé =(");
                }
            } else {
                messagesService.sendBotMessage(channel, "Vous n'avez aucun rôle de Chevalier, comparaison impossible ='(");
            }
            break;
        default:
            String message = buildStatCommandHelp();
            messagesService.sendBotMessage(channel, message);
            break;
        }
    }

    private void manageStat2SingleCommands(MessageChannel channel, User author, Guild guild, Member member, String keyWord) {
        BufferedImage image;
        switch (keyWord) {
        case "graph":
            List<PlayerStatDTO> playersSRStats = chartService.getSRStats2ForAllPlayersByKL();
            if (playersSRStats != null && !playersSRStats.isEmpty()) {
                image = chartService.drawAllPlayersSRChart(playersSRStats, this);
                messagesService.sendBufferedImage(channel, image, author.getAsMention(), "KL.png");
            } else {
                messagesService.sendBotMessage(channel, "Aucune donnée trouvée ! Pour savoir comment enregistrer vos données, tapez ?stat");
            }
            break;
        case "cancel":
            globalDao.cancelLastPlayerStats2(author.getIdLong());
            messagesService.sendBotMessage(channel, "Votre dernière statistique a été supprimée !");
            break;
        case "history":
            sendStats2HistoryForPlayer(channel, author);
            break;
        case "kl":
            HashMap<LocalDateTime, Integer> playerKLStats = chartService.getKLStats2ForPlayer(author.getIdLong());
            if (playerKLStats != null && !playerKLStats.isEmpty()) {
                image = chartService.drawKLChart(playerKLStats);
                messagesService.sendBufferedImage(channel, image, author.getAsMention(), "KL.png");
            } else {
                messagesService.sendBotMessage(channel, "Aucune donnée trouvée ! Pour savoir comment enregistrer vos données, tapez ?stat");
            }
            break;
        case "sr":
            HashMap<LocalDateTime, BigDecimal> playerSRStats = chartService.getSRStats2ForPlayer(author.getIdLong());
            if (playerSRStats != null && !playerSRStats.isEmpty()) {
                image = chartService.drawSRChart(playerSRStats);
                messagesService.sendBufferedImage(channel, image, author.getAsMention(), "KL.png");
            } else {
                messagesService.sendBotMessage(channel, "Aucune donnée trouvée ! Pour savoir comment enregistrer vos données, tapez ?stat");
            }
            break;
        case "med":
        case "medals":
            HashMap<LocalDateTime, BigDecimal> playerMedStats = chartService.getMedStats2ForPlayer(author.getIdLong());
            if (playerMedStats != null && !playerMedStats.isEmpty()) {
                image = chartService.drawMedChart(playerMedStats);
                messagesService.sendBufferedImage(channel, image, author.getAsMention(), "Med.png");
            } else {
                messagesService.sendBotMessage(channel, "Aucune donnée trouvée ! Pour savoir comment enregistrer vos données, tapez ?stat");
            }
            break;
        case "compare":
            // messagesService.sendBotMessage(channel, "Commande non disponible pour le moment");
            List<PlayerStatDTO> playerStat = globalDao.getStats2ForPlayer(author.getIdLong(), true, 1);
            List<Member> compareToMembers = new ArrayList<>();

            if (playerStat != null && !playerStat.isEmpty()) {
                List<PlayerStatDTO> otherPlayerStats = globalDao.getAllStats2ForSameKL(playerStat.get(0).getKl());
                for (PlayerStatDTO otherPlayerStat : otherPlayerStats) {
                    for (Member member1 : guild.getMembers()) {
                        if (member1.getUser().getIdLong() == otherPlayerStat.getPlayerId()) {
                            compareToMembers.add(member1);
                        }
                    }
                }
            } else {
                messagesService.sendBotMessage(channel, "Aucune donnée trouvée ! Pour savoir comment enregistrer vos données, tapez ?stat");
            }

            compareToMembers.remove(member);
            if (!compareToMembers.isEmpty()) {
                chartService.drawMultipleComparisons2(channel, author, compareToMembers);
            } else {
                messagesService.sendBotMessage(channel, "Aucun joueur trouvé correspondant à votre niveau, désolé =(");
            }
            break;
        default:
            String message = buildStatCommandHelp();
            messagesService.sendBotMessage(channel, message);
            break;
        }
    }

    private void sendStatsHistoryForPlayer(MessageChannel channel, User author) {
        List<PlayerStatDTO> playerStats = globalDao.getStatsForPlayer(author.getIdLong(), false, 19);
        if (!playerStats.isEmpty()) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            StringBuilder sb = new StringBuilder("__Vous avez enregistré les statistiques suivantes : __").append(RETOUR_LIGNE).append(RETOUR_LIGNE);
            for (PlayerStatDTO playerStat : playerStats) {
                sb.append(TAB).append("-Id: ").append(playerStat.getId()).append(ROLES_SEPARATOR);
                sb.append("KL : ").append(playerStat.getKl()).append(ROLES_SEPARATOR);
                sb.append("Médailles : ").append(helperService.formatBigNumbersToEFFormat(playerStat.getMedals())).append(ROLES_SEPARATOR).append(SPACE);
                sb.append("SR : ").append(helperService.formatBigNumbersToEFFormat(playerStat.getSr())).append(ROLES_SEPARATOR).append(SPACE);
                sb.append("Ratio : ").append(processRealSRRatio(playerStat.getSrRatio())).append(ROLES_SEPARATOR).append(SPACE);
                sb.append("Date de maj : ").append(dtf.format(playerStat.getUpdateDate())).append(RETOUR_LIGNE).append(SPACE);
            }
            messagesService.sendNormalBotMessage(channel, sb.toString());
        } else {
            messagesService.sendBotMessage(channel, "Aucune donnée trouvée ! Pour savoir comment enregistrer vos données, tapez ?stat");
        }
    }

    private void sendStats2HistoryForPlayer(MessageChannel channel, User author) {
        List<PlayerStatDTO> playerStats = globalDao.getStats2ForPlayer(author.getIdLong(), false, 19);
        if (!playerStats.isEmpty()) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            StringBuilder sb = new StringBuilder("__Vous avez enregistré les statistiques suivantes : __").append(RETOUR_LIGNE).append(RETOUR_LIGNE);
            for (PlayerStatDTO playerStat : playerStats) {
                sb.append(TAB).append("-Id: ").append(playerStat.getId()).append(ROLES_SEPARATOR);
                sb.append("KL : ").append(playerStat.getKl()).append(ROLES_SEPARATOR);
                sb.append("Médailles : ").append(helperService.formatBigNumbersToEFFormat(playerStat.getMedals())).append(ROLES_SEPARATOR).append(SPACE);
                sb.append("SR : ").append(helperService.formatBigNumbersToEFFormat(playerStat.getSr())).append(ROLES_SEPARATOR).append(SPACE);
                sb.append("Ratio : ").append(processRealSRRatio(playerStat.getSrRatio())).append(ROLES_SEPARATOR).append(SPACE);
                sb.append("Date de maj : ").append(dtf.format(playerStat.getUpdateDate())).append(RETOUR_LIGNE).append(SPACE);
            }
            messagesService.sendNormalBotMessage(channel, sb.toString());
        } else {
            messagesService.sendBotMessage(channel, "Aucune donnée trouvée ! Pour savoir comment enregistrer vos données, tapez ?stat");
        }
    }

    private float processRealSRRatio(float srRatio) {
        if (srRatio == 0.0) {
            return 0.9f;
        }
        return srRatio;
    }

    private String buildStatCommandHelp() {
        StringBuilder sb = new StringBuilder("**__Aide de la commande ?stat : __**" + RETOUR_LIGNE).append(RETOUR_LIGNE);
        sb.append(BALISE_CODE + "?stat <VOTRE_KL> <VOS_MÉDAILLES_AU_FORMAT_EF> <VOTRE_SR_AU_FORMAT_EF> [<VOTRE_RATIO_DE_SR>]" + BALISE_CODE).append(RETOUR_LIGNE);
        sb.append(TAB + "- Si le ratio de SR N'EST PAS renseigné : Enregistre vos statistiques actuelles et les compare à vos dernières données enregistrées.").append(
                RETOUR_LIGNE);
        sb.append(TAB
                + "- Si le ratio de SR EST renseigné : Affichera en plus la valeur d'un SR complet (et celui d'un SR doublé) en pourcentage de votre total de médailles (et en médailles)").append(
                RETOUR_LIGNE).append(RETOUR_LIGNE);
        sb.append("*__Exemples__* : " + RETOUR_LIGNE + BALISE_CODE + "?stat 239 10.1g 1.44f 0.987" + RETOUR_LIGNE + "?stat 303 198.1h 11.7g" + BALISE_CODE).append(
                RETOUR_LIGNE).append(RETOUR_LIGNE);
        sb.append(BALISE_CODE + "?stat cancel" + BALISE_CODE
                + " : Permet d'annuler votre dernière statistique enregistrée. Utile en cas d'erreur lors de la saisie précédente").append(RETOUR_LIGNE);
        sb.append(BALISE_CODE + "?stat cancel <ID>" + BALISE_CODE + " : Permet d'annuler la statistique dont l'id est renseigné. Cet id est récupérable via la commande "
                + BALISE_CODE + "?stat history" + BALISE_CODE).append(RETOUR_LIGNE);
        sb.append(BALISE_CODE + "?stat history" + BALISE_CODE + " : Permet de consulter vos 19 (merci Discord pour la limite...) dernières statistiques enregistrées.").append(
                RETOUR_LIGNE);
        sb.append(RETOUR_LIGNE);
        sb.append(BALISE_CODE + "?stat graph" + BALISE_CODE
                + " : Permet d'afficher la courbe actuelle des valeurs de SR (en pourcentage du total de médailles) en fonction du KL.").append(
                RETOUR_LIGNE);
        sb.append(BALISE_CODE + "?stat kl" + BALISE_CODE
                + " : Permet d'afficher la courbe d'évolution de votre KL dans le temps").append(
                RETOUR_LIGNE);
        sb.append(BALISE_CODE + "?stat sr" + BALISE_CODE
                + " : Permet d'afficher la courbe d'évolution de votre SR dans le temps").append(
                RETOUR_LIGNE);
        sb.append(BALISE_CODE + "?stat medals" + BALISE_CODE
                + " : Permet d'afficher la courbe d'évolution de votre total de médailles dans le temps").append(
                RETOUR_LIGNE);
        sb.append(RETOUR_LIGNE);
        sb.append(BALISE_CODE + "?stat compare" + BALISE_CODE + " : Permet d'afficher 3 graphiques de comparaison avec les autres joueurs de votre niveau").append(RETOUR_LIGNE);

        return sb.toString();
    }

    private Object[] calculateSrStat(PlayerStatDTO newStats, MessageChannel channel, String srRatio, boolean isRatioProvided, boolean isPreviousRatioPresent) {

        StringBuilder sb = new StringBuilder();

        if (!isRatioProvided) {
            sb.append("Vous n'avez renseigné aucun ratio de SR ! ");
            if (isPreviousRatioPresent) {
                sb.append("Le ratio utilisé sera votre dernier ratio enregistré : ").append(srRatio).append(RETOUR_LIGNE);
            } else {
                sb.append("Le ratio utilisé sera le ratio par défaut : ").append(srRatio).append(RETOUR_LIGNE);
            }
            sb.append("Pour plus de précision dans les calculs, merci de renseigner un ratio la prochaine fois ;)").append(RETOUR_LIGNE).append(RETOUR_LIGNE);
        }

        sb.append("**__Sprit Rest__**").append(RETOUR_LIGNE);

        System.out.println("KL : " + newStats.getKl());
        System.out.println("SR : " + newStats.getSr());
        System.out.println("Medals : " + newStats.getMedals());
        System.out.println("SR Ration : " + newStats.getSrRatio());

        BigDecimal fullSR = newStats.getSr().multiply(new BigDecimal(60L)).multiply(new BigDecimal(4L)).multiply(new BigDecimal(srRatio));
        BigDecimal srPercentage = fullSR.multiply(new BigDecimal(100L)).divide(newStats.getMedals(), 2, BigDecimal.ROUND_HALF_DOWN);
        sb.append(srPercentage).append("% (").append(helperService.formatBigNumbersToEFFormat(fullSR)).append(")").append(RETOUR_LIGNE);

        sb.append("**__Sprit Rest doublé__**").append(RETOUR_LIGNE);
        BigDecimal dbFullSR = fullSR.multiply(new BigDecimal(2L));
        BigDecimal dbSrPercentage = srPercentage.multiply(new BigDecimal(2L));
        sb.append(dbSrPercentage).append("% (").append(helperService.formatBigNumbersToEFFormat(dbFullSR)).append(")");

        Object[] returns = new Object[2];

        returns[0] = srPercentage;
        returns[1] = sb;
        return returns;
    }

    private void compareStats(PlayerStatDTO actualStats, PlayerStatDTO newStats, MessageChannel channel, Guild guild, User author) {
        int kl = newStats.getKl() - actualStats.getKl();
        BigDecimal medals = BigDecimal.ZERO.compareTo(newStats.getMedals().subtract(actualStats.getMedals())) != 0 ?
                (newStats.getMedals().subtract(actualStats.getMedals())).multiply(new BigDecimal(100L)).divide(actualStats.getMedals(), 2, RoundingMode.HALF_DOWN) :
                BigDecimal.ZERO;
        BigDecimal sr = BigDecimal.ZERO.compareTo(newStats.getSr().subtract(actualStats.getSr())) != 0 ?
                (newStats.getSr().subtract(actualStats.getSr())).multiply(new BigDecimal(100L)).divide(actualStats.getSr(), 2, RoundingMode.HALF_DOWN) :
                BigDecimal.ZERO;
        Duration duration = Duration.between(actualStats.getUpdateDate(), newStats.getUpdateDate()).abs();

        StringBuilder sb = new StringBuilder("Bonjour " + guild.retrieveMember(author).complete().getEffectiveName() + ", en ");
        sb.append(formatDuration(duration)).append(" vous avez gagné : ").append(RETOUR_LIGNE);
        sb.append(kl).append(" KL").append(RETOUR_LIGNE);
        sb.append(medals).append("% de médailles").append(RETOUR_LIGNE);
        sb.append(sr).append("% de SR").append(RETOUR_LIGNE);

        messagesService.sendBotMessage(channel, sb.toString());
    }

    private void addRankToRank(Guild guild, MessageChannel channel, String arg) {
        String[] args = arg.split(PARAMETERS_SEPARATOR);
        List<String> rolesToSearch = Arrays.asList(args[1].trim().split(ROLES_SEPARATOR));
        String roleToAdd = args[0].trim();

        List<Member> members = new ArrayList<>();
        for (String roleName : rolesToSearch) {
            members.addAll(guild.getMembersWithRoles(guild.getRolesByName(roleName, true)));
        }

        for (Member member : members) {
            List<Role> roleToAddObj = guild.getRolesByName(roleToAdd, true);
            if (roleToAddObj.isEmpty()) {
                messagesService.sendBotMessage(channel, "Le role " + roleToAdd + " n'a pas été trouvé !");
                return;
            }
            for (Role role : roleToAddObj) {
                guild.addRoleToMember(member, role).complete();
            }

        }

        messagesService.sendBotMessage(channel, "Traitement d'ajout du rôle " + roleToAdd + " terminé !");
    }

    private void removeRankLinkAction(Guild guild, MessageChannel channel, String arg) {
        String rankToRemove = arg.trim();

        Role role = getRoleFromRoleName(guild, channel, rankToRemove);
        linkedRolesByGuild.get(guild).remove(role);

        globalDao.deleteRankLink(guild, role);
        StringBuilder sb = new StringBuilder("Le rôle " + role.getName() + " n'est plus lié à aucun autre rôle.");
        messagesService.sendBotMessage(channel, sb.toString());
    }

    private void listAssociatedRanksAction(Guild guild, MessageChannel channel) {
        HashMap<Role, Set<Role>> linkedRanksByRole = getLinkedRanksForGuild(guild);

        if (linkedRanksByRole != null && !linkedRanksByRole.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("__**Liste des rôles liés**__").append(RETOUR_LIGNE).append(RETOUR_LIGNE);
            for (Role linkedRole : linkedRanksByRole.keySet()) {
                sb.append("*Au rôle " + linkedRole.getName() + "* => ").append(RETOUR_LIGNE);
                for (Role role : linkedRanksByRole.get(linkedRole)) {
                    sb.append(TAB + "- ").append(role.getName()).append(RETOUR_LIGNE);
                }
                sb.append(RETOUR_LIGNE);
            }
            messagesService.sendBotMessage(channel, sb.toString());

        } else {
            messagesService.sendBotMessage(channel, "Aucun rôle n'est lié à un autre !");
        }
    }

    private void associateRanksAction(Guild guild, MessageChannel channel, String arg) {
        String[] args = arg.trim().split(PARAMETERS_SEPARATOR, 2);
        String rank = args[0];
        Role role = getRoleFromRoleName(guild, channel, rank);

        String concernedRankStr = args[1];

        Set<Role> linkedRoles = createListOfRoleFromStringTable(concernedRankStr.split(ROLES_SEPARATOR), guild, channel);
        getLinkedRanksForGuild(guild);

        setLinkedRanksForGuild(guild, role, linkedRoles);

        if (!linkedRoles.isEmpty()) {
            StringBuilder sb = buildNewLinkedRankMessage(linkedRoles, role);
            messagesService.sendBotMessage(channel, sb.toString());
        }
    }

    private void setLinkedRanksForGuild(Guild guild, Role role, Set<Role> linkedRoles) {
        if (linkedRolesByGuild == null) {
            linkedRolesByGuild = new HashMap<>();
        }
        HashMap<Role, Set<Role>> linkedRolesForGuild = linkedRolesByGuild.get(guild);
        if (linkedRolesForGuild == null) {
            linkedRolesForGuild = new HashMap<>();
        }
        linkedRolesForGuild.put(role, linkedRoles);
        linkedRolesByGuild.put(guild, linkedRolesForGuild);
        globalDao.createRankLink(guild, role, linkedRoles);
    }

    private HashMap<Role, Set<Role>> getLinkedRanksForGuild(Guild guild) {
        if (linkedRolesByGuild.isEmpty() && !linkedRanksByIdsByGuild.isEmpty()) {
            linkedRolesByGuild = transformHashRanksIdsToObjects(guild, linkedRanksByIdsByGuild);
        }

        return (linkedRolesByGuild != null && !linkedRolesByGuild.isEmpty()) ? linkedRolesByGuild.get(guild) : new HashMap<>();
    }

    private Role getRoleFromRoleName(Guild guild, MessageChannel channel, String roleName) {
        Role role = null;
        List<Role> roles = guild.getRolesByName(roleName, true);

        if (roles != null && !roles.isEmpty()) {
            role = roles.get(0);
        } else {
            messagesService.sendBotMessage(channel, "Aucun rôle trouvé pour le nom : " + roleName);
        }
        return role;
    }

    private void isMemberActive(MessageChannel channel, Guild guild, String arg) {
        String[] args = arg.split(PARAMETERS_SEPARATOR);
        String name = args[0].trim();
        int limit = Integer.parseInt(args[1].trim());

        String specChannelName = null;

        if (args.length == 3) {
            specChannelName = args[2].trim();
        }

        System.out.println("---> Paramètres récupérés : " + name + ", " + specChannelName + ", " + limit);

        OffsetDateTime limitDateTime = OffsetDateTime.now().minusDays(limit);

        List<Member> members = new ArrayList<>(guild.getMembersByNickname(name, true));
        members.addAll(guild.getMembersByEffectiveName(name, true));
        members.addAll(guild.getMembersByName(name, true));

        if (members != null && !members.isEmpty()) {

            Member member = members.get(0);

            System.out.println("Membre récupéré : " + member.getEffectiveName());

            if (specChannelName == null) {
                for (TextChannel textChannel : guild.getTextChannels()) {
                    if (!getMessagesByUserAfterLimit(textChannel, member.getUser(), limitDateTime).isEmpty()) {
                        messagesService.sendBotMessage(channel,
                                "Le membre " + member.getEffectiveName() + " a été actif pendant ces " + limit + " derniers jours sur le salon : " + textChannel.getName());
                        return;
                    }
                }
                messagesService.sendBotMessage(channel, "Le membre " + member.getNickname() + " est apparemment inactif depuis plus de " + limit + " jours !");
            } else {

                List<TextChannel> specChannels = guild.getTextChannelsByName(specChannelName, true);
                if (specChannels != null && !specChannels.isEmpty()) {
                    TextChannel specChannel = specChannels.get(0);

                    System.out.println("Channel spécifique récupéré : " + specChannel.getName());

                    if (!getMessagesByUserAfterLimit(specChannel, member.getUser(), limitDateTime).isEmpty()) {
                        messagesService.sendBotMessage(channel,
                                "Le membre " + member.getEffectiveName() + " a été actif pendant ces " + limit + " derniers jours sur le salon : " + specChannel.getName());
                        return;
                    } else {
                        messagesService.sendBotMessage(channel,
                                "Le membre " + member.getEffectiveName() + " est apparemment inactif depuis plus de " + limit + " jours sur le salon " + specChannelName);
                    }
                } else {
                    messagesService.sendBotMessage(channel, "Le salon " + specChannelName + " n'a pas été trouvé, désolé !");
                }
            }
        } else {
            messagesService.sendBotMessage(channel, "Le membre " + name + " n'a pas été trouvé, désolé !");
        }
    }

    private void listInactiveMember(MessageChannel channel, Guild guild, String paramStr) {
        System.out.println("Commande ?inactivity lancée.");

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
        return channel.getIterableHistory().stream().limit(15000).filter(m -> m.getAuthor().equals(user)).filter(m -> m.getTimeCreated().isAfter(limitDateTime)).collect(
                Collectors.toList());
    }

    private void punishUser(MessageChannel channel, Guild guild, String arg, Message message, boolean isAdmin, MessageChannel modChannel) {

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
        Role adminRole = guild.getRolesByName("Admin", true).get(0);
        Role modoRole = guild.getRolesByName("Moderateur", true).get(0);

        Member memberToPerformActionOn;

        try {
            memberToPerformActionOn = guild.getMembersByEffectiveName(user.replace("@", ""), true).get(0);

            if (memberToPerformActionOn.getRoles().contains(adminRole) || memberToPerformActionOn.getRoles().contains(modoRole)) {
                messagesService.sendBotMessage(channel, "Hin hin, ce mec, c'est presque Dieu... On lui crache pas au visage ;)");
                return;
            }

            switch (action) {
                case "mute":
                    muteUser(duration, comment, guild, memberToPerformActionOn, actualChannel, message, channel);
                    break;
                case "kick":
                    kickUser(comment, guild, memberToPerformActionOn, actualChannel, message, channel);
                    break;
                case "warn":
                    warnUser(comment, memberToPerformActionOn, actualChannel, message, channel);
                    break;
                case "ban":
                    if (isAdmin) {
                        banUser(duration, comment, guild, memberToPerformActionOn, actualChannel, message, channel);
                    } else {
                        messagesService.sendMessageNotEnoughRights(channel);
                    }
                    break;
                case "unban":
                    if (isAdmin) {
                        unbanUser(comment, guild, user, message, actualChannel, channel);
                    } else {
                        messagesService.sendMessageNotEnoughRights(channel);
                    }
                    break;
                case "unmute":
                    unmuteUser(comment, guild, memberToPerformActionOn, message, actualChannel, channel);
                    break;
            }
        } catch (IndexOutOfBoundsException e) {
            messagesService.sendBotMessage(actualChannel, "L'utilisateur " + user + " n'a pas été trouvé ! =(");
            return;
        }
    }

    private void banUser(String duration, String comment, Guild guild, Member memberToBan, MessageChannel channel, Message message, MessageChannel originChannel) {
        if (memberToBan != null) {
            guild.ban(memberToBan, 0, comment).complete();
            StringBuilder sb = new StringBuilder("L'utilisateur **" + memberToBan.getEffectiveName() + "** a été banni");

            if (!"".equals(duration)) {
                try {
                    long durationLong = Long.parseLong(duration);
                    if (durationLong != 0L) {
                        sb.append(" pour une période de " + durationLong + " minutes");
                        guild.unban(memberToBan.getUser()).queueAfter(durationLong, TimeUnit.MINUTES);
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

    private void kickUser(String comment, Guild guild, Member memberToKick, MessageChannel channel, Message message, MessageChannel originChannel) {
        if (memberToKick != null) {
            guild.kick(memberToKick, comment).complete();

            StringBuilder sb = new StringBuilder("L'utilisateur **" + memberToKick.getEffectiveName() + "** a été kické !");
            messagesService.sendBotMessage(originChannel, sb.toString());
            messagesService.sendBotMessage(channel, sb.toString() + " Raison : " + comment);
            sendPrivateMessage(memberToKick.getUser(), "Vous avez été kické ! " + RETOUR_LIGNE + "La raison : " + comment);

            originChannel.deleteMessageById(message.getId()).complete();
        }
    }

    private void muteUser(String duration, String comment, Guild guild, Member memberToMute, MessageChannel channel, Message message, MessageChannel originChannel) {
        if (memberToMute != null) {
            guild.mute(memberToMute, true).complete();

            List<Role> muted = guild.getRolesByName("Muted", true);
            if (!muted.isEmpty()) {
                Role mutedRole = muted.get(0);
                guild.addRoleToMember(memberToMute, mutedRole).complete();
                StringBuilder sb = new StringBuilder("L'utilisateur **" + memberToMute.getEffectiveName() + "** a été muté");
                if (!"".equals(duration)) {
                    try {
                        long durationLong = Long.parseLong(duration);

                        sb.append(" pour une période de " + durationLong + " minutes");

                        guild.removeRoleFromMember(memberToMute, mutedRole).queueAfter(durationLong, TimeUnit.MINUTES);
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

    private void unbanUser(String comment, Guild guild, String user, Message message, MessageChannel channel, MessageChannel originChannel) {
        if (user != null) {
            User userObject = guild.getJDA().getUsersByName(user, true).get(0);
            guild.unban(userObject);

            StringBuilder sb = new StringBuilder("L'utilisateur **" + userObject.getName() + "** été \"dé-banni\" !");
            messagesService.sendBotMessage(originChannel, sb.toString());
            messagesService.sendBotMessage(channel, sb.toString() + " Raison : " + comment);
            sendPrivateMessage(userObject, "Vous avez été dé-banni ! " + RETOUR_LIGNE + "La raison : " + comment);

            originChannel.deleteMessageById(message.getId()).complete();
        }
    }

    private void unmuteUser(String comment, Guild guild, Member member, Message message, MessageChannel channel, MessageChannel originChannel) {
        if (member != null) {
            List<Role> muted = guild.getRolesByName("Muted", true);
            if (!muted.isEmpty()) {
                Role mutedRole = muted.get(0);
                guild.mute(member, false);
                guild.removeRoleFromMember(member, mutedRole).complete();

                StringBuilder sb = new StringBuilder("L'utilisateur **" + member.getEffectiveName() + "** été \"dé-muté\" !");
                messagesService.sendBotMessage(originChannel, sb.toString());
                messagesService.sendBotMessage(channel, sb.toString() + " Raison : " + comment);
                sendPrivateMessage(member.getUser(), "Vous avez été dé-muté ! " + RETOUR_LIGNE + "La raison : " + comment);
                originChannel.deleteMessageById(message.getId()).complete();
            }
        }
    }

    private void removeGlobalRank(User author, MessageChannel channel, Guild guild, String commandeComplete, String arg, Role adminRole) {
        String[] rolesToRemoveTable = arg.split(ROLES_SEPARATOR);

        Set<Role> listRolesToRemove = createListOfRoleFromStringTable(rolesToRemoveTable, guild, channel);

        for (Member member : guild.getMembers()) {
            if (member.getRoles().contains(adminRole) || member.getUser().isBot()) {
                continue;
            }
            for (Role roleToAdd : listRolesToRemove) {
                guild.removeRoleFromMember(member, roleToAdd).complete();
            }
        }

        if (!listRolesToRemove.isEmpty()) {
            StringBuilder sb = buildRemoveGlobalRanksMessage(author, commandeComplete, listRolesToRemove);
            messagesService.sendBotMessage(channel, sb.toString());
        }
    }

    private void addGlobalRank(User author, MessageChannel channel, Guild guild, String commandeComplete, String arg, Role adminRole) {
        String[] rolesToAddTable = arg.split(ROLES_SEPARATOR);

        Set<Role> listRolesToAdd = createListOfRoleFromStringTable(rolesToAddTable, guild, channel);

        for (Member member : guild.getMembers()) {
            if (member.getRoles().contains(adminRole) || member.getUser().isBot()) {
                continue;
            }

            for (Role roleToAdd : listRolesToAdd) {
                guild.addRoleToMember(member, roleToAdd).complete();
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

    private void listOnJoinRanks(Guild guild, User author, MessageChannel channel, String commandeComplete) {
        Set<Role> onJoinRanks = new HashSet<>(getOnJoinRanksForGuild(guild));

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

    private void removeOnJoinRank(User author, MessageChannel channel, Guild guild, String commandeComplete, String arg) {
        String[] rolesToRemoveTable = arg.split(ROLES_SEPARATOR);

        Set<Role> listRolesToRemove = createListOfRoleFromStringTable(rolesToRemoveTable, guild, channel);

        Set<Role> onJoinRanks = getOnJoinRanksForGuild(guild);

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

    private void addOnJoinRank(User author, MessageChannel channel, Guild guild, String commandeComplete, String arg) {
        String[] rolesToAddTable = arg.split(ROLES_SEPARATOR);
        Set<Role> listRolesToAdd = createListOfRoleFromStringTable(rolesToAddTable, guild, channel);
        Set<Role> onJoinRanks = getOnJoinRanksForGuild(guild);

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

    private StringBuilder buildNewLinkedRankMessage(Set<Role> linkedRoles, Role role) {
        StringBuilder sb = new StringBuilder();
        sb.append("Le rôle " + role.getName() + " sera désormais assigné lorsqu'un membre s'affectera un des rôles suivants : " + RETOUR_LIGNE);

        for (Role linkedRole : sortSetOfRolesToList(linkedRoles)) {
            sb.append(linkedRole.getName()).append(RETOUR_LIGNE);
        }

        return sb;
    }

    private void setOnJoinRanksForGuild(Guild guild, Set<Role> onJoinRanks) {
        if (onJoinRanksByGuild == null) {
            onJoinRanksByGuild = new HashMap<>();
        }
        onJoinRanksByGuild.put(guild, onJoinRanks);
        globalDao.saveOnJoinRanksForGuild(guild, onJoinRanks);
    }

    private Set<Role> getOnJoinRanksForGuild(Guild guild) {
        if (onJoinRanksByGuild.isEmpty() && !onJoinRanksByIdsByGuild.isEmpty()) {
            onJoinRanksByGuild = transformRanksIdsToObjects(guild, onJoinRanksByIdsByGuild);
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

    private void listDynoActionsForGuild(Guild guild, MessageChannel channel) {
        List<String> dynoActions = mapDynoActions.get(guild);

        if (dynoActions != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Liste des commandes Dyno ignorées : ").append(RETOUR_LIGNE);

            for (String dynoAction : dynoActions) {
                sb.append(dynoAction).append(RETOUR_LIGNE);
            }

            messagesService.sendBotMessage(channel, sb.toString());
        } else {
            messagesService.sendBotMessage(channel, "Aucune commande Dyno ignorée de façon spécifique pour ce serveur.");
        }
    }

    private void calculateSRandMedals(MessageChannel channel, String[] args) {

        if (args.length < 1 || (args.length == 1 && "help".equals(args[0]))) {
            StringBuilder sb = new StringBuilder("**__Aide de la commande ?revive : __**" + RETOUR_LIGNE);
            sb.append(BALISE_BLOCK_CODE + "?revive <STAGE_ATTEINT> <BONUS_DE_MEDAILLES> (<TEMPS_DE_RUN_EN_MIN>) :").append(RETOUR_LIGNE);
            sb.append(TAB + "- Si le temps de run N'EST PAS renseigné : Permet de connaître le nombre de médailles gagnées lors de la résurrection.").append(RETOUR_LIGNE);
            sb.append(TAB + "- Si le temps EST renseigné : Permet de connaître les médailles par minute que génère ce run." + BALISE_BLOCK_CODE);

            messagesService.sendNormalBotMessage(channel, sb.toString());
            return;
        }

        String[] splittedArgs = args[0].split(" ");

        Integer stage = Integer.valueOf(splittedArgs[0]);

        if (stage % 10 != 0) {
            stage = BigDecimal.valueOf(stage).setScale(-1, BigDecimal.ROUND_HALF_DOWN).intValue();
        }

        Integer medalBonus = Integer.valueOf(splittedArgs[1]);

        Integer stageForMedalGain = new BigDecimal(stage).divide(new BigDecimal(100l), 0, RoundingMode.HALF_DOWN).multiply(new BigDecimal(100l)).intValue();

        BigDecimal medalGain = globalDao.getMedalGainForStage(stageForMedalGain);
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

    private void addVoiceRole(Guild guild, Member member, User author, MessageChannel channel, String commandeComplete, String arg) {
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
            if (!guild.getRolesByName(roleName, true).isEmpty()) {
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

    private void manageAddDynoAction(Guild guild, MessageChannel channel, String arg) {

        if (arg.contains(ROLES_SEPARATOR)) {
            String[] actions = arg.split(ROLES_SEPARATOR);
            for (String action : actions) {
                addDynoAction(guild, channel, action.trim());
            }
        } else {
            String action = arg.trim();
            addDynoAction(guild, channel, action);
        }
    }

    private void manageDeleteCustomReaction(Guild guild, MessageChannel channel, String arg) {
        String[] fullParams = arg.split(ACTION_ARGS_SEPARATOR, 1);
        String keyWord = fullParams[0];
        deleteCustomReaction(guild, channel, keyWord);
    }

    private void manageDeleteDynoAction(Guild guild, MessageChannel channel, String arg) {
        String action = arg.trim();
        deleteDynoAction(guild, channel, action);
    }

    @Deprecated
    private void manageCadavreExquis(Guild guild, MessageChannel channel, String arg) {
        String[] params = arg.split(ACTION_ARGS_SEPARATOR, 2);
        EnumCadavreExquisParams param = EnumCadavreExquisParams.valueOf(params[0].toUpperCase());
        executeCadavreActions(guild, channel, params, param);
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

    private void searchUsersWithRole(Guild guild, MessageChannel channel, String roleToSearch) {
        List<Role> foundWithRoles = guild.getRolesByName(roleToSearch, true);
        if (foundWithRoles != null && !foundWithRoles.isEmpty()) {
            Role seekRole = foundWithRoles.get(0);
            List<Member> membersWithRole = guild.getMembersWithRoles(seekRole);
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

    private void searchUsersWithoutRole(Guild guild, MessageChannel channel, String roleToSearch) {
        List<Role> foundWithoutRoles = guild.getRolesByName(roleToSearch, true);
        if (foundWithoutRoles != null && !foundWithoutRoles.isEmpty()) {
            Role seekRole = foundWithoutRoles.get(0);
            List<Member> membersWithRole = guild.getMembersWithRoles(seekRole);
            List<Member> allMembers = guild.getMembers();

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
    private void executeCadavreActions(Guild guild, MessageChannel channel, String[] params, EnumCadavreExquisParams param) {
        switch (param) {
            case ADD_SUJET:
                addSujetCadavre(guild, params[1]);
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

            String sb = SINGLE_SPACE + listCadavreAction.get(new Random().nextInt(listCadavreAction.size())) + SINGLE_SPACE
                    + listCadavreComplement.get(new Random().nextInt(listCadavreComplement.size())) + SINGLE_SPACE
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

    private void addSujetCadavre(Guild guild, String param) {
        if (param != null) {
            Member cadavreSujet = guild.getMembersByName(param, false).get(0);
            if (listCadavreSujet == null) {
                listCadavreSujet = new ArrayList<>();
            }

            listCadavreSujet.add(cadavreSujet);
        }
    }

    private void executeCustomReaction(Member member, Message message, Guild guild, MessageChannel channel, String arg, String action, Role adminRole, Role modoRole) {
        CustomReactionDTO customReaction = mapCustomReactions.get(guild).get(action);
        String[] params = (arg == null || arg.isEmpty()) ? new String[0] : arg.trim().split(PARAMETERS_SEPARATOR + "+");

        boolean isAdmin = adminRole != null ? member.getRoles().contains(adminRole) : member.isOwner();
        boolean isModo = modoRole != null ? member.getRoles().contains(modoRole) : member.isOwner();
        boolean isCmds = "cmds".equals(channel.getName());

        if (isCmds || isAdmin || isModo) {
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
                            sendFormattedCustomReactionAndDeleteCommand(message, guild, channel, customReaction, params);
                            return;
                        } else {
                            messagesService.sendBotMessage(channel, "Non, toi t'es pas une princesse. Au boulot ! Va farmer tes médailles !");
                            return;
                        }
                    }
                }
                if ("aide".equals(action) || "mod".equals(action) || "admin".equals(action)) {
                    sendFormattedCustomReaction(message, guild, channel, customReaction, params);
                } else {
                    sendFormattedCustomReactionAndDeleteCommand(message, guild, channel, customReaction, params);
                }
            }
        }
    }

    private void sendFormattedCustomReactionAndDeleteCommand(Message message, Guild guild, MessageChannel channel, CustomReactionDTO customReaction, String[] params) {
        sendCustomReaction(guild, channel, customReaction, params);
        channel.deleteMessageById(message.getId()).complete();
    }

    private void sendCustomReaction(Guild guild, MessageChannel channel, CustomReactionDTO customReaction, String[] params) {
        String reactionReplaced = customReaction.getReaction();
        for (String param : params) {

            if (param.startsWith(PREFIX_TAG)) {
                String effectiveName = param.replace(PREFIX_TAG, "");
                List<Member> membersFound = guild.getMembersByEffectiveName(effectiveName, false);
                if (membersFound != null && !membersFound.isEmpty()) {
                    Member memberFound = membersFound.get(0);
                    param = memberFound.getAsMention();
                }
            }

            reactionReplaced = reactionReplaced.replaceFirst("\\$[0-9]+", param);
        }

        reactionReplaced = messagesService.replaceChannel(reactionReplaced, guild);
        messagesService.sendBotMessage(channel, reactionReplaced);
    }

    private void sendFormattedCustomReaction(Message message, Guild guild, MessageChannel channel, CustomReactionDTO customReaction, String[] params) {
        sendCustomReaction(guild, channel, customReaction, params);
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

    private void addDynoAction(Guild guild, MessageChannel channel, String dynoAction) {

        if (mapDynoActions == null) {
            mapDynoActions = new HashMap<>();
        }

        for (EnumDynoAction action : EnumDynoAction.values()) {
            if (dynoAction.equalsIgnoreCase(action.name())) {
                messagesService.sendBotMessage(channel, "La commande " + dynoAction + " est déjà ignorée Deathbot.");
                return;
            }
        }

        List<String> mapDynoActionsForGuild = mapDynoActions.get(guild);
        if (mapDynoActionsForGuild == null) {
            mapDynoActionsForGuild = new ArrayList<>();
        }
        mapDynoActionsForGuild.add(dynoAction);
        mapDynoActions.put(guild, mapDynoActionsForGuild);

        globalDao.addDynoAction(dynoAction, guild);

        messagesService.sendBotMessage(channel, "Nouvelle commande Dyno ignorée : " + dynoAction);
    }

    private void deleteDynoAction(Guild guild, MessageChannel channel, String action) {
        List<String> mapDynoActionsForGuild = mapDynoActions.get(guild);
        if (mapDynoActionsForGuild != null) {
            if (mapDynoActionsForGuild.remove(action)) {
                boolean deleted = globalDao.deleteDynoAction(action, guild);
                if (!deleted) {
                    messagesService.sendBotMessage(channel,
                            "La commande Dyno \"" + action + "\" n'a pas pu être supprimée de la liste des commandes à ignorer. Merci de contacter l'administrateur.");
                    return;
                }
                messagesService.sendBotMessage(channel, "La commande Dyno \"" + action + "\" a été supprimée de la liste des commandes à ignorer.");
            } else {
                messagesService.sendBotMessage(channel, "Aucune commande Dyno ignorée n'a été trouvée pour le mot-clé : " + action);
            }
        } else {
            messagesService.sendBotMessage(channel, "Aucune commande Dyno ignorée pour votre serveur !");
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

    private void listAssignableRanks(Guild guild, User author, MessageChannel channel, String commandeComplete) {
        Set<Role> selfAssignableRanks = new HashSet<>(getSelfAssignableRanksForGuild(guild));
        Set<Role> notSingleSelfAssignableRanksForGuild = getNotSingleSelfAssignableRanksForGuild(guild);
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

    private void removeAssignableRanks(User author, MessageChannel channel, Guild guild, String commandeComplete, String arg) {
        String[] rolesToRemoveTable = arg.split(ROLES_SEPARATOR);

        Set<Role> listRolesToRemove = createListOfRoleFromStringTable(rolesToRemoveTable, guild, channel);

        Set<Role> selfAssignableRanks = getSelfAssignableRanksForGuild(guild);

        if (selfAssignableRanks != null && !selfAssignableRanks.isEmpty()) {
            selfAssignableRanks.removeAll(listRolesToRemove);
        } else {
            selfAssignableRanks = new HashSet<>();
        }

        setAssignableRanksForGuild(guild, selfAssignableRanks);
        sendMessageOfRemovedRanks(author, channel, commandeComplete, listRolesToRemove);
    }

    private void removeNotSingleAssignableRanks(User author, MessageChannel channel, Guild guild, String commandeComplete, String arg) {
        String[] rolesToRemoveTable = arg.split(ROLES_SEPARATOR);

        Set<Role> listRolesToRemove = createListOfRoleFromStringTable(rolesToRemoveTable, guild, channel);

        Set<Role> selfAssignableRanks = getNotSingleSelfAssignableRanksForGuild(guild);

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

    private Set<Role> getSelfAssignableRanksForGuild(Guild guild) {
        if (selfAssignableRanksByGuild.isEmpty() && !selfAssignableRanksByIdsByGuild.isEmpty()) {
            selfAssignableRanksByGuild = transformRanksIdsToObjects(guild, selfAssignableRanksByIdsByGuild);
        }

        return (selfAssignableRanksByGuild != null && !selfAssignableRanksByGuild.isEmpty()) ? selfAssignableRanksByGuild.get(guild) : new HashSet<>();
    }

    private Set<Role> getNotSingleSelfAssignableRanksForGuild(Guild guild) {
        if (notSingleSelfAssignableRanksByGuild.isEmpty() && !notSingleSelfAssignableRanksByIdsByGuild.isEmpty()) {
            notSingleSelfAssignableRanksByGuild = transformRanksIdsToObjects(guild, notSingleSelfAssignableRanksByIdsByGuild);
        }

        return (notSingleSelfAssignableRanksByGuild != null && !notSingleSelfAssignableRanksByGuild.isEmpty()) ? notSingleSelfAssignableRanksByGuild.get(guild) : new HashSet<>();
    }

    private HashMap<String, CustomReactionDTO> getCustomReactionsForGuild(Guild guild) {
        if (mapCustomReactions.isEmpty() && !mapCustomReactionsByGuild.isEmpty()) {
            mapCustomReactions = transformCustomReactionsGuildNamesToGuild(guild, mapCustomReactionsByGuild);
        }
        return (mapCustomReactions != null && !mapCustomReactions.isEmpty()) ? mapCustomReactions.get(guild) : new HashMap<>();
    }

    private List<String> getDynoActionsForGuild(Guild guild) {

        if (mapDynoActions.isEmpty() && !mapDynoActionsByGuild.isEmpty()) {
            mapDynoActions = transformDynoActionsGuildNamesToGuild(guild, mapDynoActionsByGuild);
        }
        return (mapDynoActions != null && !mapDynoActions.isEmpty()) ? mapDynoActions.get(guild) : new ArrayList<>();
    }

    private HashMap<String, String> getVoiceRolesForGuild(Guild guild) {
        if (mapVoiceRole.isEmpty() && !mapVoiceRoleByGuild.isEmpty()) {
            mapVoiceRole = transformVoiceRolesGuildNamesToGuild(guild, mapVoiceRoleByGuild);
        }
        return (mapVoiceRole != null && !mapVoiceRole.isEmpty()) ? mapVoiceRole.get(guild) : new HashMap<>();
    }

    private HashMap<Guild, HashMap<String, String>> transformVoiceRolesGuildNamesToGuild(Guild messageGuild, HashMap<String, HashMap<String, String>> mapVoiceRoleByGuild) {
        HashMap<Guild, HashMap<String, String>> resultMap = new HashMap<>();

        for (String guildName : mapVoiceRoleByGuild.keySet()) {
            Guild guild = messageGuild.getJDA().getGuildsByName(guildName, true).get(0);

            HashMap<String, String> voiceRolesMap = mapVoiceRoleByGuild.get(guildName);

            resultMap.put(guild, voiceRolesMap);
        }
        return resultMap;
    }

    private HashMap<Guild, HashMap<String, CustomReactionDTO>> transformCustomReactionsGuildNamesToGuild(Guild messageGuild,
            HashMap<String, HashMap<String, CustomReactionDTO>> mapCustomReactionsByGuild) {
        HashMap<Guild, HashMap<String, CustomReactionDTO>> resultMap = new HashMap<>();

        for (String guildName : mapCustomReactionsByGuild.keySet()) {
            Guild guild = messageGuild.getJDA().getGuildsByName(guildName, true).get(0);

            HashMap<String, CustomReactionDTO> customReactionMap = mapCustomReactionsByGuild.get(guildName);

            resultMap.put(guild, customReactionMap);
        }
        return resultMap;
    }

    private HashMap<Guild, List<String>> transformDynoActionsGuildNamesToGuild(Guild messageGuild, HashMap<String, List<String>> mapDynoActionsByGuild) {
        HashMap<Guild, List<String>> resultMap = new HashMap<>();

        for (String guildName : mapDynoActionsByGuild.keySet()) {
            Guild guild = messageGuild.getJDA().getGuildsByName(guildName, true).get(0);

            List<String> dynoActionsMap = mapDynoActionsByGuild.get(guildName);

            resultMap.put(guild, dynoActionsMap);
        }
        return resultMap;
    }

    private HashMap<Guild, Set<Role>> transformRanksIdsToObjects(Guild messageGuild, HashMap<String, Set<String>> ranksByIdsByGuild) {

        HashMap<Guild, Set<Role>> resultMap = new HashMap<>();

        for (String guildName : ranksByIdsByGuild.keySet()) {
            List<Guild> guilds = messageGuild.getJDA().getGuildsByName(guildName, true);
            if (!guilds.isEmpty()) {
                Guild guild = messageGuild.getJDA().getGuildsByName(guildName, true).get(0);

                Set<String> roleNames = ranksByIdsByGuild.get(guildName);
                Set<Role> roles = new HashSet<>();
                for (String roleName : roleNames) {

                    List<Role> rolesByName = guild.getRolesByName(roleName, true);
                    if (rolesByName != null && !rolesByName.isEmpty()) {
                        Role role = rolesByName.get(0);
                        roles.add(role);
                    }
                }

                resultMap.put(guild, roles);
            }
        }

        return resultMap;
    }

    private HashMap<Guild, HashMap<Role, Set<Role>>> transformHashRanksIdsToObjects(Guild messageGuild, HashMap<String, HashMap<String, Set<String>>> ranksByIdsByGuild) {

        HashMap<Guild, HashMap<Role, Set<Role>>> resultMap = new HashMap<>();

        for (String guildName : ranksByIdsByGuild.keySet()) {
            List<Guild> guilds = messageGuild.getJDA().getGuildsByName(guildName, true);
            if (!guilds.isEmpty()) {
                Guild guild = messageGuild.getJDA().getGuildsByName(guildName, true).get(0);

                HashMap<String, Set<String>> roleNames = ranksByIdsByGuild.get(guildName);
                HashMap<Role, Set<Role>> linkedRoles = new HashMap<>();
                for (String roleName : roleNames.keySet()) {
                    Set<Role> roles = new HashSet<>();
                    for (String linkedRole : roleNames.get(roleName)) {
                        Role role = messageGuild.getRolesByName(linkedRole, true).get(0);
                        roles.add(role);
                    }
                    Role role = messageGuild.getRolesByName(roleName, true).get(0);
                    linkedRoles.put(role, roles);
                }
                resultMap.put(guild, linkedRoles);
            }
        }

        return resultMap;
    }

    private void addAssignableRanks(User author, MessageChannel channel, Guild guild, String commandeComplete, String arg) {
        String[] rolesToAddTable = arg.split(ROLES_SEPARATOR);

        Set<Role> listRolesToAdd = createListOfRoleFromStringTable(rolesToAddTable, guild, channel);
        Set<Role> selfAssignableRanks = getSelfAssignableRanksForGuild(guild);

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

    private void addNotSingleAssignableRanks(User author, MessageChannel channel, Guild guild, String commandeComplete, String arg) {
        String[] rolesToAddTable = arg.split(ROLES_SEPARATOR);

        Set<Role> listRolesToAdd = createListOfRoleFromStringTable(rolesToAddTable, guild, channel);
        Set<Role> selfAssignableRanks = getNotSingleSelfAssignableRanksForGuild(guild);

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

    private void addReminder(MessageChannel channel, Guild guild, String[] arg) {

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
        sb.append(BALISE_BLOCK_CODE + "?add_reminder <NOM_DU_REMINDER_A_CREER>;<SALON_DE_DESTINATION>;<TEXTE_A_AFFICHER>;<CRONTAB>```").append(RETOUR_LIGNE);
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

    private void removeReminder(MessageChannel channel, Guild guild, String arg) {

        String[] reminderParams = arg.split(REMINDER_SEPARATOR);

        if (reminderParams.length != 1) {
            messagesService.sendBotMessage(channel,
                    "Le nombre de paramètres n'est pas bon, désolé. Merci de contacter un administrateur ou de consulter l'aide (argument \"help\").");
            return;
        } else {
            if ("help".equals(reminderParams[0])) {
                StringBuilder sb = new StringBuilder("*** Syntaxe de la commande remove_reminder : ***").append(RETOUR_LIGNE);
                sb.append(BALISE_BLOCK_CODE + "?remove_reminder <NOM_DU_REMINDER_A_SUPPRIMER>```").append(RETOUR_LIGNE);
                sb.append("*NB : Les noms peuvent être retrouvés grâce à la commande ?list_reminders*");

                messagesService.sendNormalBotMessage(channel, sb.toString());
                return;
            } else {
                String title = reminderParams[0];

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

    private void listReminders(MessageChannel channel, Guild guild) {

        HashMap<String, ReminderDTO> reminders = Bot.reminderThreadsByGuild.get(guild.getName()).getReminders();

        if (reminders.isEmpty()) {
            messagesService.sendBotMessage(channel, "Aucun reminder mis en place pour le moment !");
            return;
        } else {

            StringBuilder sb = new StringBuilder();
            sb.append("Les reminders suivants sont actuellement actifs : ").append(RETOUR_LIGNE);
            for (ReminderDTO reminder : reminders.values()) {
                sb.append(TAB + " - Nom : ").append(reminder.getTitle()).append(", Salon : ").append(reminder.getChan()).append(", Expression Cron : ").append(
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

    private Set<Role> createListOfRoleFromStringTable(String[] rolesToAddTable, Guild guild, MessageChannel channel) {

        Set<Role> listRole = new HashSet<>();

        for (String roleToAddString : rolesToAddTable) {
            Role roleToAdd;
            try {
                roleToAdd = guild.getRolesByName(roleToAddString, true).get(0);
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

    private void manageRankCmd(User author, MessageChannel channel, Guild guild, String rankToAdd, Member member, boolean removePreviousRank) {
        if ("".equals(rankToAdd) || "help".equals(rankToAdd)) {
            listRanksOfUser(channel, member);
        } else {
            List<Role> userRoles = member.getRoles();
            List<Role> userAssignableRoles = findAssignableRole(userRoles, guild);
            addOrRemoveRankToUser(author, channel, guild, rankToAdd, member, userRoles, userAssignableRoles, removePreviousRank);
        }
    }

    private void addOrRemoveRankToUser(User author, MessageChannel channel, Guild guild, String rankToAdd, Member member, List<Role> userRoles, List<Role> userAssignableRoles,
            boolean removePreviousRank) {
        List<Role> potentialRolesToAdd = guild.getRolesByName(rankToAdd, true);
        if (!potentialRolesToAdd.isEmpty()) {
            Role roleToAdd = potentialRolesToAdd.get(0);

            Set<Role> selfAssignableRanks = getSelfAssignableRanksForGuild(guild);
            Set<Role> notSingleSelfAssignableRanks = getNotSingleSelfAssignableRanksForGuild(guild);

            boolean isSingle = selfAssignableRanks != null && selfAssignableRanks.contains(roleToAdd);
            boolean isNotSingle = notSingleSelfAssignableRanks != null && notSingleSelfAssignableRanks.contains(roleToAdd);

            if (isSingle || isNotSingle) {
                StringBuilder messageBuilder = new StringBuilder();
                if (!userRoles.contains(roleToAdd) || !removePreviousRank) {
                    member = addRankToUser(guild, member, userAssignableRoles, roleToAdd, messageBuilder, isSingle, removePreviousRank, userRoles, channel, author);
                } else {
                    member = guild.retrieveMember(member.getUser()).complete();
                    member = removeRankToUser(guild, member, roleToAdd, messageBuilder);
                    messagesService.sendBotMessageWithMention(channel, messageBuilder.toString(), author);
                }

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

                if (removePreviousRank) {
                    messagesService.sendBotMessage(channel, "Les rôles 'Chevalier' vont de 10 en 10 !"
                            + RETOUR_LIGNE
                            + "Comme je suis sympa, j'ai corrigé votre commande par `?rank "
                            + rankToAdd
                            + "`."
                            + RETOUR_LIGNE
                            + "La prochaine fois, pensez à arrondir à la dizaine inférieure ;) ");
                }
                member = guild.retrieveMember(member.getUser()).complete();
                addOrRemoveRankToUser(author, channel, guild, rankToAdd, member, userRoles, userAssignableRoles, removePreviousRank);
            } else {
                messagesService.sendBotMessage(channel, "Le rôle **" + rankToAdd + "** n'existe pas.");
            }
        }
    }

    private Member removeRankToUser(Guild guild, Member member, Role roleToAdd, StringBuilder messageBuilder) {
        guild.removeRoleFromMember(member, roleToAdd).complete();

        removeLinkedRankToMember(guild, member, roleToAdd);

        member = guild.retrieveMember(member.getUser()).complete();
        messageBuilder.append("Vous n'êtes plus **").append(roleToAdd.getName()).append("** !");
        return member;
    }

    private void removeLinkedRankToMember(Guild guild, Member member, Role roleToAdd) {
        HashMap<Role, Set<Role>> linkedRoles = getLinkedRanksForGuild(guild);
        for (Role linkedRole : linkedRoles.keySet()) {
            for (Role role : linkedRoles.get(linkedRole)) {
                if (roleToAdd.equals(role)) {
                    System.out.println("Suppression du rôle lié " + linkedRole + " pour le membre " + member.getEffectiveName());
                    guild.removeRoleFromMember(member, linkedRole).complete();
                    break;
                }
            }
        }
    }

    private Member addRankToUser(Guild guild, Member member, List<Role> userAssignableRoles, Role roleToAdd, StringBuilder messageBuilder, boolean isSingle,
            boolean removePreviousRank, List<Role> userRoles, MessageChannel channel, User author) {

        if (isSingle && getSingleRoleForGuild(guild) && !userAssignableRoles.isEmpty()) {

            for (Role role : userAssignableRoles) {
                System.out.println("Le rôle suivant est retiré du membre " + member.getEffectiveName() + " : " + role.getName());
            }

            for (Role userAssignableRole : userAssignableRoles) {
                guild.removeRoleFromMember(member, userAssignableRole).complete();
            }

            for (Role roleToRemove : userAssignableRoles) {
                removeLinkedRankToMember(guild, member, roleToRemove);
            }
            member = guild.retrieveMember(member.getUser()).complete();
        }

        if (removePreviousRank || !userRoles.contains(roleToAdd)) {
            messageBuilder.append("Vous êtes passé **").append(roleToAdd.getName()).append("** !");
            if (removePreviousRank) {
                messagesService.sendBotMessageWithMention(channel, messageBuilder.toString(), author);
            } else {
                messagesService.sendBotMessage(channel, messageBuilder.toString());
            }
        }

        guild.addRoleToMember(member, roleToAdd).complete();

        addLinkedRankToMember(guild, member, roleToAdd);

        member = guild.retrieveMember(member.getUser()).complete();

        return member;
    }

    private void addLinkedRankToMember(Guild guild, Member member, Role roleToAdd) {
        HashMap<Role, Set<Role>> linkedRoles = getLinkedRanksForGuild(guild);
        for (Role linkedRole : linkedRoles.keySet()) {
            for (Role role : linkedRoles.get(linkedRole)) {
                if (roleToAdd.equals(role)) {
                    guild.addRoleToMember(member, linkedRole).complete();
                    break;
                }
            }
        }
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

        sb.append(RETOUR_LIGNE).append("Afin de connaître la liste des rôles assignables, vous pouvez taper la commande ?list.");
        messagesService.sendBotMessage(channel, sb.toString());
    }

    private List<Role> findAssignableRole(List<Role> userRoles, Guild guild) {

        Set<Role> selfAssignableRanks = getSelfAssignableRanksForGuild(guild);

        return userRoles.stream().filter(userRole -> selfAssignableRanks != null && selfAssignableRanks.contains(userRole)).collect(Collectors.toList());
    }
}
