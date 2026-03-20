# BE Employment Contract

## Overview
Backend Spring Boot cho quy trinh hop dong lao dong:
- Tao nguoi lao dong + tao hop dong voi trang thai `PENDING_SIGN`
- Gui email thong tin dang nhap (username/password)
- Dang nhap sinh OTP, luu OTP vao Redis va gui qua email
- Xac thuc OTP de chuyen trang thai hop dong thanh `COMPLETED`

## Architecture
Code duoc tach theo cac lop:
- `controller`: nhan request/tra response
- `service`: xu ly nghiep vu
- `repository`: truy cap database
- `dto`: request/response object
- `entity`: JPA model
- `mapper`: chuyen doi entity <-> dto
- `constant`, `utils`, `response`, `exception`: ho tro mo rong va bao tri

## API
Base path: `/api/contracts`

1. `POST /init`
   - Tao staff + account + contract
   - Request body:
```json
{
  "fullName": "Nguyen Van A",
  "email": "a@example.com",
  "dateOfBirth": "1999-01-15",
  "address": "Ha Noi",
  "dateIssued": "2020-01-10",
  "issuingLocation": "Ha Noi",
  "levelOfTraining": "Dai hoc",
  "branchId": 1,
  "decisionNumber": "D1001",
  "decisionDate": "2026-03-20",
  "startDate": "2026-03-20",
  "endDate": "2026-12-31",
  "level": "Nhan vien",
  "salaryRank": "Bac 1",
  "percentageOfSalary": 85.50,
  "probationarySalary": 12000000
}
```

2. `POST /login`
   - Dang nhap va gui OTP
```json
{
  "username": "nguyenvana",
  "password": "raw_password_from_email",
  "contractCode": "CT1742386381000"
}
```

3. `POST /verify-otp`
   - Xac thuc OTP va hoan tat hop dong
```json
{
  "username": "nguyenvana",
  "contractCode": "CT1742386381000",
  "otpCode": "123456"
}
```

## Quick Try
```powershell
.\mvnw.cmd -Dtest=CredentialUtilsTest,OtpUtilsTest test
.\mvnw.cmd spring-boot:run
```

## Notes
- Can cau hinh dung MariaDB + Redis + SMTP trong `src/main/resources/application.properties`.
- OTP mac dinh 6 so, het han sau 5 phut.
- Password duoc bam voi PBKDF2 truoc khi luu DB.
- Dung script `database/schema.sql` de tao schema dong bo entity.

