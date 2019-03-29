package com.appl.warp;

import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.support.annotation.ColorInt;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

/**
 * Helper methods for UI effects.
 *
 * @author Martin Appl
 */
public class UIUtils {
    /**
     * Mixes two colors together according to progress represented by amount
     *
     * @param color1 First color to mix.
     * @param color2 Second color to mix
     * @param amount amount of first color in mix.
     * @return The mixed color.
     */
    public static @ColorInt
    int mixTwoColors(@ColorInt int color1,@ColorInt int color2, float amount) {
        final byte ALPHA_CHANNEL = 24;
        final byte RED_CHANNEL = 16;
        final byte GREEN_CHANNEL = 8;
        final byte BLUE_CHANNEL = 0;

        final float inverseAmount = 1.0f - amount;

        int a = ((int) (((float) (color1 >> ALPHA_CHANNEL & 0xff) * amount) +
            ((float) (color2 >> ALPHA_CHANNEL & 0xff) * inverseAmount))) & 0xff;
        int r = ((int) (((float) (color1 >> RED_CHANNEL & 0xff) * amount) +
            ((float) (color2 >> RED_CHANNEL & 0xff) * inverseAmount))) & 0xff;
        int g = ((int) (((float) (color1 >> GREEN_CHANNEL & 0xff) * amount) +
            ((float) (color2 >> GREEN_CHANNEL & 0xff) * inverseAmount))) & 0xff;
        int b = ((int) (((float) (color1 & 0xff) * amount) +
            ((float) (color2 & 0xff) * inverseAmount))) & 0xff;

        return a << ALPHA_CHANNEL | r << RED_CHANNEL | g << GREEN_CHANNEL | b << BLUE_CHANNEL;
    }

    /**
     * Same easing as {@link android.view.animation.AccelerateDecelerateInterpolator} but without need of
     * creating object.
     *
     * @param input The input value in range from 0 to 1
     * @return The eased value.
     */
    public static float getAccelerateDecelerateInterpolation(float input) {
        return (float) (Math.cos((input + 1) * Math.PI) / 2.0f) + 0.5f;
    }

    /**
     * Accelerate easing. Same as default {@link android.view.animation.AccelerateInterpolator}
     *
     * @param input The input value in range from 0 to 1
     * @return The eased value.
     */
    public static float getAccelerateInterpolation(float input) {
        return input * input;
    }

    /**
     * Accelerate easing. Same as default {@link android.view.animation.AccelerateInterpolator}
     *
     * @param input  The input value in range from 0 to 1
     * @param factor Degree to which the animation should be eased. Seting
     *               factor to 1.0f produces a y=x^2 parabola. Increasing factor above
     *               1.0f  exaggerates the ease-in effect (i.e., it starts even
     *               slower and ends evens faster)
     * @return The eased value.
     */
    public static float getAccelerateInterpolation(float input, float factor) {
        return (float) Math.pow(input, 2 * factor);
    }

    /**
     * Decelerate easing. Same as default {@link android.view.animation.DecelerateInterpolator}
     *
     * @param input The input value in range from 0 to 1
     * @return The eased value.
     */
    public static float getDecelerateInterpolation(float input) {
        return 1.0f - (1.0f - input) * (1.0f - input);
    }

    /**
     * Decelerate easing. Same as default {@link android.view.animation.DecelerateInterpolator}
     *
     * @param input  The input value in range from 0 to 1
     * @param factor Degree to which the animation should be eased. Setting factor to 1.0f produces
     *               an upside-down y=x^2 parabola. Increasing factor above 1.0f makes exaggerates the
     *               ease-out effect (i.e., it starts even faster and ends evens slower)
     * @return The eased value.
     */
    public static float getDecelerateInterpolation(float input, float factor) {
        return (float) (1.0f - Math.pow((1.0f - input), 2f * factor));
    }

    /**
     * Works as classic smoothStep function. Interpolates value based on x if it lies between edges.
     *
     * @param edge0 The first edge.
     * @param edge1 The second edge.
     * @param x     The progress value being interpolated.
     * @return The return value in range from 0 to 1.
     */
    public static float smootherStep(float edge0, float edge1, float x) {
        // Scale, and clamp x to 0..1 range
        x = clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        // Evaluate polynomial
        return x * x * x * (x * (x * 6f - 15f) + 10f);
    }

    /**
     * Works as classic step function. Linearly interpolates value based on x if it lies between edges.
     *
     * @param edge0 The first edge.
     * @param edge1 The second edge.
     * @param x     The progress value being interpolated.
     * @return The return value in range from 0 to 1.
     */
    public static float step(float edge0, float edge1, float x) {
        // Scale, and clamp x to 0..1 range
        return clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
    }

    /**
     * Clamps value to ensure it fits between min and max.
     *
     * @param val The value to clamp.
     * @param min The minimum value threshold.
     * @param max The maximum value threshold.
     * @return The resulting value clamped to min, max range.
     */
    public static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    /**
     * Changes just alpha component of given color.
     *
     * @param color The input color.
     * @param alpha The input alpha.
     * @return Color with alpha component set to input alpha.
     */
    public static @ColorInt
    int changeColorAlpha(@ColorInt int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Resolves attribute with dimension type from activity theme.
     *
     * @param activity The activity with theme applied.
     * @param resid    Attribute resource id.
     * @return Dimension resource in pixels with correction for current display metrics.
     */
    public static float getDimensionAttrFromTheme(Activity activity, int resid) {
        Resources.Theme theme = activity.getTheme();
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(resid, typedValue, true);
        return typedValue.getDimension(activity.getResources().getDisplayMetrics());
    }

    /**
     * Gets display width.
     *
     * @param context Application context.
     * @return The display width in pixels.
     */
    public static int getDisplayWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    /**
     * Gets display height.
     *
     * @param activity The activity.
     * @return The display height in pixels.
     */
    public static int getDisplayHeight(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y;
    }

    /**
     * Get length of line between points A and B.
     *
     * @param ax point A x coordinate
     * @param ay point A y coordinate
     * @param bx point B x coordinate
     * @param by point B y coordinate
     * @return length
     */
    public static float getLineLength(float ax, float ay, float bx, float by) {
        final float vx = bx - ax;
        final float vy = by - ay;
        return (float) Math.sqrt(vx * vx + vy * vy);
    }

    /**
     * Return pseudo random number within given range.
     *
     * @param start Start of the range.
     * @param end   End of the range.
     * @return Random number within range.
     */
    public static float getRandomFromRange(float start, float end) {
        return (float) (start + (end - start) * Math.random());
    }

    /**
     * Return pseudo random number within given range.
     *
     * @param start  Start of the range.
     * @param end    End of the range.
     * @param random Instance of Random class.
     * @return Random number within range.
     */
    public static float getRandomFromRange(float start, float end, Random random) {
        return start + (end - start) * random.nextFloat();
    }

    /**
     * Calculates measured value for {@link View#setMeasuredDimension(int, int)} with restrictions from measureSpec.
     *
     * @param desiredDimension     Size the view wants to have in particular axis.
     * @param dimensionMeasureSpec Measure specification with constrains implied by parent view.
     * @return Measured dimension the view will have in particular axis.
     */
    public static int calculateMeasuredDimension(int desiredDimension, int dimensionMeasureSpec) {
        int mode = View.MeasureSpec.getMode(dimensionMeasureSpec);
        int size = View.MeasureSpec.getSize(dimensionMeasureSpec);

        int dimension;

        //Measure
        if (mode == View.MeasureSpec.EXACTLY) {
            //Must be this size
            dimension = size;
        } else if (mode == View.MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            dimension = Math.min(desiredDimension, size);
        } else {
            //Be whatever you want
            dimension = desiredDimension;
        }
        return dimension;
    }

}
