package com.example.boardnotes.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.boardnotes.domain.DiagramModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DiagramView extends View {
    private static final int COLUMNS = 3;

    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] line = new float[4];

    private final float nodeWidth;
    private final float nodeHeight;
    private final float spacing;

    @Nullable private String diagramJson;
    private List<DiagramModel.Node> nodes = Collections.emptyList();
    private List<DiagramModel.Edge> edges = Collections.emptyList();
    private final Map<String, RectF> boxes = new HashMap<>();

    public DiagramView(@NonNull Context context) {
        this(context, null);
    }

    public DiagramView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        nodeWidth = dp(90);
        nodeHeight = dp(40);
        spacing = dp(16);

        // One colour for everything: lines, box outlines and text.
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(dp(1.5f));
        boxPaint.setColor(0xFF000000);

        textPaint.setColor(0xFF000000);
        textPaint.setTextSize(sp(12));
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setDiagramJson(@Nullable String json) {
        // LiveData re-delivers the whole screen state on every change.
        if (Objects.equals(json, diagramJson)) {
            return;
        }
        diagramJson = json;
        DiagramModel model = DiagramModel.fromJson(json);
        nodes = model == null ? Collections.emptyList() : model.nodes();
        edges = model == null ? Collections.emptyList() : model.edges();
        placeBoxes();
        invalidate();
    }

    private void placeBoxes() {
        boxes.clear();
        for (int i = 0; i < nodes.size(); i++) {
            float left = (i % COLUMNS) * (nodeWidth + spacing);
            float top = (i / COLUMNS) * (nodeHeight + spacing * 2);
            boxes.put(nodes.get(i).id, new RectF(left, top, left + nodeWidth, top + nodeHeight));
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        for (DiagramModel.Edge edge : edges) {
            if (connect(boxes.get(edge.from), boxes.get(edge.to))) {
                canvas.drawLine(line[0], line[1], line[2], line[3], boxPaint);
            }
        }
        for (DiagramModel.Node node : nodes) {
            RectF box = boxes.get(node.id);
            canvas.drawRect(box, boxPaint);
            float baseline = box.centerY() - (textPaint.ascent() + textPaint.descent()) / 2;
            canvas.drawText(node.label, box.centerX(), baseline, textPaint);
        }
    }

    private boolean connect(RectF from, RectF to) {
        if (from == to) {
            return false;   // an edge from a box to itself: not drawn
        }
        if (to.top >= from.bottom) {
            setLine(from.centerX(), from.bottom, to.centerX(), to.top);     // below
        } else if (to.bottom <= from.top) {
            setLine(from.centerX(), from.top, to.centerX(), to.bottom);     // above
        } else if (to.left >= from.right) {
            setLine(from.right, from.centerY(), to.left, to.centerY());     // to the right
        } else {
            setLine(from.left, from.centerY(), to.right, to.centerY());     // to the left
        }
        return true;
    }

    private void setLine(float x1, float y1, float x2, float y2) {
        line[0] = x1;
        line[1] = y1;
        line[2] = x2;
        line[3] = y2;
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                getResources().getDisplayMetrics());
    }
}
