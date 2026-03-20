package com.be_employment_contract.utils;

public final class MailTemplateUtils {

    private MailTemplateUtils() {
    }

    public static String credentialMailBody(String fullName, String username, String password, String ContractCode) {
        return "Hello " + fullName + ",\n\n"
                + "Congratulations on your new employment contract with code " + ContractCode + "!\n"
                + "Your account has been created for Employment Contract system.\n"
                + "Username: " + username + "\n"
                + "Password: " + password + "\n\n"
                + "Please login and complete OTP verification to sign your contract.\n"
                + "Regards.";
    }

    public static String otpMailBody(String fullName, String otpCode, long ttlMinutes) {
        return "Hello " + fullName + ",\n\n"
                + "Your OTP code is: " + otpCode + "\n"
                + "Code will expire in " + ttlMinutes + " minute(s).\n\n"
                + "If this was not requested by you, please ignore this email.";
    }
}

