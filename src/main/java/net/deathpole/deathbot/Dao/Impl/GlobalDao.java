package net.deathpole.deathbot.Dao.Impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import net.deathpole.deathbot.CustomReactionDTO;
import net.deathpole.deathbot.ReminderDTO;
import net.deathpole.deathbot.Dao.IGlobalDao;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

/**
 * Created by nicolas on 24/10/17.
 */
public class GlobalDao implements IGlobalDao {

    public GlobalDao() {
        try (Connection conn = getConnectionToDB()){
            Statement st = conn.createStatement();

            InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream("script/initDB.sql");

            Scanner s = new Scanner(script).useDelimiter("\\A");
            String scriptContent = s.hasNext() ? s.next() : "";

            String[] inst = scriptContent.split(";");

            System.out.println("DeathbotExecution : Init de bdd : \r\n");
            for (int i = 0; i < inst.length; i++) {
                if (!inst[i].trim().equals("")) {
                    st.executeUpdate(inst[i]);
                    System.out.println("DeathbotExecution : >>>> " + inst[i]);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String read(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
    }

    private static Connection getConnection() throws URISyntaxException, SQLException {
        String dbUrl = System.getenv("JDBC_DATABASE_URL");
        // String dbUrl = "jdbc:postgresql://localhost:5432/deathbot?user=postgres&password=postgres";
        return DriverManager.getConnection(dbUrl);
    }

    @Override
    public Connection getConnectionToDB() {
        Connection conn =  null;
        try {
            Class.forName("org.postgresql.Driver");
            conn = getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return conn;
    }


    @Override
    public HashMap<String, Set<String>> initAssignableRanksbyGuild(boolean single) {

        Connection conn = getConnectionToDB();

        HashMap<String, Set<String>> resultMap = new HashMap<>();

        try {
            Statement statement = conn.createStatement();
            String sql = "SELECT * FROM ASSIGNABLE_RANKS WHERE SINGLE = " + single + " ORDER BY GUILD_NAME ";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                String guildName = rs.getString("GUILD_NAME");
                String roleName = rs.getString("ROLE_NAME");

                Set<String> rolesForGuild = resultMap.get(guildName);
                if (rolesForGuild == null) {
                    rolesForGuild = new HashSet<>();
                }

                rolesForGuild.add(roleName);
                resultMap.put(guildName, rolesForGuild);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return resultMap;
    }

    @Override
    public void saveAssignableRanksForGuild(Guild guild, Set<Role> selfAssignableRanks, boolean single) {
        Connection conn = getConnectionToDB();

        try {
            Statement statement = conn.createStatement();
            String sqlDelete = "DELETE FROM ASSIGNABLE_RANKS WHERE GUILD_NAME = '" + guild.getName() + "' AND ASSIGNABLE_RANKS.SINGLE = " + single;
            int deletedCount = statement.executeUpdate(sqlDelete);
            System.out.println("DeathbotExecution : Nombre de ranks assignables supprimés : " + deletedCount);

            Statement stmnt = conn.createStatement();
            for (Role role : selfAssignableRanks) {
                String sqlInsert = "INSERT INTO ASSIGNABLE_RANKS(GUILD_NAME, ROLE_NAME, SINGLE) VALUES ('" + guild.getName() + "', '" + role.getName() + "', " + single + ")";
                stmnt.addBatch(sqlInsert);
            }

            stmnt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void saveWelcomeMessage(Guild guild, String message) {
        Connection conn = getConnectionToDB();

        message = message == null || message.isEmpty() ? null : message;

        try {

            String sqlUpdate = "UPDATE WELCOME_MESSAGE SET WELCOME_MESSAGE = ? WHERE GUILD_NAME = ?";

            PreparedStatement stmnt = conn.prepareStatement(sqlUpdate);
            stmnt.setString(1, message);
            stmnt.setString(2, guild.getName());
            int count = stmnt.executeUpdate();

            if (count == 0) {
                String sqlInsert = "INSERT INTO WELCOME_MESSAGE(GUILD_NAME, WELCOME_MESSAGE) VALUES(?, ?)";
                stmnt = conn.prepareStatement(sqlInsert);
                stmnt.setString(1, guild.getName());
                stmnt.setString(2, message);

                stmnt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void saveActivationState(Guild guild, boolean activated) {
        Connection conn = getConnectionToDB();

        try {
            Statement stmnt = conn.createStatement();
            String sqlUpdate = "UPDATE ACTIVATION_STATE SET ACTIVATED = " + activated + " WHERE GUILD_NAME = '" + guild.getName() + "'";

            int count = stmnt.executeUpdate(sqlUpdate);

            if (count == 0) {
                Statement statement = conn.createStatement();
                String sqlInsert = "INSERT INTO ACTIVATION_STATE(GUILD_NAME, ACTIVATED) VALUES('" + guild.getName() + "', " + activated + ")";

                statement.executeUpdate(sqlInsert);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean getActivationState(Guild guild) {
        Connection conn = getConnectionToDB();

        boolean activated = false;
        try {
            Statement statement = conn.createStatement();
            String sql = "SELECT * FROM ACTIVATION_STATE WHERE GUILD_NAME = '" + guild.getName() + "'";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                activated = rs.getBoolean("ACTIVATED");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return activated;
    }

    @Override
    public String getWelcomeMessage(Guild guild) {
        Connection conn = getConnectionToDB();

        String welcomeMessage = null;
        try {
            Statement statement = conn.createStatement();
            String sql = "SELECT * FROM WELCOME_MESSAGE WHERE GUILD_NAME = '" + guild.getName() + "'";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                welcomeMessage = rs.getString("WELCOME_MESSAGE");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return welcomeMessage;
    }

    @Override
    public HashMap<String, HashMap<String, CustomReactionDTO>> initMapCustomReactions() {
        Connection conn = getConnectionToDB();

        HashMap<String, HashMap<String, CustomReactionDTO>> resultMap = new HashMap<>();

        try {
            Statement statement = conn.createStatement();
            String sql = "SELECT * FROM CUSTOM_REACTION ORDER BY GUILD_NAME ";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                String guildName = rs.getString("GUILD_NAME");
                String command = rs.getString("COMMAND");
                String reaction = rs.getString("REACTION");
                int noOfParam = rs.getInt("NUMBER_OF_PARAMS");

                HashMap<String, CustomReactionDTO> customReactionForGuild = resultMap.get(guildName);
                if (customReactionForGuild == null) {
                    customReactionForGuild = new HashMap<>();
                }

                CustomReactionDTO cr = new CustomReactionDTO();
                cr.setReaction(reaction);
                cr.setNumberOfParams(noOfParam);

                customReactionForGuild.put(command, cr);
                resultMap.put(guildName, customReactionForGuild);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return resultMap;
    }

    @Override
    public void saveCustomReaction(String keyWord, CustomReactionDTO customReaction, Guild guild) {
        Connection conn = getConnectionToDB();

        try {
            String sqlUpdate = "UPDATE CUSTOM_REACTION " + "SET REACTION = ? ," + "NUMBER_OF_PARAMS = ?" + "WHERE GUILD_NAME = ? " + "AND COMMAND = ?";
            PreparedStatement statement = conn.prepareStatement(sqlUpdate);
            statement.setString(1, customReaction.getReaction());
            statement.setInt(2, customReaction.getNumberOfParams());
            statement.setString(3, guild.getName());
            statement.setString(4, keyWord);

            int count = statement.executeUpdate();

            if (count == 0) {
                String sqlInsert = "INSERT INTO CUSTOM_REACTION(REACTION, NUMBER_OF_PARAMS, GUILD_NAME, COMMAND) VALUES(?, ?, ?, ?)";
                statement = conn.prepareStatement(sqlInsert);

                statement.setString(1, customReaction.getReaction());
                statement.setInt(2, customReaction.getNumberOfParams());
                statement.setString(3, guild.getName());
                statement.setString(4, keyWord);

                statement.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public HashMap<String, ReminderDTO> getRemindersForGuild(Guild guild) {
        Connection conn = getConnectionToDB();

        HashMap<String, ReminderDTO> results = new HashMap<>();

        try {
            String sql = "SELECT * FROM REMINDER WHERE GUILD_NAME = '" + guild.getName() + "'";
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                String title = rs.getString("TITLE");
                String text = rs.getString("TEXT");
                String chan = rs.getString("CHAN");
                String cronTab = rs.getString("CRONTAB");
                Timestamp timestamp = rs.getTimestamp("LAST_EXECUTION_TIME");

                ReminderDTO reminder = new ReminderDTO();
                reminder.setTitle(title);
                reminder.setText(text);
                reminder.setChan(chan);
                reminder.setCronTab(cronTab);
                if (timestamp != null) {
                    reminder.setLastExecutionTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp.getTime()), ZoneId.of("Europe/Paris")));
                }

                results.put(title, reminder);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return results;
    }

    @Override
    public boolean deleteRemindersForGuild(Guild guild, String title) {
        Connection conn = getConnectionToDB();

        HashMap<String, ReminderDTO> results = new HashMap<>();

        try {
            String sql = "DELETE FROM REMINDER WHERE GUILD_NAME = ? AND TITLE = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, guild.getName());
            ps.setString(2, title);
            int count = ps.executeUpdate();

            return count > 0 ? true : false;

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    @Override
    public void saveReminder(ReminderDTO reminder, Guild guild) {
        Connection conn = getConnectionToDB();

        try {
            String sqlUpdate = "UPDATE REMINDER " + "SET TEXT = ?," + "CHAN = ?," + "CRONTAB = ?" + "WHERE GUILD_NAME = ? " + "AND TITLE = ?";
            PreparedStatement statement = conn.prepareStatement(sqlUpdate);
            statement.setString(1, reminder.getText());
            statement.setString(2, reminder.getChan());
            statement.setString(3, reminder.getCronTab());
            statement.setString(4, guild.getName());
            statement.setString(5, reminder.getTitle());

            int count = statement.executeUpdate();

            if (count == 0) {
                String sqlInsert = "INSERT INTO REMINDER(TEXT, CHAN, CRONTAB, GUILD_NAME, TITLE) VALUES(?, ?, ?, ?, ?)";
                statement = conn.prepareStatement(sqlInsert);

                statement.setString(1, reminder.getText());
                statement.setString(2, reminder.getChan());
                statement.setString(3, reminder.getCronTab());
                statement.setString(4, guild.getName());
                statement.setString(5, reminder.getTitle());

                statement.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean deleteCustomReaction(String keyWord, Guild guild) {
        Connection conn = getConnectionToDB();

        try {
            String sqlUpdate = "DELETE FROM CUSTOM_REACTION WHERE GUILD_NAME = ? AND COMMAND = ?";
            PreparedStatement statement = conn.prepareStatement(sqlUpdate);
            statement.setString(1, guild.getName());
            statement.setString(2, keyWord);

            int count = statement.executeUpdate();

            return count > 0 ? true : false;

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public HashMap<String, HashMap<String, String>> initMapVoiceRoles() {
        Connection conn = getConnectionToDB();

        HashMap<String, HashMap<String, String>> resultMap = new HashMap<>();

        try {
            Statement statement = conn.createStatement();
            String sql = "SELECT * FROM VOICE_ROLES ORDER BY GUILD_NAME ";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                String guildName = rs.getString("GUILD_NAME");
                String channelName = rs.getString("CHANNEL_NAME");
                String role = rs.getString("ROLE");

                HashMap<String, String> voiceRolesForGuild = resultMap.get(guildName);
                if (voiceRolesForGuild == null) {
                    voiceRolesForGuild = new HashMap<>();
                }

                voiceRolesForGuild.put(channelName, role);
                resultMap.put(guildName, voiceRolesForGuild);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return resultMap;
    }

    @Override
    public void saveVoiceRole(String channelName, String roleName, Guild guild) {
        Connection conn = getConnectionToDB();

        try {
            String sqlUpdate = "UPDATE voice_roles " + "SET channel_name = ? ," + "role = ?" + "WHERE GUILD_NAME = ? ";
            PreparedStatement statement = conn.prepareStatement(sqlUpdate);
            statement.setString(1, channelName);
            statement.setString(2, roleName);
            statement.setString(3, guild.getName());

            int count = statement.executeUpdate();

            if (count == 0) {
                String sqlInsert = "INSERT INTO voice_roles(channel_name, role, GUILD_NAME) VALUES(?, ?, ?)";
                statement = conn.prepareStatement(sqlInsert);

                statement.setString(1, channelName);
                statement.setString(2, roleName);
                statement.setString(3, guild.getName());

                statement.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public BigDecimal getMedalGainForStage(Integer stage) {
        Connection conn = getConnectionToDB();

        BigDecimal medalGain = null;
        try {
            Statement statement = conn.createStatement();
            String sql = "SELECT * FROM MEDAL_GAIN WHERE STAGE = " + stage;
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                medalGain = rs.getBigDecimal("medals");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return medalGain;
    }

    @Override
    public HashMap<String, Set<String>> initOnJoinRanksbyGuild() {
        Connection conn = getConnectionToDB();

        HashMap<String, Set<String>> resultMap = new HashMap<>();

        try {
            Statement statement = conn.createStatement();
            String sql = "SELECT * FROM ONJOIN_RANKS ORDER BY GUILD_NAME ";
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                String guildName = rs.getString("GUILD_NAME");
                String roleName = rs.getString("ROLE_NAME");

                Set<String> rolesForGuild = resultMap.get(guildName);
                if (rolesForGuild == null) {
                    rolesForGuild = new HashSet<>();
                }

                rolesForGuild.add(roleName);
                resultMap.put(guildName, rolesForGuild);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return resultMap;
    }

    @Override
    public void saveOnJoinRanksForGuild(Guild guild, Set<Role> onJoinRanks) {
        Connection conn = getConnectionToDB();

        try {
            Statement statement = conn.createStatement();
            String sqlDelete = "DELETE FROM ONJOIN_RANKS WHERE GUILD_NAME = '" + guild.getName() + "'";
            int deletedCount = statement.executeUpdate(sqlDelete);
            System.out.println("DeathbotExecution : Nombre de onjoin ranks supprimés : " + deletedCount);

            Statement stmnt = conn.createStatement();
            for (Role role : onJoinRanks) {
                String sqlInsert = "INSERT INTO ONJOIN_RANKS(GUILD_NAME, ROLE_NAME) VALUES ('" + guild.getName() + "', '" + role.getName() + "')";
                stmnt.addBatch(sqlInsert);
            }

            stmnt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void updateExecutedTime(Guild guild, ReminderDTO reminder) {
        Connection conn = getConnectionToDB();

        try {
            String sqlUpdate = "UPDATE reminder " + "SET last_execution_time = ? WHERE GUILD_NAME = ? AND TITLE = ?";
            PreparedStatement statement = conn.prepareStatement(sqlUpdate);
            statement.setTimestamp(1, Timestamp.valueOf(reminder.getLastExecutionTime()));
            statement.setString(2, guild.getName());
            statement.setString(3, reminder.getTitle());

            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
