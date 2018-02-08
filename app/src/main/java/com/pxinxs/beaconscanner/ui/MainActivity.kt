package com.pxinxs.beaconscanner.ui

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.view.View
import android.view.animation.LinearInterpolator
import com.kontakt.sdk.android.ble.configuration.ActivityCheckConfiguration
import com.kontakt.sdk.android.ble.configuration.ForceScanConfiguration
import com.kontakt.sdk.android.ble.configuration.ScanMode
import com.kontakt.sdk.android.ble.configuration.ScanPeriod
import com.kontakt.sdk.android.ble.manager.ProximityManager
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener
import com.kontakt.sdk.android.common.KontaktSDK
import com.kontakt.sdk.android.common.profile.IBeaconRegion
import com.kontakt.sdk.android.common.profile.IBeaconDevice
import com.kontakt.sdk.android.common.profile.IEddystoneNamespace
import com.kontakt.sdk.android.common.profile.IEddystoneDevice
import com.kontakt.sdk.android.ble.manager.listeners.EddystoneListener
import com.kontakt.sdk.android.common.profile.ISecureProfile
import com.kontakt.sdk.android.ble.manager.listeners.SecureProfileListener
import com.kontakt.sdk.android.ble.exception.ScanError
import com.kontakt.sdk.android.ble.manager.listeners.ScanStatusListener
import com.kontakt.sdk.android.ble.rssi.RssiCalculators
import com.kontakt.sdk.android.ble.spec.EddystoneFrameType
import com.pxinxs.beaconscanner.R
import com.pxinxs.beaconscanner.utils.toast
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener, View.OnClickListener {

    companion object {
        private const val COARSE_LOCATION = 100
        private const val BLUETOOTH = 200

        private const val API_KEY = "your API key here"    // TODO: your API key here
    }

    var proximityManager: ProximityManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        KontaktSDK.initialize(API_KEY)

        proximityManager = ProximityManagerFactory.create(this)
        proximityManager?.setIBeaconListener(createIBeaconListener())
        proximityManager?.setEddystoneListener(createEddystoneListener())
//        proximityManager?.setSecureProfileListener(createSecureProfileListener())   // for Beacon Pro
        proximityManager?.setScanStatusListener(createScanStatusListener())
//        setConfiguration()

        //
        // you can set here Eddystone Filters/IBeacon Filters
        //

        srlBeacons.setOnRefreshListener(this)
        srlBeacons.setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.CYAN)

        fabStopScan.setOnClickListener(this)
        fabStopScan.animate().translationY(0f).setInterpolator(LinearInterpolator()).start()    // hide fab
    }

    override fun onStart() {
        super.onStart()
        checkPermission()
    }

    override fun onClick(view: View?) {
        when (view) {
            fabStopScan -> {
                proximityManager?.stopScanning()
                scanStopped()
            }
        }
    }

    override fun onRefresh() {
        checkPermission()
    }

    private fun checkPermission() {
        val checkSelfPermissionResult = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (PackageManager.PERMISSION_GRANTED == checkSelfPermissionResult) {
            // already granted
            checkBluetoothEnabledAndStart()
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // we should show some explanation for user here
                showAlertDialogNeedPermission()
            } else {
                // request permission
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), COARSE_LOCATION)
            }
        }
    }

    private fun checkBluetoothEnabledAndStart() {
        if (BluetoothAdapter.getDefaultAdapter().isEnabled) {
            startScanning()
        } else {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), BLUETOOTH)
        }
    }

    private fun startScanning() {
        proximityManager?.connect { proximityManager?.startScanning() }
    }

    private fun createIBeaconListener(): IBeaconListener {
        return object : IBeaconListener {
            override fun onIBeaconDiscovered(iBeacon: IBeaconDevice, region: IBeaconRegion) {
                // device has been discovered (1 device, 1 space)
            }

            override fun onIBeaconsUpdated(iBeacons: List<IBeaconDevice>, region: IBeaconRegion) {
                // discovered devices have been updated (n devices, 1 space)
            }

            override fun onIBeaconLost(iBeacon: IBeaconDevice, region: IBeaconRegion) {
                // earlier discovered device has been lost (is considered inactive)
            }
        }
    }

    private fun createEddystoneListener(): EddystoneListener {
        return object : EddystoneListener {
            override fun onEddystoneDiscovered(eddystone: IEddystoneDevice?, namespace: IEddystoneNamespace?) {
                // device has been discovered (1 device, 1 space)
            }

            override fun onEddystonesUpdated(eddystones: MutableList<IEddystoneDevice>?, namespace: IEddystoneNamespace?) {
                // discovered devices have been updated (n devices, 1 space)
            }

            override fun onEddystoneLost(eddystone: IEddystoneDevice?, namespace: IEddystoneNamespace?) {
                // earlier discovered device has been lost (is considered inactive)
            }
        }
    }

    private fun createSecureProfileListener(): SecureProfileListener {
        return object : SecureProfileListener {
            override fun onProfileDiscovered(profile: ISecureProfile) {
                //Profile discovered
            }

            override fun onProfilesUpdated(profiles: List<ISecureProfile>) {
                //Profiles updated
            }

            override fun onProfileLost(profile: ISecureProfile) {
                //Profile lost
            }
        }
    }

    private fun setConfiguration() {
        proximityManager?.configuration()
                ?.scanMode(ScanMode.BALANCED)
                ?.scanPeriod(ScanPeriod.RANGING)
                ?.activityCheckConfiguration(ActivityCheckConfiguration.DISABLED)
                ?.forceScanConfiguration(ForceScanConfiguration.DISABLED)
                ?.deviceUpdateCallbackInterval(TimeUnit.SECONDS.toMillis(5))
                ?.rssiCalculator(RssiCalculators.DEFAULT)
                ?.cacheFileName("Example")
                ?.resolveShuffledInterval(3)
                ?.monitoringEnabled(true)
                ?.monitoringSyncInterval(10)
                ?.eddystoneFrameTypes(Arrays.asList(EddystoneFrameType.UID, EddystoneFrameType.URL))
    }

    private fun createScanStatusListener(): ScanStatusListener {
        return object : ScanStatusListener {
            override fun onScanStart() {
                scanStarted()
            }

            override fun onScanStop() {
                scanStopped()
            }

            override fun onScanError(error: ScanError) {
                toast("Error occurred")
            }

            override fun onMonitoringCycleStart() {
                toast("Monitoring cycle started")
            }

            override fun onMonitoringCycleStop() {
                toast("Monitoring cycle finished")
            }
        }
    }

    private fun scanStarted() {
        srlBeacons.isRefreshing = true
        fabStopScan.visibility = View.VISIBLE
        tvSwipeInfo.text = getString(R.string.click_stop_to_cancel_scan)

        // show fab
        fabStopScan.animate().translationY(0f).setInterpolator(LinearInterpolator()).start()
    }

    private fun scanStopped() {
        srlBeacons.isRefreshing = false
        tvSwipeInfo.text = getString(R.string.swipe_down_to_scan)

        // hide fab
        val bottomMargin = (fabStopScan.layoutParams as CoordinatorLayout.LayoutParams).bottomMargin
        fabStopScan.animate().translationY(fabStopScan.height + bottomMargin.toFloat()).setInterpolator(LinearInterpolator()).start()
    }

    private fun showAlertDialogNeedPermission() {
        AlertDialog.Builder(this)
                .setTitle("This app needs location access")
                .setMessage("Please grant location access so this app can detect beacons.")
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), COARSE_LOCATION)
                }
                .show()
    }

    private fun showSnackBar(message: String, color: Int = R.color.dark_blue) {
        val snackBar = Snackbar.make(tvSwipeInfo, message, Snackbar.LENGTH_SHORT)
        snackBar.view.setBackgroundColor(ContextCompat.getColor(this, color))
        snackBar.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == COARSE_LOCATION) {
                checkPermission()
            }
        } else {
            srlBeacons.isRefreshing = false
            showSnackBar(getString(R.string.permission_not_granted), R.color.red)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == BLUETOOTH) {
                checkPermission()
            }
        } else {
            srlBeacons.isRefreshing = false
            showSnackBar(getString(R.string.permission_not_granted), R.color.red)
        }
    }

    override fun onStop() {
        proximityManager?.stopScanning()
        super.onStop()
    }

    override fun onDestroy() {
        proximityManager?.disconnect()
        proximityManager = null
        super.onDestroy()
    }
}
