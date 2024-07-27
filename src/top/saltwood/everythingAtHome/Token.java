package top.saltwood.everythingAtHome;

import com.sun.net.httpserver.HttpExchange;

import java.lang.reflect.Field;

public class Token {
    public String value;
    public boolean permissionRequestUpdateFiles = false;
    public boolean permissionRequestSaveAll = false;
    public boolean permissionRequestAddCluster = false;
    public boolean permissionRequestListCluster = false;
    public boolean permissionRequestRemoveCluster = false;
    public boolean permissionRequestNewCluster = false;
    public boolean permissionRequestAddFile = false;
    public boolean permissionRequestListFile = false;
    public boolean permissionRequestRemoveFile = false;
    public boolean permissionRequestDownloadFileFromCenter = false;
    public boolean permissionAll = false; // not recommended
    
    public Token() {
        this.value = Utils.generateRandomHexString(64);
    }
    
    public boolean verifyPermission(HttpExchange exchange, String permission) {
        String token = Utils.parseBodyToDictionary(exchange.getRequestURI().getQuery()).get("token").toString();
        if (token == null || !token.equals(this.value)) {
            return false;
        }
        boolean isOn = false;
        try {
            Field field = this.getClass().getField(permission);
            isOn = (boolean) field.get(this);
        } catch (Exception e) {
            isOn = false;
        }
        return isOn || this.permissionAll;
    }
    
    public boolean setPermission(String permission, boolean status) {
        try {
            Field field = this.getClass().getField(permission);
            field.set(this, status);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
