package it.unibo.studio.unigo.main.adapteritems;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

// Classe RelativeLayout container di ciascun elemento della GridView delle immagini (nell'activity NewRequest)
// Questa classe è necessaria per forzare gli elementi della GridView ad assumere una forma quadrata
public class GridViewItem extends RelativeLayout
{
    public GridViewItem(Context context) {
        super(context);
    }

    public GridViewItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GridViewItem(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
