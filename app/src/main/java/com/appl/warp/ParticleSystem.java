package com.appl.warp;

import java.util.ArrayList;

import android.graphics.Paint;

/**
 * Particle system for storage of line coordinates. Coordinates are then submitted for drawing by batch method on
 * canvas.
 *
 * @author Martin Appl (appl)
 */
public class ParticleSystem {

    /**
     * Number of particles stored in one pool instance.
     */
    private static final int PARTICLES_PER_POOL = 128;

    private final ArrayList<ParticlePool> mPoolList = new ArrayList<>();
    private int mFullPools;

    /**
     * The constructor.
     */
    public ParticleSystem() {
        mPoolList.add(new ParticlePool());
    }

    /**
     * Instance of this class stores particles that can be submitted to canvas with single draw call.
     */
    public static class ParticlePool {

        private int mOccupied;
        private float[] mLineCoords = new float[PARTICLES_PER_POOL * 4];

        /**
         * Returns line coordinates in correct format for
         * {@link android.graphics.Canvas#drawLines(float[], int, int, Paint)}.
         *
         * @return Line coordinates in array formatted as x1, y1, x2, y2 ...
         */
        public float[] getLineCoordinates() {
            return mLineCoords;
        }

        /**
         * Returns number of particles in pool currently in use. Rest of data in array is invalid.
         *
         * @return The number of stored particles.
         */
        public int getOccupiedLineCoordinatesCount() {
            return mOccupied * 4;
        }

        /**
         * Add new line coordinates to the pool.
         *
         * @param x1 Line start X coordinate.
         * @param y1 Line start Y coordinate.
         * @param x2 Line end X coordinate.
         * @param y2 Line end Y coordinate.
         */
        public void addLine(float x1, float y1, float x2, float y2) {
            final int baseIndex = mOccupied * 4;
            mLineCoords[baseIndex] = x1;
            mLineCoords[baseIndex + 1] = y1;
            mLineCoords[baseIndex + 2] = x2;
            mLineCoords[baseIndex + 3] = y2;
            mOccupied++;
        }

        /**
         * Check if pool is fully occupied.
         *
         * @return True if the pool is full. False if there is still some empty space.
         */
        public boolean isFull() {
            return mOccupied == PARTICLES_PER_POOL;
        }

        /**
         * Remove all particles from the pool.
         */
        public void clear() {
            mOccupied = 0;
        }
    }

    /**
     * Get number of particle pools in the system.
     *
     * @return Particles pool count.
     */
    public int getPoolCount() {
        return mPoolList.size();
    }

    /**
     * Get particle pool at specified index.
     *
     * @param index Index of particle pool.
     * @return The particle pool at given index.
     */
    public ParticlePool getParticlePool(int index) {
        return mPoolList.get(index);
    }

    /**
     * Remove all particle pools from the system.
     */
    public void clear() {
        for(int i = 0; i < mPoolList.size(); i++){
            mPoolList.get(i).clear();
        }
        mFullPools = 0;
    }

    /**
     * Add new line to particle system. If all current pools are full, new pool is created and added to the system.
     *
     * @param x1 Line start X coordinate.
     * @param y1 Line start Y coordinate.
     * @param x2 Line end X coordinate.
     * @param y2 Line end Y coordinate.
     */
    public void addLine(float x1, float y1, float x2, float y2) {
        ParticlePool pool = mPoolList.get(mFullPools);
        if (pool.isFull()) {
            mFullPools++;
            if (mFullPools == mPoolList.size()) {
                mPoolList.add(new ParticlePool());
            }
            pool = mPoolList.get(mFullPools);
        }
        pool.addLine(x1, y1, x2, y2);
    }
}
