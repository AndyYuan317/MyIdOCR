# MyIdOCR
Take photo or local picture recognization.
这是个简单的利用Google自带的OCR来做的一个图片识别demo

效果如下图：下图是主界面和拍照剪切界面如下图：

项目详细地址：https://github.com/AndyYuan317/MyIdOCR/tree/master

：

下面的是剪切过图片后进行识别的时候如下：



下面开始介绍：

1.OCR简介

OCR （Optical Character Recognition，光学字符识别）是指电子设备（例如扫描仪或数码相机）检查纸上打印的字符，通过检测暗、亮的模式确定其形状，然后用字符识别方法将形状翻译成计算机文字的过程；

2.Tesseract简介

Tesseract是Ray Smith于1985到1995年间在惠普布里斯托实验室开发的一个OCR引擎，曾经在1995 UNLV精确度测试中名列前茅。但1996年后基本停止了开发。2006年，Google邀请Smith加盟，重启该项目。目前项目的许可证是Apache 2.0。该项目目前支持Windows、Linux和Mac OS等主流平台。但作为一个引擎，它只提供命令行工具。 
现阶段的Tesseract由Google负责维护，是最好的开源OCR Engine之一，并且支持中文。

主页地址：https://github.com/tesseract-ocr

在Tesseract的主页中，我们可以下载到Tesseract的源码及语言包，常用的语言包为

中文：chi-sim.traineddata

英文：eng.traineddata

3.Tess-two

因为Tesseract使用C++实现的，在Android中不能直接使用，需要封装JavaAPI才能在Android平台中进行调用，这里我们直接使用TessTwo项目，tess-two是TesseraToolsForAndroid的一个git分支，使用简单，切集成了leptonica，在使用之前需要先从git上下载源码进行编译。

4.主要项目源码如下：
 

1.在MainActivity中主要是申请权限，提前把字库复制到手机SD卡上供识别的时候使用
  代码如下所示：
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //权限请求
    String[] permissions = new String[]{Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    List<String> mPermissionList = new ArrayList<>();
    //申明一个请求码
    private final int mRequestCode = 100;
    private Button imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (Button) findViewById(R.id.btn_camera);
        imageView.setOnClickListener(this);

        //开启一个线程识别
        new Thread(new Runnable() {
            @Override
            public void run() {
                deepFile("tessdata");
            }
        }).start();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_camera:
                //首先检查权限,再跳转到拍照界面
                checkSelfPermission();
                break;
        }
    }

    /**
     * 将assets中的文件复制出
     * 字库复制到手机SD卡上
     * @param path
     */
    public void deepFile(String path) {
        String newPath = getExternalFilesDir(null) + "/";
        try {
            String str[] = getAssets().list(path);
            if (str.length > 0) {//如果是目录
                File file = new File(newPath + path);
                file.mkdirs();
                for (String string : str) {
                    path = path + "/" + string;
                    deepFile(path);
                    path = path.substring(0, path.lastIndexOf('/'));//回到原来的path
                }
            } else {//如果是文件
                InputStream is = getAssets().open(path);
                FileOutputStream fos = new FileOutputStream(new File(newPath + path));
                byte[] buffer = new byte[1024];
                while (true) {
                    int len = is.read(buffer);
                    if (len == -1) {
                        break;
                    }
                    fos.write(buffer, 0, len);
                    Log.d("写入文件中", "deepFile: *********************" + len);
                }
                Log.d("文件长度", "deepFile: *************fileLength" + path.length());
                is.close();
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 权限开启的回调
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPermissionDismiss = false;//有权限没有通过
        if (mRequestCode == requestCode) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == -1) {
                    hasPermissionDismiss = true;
                    break;
                }
            }
        }
        if (hasPermissionDismiss) {//如果有没有被允许的权限
            Toast.makeText(this, "有权限未通过", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(this, TakePhoteActivity.class);
            startActivity(intent);
        }
    }

    /**
     * 检查权限
     */
    void checkSelfPermission() {
        mPermissionList.clear();//清空已经允许的没有通过的权限
        //逐个判断是否还有未通过的权限
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(this, permissions[i]) !=
                    PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);//添加还未授予的权限到mPermissionList中
            }
        }
        //申请权限
        if (mPermissionList.size() > 0) {//有权限没有通过，需要申请
            ActivityCompat.requestPermissions(this, permissions, mRequestCode);
        } else {
            Intent intent = new Intent(this, TakePhoteActivity.class);
            startActivity(intent);
        }
    }
}
2：是调用相机拍照并剪切图片的主要代码：

/**
 * Created by AndyYuan on time at 2019/5/28.
 */

public class TakePhoteActivity extends AppCompatActivity implements CameraPreview.OnCameraStatusListener, SensorEventListener {


    //true:横屏   false:竖屏
    public static final boolean isTransverse = true;


    private static final String TAG = "TakePhoteActivity";
    public static final Uri IMAGE_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    public static final String PATH = Environment.getExternalStorageDirectory().toString() + "/AndroidMedia/";
    CameraPreview mCameraPreview;
    CropImageView mCropImageView;
    RelativeLayout mTakePhotoLayout;
    LinearLayout mCropperLayout;

    private ImageView btnClose;
    private ImageView btnShutter;
    private Button btnAlbum;

    private ImageView btnStartCropper;
    private ImageView btnCloseCropper;


    /**
     * 旋转文字
     */
    private boolean isRotated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置横屏
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // 设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_take_phote);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        btnClose = (ImageView) findViewById(R.id.btn_close);
        btnClose.setOnClickListener(onClickListener);
        btnShutter = (ImageView) findViewById(R.id.btn_shutter);
        btnShutter.setOnClickListener(onClickListener);
        btnAlbum = (Button) findViewById(R.id.btn_album);
        btnAlbum.setOnClickListener(onClickListener);

        btnStartCropper = (ImageView) findViewById(R.id.btn_startcropper);
        btnStartCropper.setOnClickListener(cropcper);
        btnCloseCropper = (ImageView) findViewById(R.id.btn_closecropper);
        btnCloseCropper.setOnClickListener(cropcper);

        mTakePhotoLayout = (RelativeLayout) findViewById(R.id.take_photo_layout);
        mCameraPreview = (CameraPreview) findViewById(R.id.cameraPreview);
        FocusView focusView = (FocusView) findViewById(R.id.view_focus);

        mCropperLayout = (LinearLayout) findViewById(R.id.cropper_layout);
        mCropImageView = (CropImageView) findViewById(R.id.CropImageView);
        mCropImageView.setGuidelines(2);

        mCameraPreview.setFocusView(focusView);
        mCameraPreview.setOnCameraStatusListener(this);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isTransverse) {
            if (!isRotated) {
                TextView tvHint = (TextView) findViewById(R.id.hint);
                ObjectAnimator animator = ObjectAnimator.ofFloat(tvHint, "rotation", 0f, 90f);
                animator.setStartDelay(800);
                animator.setDuration(500);
                animator.setInterpolator(new LinearInterpolator());
                animator.start();

                ImageView btnShutter = (ImageView) findViewById(R.id.btn_shutter);
                ObjectAnimator animator1 = ObjectAnimator.ofFloat(btnShutter, "rotation", 0f, 90f);
                animator1.setStartDelay(800);
                animator1.setDuration(500);
                animator1.setInterpolator(new LinearInterpolator());
                animator1.start();

                View view = findViewById(R.id.crop_hint);
                AnimatorSet animSet = new AnimatorSet();
                ObjectAnimator animator2 = ObjectAnimator.ofFloat(view, "rotation", 0f, 90f);
                ObjectAnimator moveIn = ObjectAnimator.ofFloat(view, "translationX", 0f, -50f);
                animSet.play(animator2).before(moveIn);
                animSet.setDuration(10);
                animSet.start();

                ObjectAnimator animator3 = ObjectAnimator.ofFloat(btnAlbum, "rotation", 0f, 90f);
                animator3.setStartDelay(800);
                animator3.setDuration(500);
                animator3.setInterpolator(new LinearInterpolator());
                animator3.start();
                isRotated = true;
            }
        } else {
            if (!isRotated) {
                View view = findViewById(R.id.crop_hint);
                AnimatorSet animSet = new AnimatorSet();
                ObjectAnimator animator2 = ObjectAnimator.ofFloat(view, "rotation", 0f, 90f);
                ObjectAnimator moveIn = ObjectAnimator.ofFloat(view, "translationX", 0f, -50f);
                animSet.play(animator2).before(moveIn);
                animSet.setDuration(10);
                animSet.start();
                isRotated = true;
            }
        }
        mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.e(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
    }

    /**
     * 拍照界面
     */
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_close: //关闭相机
                    finish();
                    break;
                case R.id.btn_shutter: //拍照
                    if (mCameraPreview != null) {
                        mCameraPreview.takePicture();
                    }
                    break;
                case R.id.btn_album: //相册
                    Intent intent = new Intent();
                    /* 开启Pictures画面Type设定为image */
                    intent.setType("image/*");
                    /* 使用Intent.ACTION_GET_CONTENT这个Action */
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    /* 取得相片后返回本画面 */
                    startActivityForResult(intent, 1);
                    break;
            }
        }
    };

    /**
     * 截图界面
     */
    private View.OnClickListener cropcper = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_closecropper:
                    showTakePhotoLayout();
                    break;
                case R.id.btn_startcropper:
                    //获取截图并旋转90度
                    Bitmap cropperBitmap = mCropImageView.getCroppedImage();

                    Bitmap bitmap;
                    bitmap = Utils.rotate(cropperBitmap, -90);

                    // 系统时间
                    long dateTaken = System.currentTimeMillis();
                    // 图像名称
                    String filename = DateFormat.format("yyyy-MM-dd kk.mm.ss", dateTaken).toString() + ".jpg";
                    Uri uri = insertImage(getContentResolver(), filename, dateTaken, PATH, filename, bitmap, null);

                    Intent intent = new Intent(TakePhoteActivity.this, ShowCropperedActivity.class);
                    intent.setData(uri);
                    intent.putExtra("path", PATH + filename);
                    intent.putExtra("width", bitmap.getWidth());
                    intent.putExtra("height", bitmap.getHeight());
//                  intent.putExtra("cropperImage", bitmap);
                    startActivity(intent);
                    bitmap.recycle();
                    finish();
                    break;
            }
        }
    };

    /**
     * 拍照成功后回调
     * 存储图片并显示截图界面
     */
    @Override
    public void onCameraStopped(byte[] data) {
        Log.i("TAG", "==onCameraStopped==");
        // 创建图像
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

        if (!isTransverse) {
            bitmap = Utils.rotate(bitmap, 90);
        }
        // 系统时间
        long dateTaken = System.currentTimeMillis();
        // 图像名称
        String filename = DateFormat.format("yyyy-MM-dd kk.mm.ss", dateTaken).toString() + ".jpg";
        // 存储图像（PATH目录）
        Uri source = insertImage(getContentResolver(), filename, dateTaken, PATH, filename, bitmap, data);

        //准备截图
        bitmap = Utils.rotate(bitmap, 90);
        mCropImageView.setImageBitmap(bitmap);
        showCropperLayout();
    }

    /*
     * 获取图片回调
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            Log.e("uri", uri.toString());
            ContentResolver cr = this.getContentResolver();
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
                //与拍照保持一致方便处理
                bitmap = Utils.rotate(bitmap, 90);
                mCropImageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                Log.e("Exception", e.getMessage(), e);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
        showCropperLayout();
    }

    /**
     * 存储图像并将信息添加入媒体数据库
     */
    private Uri insertImage(ContentResolver cr, String name, long dateTaken,
                            String directory, String filename, Bitmap source, byte[] jpegData) {
        OutputStream outputStream = null;
        String filePath = directory + filename;
        try {
            File dir = new File(directory);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(directory, filename);
            if (file.createNewFile()) {
                outputStream = new FileOutputStream(file);
                if (source != null) {
                    source.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                } else {
                    outputStream.write(jpegData);
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
            return null;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return null;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Throwable t) {
                }
            }
        }
        ContentValues values = new ContentValues(7);
        values.put(MediaStore.Images.Media.TITLE, name);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.DATE_TAKEN, dateTaken);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.DATA, filePath);
        return cr.insert(IMAGE_URI, values);
    }

    private void showTakePhotoLayout() {
        mTakePhotoLayout.setVisibility(View.VISIBLE);
        mCropperLayout.setVisibility(View.GONE);
    }

    private void showCropperLayout() {
        mTakePhotoLayout.setVisibility(View.GONE);
        mCropperLayout.setVisibility(View.VISIBLE);
        mCameraPreview.start();   //继续启动摄像头
    }


    private float mLastX = 0;
    private float mLastY = 0;
    private float mLastZ = 0;
    private boolean mInitialized = false;
    private SensorManager mSensorManager;
    private Sensor mAccel;


    /**
     * 位移 自动对焦
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        if (!mInitialized) {
            mLastX = x;
            mLastY = y;
            mLastZ = z;
            mInitialized = true;
        }
        float deltaX = Math.abs(mLastX - x);
        float deltaY = Math.abs(mLastY - y);
        float deltaZ = Math.abs(mLastZ - z);

        if (deltaX > 0.8 || deltaY > 0.8 || deltaZ > 0.8) {
            mCameraPreview.setFocus();
        }
        mLastX = x;
        mLastY = y;
        mLastZ = z;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}

3：获取剪切的图片并开始利用字库进行识别：

/**
 * Created by AndyYuan on time at 2019/5/28.
 */

public class ShowCropperedActivity extends AppCompatActivity {

    //sd卡路径
    private static String LANGUAGE_PATH = "";
    //识别语言
    private static final String LANGUAGE = "chi_sim";//chi_sim | eng

    private static final String TAG = "ShowCropperedActivity";
    private ImageView imageView;
    private ImageView imageView2;
    private ImageView imageView3;
    private TextView textView;

    private Uri uri;
    private String result;
    private TessBaseAPI baseApi = new TessBaseAPI();
    private Handler handler = new Handler();
    private ProgressDialog dialog;

    int endWidth, endHeight;
    private ColorMatrix colorMatrix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_croppered);

        LANGUAGE_PATH = getExternalFilesDir("") + "/";
        Log.e("---------", LANGUAGE_PATH);

        Thread myThread = new Thread(runnable);
        dialog = new ProgressDialog(this);
        dialog.setMessage("正在识别...");
        dialog.setCancelable(false);
        dialog.show();

        imageView = (ImageView) findViewById(R.id.image);
        imageView2 = (ImageView) findViewById(R.id.image2);
        imageView3 = (ImageView) findViewById(R.id.image3);
        textView = (TextView) findViewById(R.id.text);

        int width = getIntent().getIntExtra("width", 0);
        int height = getIntent().getIntExtra("height", 0);
        if (width != 0 && height != 0) {
            int screenWidth = Utils.getWidthInPx(this);
            float scale = (float) screenWidth / (float) width;
            final ViewGroup.LayoutParams lp = imageView.getLayoutParams();
            int imgHeight = (int) (scale * height);
            endWidth = screenWidth;
            endHeight = imgHeight;
            lp.height = imgHeight;
            imageView.setLayoutParams(lp);
            Log.e(TAG, "imageView.getLayoutParams().width:" + imageView.getLayoutParams().width);
        }

        uri = getIntent().getData();
        imageView.setImageURI(uri);

        baseApi.init(LANGUAGE_PATH, LANGUAGE);
        Log.d("*************", "onCreate: ***********路径为：" + LANGUAGE_PATH);
//        baseApi.init(getSDPath(),LANGUAGE);
        //设置设别模式
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);

        myThread.start();
    }


    /**
     * uri转bitmap
     */
    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            // 读取uri所在的图片
            return MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        } catch (Exception e) {
            Log.e("[Android]", e.getMessage());
            Log.e("[Android]", "目录为：" + uri);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 灰度化处理
     */
    public Bitmap convertGray(Bitmap bitmap3) {
        colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);

        Paint paint = new Paint();
        paint.setColorFilter(filter);
        Bitmap result = Bitmap.createBitmap(bitmap3.getWidth(), bitmap3.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        canvas.drawBitmap(bitmap3, 0, 0, paint);
        return result;
    }

    /**
     * 二值化
     *
     * @param tmp 二值化阈值 默认100
     */
    private Bitmap binaryzation(Bitmap bitmap22, int tmp) {
        // 获取图片的宽和高
        int width = bitmap22.getWidth();
        int height = bitmap22.getHeight();
        // 创建二值化图像
        Bitmap bitmap;
        bitmap = bitmap22.copy(Bitmap.Config.ARGB_8888, true);
        // 遍历原始图像像素,并进行二值化处理
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                // 得到当前的像素值
                int pixel = bitmap.getPixel(i, j);
                // 得到Alpha通道的值
                int alpha = pixel & 0xFF000000;
                // 得到Red的值
                int red = (pixel & 0x00FF0000) >> 16;
                // 得到Green的值
                int green = (pixel & 0x0000FF00) >> 8;
                // 得到Blue的值
                int blue = pixel & 0x000000FF;

                if (red > tmp) {
                    red = 255;
                } else {
                    red = 0;
                }
                if (blue > tmp) {
                    blue = 255;
                } else {
                    blue = 0;
                }
                if (green > tmp) {
                    green = 255;
                } else {
                    green = 0;
                }

                // 通过加权平均算法,计算出最佳像素值
                int gray = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
                // 对图像设置黑白图
                if (gray <= 95) {
                    gray = 0;
                } else {
                    gray = 255;
                }
                // 得到新的像素值
                int newPiexl = alpha | (gray << 16) | (gray << 8) | gray;
                // 赋予新图像的像素
                bitmap.setPixel(i, j, newPiexl);
            }
        }
        return bitmap;
    }

    /**
     * 识别线程
     */
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            final Bitmap bitmap_1 = convertGray(getBitmapFromUri(uri));

            baseApi.setImage(bitmap_1);
            result = baseApi.getUTF8Text();
            baseApi.end();

            handler.post(new Runnable() {
                @Override
                public void run() {
                    imageView2.setImageBitmap(bitmap_1);
                    textView.setText(result);
                    dialog.dismiss();
                }
            });
        }
    };
}

--------------------- 
作者：AndyYuan317 
来源：CSDN 
原文：https://blog.csdn.net/qq_42618969/article/details/90639178 
版权声明：本文为博主原创文章，转载请附上博文链接！
