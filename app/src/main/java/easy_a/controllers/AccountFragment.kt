package easy_a.controllers

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
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
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.easy_a.R
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import easy_a.models.UserResponse
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.text.ParseException

class AccountFragment : Fragment() {

    private lateinit var datePickerDialog: DatePickerDialog
    private lateinit var dateButton: Button
    private lateinit var profilePictureButton: ShapeableImageView
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var currentPhotoUri: Uri
    private lateinit var imageView: ImageView
    private lateinit var saveButton: Button
    private lateinit var logoutButton: Button
    private lateinit var deleteButton: Button

    private lateinit var sessionManager: SessionManager

    private val CAMERA_PERMISSION_REQUEST_CODE = 101

    private lateinit var firstNameEditText: TextInputEditText
    private lateinit var lastNameEditText: TextInputEditText
    private lateinit var genderSpinner: Spinner
    private lateinit var emailEditText: TextInputEditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.account_fragment, container, false)


        sessionManager = SessionManager(requireContext())

        dateButton = view.findViewById(R.id.datePickerButton)
        profilePictureButton = view.findViewById(R.id.profilePicture)
        imageView = view.findViewById(R.id.cameraImageView)
        initImagePickers()
        initDatePicker()
        dateButton.text = todaysDate

        // Set click listener for the profile picture button
        profilePictureButton.setOnClickListener {
            openImagePicker()
        }

        // Find views
        dateButton = view.findViewById(R.id.datePickerButton)
        profilePictureButton = view.findViewById(R.id.profilePicture)
        imageView = view.findViewById(R.id.cameraImageView)
        firstNameEditText = view.findViewById(R.id.firstNameEditText)
        lastNameEditText = view.findViewById(R.id.lastNameEditText)
        genderSpinner = view.findViewById(R.id.genderSpinner)
        emailEditText = view.findViewById(R.id.emailEditText)

        readData()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        dateButton.setOnClickListener {
            openDateClicker()
        }

        saveButton =
            view.findViewById(R.id.btnSaveButton) // Replace 'saveButton' with the ID of your button
        saveButton.setOnClickListener {
            btnSaveButtonClicked(this) // Pass the fragment instance to the click handler
        }

        logoutButton =
            view.findViewById(R.id.btnLogout) // Replace 'saveButton' with the ID of your button
        logoutButton.setOnClickListener {
            btnLogoutClicked(this) // Pass the fragment instance to the click handler
        }

        deleteButton =
            view.findViewById(R.id.btnDelete) // Replace 'saveButton' with the ID of your button
        deleteButton.setOnClickListener {
            //btnDeleteClicked(this) // Pass the fragment instance to the click handler
        }
    }

    private fun readData() {
        // Fetch user data from session manager
        val firstName = sessionManager.getFirstName() ?: ""
        val lastName = sessionManager.getLastName() ?: ""
        val gender = sessionManager.getGender() ?: ""
        val dob = sessionManager.getDateOfBirth() ?: ""
        val email = sessionManager.getEmail() ?: ""
        val profilePictureUrl = sessionManager.getProfilePictureUrl() // Fetch profile picture URL

        // Set text for EditText fields
        firstNameEditText.setText(firstName)
        lastNameEditText.setText(lastName)
        emailEditText.setText(email)

        // Set the date button text
        dateButton.text = dob

        // Load the profile picture using Picasso if the URL is not null
        if (!profilePictureUrl.isNullOrEmpty()) {
            loadProfilePicture(profilePictureUrl)
        }

        // Set the spinner selection based on gender
        val genderAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.gender_array, // Ensure this array is defined in strings.xml
            android.R.layout.simple_spinner_item
        )
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        genderSpinner.adapter = genderAdapter

        // Find the index of the selected gender and set it to the spinner
        val genderPosition = genderAdapter.getPosition(gender)
        if (genderPosition >= 0) {
            genderSpinner.setSelection(genderPosition)
        }
    }

    private fun btnSaveButtonClicked(fragment: Fragment) {
        // Retrieve user input from UI elements
        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val gender = genderSpinner.selectedItem.toString()
        val dobString = dateButton.text.toString()

        Log.d("UpdateUserRequest", "DOB: $dobString")

        // Validate the length of first name and last name
        val isFirstNameValid = validateLength(firstName, "First name", 50)
        val isLastNameValid = validateLength(lastName, "Last name", 50)

        // Proceed only if both validations pass
        if (isFirstNameValid && isLastNameValid) {
            // Retrieve the UID from session manager (assumes session management is implemented)
            val sessionManager = SessionManager(fragment.requireContext())
            val uid = sessionManager.getUid()

            if (uid != null) {
                // Create MultipartBody.Part for the image, if available
                var profileImagePart: MultipartBody.Part? = null
                val drawable = profilePictureButton.drawable

                if (drawable is BitmapDrawable) {
                    val bitmap = drawable.bitmap
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val imageData = baos.toByteArray()

                    // Create RequestBody for the image file
                    val requestFile = RequestBody.create(MediaType.parse("image/jpeg"), imageData)

                    // Create MultipartBody.Part using the file request body
                    profileImagePart = MultipartBody.Part.createFormData("profileImage", "profile.jpg", requestFile)
                }

                // Create RequestBody objects for the text fields
                val uidRequestBody = RequestBody.create(MediaType.parse("text/plain"), uid)
                val firstNameRequestBody = RequestBody.create(MediaType.parse("text/plain"), firstName)
                val lastNameRequestBody = RequestBody.create(MediaType.parse("text/plain"), lastName)
                val genderRequestBody = RequestBody.create(MediaType.parse("text/plain"), gender)

                // Handle the API response asynchronously
                if (dobString.isNotEmpty()) {
                    // Parse the date string to a Date object
                    val parsedDob: Date? = try {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dobString)
                    } catch (e: ParseException) {
                        null // Log or handle parse exception if needed
                    }

                    // Format the date to the required format for the API
                    val formattedDob = parsedDob?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                    }

                    val dateRequestBody =
                        formattedDob?.let { RequestBody.create(MediaType.parse("text/plain"), it) }

                    Log.d("UpdateUserRequest", "UID: $uid, First Name: $firstName, Last Name: $lastName, Gender: $gender, DOB: $formattedDob")

                    // Call the API only if formattedDob is not null
                    if (dateRequestBody != null) {
                        RetrofitClient.apiService.updateUserSettings(
                            uidRequestBody,
                            firstNameRequestBody,
                            lastNameRequestBody,
                            genderRequestBody,
                            dateRequestBody,
                            profileImagePart
                        ).enqueue(object : Callback<UserResponse> {
                            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                                if (response.isSuccessful) {
                                    // Update Shared Preferences with new user details
                                    val sharedPreferences = fragment.requireContext().getSharedPreferences("com.example.easy_a", Context.MODE_PRIVATE)
                                    val editor = sharedPreferences.edit()

                                    val dateOfBirthString = response.body()?.dateOfBirth // Assuming this is your original date string
                                    val formattedDateOfBirth = formatDateOfBirth(dateOfBirthString)

                                    editor.putString("firstname", response.body()?.firstName)
                                    editor.putString("lastname", response.body()?.lastName)
                                    editor.putString("gender", response.body()?.gender)
                                    editor.putString("dateOfBirth", formattedDateOfBirth)
                                    editor.putString("profilePictureUrl", response.body()?.profilePicture)
                                    editor.apply() // Save changes to Shared Preferences

                                    // Notify the MainScreen about the updated profile picture
                                    (fragment.activity as? ProfileUpdateListener)?.onProfileUpdated(response.body()?.profilePicture)

                                    Toast.makeText(fragment.requireContext(), "User updated successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(fragment.requireContext(), "Failed to update user: ${response.code()}", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                                Toast.makeText(fragment.requireContext(), "Failed to update user: onFailure", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            } else {
                // If UID is null, handle it appropriately (e.g., redirect to login)
                Toast.makeText(fragment.requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Inform the user of invalid input
            Toast.makeText(fragment.requireContext(), "Please check the input fields", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDateOfBirth(dateString: String?): String? {
        return dateString?.let {
            // Remove the "Timestamp: " prefix if it exists
            val cleanDateString = it.replace("Timestamp: ", "").trim()

            // Parse the original timestamp format
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val date: Date? = inputFormat.parse(cleanDateString)

            // Format it to the desired format
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            date?.let { outputFormat.format(it) }
        }
    }

    private fun validateLength(value: String, fieldName: String, maxLength: Int): Boolean
    {
        return if (value.length > maxLength)
        {
            // Field exceeds maximum length, show error message
            Toast.makeText(
                requireContext(),
                "$fieldName cannot be longer than $maxLength characters",
                Toast.LENGTH_SHORT
            ).show()
            false
        } else
        {
            true
        }
    }

    private fun btnLogoutClicked(fragment: Fragment)
    {
        FirebaseAuth.getInstance().signOut()
        // Redirect the user to the login screen
        val intent = Intent(fragment.requireContext(), LoginScreen::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        fragment.requireContext().startActivity(intent)
        // Finish the current activity to prevent the user from returning using the back button
        fragment.requireActivity().finish()
    }

    private fun loadProfilePicture(imageUrl: String)
    {
        // Load image with Picasso
        Picasso.get().load(imageUrl)
            .into(profilePictureButton, object : com.squareup.picasso.Callback
            {
                override fun onSuccess()
                {
                    // Image loaded successfully
                    profilePictureButton.visibility = View.VISIBLE
                    profilePictureButton.shapeAppearanceModel =
                        profilePictureButton.shapeAppearanceModel.toBuilder()
                            .setAllCornerSizes(ShapeAppearanceModel.PILL).build()
                    profilePictureButton.scaleType = ImageView.ScaleType.CENTER_CROP
                    imageView.isVisible = false
                }

                override fun onError(e: Exception?)
                {
                    // Handle error loading image
                    Log.e(ContentValues.TAG, "Error loading profile picture", e)
                }
            })
    }

    private fun initDatePicker()
    {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val newMonth = month + 1
            val date = makeDateString(day, newMonth, year)
            dateButton.text = date
        }

        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val style = AlertDialog.THEME_HOLO_DARK

        datePickerDialog =
            DatePickerDialog(requireContext(), style, dateSetListener, year, month, day)
    }

    private fun makeDateString(day: Int, month: Int, year: Int): String
    {
        val formattedDay = if (day < 10) "0$day" else day.toString()
        val formattedMonth = if (month < 10) "0$month" else month.toString()
        return "$formattedDay/$formattedMonth/$year"
    }

    private fun openDateClicker()
    {
        datePickerDialog.show()
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

    private fun initImagePickers()
    {
        // Initialize the launcher for picking images
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let { imageUri ->
                    // Set the chosen image as profile picture
                    setProfilePicture(imageUri)
                }
            }

        // Initialize the launcher for taking pictures
        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK)
                {
                    val imageBitmap = result.data?.extras?.get("data") as Bitmap
                    setProfilePictureFromBitmap(imageBitmap)
                }
            }
    }

    private fun setProfilePictureFromBitmap(imageBitmap: Bitmap)
    {
        // Set the captured image as profile picture
        profilePictureButton.setImageBitmap(imageBitmap)
        profilePictureButton.visibility = View.VISIBLE
        profilePictureButton.shapeAppearanceModel =
            profilePictureButton.shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(ShapeAppearanceModel.PILL).build()
        profilePictureButton.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.isVisible = false
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

    private fun setProfilePicture(imageUri: Uri)
    {
        profilePictureButton.visibility = View.VISIBLE
        profilePictureButton.setImageURI(imageUri)
        profilePictureButton.shapeAppearanceModel =
            profilePictureButton.shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(ShapeAppearanceModel.PILL).build()
        profilePictureButton.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.isVisible = false
    }

    private val todaysDate: String
        get()
        {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            var month = cal.get(Calendar.MONTH)
            month += 1
            val day = cal.get(Calendar.DAY_OF_MONTH)
            return makeDateString(day, month, year)
        }
}