#include <jni.h>
#include <stdio.h>
#include "com_example_EndCityFinder.h"
#include "generator.h"
#include "finders.h"
#include <stdint.h>
#include <math.h>

// Structure to represent an End City with coordinates and ship presence
typedef struct {
    int x;
    int z;
    int hasShip;
} EndCity;

// JNI method implementation to find End Cities
JNIEXPORT jobjectArray JNICALL Java_com_example_EndCityFinder_findEndCities(JNIEnv *env, jobject obj, jlong seed, jint centerX, jint centerZ, jint radius) {
    // Set up the generator for the End dimension
    Generator g;
    setupGenerator(&g, MC_1_20, 0);
    applySeed(&g, DIM_END, seed);

    // Get the structure configuration for End Cities
    StructureConfig endCityConfig;
    if (!getStructureConfig(End_City, MC_1_20, &endCityConfig)) {
        printf("Failed to get structure configuration for End City.\n");
        return NULL;
    }

    // Initialize surface noise for End City terrain check
    SurfaceNoise sn;
    initSurfaceNoise(&sn, DIM_END, seed);

    // Calculate the region boundaries
    double blocksPerRegion = endCityConfig.regionSize * 16.0;
    int regXStart = (int) floor((centerX - radius) / blocksPerRegion);
    int regXEnd = (int) ceil((centerX + radius) / blocksPerRegion);
    int regZStart = (int) floor((centerZ - radius) / blocksPerRegion);
    int regZEnd = (int) ceil((centerZ + radius) / blocksPerRegion);

    // List to hold End City objects
    jclass endCityClass = (*env)->FindClass(env, "com/example/EndCity");
    jobjectArray endCities = (*env)->NewObjectArray(env, 100, endCityClass, NULL); // Assume a max of 100 End Cities
    int endCityCount = 0;

    // Search for End Cities in the specified area
    for (int regX = regXStart; regX <= regXEnd; regX++) {
        for (int regZ = regZStart; regZ <= regZEnd; regZ++) {
            Pos pos;
            if (!getStructurePos(End_City, MC_1_20, seed, regX, regZ, &pos)) {
                continue; // This region is not suitable
            }
            if (pos.x < centerX - radius || pos.x > centerX + radius || pos.z < centerZ - radius || pos.z > centerZ + radius) {
                continue; // Structure is outside the specified area
            }
            if (!isViableStructurePos(End_City, &g, pos.x, pos.z, 0)) {
                continue; // Biomes are not viable
            }
            if (!isViableEndCityTerrain(&g, &sn, pos.x, pos.z)) {
                continue; // End City terrain is not suitable
            }

            // Check for the presence of a ship
            Piece pieces[END_CITY_PIECES_MAX];
            int numPieces = getEndCityPieces(pieces, seed, pos.x >> 4, pos.z >> 4);
            int hasShip = 0;
            for (int i = 0; i < numPieces; i++) {
                if (pieces[i].type == END_SHIP) {
                    hasShip = 1;
                    break;
                }
            }

            // Create End City object and add to the list
            jobject endCity = (*env)->NewObject(env, endCityClass, (*env)->GetMethodID(env, endCityClass, "<init>", "(IIZ)V"), pos.x, pos.z, hasShip);
            (*env)->SetObjectArrayElement(env, endCities, endCityCount++, endCity);

            if (endCityCount >= 100) break; // Limit the number of End Cities
        }
        if (endCityCount >= 100) break; // Limit the number of End Cities
    }

    return endCities;
}
