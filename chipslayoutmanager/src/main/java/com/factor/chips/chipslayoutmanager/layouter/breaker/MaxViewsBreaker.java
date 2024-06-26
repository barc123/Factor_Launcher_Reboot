package com.factor.chips.chipslayoutmanager.layouter.breaker;


import com.factor.chips.chipslayoutmanager.layouter.AbstractLayouter;

/** brakes the row in case max views size in row reached */
public class MaxViewsBreaker extends RowBreakerDecorator {

    private int maxViewsInRow;

    MaxViewsBreaker(int maxViewsInRow, ILayoutRowBreaker decorate) {
        super(decorate);
        this.maxViewsInRow = maxViewsInRow;
    }

    @Override
    public boolean isRowBroke(AbstractLayouter al) {
        return super.isRowBroke(al)
                || al.getRowSize() >= maxViewsInRow;
    }
}
