package com.example.chathurang;

public final class ThemeManager {

    private static GameTheme currentTheme = GameTheme.CLASSIC;

    private ThemeManager() {}

    public static GameTheme getTheme() {
        return currentTheme;
    }

    public static void toggleTheme() {
        currentTheme = (currentTheme == GameTheme.CLASSIC)
                ? GameTheme.DARK_ELEGANT
                : GameTheme.CLASSIC;
    }

    public static void setTheme(GameTheme theme) {
        currentTheme = theme;
    }
}
