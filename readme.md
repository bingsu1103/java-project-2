# Bài tập: Ứng dụng Chat

- Môn học: Lập trình ứng dụng Java
  Lớp: CQ2024-7
- Giảng viên lý thuyết: ThS Nguyễn Văn Khiết
- Giảng viên thực hành: ThS Mai Anh Tuấn và ThS Hồ Tuấn Thanh
- Liên hệ hỏi đáp bài tập: khuyến khích hỏi trên Moodle hoặc hỏi qua email (với các câu hỏi riêng tư)

# Yêu cầu chung

- Đây là bài tập cá nhân, mỗi SV tự làm. Các trường hợp giống bài của nhau, giống bài làm trên Internet (ở một mức độ nhất định) sẽ tính là gian lận trong học tập, dẫn đến kết quả là 0đ toàn bộ môn học.
- SV cần nộp những tài liệu sau đây:
  - Toàn bộ source code bài làm.
  - File JAR có thể chạy được.
  - File README.md hướng dẫn chạy chương trình (client, server).
  - Một file báo cáo quá trình làm việc, viết bằng định dạng Markdown, sau đó convert sang PDF (nộp file Markdown + file PDF) chứa các nội dung sau đây: MSSV, Họ tên, Mức độ sử dụng AI trong project (0% -> 100%), Tuyên bố sử dụng AI theo mẫu A hoặc mẫu B (xem file AI Usage Guideline), Một bảng đánh giá các chức năng, các công việc đánh hoàn thành, gồm các cột: Tên tính năng, Điểm tự đánh giá (xem thang điểm bên dưới).
  - Một text file (txt hoặc md) chứa toàn bộ git commit log của project của tất cả các branch (nếu có nhiều branch). Lưu ý: yêu cầu git là default trong việc lập trình. Trong bài tập này, yêu cầu git hơi trễ, nên sẽ ko chấm 0đ nếu ko sử dụng git, nhưng chắc chắn là có điểm trừ nếu thiếu phần này.
  - Link Youtube Playlist (mode=Unlist hoặc Public) để demo hướng dẫn sử dụng từng tính năng. Tính năng nào ko demo, xem như ko làm, ko được chấm điểm.
- SV nếu thiếu một trong các tài liệu trên (trừ việc thiếu git), sẽ được chấm 0đ bài tập này.
- Bài làm được zip lại thành file có định dạng MSSV.zip. Link nộp bài nộp được tối đa 20 files, mỗi file tối đa 20MB => bài nộp tối đa 200MB. Nếu 1 file zip bài làm quá 20MB, SV có thể dùng các tính năng zip and split trên WinRar (hoặc các app tương tự) để split ra nhiều part khi nộp. Các bài nộp link Google Drive, One Drive, Dropbox sẽ không được chấm điểm.
- Nộp bài trên Moodle đúng giờ. Không nhận bài trễ.

# Một số yêu cầu kỹ thuật

1. Ngôn ngữ lập trình Java
2. SV cần viết app Java Swing hoặc Java FX. Các bài làm dạng web app, mobile app sẽ không được chấm điểm.
3. Sử dụng Git / Github để quản lý source code version.
4. Repo cần để mode Private tránh bị mất source code.
5. Nguyên tắc là SV có thể dùng AI hỗ trợ trong quá trình làm bài này. Nhưng: cần sử dụng AI một cách bài bản, ghi nhận lại quá trình sử dụng AI theo file AI Guideline Usage, tự đánh giá được mức độ đóng góp của mình là bao nhiêu, mức độ đóng góp của AI là bao nhiêu trong bài tập, từ đó tìm cách nâng cao chất lượng bài làm, mức độ đóng góp của cá nhân so với SV. Mô tả phần này kỹ trong file tự đánh giá.

# Đề bài

Sinh viên viết một chương trình chat (giao diện đồ họa) có các chức năng sau:

- Đăng ký chat user (đăng ký từ ứng dụng client), đăng nhập sau đăng ký.
- Chương trình cho phép một user có thể chat với nhiều user khác (đang online) cùng lúc.
- Chương trình cho phép user tạo các group chat và chat trong các group này.
- Cho phép gởi file trong khi chat.
- Cho người dùng xem lịch sử chat của mình, xoá các dòng lịch sử chat.
- Các chức năng không bắt buộc: voice chat, webcam.

- [1 điểm] Màn hình Server với đủ thông tin: config, đóng / mở server, danh sách client đang kết nối.
- [1 điểm] Màn hình Client đủ thông tin về các chức năng cơ bản của chương trình: danh sách server để kết nối / thông tin của server nếu đang kết nối, danh sách các user đang online.
- [1 điểm] Cho phép quản lý danh sách server cho client như: thêm, xóa, sửa và ghi nhớ dạng file config.
- [1 điểm] Đăng ký chat user (đăng ký từ ứng dụng client).
- [1 điểm] Khung chat với đầy đủ thông tin người chat, nội dung chat.
- [0.5 điểm] Cho phép lựa chọn ENTER là gửi nội dung chat hoặc xuống dòng (cho phép nội dung chat nhiều dòng).
- [0.5 điểm] Cho phép sử dụng emoji đơn giản (tối thiểu 5 emoji).
- [1 điểm] Cho phép chat với nhiều user (chia nhiều tab).
- [1 điểm] Cho phép gởi file trong khi chat.
- [1 điểm] Cho phép tạo group chat và thực hiện chat nhóm.
- [1 điểm] Cho phép xem lịch sử chat của mình, xoá các dòng lịch sử chat.
- [1 điểm] Cho phép sử dụng voice chat.
- [1 điểm] Cho phép sử dụng webcam.
