/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.agarwalamit.corineLandcover;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by amit on 31.07.17.
 */

public class LandCoverUtils {

    public static final String CORINE_LANDCOVER_TAG_ID = "CODE_12";

    private static Map<ActivityTypeFromLandCover, List<Integer>> getActivityTypeToLandCoverIds(){

        Map<ActivityTypeFromLandCover, List<Integer>> activityType2LandCoverId = new HashMap<>();
        {
            List<Integer> landCoverIds = new ArrayList<>();
            landCoverIds.add(111); // continuous urban fabric
            landCoverIds.add(112); // Discontinuous urban fabric
            activityType2LandCoverId.put(ActivityTypeFromLandCover.HOME,landCoverIds);
        }
        {
            List<Integer> landCoverIds = new ArrayList<>();
            landCoverIds.add(111); // continuous urban fabric
            landCoverIds.add(112); // Discontinuous urban fabric
            landCoverIds.add(121); //Industrial or commercial use
            landCoverIds.add(123); //Port areas
            landCoverIds.add(124); //Airports
            landCoverIds.add(133); //Construction sites
            landCoverIds.add(142); //Sport and leisure facilities
            activityType2LandCoverId.put(ActivityTypeFromLandCover.OTHER_THAN_HOME, landCoverIds);
        }
        return activityType2LandCoverId;
    }

    public static List<Integer> getLandCoverIds(final ActivityTypeFromLandCover activityTypeFromLandCover){
        return LandCoverUtils.getActivityTypeToLandCoverIds().get(activityTypeFromLandCover);
    }

    public static List<ActivityTypeFromLandCover> getActivitiesTypeFromZone(final int landCoverId){
        Map<ActivityTypeFromLandCover, List<Integer>> activityTypeToLandCoverIds = LandCoverUtils.getActivityTypeToLandCoverIds();
        List<ActivityTypeFromLandCover> output = new ArrayList<>();
        for(ActivityTypeFromLandCover activityTypeFromLandCover : ActivityTypeFromLandCover.values() ) {
            if (activityTypeToLandCoverIds.get(activityTypeFromLandCover).contains(landCoverId)) {
                output.add(activityTypeFromLandCover);
            }
        }
        return output;
    }
}
