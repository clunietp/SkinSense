package com.arnab.skinsense;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

public class TfModel {

    static final String TAG = "TfModel";
    Interpreter _tfInt = null;

    private static final String MODEL_FILE_TFLITE = "stack_model.tflite";

    /** Memory-map the model file in Assets. */
    // https://stackoverflow.com/q/50481852
    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd( MODEL_FILE_TFLITE );
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public TfModel( Activity activity ) {

        try {
            this._tfInt = new Interpreter( loadModelFile(activity ) );
            Log.i(TAG, "Successfully loaded TF-Lite model");
        }
        catch ( Exception ex ) {
            Log.e(TAG, ex.toString() );
        }


    }

    /*
        Preprocesses RGB bitmap IAW keras/imagenet

        Port of https://github.com/tensorflow/tensorflow/blob/v2.3.1/tensorflow/python/keras/applications/imagenet_utils.py#L169
        with data_format='channels_last', mode='caffe'

         Convert the images from RGB to BGR, then will zero-center each color channel with respect to the ImageNet dataset, without scaling.
         Returns 3D float array
    */
    static float[][][] imagenet_preprocess_input_caffe( Bitmap bitmap ) {
        // https://github.com/tensorflow/tensorflow/blob/v2.3.1/tensorflow/python/keras/applications/imagenet_utils.py#L210
        final float[] imagenet_means_caffe = new float[]{103.939f, 116.779f, 123.68f};

        float[][][] result = new float[bitmap.getHeight()][bitmap.getWidth()][3];   // assuming rgb
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x++) {

                final int px = bitmap.getPixel(x, y);

                // rgb-->bgr, then subtract means.  no scaling
                result[y][x][0] = (Color.blue(px) - imagenet_means_caffe[0] );
                result[y][x][1] = (Color.green(px) - imagenet_means_caffe[1] );
                result[y][x][2] = (Color.red(px) - imagenet_means_caffe[2] );
            }
        }

        return result;
    }

    /*
    Run inference, return array of 7 probabilities.  result[0] is binary, results[1-6] are categorical probabilities
     */
    public float[] run( Activity activity, Bitmap bitmap, float[] metaData ) {

        float[][] meta = new float[1][18];
        assert(metaData.length==18);
        meta[0]=metaData;

        /*
        // test patient data for PAT_1790_3425_942.png
        meta[0] = new float[]{
                0.f, 0.f, 0.41124412f,  0.f, 0.f, 1.f,
                1.f,1.f, -0.28738758f, 5.f, -0.17573152f, -0.11812692f,
                1.f, 0.f, 0.f, 0.f, 0.f, 1.f
        };
         */

        // load the test image
        /*
        Bitmap bitmap = null;
        try {
            // get input stream
            bitmap = BitmapFactory.decodeStream(activity.getAssets().open("PAT_1790_3425_942.png"));
        }
        catch(IOException ex) {
            Log.e(TAG,ex.toString());
            return;
        }
         */

        assert (bitmap.getWidth() == bitmap.getHeight() && bitmap.getHeight() == 224 );

        // run imagenet preprocessing
        float[][][][] imgValues = new float[1][bitmap.getHeight()][bitmap.getWidth()][3];
        imgValues[0]=imagenet_preprocess_input_caffe(bitmap);

        Map<Integer,Object> outputs = new HashMap<Integer, Object>();
        outputs.put(0, new float[1][1]);    // binary
        outputs.put(1, new float[1][6]);    // categorical

        // run inference
        //  https://www.tensorflow.org/lite/guide/inference#load_and_run_a_model_in_java

        Object inputs[] = new Object[]{ imgValues, meta };

        this._tfInt.runForMultipleInputsOutputs( inputs, outputs );

        float[] result = new float[7];
        float[] output0 = ((float[][])outputs.get(0))[0];
        float[] output1 = ((float[][])outputs.get(1))[0];

        result[0]= output0[0];
        result[1]=output1[0];
        result[2]=output1[1];
        result[3]=output1[2];
        result[4]=output1[3];
        result[5]=output1[4];
        result[6]=output1[5];

        return result;
    }

}