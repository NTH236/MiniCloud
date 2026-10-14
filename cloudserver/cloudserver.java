package cloudserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class cloudserver {
    public static final int PORT = 8888;

    public static void main(String[] args) {
        File cloudFolder = getCloudDirectory();
        if (!cloudFolder.exists()) {
            cloudFolder.mkdirs();
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("=== TRẠM LƯU TRỮ CLOUD SERVER ĐÃ KÍCH HOẠT (PORT: " + PORT + ") ===");
            System.out.println("Kho lưu trữ Cloud đặt tại: " + cloudFolder.getAbsolutePath());

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("\n[+] Tín hiệu kết nối mới từ Client: " + clientSocket.getInetAddress());

                // Phân luồng xử lý độc lập cho từng Client
                new Thread(() -> handleClient(clientSocket, cloudFolder)).start();
            }
        } catch (Exception e) {
            System.err.println("Lỗi hệ thống CloudServer: " + e.getMessage());
        }
    }

    // Tự động nhận diện thư mục 'cloud' dù chạy từ CONSOLEAPP hay chạy trong CloudServer
    private static File getCloudDirectory() {
        File dirFromRoot = new File("CloudServer/cloud");
        if (dirFromRoot.getParentFile() != null && dirFromRoot.getParentFile().exists()) {
            return dirFromRoot;
        }
        return new File("cloud");
    }

    private static void handleClient(Socket socket, File cloudFolder) {
        try (socket;
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            boolean isRunning = true;
            while (isRunning) {
                // Chờ thông điệp yêu cầu từ Client
                String action = dis.readUTF();

                switch (action) {
                    case "UPLOAD":
                        handleUpload(dis, dos, cloudFolder);
                        break;
                    case "DOWNLOAD":
                        handleDownload(dis, dos, cloudFolder);
                        break;
                    case "EXIT":
                        System.out.println("[-] Client " + socket.getInetAddress() + " đã ngắt kết nối.");
                        isRunning = false;
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("[-] Đã đóng phiên làm việc với Client.");
        }
    }

    // Xử lý nhận file và lưu vào thư mục 'cloud'
    private static void handleUpload(DataInputStream dis, DataOutputStream dos, File cloudFolder) throws Exception {
        // Nhận thông điệp kiểm tra đường dẫn/file từ Client gửi lên
        String clientFilePath = dis.readUTF();
        System.out.println("-> Nhận thông điệp yêu cầu lưu trữ từ đường dẫn: " + clientFilePath);

        // Kiểm tra tín hiệu xác nhận file có tồn tại và đọc được hay không
        boolean canReadFile = dis.readBoolean();
        if (!canReadFile) {
            System.out.println("-> Hủy tiếp nhận: File phía Client không tồn tại hoặc không thể đọc.");
            return;
        }

        String fileName = dis.readUTF();
        long fileSize = dis.readLong();
        File targetFile = new File(cloudFolder, fileName);

        System.out.println("-> Đang tiếp nhận '" + fileName + "' (" + fileSize + " bytes) vào kho cloud...");

        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
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

        System.out.println("-> Hoàn tất! Đã cất giữ tại: " + targetFile.getPath());
        dos.writeUTF("[CLOUD SERVER] Lưu trữ thành công file '" + fileName + "' vào thư mục cloud!");
        dos.flush();
    }

    // Xử lý tìm kiếm file trong 'cloud' và gửi về thư mục 'download' của Client
    private static void handleDownload(DataInputStream dis, DataOutputStream dos, File cloudFolder) throws Exception {
        String requestedFileName = dis.readUTF();
        System.out.println("-> Client yêu cầu tìm và tải file: " + requestedFileName);

        File fileInCloud = new File(cloudFolder, requestedFileName);

        // Kiểm tra sự tồn tại của file trong kho 'cloud'
        if (fileInCloud.exists() && fileInCloud.isFile()) {
            dos.writeBoolean(true); // Báo hiệu tìm thấy file
            dos.writeUTF("[CLOUD SERVER] Đã tìm thấy '" + requestedFileName + "'. Bắt đầu truyền tải...");
            dos.writeLong(fileInCloud.length());

            try (FileInputStream fis = new FileInputStream(fileInCloud)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
            }
            dos.flush();
            System.out.println("-> Đã gửi file '" + requestedFileName + "' về cho Client.");
        } else {
            dos.writeBoolean(false); // Báo hiệu file không tồn tại
            dos.writeUTF("[THÔNG BÁO TỪ CLOUD] Tập tin '" + requestedFileName + "' không tồn tại trên kho lưu trữ Cloud!");
            dos.flush();
            System.out.println("-> Từ chối: Không tìm thấy file '" + requestedFileName + "' trong kho cloud.");
        }
    }
}