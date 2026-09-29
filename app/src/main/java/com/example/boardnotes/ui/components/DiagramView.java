package com.example.boardnotes.ui.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;

import com.example.boardnotes.R;
import com.example.boardnotes.domain.DiagramModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DiagramView extends View {
    private static final int FALLBACK_COLUMNS = 3;
    private static final int MAX_LABEL_LINES = 3;
    private static final double ARROW_SPREAD = Math.toRadians(25);

    public interface OnNodeSelectedListener {
        void onNodeSelected(String nodeId, String label);
    }

    private final Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgeLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrowPath = new Path();
    private final float[] line = new float[4];
    private final float arrowLength;
    private final float labelGap;

    @ColorInt private int inkColor;
    private float nodeCornerRadius;
    private float labelTextSize;
    private float nodeWidth;
    private float layoutSpacing;

    @Nullable private String diagramJson;
    private List<DiagramModel.Node> nodes = Collections.emptyList();
    private List<DiagramModel.Edge> edges = Collections.emptyList();
    private final Map<String, RectF> boxes = new HashMap<>();
    private final Map<String, List<String>> labelLines = new HashMap<>();
    private float contentWidth;
    private float contentHeight;

    @Nullable private OnNodeSelectedListener listener;
    @Nullable private String selectedId;
    private float drawScale = 1f;
    private float drawLeft;
    private float drawTop;

    public DiagramView(@NonNull Context context) {
        this(context, null);
    }

    public DiagramView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        arrowLength = dp(8);
        labelGap = dp(4);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DiagramView);
        try {
            inkColor = a.getColor(R.styleable.DiagramView_inkColor, 0xFF000000);
            nodeCornerRadius = a.getDimension(R.styleable.DiagramView_nodeCornerRadius, dp(8));
            labelTextSize = a.getDimension(R.styleable.DiagramView_labelTextSize, sp(12));
            nodeWidth = a.getDimension(R.styleable.DiagramView_nodeWidth, dp(90));
            layoutSpacing = a.getDimension(R.styleable.DiagramView_layoutSpacing, dp(16));
        } finally {
            // TypedArrays come from a shared pool. Handing it back is mandatory.
            a.recycle();
        }
        applyPaints();
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
        selectedId = null;
        placeBoxes();
        invalidate();
    }

    private void placeBoxes() {
        boxes.clear();
        contentWidth = 0;
        contentHeight = 0;
        labelLines.clear();

        boolean hasPositions = !nodes.isEmpty();
        for (DiagramModel.Node node : nodes) {
            if (node.row < 0 || node.col < 0) {
                hasPositions = false;
            }
        }

        float padding = layoutSpacing / 2;
        int mostLines = 1;
        for (DiagramModel.Node node : nodes) {
            List<String> lines = wrap(node.label, nodeWidth - 2 * padding);
            labelLines.put(node.id, lines);
            mostLines = Math.max(mostLines, lines.size());
        }
        float nodeHeight = mostLines * textPaint.getFontSpacing() + 2 * padding;

        float columnGap = layoutSpacing * 4;
        Set<String> taken = new HashSet<>();
        for (int i = 0; i < nodes.size(); i++) {
            DiagramModel.Node node = nodes.get(i);
            int row = hasPositions ? node.row : i / FALLBACK_COLUMNS;
            int col = hasPositions ? node.col : i % FALLBACK_COLUMNS;
            while (!taken.add(row + "," + col)) {
                col++;
            }
            float left = col * (nodeWidth + columnGap);
            float top = row * (nodeHeight + layoutSpacing * 2);
            boxes.put(node.id, new RectF(left, top, left + nodeWidth, top + nodeHeight));
            contentWidth = Math.max(contentWidth, left + nodeWidth);
            contentHeight = Math.max(contentHeight, top + nodeHeight);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (boxes.isEmpty() || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        drawScale = Math.min(1f, Math.min(getWidth() / contentWidth,
                getHeight() / contentHeight));
        drawLeft = (getWidth() - contentWidth * drawScale) / 2;
        drawTop = (getHeight() - contentHeight * drawScale) / 2;
        canvas.save();
        canvas.translate(drawLeft, drawTop);
        canvas.scale(drawScale, drawScale);

        canvas.save();
        for (RectF box : boxes.values()) {
            clipOut(canvas, box);
        }
        for (DiagramModel.Edge edge : edges) {
            if (connect(boxes.get(edge.from), boxes.get(edge.to))) {
                canvas.drawLine(line[0], line[1], line[2], line[3], boxPaint);
                drawArrowhead(canvas);
                if (!edge.label.isEmpty()) {
                    drawEdgeLabel(canvas, edge.label);
                }
            }
        }
        canvas.restore();
        for (DiagramModel.Node node : nodes) {
            drawNode(canvas, node);
        }
        canvas.restore();
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

    private void applyPaints() {
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(dp(1.5f));
        boxPaint.setColor(inkColor);

        textPaint.setColor(inkColor);
        textPaint.setTextSize(labelTextSize);
        textPaint.setTextAlign(Paint.Align.CENTER);

        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setColor(inkColor);

        edgeLabelPaint.setColor(inkColor);
        edgeLabelPaint.setTextSize(labelTextSize * 0.85f);

        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(dp(3.5f));
        selectedPaint.setColor(inkColor);
    }

    public void setInkColor(@ColorInt int color) {
        inkColor = color;
        applyPaints();
        invalidate();
    }

    public void setNodeCornerRadius(@Px float radius) {
        nodeCornerRadius = radius;
        invalidate();
    }

    public void setLabelTextSize(@Px float size) {
        labelTextSize = size;
        applyPaints();
        placeBoxes();
        invalidate();
    }

    public void setNodeWidth(@Px float width) {
        nodeWidth = width;
        placeBoxes();
        invalidate();
    }

    public void setLayoutSpacing(@Px float spacing) {
        layoutSpacing = spacing;
        placeBoxes();
        invalidate();
    }

    @SuppressWarnings("deprecation")
    private static void clipOut(Canvas canvas, RectF box) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.clipOutRect(box);
        } else {
            canvas.clipRect(box, Region.Op.DIFFERENCE);
        }
    }

    private void drawNode(Canvas canvas, DiagramModel.Node node) {
        RectF box = boxes.get(node.id);
        Paint outline = node.id.equals(selectedId) ? selectedPaint : boxPaint;
        canvas.drawRoundRect(box, nodeCornerRadius, nodeCornerRadius, outline);

        List<String> lines = labelLines.get(node.id);
        float lineHeight = textPaint.getFontSpacing();
        float baseline = box.centerY() - lines.size() * lineHeight / 2 - textPaint.ascent();
        for (String text : lines) {
            canvas.drawText(text, box.centerX(), baseline, textPaint);
            baseline += lineHeight;
        }
    }

    private void drawArrowhead(Canvas canvas) {
        float tipX = line[2];
        float tipY = line[3];
        double angle = Math.atan2(tipY - line[1], tipX - line[0]);
        arrowPath.reset();
        arrowPath.moveTo(tipX, tipY);
        arrowPath.lineTo(tipX - arrowLength * (float) Math.cos(angle - ARROW_SPREAD),
                tipY - arrowLength * (float) Math.sin(angle - ARROW_SPREAD));
        arrowPath.lineTo(tipX - arrowLength * (float) Math.cos(angle + ARROW_SPREAD),
                tipY - arrowLength * (float) Math.sin(angle + ARROW_SPREAD));
        arrowPath.close();
        canvas.drawPath(arrowPath, arrowPaint);
    }

    private void drawEdgeLabel(Canvas canvas, String label) {
        float midX = (line[0] + line[2]) / 2;
        float midY = (line[1] + line[3]) / 2;
        if (line[1] == line[3]) {
            canvas.drawText(label, midX - edgeLabelPaint.measureText(label) / 2,
                    midY - labelGap, edgeLabelPaint);
        } else {
            canvas.drawText(label, midX + labelGap,
                    midY - (edgeLabelPaint.ascent() + edgeLabelPaint.descent()) / 2,
                    edgeLabelPaint);
        }
    }

    private List<String> wrap(String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && textPaint.measureText(candidate) > maxWidth) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(candidate);
            }
        }
        lines.add(line.toString());

        if (lines.size() > MAX_LABEL_LINES) {
            lines = new ArrayList<>(lines.subList(0, MAX_LABEL_LINES));
            String last = lines.get(MAX_LABEL_LINES - 1);
            while (last.length() > 1 && textPaint.measureText(last + "...") > maxWidth) {
                last = last.substring(0, last.length() - 1);
            }
            lines.set(MAX_LABEL_LINES - 1, last.trim() + "...");
        }
        return lines;
    }

    public void setOnNodeSelectedListener(@Nullable OnNodeSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            DiagramModel.Node tapped = nodeAt(event.getX(), event.getY());
            if (tapped != null) {
                selectedId = tapped.id;
                invalidate();
                if (listener != null) {
                    listener.onNodeSelected(tapped.id, tapped.label);
                }
            }
            performClick();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Nullable
    private DiagramModel.Node nodeAt(float x, float y) {
        float diagramX = (x - drawLeft) / drawScale;
        float diagramY = (y - drawTop) / drawScale;
        for (DiagramModel.Node node : nodes) {
            if (boxes.get(node.id).contains(diagramX, diagramY)) {
                return node;
            }
        }
        return null;
    }
}
