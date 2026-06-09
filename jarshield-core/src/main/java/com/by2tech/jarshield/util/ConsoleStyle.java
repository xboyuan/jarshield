package com.by2tech.jarshield.util;

import com.by2tech.jarshield.Const;

/**
 * 控制台 ANSI 彩色输出（agent / CLI 共用）。
 */
public final class ConsoleStyle {

    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String DIM = "\033[2m";
    private static final String RED = "\033[91m";
    private static final String GREEN = "\033[92m";
    private static final String YELLOW = "\033[93m";
    private static final String BLUE = "\033[94m";
    private static final String MAGENTA = "\033[95m";
    private static final String CYAN = "\033[96m";
    private static final String WHITE = "\033[97m";
    private static final String GRAY = "\033[90m";

    private static final String BORDER = "══════════════════════════════════════════════════════";

    private ConsoleStyle() {
    }

    private static boolean useColor() {
        if ("false".equalsIgnoreCase(System.getProperty("jarshield.color"))) {
            return false;
        }
        if ("dumb".equals(System.getenv("TERM"))) {
            return false;
        }
        return true;
    }

    private static String paint(String code, String text) {
        if (!useColor()) {
            return text;
        }
        return code + text + RESET;
    }

    /**
     * Agent 启动标识（含 Provider 来源），不闭合底边，便于失败时追加错误行。
     */
    public static void printAgentHeader() {
        System.out.println();
        System.out.println(paint(CYAN, "  " + BORDER));
        System.out.println(paint(CYAN, "  ║ ") + paint(BOLD + WHITE, "JarShield")
                + paint(GRAY, " " + Const.VERSION)
                + paint(CYAN, "  ·  ")
                + paint(GREEN, "Spring Boot 3+"));
        System.out.println(paint(CYAN, "  ║ ")
                + paint(DIM, "Provider: ")
                + paint(MAGENTA, "com.by2tech"));
    }

    /** 正常通过密码校验后闭合 header（失败路径由 {@link #printStartupFailed} 闭合）。 */
    public static void printAgentFooter() {
        System.out.println(paint(CYAN, "  " + BORDER));
        System.out.println();
    }

    /**
     * CLI 完整方框 banner（交互模式）。
     */
    public static void printCliBanner() {
        System.out.println();
        String[] rainbow = {RED, YELLOW, GREEN, CYAN, BLUE, MAGENTA};
        if (!useColor()) {
            System.out.println("=========================================================");
            System.out.println("=      JarShield " + Const.VERSION + "   Spring Boot 3+        =");
            System.out.println("=      Provider: com.by2tech                            =");
            System.out.println("=========================================================");
            System.out.println();
            return;
        }
        StringBuilder top = new StringBuilder("  ");
        for (int i = 0; i < 54; i++) {
            top.append(rainbow[i % rainbow.length]).append("=").append(RESET);
        }
        System.out.println(top);
        System.out.println(paint(BLUE, "  = ") + paint(BOLD + CYAN, "JarShield")
                + paint(WHITE, " " + Const.VERSION)
                + paint(GREEN, "   Spring Boot 3+")
                + paint(BLUE, "     ="));
        System.out.println(paint(BLUE, "  = ") + paint(DIM, "Provider: ")
                + paint(MAGENTA, "com.by2tech")
                + paint(BLUE, "                          ="));
        StringBuilder bottom = new StringBuilder("  ");
        for (int i = 53; i >= 0; i--) {
            bottom.append(rainbow[i % rainbow.length]).append("=").append(RESET);
        }
        System.out.println(bottom);
        System.out.println();
    }

    /**
     * 启动失败（密码缺失 / 密码错误）。需先调用 {@link #printAgentHeader()}。
     */
    public static void printStartupFailed(String reason, String hint) {
        System.out.println(paint(CYAN, "  ║ ") + paint(BOLD + RED, "✖ Startup failed")
                + paint(RED, " — " + reason));
        if (StrUtils.isNotEmpty(hint)) {
            System.out.println(paint(CYAN, "  ║ ") + paint(YELLOW, "→ " + hint));
        }
        printAgentFooter();
    }

    /**
     * 密码校验通过（仅 debug 模式输出，避免与 Spring Boot banner 重复）。
     */
    public static void printPasswordVerified() {
        if (!Const.DEBUG) {
            return;
        }
        System.out.println(paint(GREEN, "  :: JarShield :: Password verified.") + "\n");
    }

    /**
     * 控制台密码输入提示。
     */
    public static String passwordPrompt() {
        return paint(CYAN, "JarShield") + paint(DIM, " Password: ");
    }
}
