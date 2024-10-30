package easy_a.controllers

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.BuildConfig
import easy_a.application.BuildConfig as AppBuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.asTextOrNull
import com.google.ai.client.generativeai.type.generationConfig
import easy_a.application.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EZFragment : Fragment() {
    private lateinit var chatAdapter: ChatAdapter // RecyclerView adapter
    private var selectedFileUri: Uri? = null

    private val CAMERA_PERMISSION_REQUEST_CODE = 101

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>

    private lateinit var sessionManager: SessionManager

    private val sampleResponses = listOf(
        "I'm here to help you with your Android development!",
        "Yes, I can assist with building chat bots.",
        "What else do you need help with?",
        "Feel free to ask me anything about Android!",
        "I'm glad to assist you with your project."
    )

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreate(savedInstanceState)
        val view = inflater.inflate(R.layout.ez_fragment, container, false)

        // Initialize RecyclerView
        val chatRecyclerView = view.findViewById<RecyclerView>(R.id.chatRecyclerView)
        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        chatAdapter = ChatAdapter(requireContext())
        chatRecyclerView.adapter = chatAdapter

        val messageInput = view.findViewById<EditText>(R.id.messageInput)
        val sendButton = view.findViewById<ImageButton>(R.id.sendButton)
        val uploadFileButton = view.findViewById<ImageButton>(R.id.uploadFileButton)

        val titleTextView = view.findViewById<TextView>(R.id.titleTextView)

        sessionManager = SessionManager(requireContext())

        if (!sessionManager.isDarkMode()) {
            // Set all text elements to black for light mode
            titleTextView.setTextColor(resources.getColor(R.color.black))
            messageInput.background = resources.getDrawable(R.drawable.textfield_light)
        }

        initImagePickers()

        sendMessageToChatBot(
            "Please send the user a hello message. Your name is EZ throughout this session. You are a study assistant chat bot that helps students of any level achieve their academic goals. This is not a question but a command. Please respond to this with a warm greeting message.",
            selectedFileUri
        )

        sendButton.setOnClickListener {
            var userMessage = messageInput.text.toString()

            if (userMessage.isNotEmpty() && selectedFileUri != null) {
                Toast.makeText(requireContext(), getString(R.string.enter_message_or_image), Toast.LENGTH_SHORT).show()
            }

            // Send text, file, or both
            if (userMessage.isNotEmpty() || selectedFileUri != null) {

                if (userMessage.isEmpty() && selectedFileUri != null) {
                    userMessage =
                        "**Uploaded Image** : ```" + selectedFileUri!!.path.toString() + "```"

                    val imageBitmap = uriToBitmap(selectedFileUri!!)

                    addMessageToChat(userMessage, true, getCurrentTimeString(), imageBitmap)
                    messageInput.text.clear()

                    sendMessageToChatBot("", selectedFileUri)
                    selectedFileUri = null
                } else {
                    if (userMessage.isNotEmpty() && selectedFileUri == null) {
                        addMessageToChat(userMessage, true, getCurrentTimeString())
                        messageInput.text.clear()

                        sendMessageToChatBot("Your name is EZ. Always refer to that if asked. You are a student study assistant chat bot. Please answer this question: " + userMessage, selectedFileUri)
                        selectedFileUri = null
                    }
                }
            } else {
                Toast.makeText(requireContext(), getString(R.string.enter_message_or_file), Toast.LENGTH_SHORT).show()
            }
        }

        uploadFileButton.setOnClickListener {
            openImagePicker()
        }


        return view
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
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA
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
        // Initialize the launcher for picking an image from the gallery
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let { imageUri ->
                    // Set the chosen image as profile picture
                    selectedFileUri = imageUri

                    // Display a toast with the selected file information
                    Toast.makeText(requireContext(), getString(R.string.image_selected, imageUri.toString()), Toast.LENGTH_SHORT).show()
                } ?: run {
                    // In case no image was selected, notify the user
                    Toast.makeText(requireContext(), getString(R.string.no_image_selected), Toast.LENGTH_SHORT).show()
                }
            }

        // Initialize the launcher for taking pictures
        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // Get the Bitmap from the result
                    val imageBitmap = result.data?.extras?.get("data") as Bitmap

                    // Convert Bitmap to Uri
                    selectedFileUri = bitmapToUri(imageBitmap)

                    // Display a toast with the file URI information
                    Toast.makeText(requireContext(), getString(R.string.image_captured, selectedFileUri.toString()), Toast.LENGTH_SHORT).show()
                } else {
                    // In case the picture was not taken, notify the user
                    Toast.makeText(requireContext(), getString(R.string.no_image_captured), Toast.LENGTH_SHORT).show()
                }
            }

    }

    private fun bitmapToUri(bitmap: Bitmap): Uri? {
        val resolver = activity?.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "image_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val imageUri: Uri? = resolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        imageUri?.let {
            val outputStream = resolver?.openOutputStream(it)
            outputStream?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            }
        }
        return imageUri
    }


    private fun addMessageToChat(message: String, isUserMessage: Boolean, timeSent: String, bitmap: Bitmap?=null) {
        chatAdapter.addMessage(message, isUserMessage, timeSent, bitmap)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateRandomMessage() {
        // Get a random index from the sample responses
        val randomIndex = Random.nextInt(sampleResponses.size)
        // Return a random response
        activity?.runOnUiThread {
            addMessageToChat(sampleResponses[randomIndex], false, getCurrentTimeString())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getCurrentTimeString(): String {
        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm") // You can adjust the pattern as needed
        return currentTime.format(formatter)
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        return MediaStore.Images.Media.getBitmap(activity?.contentResolver, uri)
    }

    // Send the user message to the AI model
    // Modify the function to handle text, file, or both
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendMessageToChatBot(message: String, fileUri: Uri?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiKey = AppBuildConfig.AI_API_KEY

                //Log.d("API_Key", apiKey)
                // Initialize the AI model
                val model = GenerativeModel(
                    "gemini-1.5-flash",
                    apiKey,
                    generationConfig {
                        temperature = 1f
                        topP = 0.95f
                        topK = 64
                        maxOutputTokens = 8192
                        responseMimeType = "text/plain"
                    }
                )

                if (message.isNotEmpty())
                {
                    val textResponse = model.generateContent(message)

                    // Process the AI's response
                    var aiResponse = ""
                    textResponse.candidates.firstOrNull()?.content?.parts?.forEach { part ->
                        aiResponse += part.asTextOrNull()
                    }

                    // Display the AI's response on the UI thread
                    activity?.runOnUiThread {
                        addMessageToChat(aiResponse, false, getCurrentTimeString())
                    }
                }
                else
                {
                    Log.d("FileSelection", fileUri.toString())

                    if (fileUri != null)
                    {
                        val imageResponse = model.generateContent(uriToBitmap(fileUri))

                        // Process the AI's response
                        var aiResponse = ""
                        imageResponse.candidates.firstOrNull()?.content?.parts?.forEach { part ->
                            aiResponse += part.asTextOrNull()
                        }

                        // Display the AI's response on the UI thread
                        activity?.runOnUiThread {
                            addMessageToChat(aiResponse, false, getCurrentTimeString())
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}