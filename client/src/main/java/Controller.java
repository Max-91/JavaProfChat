import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    private ListView<String> clientList;
    @FXML
    private TextArea textArea;
    @FXML
    private TextField textField;
    @FXML
    private HBox authPanel;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private HBox msgPanel;

    private final String IP_ADDRESS = "localhost";
    private final int PORT = 8189;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private Stage stage;
    private Stage regStage;
    private RegController regController;

    private boolean authenticated;
    private String nickname;

    // Метод перерисовки в зависимости от того прошла ли идентификация или нет
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);
        clientList.setVisible(authenticated);
        clientList.setManaged(authenticated);
        if (!authenticated) {
            nickname = "";
            setTitle("Балабол");
        } else {
            setTitle(String.format("[ %s ] - Балабол", nickname));
            textArea.clear();
        }

    }

    // Открытие окна
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) textField.getScene().getWindow(); // Используется в методе изменения заголовка окна
            stage.setOnCloseRequest(event -> { // Действие при закрытие (посылать серверу end
                System.out.println("bye");
                if (socket != null && !socket.isClosed()) {
                    try {
                        out.writeUTF("/end");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
        setAuthenticated(false);
        createRegWindow();
    }

    // Выполнение соединения после нажатия SignIn
    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF(); // !..! Ожидание пакета от сервера
                        if (str.startsWith("/")) {
                            if (str.startsWith("/authok ")) {
                                nickname = str.split("\\s")[1];
                                setAuthenticated(true);
                                break;
                            }
                            if (str.startsWith("/regok")) {
                                regController.addMessageTextArea("Регистрация прошла успешно");
                            }
                            if (str.startsWith("/regno")) {
                                regController.addMessageTextArea("Зарегистрироватся не удалось\n" +
                                        " возможно такой логин или никнейм уже заняты");
                            }
                        } else {
                            textArea.appendText(str + "\n");
                        }
                    }
                    // Открытие файла после авторизации (чтение истории и запись)
                    // Чтение истории из файла (использование nio из-за readAllLines)
                    Path path = Paths.get("history_" + nickname + ".txt");
                    if (Files.exists(path)) {
                        // Файл с историей есть
                        try {
                            List content = Files.readAllLines(path);
                            int startElement = 0;
                            if (content.toArray().length > 100) {
                                startElement = content.toArray().length - 100;
                            }
                            for (int i = startElement; i < content.toArray().length; i++) {
                                System.out.println(content.toArray()[i]);
                                textArea.appendText((String) content.toArray()[i]+"\n");
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                    // Запись в файл информации о входе (использование io)
                    File file = new File("history_" + nickname + ".txt");
                    try (FileOutputStream fileOutputStream = new FileOutputStream(file,true)) {
                        String techMes = String.format("Подключение к чату в %1$tT %1$te %1$tB %1$tY \n", new Date());
                        fileOutputStream.write(techMes.getBytes());
                        //цикл работы
                        while (true) {
                            String str = in.readUTF(); // !..! Ожидание пакета от сервера
                            if (str.startsWith("/")) { // Проверка пришло ли служебное сообщение
                                if (str.equals("/end")) {
                                    break;
                                }
                                if (str.startsWith("/clientlist ")) {
                                    String[] token = str.split("\\s");
                                    Platform.runLater(() -> {
                                        clientList.getItems().clear();
                                        for (int i = 1; i < token.length; i++) {
                                            clientList.getItems().add(token[i]);
                                        }
                                    });
                                }
                            } else {
                                textArea.appendText(str + "\n");
                                fileOutputStream.write((str + "\n").getBytes());
                            }
                        }
                        // От сервера пришел "/end"
                        techMes = String.format("Отключение от чата в %1$tT %1$te %1$tB %1$tY \n", new Date());
                        fileOutputStream.write(techMes.getBytes());
                        fileOutputStream.close();
                    }
                } catch (EOFException e) {
                    textArea.appendText("Соединение с сервером прервано");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    setAuthenticated(false);
                    try {
                        socket.close();
                        in.close();
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Нажатие клавиши "Send"
    public void sendMsg(ActionEvent actionEvent) {
        if (textField.getText().trim().length() == 0) {
            return;
        }
        try {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Нажатие клавиши "Sign In"
    public void tryToAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        String msg = String.format("/auth %s %s",
                loginField.getText().trim(), passwordField.getText().trim());
        try {
            out.writeUTF(msg);
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Изменение заголовка окна
    private void setTitle(String title) {
        Platform.runLater(() -> {
            stage.setTitle(title);
        });
    }

    // Нажатие на название пользователя в списке
    public void clickClientList(MouseEvent mouseEvent) {
        textField.setText(String.format("/w %s ", clientList.getSelectionModel().getSelectedItem()));
    }

    // Нажатие на название пользователя в списке (после отпускания)
    public void releasedMouseClientList(MouseEvent mouseEvent) {
        System.out.println(clientList.getSelectionModel().getSelectedItem());
        System.out.println(mouseEvent.getButton());
        System.out.println(mouseEvent.getClickCount());
    }

    // Создание окна регистрации
    private void createRegWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("reg.fxml"));
            Parent root = fxmlLoader.load();
            regStage = new Stage();
            regStage.setTitle("Регистрация в чате Балабол");
            regStage.setScene(new Scene(root, 400, 300));
            regStage.initModality(Modality.APPLICATION_MODAL);

            regController = fxmlLoader.getController();
            regController.setController(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Отображение окна
    public void regStageShow(ActionEvent actionEvent) {
        regStage.show();
    }

    // Отправка сообщения о регистрации на сервер
    public void tryRegistration(String login, String password, String nickname) {
        String msg = String.format("/reg %s %s %s", login, password, nickname);

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
