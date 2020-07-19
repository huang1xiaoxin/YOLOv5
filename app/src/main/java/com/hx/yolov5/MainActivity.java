package com.hx.yolov5;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final String TAG = "MainActivity";
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private ImageView resultImageView;
    private SeekBar nmsSeekBar;
    private SeekBar thresholdSeekBar;
    private TextView thresholdTextview;
    private double threshold = 0.3, nms_threshold = 0.7;
    private ImageView detect;
    private CircleImageView pictureImageView;
    private CircleImageView cameraImageView;
    private Bitmap imageBitmap;
    private LinearLayout mLoadingLinearLayout;
    private static final int CAMERA_RESULT = 0;
    private Uri imageUri;
    private File mediaFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        requestPermission();
        YOLOv5.init(getAssets());
        resultImageView = findViewById(R.id.imageView);
        thresholdTextview = findViewById(R.id.text_desc);
        cameraImageView = findViewById(R.id.camera);
        nmsSeekBar = findViewById(R.id.nms_seek);
        mLoadingLinearLayout = findViewById(R.id.loading_layout);
        thresholdSeekBar = findViewById(R.id.threshold_seek);
        pictureImageView = findViewById(R.id.picture);
        thresholdTextview.setText(String.format(Locale.ENGLISH, "Thresh:%.2f,NMS:%.2f", threshold, nms_threshold));
        nmsSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                nms_threshold = i / 100.f;
                thresholdTextview.setText(String.format(Locale.ENGLISH, "Thresh:%.2f,NMS:%.2f", threshold, nms_threshold));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        thresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                threshold = i / 100.f;
                thresholdTextview.setText(String.format(Locale.ENGLISH, "Thresh:%.2f,NMS:%.2f", threshold, nms_threshold));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        detect = findViewById(R.id.detect);
        detect.setOnClickListener(this);
        cameraImageView.setOnClickListener(this);
        pictureImageView.setOnClickListener(this);
        initLoadingView();
    }

    private void requestPermission() {
        int permission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            //动态申请权限
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
        //拍照的权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_EXTERNAL_STORAGE);

        }
    }

    /**
     * 检查申请权限的结果
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                this.finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            //相册选择图片返回的结果
            case REQUEST_PICK_IMAGE:
                imageBitmap = getBitmapFromUri(getRealUri(data.getData()), 400, 400);
                break;
            case CAMERA_RESULT:
                //拍照返回的结果
                imageBitmap = getBitmapFromUri(mediaFile.getAbsolutePath(), 400, 400);
                break;
            default:
                break;
        }
        if (imageBitmap != null) {
            resultImageView.setImageBitmap(imageBitmap);
        }

    }

    /**
     * 对图片进行识别
     *
     * @param image 原始图片的bitmap
     * @return //返回处理好的图片
     */

    private Bitmap detectBitmap(Bitmap image) {
        Box[] result = YOLOv5.detect(image, threshold, nms_threshold);
        Bitmap mutableBitmap = image.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        final Paint boxPaint = new Paint();
        boxPaint.setAlpha(200);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4 * image.getWidth() / 800);
        boxPaint.setTextSize(40 * image.getWidth() / 800);
        for (Box box : result) {
            boxPaint.setColor(box.getColor());
            boxPaint.setStyle(Paint.Style.FILL);
            canvas.drawText(box.getLabel(), box.x0, box.y0, boxPaint);
            boxPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(box.getRect(), boxPaint);
        }
        return mutableBitmap;
    }

    /**
     * 获取相册中返回的真正的路径
     *
     * @param selectedImage
     * @return
     */
    public String getRealUri(Uri selectedImage) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = this.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        assert cursor != null;
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();

        return picturePath;
    }

    /**
     * 读取图片
     *
     * @param path
     * @return
     */
    @TargetApi(Build.VERSION_CODES.ECLAIR)
    public int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }


    /**
     * 点击事件
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            //选择图片
            case R.id.picture:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_PICK_IMAGE);
                break;
            case R.id.camera://打开相机
                IntentCamera();
                break;
            case R.id.detect://识别
                if (imageBitmap == null) {
                    return;
                }
                showLoading();
                detectPicture();
                break;
            default:
                break;
        }
    }

    /**
     * 点击识别按钮时调用的方法
     */
    @SuppressLint("CheckResult")
    private void detectPicture() {
        if (imageBitmap == null) {
            Toast.makeText(this, "请选择图片！！", Toast.LENGTH_SHORT).show();
            return;
        }
        Observable.create(new ObservableOnSubscribe<Bitmap>() {
            @Override
            public void subscribe(ObservableEmitter<Bitmap> emitter) throws Exception {
                emitter.onNext(detectBitmap(imageBitmap));
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe((bitmap) -> {
            resultImageView.setImageBitmap(bitmap);
            hideLoading();
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Toast.makeText(getApplicationContext(), "请选择图片！！", Toast.LENGTH_SHORT).show();
                hideLoading();
            }
        });


    }

    //初始化加载框
    private void initLoadingView() {
        //加载按钮的属性
        LinearLayout.LayoutParams proLayoutParams = new LinearLayout.LayoutParams(
                dip2dx(60), dip2dx(60)
        );
        ProgressBar progressBar = new ProgressBar(getApplicationContext());
        Drawable drawable = getApplicationContext().getDrawable(R.drawable.progressbar);
        progressBar.setIndeterminateDrawable(drawable);
        //加载TextView的属性
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        TextView textView = new TextView(getApplicationContext());
        textView.setGravity(Gravity.CENTER);
        textView.setText("识别中...");
        textView.setLayoutParams(textParams);
        mLoadingLinearLayout.addView(progressBar, proLayoutParams);
        mLoadingLinearLayout.addView(textView, textParams);

    }

    //显示初始框
    private void showLoading() {
        if (mLoadingLinearLayout.getVisibility() != View.VISIBLE) {
            mLoadingLinearLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 隐藏加载框
     */
    private void hideLoading() {
        if (mLoadingLinearLayout.getVisibility() == View.VISIBLE) {
            mLoadingLinearLayout.setVisibility(View.GONE);
        }
    }

    /**
     * dip转化为dp
     *
     * @param dp
     * @return
     */
    private int dip2dx(int dp) {
        float scan = getApplicationContext().getResources().getDisplayMetrics().density;
        return (int) (scan * dp + 0.5f);
    }

    /**
     * 获取uri
     */
    public Uri getMediaFileUri(Context context) {
        String cameraPath = getFilesDir() + File.separator + "images" + File.separator;
        mediaFile = new File(cameraPath, "picture" + System.currentTimeMillis() + ".jpg");
        if (!mediaFile.exists()) {
            mediaFile.getParentFile().mkdirs();
        }
        //sdk>=24 android7以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            imageUri = FileProvider.getUriForFile(context, "com.hx.yolov5.provider", mediaFile);

        } else {
            imageUri = Uri.fromFile(mediaFile);
        }
        return imageUri;
    }

    /**
     * 打开相机
     */
    private void IntentCamera() {
        Intent openCameraIntent = new Intent("android.media.action.IMAGE_CAPTURE");
        imageUri = getMediaFileUri(MainActivity.this);
        openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        //将存储图片的uri读写权限授权给相机应用
        //Android7.0添加临时权限标记，此步千万别忘了
        openCameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        openCameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(openCameraIntent, CAMERA_RESULT);
    }


    /**
     * 通过采样率来加载图片
     */
    public Bitmap getBitmapFromUri(String uri, int width, int height) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inSampleSize = calculateInSampleSize(options, width, height);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(uri, options);
        return bitmap;
    }

    /**
     * 计算合适的采样率
     *
     * @param options
     * @param width
     * @param height
     * @return
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int width, int height) {
        final int originHeight = options.outHeight;
        final int originWidth = options.outWidth;
        int inSampleSize = 1;
        if (originHeight > height || originWidth > width) {
            final int halfHeight = originHeight / 2;
            final int halfWidth = originWidth / 2;
            while ((halfHeight / inSampleSize) >= height && (halfWidth / inSampleSize) >= width) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }


    /**
     * 对图片进行质量的压缩
     * 改方法删除，因为压缩后的图片过于模糊
     *
     * @param bitmap
     * @return
     */
    @Deprecated
    private Bitmap compressImage(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        //质量压缩方法，这里的100表示不压缩，把压缩后数据存放到outputStream
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        int options = 100;
        //是否大于100k
        while (outputStream.toByteArray().length / 1024 > 100) {
            //重置outputStream
            outputStream.reset();
            //第一个参数，图片格式,第二个参数：图片的质量，100为最高，0为最差  ，第三个参数：保存压缩后的数据的流
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, outputStream);
            options -= 10;
            if (options <= 0) {
                break;
            }
        }
        ////把压缩后的数据存放到ByteArrayInputStream中
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        //生成图片
        Bitmap imageBitmap = BitmapFactory.decodeStream(inputStream, null, null);
        return imageBitmap;
    }

}
