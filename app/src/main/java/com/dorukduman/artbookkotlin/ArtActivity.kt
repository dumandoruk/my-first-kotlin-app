package com.dorukduman.artbookkotlin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dorukduman.artbookkotlin.databinding.ActivityArtBinding
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.graphics.ImageDecoder
import android.graphics.ImageDecoder.decodeBitmap
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream

class ArtActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArtBinding

    // 1. Galeriye gidip resmi alacak Launcher
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    // 2. İzni isteyecek Launcher
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    // Seçilen resmin URI'sini tutmak için (İsteğe bağlı)
    var selectedPicture: Uri? = null
    var selectedBitmap: Bitmap? = null
    private lateinit var database: SQLiteDatabase



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityArtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        database=this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)

        registerLauncher()

        val intent=intent
        val info=intent.getStringExtra("info")
        if(info.equals("new")) {
            binding.artNameText.setText("")
            binding.artistNameText.setText("")
            binding.artYearText.setText("")
            binding.saveButton.visibility= View.VISIBLE
            binding.selectImageView.setImageResource(R.drawable.select_image)
        } else{
            binding.saveButton.visibility=View.INVISIBLE
            val selectedId=intent.getIntExtra("id",1)

            val cursor=database.rawQuery("SELECT * FROM arts WHERE id=?", arrayOf(selectedId.toString()))

            val artNameIx=cursor.getColumnIndex("artname")
            val artistNameIx=cursor.getColumnIndex("artistname")
            val yearIx=cursor.getColumnIndex("year")
            val imageIx=cursor.getColumnIndex("image")

            while(cursor.moveToNext()){
                binding.artNameText.setText(cursor.getString(artNameIx))
                binding.artistNameText.setText(cursor.getString(artistNameIx))
                binding.artYearText.setText(cursor.getString(yearIx))

                val byteArray=cursor.getBlob(imageIx)
                val bitmap= BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.selectImageView.setImageBitmap(bitmap)
            }
            cursor.close()
        }

    }

    fun saveButtonClicked(view: View) {

        val artName = binding.artNameText.text.toString()
        val artistName = binding.artistNameText.text.toString()
        val year = binding.artYearText.text.toString()

        if (selectedBitmap != null) {
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!, 300)

            val outputBitmap = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputBitmap)
            val byteArray = outputBitmap.toByteArray()

            try {
               //val database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null)
                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, artistname VARCHAR, year VARCHAR, image BLOB)")

                val sqlString =
                    "INSERT INTO arts (artname, artistname, year, image) VALUES (?, ?, ?, ?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1, artName)
                statement.bindString(2, artistName)
                statement.bindString(3, year)
                statement.bindBlob(4, byteArray)
                statement.execute()

            } catch (e: Exception) {
                e.printStackTrace()
            }

            val intent = Intent(this@ArtActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)

        }
    }

    private fun makeSmallerBitmap(image: Bitmap, maximumSize: Int): Bitmap {
        var width = image.width
        var height = image.height

        val bitmapRatio: Double = width.toDouble() / height.toDouble()
        if (bitmapRatio > 1) {
            // landscape
            width = maximumSize
            val scaledHeight = width / bitmapRatio
            height = scaledHeight.toInt()
        } else {
            // portrait
            height = maximumSize
            val scaledWidth = height * bitmapRatio
            width = scaledWidth.toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    fun selectImage(view: View) {
        // Android sürümüne göre hangi izni isteyeceğimizi belirliyoruz
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Give Permission")
                    {
                        permissionLauncher.launch(permission)
                    }.show()
            } else {
                permissionLauncher.launch(permission)
            }
        } else {
            val intentToGallery =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)
        }
    }


    private fun registerLauncher() {
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val intentFromResult = result.data
                    if (intentFromResult != null) {
                        // It seems you intended to use intentFromResult.data here
                        val imageData: Uri? = intentFromResult.data
                        if (imageData != null) {
                            try {
                                if (Build.VERSION.SDK_INT >= 28) {
                                    // 1. Create the source from the URI
                                    val source = ImageDecoder.createSource(
                                        this@ArtActivity.contentResolver,
                                        imageData
                                    )
                                    // 2. Decode the source into a bitmap
                                    selectedBitmap = ImageDecoder.decodeBitmap(source)
                                    binding.selectImageView.setImageBitmap(selectedBitmap)
                                } else {
                                    // Use the older, deprecated method for APIs below 28
                                    selectedBitmap = MediaStore.Images.Media.getBitmap(
                                        this.contentResolver,
                                        imageData
                                    )
                                    binding.selectImageView.setImageBitmap(selectedBitmap)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your app.
                    val intentToGallery =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    activityResultLauncher.launch(intentToGallery)
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied.
                    Toast.makeText(
                        this,
                        "Permission denied, gallery access is required to select an image.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
