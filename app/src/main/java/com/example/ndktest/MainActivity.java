package com.example.ndktest;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private Bitmap mBitmap;
    private ImageView mImageView;
    private static final int CHANGE_BLUE = 0;
    private static final int CHANGE_RED = 1;
    private static final int CHANGE_WHITE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button changeSize = findViewById(R.id.changeSize);
        changeSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeSize();
            }
        });
        Button changeBlue = findViewById(R.id.changeBlue);
        changeBlue.setOnClickListener(this);
        Button changeRed = findViewById(R.id.changeRed);
        changeRed.setOnClickListener(this);
        Button changeWhite = findViewById(R.id.changeWhite);
        changeWhite.setOnClickListener(this);
        Button save = findViewById(R.id.save);
        Button choice = findViewById(R.id.choicePic);
        choice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAbulm();
            }
        });
        initLoaderOpenCV();
        mImageView = findViewById(R.id.show);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveBitmap(mBitmap);
            }
        });
    }

    private void changeSize() {
        int w = mBitmap.getWidth();
        int h = mBitmap.getHeight();
        int[] pixel = new int[w * h];
        EditText editText1 = findViewById(R.id.nW);
        int nW = Integer.parseInt(editText1.getText().toString());
        EditText editText2 = findViewById(R.id.nH);
        int nH = Integer.parseInt(editText2.getText().toString());
        mBitmap.getPixels(pixel, 0, w, 0, 0, w, h);
        int[] result = NDKUtils.grayProc(pixel, w, h, nW, nH);
        mBitmap = Bitmap.createBitmap(nW, nH, Bitmap.Config.RGB_565);
        mBitmap.setPixels(result, 0, nW, 0, 0, nW, nH);
        mImageView.setImageBitmap(mBitmap);
    }

    @Override
    public void onClick(View view) {
        int type = -1;
        switch (view.getId()) {
            case R.id.changeBlue:
                type = CHANGE_BLUE;
                break;
            case R.id.changeRed:
                type = CHANGE_RED;
                break;
            case R.id.changeWhite:
                type = CHANGE_WHITE;
                break;
            default:
                break;
        }
        startDetail(type);
    }

    public static void saveBitmap(Bitmap bm) {
        Log.e(TAG, "保存图片");
        File sdDir = Environment.getExternalStorageDirectory();
        String tmpFile = sdDir.toString() + "/DCIM/" + "occlusionCap.png";
        File f = new File(tmpFile);
        try {
            FileOutputStream out = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            Log.i(TAG, "已经保存");
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void startDetail(int type) {
        Mat image = new Mat();
        mBitmap = ((BitmapDrawable)getResources().getDrawable(R.drawable.stx1)).getBitmap();
        Utils.bitmapToMat(mBitmap, image);

        Mat hsvImg = new Mat();
        Imgproc.cvtColor(image, hsvImg, Imgproc.COLOR_BGR2HSV);


        List<Mat> list = new ArrayList<>();
        Core.split(hsvImg, list);

        Mat roiH = list.get(0).submat(new Rect(0, 0, 20, 20));
        Mat roiS = list.get(1).submat(new Rect(0, 0, 20, 20));

        Log.i(TAG, "start sum bg");
        int SumH = 0;
        int SumS = 0;
        byte[] h = new byte[1];
        byte[] s = new byte[1];
        //取一块蓝色背景，计算出它的平均色调和平均饱和度
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                roiH.get(j, i, h);
                roiS.get(j, i, s);

                SumH = h[0] + SumH;
                SumS = s[0] + SumS;
            }
        }

        int avgH, avgS;//蓝底的平均色调和平均饱和度
        avgH = SumH / 400;
        avgS = SumS / 400;


        Log.i(TAG, "depth=" + list.get(0).depth());
        Log.i(TAG, "start sum detail all photo");
        //遍历整个图像
        int nl = hsvImg.height();
        int nc = hsvImg.width();


//        byte[] changeColor = new byte[]{127};

        byte[] hArray = new byte[nl * nc];
        byte[] sArray = new byte[nl * nc];
        byte[] vArray = new byte[nl * nc];

        list.get(0).get(0, 0, hArray);
        list.get(1).get(0, 0, sArray);
        list.get(2).get(0, 0, vArray);

        int row, index;
        for (int j = 0; j < nl; j++) {
            row = j * nc;
            for (int i = 0; i < nc; i++) {
                index = row + i;

                if (hArray[index] <= (avgH + 20) && hArray[index] >= (avgH - 20)
                        && sArray[index] <= (avgS + 150)
                        && sArray[index] >= (avgS - 150)
                ) {
                    if (type == CHANGE_RED) {
                        hArray[index] = 127;
                    }
                    if (type == CHANGE_WHITE) {
                        hArray[index] = 0;
                        sArray[index] = 0;
                        vArray[index] = (byte) 255;

                    }
                    if (type == CHANGE_BLUE) {
                        hArray[index] = 24;
                    }
                }
            }
        }

        list.get(0).put(0, 0, hArray);
        list.get(1).put(0, 0, sArray);
//        list.get(2).put(0,0,vArray);


        Log.i(TAG, "merge photo");
        Core.merge(list, hsvImg);

        Imgproc.cvtColor(hsvImg, image, Imgproc.COLOR_HSV2BGR);

        Bitmap resultBitmap = getResultBitmap();
        Utils.matToBitmap(image, resultBitmap);
        mBitmap = resultBitmap;
        mImageView.setImageBitmap(resultBitmap);
    }

    private Bitmap getResultBitmap() {
        return Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.RGB_565);
    }

    private void initLoaderOpenCV() {
        boolean success = OpenCVLoader.initDebug();
        if (!success) {
            Log.d(TAG, "初始化失败");
        }
    }

    public void openAbulm() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    Uri imageUri = 
                    mBitmap = getBitmap(path);
                    mImageView.setImageBitmap(mBitmap);
                }
                break;

            default:
                break;
        }
    }

    public static Bitmap getBitmap(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        File file = new File(filePath);
        InputStream inputStream = null;
        try {
             inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inMutable = true;
        return BitmapFactory.decodeStream(inputStream,new android.graphics.Rect(),options);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // 文件提供
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // 外部存储设备提供
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/"
                            + split[1];
                }
                // TODO handle non-primary volumes
            }
            // 下载文件提供
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // 媒体提供
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = MediaStore.MediaColumns._ID + "=?";
                final String[] selectionArgs = new String[] { split[1] };
                return getDataColumn(context, contentUri, selection,
                        selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri . This is useful for
     * MediaStore Uris , and other file - based ContentProviders.
     *
     * @param context
     *            The context.
     * @param uri
     *            The Uri to query.
     * @param selection
     *            (Optional) Filter used in the query.
     * @param selectionArgs
     *            (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri,
                                       String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = MediaStore.MediaColumns.DATA;
        final String[] projection = { column };
        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri
                .getAuthority());
    }
}