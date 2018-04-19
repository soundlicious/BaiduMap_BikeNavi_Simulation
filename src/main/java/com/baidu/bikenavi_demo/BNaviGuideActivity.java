/*
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.bikenavi_demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.bikenavi.adapter.IBRouteGuidanceListener;
import com.baidu.mapapi.bikenavi.adapter.IBTTSPlayer;
import com.baidu.mapapi.bikenavi.model.BikeRouteDetailInfo;
import com.baidu.mapapi.bikenavi.model.RouteGuideKind;
import com.baidu.mapapi.bikenavi.params.BikeNaviLaunchParam;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.route.BikingRouteLine;
import com.baidu.mapapi.search.route.DrivingRouteLine.DrivingStep;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static android.content.ContentValues.TAG;

public class BNaviGuideActivity extends Activity implements LocationListener, IBRouteGuidanceListener {

    private static final long LOCATION_REFRESH_TIME = 1;
    private static final float LOCATION_REFRESH_DISTANCE = 1;
    private static final int BNAVIGUIDE_REQUEST_CODE = 100;
    private static final float GPS_ACCURACY = 65;
    public static final String IS_SIMULATION = "isSimulation";
    private BikeNavigateHelper mNaviHelper;

    BikeNaviLaunchParam param =  new BikeNaviLaunchParam();
    private LocationManager mLocationManager;
    private boolean isPermissionRequested = false;
    private Location location;
    private String mocLocationProvider = LocationManager.GPS_PROVIDER;//"MockLocationBikeNaviProvider";
    private boolean isSimulation;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNaviHelper.quit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNaviHelper.resume();
    }


    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 23 && !isPermissionRequested) {

            isPermissionRequested = true;

            ArrayList<String> permissions = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

            if (permissions.size() == 0) {
                initGpsListener();
                return;
            } else {
                requestPermissions(permissions.toArray(new String[permissions.size()]), BNAVIGUIDE_REQUEST_CODE);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void initGpsListener() {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME * 1000,
                LOCATION_REFRESH_DISTANCE, this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        initGpsListener();
    }

    public void mockLocationProvider(){
        if (mLocationManager.getProvider(mocLocationProvider) == null){
            mLocationManager.addTestProvider(mocLocationProvider, false, false,
                    false, false, true, true, true, 0, 5);
            mLocationManager.setTestProviderEnabled(mocLocationProvider, true);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isSimulation = getIntent().getBooleanExtra(IS_SIMULATION, false);

        mNaviHelper = BikeNavigateHelper.getInstance();

        View view = mNaviHelper.onCreate(BNaviGuideActivity.this);
        if (view != null) {
            setContentView(view);
        }
        requestPermission();



        mNaviHelper.startBikeNavi(BNaviGuideActivity.this);

        mNaviHelper.setTTsPlayer((s, b) -> {
            Log.d("tts", s);
            return 0;
        });

        mNaviHelper.setRouteGuidanceListener(this, this);
        if (isSimulation)
            launchSimulation();
    }

    @SuppressLint("MissingPermission")
    private void launchSimulation() {
//        if (mLocationManager.getProvider(mocLocationProvider) == null) {
            mLocationManager.addTestProvider(mocLocationProvider, false, false,
                    false, false, true, true, true, 0, (int) GPS_ACCURACY);
            mLocationManager.setTestProviderEnabled(mocLocationProvider, true);
            //mLocationManager.requestLocationUpdates(mocLocationProvider, 0, 0, this);
//        }
        List<BikingRouteLine.BikingStep> drivingsteps = SharedBikingSteps.getInstance().getDrivingSteps();
        if (drivingsteps != null) {
            LatLng entrancePt = drivingsteps.get(0).getEntrance().getLocation();
            LatLng firstWayPoint = drivingsteps.get(0).getWayPoints().get(0);
            Log.i(TAG, "isEntrancePt == first way point : " + entrancePt.toString().equals(firstWayPoint.toString()) + " entrance Pt : " + entrancePt.toString() + " firstwaypt : " + firstWayPoint.toString());
                Observable
                        .fromArray(drivingsteps)
                        .concatMapIterable(steps -> steps)
                        .map(BikingRouteLine.BikingStep::getWayPoints)
                        .concatMapIterable(waypoints -> waypoints)
                        .zipWith(Observable.interval(LOCATION_REFRESH_TIME, TimeUnit.SECONDS), (item, interval) -> item)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .onErrorResumeNext(throwable -> null)
                        .doOnComplete(()->Log.i(TAG, "Location Mock Completed"))
                        .forEach(this::mockLocation);
        }

    }

    private void mockLocation(LatLng location) {
            Log.i(TAG, "new location to mock : " + location.toString());
            Location mockLocation = new Location(mocLocationProvider);
            mockLocation.setLatitude(location.latitude);
            mockLocation.setLongitude(location.longitude);
            mockLocation.setAltitude(25.496173396880245);
            mockLocation.setTime(System.currentTimeMillis());
            mockLocation.setBearing(90f);
            mockLocation.setSpeed(90);
            BuildConfig
            mockLocation.setAccuracy(GPS_ACCURACY);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                mockLocation.setElapsedRealtimeNanos(System.nanoTime());
            mLocationManager.setTestProviderLocation(mocLocationProvider, mockLocation);
    }

    //region Listener
    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        Log.i(TAG, "onLocationChanged - : " + location.toString());
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onRouteGuideIconUpdate(Drawable icon) {

    }

    @Override
    public void onRouteGuideKind(RouteGuideKind routeGuideKind) {
        Log.i(TAG,"OnRouteGuideKind - " + routeGuideKind );
    }

    @Override
    public void onRoadGuideTextUpdate(CharSequence charSequence, CharSequence charSequence1) {
        Toast.makeText(BNaviGuideActivity.this, "OnRoadGuideTextUpdate - sequence 1 : " + charSequence + " sequence 2 : " + charSequence1 , Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onRemainDistanceUpdate(CharSequence charSequence) {

    }

    @Override
    public void onRemainTimeUpdate(CharSequence charSequence) {

    }

    @Override
    public void onGpsStatusChange(CharSequence charSequence, Drawable drawable) {

    }

    @Override
    public void onRouteFarAway(CharSequence charSequence, Drawable drawable) {

    }

    @Override
    public void onRoutePlanYawing(CharSequence charSequence, Drawable drawable) {

    }

    @Override
    public void onReRouteComplete() {
        if (location != null) {
            param.stPt(new LatLng(location.getLatitude(), location.getLongitude()));
            Log.i(TAG, "ReRoute Completed, new start position : " + param.getStartPt().toString());
        } else
            Log.i(TAG, "ReRoute Completed, without location set");
    }

    @Override
    public void onArriveDest() {

    }

    @Override
    public void onVibrate() {

    }

    @Override
    public void onGetRouteDetailInfo(BikeRouteDetailInfo bikeRouteDetailInfo) {
    }
    //endregion

}
