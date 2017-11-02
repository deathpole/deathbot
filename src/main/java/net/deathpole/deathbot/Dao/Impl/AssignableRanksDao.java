package net.deathpole.deathbot.Dao.Impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.h2.tools.RunScript;
import org.h2.util.IOUtils;

import net.deathpole.deathbot.Dao.IAssignableRanksDao;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

/**
 * Created by nicolas on 24/10/17.
 */
public class AssignableRanksDao implements IAssignableRanksDao {

    public AssignableRanksDao() {
        try (Connection conn = getConnectionToDB()){
            InputStream scriptLocation = Thread.currentThread().getContextClassLoader().getResourceAsStream("script/initDB.sql");
            RunScript.execute(conn, IOUtils.getBufferedReader(scriptLocation));
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public Connection getConnectionToDB() {
        Connection conn =  null;
        try {
            Class.forName("org.h2.Driver");
            conn = DriverManager.getConnection("jdbc:h2:~/bdd_deathbot");
        } catch (SQLException | ClassNotFoundException e) {
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
            String sql = "SELECT * FROM PUBLIC.ASSIGNABLE_RANKS WHERE SINGLE = " + single + " ORDER BY GUILD_NAME ";
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
            String sqlDelete = "DELETE FROM PUBLIC.ASSIGNABLE_RANKS WHERE GUILD_NAME = '" + guild.getName() + "' AND ASSIGNABLE_RANKS.SINGLE = " + single;
            int deletedCount = statement.executeUpdate(sqlDelete);
            System.out.println("Nombre de ranks asisgnables supprimés : " + deletedCount);

            conn.commit();

            Statement stmnt = conn.createStatement();
            for (Role role : selfAssignableRanks) {
                String sqlInsert = "INSERT INTO PUBLIC.ASSIGNABLE_RANKS(GUILD_NAME, ROLE_NAME, SINGLE) VALUES ('" + guild.getName() + "', '" + role.getName() + "', " + single + ")";
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
    public void saveActivationState(Guild guild, boolean activated) {
        Connection conn = getConnectionToDB();

        try {
            Statement stmnt = conn.createStatement();
            String sqlUpdate = "UPDATE PUBLIC.ACTIVATION_STATE SET ACTIVATED = " + activated + " WHERE GUILD_NAME = '" + guild.getName() + "'";

            int count = stmnt.executeUpdate(sqlUpdate);

            if (count == 0) {
                Statement statement = conn.createStatement();
                String sqlInsert = "INSERT INTO PUBLIC.ACTIVATION_STATE(GUILD_NAME, ACTIVATED) VALUES('" + guild.getName() + "', " + activated + ")";

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
            String sql = "SELECT * FROM PUBLIC.ACTIVATION_STATE WHERE GUILD_NAME = '" + guild.getName() + "'";
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
}
