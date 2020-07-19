package com.hx.yolov5;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

@SuppressLint("AppCompatCustomView")
public class CircleImageView extends ImageView {
    private static final int RECT = 1;
    private static final int CIRCLE = 2;
    private Paint mPaint;
    private Bitmap mRawBitmap;
    private BitmapShader mShader;
    private Matrix matrix;
    private Paint mBorderPaint;

    private RectF mRectBorder;
    private RectF mRectBitmap;

    //边框的宽度
    private int mBorderWidth;

    //边框的颜色
    private int mBorderColor;

    //是否设置边框，默认不设置 false
    private boolean createBorder;

    /**
     * 0 ClAMP ： Bitmap以其内容的最后一行像素填充剩余的高的空白或者最后一列像素填充剩余宽空白
     * 1 MIRROR ：Bitmap以其内容以镜像的方式填充剩余空白
     * 2 REPEAT ：Bitmap以其内容以重复的方式填充剩余空白
     */
    private int mTileY = 0;

    private int mTileX = 0;

    //边角得半径
    private int mRoundRadius;


    /**
     * circle 圆形 默认
     * rect 带圆角得形状
     */
    private int mShapeType;


    public CircleImageView(Context context) {
        this(context, null);
    }

    public CircleImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CircleImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
        //获取自定以的属性
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView);
        mTileX = array.getInt(R.styleable.CircleImageView_mTileX, 0);
        mTileY = array.getInt(R.styleable.CircleImageView_mTileY, 0);
        mBorderColor = array.getColor(R.styleable.CircleImageView_mBorderColor, 0xFF0080FF);
        mBorderWidth = array.getDimensionPixelOffset(R.styleable.CircleImageView_mBorderWidth, 4);
        createBorder = array.getBoolean(R.styleable.CircleImageView_createBorder, false);
        mShapeType = array.getInt(R.styleable.CircleImageView_mShapeType, CIRCLE);
        mRoundRadius = array.getDimensionPixelOffset(R.styleable.CircleImageView_mRoundRadius, 10);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //支持wrapContent
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthSpecMode == MeasureSpec.AT_MOST && heightMeasureSpec == MeasureSpec.AT_MOST) {
            setMeasuredDimension(100, 100);
        } else if (widthSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(100, heightSpecSize);
        } else if (heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSpecSize, 100);
        }
    }

    /**
     * 在onDraw中不要有过多复杂的逻辑，和过于复杂多余的计算，否则会导致绘制不全的现象
     *
     *
     * @param canvas
     */

    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap bitmap = getBitmap(getDrawable());
        if (bitmap != null) {
            //支持Padding的属性
            final int paddingLeft = getPaddingLeft();
            final int paddingRight = getPaddingRight();
            final int paddingTop = getPaddingTop();
            final int paddingBottom = getPaddingBottom();
            float width = getWidth() - paddingLeft - paddingRight;
            float height = getHeight() - paddingTop - paddingBottom;
            float diameter = Math.min(width, height);
            float dstWidth = mShapeType == RECT ? width : diameter;
            float dstHeight = mShapeType == RECT ? height : diameter;
            float doubleBorderWidth = mBorderWidth * 2.0f;
            float halfBorderWidth = mBorderWidth / 2.0f;
            //判断是否已经创建，或者复用
            if (mShader == null || !bitmap.equals(mRawBitmap)) {
                mRawBitmap = bitmap;
                mShader = createBitmapShader(mRawBitmap, mTileX, mTileY);
            }
            if (mShader != null) {
                //设置缩放比例
                matrix.setScale((dstWidth - doubleBorderWidth) / mRawBitmap.getWidth(), (dstHeight - doubleBorderWidth) / mRawBitmap.getHeight());
                mShader.setLocalMatrix(matrix);
            }
            mPaint.setShader(mShader);
            if (createBorder) {
                mBorderPaint.setStyle(Paint.Style.STROKE);
                mBorderPaint.setStrokeWidth(mBorderWidth);
                //如果是不设置边框得 使边框得画笔变为透明
                mBorderPaint.setColor(createBorder ? mBorderColor : Color.TRANSPARENT);
            }
            if (mShapeType == RECT) {
                createRoundRect(canvas, width, height, doubleBorderWidth, halfBorderWidth);
            } else {
                createCircle(canvas, diameter / 2.0f, halfBorderWidth);
            }
        } else {
            super.onDraw(canvas);
        }
    }

    private void createRoundRect(Canvas canvas, float width, float height, float doubleBorderWidth, float halfBorderWidth) {
        mRectBorder.set(halfBorderWidth, halfBorderWidth, width - halfBorderWidth, height - halfBorderWidth);
        mRectBitmap.set(0.0f, 0.0f, width - doubleBorderWidth, height - doubleBorderWidth);
        float bitmapRadius = Math.max((mRoundRadius - mBorderWidth), 0.0f);
        if (createBorder) {
            float rectRadius = Math.max(mRoundRadius - halfBorderWidth, 0.0f);
            //画边边框
            canvas.drawRoundRect(mRectBorder, rectRadius, rectRadius, mBorderPaint);
            //画布平移
            canvas.translate(mBorderWidth, mBorderWidth);
        }
        //画图像得
        canvas.drawRoundRect(mRectBitmap, bitmapRadius, bitmapRadius, mPaint);
    }


    private void createCircle(Canvas canvas, float radius, float halfBorderWidth) {
        float realRadius = radius - mBorderWidth;
        if (createBorder) {
            canvas.drawCircle(radius, radius, radius - halfBorderWidth, mBorderPaint);
            canvas.translate(mBorderWidth, mBorderWidth);
        }
        canvas.drawCircle(realRadius, realRadius, realRadius, mPaint);
    }


    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setAntiAlias(true);//设置抗锯齿
        //该方法千万别放到onDraw()方法里面调用，否则会不停的重绘的，因为该方法调用了invalidate() 方法
        //View Layer 绘制所消耗的实际时间是比不使用 View Layer 时要高的，所以要慎重使用。所以我们将View Layer关闭
        //否则会出现黑色背景的现象
        setLayerType(View.LAYER_TYPE_NONE, null);
        matrix = new Matrix();
        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setAntiAlias(true);
        mRectBitmap = new RectF();
        mRectBorder = new RectF();
    }

    /**
     * 根据不同的类型获取Bitmap
     *
     * @param drawable
     * @return
     */
    public Bitmap getBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof ColorDrawable) {
            //颜色类型
            Rect rect = drawable.getBounds();
            int width = rect.right - rect.left;
            int height = rect.bottom - rect.top;
            int color = ((ColorDrawable) drawable).getColor();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawARGB(Color.alpha(color), Color.red(color), Color.green(color), Color.blue(color));
            return bitmap;
        } else {
            return null;
        }
    }

    /**
     * 根据不同的mTileX，mTileY创建BitmapShader
     *
     * @param mTileX
     * @param mTileY
     * @return
     */
    public BitmapShader createBitmapShader(Bitmap bitmap, int mTileX, int mTileY) {
        BitmapShader.TileMode tileModeX;
        BitmapShader.TileMode tileModeY;
        switch (mTileX) {
            case 1:
                tileModeX = BitmapShader.TileMode.MIRROR;
                break;
            case 2:
                tileModeX = BitmapShader.TileMode.REPEAT;
                break;
            default:
                tileModeX = BitmapShader.TileMode.CLAMP;
        }

        switch (mTileY) {
            case 1:
                tileModeY = BitmapShader.TileMode.MIRROR;
                break;
            case 2:
                tileModeY = BitmapShader.TileMode.REPEAT;
                break;
            default:
                tileModeY = BitmapShader.TileMode.CLAMP;
                break;
        }
        return new BitmapShader(bitmap, tileModeX, tileModeY);

    }

    public int getTileX() {
        return mTileX;
    }

    public void setTileX(int mTileX) {
        this.mTileX = mTileX;
    }

    public int getTileY() {
        return mTileY;
    }

    public void setTileY(int mTileY) {
        this.mTileY = mTileY;
    }

    public Paint getmPaint() {
        return mPaint;
    }

    public void setmPaint(Paint mPaint) {
        this.mPaint = mPaint;
    }

    public Bitmap getmRawBitmap() {
        return mRawBitmap;
    }

    public void setmRawBitmap(Bitmap mRawBitmap) {
        this.mRawBitmap = mRawBitmap;
    }

    public BitmapShader getmShader() {
        return mShader;
    }

    public void setmShader(BitmapShader mShader) {
        this.mShader = mShader;
    }

    @Override
    public Matrix getMatrix() {
        return matrix;
    }

    public void setMatrix(Matrix matrix) {
        this.matrix = matrix;
    }

    public Paint getmBorderPaint() {
        return mBorderPaint;
    }

    public void setmBorderPaint(Paint mBorderPaint) {
        this.mBorderPaint = mBorderPaint;
    }

    public RectF getmRectBorder() {
        return mRectBorder;
    }

    public void setmRectBorder(RectF mRectBorder) {
        this.mRectBorder = mRectBorder;
    }

    public RectF getmRectBitmap() {
        return mRectBitmap;
    }

    public void setmRectBitmap(RectF mRectBitmap) {
        this.mRectBitmap = mRectBitmap;
    }

    public int getmBorderWidth() {
        return mBorderWidth;
    }

    public void setmBorderWidth(int mBorderWidth) {
        this.mBorderWidth = mBorderWidth;
    }

    public int getmBorderColor() {
        return mBorderColor;
    }

    public void setmBorderColor(int mBorderColor) {
        this.mBorderColor = mBorderColor;
    }

    public boolean isCreateBorder() {
        return createBorder;
    }

    public void setCreateBorder(boolean createBorder) {
        this.createBorder = createBorder;
    }

    public int getmRoundRadius() {
        return mRoundRadius;
    }

    public void setmRoundRadius(int mRoundRadius) {
        this.mRoundRadius = mRoundRadius;
    }

    public int getmShapeType() {
        return mShapeType;
    }

    public void setmShapeType(int mShapeType) {
        this.mShapeType = mShapeType;
    }
}
