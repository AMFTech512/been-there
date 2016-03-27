package com.austinfay.beenthere;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;

/**
 * Created by Austin on 7/18/2015.
 * Beenthere
 */
public class MapColors {

    public static float colorsFloat[] = {
        BitmapDescriptorFactory.HUE_AZURE,
        BitmapDescriptorFactory.HUE_BLUE,
        BitmapDescriptorFactory.HUE_CYAN,
        BitmapDescriptorFactory.HUE_GREEN,
        BitmapDescriptorFactory.HUE_MAGENTA,
        BitmapDescriptorFactory.HUE_ORANGE,
        BitmapDescriptorFactory.HUE_RED,
        BitmapDescriptorFactory.HUE_ROSE,
        BitmapDescriptorFactory.HUE_VIOLET,
        BitmapDescriptorFactory.HUE_YELLOW
    };

    /*public static int markerBitmaps[] = {
            R.drawable.map_marker_azure,
            R.drawable.map_marker_blue,
            R.drawable.map_marker_cyan,
            R.drawable.map_marker_green,
            R.drawable.map_marker_magenta,
            R.drawable.map_marker_orange,
            R.drawable.map_marker_red,
            R.drawable.map_marker_rose,
            R.drawable.map_marker_violet,
            R.drawable.map_marker_yellow
    };*/

    public static String colorsString[] = {
            "#439bf4",
            "#3E3EC4",
            "#3BD3D3",
            "#00CA10",
            "#E2519C",
            "#F26000",
            "#CC000D",
            "#EA2C8B",
            "#8558B5",
            "#D8D800"
    };

    public static Bitmap generateIcon(int color, Context context, int resource){

        Bitmap icon1 = BitmapFactory.decodeResource(context.getResources(),
                resource);

        Bitmap image = icon1.copy(icon1.getConfig(), true);

        int startColor = 0xff808080;

        float targetHSV[] = new float[3];

        Color.colorToHSV(color, targetHSV);

        for(int x = 0; x < image.getWidth(); x++){

            for(int y = 0; y < image.getHeight(); y++){

                int pixColor = image.getPixel(x, y);
                float pixHSV[] = new float[3];
                Color.colorToHSV(pixColor, pixHSV);

                if(pixColor == startColor) {
                    image.setPixel(x, y, color);
                } else if(pixHSV[0] > 121 && pixHSV[0] < 140){

                    if(pixHSV[1] == 1 && pixHSV[2] == 1){
                        image.setPixel(x, y,
                                Color.HSVToColor(new float[] {0, 0, 100}));
                    } else {

                        float C1_START[] = {128, 128, 128};
                        float C2_START[] = {0, 255, 33};
                        float C3_START[] = {Color.red(pixColor), Color.green(pixColor), Color.blue(pixColor)};

                        float C1_FINISH[] = {Color.red(color), Color.green(color), Color.blue(color)};
                        float C2_FINISH[] = {255, 255, 255};

                        float pVals[] = {
                                (C3_START[0] - C1_START[0]) / (C2_START[0] - C1_START[0]),
                                (C3_START[1] - C1_START[1]) / (C2_START[1] - C1_START[1]),
                                (C3_START[2] - C1_START[2]) / (C2_START[2] - C1_START[2])
                        };

                        float C3_FINISH[] = {
                                (1 - pVals[0]) * C1_FINISH[0] + (pVals[0] * C2_FINISH[0]),
                                (1 - pVals[1]) * C1_FINISH[1] + (pVals[1] * C2_FINISH[1]),
                                (1 - pVals[2]) * C1_FINISH[2] + (pVals[2] * C2_FINISH[2])
                        };

                        image.setPixel(x, y, Color.rgb((int) C3_FINISH[0], (int) C3_FINISH[1], (int) C3_FINISH[2]));

                    }

                } else if(pixHSV[2] != 1 && pixHSV[2] != 0) {
                    image.setPixel(x, y, Color.HSVToColor(Color.alpha(pixColor), targetHSV));
                }

            }

        }

        return image;

    }

    public static float randomColor(){

        int index = (int) (Math.random() * colorsFloat.length);

        return colorsFloat[index];

    }

    public static String getHexValue(float color){

        String returnColor = "";

        for(int i = 0; i < colorsFloat.length; i++){
            if(color == colorsFloat[i]) returnColor = colorsString[i];
        }

        return returnColor;

    }



    /*public static int getMarkerDrawable(float color){

        int returnImage = 0;

        for(int i = 0; i < colorsFloat.length; i++){
            if(color == colorsFloat[i]) returnImage = markerBitmaps[i];
        }

        return returnImage;

    }*/

}
