package easy_a.controllers

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import easy_a.application.R
import easy_a.models.QuestionPaperResponse
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class StudyCreateFragment : Fragment() {

    private lateinit var username: TextView
    private lateinit var back: ImageButton
    private lateinit var createQuestionPaper: Button
    private lateinit var sessionManager: SessionManager

    private lateinit var btnDueDate: Button
    private lateinit var editPaperName: EditText
    private lateinit var editDescription: EditText
    private var selectedPDFFile: File? = null // Keep it as File

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.study_create_fragment, container, false)

        username = view.findViewById(R.id.username)
        sessionManager = SessionManager(requireContext())

        editPaperName = view.findViewById(R.id.editPaperName)
        editDescription = view.findViewById(R.id.editDescription)
        btnDueDate = view.findViewById(R.id.btnDueDate)

        val firstName = sessionManager.getFirstName() ?: ""
        val lastName = sessionManager.getLastName() ?: ""
        val fullName = firstName + " " + lastName
        val email = sessionManager.getEmail() ?: ""

        username.text = if (firstName.isEmpty()) email else fullName

        btnDueDate.setOnClickListener {
            showDatePickerDialog()
        }

        val btnUploadPDF: Button = view.findViewById(R.id.btnUploadPDF) // Ensure this button is defined in your XML
        btnUploadPDF.setOnClickListener {
            openFileChooser()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        back = view.findViewById(R.id.btnBack)
        back.setOnClickListener {
            btnBack(this)
        }

        createQuestionPaper = view.findViewById(R.id.btnCreatePaper)
        createQuestionPaper.setOnClickListener {
            btnCreatePaperClicked(this)
        }
    }

    private fun btnCreatePaperClicked(fragment: Fragment) {
        val uid = sessionManager.getUid() // Get the user ID
        val questionPaperName = editPaperName.text.toString().trim() // Get the name from EditText
        val questionPaperDueDate = btnDueDate.text.toString().trim() // Replace with actual input
        val questionPaperDescription = editDescription.text.toString().trim() // Get the description from EditText

        // Validate inputs
        when {
            questionPaperName.isEmpty() -> {
                Toast.makeText(requireContext(), "Please enter a paper name.", Toast.LENGTH_SHORT).show()
                return
            }
            questionPaperName.length > 50 -> {
                Toast.makeText(requireContext(), "Paper name cannot exceed 50 characters.", Toast.LENGTH_SHORT).show()
                return
            }
            questionPaperDescription.isEmpty() -> {
                Toast.makeText(requireContext(), "Please enter a description.", Toast.LENGTH_SHORT).show()
                return
            }
            questionPaperDueDate == ("Select a Due Date") -> {
                Toast.makeText(requireContext(), "Please select a due date.", Toast.LENGTH_SHORT).show()
                return
            }
            questionPaperDescription.length > 150 -> {
                Toast.makeText(requireContext(), "Description cannot exceed 150 characters.", Toast.LENGTH_SHORT).show()
                return
            }
            selectedPDFFile == null -> {
                Toast.makeText(requireContext(), "Please upload a PDF file.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Create RequestBody instances
        val uidRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), uid!!)
        val nameRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), questionPaperName)
        val dueDateRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), questionPaperDueDate)
        val descriptionRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), questionPaperDescription)

        // Prepare the PDF file part
        val pdfFilePart: MultipartBody.Part = MultipartBody.Part.createFormData(
            "pdfFile",
            selectedPDFFile!!.name,
            RequestBody.create("application/pdf".toMediaTypeOrNull(), selectedPDFFile!!)
        )

        // API call
        RetrofitClient.apiService.createQuestionPaper(
            uidRequestBody,
            nameRequestBody,
            dueDateRequestBody,
            descriptionRequestBody,
            pdfFilePart
        ).enqueue(object : Callback<QuestionPaperResponse> {
            override fun onResponse(call: Call<QuestionPaperResponse>, response: Response<QuestionPaperResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Study paper created successfully!", Toast.LENGTH_SHORT).show()
                    navigateToFragment(StudyListFragment())
                } else {
                    Toast.makeText(requireContext(), "Failed to create study paper: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<QuestionPaperResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.d("UpdateUserRequest", "File: ${t.message}") // Log the filename
            }
        })
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Select a PDF"), PDF_PICKER_REQUEST)
    }

    private fun showDatePickerDialog() {
        // Get the current date
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Create a DatePickerDialog
        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            // Format the selected date and display it in the EditText
            val selectedDate = "$selectedYear-${selectedMonth + 1}-$selectedDay"
            btnDueDate.text = selectedDate
        }, year, month, day)

        // Show the dialog
        datePickerDialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PDF_PICKER_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            data.data?.let { uri ->
                Log.d("UpdateUserRequest", "URI: $uri") // Log the URI
                val fileName = getFileName(uri)
                Log.d("UpdateUserRequest", "File: $fileName") // Log the filename
                if (fileName != null) {
                    selectedPDFFile = getFileFromUri(uri) // Get the file from the URI
                    val btnUploadPDF: Button = view?.findViewById(R.id.btnUploadPDF) ?: return
                    btnUploadPDF.text = fileName // Set button text to the file name
                } else {
                    Toast.makeText(requireContext(), "Failed to get file name", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val file = File(requireContext().cacheDir, getFileName(uri) ?: "tempfile.pdf")
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file
        } catch (e: Exception) {
            Log.e("FileError", "Error getting file from URI: ${e.message}")
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        val returnCursor = requireContext().contentResolver.query(uri, null, null, null, null)
        returnCursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst()) {
                fileName = it.getString(nameIndex) // Ensure we're using 'it' instead of 'returnCursor'
            }
        }

        // If filename is still null, try to extract it from the URI's last path segment
        if (fileName == null) {
            fileName = uri.lastPathSegment // This can be a fallback
        }

        return fileName
    }

    companion object {
        private const val PDF_PICKER_REQUEST = 1
    }

    private fun btnBack(fragment: Fragment) {
        navigateToFragment(StudyListFragment())
    }

    private fun navigateToFragment(fragment: Fragment) {
        // Replace the current fragment with the new fragment
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }
}