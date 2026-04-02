package com.be_employment_contract.utils;

public final class MailTemplateUtils {

    private MailTemplateUtils() {
    }

    public static String credentialMailBody(String fullName, String username, String password, String ContractCode) {
        return "Xin chào " + fullName + ",\n\n"
                + "Chúc mừng bạn đã chúng tuyển vào Ngân hàng nông nghiệp và phát triển nông thôn - Agribank "  + "!\n"
                + "Hãy truy cập vào đường dẫn http://localhost:3000/signAProbationContract/" + ContractCode + " để xem và ký hợp đồng thử việc của bạn.\n\n"
                + "Tài khoản của bạn đã được tạo bởi hệ thống hợp đồng điện tử.\n"
                + "Username: " + username + "\n"
                + "Password: " + password + "\n\n"
                + "Hãy đăng nhập để lấy mã xác thực OTP thực hiện ký hợp đồng.\n"
                + "Chúc mừng.";
    }

    public static String otpMailBody(String fullName, String otpCode, long ttlMinutes) {
        return "Xin chào " + fullName + ",\n\n"
                + "Mã OTP của bạn là: " + otpCode + "\n"
                + "Mã sẽ hết hạn trong " + ttlMinutes + " phút.\n\n"
                + "Nếu bạn không yêu cầu điều này, vui lòng bỏ qua email này.";
    }
}

