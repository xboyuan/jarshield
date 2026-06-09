package com.by2tech.jarshield;

import com.by2tech.jarshield.util.IoUtils;
import com.by2tech.jarshield.util.StrUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 安装 Spring Boot 控制台 banner，标明应用受 JarShield 保护。
 */
public class ShieldBanner {

    private static final String BANNER_RESOURCE = "/banner-jarshield.txt";
    private static final String MARKER = ":: JarShield ::";

    /**
     * 将 JarShield banner 写入 BOOT-INF/classes/banner.txt。
     * 若应用已有 banner.txt，则在顶部追加 JarShield 标识（不覆盖原内容）。
     */
    public static void installBanner(File targetClassesDir) {
        if (targetClassesDir == null || !targetClassesDir.isDirectory()) {
            return;
        }
        String shieldBanner = loadBannerText();
        if (StrUtils.isEmpty(shieldBanner)) {
            return;
        }
        File appBanner = new File(targetClassesDir, "banner.txt");
        if (appBanner.exists()) {
            String existing = IoUtils.readTxtFile(appBanner);
            if (existing != null && existing.contains(MARKER)) {
                return;
            }
            IoUtils.writeTxtFile(appBanner, shieldBanner + "\n" + (existing == null ? "" : existing));
        } else {
            IoUtils.writeTxtFile(appBanner, shieldBanner);
        }
    }

    private static String loadBannerText() {
        try (InputStream in = ShieldBanner.class.getResourceAsStream(BANNER_RESOURCE)) {
            if (in == null) {
                return null;
            }
            String text = new String(IoUtils.toBytes(in), StandardCharsets.UTF_8);
            return text.replace("${jarshield.version}", Const.VERSION);
        } catch (Exception e) {
            return null;
        }
    }
}
