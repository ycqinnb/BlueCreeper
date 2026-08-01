package yc.ycqin.doth.core;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class DOTHConfig {
    public static boolean enableAllReturn = false;
    public static boolean replaceEventBus = false;
    /** 强兼模式：ASM 给所有非白名单方法包 try-catch */
    public static boolean strongCompat;
    /** 替换 World.loadedEntityList 为自定义过滤列表，拦截禁生成实体 */
    public static boolean replaceEntityList = false;
    /** 拦截非白名单 mod 的事件监听器（需 replaceEventBus=true） */
    public static boolean blockModEvents;

    private static final String FILE_NAME = "config/doth.cfg";
    private static long lastModified;

    public static void reload() {
        try {
            Path path = Paths.get(FILE_NAME);
            if (!Files.exists(path)) { saveDefault(); return; }
            long mod = Files.getLastModifiedTime(path).toMillis();
            if (mod == lastModified) return;
            lastModified = mod;
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(path)) { p.load(r); }
            boolean needSave = false;
            if (!p.containsKey("enableAllReturn")) { needSave = true; }
            if (!p.containsKey("replaceEventBus")) { needSave = true; }
            if (!p.containsKey("strongCompat")) { needSave = true; }
            if (!p.containsKey("replaceEntityList")) { needSave = true; }
            if (!p.containsKey("blockModEvents")) { needSave = true; }
            enableAllReturn = Boolean.parseBoolean(p.getProperty("enableAllReturn", "false"));
            replaceEventBus = Boolean.parseBoolean(p.getProperty("replaceEventBus", "false"));
            strongCompat = Boolean.parseBoolean(p.getProperty("strongCompat", "false"));
            replaceEntityList = Boolean.parseBoolean(p.getProperty("replaceEntityList", "false"));
            blockModEvents = Boolean.parseBoolean(p.getProperty("blockModEvents", "false"));
            if (needSave) saveDefault();
        } catch (Exception e) {
            System.err.println("[DOTH] Config reload failed: " + e);
        }
    }

    private static void saveDefault() {
        try {
            Files.createDirectories(Paths.get("config"));
            try (Writer w = Files.newBufferedWriter(Paths.get(FILE_NAME))) {
                w.write("# ========================================\r\n");
                w.write("# 蓝C的小剑剑 (BlueCreeperSword) 配置\r\n");
                w.write("# ========================================\r\n");
                w.write("#\r\n");
                w.write("# enableAllReturn  : 是否启用allreturn注入，不启用游戏内开启无效\r\n");
                w.write("# replaceEventBus  : 替换Forge事件总线,渲染相关的防御需要打开这个\r\n");
                w.write("# strongCompat     : 强兼模式，替换双端 run() 尝试吃掉报错\r\n");
                w.write("# replaceEntityList: 替换 loadedEntityList 以增强禁生成\r\n");
                w.write("# blockModEvents   : 拦截事件，需开启replaceEventBus才能生效\r\n");
                w.write("#\r\n");
                w.write("enableAllReturn=" + enableAllReturn + "\r\n");
                w.write("replaceEventBus=" + replaceEventBus + "\r\n");
                w.write("strongCompat=" + strongCompat + "\r\n");
                w.write("replaceEntityList=" + replaceEntityList + "\r\n");
                w.write("blockModEvents=" + blockModEvents + "\r\n");
            }
        } catch (Exception e) { /* ignore */ }
    }
}
