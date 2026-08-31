package com.um.apicenter.admin;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.Console;
import java.util.Arrays;

public final class PasswordHashTool {

    private PasswordHashTool() {
    }

    public static void main(String[] args) {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("请从 PowerShell 或命令提示符运行此命令，以便安全读取密码。");
        }

        char[] password = console.readPassword("请输入管理员密码: ");
        char[] confirmation = console.readPassword("请再次输入管理员密码: ");
        try {
            if (password == null || password.length == 0 || !Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("两次密码不一致或密码为空。");
            }
            System.out.println(new BCryptPasswordEncoder().encode(new String(password)));
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
            if (confirmation != null) {
                Arrays.fill(confirmation, '\0');
            }
        }
    }
}
