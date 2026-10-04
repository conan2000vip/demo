Có, **mô hình bạn mô tả làm được**, và vẫn dùng chung một tài khoản. Cần phân biệt **ai đang sử dụng app** với **profile nào đang được xem**:

1. **Lần đầu:** chọn profile của mình và tạo PIN. Những lần đăng nhập sau, nhập PIN để xác nhận mình là người đó. PIN không cần thiết lập lại mỗi lần.
2. Sau khi xác nhận, app biết người đang dùng là **TA THI HAI YEN**. Khi chọn xem profile **PHUNG CHI DUNG**, đó là xem dữ liệu của chồng với tư cách TA — không phải biến TA thành chồng.
3. App kiểm tra cài đặt chia sẻ của PHUNG CHI DUNG đối với TA theo từng danh mục. Nếu chồng không chia sẻ cân nặng, các trang, biểu đồ và dữ liệu cân nặng của chồng sẽ bị chặn; những danh mục được chia sẻ vẫn xem bình thường.
4. Khi chuyển ngược lại profile khác, quyền xem vẫn dựa trên **người đã xác thực bằng PIN**, không dựa vào profile đang hiển thị. Như vậy không thể bấm switch để mạo nhận người khác.

Ví dụ: TA đã xác thực bằng PIN của mình, rồi mở profile của PHUNG. Nếu PHUNG cho TA xem giấc ngủ nhưng không cho xem cân nặng, TA xem được giấc ngủ còn cân nặng bị chặn — kể cả khi cố mở đường dẫn hoặc biểu đồ trực tiếp.

Điểm quan trọng: **không cần PIN mỗi lần switch**, nếu switch chỉ là xem profile theo quyền được chia sẻ. Nhưng PIN phải xác nhận người dùng ở bước vào app; việc chặn dữ liệu phải được thực thi ở máy chủ cho mọi trang và API, không chỉ ẩn mục trên giao diện. Với tài khoản chung, quy trình quên/đặt lại PIN cũng cần thiết kế cẩn thận để người khác không thể dùng chức năng đặt lại nhằm mạo danh chủ profile.
