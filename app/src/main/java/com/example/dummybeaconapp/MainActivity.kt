package com.example.dummybeaconapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kkmcn.kbeaconlib2.*
import com.kkmcn.kbeaconlib2.KBAdvPackage.*
import com.kkmcn.kbeaconlib2.KBeacon.ConnStateDelegate
import java.util.*


class MainActivity : AppCompatActivity(), KBeaconsMgr.KBeaconMgrDelegate, ConnStateDelegate{

    private lateinit var mBeaconsMgr: KBeaconsMgr
    private val REQUEST_BLUETOOTH_PERMISSIONS = 1
    private var mScanFailedContinueNum = 0
    private val mBeaconsDictory: HashMap<String, KBeacon>? = null
    private val MAX_ERROR_SCAN_NUMBER = 2
    private lateinit var mBeaconsArray: Array<KBeacon>
    lateinit var textView : TextView
    private val DEFAULT_PASSWORD = "0000000000000000"
    private var isBlinking = false
    private var mBeacon: KBeacon? = null
    private val PERMISSION_CONNECT = 20
    private val READ_DEFAULT_PARAMETERS = true


    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val button : Button = findViewById(R.id.btn)
        val button2 : Button = findViewById(R.id.btn2)
        val textView : TextView = findViewById(R.id.hello)
        mBeaconsMgr = KBeaconsMgr.sharedBeaconManager(this)
        mBeacon = mBeaconsMgr.getBeacon("DC:0D:30:11:EE:47")

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

    private fun check2RequestPermission(): Boolean {
        var bHasPermission = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    PERMISSION_CONNECT)
                bHasPermission = false
            }
        }
        return bHasPermission
    }


    private fun stopBlinking() {
        val textView : TextView = findViewById(R.id.hello)
       // textView.visibility = View.INVISIBLE
        textView.clearAnimation()
        isBlinking = false
    }
    @SuppressLint("ResourceAsColor")
    override fun onBeaconDiscovered(beacons: Array<out KBeacon>?) {

        val button : Button = findViewById(R.id.btn)
        val button2 : Button = findViewById(R.id.btn2)
        var textView : TextView = findViewById(R.id.hello)
        mBeaconsMgr.stopScanning()
        textView.text = "BEACON DISCOVERED"
        textView.setTextColor(R.color.green)
        button2.setOnClickListener {
            Log.d(TAG,"buttonclicked")
            if(READ_DEFAULT_PARAMETERS) {
                    //connect to device with default parameters
                    if (check2RequestPermission()) {
                        mBeacon = mBeaconsMgr.getBeacon("DC:0D:30:11:EE:47")
                        mBeacon!!.connect(DEFAULT_PASSWORD,20 * 1000, this)
                    }
                } else {
                //connect to device with specified parameters
                //When the app is connected to the KBeacon device, the app can specify which the configuration parameters to be read,
                //The parameter that can be read include: common parameters, advertisement parameters, trigger parameters, and sensor parameters
                val connPara = KBConnPara()
                connPara.syncUtcTime = true
                connPara.readCommPara = true
                connPara.readSlotPara = true
                connPara.readTriggerPara = false
                connPara.readSensorPara = false
                mBeacon!!.connectEnhanced(
                    DEFAULT_PASSWORD, 20 * 1000,
                    connPara,
                    this
                )
            }
        }

        stopBlinking()


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
        KBConnState.Connected
        if (state == KBConnState.Connected) {
            Log.v(TAG, "device has connected")
            invalidateOptionsMenu()
            textView.text = "Connected"
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
        }
    }
}