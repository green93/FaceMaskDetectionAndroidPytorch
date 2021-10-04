package com.example.facemaskdetectionandroid;

import androidx.appcompat.app.AppCompatActivity;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.soloader.nativeloader.NativeLoader;
import com.facebook.soloader.nativeloader.SystemDelegate;
import com.google.android.material.button.MaterialButton;


import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.PyTorchAndroid;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private Module mModule = null;
    private ResultView resultView;
    private ImageView imageView;
    private TextView textView;
    private MaterialButton materialButton;
    private Bitmap imageBitmap;
    private String path="test.jpg";
    private static final String TAG = "MainActivity";
    private Tensor outputTensor;

    // This One is Must for including binaries of vision
    static {
        if (!NativeLoader.isInitialized()) {
            NativeLoader.init(new SystemDelegate());
        }
        NativeLoader.loadLibrary("pytorch_jni");
        NativeLoader.loadLibrary("torchvision_ops");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Inflate Layout
        infalteLayout();
        // get Image in Bitmap and Set to ImageView
        imageBitmap = loadImage("1.jpeg");
        imageView.setImageBitmap(imageBitmap);

        try {
            mModule = getObjectDetectionModelFromAsset("model.torchscript.pt");
            Log.d(TAG, "onCreate Model: "+mModule.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            finish();
        }

//        detectMaskFromImage();



    }

    private ArrayList<Result> detectMaskFromImage() {
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(imageBitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        Log.d(TAG, "detectMaskFromImage: "+inputTensor);

        final Tensor[] outputTensor = mModule.forward(IValue.from(inputTensor)).toTensorList();

        final float[] bboxList = outputTensor[0].getDataAsFloatArray();
        final float[] scoreList = outputTensor[1].getDataAsFloatArray();
        final long[] classList = outputTensor[2].getDataAsLongArray();

        Log.d(TAG, "detectMaskFromImage: "+bboxList.length + scoreList.length + classList.length);

        ArrayList<Result> resultArrayList = new ArrayList<>();
        for (int i=0, j=0, k=0 ; i<bboxList.length && j<scoreList.length && k<classList.length ; i=i+3, j++, k++){

            Rect rect = new Rect();
            rect.left = (int) bboxList[i];
            rect.top = (int) bboxList[i+1];
            rect.right = (int) bboxList[i+2];
            rect.bottom = (int) bboxList[i+3];

            int cls = (int) classList[k];
            float score = classList[k];

            Result result = new Result(cls, score, rect);
            resultArrayList.add(result);
        }

        final ArrayList<Result> results = PrePostProcessor.nonMaxSuppression(resultArrayList, 10, (float) 0.3);

        Log.d(TAG, "detectMaskFromImage: "+Arrays.toString(resultArrayList.toArray()));
        Log.d(TAG, "detectMaskFromImage: "+Arrays.toString(results.toArray()));

        resultView.setResults(resultArrayList);


        return resultArrayList;
    }

    private Bitmap loadImage(String imageFile){
        Bitmap bitmap = null;
        try {
           bitmap  = BitmapFactory.decodeStream(getAssets().open(imageFile));
//           bitmap =  Bitmap.createScaledBitmap(bitmap, 512, 512, false);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "BitMap Null", Toast.LENGTH_LONG).show();
            return bitmap;
        }

        return bitmap;


    }

    private void infalteLayout() {
        imageView = findViewById(R.id.mainActivity_iv);
        textView = findViewById(R.id.mainActivity_tv);
        materialButton = findViewById(R.id.mainActivity_mbtn);
        resultView = findViewById(R.id.mainactivity_rv);
    }

    private Module getObjectDetectionModelFromAsset(String fileName) throws FileNotFoundException {
//        Module module = PyTorchAndroid.loadModuleFromAsset(getAssets(), fileName);
        Module module = PyTorchAndroid.loadModuleFromAsset(getAssets(), fileName);
        return module;
    }

}