package dev.jaycee.punchy;

public final class ProgressTextTest {
    public static void main(String[] args) {
        Object[][] cases = {
            {"14% of 275 MB", 14}, {"1.5 MB/s · 28.9%", 28}, {"Downloading 7,5 %", 7},
            {"٠١٤٪", 14}, {"７３％", 73}, {"100%", 100}, {"0%", 0},
            {"275 MB", -1}, {"1000%", -1}, {"101%", -1}, {"-25%", -1},
            {"1.234%", -1}, {null, -1}, {"Unknown total", -1}
        };
        for (Object[] example : cases) {
            int actual = ProgressText.percent((String) example[0]);
            if (actual != (int) example[1]) throw new AssertionError(example[0] + ": " + actual);
        }
        System.out.println("PASS: " + cases.length + " notification percentage cases");
    }
}
