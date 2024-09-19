package com.example.proactive_opsc7311_poe.controllers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.proactive_opsc7311_poe.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddExerciseFragment : Fragment()
{
    private val db = Firebase.firestore

    private lateinit var backButton: ImageButton
    private lateinit var progressImage: ImageButton
    private lateinit var workoutNameTitle: TextView
    private lateinit var createExercise: Button

    private lateinit var exerciseName: EditText
    private lateinit var exerciseDescription: EditText
    private lateinit var exerciseDateChooser: Button
    private lateinit var exerciseStartTimeChooser: Button
    private lateinit var exerciseEndTimeChooser: Button
    private lateinit var exerciseMinDailyGoal: EditText
    private lateinit var exerciseMaxDailyGoal: EditText
    private lateinit var exerciseCategory: Spinner

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>

    private lateinit var selectedDate: Date
    private lateinit var selectedStartTime: Calendar
    private lateinit var selectedEndTime: Calendar

    private var workoutID = ""
    private var imagePath = ""
    private val CAMERA_PERMISSION_REQUEST_CODE = 101

    private lateinit var helpButton: Button


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View?
    {
        val view = inflater.inflate(R.layout.add_exercise_fragment, container, false)

        workoutID = arguments?.getString("workout_id") ?: ""

        workoutNameTitle = view.findViewById(R.id.workoutNameTitle)

        exerciseName = view.findViewById(R.id.exerciseName)
        exerciseDescription = view.findViewById(R.id.exerciseDescription)
        exerciseDateChooser = view.findViewById(R.id.btnDate)
        exerciseStartTimeChooser = view.findViewById(R.id.btnStartTime)
        exerciseEndTimeChooser = view.findViewById(R.id.btnEndTime)
        exerciseMinDailyGoal = view.findViewById(R.id.editDailyMin)
        exerciseMaxDailyGoal = view.findViewById(R.id.editDailyMax)
        exerciseCategory = view.findViewById(R.id.spinnerChooseCategory)


        initImagePickers()
        readData(workoutID)


        val categories = resources.getStringArray(R.array.category_array).toMutableList()

        // Set an onTouchListener to the spinner
        exerciseCategory.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP)
            {
                // Remove the first item from the list
                if (categories.size > 1 && categories[0] == "Select a category")
                {
                    categories.removeAt(0)
                    // Update the adapter with the new list
                    val adapter = ArrayAdapter(
                        requireContext(), android.R.layout.simple_spinner_dropdown_item, categories
                    )
                    exerciseCategory.adapter = adapter
                }
            }
            false // Return false to allow the event to be handled by the spinner
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        backButton = view.findViewById(R.id.btnBack)
        backButton.setOnClickListener {
            btnBackClicked()
        }

        progressImage = view.findViewById(R.id.imageAddProgress)
        progressImage.setOnClickListener {
            imageAddProgressClicked()
        }

        createExercise = view.findViewById(R.id.btnCreateExercise)
        createExercise.setOnClickListener {
            btnCreateExerciseClicked(this, workoutID)
        }

        exerciseDateChooser.setOnClickListener {
            showDatePicker()
        }

        selectedStartTime = Calendar.getInstance()
        selectedEndTime = Calendar.getInstance()

        exerciseStartTimeChooser.setOnClickListener {
            showTimePicker(true)
        }

        exerciseEndTimeChooser.setOnClickListener {
            showTimePicker(false)
        }

        helpButton = view.findViewById(R.id.btnHelp)
        helpButton.setOnClickListener {
            btnHelpClicked()
        }
    }

    private fun btnHelpClicked()
    {
        navigateToFragment(
            HelpFragment(
                "help_title_add_exercise",
                "help_content_add_exercise",
                requireContext()
            )
        )
    }

    private fun navigateToFragment(fragment: Fragment)
    {
        // Replace the current fragment with the new fragment
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }

    // user name code
    private fun readData(workoutID: String)
    {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { currentUser ->
            val userId = currentUser.uid

            db.collection("users").whereEqualTo("uid", userId).get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty)
                    {
                        // There should be only one document with the given UID
                        val document = querySnapshot.documents[0]
                        val userData = document.data

                        val username = view?.findViewById<TextView>(R.id.username)

                        val firstname = userData?.get("firstname") as? String ?: ""

                        if (firstname.isNotEmpty())
                        {
                            username?.text = firstname
                        } else
                        {
                            username?.text = "User"
                        }

                        val userDocRef = document.reference

                        userDocRef.collection("workouts").whereEqualTo("workoutID", workoutID).get()
                            .addOnSuccessListener { workoutsSnapshot ->
                                if (!workoutsSnapshot.isEmpty)
                                {
                                    val workoutDocument = workoutsSnapshot.documents[0]

                                    val workoutName = workoutDocument.getString("name") ?: ""

                                    workoutNameTitle.text = workoutName
                                }
                            }.addOnFailureListener { e ->
                                Log.w("readData", "Error getting workouts: ", e)
                            }
                    }
                }
        }
    }


    fun btnCreateExerciseClicked(fragment: Fragment, workoutID: String)
    {
        val exerciseName = exerciseName.text.toString().trim()
        val exerciseDescription = exerciseDescription.text.toString().trim()

        // Validation for name and description length
        when
        {
            exerciseName.isEmpty() || exerciseDescription.isEmpty() ->
            {
                Toast.makeText(
                    fragment.requireContext(),
                    "Please enter exercise details before saving.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            exerciseName.length >= 50 ->
            {
                Toast.makeText(
                    fragment.requireContext(),
                    "Please ensure exercise name character length no more than 50.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            exerciseDescription.length >= 150 ->
            {
                Toast.makeText(
                    fragment.requireContext(),
                    "Please ensure exercise description character length no more than 150.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        // Convert the selected date to a Firestore Timestamp
        val timestamp = Timestamp(selectedDate)
        // Convert the selected start and end times to Firestore Timestamps
        val startTimestamp = Timestamp(selectedStartTime.time)
        val endTimestamp = Timestamp(selectedEndTime.time)

        val exerciseMinDaily = exerciseMinDailyGoal.text.toString().trim().toInt()
        val exerciseMaxDaily = exerciseMaxDailyGoal.text.toString().trim().toInt()
        val exerciseCategory = exerciseCategory.selectedItem.toString()

        val exerciseDetails = hashMapOf(
            "name" to exerciseName,
            "description" to exerciseDescription,
            "date" to timestamp, // Date as a timestamp
            "startTime" to startTimestamp, // Start time as a timestamp
            "endTime" to endTimestamp, // End time as a timestamp
            "min" to exerciseMinDaily,
            "max" to exerciseMaxDaily,
            "category" to exerciseCategory,
        )

        val user = FirebaseAuth.getInstance().currentUser

        user?.let { currentUser ->
            val userId = currentUser.uid
            db.collection("users").whereEqualTo("uid", userId).get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty)
                    {
                        val userDocumentRef = querySnapshot.documents[0].reference
                        userDocumentRef.collection("workouts").whereEqualTo("workoutID", workoutID)
                            .get().addOnSuccessListener { workoutsSnapshot ->
                                if (!workoutsSnapshot.isEmpty)
                                {
                                    Log.w("readData", "happened")

                                    val workoutDocument = workoutsSnapshot.documents[0]

                                    val workoutTotalExercises =
                                        workoutDocument.getLong("total-exercises")?.toInt() ?: 0

                                    Log.w("readData", workoutTotalExercises.toString())

                                    val workoutDocRef = workoutDocument.reference


                                    workoutDocRef.collection("exercises").add(exerciseDetails)
                                        .addOnSuccessListener { documentReference ->

                                            workoutDocRef.update(
                                                mapOf(
                                                    "total-exercises" to (workoutTotalExercises + 1),
                                                )
                                            )

                                            val exerciseID = documentReference.id

                                            documentReference.update(
                                                mapOf(
                                                    "exerciseID" to exerciseID,
                                                    "image" to "progress_photos/$userId/$workoutID/$exerciseID.jpg"
                                                )
                                            )

                                            uploadProgressImage(userId, workoutID, exerciseID)

                                            Toast.makeText(
                                                fragment.requireContext(),
                                                "Added new exercise: $exerciseName",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            btnBackClicked()

                                        }.addOnFailureListener { e ->
                                            Log.w("readData", "Error getting exercises: ", e)
                                        }
                                }
                            }.addOnFailureListener { e ->
                                Log.w("readData", "Error getting workouts: ", e)
                            }
                    }
                }
        }
    }

    private fun uploadProgressImage(userId: String, workoutID: String, exerciseID: String)
    {
        val drawable = progressImage.drawable

        if (drawable is BitmapDrawable)
        {
            val storageRef = Firebase.storage.reference
            val progressImageRef =
                storageRef.child("progress_photos/$userId/$workoutID/$exerciseID.jpg")

            // Get the bitmap from the profile picture image view
            val bitmap = (progressImage.drawable as BitmapDrawable).bitmap

            // Convert bitmap to bytes
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            // Upload the image bytes to Firebase Storage
            val uploadTask = progressImageRef.putBytes(data)
            uploadTask.addOnSuccessListener { taskSnapshot ->
                taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { downloadUri ->
                    imagePath = downloadUri.toString()
                }
            }.addOnFailureListener {
                // Handle unsuccessful upload
                Log.e(ContentValues.TAG, "Error uploading profile photo")
                // Show a toast or perform any other action to indicate failure
                // For example, you can display a toast message:
                Toast.makeText(
                    requireContext(), "Failed to upload profile photo", Toast.LENGTH_SHORT
                ).show()
            }
        } else
        {
            // Handle the case when the drawable is not a BitmapDrawable
            Log.e(ContentValues.TAG, "Progress picture is not a BitmapDrawable")
        }
    }

    private fun showDatePicker()
    {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(), { _, year, monthOfYear, dayOfMonth ->
                // Set the calendar to the selected date
                calendar.set(year, monthOfYear, dayOfMonth, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                selectedDate = calendar.time

                // Format the date and set it as the button text
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                exerciseDateChooser.text = dateFormat.format(selectedDate)
            }, year, month, day
        )
        datePickerDialog.show()
    }

    private fun showTimePicker(isStartTime: Boolean)
    {
        val calendar = if (isStartTime) selectedStartTime else selectedEndTime
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            requireContext(), { _, selectedHour, selectedMinute ->
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                // Update the button text with the selected time
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                if (isStartTime)
                {
                    exerciseStartTimeChooser.text = timeFormat.format(calendar.time)
                } else
                {
                    exerciseEndTimeChooser.text = timeFormat.format(calendar.time)
                }
            }, hour, minute, true // Use 24-hour format
        )
        timePickerDialog.show()
    }


    private fun btnBackClicked()
    {
        val fragment = ViewExercisesFragment().apply {
            arguments = Bundle().apply {
                putString("workout_id", workoutID)
            }
        }
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
            .addToBackStack(null).commit()
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
}