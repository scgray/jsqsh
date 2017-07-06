package org.sqsh.renderers;

import org.sqsh.ColumnDescription;
import org.sqsh.Renderer;
import org.sqsh.RendererManager;
import org.sqsh.Session;
import org.sqsh.util.TimeUtils;

public class CountRenderer
        extends Renderer {

    private static long updateInterval = 10_000L;

    private int resultCount;
    private long rowCount;
    private long startTime;
    private long batchStartTime;

    public CountRenderer(Session session, RendererManager manager) {

        super(session, manager);
    }

    public void setUpdateInterval(long updateInterval) {
        CountRenderer.updateInterval = updateInterval;
    }

    public long getUpdateInterval() {
        return CountRenderer.updateInterval;
    }

    @Override
    public void header (ColumnDescription[] columns) {

        super.header(columns);
        if (startTime == 0L) {
            startTime = System.currentTimeMillis();
            batchStartTime = startTime;
        } else {
            batchStartTime = System.currentTimeMillis();
        }

        ++resultCount;
        rowCount = 0L;
    }

    @Override
    public boolean isDiscard() {

        return true;
    }

    @Override
    public boolean row (String[] row) {

        ++rowCount;
        if (updateInterval > 0 && (rowCount % updateInterval) == 0) {
            batchStats();
        }
        return true;
    }

    @Override
    public boolean flush () {
        batchStats();
        return true;
    }

    private void batchStats() {
        final long now = System.currentTimeMillis();
        final long batchMillis = now - batchStartTime;
        final long overallMillis = now - startTime;
        session.out.format("Result set #%d returned %d rows (%s, %.2f rows/sec), %d total (%s, %2f rows/sec)",
                resultCount,
                updateInterval,
                TimeUtils.millisToDurationString(batchMillis),
                (updateInterval / (batchMillis / 1000.0)),
                rowCount,
                TimeUtils.millisToDurationString(overallMillis),
                (rowCount / (overallMillis / 1000.0))
                );
        session.out.println();
        batchStartTime = System.currentTimeMillis();
    }
}
