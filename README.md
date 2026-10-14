# Java Socket Cloud Storage (Mini-Drive Console App)

Ứng dụng mô phỏng hệ thống lưu trữ đám mây (tương tự Google Drive ở quy mô thu nhỏ) hoạt động trên giao diện dòng lệnh (Console), được xây dựng bằng ngôn ngữ **Java** sử dụng giao thức mạng **TCP Socket**. 

Hệ thống cho phép nhiều máy trạm (**Client**) kết nối đồng thời tới máy chủ (**CloudServer**) để tải tập tin lên kho lưu trữ chung hoặc trích xuất tập tin từ đám mây về máy cá nhân.

---

## Tính năng chính

### 1. Phía Client (`Client/client.java`)
* **Lưu trữ tập tin lên Cloud (Upload):** 
  * Cho phép người dùng nhập tên tập tin nằm trong thư mục `Client` hoặc đường dẫn tuyệt đối bất kỳ trên máy tính (ví dụ: `/home/alpha/Pictures/image.png`).
  * Tự động kiểm tra tính hợp lệ và sự tồn tại của tập tin trước khi truyền dữ liệu lên Server.
* **Tải tập tin từ Cloud (Download):**
  * Yêu cầu tải tập tin từ Server chỉ bằng cách nhập tên file (ví dụ: `document.txt`).
  * Tự động lưu trữ tập tin nhận được vào thư mục `Client/download/`.
  * Nhận và hiển thị thông báo trực quan nếu tập tin không tồn tại trên hệ thống Cloud.

### 2. Phía Cloud Server (`CloudServer/CloudServer.java`)
* **Xử lý đa luồng (Multi-threading):** Hỗ trợ phục vụ nhiều Client kết nối và thao tác cùng lúc mà không gây nghẽn mạng.
* **Quản lý kho lưu trữ (`CloudServer/cloud/`):** Chỉ thực hiện mở luồng ghi tập tin vào thư mục `cloud` sau khi nhận được tín hiệu xác nhận tập tin hợp lệ từ phía Client.
* **Tìm kiếm và phản hồi:** Kiểm tra sự tồn tại của tập tin trong kho `cloud` khi có yêu cầu tải về, gửi cờ báo hiệu (`boolean`) kèm dữ liệu hoặc thông báo lỗi chi tiết về cho Client.

---

## Cấu trúc thư mục dự án

```text
CONSOLEAPP/
├── Client/
│   ├── download/          # Thư mục tự động lưu các file tải về từ Cloud
│   └── client.java        # Mã nguồn chương trình điều khiển phía Client
├── CloudServer/
│   ├── cloud/             # Kho lưu trữ tập trung các file được gửi lên Server
│   └── CloudServer.java   # Mã nguồn chương trình máy chủ Cloud đa luồng
└── README.md              # Tài liệu hướng dẫn sử dụng
```

---

## Yêu cầu hệ thống

* **Java Development Kit (JDK):** Phiên bản **17** hoặc **21** trở lên.
* **Môi trường:** Tương thích với mọi hệ điều hành (Linux, Windows, macOS).

---

## Hướng dẫn biên dịch và chạy chương trình

Mở Terminal tại thư mục gốc của dự án (`CONSOLEAPP`) và thực hiện theo các bước sau:

### Bước 1: Biên dịch mã nguồn
```bash
javac CloudServer/CloudServer.java Client/client.java
```

### Bước 2: Khởi động Cloud Server
Mở một cửa sổ Terminal thứ nhất để chạy máy chủ lắng nghe tại cổng `8888`:
```bash
java CloudServer.CloudServer
```

### Bước 3: Khởi động Client
Mở một cửa sổ Terminal thứ hai để chạy giao diện điều khiển của Client:
```bash
java Client.client
```

*(Lưu ý: Nếu sử dụng VS Code hoặc IntelliJ IDEA, bạn có thể nhấn trực tiếp nút **Run** trên từng file `CloudServer.java` và `client.java`).*

---

## Cơ chế Giao thức truyền tải (Application Protocol)

Hệ thống sử dụng `DataInputStream` và `DataOutputStream` kết hợp với vùng đệm `byte[4096]` để truyền tải cả thông tin điều khiển (Header) lẫn dữ liệu nhị phân (Body) trên cùng một kết nối TCP mà không bị xung đột luồng:

1. **Quy trình Upload (`UPLOAD`):**
   * **Client -> Server:** Gửi lệnh `"UPLOAD"` $\rightarrow$ Gửi đường dẫn gốc $\rightarrow$ Gửi cờ kiểm tra hợp lệ (`boolean`).
   * Nếu cờ là `true`: **Client** gửi tiếp tên file (`String`), kích thước file (`long`) và luồng `byte[]` của file.
   * **Server -> Client:** Đọc đúng số lượng byte theo kích thước đã báo cáo, lưu vào `CloudServer/cloud/` và gửi chuỗi xác nhận hoàn tất.

2. **Quy trình Download (`DOWNLOAD`):**
   * **Client -> Server:** Gửi lệnh `"DOWNLOAD"` $\rightarrow$ Gửi tên file cần tìm (`String`).
   * **Server -> Client:** Kiểm tra trong thư mục `CloudServer/cloud/` và gửi về cờ trạng thái (`boolean`) kèm chuỗi thông báo.
   * Nếu cờ là `true`: **Server** gửi tiếp kích thước file (`long`) và bơm toàn bộ mảng `byte[]` về để **Client** ghi vào `Client/download/`.