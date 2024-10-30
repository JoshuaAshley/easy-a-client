package easy_a.controllers

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import easy_a.application.R
import easy_a.models.QuestionPaperResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class PDFFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var pdfImageView: ImageView
    private lateinit var nextButton: Button
    private lateinit var previousButton: Button
    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var currentPageIndex: Int = 0
    private lateinit var back: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.pdf_fragment, container, false)
        pdfImageView = view.findViewById(R.id.pdfImageView)
        nextButton = view.findViewById(R.id.nextButton)
        previousButton = view.findViewById(R.id.previousButton)

        sessionManager = SessionManager(requireContext())
        readQuestionPaper() // Load the question paper directly

        nextButton.setOnClickListener {
            showNextPage()
        }

        previousButton.setOnClickListener {
            showPreviousPage()
        }

        back = view.findViewById(R.id.btnBack)
        back.setOnClickListener {
            btnBack(this)
        }

        return view
    }

    private fun readQuestionPaper() {
        val uid = sessionManager.getUid()
        val questionPaperId = sessionManager.getQuestionPaperId()

        RetrofitClient.apiService.getQuestionPaper(uid!!, questionPaperId!!).enqueue(
            object : Callback<QuestionPaperResponse> {
                override fun onResponse(
                    call: Call<QuestionPaperResponse>,
                    response: Response<QuestionPaperResponse>
                ) {
                    if (response.isSuccessful) {
                        val pdfUrl = response.body()?.pdfLocation
                        Log.d("PDFFragment", "PDF URL: $pdfUrl")

                        if (!pdfUrl.isNullOrEmpty()) {
                            loadPdfFromUrl(pdfUrl)
                        }
                    } else {
                        Log.e("PDFFragment", "Error: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<QuestionPaperResponse>, t: Throwable) {
                    Log.e("PDFFragment", "API call failed: ${t.message}", t)
                }
            }
        )
    }

    private fun loadPdfFromUrl(pdfUrl: String) {
        Thread {
            try {
                // Open a connection to the PDF URL
                val url = URL(pdfUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                // Check if the connection was successful
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    // Create a temporary file
                    val tempFile = File.createTempFile("tempPdf", ".pdf")
                    tempFile.deleteOnExit() // Ensure it gets deleted on exit

                    // Write the InputStream to the temporary file
                    connection.inputStream.use { inputStream ->
                        FileOutputStream(tempFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    // Open the PDF file with PdfRenderer
                    fileDescriptor = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    pdfRenderer = PdfRenderer(fileDescriptor!!)

                    // Show the first page
                    showPage(currentPageIndex)
                }
            } catch (e: Exception) {
                Log.e("PDFFragment", "Error loading PDF: ${e.message}", e)
            }
        }.start()
    }

    private fun showPage(index: Int) {
        if (pdfRenderer != null && index < pdfRenderer!!.pageCount) {
            val page = pdfRenderer!!.openPage(index)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)

            // Render the page onto the bitmap
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close() // Close the page after rendering

            // Update the ImageView on the main thread
            requireActivity().runOnUiThread {
                pdfImageView.setImageBitmap(bitmap)
                updateNavigationButtons()
            }
        }
    }

    private fun showNextPage() {
        if (currentPageIndex < (pdfRenderer?.pageCount ?: 0) - 1) {
            currentPageIndex++
            showPage(currentPageIndex)
        } else {
            Toast.makeText(requireContext(), getString(R.string.last_page_message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPreviousPage() {
        if (currentPageIndex > 0) {
            currentPageIndex--
            showPage(currentPageIndex)
        } else {
            Toast.makeText(requireContext(), getString(R.string.first_page_message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateNavigationButtons() {
        nextButton.isEnabled = currentPageIndex < (pdfRenderer?.pageCount ?: 0) - 1
        previousButton.isEnabled = currentPageIndex > 0
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfRenderer?.close() // Close the PDF renderer
        fileDescriptor?.close() // Close the file descriptor
    }

    private fun btnBack(fragment: Fragment) {
        navigateToFragment(QuestionListFragment())
    }

    private fun navigateToFragment(fragment: Fragment) {
        // Replace the current fragment with the new fragment
        parentFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }
}