package com.leekyoungil.illuminati.client.prossor.infra.backup.impl;

import com.leekyoungil.illuminati.client.prossor.executor.IlluminatiExecutorType;
import com.leekyoungil.illuminati.client.prossor.infra.backup.Backup;
import com.leekyoungil.illuminati.client.prossor.infra.backup.configuration.H2ConnectionFactory;
import com.leekyoungil.illuminati.common.util.StringObjectUtils;
import org.apache.commons.collections.CollectionUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class H2Backup<T> implements Backup<T> {

    private static H2Backup H2_BACKUP;

    private final H2ConnectionFactory H2_CONN = H2ConnectionFactory.getInstance();
    private Statement stmt = null;
    private static final String TABLE_NAME = "illuminati_backup";
    private boolean tableIsCreated = false;

    private H2Backup () {
        if (H2_CONN.isConnected() == true) {
            try {
                this.stmt = H2_CONN.getDbConnection().createStatement();
                this.createTable();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static H2Backup getInstance () {
        if (H2_BACKUP == null) {
            synchronized (H2Backup.class) {
                if (H2_BACKUP == null) {
                    H2_BACKUP = new H2Backup();
                }
            }
        }

        return H2_BACKUP;
    }

    private void createTable () {
        //CREATE TABLE IF NOT EXISTS TEST(ID INT PRIMARY KEY, NAME VARCHAR(255));
        StringBuilder tableExecuteCommand = new StringBuilder();
        tableExecuteCommand.append("CREATE TABLE IF NOT EXISTS ");
        tableExecuteCommand.append(TABLE_NAME);
        tableExecuteCommand.append(" ( ");
        tableExecuteCommand.append(" ID INTEGER PRIMARY KEY AUTO_INCREMENT");
        tableExecuteCommand.append(", JSON_DATA TEXT ");
        tableExecuteCommand.append(" ) ");

        try {
            this.stmt.execute(tableExecuteCommand.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override public void append(IlluminatiExecutorType illuminatiExecutorType, T data) {
        StringBuilder insertExecuteCommand = new StringBuilder();
        insertExecuteCommand.append("INSERT INTO ");
        insertExecuteCommand.append(TABLE_NAME);
        insertExecuteCommand.append(" (JSON_DAta) ");
        insertExecuteCommand.append("VALUES ('"+data+"')");

        try {
            this.stmt.execute(insertExecuteCommand.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override public List<T> getDataByList(boolean isPaging, boolean isAfterDelete, int from, int size) {
        List<T> dataList = new ArrayList<T>();
        List<Integer> idList = new ArrayList<Integer>();

        try {
            ResultSet rs = this.stmt.executeQuery(this.getSelectQuery(isPaging, from, size));
            while (rs.next()) {
                idList.add(rs.getInt("ID"));
                dataList.add((T) rs.getString("JSON_DATA"));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

        if (isAfterDelete == true && CollectionUtils.isNotEmpty(idList) == true) {
            for (Integer id : idList) {
                this.deleteById(id);
            }
        }

        return dataList;
    }

    @Override public Map<Integer, T> getDataByMap(boolean isPaging, boolean isAfterDelete, int from, int size) {
        String selectQuery = this.getSelectQuery(isPaging, from, size);

        if (StringObjectUtils.isValid(selectQuery) == false) {
            return null;
        }

        Map<Integer, T> dataMap = new HashMap<Integer, T>();

        try {
            ResultSet rs = this.stmt.executeQuery(selectQuery);
            while (rs.next()) {
                dataMap.put(rs.getInt("ID"), (T) rs.getString("JSON_DATA"));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

        if (isAfterDelete == true && dataMap.isEmpty() == false) {
            for (Map.Entry<Integer, T> entry : dataMap.entrySet()) {
                this.deleteById(entry.getKey());
            }
        }

        return dataMap;
    }

    @Override public void deleteById(int id) {
        StringBuilder deleteExecuteCommand = new StringBuilder();
        deleteExecuteCommand.append("DELETE FROM ");
        deleteExecuteCommand.append(TABLE_NAME);
        deleteExecuteCommand.append(" WHERE ID = ");
        deleteExecuteCommand.append(id);

        try {
            this.stmt.executeUpdate(deleteExecuteCommand.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override public int getCount() {
        StringBuilder countExecuteCommand = new StringBuilder();
        countExecuteCommand.append("SELECT count(1) FROM ");
        countExecuteCommand.append(TABLE_NAME);

        try {
            ResultSet rs = this.stmt.executeQuery(countExecuteCommand.toString());
            if (rs.next() == true) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }

        return 0;
    }

    private String getSelectQuery (boolean isPaging, int from, int size) {
        StringBuilder selectExecuteCommand = new StringBuilder();
        selectExecuteCommand.append("SELECT ID, JSON_DATA FROM ");
        selectExecuteCommand.append(TABLE_NAME);

        if (isPaging == true) {
            selectExecuteCommand.append(" LIMIT "+from+", "+size);
        }

        return selectExecuteCommand.toString();
    }
}
