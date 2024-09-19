package com.example.proactive_opsc7311_poe.controllers

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues.TAG
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
import com.example.proactive_opsc7311_poe.R
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class AccountFragment : Fragment()
{

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

    private val CAMERA_PERMISSION_REQUEST_CODE = 101

    private lateinit var firstNameEditText: TextInputEditText
    private lateinit var lastNameEditText: TextInputEditText
    private lateinit var genderSpinner: Spinner
    private lateinit var emailEditText: TextInputEditText

    private val db = Firebase.firestore


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View?
    {
        val view = inflater.inflate(R.layout.account_fragment, container, false)

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
            btnDeleteClicked(this) // Pass the fragment instance to the click handler
        }
    }

    private fun btnDeleteClicked(fragment: Fragment)
    {
        val alertDialogBuilder = AlertDialog.Builder(fragment.requireContext())
        alertDialogBuilder.setTitle("Delete Account")
        alertDialogBuilder.setMessage("Are you sure you want to delete your account?")
        alertDialogBuilder.setPositiveButton("Yes") { dialog, _ ->
            // User clicked Yes, delete account
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid

            userId?.let { uid ->
                // Query Firestore to find the document with the matching UID
                db.collection("users").whereEqualTo("uid", uid).get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty)
                        {
                            // There should be only one document with the given UID
                            val document = querySnapshot.documents[0]
                            // Delete Firebase Authentication user
                            currentUser.delete().addOnSuccessListener {
                                // User deleted successfully, now delete corresponding document in Firestore
                                document.reference.delete().addOnSuccessListener {
                                    // Deletion successful
                                    Log.d(TAG, "User document deleted successfully")

                                    // Redirect the user to the login screen
                                    val intent =
                                        Intent(fragment.requireContext(), LoginScreen::class.java)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    fragment.requireContext().startActivity(intent)

                                    // Finish the current activity to prevent the user from returning using the back button
                                    fragment.requireActivity().finish()
                                }.addOnFailureListener { exception ->
                                    Log.w(TAG, "Error deleting user document", exception)
                                    // Show error message or handle failure
                                }
                            }.addOnFailureListener { exception ->
                                Log.w(TAG, "Error deleting user", exception)
                                // Show error message or handle failure
                            }
                        } else
                        {
                            Log.d(TAG, "No document found for the user's UID")
                            // Handle the case when no document is found
                        }
                    }.addOnFailureListener { exception ->
                        Log.w(TAG, "Error getting document.", exception)
                        // Show error message or handle failure
                    }
            }
        }
        alertDialogBuilder.setNegativeButton("No") { dialog, _ ->
            // User clicked No, do nothing
            dialog.dismiss()
        }
        alertDialogBuilder.create().show()
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


    private fun readData()
    {
        // Show loading indicator while data is being fetched
        showLoadingIndicator(true)

        val user = FirebaseAuth.getInstance().currentUser

        user?.let { currentUser ->
            val userId = currentUser.uid

            // Query Firestore collection to find the document with UID matching the logged-in user's UID
            db.collection("users").whereEqualTo("uid", userId).get()
                .addOnSuccessListener { querySnapshot ->
                    // Hide loading indicator once data is fetched
                    showLoadingIndicator(false)

                    Log.w(TAG, user.email.toString())
                    emailEditText.setText(user.email)

                    if (!querySnapshot.isEmpty)
                    {
                        // There should be only one document with the given UID
                        val document = querySnapshot.documents[0]
                        val userData = document.data

                        // Set values to the corresponding components
                        firstNameEditText.setText(userData?.get("firstname") as? String ?: "")
                        lastNameEditText.setText(userData?.get("lastname") as? String ?: "")

                        val gender = userData?.get("gender") as? String ?: ""
                        val genderPosition = when (gender)
                        {
                            "male" -> 0
                            "female" -> 1
                            else -> 2 // Default position if gender is not Male or Female
                        }
                        genderSpinner.setSelection(genderPosition)

                        val dobTimestamp = userData?.get("dob") as? Timestamp
                        dobTimestamp?.let { timestamp ->
                            val dobDate = timestamp.toDate()
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val dobString = dateFormat.format(dobDate)
                            dateButton.text = dobString
                        }

                        // Check if the user has a profile picture URL
                        val profilePictureUrl = "profile_photos/$userId.jpg"
                        val storageRef = Firebase.storage.reference

                        // Get the reference to the image file in Firebase Storage
                        val profileImageRef = storageRef.child(profilePictureUrl)

                        // Get the download URL for the image
                        profileImageRef.downloadUrl.addOnSuccessListener { uri ->
                            // Call loadProfilePicture with the URL if the URI is not null and not empty
                            if (uri.toString().isNotEmpty())
                            {
                                loadProfilePicture(uri.toString())
                            } else
                            {
                                // Handle the case when the download URL is empty
                                Log.d(TAG, "Profile picture URL is empty.")
                                // You can set a default profile picture or handle it according to your app's logic
                            }
                        }.addOnFailureListener { exception ->
                            Log.e(TAG, "Error getting download URL", exception)
                            // Handle the failure to get the download URL
                        }
                    } else
                    {
                        Log.d(TAG, "No document found for the user's UID")
                    }
                }.addOnFailureListener { exception ->
                    // Hide loading indicator if an error occurs
                    showLoadingIndicator(false)

                    Log.w(TAG, "Error getting documents.", exception)
                }
        } ?: run {
            Log.d(TAG, "User is not logged in.")
            // Handle the case when the user is not logged in
        }
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
                    Log.e(TAG, "Error loading profile picture", e)
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

    fun String.toTimestamp(): Timestamp
    {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = dateFormat.parse(this)
        return Timestamp(date)
    }

    fun btnSaveButtonClicked(fragment: Fragment)
    {
        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val gender = genderSpinner.selectedItem.toString().toLowerCase()
        val dobString = dateButton.text.toString()

        var dobTimestamp = todaysDate.toTimestamp()

        // Convert the date string to a Timestamp
        if (dobString != "")
        {
            dobTimestamp = dobString.toTimestamp()
        }

        // Validate first name and last name length
        val isFirstNameValid = validateLength(firstName, "First name", 50)
        val isLastNameValid = validateLength(lastName, "Last name", 50)

        if (isFirstNameValid && isLastNameValid)
        {
            // Get the current user
            val currentUser = FirebaseAuth.getInstance().currentUser

            currentUser?.let { user ->
                val userId = currentUser.uid
                // Query Firestore collection to find the document with UID matching the logged-in user's UID
                db.collection("users").whereEqualTo("uid", userId).get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty)
                        {
                            // There should be only one document with the given UID
                            val document = querySnapshot.documents[0]
                            // Update the document with the new values
                            document.reference.update(
                                mapOf(
                                    "firstname" to if (firstName.isNotEmpty()) firstName else "",
                                    "lastname" to if (lastName.isNotEmpty()) lastName else "",
                                    "gender" to gender,
                                    "dob" to dobTimestamp
                                )
                            ).addOnSuccessListener {
                                Log.d(TAG, "Account successfully updated!")
                                // Show a toast or perform any other action to indicate success
                                // For example, you can display a toast message:
                                Toast.makeText(
                                    requireContext(),
                                    "Profile updated successfully",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Upload profile photo to Firebase Storage only if an image is selected
                                if (profilePictureButton.drawable != null)
                                {
                                    uploadProfilePhoto(user.uid)
                                }
                            }.addOnFailureListener { e ->
                                Log.w(TAG, "Error updating document", e)
                                // Show a toast or perform any other action to indicate failure
                                // For example, you can display a toast message:
                                Toast.makeText(
                                    requireContext(), "Failed to update profile", Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else
                        {
                            Log.d(TAG, "No document found for the user's UID")
                        }
                    }.addOnFailureListener { exception ->
                        Log.w(TAG, "Error getting document.", exception)
                    }
            }
        }
    }

    private fun uploadProfilePhoto(userId: String)
    {
        val drawable = profilePictureButton.drawable
        if (drawable is BitmapDrawable)
        {
            val storageRef = Firebase.storage.reference
            val profilePhotosRef = storageRef.child("profile_photos/$userId.jpg")

            // Get the bitmap from the profile picture image view
            val bitmap = (profilePictureButton.drawable as BitmapDrawable).bitmap

            // Convert bitmap to bytes
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            // Upload the image bytes to Firebase Storage
            val uploadTask = profilePhotosRef.putBytes(data)
            uploadTask.addOnSuccessListener { taskSnapshot ->
            }.addOnFailureListener { exception ->
                // Handle unsuccessful upload
                Log.e(TAG, "Error uploading profile photo", exception)
                // Show a toast or perform any other action to indicate failure
                // For example, you can display a toast message:
                Toast.makeText(
                    requireContext(), "Failed to upload profile photo", Toast.LENGTH_SHORT
                ).show()
            }
        } else
        {
            // Handle the case when the drawable is not a BitmapDrawable
            Log.e(TAG, "Profile picture is not a BitmapDrawable")
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