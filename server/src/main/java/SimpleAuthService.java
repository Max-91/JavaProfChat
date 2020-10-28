/*
 Данный класс обрабатывает аудентификацию пользователя.
 Класс имеет интерфейс AuthService, у которого есть метод getNicknameByLoginAndPassword(login, pass),
 который используется для опеределение есть ли такой пользователь или нет.
 Интерфейс используется, чтобы в коде ClientHandler можно было исполльзовать другой класс вместо SimpleAuthService,
 но с интерфесом AuthService, содержажим метод getNicknameByLoginAndPassword(login, pass).
 Например FileAuthService (в файле содержаться логины и пароли) или DBAuthService (в БД храняться логины и пароли).
 */

import java.util.ArrayList;
import java.util.List;

public class SimpleAuthService implements AuthService {
    private class UserData {
        String login;
        String password;
        String nickname;

        public UserData(String login, String password, String nickname) {
            this.login = login;
            this.password = password;
            this.nickname = nickname;
        }
    }

    private List<UserData> users;

    public SimpleAuthService() {
        users = new ArrayList<>();
        users.add(new UserData("qwe", "qwe", "qwe"));
        users.add(new UserData("asd", "asd", "asd"));
        users.add(new UserData("zxc", "zxc", "zxc"));
        for (int i = 1; i < 10; i++) {
            users.add(new UserData("login" + i, "pass" + i, "nick" + i));
        }
    }

    // Этим методом (из интерфейса) проверяем есть ли такой пользователь, и если есть выдаем его ник, если нет то null
    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        for (UserData user : users) {
            if (user.login.equals(login) && user.password.equals(password)) {
                return user.nickname;
            }
        }
        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        for (UserData user : users) {
            if (user.login.equals(login) || user.nickname.equals(nickname)) {
                return false;
            }
        }
        users.add(new UserData(login, password, nickname));
        return true;
    }
}
