
public class StartServer {
    public static void main(String[] args) {
        new Server(); // Сделан отдельный класс сервер, так как нельзя в статическом классе использовать this в ClientHandler
    }
}
