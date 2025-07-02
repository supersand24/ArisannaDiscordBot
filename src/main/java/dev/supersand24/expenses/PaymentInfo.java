package dev.supersand24.expenses;

public class PaymentInfo {

    private String appName;
    private String detail;

    public PaymentInfo(String appName, String detail) {
        this.appName = appName;
        this.detail = detail;
    }

    public String getAppName() { return appName; }
    public String getDetail() { return detail; }

}
