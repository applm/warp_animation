package com.appl.warp;

import android.animation.Animator;
import android.animation.TimeAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.support.annotation.NonNull;

/**
 * Helper for backwards compatible time animator. Provides {@link Listener#onUpdateAnimationState(long, long)}
 * callback which can update state of continuous animations.
 */
public class ContinuousAnimationHelper {

    /**
     * Interface for update animation callback.
     */
    public interface Listener {
        /**
         * Called when new frame of animation should be prepared.
         *
         * @param animationTime Total time since start of continuous animation.
         * @param deltaTime Delta time elapsed from previous animation frame.
         */
        void onUpdateAnimationState(long animationTime, long deltaTime);
    }

    private Listener mListener;
    private Animator mAnimator;
    private long mLastUpdateTime;
    private boolean mIsStarted;

    /**
     * The constructor.
     *
     * @param listener The listener for animation updates.
     */
    public ContinuousAnimationHelper(@NonNull Listener listener) {
        mListener = listener;
    }

    @TargetApi(16)
    private void initTimeAnimator() {
        TimeAnimator timeAnimator = new TimeAnimator();
        timeAnimator.setTimeListener(new TimeAnimator.TimeListener() {
            @Override
            public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
                mListener.onUpdateAnimationState(totalTime, deltaTime);
                mLastUpdateTime = totalTime;
            }
        });
        mAnimator = timeAnimator;
    }

    private void initValueAnimator() {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0f);
        valueAnimator.setDuration(Long.MAX_VALUE);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                long totalTime = animation.getCurrentPlayTime();
                long deltaTime = totalTime - mLastUpdateTime;
                mListener.onUpdateAnimationState(totalTime, deltaTime);
                mLastUpdateTime = totalTime;
            }
        });
        mAnimator = valueAnimator;
    }

    /**
     * Start continuous animation. You will get updates to {@link Listener#onUpdateAnimationState(long, long)}.
     */
    public void startAnimation() {
        if (!mIsStarted) {
            mIsStarted = true;

            if (VERSION.SDK_INT >= 16) {
                initTimeAnimator();
            } else {
                initValueAnimator();
            }

            mAnimator.start();
        }
    }

    /**
     * Stop continuous animation.
     */
    public void stopAnimation() {
        if(mIsStarted) {
            mIsStarted = false;
            if (mAnimator != null) {
                mAnimator.end();
                mAnimator = null;
            }
        }
    }

    /**
     * Check if time animator is started.
     *
     * @return True if animator running, false otherwise.
     */
    public boolean isStarted(){
        return mIsStarted;
    }
}
