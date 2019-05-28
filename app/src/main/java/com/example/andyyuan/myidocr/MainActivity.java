package com.example.andyyuan.myidocr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;




import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
