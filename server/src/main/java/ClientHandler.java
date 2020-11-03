/*
    Класс для создание отдельный экземпляров подключений для каждого клиента.
    В данном задание сервер получается от одного из клиентов сообщение (в каком-то ClientHandler из списка clients
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientHandler {
    DataInputStream in;
    DataOutputStream out;
    Server server;
    Socket socket;

    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            System.out.println("Подключился клиент, сокет: " + socket.getRemoteSocketAddress());
            server.service.execute(()->{
            //new Thread(() -> {
                try {
                    socket.setSoTimeout(5000); // Таймаут на отключение клиента, если клиент не зарегистрирован
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        // Ответ на запрос о регистрации нового пользователя
                        if (str.startsWith("/reg ")) {
                            String[] token = str.split("\\s");
                            if (token.length < 4) {
                                continue;
                            }
                            boolean b = server.getAuthService()
                                    .registration(token[1], token[2], token[3]);
                            if (b) {
                                sendMsg("/regok");
                            } else {
                                sendMsg("/regno");
                            }
                        }
                        // Ответ на запрос о входе
                        if (str.startsWith("/auth ")) {
                            String[] token = str.split("\\s");
                            if (token.length < 3) {
                                continue;
                            }
                            String newNick = server.getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);
                            if (newNick != null) {
                                login = token[1];
                                if (!server.isLoginAuthenticated(login)) {
                                    nickname = newNick;
                                    sendMsg("/authok " + newNick);
                                    server.subscribe(this);
                                    break;
                                } else {
                                    sendMsg("С этим логином уже вошли в чат");
                                }
                            } else {
                                sendMsg("Неверный логин / пароль");
                            }
                        }
                    }
                    socket.setSoTimeout(0); // Отключение таймаута, так как клиент залогинился
                    //цикл работы
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                sendMsg("/end");
                                break;
                            }
                            // Ответ на запрос об изменение ника
                            if (str.startsWith("/chg ")) {
                                String[] token = str.split("\\s");
                                System.out.println("Запрос изменения: "+login+" на "+token[1]);
                                if (token.length < 2) {
                                    continue;
                                }
                                if (server.getAuthService().changeNickname(login, token[1])) {
                                    sendMsg("Ник успешно изменен, войдите повторно");
                                    sendMsg("/end ");

                                    break;
                                } else {
                                    sendMsg("Такой ник занят");
                                }
                            }
                        } else {
                            server.broadcastMsg(this, str);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("Отключение клиента по времени бездействия, сокет: " + socket.getRemoteSocketAddress());
                    sendMsg("Время соединения превышено, вы отключены");
                    sendMsg("/end");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    // ЗАВЕРШЕНИЕ СВЯЗИ
                    server.unsubscribe(this);
                    System.out.println("Клиент отключился, сокет: " + socket.getRemoteSocketAddress());
                    try {
                        socket.close();
                        in.close();
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            //}).start();
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
