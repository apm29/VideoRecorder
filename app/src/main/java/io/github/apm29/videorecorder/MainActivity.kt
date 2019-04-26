package io.github.apm29.videorecorder

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Camera
import android.media.CamcorderProfile
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.view.SurfaceHolder
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val storage = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val camera =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audio =
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        if (!storage || !camera || !audio) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO),
                1023
            )
        } else {
            init()
        }


    }

    private fun init() {

        surface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
            }

            override fun surfaceCreated(holder: SurfaceHolder?) {
                startPreviewInit(holder)
            }
        })
        surface.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    private var camera: Camera? = null

    private var mediaRecorder: MediaRecorder? = null
    private var initialized = false
    private fun startPreviewInit(holder: SurfaceHolder?) {
        val camera = Camera.open()
        initCameraParam()
//        camera?.setPreviewDisplay(holder)
//        camera?.setPreviewCallback { data, _ ->
//            println("data = [${data}]")
//        }

        mediaRecorder = MediaRecorder()
        val path = startRecord(camera, holder)
        var startTime = System.currentTimeMillis()
        btn_capture.isEnabled =true
        btn_capture.setOnClickListener {
            stopRecord()
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val time = (System.currentTimeMillis() - startTime) * 1000-100
            println(time)
            val bitmap = retriever.getFrameAtTime(time)
            image.setImageBitmap(bitmap)
            startRecord(camera,holder)
            startTime = System.currentTimeMillis()
        }
    }

    private fun startRecord(camera: Camera?, holder: SurfaceHolder?): String {
        camera?.unlock()
        mediaRecorder?.setCamera(camera)

        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
        mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.CAMERA)


        val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P)
        mediaRecorder?.setProfile(profile)

        val path =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath + "/record.mp4"
        mediaRecorder?.setOutputFile(path)
        mediaRecorder?.setPreviewDisplay(holder?.surface)
        mediaRecorder?.setOrientationHint(90)
        mediaRecorder?.prepare()
        initialized = true
        mediaRecorder?.start()
        return path
    }

    private fun initCameraParam() {
        camera?.apply {
            //parameters.setPreviewSize(640, 480)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1023 && grantResults.all {
                it == PackageManager.PERMISSION_GRANTED
            }) {
            init()
        } else {
            Toast.makeText(this, "权限被拒", Toast.LENGTH_LONG).show()

            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        stopRecord()
    }

    private fun stopRecord() {
        mediaRecorder?.reset()
        mediaRecorder?.release()
        camera?.lock()
        camera?.release()
        initialized = false
        btn_capture.isEnabled = false
    }

    override fun onDestroy() {
        super.onDestroy()
        camera?.release()
    }
}
