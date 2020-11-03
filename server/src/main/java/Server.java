/*
    В данном задание сервер получается от одного из клиентов сообщение (в каком-то ClientHandler из списка clients)
    его транслирует остальным через метод broadcastMsg

 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class Server {
    private List<ClientHandler> clients; // Список подключений
    private AuthService authService; //Объект для обработки аудентификации
    ExecutorService service = Executors.newCachedThreadPool();

    public Server() {
        clients = new CopyOnWriteArrayList<>();
        authService = new DatabaseAuthService();
        ServerSocket server = null;
        Socket socket = null;
        final int PORT = 8189;
        try {
            server = new ServerSocket(PORT);
            System.out.println("Сервер запустился");

            while (true) { // Бесконечный цикл в котором создается ClientHandler для нового подключения
                socket = server.accept(); // !...! Точка ожидания нового подключения
                new ClientHandler(this, socket); // Выполняется при появление нового подключения
                System.out.println("Кол-во активных потоков: " + ((ThreadPoolExecutor) service).getPoolSize());
                System.out.println("Кол-во завершенных потоков: " + ((ThreadPoolExecutor) service).getCompletedTaskCount());
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            service.shutdown();
        }
    }

    // Процедура отправки всем сообщения
    public void broadcastMsg(ClientHandler sender, String msg) {
        if (msg.startsWith("/w ")) { // Именная отправка
            String[] wordMsg = msg.split("\\s");
            if (wordMsg.length >= 3) {
                String nicSender = sender.getNickname();
                String nicReceiver = wordMsg[1];
                String message = String.format("[ %s->%s ]: ", nicSender, nicReceiver);
                for (int i = 2; i < wordMsg.length; i++) {
                    message += wordMsg[i] + " ";
                }
                for (ClientHandler c : clients) {
                    if (nicReceiver.equals(c.getNickname()) || nicSender.equals(c.getNickname())) {
                        c.sendMsg(message);
                    }
                }
            }
        } else { // Отправка сообщения всем
            String message = String.format("[ %s ]: %s", sender.getNickname(), msg);
            for (ClientHandler c : clients) {
                c.sendMsg(message);
            }
        }
    }

    // Добавление нового подключения в список клиентов
    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
    }

    // Исключения подключения в список клиентов
    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    // Метод искользуется для получения доступа к authService внутри ClassHandler
    public AuthService getAuthService() {
        return authService;
    }

    // Метод проверки зарегистрирован ли уже пользователь с таким же именем
    public boolean isLoginAuthenticated(String login) {
        for (ClientHandler c : clients) {
            String tempStr = c.getLogin();
            if (c.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    // Отправка всем пользователям списка клиентов
    public void broadcastClientList() {
        StringBuilder sb = new StringBuilder("/clientlist ");

        for (ClientHandler c : clients) {
            sb.append(c.getNickname()).append(" ");
        }
//        sb.setLength(sb.length() );
        String message = sb.toString();
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }
}
