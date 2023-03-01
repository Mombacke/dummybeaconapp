package com.example.dummybeaconapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kkmcn.kbeaconlib2.KBAdvPackage.*
import com.kkmcn.kbeaconlib2.KBConnState
import com.kkmcn.kbeaconlib2.KBConnectionEvent
import com.kkmcn.kbeaconlib2.KBeacon
import com.kkmcn.kbeaconlib2.KBeacon.ConnStateDelegate
import com.kkmcn.kbeaconlib2.KBeaconsMgr
import java.util.*


class MainActivity : AppCompatActivity(), KBeaconsMgr.KBeaconMgrDelegate, ConnStateDelegate{

    private lateinit var mBeaconsMgr: KBeaconsMgr
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1
    private var mScanFailedContinueNum = 0
    private val mBeaconsDictory: HashMap<String, KBeacon>? = null
    private val MAX_ERROR_SCAN_NUMBER = 2
    private lateinit var mBeaconsArray: Array<KBeacon>
    lateinit var textView : TextView
    private var isBlinking = false
    private val mBeacon: KBeacon? = null


    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val button : Button = findViewById(R.id.btn)
        val button2 : Button = findViewById(R.id.btn2)
        val textView : TextView = findViewById(R.id.hello)
        mBeaconsMgr = KBeaconsMgr.sharedBeaconManager(this)
        if (mBeaconsMgr == null) {
            Toast.makeText(this,"Make sure the phone supports BLE function",Toast.LENGTH_SHORT).show()
            return
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "PERMISSION NOT GRANTED")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                REQUEST_BLUETOOTH_PERMISSIONS
            )
        }else {
            if (mBeacon?.state == KBConnState.Connected){
                textView.text = "Connected"
            }
            button.setOnClickListener {
                mBeaconsMgr.delegate = this
                val nStartScan = mBeaconsMgr.startScanning()

                if (nStartScan == 0) {
                    Log.v(TAG, "start scan success")
                    textView.text = "Scanning"
                    textView.setTextColor(R.color.purple_200)
                    textView.startAnimation(AnimationUtils.loadAnimation(this,R.anim.blink))
                } else if (nStartScan == KBeaconsMgr.SCAN_ERROR_BLE_NOT_ENABLE) {
                    Toast.makeText(this, "BLE function is not enabled", Toast.LENGTH_LONG).show()
                } else if (nStartScan == KBeaconsMgr.SCAN_ERROR_NO_PERMISSION) {
                    Toast.makeText(
                        this,
                        "BLE scanning has no location permission",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(this, "BLE scanning unknown error", Toast.LENGTH_LONG).show()
                }
            }
        }

    }

    private fun stopBlinking() {
        val textView : TextView = findViewById(R.id.hello)
       // textView.visibility = View.INVISIBLE
        textView.clearAnimation()
        isBlinking = false
    }

    private val beaconMgrExample: KBeaconsMgr.KBeaconMgrDelegate =
        object : KBeaconsMgr.KBeaconMgrDelegate {
            // Get advertisement packet during scanning callback
            override fun onBeaconDiscovered(beacons: Array<out KBeacon>?) {
                stopBlinking()
                if (beacons != null) {
                    for (beacon in beacons) {
                        // Get beacon adv common info
                        Log.v(TAG, "beacon mac:" + beacon.mac)
                        Log.v(TAG, "beacon name:" + beacon.name)
                        Log.v(TAG, "beacon rssi:" + beacon.rssi)

                        for (advPacket in beacon.allAdvPackets()) {
                            when (advPacket.advType) {
                                KBAdvType.IBeacon -> {
                                    val advIBeacon = advPacket as KBAdvPacketIBeacon
                                    Log.v(TAG,"iBeacon uuid:${advIBeacon.uuid}")
                                    Log.v(TAG,"iBeacon major:${advIBeacon.majorID}")
                                    Log.v(TAG,"iBeacon minor:${advIBeacon.minorID}")
                                }

                                KBAdvType.EddyTLM -> {
                                    val advTLM = advPacket as KBAdvPacketEddyTLM
                                    Log.v(TAG,"TLM battery:${advTLM.batteryLevel}")
                                    Log.v(TAG,"TLM Temperature:${advTLM.temperature}")
                                    Log.v(TAG,"TLM adv count:${advTLM.advCount}")
                                }

                                KBAdvType.Sensor -> {
                                    val advSensor = advPacket as KBAdvPacketSensor
                                    Log.v(TAG, "Device battery:${advSensor.batteryLevel}")
                                    Log.v(TAG, "Device temp:${advSensor.temperature}")

                                    //device that has acc sensor
                                    val accPos = advSensor.accSensor
                                    if (accPos != null) {
                                        val strAccValue = String.format(
                                            Locale.ENGLISH, "x:%d; y:%d; z:%d",
                                            accPos.xAis, accPos.yAis, accPos.zAis)
                                        Log.v(TAG, "Sensor Acc:$strAccValue")
                                    }

                                    //device that has humidity sensor
                                    advSensor.humidity?.let {
                                        Log.v(TAG, "Sensor humidity:$it")
                                    }

                                    //device that has cutoff sensor
                                    advSensor.watchCutoff?.let {
                                        Log.v(TAG, "cutoff flag:$it")
                                    }

                                    //device that has PIR sensor
                                    advSensor.pirIndication?.let {
                                        Log.v(TAG, "pir indication:$it")
                                    }

                                    //device that has light sensor
                                    advSensor.luxValue?.let {
                                        Log.v(TAG, "light level:$it")
                                    }
                                }

                                KBAdvType.EddyUID -> {
                                    val advUID = advPacket as KBAdvPacketEddyUID
                                    Log.v(TAG,"UID Nid:${advUID.nid}")
                                    Log.v(TAG,"UID Sid:${advUID.sid}")
                                }

                                KBAdvType.EddyURL -> {
                                    val advURL = advPacket as KBAdvPacketEddyURL
                                    Log.v(TAG,"URL:${advURL.url}")
                                }

                                KBAdvType.System -> {
                                    val advSystem = advPacket as KBAdvPacketSystem
                                    Log.v(TAG,"System mac:${advSystem.macAddress}")
                                    Log.v(TAG,"System model:${advSystem.model}")
                                    Log.v(TAG,"System batt:${advSystem.batteryPercent}")
                                    Log.v(TAG,"System ver:${advSystem.version}")
                                }

                                else -> {}
                            }
                        }

                        //clear all scanned packet
                        beacon.removeAdvPacket()
                    }
                }

                }

            override fun onCentralBleStateChang(nNewState: Int) {
                if (nNewState == KBeaconsMgr.BLEStatePowerOff) {
                    Log.e(
                        TAG,
                        "BLE function is power off"
                    )
                } else if (nNewState == KBeaconsMgr.BLEStatePowerOn) {
                    Log.e(
                        TAG,
                        "BLE function is power on"
                    )
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(
                   TAG,
                    "Start N scan failed：$errorCode"
                )
                if (mScanFailedContinueNum >= MAX_ERROR_SCAN_NUMBER) {
                   // Toast.makeText(this,"BLE function is not enabled",Toast.LENGTH_LONG).show()
                    Log.d(TAG,"BLE function is not enabled")
                }
                mScanFailedContinueNum++
            }
        }


    @SuppressLint("ResourceAsColor")
    override fun onBeaconDiscovered(beacons: Array<out KBeacon>?) {

        val button : Button = findViewById(R.id.btn)
        val button2 : Button = findViewById(R.id.btn2)
        var textView : TextView = findViewById(R.id.hello)
        mBeaconsMgr.stopScanning()
        textView.text = "BEACON DISCOVERED"
        textView.setTextColor(R.color.green)

        stopBlinking()

        button2.setOnClickListener {
            mBeacon?.connect("0000000000000000",20 * 1000, this)
        }


        Log.d(TAG,"beacon discovered")
        for (pBeacons in beacons!!) {
            mBeaconsDictory?.put(pBeacons.mac, pBeacons)
            Log.d(TAG,"MAC ADDRESS : " + pBeacons.mac )

        }
    /*    if (mBeaconsDictory?.size!! > 0) {
            mBeaconsArray = Array<KBeacon>(mBeaconsDictory.size) { index ->
                mBeaconsDictory.values.elementAt(
                    index
                )
            }
            mBeaconsDictory.values.toList().toTypedArray<KBeacon>().copyInto(mBeaconsArray)


     */
            //mDevListAdapter.notifyDataSetChanged()
        }


    override fun onCentralBleStateChang(nNewState: Int) {
        Log.e(
            TAG,
            "centralBleStateChang：$nNewState"
        )
    }

    override fun onScanFailed(errorCode: Int) {
        Log.e(
            TAG,
            "Start N scan failed：$errorCode"
        )
        if (mScanFailedContinueNum >= MAX_ERROR_SCAN_NUMBER) {
            Log.d(TAG,"scan encount error, error time:$mScanFailedContinueNum")
        }
        mScanFailedContinueNum++
    }
    private var nDeviceConnState = KBConnState.Disconnected

    override fun onConnStateChange(beacon: KBeacon?, state: KBConnState?, nReason: Int) {
        val button2 : Button = findViewById(R.id.btn2)
        var textView : TextView = findViewById(R.id.hello)
        if (state == KBConnState.Connected) {
            Log.v(TAG, "device has connected")
            invalidateOptionsMenu()
            textView.text = "Connected"
            button2.setOnClickListener {
                mBeacon?.disconnect()
            }
            //updateDeviceToView()
            nDeviceConnState = state
        } else if (state == KBConnState.Connecting) {
            textView.text = "Connecting"
            Log.v(TAG, "device start connecting")
            invalidateOptionsMenu()
            nDeviceConnState = state
        } else if (state == KBConnState.Disconnecting) {
            Log.e(
                TAG,
                "connection error, now disconnecting"
            )
        } else {
            if (nDeviceConnState == KBConnState.Connecting) {
                if (nReason == KBConnectionEvent.ConnAuthFail) {
                    val inputServer = EditText(this)
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(getString(R.string.auth_error_title))
                    builder.setView(inputServer)
                    builder.setNegativeButton(R.string.Dialog_Cancel, null)
                    builder.setPositiveButton(R.string.Dialog_OK, null)
                    val alertDialog = builder.create()
                    alertDialog.show()
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val strNewPassword = inputServer.text.toString().trim { it <= ' ' }
                        if (strNewPassword.length < 8 || strNewPassword.length > 16) {
                            Toast.makeText(
                                this@MainActivity,
                                R.string.connect_error_auth_format,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            alertDialog.dismiss()
                        }
                    }
                } else {
                    Toast.makeText(this@MainActivity,"connect to device failed, reason:$nReason",Toast.LENGTH_SHORT).show()
                }
            }
          //  button2.setEnabled(false)
            Log.e(
                TAG,
                "device has disconnected:$nReason"
            )
            invalidateOptionsMenu()
        }    }
}