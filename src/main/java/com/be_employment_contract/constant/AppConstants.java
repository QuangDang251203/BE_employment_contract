package com.be_employment_contract.constant;

public final class AppConstants {

    private AppConstants() {
    }

    public static final String REDIS_OTP_PREFIX = "otp";
    public static final int OTP_LENGTH = 6;
    public static final long OTP_TTL_MINUTES = 5L;
    public static final int INITIAL_PASSWORD_LENGTH = 10;
    public static final String UPLOAD_ROOT_DIR = "uploads";
    public static final String STAFF_DOCUMENT_STORAGE_DIR = "C:/Users/Hi/OneDrive - utt.vn/DOCUMENT_FOR_EMPLOYMENT_CONTRACT";
    public static final String CONTRACT_TEMPLATE_CLASSPATH = "templates/[AGRI] Hợp đồng thử việc.docx";
    public static final String CONTRACT_OUTPUT_DIR = "C:/Users/Hi/OneDrive - utt.vn/CÔNG VIỆC/Template_demo";
    public static final String CONTRACT_FILE_TYPE_GENERATED_PDF = "GENERATED_PDF";
    public static final String CONTRACT_FILE_TYPE_SIGNED_PDF = "SIGNED_PDF";
    public static final String CONTRACT_FILE_TYPE_STAMPED_PDF = "STAMPED_PDF";

    public static final String EMAIL_SUBJECT_CREDENTIAL = "Employment Contract - Account Information";
    public static final String EMAIL_SUBJECT_OTP = "Employment Contract - OTP Verification";
}
