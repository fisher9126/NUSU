# NUSU - Quản lý tàu vũ trụ và phi hành gia

Ứng dụng full stack: giao diện HTML/CSS/JS thuần gọi REST API Spring Boot, dữ liệu lưu trong PostgreSQL. Cả ba chạy bằng Docker Compose.

```
Trình duyệt --> nginx (frontend, cổng WEB_PORT) --/api--> Spring Boot (backend:8080) --> PostgreSQL (database)
```

## Cấu trúc

```
NUSU/
├── backend/            Spring Boot 3 (Java 17, Gradle, JPA, validation)
├── frontend/           index.html, style.css, app.js, nginx.conf
├── docker-compose.yml
├── .env.example
└── README.md
```

## API

| Method | Đường dẫn | Mô tả |
| --- | --- | --- |
| GET | `/api/stats` | Số liệu tổng quan |
| GET / POST | `/api/spaceships` | Danh sách / thêm tàu |
| GET / PUT / DELETE | `/api/spaceships/{id}` | Xem / sửa / xóa tàu |
| GET / POST | `/api/astronauts` | Danh sách / thêm phi hành gia |
| GET / PUT / DELETE | `/api/astronauts/{id}` | Xem / sửa / xóa phi hành gia |

Quy tắc nghiệp vụ: tên tàu không được trùng; không phân công vượt sức chứa của tàu; xóa tàu thì phi hành gia trên tàu đó chuyển về "chưa phân công". Lần chạy đầu tự thêm vài dữ liệu mẫu.

## Chạy thử trên máy cá nhân

```bash
cp .env.example .env
```

Mở `.env`, đổi `WEB_PORT=8081` (tránh chiếm cổng 80), rồi:

```bash
docker compose up -d --build
docker compose ps
```

Lần build đầu mất vài phút. Mở http://localhost:8081. Xem log: `docker compose logs -f backend`. Dừng: `docker compose down` (dữ liệu vẫn giữ trong volume `nusu-data`).

## Deploy lên VPS (Ubuntu 22.04 / 24.04)

Khuyến nghị VPS 2 GB RAM. Build Gradle trên 1 GB RAM dễ bị treo, nếu chỉ có 1 GB thì tạo swap ở bước 2.

### 1. Kết nối VPS

```bash
ssh root@IP_VPS
```

### 2. Cập nhật hệ thống và (nếu RAM thấp) tạo swap

```bash
apt update && apt upgrade -y

fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
```

### 3. Cài Docker

```bash
curl -fsSL https://get.docker.com | sh
docker --version
docker compose version
```

### 4. Mở tường lửa

Mở SSH trước khi bật `ufw`, nếu không sẽ tự khóa mình khỏi VPS.

```bash
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
ufw status
```

### 5. Đưa mã nguồn lên VPS

Cách A, dùng GitHub (nên dùng): đẩy thư mục NUSU lên một repo, rồi trên VPS:

```bash
git clone https://github.com/<ten-ban>/<repo>.git
cd <repo>
```

Cách B, copy trực tiếp từ máy ông (chạy trên máy cá nhân, không phải VPS):

```bash
scp -r ./NUSU root@IP_VPS:/root/
```

### 6. Cấu hình `.env`

```bash
cd /root/NUSU
cp .env.example .env
nano .env
```

Đổi `DB_PASSWORD` thành mật khẩu mạnh, giữ `WEB_PORT=80`. Lưu bằng Ctrl+O, Enter, Ctrl+X.

### 7. Chạy

```bash
docker compose up -d --build
docker compose ps
```

Kết quả đạt: `nusu-db` ở trạng thái `Up (healthy)`, `nusu-backend` và `nusu-frontend` ở trạng thái `Up`. Mở trình duyệt vào `http://IP_VPS`.

Kiểm tra nhanh API ngay trên VPS: `curl http://localhost/api/stats`

### 8. Gắn tên miền và HTTPS (tuỳ chọn)

1. Tại nơi mua tên miền, tạo bản ghi `A` trỏ `nusu.example.com` về `IP_VPS`.
2. Trong `.env` đổi `WEB_PORT=8081`, và trong `docker-compose.yml` sửa dòng ports của `frontend` thành `"127.0.0.1:${WEB_PORT}:80"` (Docker mở cổng bỏ qua `ufw`, nên phải bind vào localhost mới thực sự đóng với bên ngoài).
3. Chạy lại: `docker compose up -d`
4. Cài Caddy làm reverse proxy, tự cấp chứng chỉ HTTPS (xem hướng dẫn cài tại caddyserver.com/docs/install), rồi sửa `/etc/caddy/Caddyfile`:

```
nusu.example.com {
    reverse_proxy localhost:8081
}
```

```bash
systemctl reload caddy
```

### 9. Cập nhật phiên bản mới

```bash
cd /root/NUSU
git pull
docker compose up -d --build
```

### 10. Sao lưu dữ liệu

```bash
docker compose exec -T database pg_dump -U nusu nusu > backup.sql
```

(Thay `nusu nusu` bằng `DB_USER` và `DB_NAME` trong `.env` nếu ông đã đổi.)

## Xử lý lỗi thường gặp

| Hiện tượng | Nguyên nhân / cách xử lý |
| --- | --- |
| Không vào được `http://IP_VPS` | Kiểm tra `ufw status` đã mở 80, và `docker compose ps` đủ 3 container |
| Trang mở nhưng báo lỗi tải dữ liệu | `docker compose logs backend`, thường do backend chưa lên hoặc sai mật khẩu DB |
| Đổi `DB_PASSWORD` rồi backend không kết nối được | Mật khẩu chỉ được áp dụng lần tạo DB đầu tiên. Đổi lại đúng mật khẩu cũ, hoặc xóa volume (mất dữ liệu): `docker compose down -v` |
| `port is already allocated` | Cổng `WEB_PORT` đang bị chiếm, đổi sang cổng khác trong `.env` |
| Build bị `Killed` | Thiếu RAM, tạo swap ở bước 2 |

## Lưu ý

- `spring.jpa.hibernate.ddl-auto=update` tiện cho demo; môi trường thật nên dùng Flyway hoặc Liquibase.
- Chưa có đăng nhập. Nếu để website công khai, bất kỳ ai cũng sửa/xóa được dữ liệu, nên thêm xác thực hoặc chỉ để dùng thử.
- Mã nguồn chưa được build hay chạy thử trong môi trường của tui, ông chạy `docker compose up -d --build` và báo lỗi nếu có.
