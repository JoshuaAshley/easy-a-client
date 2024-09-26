package easy_a.controllers

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import java.util.Calendar

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
            //btnSaveButtonClicked(this) // Pass the fragment instance to the click handler
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

    private fun showLoadingIndicator(show: Boolean)
    {
        if (show)
        {

            // Disable the spinner and set a loading message as its selected item
            genderSpinner.isEnabled = false
            firstNameEditText.isEnabled = false
            lastNameEditText.isEnabled = false
            emailEditText.isEnabled = false
            dateButton.isEnabled = false

            // Show loading indicator
            firstNameEditText.setText("Loading...")
            lastNameEditText.setText("Loading...")
            emailEditText.setText("Loading...")
            dateButton.setText("Loading...")

            // Create a temporary adapter with the loading message
            val loadingAdapter = ArrayAdapter<String>(
                requireContext(), android.R.layout.simple_spinner_item, arrayOf("Loading...")
            )
            loadingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            genderSpinner.adapter = loadingAdapter

            // You can similarly set loading indicators for other fields if needed
        } else
        {

            genderSpinner.isEnabled = true
            firstNameEditText.isEnabled = true
            lastNameEditText.isEnabled = true
            emailEditText.isEnabled = true
            dateButton.isEnabled = true

            // Hide loading indicator
            firstNameEditText.text = null
            lastNameEditText.text = null
            emailEditText.text = null
            dateButton.text = null

            // Reset the spinner adapter to its original array values
            val originalAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.gender_array, android.R.layout.simple_spinner_item
            )
            originalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            genderSpinner.adapter = originalAdapter

            // Clear loading indicators for other fields if needed
        }
    }

    private fun uploadProfilePhoto(userId: String)
    {
        val drawable = profilePictureButton.drawable
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