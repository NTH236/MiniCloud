package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class client {
    public static final String SERVER_IP = "127.0.0.1";
    public static final int SERVER_PORT = 8888;

    public static void main(String[] args) {
        File downloadFolder = getDownloadDirectory();
        if (!downloadFolder.exists()) {
            downloadFolder.mkdirs();
        }

        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("=== ĐÃ KẾT NỐI VỚI HỆ THỐNG CLOUD SERVER ===");
            System.out.println("Thư mục nhận file (download): " + downloadFolder.getAbsolutePath());

            boolean isConnected = true;
            while (isConnected) {
                System.out.println("\n---------------- MENU ĐIỀU KHIỂN ----------------");
                System.out.println("1. Lưu trữ file lên Cloud (Upload)");
                System.out.println("2. Tải file từ Cloud về máy (Download)");
                System.out.println("3. Thoát chương trình");
                System.out.print("Chỉ huy chọn chức năng (1-3): ");

                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        uploadFileToCloud(scanner, dis, dos);
                        break;
                    case "2":
                        downloadFileFromCloud(scanner, dis, dos, downloadFolder);
                        break;
                    case "3":
                        dos.writeUTF("EXIT");
                        dos.flush();
                        isConnected = false;
                        System.out.println("Đã ngắt kết nối với Cloud Server.");
                        break;
                    default:
                        System.out.println("Lựa chọn không hợp lệ. Vui lòng nhập 1, 2 hoặc 3.");
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("Không thể kết nối đến Cloud Server: " + e.getMessage());
        }
    }

    // Tự động định vị thư mục 'Client/download'
    private static File getDownloadDirectory() {
        File dirFromRoot = new File("Client/download");
        if (dirFromRoot.getParentFile() != null && dirFromRoot.getParentFile().exists()) {
            return dirFromRoot;
        }
        return new File("download");
    }

    // Tìm file thông minh: Kiểm tra đường dẫn trực tiếp (tuyệt đối) trước, sau đó tìm trong thư mục 'Client'
    private static File resolveClientFile(String inputPath) {
        File directFile = new File(inputPath);
        if (directFile.exists() && directFile.isFile()) {
            return directFile;
        }
        File fileInClientDir = new File("Client", inputPath);
        if (fileInClientDir.exists() && fileInClientDir.isFile()) {
            return fileInClientDir;
        }
        return directFile; // Trả về đối tượng ban đầu nếu không tìm thấy ở cả 2 nơi
    }

    // Chức năng 1: Gửi file lên thư mục 'cloud' của Server
    private static void uploadFileToCloud(Scanner scanner, DataInputStream dis, DataOutputStream dos) throws Exception {
        System.out.print("Nhập tên file (trong thư mục Client) hoặc đường dẫn bất kỳ trên máy: ");
        String inputPath = scanner.nextLine().trim();

        if (inputPath.isEmpty()) {
            System.out.println("Đường dẫn không được để trống!");
            return;
        }

        File fileToSend = resolveClientFile(inputPath);

        // Gửi thông điệp yêu cầu lên Server
        dos.writeUTF("UPLOAD");
        dos.writeUTF(inputPath);

        // Kiểm tra xem file có tồn tại và đọc được không
        if (!fileToSend.exists() || !fileToSend.isFile() || !fileToSend.canRead()) {
            dos.writeBoolean(false); // Báo cho Server biết file không hợp lệ để hủy phiên chờ
            dos.flush();
            System.out.println("[CẢNH BÁO] Không tìm thấy file hoặc không thể đọc file tại: " + inputPath);
            return;
        }

        // Xác nhận với Server là file tồn tại và bắt đầu đẩy dữ liệu
        dos.writeBoolean(true);
        dos.writeUTF(fileToSend.getName());
        dos.writeLong(fileToSend.length());

        System.out.println("Đang chuyển file '" + fileToSend.getName() + "' lên Cloud...");
        try (FileInputStream fis = new FileInputStream(fileToSend)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
        }
        dos.flush();

        // Nhận phản hồi hoàn tất từ CloudServer
        String serverMsg = dis.readUTF();
        System.out.println(serverMsg);
    }

    // Chức năng 2: Tải file từ 'cloud' của Server về 'download' của Client
    private static void downloadFileFromCloud(Scanner scanner, DataInputStream dis, DataOutputStream dos, File downloadFolder) throws Exception {
        System.out.print("Nhập tên file cần lấy từ Cloud (VD: anh.png, note.txt): ");
        String fileName = scanner.nextLine().trim();

        if (fileName.isEmpty()) {
            System.out.println("Tên file không được để trống!");
            return;
        }

        // Gửi thông điệp tìm kiếm file lên CloudServer
        dos.writeUTF("DOWNLOAD");
        dos.writeUTF(fileName);
        dos.flush();

        // Nhận kết quả tìm kiếm từ Server
        boolean isExist = dis.readBoolean();
        String serverNotice = dis.readUTF();
        System.out.println(serverNotice); // Hiển thị thông báo trực quan từ Server

        if (isExist) {
            long fileSize = dis.readLong();
            File savedFile = new File(downloadFolder, fileName);

            try (FileOutputStream fos = new FileOutputStream(savedFile)) {
                byte[] buffer = new byte[4096];
                long totalRead = 0;

                while (totalRead < fileSize) {
                    int bytesToRead = (int) Math.min(buffer.length, fileSize - totalRead);
                    int bytesRead = dis.read(buffer, 0, bytesToRead);
                    if (bytesRead == -1) break;

                    fos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }
            }
            System.out.println("[THÀNH CÔNG] File đã được tải về thư mục download: " + savedFile.getPath());
        }
    }
}