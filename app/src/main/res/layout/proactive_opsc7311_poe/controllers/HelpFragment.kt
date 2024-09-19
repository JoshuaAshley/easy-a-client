package com.example.proactive_opsc7311_poe.controllers

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.proactive_opsc7311_poe.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore


class HelpFragment(
    private val helpTitleID: String,
    private val helpContentID: String,
    private val context: Context
) : Fragment()
{
    private lateinit var helpTitle: TextView
    private lateinit var helpContent: TextView
    private var userId: String? = null
    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View?
    {
        val view = inflater.inflate(R.layout.help_fragment, container, false)
        helpTitle = view.findViewById(R.id.HelpTitle)
        helpContent = view.findViewById(R.id.HelpContent)

        readData()

        helpTitle.text = setHelpContent(helpTitleID)
        helpContent.text = setHelpContent(helpContentID)


        return view
    }

    fun setHelpContent(content: String): CharSequence
    {
        // Retrieve the resource ID for the help content string
        val resourceId = context.resources.getIdentifier(content, "string", context.packageName)

        // If the resource ID is valid, retrieve the help content string
        val helpText = if (resourceId != 0)
        {
            context.resources.getText(resourceId)
        } else
        {
            // If the resource ID is not found, return a default string
            context.resources.getString(R.string.default_help_content)
        }

        /*
        val helpText = when (content) {
            "Home-Page" -> "Help content for the Home Page"
            "Choose-Workout-Plan" -> "Help content for choosing a Workout Plan"
            "New-Workout-Plan" -> "Help content for starting a new Workout Plan"
            "Exercise-Page" -> "Help content for the Exercise Page"
            "Add-Exercise" -> "Help content for adding an Exercise"
            "View-Exercise–stats" -> "Help content for viewing Exercise statistics"
            "Log-Execrise-data" -> "Help content for logging Exercise data"
            "Progress-charts" -> "Help content for Progress charts"
            "View-Workouts-and-exercises" -> "Help content for viewing Workouts and exercises"
            "Edit-Profile-Page" -> "Help content for editing Profile Page"
            else -> "Help content for other pages or scenarios"
        }
*/
        return helpText
    }


    private fun readData()
    {
        val user = FirebaseAuth.getInstance().currentUser

        user?.let { currentUser ->
            userId = currentUser.uid

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
                    }
                }
        }
    }
}