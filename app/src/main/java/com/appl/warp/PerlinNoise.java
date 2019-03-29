package com.appl.warp;

/**
 * Helper class for generating Perlin noise. Right now only one dimensional noise is implemented.
 *
 * @author Martin Appl (appl)
 */
public class PerlinNoise {

    /**
     * Permutation table. This is just a random jumble of all numbers 0-255.
     * <p>
     * This produce a repeatable pattern of 256, but Ken Perlin stated
     * that it is not a problem for graphic texture as the noise features disappear
     * at a distance far enough to be able to see a repeatable pattern of 256.
     * <p>
     * This needs to be exactly the same for all instances on all platforms,
     * so it's easiest to just keep it as static explicit data.
     * This also removes the need for any initialisation of this class.
     */
    static final int[] PERMUTATION = {
        151, 160, 137, 91, 90, 15,
        131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23,
        190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32, 57, 177, 33,
        88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175, 74, 165, 71, 134, 139, 48, 27, 166,
        77, 146, 158, 231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244,
        102, 143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169, 200, 196,
        135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226, 250, 124, 123,
        5, 202, 38, 147, 118, 126, 255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42,
        223, 183, 170, 213, 119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9,
        129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224, 232, 178, 185, 112, 104, 218, 246, 97, 228,
        251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241, 81, 51, 145, 235, 249, 14, 239, 107,
        49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254,
        138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180
    };

    /**
     * 1D Perlin simplex noise
     *
     * @param x float coordinate. Must be positive due to its usage as index to hash table.
     * @return Noise value in the range[-1; 1], value of 0 on all integer coordinates.
     */
    public static float noise(float x) {
        float n0, n1;   // Noise contributions from the two "corners"

        // No need to skew the input space in 1D

        // Corners coordinates (nearest integer values):
        int i0 = fastFloor(x);
        int i1 = i0 + 1;
        // Distances to corners (between 0 and 1):
        float x0 = x - i0;
        float x1 = x0 - 1.0f;

        // Calculate the contribution from the first corner
        float t0 = 1.0f - x0 * x0;

        t0 *= t0;
        n0 = t0 * t0 * grad(hash(i0), x0);

        // Calculate the contribution from the second corner
        float t1 = 1.0f - x1 * x1;

        t1 *= t1;
        n1 = t1 * t1 * grad(hash(i1), x1);

        // The maximum value of this noise is 8*(3/4)^4 = 2.53125
        // A factor of 0.395 scales to fit exactly within [-1,1]
        return 0.395f * (n0 + n1);
    }

    /**
     * Adds together outputs of noise with frequencies that are multiples of two. By playing with input parameters
     * can be achieved variety of different noise.
     *
     * @param x         Position of input axis. For example time axis.
     * @param octaves   How many frequencies are put together to produce output.
     * @param roughness How roughly output jumps around.
     * @param scale     How far away are frequencies from each other.
     * @return Noise output value for this given position on input axis.
     */
    public static float octavedNoise(float x, int octaves, float roughness, float scale) {
        float noiseSum = 0;
        float layerFrequency = scale;
        float layerWeight = 1;
        float weightSum = 0;

        if (x < 0f) {
            x *= -1f;
        }

        for (int octave = 0; octave < octaves; octave++) {
            noiseSum += noise(x * layerFrequency) * layerWeight;
            layerFrequency *= 2;
            weightSum += layerWeight;
            layerWeight *= roughness;
        }
        return noiseSum / weightSum;
    }

    private static int fastFloor(float fp) {
        int i = (int) fp;
        return (fp < i) ? (i - 1) : (i);
    }

    /**
     * Helper function to hash an integer using the above permutation table
     * <p>
     * This function is called N+1 times for a noise of N dimension.
     * <p>
     * Using a real hash function would be better to improve the "repeatability of 256" of the above permutation table,
     * but fast integer Hash functions uses more time and have bad random properties.
     *
     * @param i Integer value to hash
     * @return 8-bits hashed value
     */
    private static int hash(int i) {
        return PERMUTATION[i % PERMUTATION.length];
    }

    /**
     * Helper function to compute gradients-dot-residual vectors (1D)
     *
     * @note that these generate gradients of more than unit length. To make
     * a close match with the value range of classic Perlin noise, the final
     * noise values need to be rescaled to fit nicely within [-1,1].
     * (The simplex noise functions as such also have different scaling.)
     * Note also that these noise functions are the most practical and useful
     * signed version of Perlin noise.
     *
     * @param[in] hash  hash value
     * @param[in] x     distance to the corner
     * @return gradient value
     */
    private static float grad(int hash, float x) {
        int h = hash & 0x0F;        // Convert low 4 bits of hash code
        float grad = 1.0f + (h & 7);    // Gradient value 1.0, 2.0, ..., 8.0
        if ((h & 8) != 0) grad = -grad; // Set a random sign for the gradient
        return (grad * x);              // Multiply the gradient with the distance
    }
}
