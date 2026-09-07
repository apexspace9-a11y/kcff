package com.apex.kcff;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class ExpenseChartView extends View {
    private int[] values = new int[7];
    private String[] labels = new String[]{"T2","T3","T4","T5","T6","T7","CN"};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int accent = Color.rgb(79, 209, 197);
    private final int muted = Color.rgb(142, 163, 188);
    private final int grid = Color.rgb(38, 55, 76);
    private final float density;

    public ExpenseChartView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        setMinimumHeight(dp(190));
    }

    public void setData(int[] input, String[] inputLabels) {
        if (input != null && input.length == 7) values = input.clone();
        if (inputLabels != null && inputLabels.length == 7) labels = inputLabels.clone();
        invalidate();
    }

    private int dp(int n) {
        return (int) (n * density + 0.5f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        int left = dp(8);
        int right = w - dp(8);
        int top = dp(18);
        int bottom = h - dp(34);
        int chartH = Math.max(dp(80), bottom - top);
        int max = 1;
        for (int v : values) max = Math.max(max, v);

        paint.setStrokeWidth(dp(1));
        paint.setColor(grid);
        for (int i = 0; i < 3; i++) {
            float y = top + chartH * (i / 2f);
            canvas.drawLine(left, y, right, y, paint);
        }

        float slot = (right - left) / 7f;
        float barW = slot * 0.52f;
        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < 7; i++) {
            float cx = left + slot * i + slot / 2f;
            float ratio = values[i] / (float) max;
            float barH = ratio * (chartH - dp(16));
            float y = bottom - barH;
            paint.setColor(accent);
            RectF r = new RectF(cx - barW / 2f, y, cx + barW / 2f, bottom);
            canvas.drawRoundRect(r, dp(5), dp(5), paint);

            paint.setColor(muted);
            paint.setTextSize(dp(10));
            canvas.drawText(labels[i], cx, h - dp(10), paint);
            if (values[i] > 0) {
                paint.setColor(Color.WHITE);
                paint.setTextSize(dp(9));
                canvas.drawText(String.valueOf(values[i]), cx, Math.max(dp(12), y - dp(5)), paint);
            }
        }
    }
}
