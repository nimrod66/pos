package com.example.pos.terminal.printer;

public class LabelTemplate {

    public static final LabelTemplate SMALL_25x15 = new LabelTemplate(25, 15);
    public static final LabelTemplate STANDARD_50x25 = new LabelTemplate(50, 25);
    public static final LabelTemplate LARGE_75x50 = new LabelTemplate(75, 50);
    public static final LabelTemplate SHELF_100x30 = new LabelTemplate(100, 30);

    private final int widthMm;
    private final int heightMm;

    public LabelTemplate(int widthMm, int heightMm) {
        this.widthMm = widthMm;
        this.heightMm = heightMm;
    }

    public int widthMm() { return widthMm; }
    public int heightMm() { return heightMm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LabelTemplate that)) return false;
        return widthMm == that.widthMm && heightMm == that.heightMm;
    }

    @Override
    public int hashCode() {
        return 31 * widthMm + heightMm;
    }
}
