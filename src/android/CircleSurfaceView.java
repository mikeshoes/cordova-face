package cordova.plugin.face.recognize;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;

public class CircleSurfaceView extends SurfaceView {
    public CircleSurfaceView(Context context) {
        super(context);
    }

    public CircleSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircleSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CircleSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void draw(Canvas canvas) {
        Log.i("onDraw", "draw: test");
        Path path = new Path();
        //设置裁剪的圆心，半径
        ViewGroup.LayoutParams params = this.getLayoutParams();
        Log.e("height:", String.valueOf(params.height));
        path.addCircle( params.height / 2, params.height / 2, params.height / 2, Path.Direction.CCW);
        //裁剪画布，并设置其填充方式
        if (Build.VERSION.SDK_INT >= 26 ) {
            canvas.clipPath(path);
        } else {
            canvas.clipPath(path, Region.Op.REPLACE);
        }
        super.draw(canvas);
    }
}
