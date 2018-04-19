package com.baidu.bikenavi_demo;

import com.baidu.mapapi.search.route.BikingRouteLine;
import com.baidu.mapapi.search.route.DrivingRouteLine;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pablo on 18/04/2018.
 */

class SharedDrivingSteps {

    private final static SharedDrivingSteps INSTANCE = new SharedDrivingSteps();
    private List<DrivingRouteLine.DrivingStep> drivingSteps = new ArrayList<>();

    private SharedDrivingSteps(){}

    public static SharedDrivingSteps getInstance(){
        return INSTANCE;
    }

    public void setDrivingSteps(List<DrivingRouteLine.DrivingStep> drivingSteps) {
        if (drivingSteps == null)
            this.drivingSteps = new ArrayList<>();
        else
            this.drivingSteps = drivingSteps;
    }

    public List<DrivingRouteLine.DrivingStep> getDrivingSteps(){
        return drivingSteps;
    }
}

class SharedBikingSteps {

    private final static SharedBikingSteps INSTANCE = new SharedBikingSteps();
    private List<BikingRouteLine.BikingStep> drivingSteps = new ArrayList<>();

    private SharedBikingSteps(){}

    public static SharedBikingSteps getInstance(){
        return INSTANCE;
    }

    public void setDrivingSteps(List<BikingRouteLine.BikingStep> drivingSteps) {
        if (drivingSteps == null)
            this.drivingSteps = new ArrayList<>();
        else
            this.drivingSteps = drivingSteps;
    }

    public List<BikingRouteLine.BikingStep> getDrivingSteps(){
        return drivingSteps;
    }
}