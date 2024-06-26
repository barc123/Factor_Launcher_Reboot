package com.factor.chips.chipslayoutmanager.layouter;


import androidx.annotation.CallSuper;
import androidx.recyclerview.widget.RecyclerView;

public class MeasureSupporter extends RecyclerView.AdapterDataObserver implements IMeasureSupporter {

    private final RecyclerView.LayoutManager lm;
    private boolean isAfterRemoving;

    private int measuredWidth;
    private int measuredHeight;

    private boolean isRegistered;

    /**
     * width of RecyclerView before removing item
     */
    private Integer beforeRemovingWidth = null;

    /**
     * width which we receive after {@link RecyclerView.LayoutManager#onLayoutChildren} method finished.
     * Contains correct width after auto-measuring
     */
    private int autoMeasureWidth = 0;

    /**
     * height of RecyclerView before removing item
     */
    private Integer beforeRemovingHeight = null;

    /**
     * height which we receive after {@link RecyclerView.LayoutManager#onLayoutChildren} method finished.
     * Contains correct height after auto-measuring
     */
    private int autoMeasureHeight = 0;

    public MeasureSupporter(RecyclerView.LayoutManager lm) {
        this.lm = lm;
    }

    @Override
    public void onSizeChanged() {
        autoMeasureWidth = lm.getWidth();
        autoMeasureHeight = lm.getHeight();
    }

    boolean isAfterRemoving() {
        return isAfterRemoving;
    }

    @Override
    public int getMeasuredWidth() {
        return measuredWidth;
    }

    private void setMeasuredWidth(int measuredWidth) {
        this.measuredWidth = measuredWidth;
    }

    @Override
    public int getMeasuredHeight() {
        return measuredHeight;
    }

    @Override
    public boolean isRegistered() {
        return isRegistered;
    }

    @Override
    public void setRegistered(boolean isRegistered) {
        this.isRegistered = isRegistered;
    }

    private void setMeasuredHeight(int measuredHeight) {
        this.measuredHeight = measuredHeight;
    }

    @Override
    @CallSuper
    public void measure(int autoWidth, int autoHeight) {
        if (isAfterRemoving()) {
            setMeasuredWidth(Math.max(autoWidth, beforeRemovingWidth));
            setMeasuredHeight(Math.max(autoHeight, beforeRemovingHeight));
        } else {
            setMeasuredWidth(autoWidth);
            setMeasuredHeight(autoHeight);
        }
    }

    @Override
    public void onItemsRemoved(final RecyclerView recyclerView){
        //subscribe to next animations tick
        lm.postOnAnimation(new Runnable() {

            private void onFinished() {
                //when removing animation finished return auto-measuring back
                isAfterRemoving = false;
                // and process measure again
                lm.requestLayout();
            }

            @Override
            public void run() {
                if (recyclerView.getItemAnimator() != null) {
                    //listen removing animation
                    recyclerView.getItemAnimator().isRunning(this::onFinished);
                } else {
                    onFinished();
                }
            }
        });
    }

    @Override
    @CallSuper
    public void onItemRangeRemoved(int positionStart, int itemCount) {
        super.onItemRangeRemoved(positionStart, itemCount);
        isAfterRemoving = true;

        beforeRemovingWidth = autoMeasureWidth;
        beforeRemovingHeight = autoMeasureHeight;
    }
}
