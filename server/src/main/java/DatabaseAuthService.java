import org.sqlite.ExtendedCommand;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseAuthService implements AuthService {
    private static Connection connection;
    private static Statement stmt;
    private static PreparedStatement psInsert;
    private static PreparedStatement psSelect;
    private static PreparedStatement psSelectDub;
    private static PreparedStatement psSelectFreeNick;
    private static PreparedStatement psUpdate;

    public DatabaseAuthService() {
    }

    // Подключение к базе данных SQLite
    private static void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:main.db");
        stmt = connection.createStatement();
        psInsert = connection.prepareStatement("INSERT INTO users(login,pass,nick) VALUES (?,?,?)");
        psUpdate = connection.prepareStatement("UPDATE users SET nick=? WHERE login=?");
        psSelect = connection.prepareStatement("SELECT nick FROM users WHERE (login=?) and (pass=?)");
        psSelectDub = connection.prepareStatement("SELECT login FROM users WHERE (login=?) or (nick=?)");
        psSelectFreeNick = connection.prepareStatement("SELECT login FROM users WHERE (nick=?)");
    }

    // Отсоединение от базы данных
    private void disconnect() {
        try {
            stmt.close();
            psSelect.close();
            psSelectDub.close();
            psSelectFreeNick.close();
            psUpdate.close();
            psInsert.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Этим методом (из интерфейса) проверяем есть ли такой пользователь, и если есть выдаем его ник, если нет то null
    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        try {
            connect();
            // Поиск имени и пароля
            psSelect.setString(1, login);
            psSelect.setString(2, password);
            ResultSet rs = psSelect.executeQuery();
            if (rs.next()) {
                return rs.getString("nick");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
        return null;
    }

    // Этим методом (из интерфейса) создаем нового пользователя
    @Override
    public boolean registration(String login, String password, String nickname) {
        boolean result = false;
        try {
            connect();
            // Проверка есть ли уже такое имя или ник
            psSelectDub.setString(1, login);
            psSelectDub.setString(2, nickname);
            ResultSet rs = psSelectDub.executeQuery();
            if (rs.next()) {
                return false;
            }
            // Запись в БД
            psInsert.setString(1, login);
            psInsert.setString(2, password);
            psInsert.setString(3, nickname);
            psInsert.executeUpdate();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
        return result;
    }

    // Этим методом (из интерфейса) меняем ник
    @Override
    public boolean changeNickname(String login, String newNickname) {
        boolean result = false;
        try {
            connect();
            // Проверка свободен ли новый ник
            psSelectFreeNick.setString(1, newNickname);
            ResultSet rs = psSelectFreeNick.executeQuery();
            if (rs.next()) {
                return false;
            }
            // Изменение в БД
            psUpdate.setString(2, login);
            psUpdate.setString(1, newNickname);
            System.out.println(psUpdate.executeUpdate());
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
        return result;
    }
}
