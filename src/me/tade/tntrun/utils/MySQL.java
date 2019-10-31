package me.tade.tntrun.utils;

import me.tade.tntrun.TNTRun;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.logging.Logger;

public class MySQL {

    private static final Logger logger = Bukkit.getLogger();
    public Connection c = null;
    protected boolean connected = false;
    private String driver;
    private String connectionString;

    public MySQL(String hostname, int port, String database, String username, String password) {
        driver = "com.mysql.jdbc.Driver";
        connectionString = "jdbc:mysql://" + hostname + ":" + port + "/" + database + "?user=" + username
                + "&password=" + password;
    }

    public Connection open() {
        try {
            Class.forName(driver);
            this.c = DriverManager.getConnection(connectionString);

            return c;
        } catch (SQLException e) {
            System.out.println("Could not connect to Database! because: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println(driver + " not found!");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return this.c;
    }

    public Connection getConn() {
        return this.c;
    }

    public void close() {
        try {
            if (c != null) {
                c.close();
            }
        } catch (SQLException ex) {
            Bukkit.getLogger().info(ex.getMessage());
        }

        c = null;
    }

    public boolean isConnected() {
        try {
            return ((c != null) && !c.isClosed());
        } catch (SQLException e) {
            return false;
        }
    }

    public Result query(final String query) {
        if (!isConnected()) {
            open();
        }

        return query(query, true);
    }

    public Result query(final String query, boolean retry) {
        if (!isConnected()) {
            open();
        }

        try {
            PreparedStatement statement = null;

            try {
                if (!isConnected()) {
                    open();
                }

                statement = c.prepareStatement(query);

                if (statement.execute()) {
                    return new Result(statement, statement.getResultSet());
                }
            } catch (final SQLException e) {
                e.printStackTrace();
                final String msg = e.getMessage();

                logger.severe("Database query error: " + msg);

                if (retry && msg.contains("_BUSY")) {
                    logger.severe("Retrying query...");
                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(TNTRun.get(), new Runnable() {
                        @Override
                        public void run() {
                            query(query, false);
                        }
                    }, 20);
                }
            }

            if (statement != null) {
                statement.close();
            }
        } catch (SQLException ex) {
            Bukkit.getLogger().info(ex.getMessage());
        }

        return null;
    }

    protected Statements getStatement(String query) {
        String trimmedQuery = query.trim();

        if (trimmedQuery.substring(0, 6).equalsIgnoreCase("SELECT")) {
            return Statements.SELECT;
        }

        if (trimmedQuery.substring(0, 6).equalsIgnoreCase("INSERT")) {
            return Statements.INSERT;
        }

        if (trimmedQuery.substring(0, 6).equalsIgnoreCase("UPDATE")) {
            return Statements.UPDATE;
        }

        if (trimmedQuery.substring(0, 6).equalsIgnoreCase("DELETE")) {
            return Statements.DELETE;
        }

        if (trimmedQuery.substring(0, 6).equalsIgnoreCase("CREATE")) {
            return Statements.CREATE;
        }

        if (trimmedQuery.substring(0, 5).equalsIgnoreCase("ALTER")) {
            return Statements.ALTER;
        }

        if (trimmedQuery.substring(0, 4).equalsIgnoreCase("DROP")) {
            return Statements.DROP;
        }

        if (trimmedQuery.substring(0, 8).equalsIgnoreCase("TRUNCATE")) {
            return Statements.TRUNCATE;
        }

        if (trimmedQuery.substring(0, 6).equalsIgnoreCase("RENAME")) {
            return Statements.RENAME;
        }

        if (trimmedQuery.substring(0, 2).equalsIgnoreCase("DO")) {
            return Statements.DO;
        }

        if (trimmedQuery.substring(0, 7).equalsIgnoreCase("REPLACE")) {
            return Statements.REPLACE;
        }

        if (trimmedQuery.substring(0, 4).equalsIgnoreCase("LOAD")) {
            return Statements.LOAD;
        }

        if (trimmedQuery.substring(0, 7).equalsIgnoreCase("HANDLER")) {
            return Statements.HANDLER;
        }

        if (trimmedQuery.substring(0, 4).equalsIgnoreCase("CALL")) {
            return Statements.CALL;
        }

        return Statements.SELECT;
    }

    public void setValue(Player p, StatsType type, int value) {
        query("UPDATE `tntrun_stats` SET `" + type.toString().toLowerCase() + "`='" + value + "' WHERE `username`='" + p.getName() + "';");
    }

    public int getValue(Player p, StatsType type) {
        try {
            int stat = -1;
            ResultSet rs = TNTRun.get().getSql().query("SELECT * FROM `tntrun_stats` WHERE `username`='" + p.getName() + "'").getResultSet();

            while (rs.next()) {
                stat = rs.getInt(type.toString().toLowerCase());
            }
            return stat;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    protected enum Statements {
        SELECT, INSERT, UPDATE, DELETE, DO, REPLACE, LOAD, HANDLER, CALL, CREATE, ALTER, DROP, TRUNCATE, RENAME, START,
        COMMIT, ROLLBACK, SAVEPOINT, LOCK, UNLOCK, PREPARE, EXECUTE, DEALLOCATE, SET, SHOW, DESCRIBE, EXPLAIN, HELP,
        USE, ANALYZE, ATTACH, BEGIN, DETACH, END, INDEXED, ON, PRAGMA, REINDEX, RELEASE, VACUUM
    }

    public enum StatsType {
        VICTORIES,
        LOSES,
        BLOCKS_DESTROYED,
        PLAYED
    }

    public class Result {

        private ResultSet resultSet;
        private Statement statement;

        public Result(Statement statement, ResultSet resultSet) {
            this.statement = statement;
            this.resultSet = resultSet;
        }

        public ResultSet getResultSet() {
            return this.resultSet;
        }

        public void close() {
            try {
                this.statement.close();
                this.resultSet.close();
            } catch (SQLException e) {
            }
        }
    }
}
