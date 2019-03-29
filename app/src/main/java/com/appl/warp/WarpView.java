package com.appl.warp;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

/**
 * Warp animation view.
 *
 * @author Martin Appl (appl)
 */
public class WarpView extends View implements ContinuousAnimationHelper.Listener {
    /**
     * Duration of animating increase in star birth rate.
     */
    private static final int BIRTH_RATE_ACCELERATION_DURATION = 2000;
    /**
     * Duration of speed up in animation.
     */
    private static final int SPEED_UP_ANIMATION_DURATION = 1000;

    /**
     * Base star speed as a fraction of view per millisecond.
     */
    private static final float BASIC_SPEED = 0.001f;
    /**
     * Base length of warped star in dp.
     */
    private static final int BASE_LINE_LENGTH_DP = 12;
    /**
     * Radius from center, within center born stars are created.
     */
    private static final float BIRTH_DISTANCE_FACTOR = 0.04f;
    /**
     * Radius from center where previously born stars are removed to speed up animation finish.
     */
    private static final float SLOW_DOWN_REMOVE_STAR_THRESHOLD = 0.15f;
    /**
     * Shake amplitude as a fraction of smaller widget side.
     */
    private static final float SHAKE_AMPLITUDE_FACTOR = 0.05f;
    /**
     * Shaking frequency factor for Perlin noise.
     */
    private static final float NOISE_FREQUENCY_FACTOR = 0.03f;

    /**
     * Max speed as a fraction of view per millisecond.
     */
    private static final float FULL_SPEED = 20 * BASIC_SPEED;
    /**
     * Star birth rate in center during full speed.
     */
    private static final float FULL_SPEED_BIRTH_RATE_CENTER = 0.2f;
    /**
     * Star birth everywhere else but center during full speed.
     */
    private static final float FULL_SPEED_BIRTH_RATE_RANDOM = 1f;


    /**
     * Min speed as a fraction of view per millisecond.
     */
    private static final float START_SPEED = 3f * BASIC_SPEED;
    /**
     * Star birth rate in center during start speed.
     */
    private static final float START_SPEED_BIRTH_RATE_CENTER = 0.01f;
    /**
     * Star birth everywhere else but center during start speed.
     */
    private static final float START_SPEED_BIRTH_RATE_RANDOM = 0.1f;

    /**
     * Scale factor of warp zone bitmap when animated out.
     */
    private static final float MAX_WARP_ZONE_SCALE = 6f;
    /**
     * Factor which multiplies half of smaller widget side to get warp zone radius.
     */
    private static final float WARP_ZONE_FACTOR = 2f;

    private final LinkedList<Star> mStars = new LinkedList<>();
    private final ContinuousAnimationHelper mAnimationHelper = new ContinuousAnimationHelper(this);
    private final Paint mPaint = new Paint();
    private final Random mRandom = new Random(System.currentTimeMillis());
    private final ParticleSystem mWhiteStars = new ParticleSystem();
    private final ParticleSystem mBlueStars = new ParticleSystem();
    private final float mBaseLineLength;
    private final int mColorWhite;
    private final int mColorBlue;
    private final int mColorBackground;

    private RadialGradient mBackgroundCenterShader;
    private RadialGradient mWarpZoneShader;

    /**
     * Current speed as a fraction of distance from widget center to edge per millisecond;
     */
    private float mSpeed = START_SPEED;
    private float mBirthRateCenter = FULL_SPEED_BIRTH_RATE_CENTER;
    private float mBirthRateRandom = FULL_SPEED_BIRTH_RATE_RANDOM;
    private float mBirthFractionFromLastFrameCenter;
    private float mBirthFractionFromLastFrameRandom;

    private float mShakeAmplitude;
    private float mShakeTimelineX = mRandom.nextFloat();
    private float mShakeTimelineY = mRandom.nextFloat();
    private float mCameraOffsetX;
    private float mCameraOffsetY;
    private float mWarpZoneScale = 0f;
    private float mWarpZoneAlpha = 0f;

    private float mGlobalAlpha = 1f;

    private boolean mIsFinishing = false;
    private boolean mIsStarted;
    private boolean mShakeAmplitudeInvalid;
    private AnimatorSet mStartAnimations;

    public WarpView(Context context) {
        this(context, null);
    }

    public WarpView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WarpView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mBaseLineLength = convertDpToPx(BASE_LINE_LENGTH_DP);

        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(convertDpToPx(2f));
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        final Resources resources = getResources();
        mColorWhite = resources.getColor(R.color.warp_color_white);
        mColorBlue = resources.getColor(R.color.warp_color_blue);
        mColorBackground = resources.getColor(R.color.warp_background);
    }

    /**
     * Representation of single star stretched by warping.
     */
    private static final class Star {
        /**
         * Angle from center of view in radians.
         */
        float mAngle;
        /**
         * Stretch coefficient that is multiplied with calculated length and speed of stretched star.
         * This creates impression that some stars are closer and some further away from warp travel line.
         */
        float mStretchFactor;
        /**
         * Distance from the center of view. In range 0-1.
         */
        float mDistance;
        /**
         * Color of this particle.
         */
        int mColor;
    }

    /**
     * Generates new random star at given distance from center.
     *
     * @param distance The distance from center.
     * @return New Star instance.
     */
    private Star generateStar(float distance) {
        Star star = new Star();
        star.mAngle = UIUtils.getRandomFromRange(0f, (float) (2.0 * Math.PI), mRandom);
        star.mStretchFactor = UIUtils.getRandomFromRange(0.3f, 1.0f, mRandom);
        star.mDistance = distance;
        star.mColor = mRandom.nextBoolean() ? mColorWhite : mColorBlue;
        return star;
    }

    /**
     * Remove all stars from the animation state.
     */
    public void clearStars() {
        mStars.clear();
    }

    @Override
    public void onUpdateAnimationState(long animationTime, long deltaTime) {
        Iterator<Star> iterator = mStars.iterator();
        while (iterator.hasNext()) {
            Star star = iterator.next();
            if (star.mDistance > 1f) {
                iterator.remove();
            } else if (star.mDistance < BIRTH_DISTANCE_FACTOR) {
                star.mDistance += deltaTime * BIRTH_DISTANCE_FACTOR * 0.001;
            } else {
                final float speed = mSpeed * UIUtils.getDecelerateInterpolation(star.mStretchFactor);
                star.mDistance += deltaTime * speed * UIUtils.getAccelerateInterpolation(star.mDistance);
            }
        }

        final float rawCountCenter = deltaTime * mBirthRateCenter * UIUtils.getRandomFromRange(0.5f, 1f, mRandom) +
            mBirthFractionFromLastFrameCenter;
        final int birthCountCenter = (int) rawCountCenter;
        mBirthFractionFromLastFrameCenter = (float) (rawCountCenter - Math.floor(rawCountCenter));
        for (int i = 0; i < birthCountCenter; i++) {
            mStars.add(generateStar(
                UIUtils.getRandomFromRange(BIRTH_DISTANCE_FACTOR * 0.0001f, BIRTH_DISTANCE_FACTOR, mRandom)));
        }

        final float rawCountRandom = deltaTime * mBirthRateRandom * UIUtils.getRandomFromRange(0.5f, 1f, mRandom) +
            mBirthFractionFromLastFrameRandom;
        final int birthCountRandom = (int) rawCountRandom;
        mBirthFractionFromLastFrameRandom = (float) (rawCountRandom - Math.floor(rawCountRandom));
        for (int i = 0; i < birthCountRandom; i++) {
            mStars.add(generateStar(UIUtils.getDecelerateInterpolation(
                UIUtils.getRandomFromRange(BIRTH_DISTANCE_FACTOR, 0.8f, mRandom))));
        }

        updateParticleSystems();

        if (mShakeAmplitude > 0f) {
            if (mShakeTimelineX > 10f) {
                mShakeTimelineX = mRandom.nextFloat();
            }
            if (mShakeTimelineY > 10f) {
                mShakeTimelineY = mRandom.nextFloat();
            }
            mShakeTimelineX += deltaTime * NOISE_FREQUENCY_FACTOR;
            mShakeTimelineY += deltaTime * NOISE_FREQUENCY_FACTOR;
            mCameraOffsetX = PerlinNoise.octavedNoise(mShakeTimelineX, 2, 1.5f, 0.75f) * mShakeAmplitude;
            mCameraOffsetY = PerlinNoise.octavedNoise(mShakeTimelineY, 2, 1.5f, 0.75f) * mShakeAmplitude;
        } else {
            mCameraOffsetX = 0f;
            mCameraOffsetY = 0f;
        }

        if(mIsFinishing && mStars.isEmpty()){
            mAnimationHelper.stopAnimation();
            mIsStarted = false;
        }

        invalidate();
    }

    private void updateParticleSystems() {
        mWhiteStars.clear();
        mBlueStars.clear();

        final float widthHalf = getWidth() / 2f;
        final float heightHalf = getHeight() / 2f;
        final float edgeRadius = (float) Math.hypot(widthHalf, heightHalf) + mShakeAmplitude;
        final float speedFactor = (mSpeed / BASIC_SPEED + 1f);

        for (Star star : mStars) {
            if (star.mDistance < BIRTH_DISTANCE_FACTOR) {
                continue;
            }
            final float offset = edgeRadius * star.mDistance;
            final float distanceFactor = UIUtils.getAccelerateInterpolation(star.mDistance);
            final float length = 1f + distanceFactor * speedFactor * star.mStretchFactor * mBaseLineLength;
            final float cos = (float) Math.cos(star.mAngle);
            final float sin = (float) Math.sin(star.mAngle);
            final float x1 = cos * offset;
            final float y1 = sin * offset;
            final float x2 = cos * (offset + length);
            final float y2 = sin * (offset + length);

            (star.mColor == mColorWhite ? mWhiteStars : mBlueStars).addLine(x1, y1, x2, y2);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int[] centerGradientColors = getResources().getIntArray(R.array.warp_center_gradient);
        float[] centerGradientStops = new float[3];
        centerGradientStops[0] = 0f;
        centerGradientStops[1] = 0.15f;
        centerGradientStops[2] = 1f;
        final float halfWidth = w / 2f;
        final float halfHeight = h / 2f;
        float radius = Math.min(halfHeight, halfWidth);
        mBackgroundCenterShader = new RadialGradient(0f, 0f, radius, centerGradientColors, centerGradientStops,
                TileMode.CLAMP);

        int[] warpZoneGradientColors = getResources().getIntArray(R.array.warp_zone_gradient);
        float[] warpZoneGradientStops = new float[6];
        warpZoneGradientStops[0] = 0f;
        warpZoneGradientStops[1] = 0.1f;
        warpZoneGradientStops[2] = 0.22f;
        warpZoneGradientStops[3] = 0.55f;
        warpZoneGradientStops[4] = 0.7f;
        warpZoneGradientStops[5] = 1f;
        mWarpZoneShader = new RadialGradient(0f, 0f, WARP_ZONE_FACTOR * radius, warpZoneGradientColors,
                warpZoneGradientStops, TileMode.CLAMP);

        if(mShakeAmplitudeInvalid){
            mShakeAmplitude = Math.min(w * SHAKE_AMPLITUDE_FACTOR,
                    h * SHAKE_AMPLITUDE_FACTOR);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int startState = canvas.save();
        final float halfWidth = getWidth() / 2f;
        final float halfHeight = getHeight() / 2f;

        canvas.drawColor(UIUtils.changeColorAlpha(mColorBackground, (int) (mGlobalAlpha * 255f)));

        canvas.translate(halfWidth + mCameraOffsetX, halfHeight + mCameraOffsetY);

        mPaint.setColor(UIUtils.changeColorAlpha(mColorWhite, (int) (mGlobalAlpha * 255f)));
        for (int i = 0; i < mWhiteStars.getPoolCount(); i++) {
            final ParticleSystem.ParticlePool pool = mWhiteStars.getParticlePool(i);
            canvas.drawLines(pool.getLineCoordinates(), 0, pool.getOccupiedLineCoordinatesCount(), mPaint);
        }

        mPaint.setColor(UIUtils.changeColorAlpha(mColorBlue, (int) (mGlobalAlpha * 255f)));
        for (int i = 0; i < mBlueStars.getPoolCount(); i++) {
            final ParticleSystem.ParticlePool pool = mBlueStars.getParticlePool(i);
            canvas.drawLines(pool.getLineCoordinates(), 0, pool.getOccupiedLineCoordinatesCount(), mPaint);
        }

        float centerRadius = Math.min(halfHeight, halfWidth);
        mPaint.setShader(mBackgroundCenterShader);
        mPaint.setAlpha((int) (mGlobalAlpha * 255f));
        canvas.drawRect(-centerRadius, -centerRadius, centerRadius, centerRadius, mPaint);
        mPaint.setShader(null);

        if (mWarpZoneAlpha > 0f && mWarpZoneScale > 0f) {
            mPaint.setAlpha((int) (mWarpZoneAlpha * mGlobalAlpha * 255f));
            mPaint.setShader(mWarpZoneShader);
            final float warpRadius = centerRadius * WARP_ZONE_FACTOR;
            if (mWarpZoneScale != 1f) {
                canvas.scale(mWarpZoneScale, mWarpZoneScale);
                canvas.drawRect(-warpRadius, -warpRadius, warpRadius, warpRadius, mPaint);
                canvas.restore();
            } else {
                canvas.drawRect(-warpRadius, -warpRadius, warpRadius, warpRadius, mPaint);
            }
            mPaint.setShader(null);
            mPaint.setAlpha(255);
        }

        canvas.restoreToCount(startState);
    }

    private ValueAnimator animateStarBirthAcceleration() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                final float value = (float) valueAnimator.getAnimatedValue();
                mBirthRateCenter = START_SPEED_BIRTH_RATE_CENTER +
                        (FULL_SPEED_BIRTH_RATE_CENTER - START_SPEED_BIRTH_RATE_CENTER) * value;
                mBirthRateRandom = START_SPEED_BIRTH_RATE_RANDOM +
                        (FULL_SPEED_BIRTH_RATE_RANDOM - START_SPEED_BIRTH_RATE_CENTER) * value;
            }
        });
        animator.setDuration(BIRTH_RATE_ACCELERATION_DURATION);
        animator.setInterpolator(new AccelerateInterpolator());
        return animator;
    }

    private ValueAnimator animateSpeedUp() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                final float value = (float) valueAnimator.getAnimatedValue();
                mSpeed = START_SPEED + (FULL_SPEED - START_SPEED) * value;
                mShakeAmplitude = Math.min(WarpView.this.getWidth() * SHAKE_AMPLITUDE_FACTOR,
                        WarpView.this.getHeight() * SHAKE_AMPLITUDE_FACTOR)
                        * value;
            }
        });
        animator.setDuration(SPEED_UP_ANIMATION_DURATION);
        animator.setStartDelay(BIRTH_RATE_ACCELERATION_DURATION - SPEED_UP_ANIMATION_DURATION);
        return animator;
    }

    private ValueAnimator animateInWarpZones(){
        mWarpZoneAlpha = 1f;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float value = (float) animation.getAnimatedValue();
                mWarpZoneScale = value;
            }
        });
        animator.setDuration(SPEED_UP_ANIMATION_DURATION);
        animator.setStartDelay(BIRTH_RATE_ACCELERATION_DURATION - SPEED_UP_ANIMATION_DURATION);
        return animator;
    }

    /**
     * Start warp animation. Stars will gradually fill the widget and than speed up.
     */
    public void startWarpAnimation() {
        clearStars();
        mIsFinishing = false;
        mIsStarted = true;
        mSpeed = START_SPEED;
        mShakeAmplitude = 0f;

        mStartAnimations = new AnimatorSet();
        mStartAnimations.playTogether(animateStarBirthAcceleration(), animateSpeedUp(), animateInWarpZones());
        mStartAnimations.start();

        mAnimationHelper.startAnimation();
    }

    /**
     * Restarts animation with stars around.
     */
    public void restartAnimation(){
        mIsFinishing = false;
        mIsStarted = true;
        mSpeed = FULL_SPEED;
        mBirthRateCenter = FULL_SPEED_BIRTH_RATE_CENTER;
        mBirthRateRandom = FULL_SPEED_BIRTH_RATE_RANDOM;
        mShakeAmplitudeInvalid = true;
        mWarpZoneAlpha = 1f;
        mWarpZoneScale = 1f;

        clearStars();
        for (int i = 0; i < 50; i++) {
            mStars.add(generateStar(
                    UIUtils.getRandomFromRange(BIRTH_DISTANCE_FACTOR * 0.0001f, BIRTH_DISTANCE_FACTOR, mRandom)));
        }
        for (int i = 0; i < 600; i++) {
            mStars.add(generateStar(UIUtils.getDecelerateInterpolation(
                    UIUtils.getRandomFromRange(BIRTH_DISTANCE_FACTOR, 0.8f, mRandom))));
        }

        mAnimationHelper.startAnimation();
    }

    /**
     * Stop running animators when paused.
     */
    public void onPause(){
        if(mIsStarted){
            mAnimationHelper.stopAnimation();
        }
    }

    /**
     * Restart animators when resumed.
     */
    public void onResume(){
        if(mIsStarted){
            mAnimationHelper.startAnimation();
        }
    }

    /**
     * Set alpha for all components of warp animation. Can be used to fade in.
     *
     * @param alpha The global alpha value.
     */
    public void setGlobalAlpha(float alpha){
        mGlobalAlpha = alpha;
        invalidate();
    }

    /**
     * Finish warp animation. Stars will fly out of the widget and warp zone image is stretched and faded away.
     *
     * @param slowDownDuration Duration of shaking slow down.
     */
    public void finishWarpAnimation(int slowDownDuration) {
        if(mStartAnimations != null && mStartAnimations.isStarted()){
            mStartAnimations.cancel();
            mStartAnimations = null;
        }

        mBirthRateCenter = 0f;
        mBirthRateRandom = 0f;
        mIsFinishing = true;

        Iterator<Star> iterator = mStars.iterator();
        while (iterator.hasNext()) {
            Star star = iterator.next();
            if (star.mDistance < SLOW_DOWN_REMOVE_STAR_THRESHOLD) {
                iterator.remove();
            }
        }

        AnimatorSet set = new AnimatorSet();

        ValueAnimator shakeAnimator = ValueAnimator.ofFloat(1f, 0f);
        shakeAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                final float value = (float) valueAnimator.getAnimatedValue();
                mShakeAmplitude = Math.min(WarpView.this.getWidth() * SHAKE_AMPLITUDE_FACTOR,
                        WarpView.this.getHeight() * SHAKE_AMPLITUDE_FACTOR)
                        * value;
            }
        });
        ValueAnimator warpZoneAnimator = ValueAnimator.ofFloat(0f, 1f);
        warpZoneAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                final float value = (float) valueAnimator.getAnimatedValue();
                mWarpZoneScale = 1f + value * (MAX_WARP_ZONE_SCALE - 1f);
                mWarpZoneAlpha = 1f - value;
            }
        });
        warpZoneAnimator.setInterpolator(new AccelerateInterpolator());

        set.playTogether(shakeAnimator, warpZoneAnimator);
        set.setDuration(slowDownDuration);
        set.start();
    }

    /**
     * Converts dp to pixels. Float input and output.
     *
     * @param dp Size in dp.
     * @return Size in pixels.
     */
    private float convertDpToPx(float dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
    }
}
