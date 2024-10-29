package easy_a.controllers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import easy_a.application.R
import easy_a.models.QuestionPaperResponse
import easy_a.models.QuestionResponse
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class QuestionAddFragment : Fragment() {

    private lateinit var backButton: ImageButton
    private lateinit var progressImage: ImageButton

    private lateinit var username: TextView
    private lateinit var addQuestion: Button
    private lateinit var paperName: TextView
    private lateinit var sessionManager: SessionManager

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>

    private val CAMERA_PERMISSION_REQUEST_CODE = 101

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.question_add_fragment, container, false)

        username = view.findViewById(R.id.username)

        sessionManager = SessionManager(requireContext())

        paperName = view.findViewById(R.id.paperName)

        val firstName = sessionManager.getFirstName() ?: ""
        val lastName = sessionManager.getLastName() ?: ""
        val fullName = "$firstName $lastName"
        val email = sessionManager.getEmail() ?: ""

        username.text = if (firstName.isEmpty()) email else fullName

        initImagePickers()

        readQuestionPaper()

        val title = view.findViewById<TextView>(R.id.titleTextView)
        val name = view.findViewById<TextView>(R.id.nameTextView)
        val description = view.findViewById<TextView>(R.id.descriptionTextView)

        val questionNumber = view.findViewById<EditText>(R.id.questionNumber)
        val questionDescription = view.findViewById<EditText>(R.id.questionDescription)
        progressImage = view.findViewById(R.id.imageAddProgress)

        if (!sessionManager.isDarkMode()) {
            // Set all text elements to black for light mode
            title.setTextColor(resources.getColor(R.color.black))
            name.setTextColor(resources.getColor(R.color.black))
            description.setTextColor(resources.getColor(R.color.black))
            paperName.setTextColor(resources.getColor(R.color.black))
            questionNumber.background = resources.getDrawable(R.drawable.textfield_light)
            questionDescription.background = resources.getDrawable(R.drawable.textfield_light)
            progressImage.background = resources.getDrawable(R.drawable.textfield_light)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backButton = view.findViewById(R.id.btnBack)
        backButton.setOnClickListener {
            btnBackClicked(this)
        }

        addQuestion = view.findViewById(R.id.btnAddQuestion)
        addQuestion.setOnClickListener {
            btnAddQuestion(this)
        }

        progressImage = view.findViewById(R.id.imageAddProgress)
        progressImage.setOnClickListener {
            imageAddProgressClicked()
        }
    }

    private fun btnAddQuestion(fragment: Fragment) {
        val questionNumber = view?.findViewById<EditText>(R.id.questionNumber)?.text.toString()
        val questionDescription = view?.findViewById<EditText>(R.id.questionDescription)?.text.toString()
        val uid = sessionManager.getUid()
        val questionPaperId = sessionManager.getQuestionPaperId()

        // Validate inputs
        if (questionNumber.isEmpty() || questionDescription.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Prepare the request body
        val requestBodyUid = RequestBody.create("text/plain".toMediaTypeOrNull(), uid!!)
        val requestBodyQid = RequestBody.create("text/plain".toMediaTypeOrNull(), questionPaperId!!)
        val requestBodyQuestionNumber = RequestBody.create("text/plain".toMediaTypeOrNull(), questionNumber)
        val requestBodyQuestionDescription = RequestBody.create("text/plain".toMediaTypeOrNull(), questionDescription)

        // Prepare the optional image file if needed
        var imageFile: MultipartBody.Part? = null
        val drawable = progressImage.drawable

        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            // Save the bitmap as a file
            val imageFilePath = saveImageToFile(bitmap, questionNumber)

            // Create RequestBody for the image file
            val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageFilePath)

            // Create MultipartBody.Part using the file request body
            imageFile = MultipartBody.Part.createFormData("questionImage", "$questionNumber.jpg", requestFile)
        }

        // Make the API call
        RetrofitClient.apiService.createQuestion(
            requestBodyUid,
            requestBodyQid,
            requestBodyQuestionNumber,
            requestBodyQuestionDescription,
            imageFile
        ).enqueue(object : Callback<QuestionResponse> {
            override fun onResponse(call: Call<QuestionResponse>, response: Response<QuestionResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Question added successfully!", Toast.LENGTH_SHORT).show()
                    navigateToFragment(QuestionListFragment())
                } else {
                    Log.e("QuestionAddFragment", "Error: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "Failed to add question: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<QuestionResponse>, t: Throwable) {
                Log.e("QuestionAddFragment", "API call failed: ${t.message}", t)
                Toast.makeText(requireContext(), "API call failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveImageToFile(bitmap: Bitmap, questionNumber: String): File {
        // Create a temporary file in the cache directory
        val file = File(requireContext().cacheDir, "$questionNumber.jpg")
        val outStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
        outStream.flush()
        outStream.close()
        return file
    }

    private fun imageAddProgressClicked()
    {
        openImagePicker()
    }

    private fun openImagePicker()
    {
        if (hasCameraPermission())
        {
            showImagePickerDialog()
        } else
        {
            requestCameraPermission()
        }
    }

    private fun hasCameraPermission(): Boolean
    {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission()
    {
        ActivityCompat.requestPermissions(
            requireActivity(), arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    private fun showImagePickerDialog()
    {
        AlertDialog.Builder(requireContext()).setTitle("Profile Picture")
            .setMessage("Select an option:").setPositiveButton("Take Photo") { _, _ ->
                // Launch camera app to take a photo
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                try
                {
                    if (takePictureIntent.resolveActivity(requireContext().packageManager) != null)
                    {
                        takePictureLauncher.launch(takePictureIntent)
                    }
                } catch (e: ActivityNotFoundException)
                {
                    // Handle the ActivityNotFoundException here
                }
            }.setNegativeButton("Choose from Gallery") { _, _ ->
                // Launch gallery to choose a photo
                pickImageLauncher.launch("image/*")
            }.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    )
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE)
        {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                showImagePickerDialog()
            }
        }
    }

    private fun initImagePickers()
    {
        // Initialize the launcher for picking images
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let { imageUri ->
                    // Set the chosen image as profile picture
                    setProgressPicture(imageUri)
                }
            }

        // Initialize the launcher for taking pictures
        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK)
                {
                    val imageBitmap = result.data?.extras?.get("data") as Bitmap
                    setProgressPictureFromBitmap(imageBitmap)
                }
            }
    }

    private fun setProgressPicture(imageUri: Uri)
    {
        progressImage.visibility = View.VISIBLE
        progressImage.setImageURI(imageUri)
        progressImage.scaleType = ImageView.ScaleType.FIT_CENTER
    }

    private fun setProgressPictureFromBitmap(imageBitmap: Bitmap)
    {
        // Set the captured image as profile picture
        progressImage.setImageBitmap(imageBitmap)
        progressImage.visibility = View.VISIBLE
        progressImage.scaleType = ImageView.ScaleType.FIT_CENTER
    }

    private fun readQuestionPaper()
    {
        val uid = sessionManager.getUid()
        val questionPaperId = sessionManager.getQuestionPaperId()


        RetrofitClient.apiService.getQuestionPaper(uid!!, questionPaperId!!).enqueue(
            object : Callback<QuestionPaperResponse> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onResponse(
                    call: Call<QuestionPaperResponse>,
                    response: Response<QuestionPaperResponse>
                ) {
                    if (response.isSuccessful) {
                        paperName.text = response.body()?.questionPaperName.toString()
                        Log.e("StudyListFragment", "Name: ${response.body()?.questionPaperName.toString()}")
                    } else {
                        // Log the error if the response isn't successful
                        Log.e("StudyListFragment", "Error: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<QuestionPaperResponse>, t: Throwable) {
                    // Log the error when the call fails
                    Log.e("StudyListFragment", "API call failed: ${t.message}", t)
                }
            }
        )
    }

    private fun btnBackClicked(fragment: Fragment) {
        navigateToFragment(QuestionListFragment())
    }

    private fun navigateToFragment(fragment: Fragment)
    {
        // Replace the current fragment with the new fragment
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }
}