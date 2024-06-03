package com.example.myai

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myai.databinding.ActivityMainBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

class MainActivity : AppCompatActivity() {
    private val TAG: String = "Geminiii"       // ? = Nullable
    lateinit var binding: ActivityMainBinding
    val geminiApiKey = "AIzaSyDmjBZUIkYZp9e0wEaydIeUN1Qy8QzuzWs"
    val GEMINI_PRO = "gemini-pro"
    val GEMINI_PRO_VISION = "gemini-pro-vision"
    var finalImgBitmap : Bitmap? = null
//    var modalName : String = GEMINI_PRO

    lateinit var generativeModel: GenerativeModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.generate.isEnabled = false
        binding.progressBar.visibility = View.GONE


        initializeGemini(GEMINI_PRO)
        removeEverything()


//        val prompt = "Write an essay to The Cow in 10 lines separated by bullets."

        binding.promptInputLayout.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            //                binding.generate.isClickable = false
//                binding.generate.alpha = 0.7f
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0?.trim() == "") {
//                    binding.generate.isClickable = false
//                    binding.generate.alpha = 0.7f
                    binding.generate.isEnabled = false
                } else {
//                    binding.generate.isClickable = true
//                    binding.generate.alpha = 1.0f
                    binding.generate.isEnabled = true
                }
            }

            override fun afterTextChanged(p0: Editable?) {
                Log.d(TAG, "afterTextChanged: $p0")
                if (p0?.toString()?.trim() == "") {
//                    binding.generate.isClickable = false
//                    binding.generate.alpha = 0.7f
                    binding.generate.isEnabled = false
                } else {
//                    binding.generate.isClickable = true
//                    binding.generate.alpha = 1.0f
                    binding.generate.isEnabled = true
                }
            }

        })

        binding.modalChangeSwitch.setOnCheckedChangeListener { switch, isChecked ->
            if (isChecked){
                initializeGemini(GEMINI_PRO_VISION)
                binding.descriptionTxt.text = "Select an image first to ask questions related to it.\nExample: \"How many people are there in the picture?\""
                binding.imageThumbnail.visibility = View.VISIBLE
                binding.promptInputLayout.isEnabled = false
                removeEverything()
            }else{
                initializeGemini(GEMINI_PRO)
                binding.descriptionTxt.text = "Enable the \"Ask with an Image\" button to ask the questions to the AI with respect to the image you selected. "
                binding.imageThumbnail.visibility = View.GONE
                binding.promptInputLayout.isEnabled = true
                removeEverything()
                finalImgBitmap = null
            }
        }


        binding.generate.setOnClickListener {

            val inputPrompt = binding.promptInputLayout.editText?.text.toString()
            if (inputPrompt.trim() != "" && finalImgBitmap != null)
//                TODO("pass image here...with prompt")
                generateGemini(inputPrompt, finalImgBitmap!!)
            else if (inputPrompt.trim() != ""){
                generateGemini(inputPrompt, finalImgBitmap)
            }else
                Toast.makeText(applicationContext,"Please write something or insert image before generate content.",
                    Toast.LENGTH_SHORT).show()
        }

        val activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    // Intent? = nullable
                    //  Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type Intent?
                   val imgUri = result.data?.data
                    if (imgUri != null){
                        try {
//                            TODO("Change below from deprecated to new method of getting bitmap")
                            finalImgBitmap = MediaStore.Images.Media.getBitmap(contentResolver,imgUri)
                            if (finalImgBitmap != null){

                                binding.thumbnail.setImageBitmap(finalImgBitmap)
                                binding.thumbnail.isEnabled = false
                                binding.removeImage.visibility = View.VISIBLE
                                binding.promptInputLayout.isEnabled = true
                                if (binding.promptInputLayout.editText?.text.toString() != "") {
                                    binding.generate.isEnabled = true
                                }
                            }

                        }catch (e: FileNotFoundException){
                            e.printStackTrace()
                        }
                    }else {
                        Toast.makeText(this@MainActivity,"Image not selected!", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        binding.thumbnail.setOnClickListener {view ->

            view.isEnabled = false
//            val getImgIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            val getImgIntent = Intent(Intent.ACTION_PICK)
            getImgIntent.type = "image/*"
            activityResultLauncher.launch(getImgIntent)
            view.isEnabled = true
        }

        binding.removeImage.setOnClickListener{view ->
            finalImgBitmap = null
            view.visibility = View.GONE
            binding.generate.isEnabled = false
            binding.thumbnail.setImageResource(R.drawable.add_image)
            binding.thumbnail.isEnabled = true
            binding.promptInputLayout.isEnabled = false
        }

    }

    private fun removeEverything() {

        binding.thumbnail.setImageResource(R.drawable.add_image)
        binding.thumbnail.isEnabled = true
        binding.removeImage.visibility = View.GONE
        binding.promptInputLayout.editText?.setText("")
        binding.generate.isEnabled = false
    }


    private fun initializeGemini(modalName: String){
        generativeModel = GenerativeModel(
            // Use a model that's applicable for your use case (see "Implement basic use cases" below)
            //        TODO("change the modal name here...")
            modelName = modalName,
//            modelName = "gemini-pro-vision",
            // Access your API key as a Build Configuration variable (see "Set up your API key" above)
            apiKey = geminiApiKey
        )
    }

    private fun generateGemini(prompt: String, bitmap: Bitmap?) {
        binding.responseTxt.text = "Loading..."
        binding.progressBar.visibility = View.VISIBLE
//        binding.generate.isEnabled = false
        removeKeyboard()


//        Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
//        R.drawable.icon_resource);
//        Bitmap bitmap =

        var inputContent: Content

        if (bitmap == null) {


            inputContent = content {
                text(prompt)
            }
        }else {

            inputContent = content {
                image(bitmap)
                text(prompt)
            }
        }

//        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.uttam_ai_logo)


        lifecycleScope.launch(Dispatchers.IO) {

            try{
                val response = generativeModel.generateContent(inputContent)

                val resStr = response.text.toString()
//            Log.d("GeminiR", "onCreate: Response Gemini: " + resStr)
                withContext(Dispatchers.Main) {
//                binding.responseTxt.text = resStr
//                binding.responseTxt.text = Html.fromHtml(resStr, Html.FROM_HTML_MODE_LEGACY)
                    val markwon = Markwon.builder(this@MainActivity)
                        .usePlugin(CorePlugin.create())
                        .build();
                    markwon.setMarkdown(binding.responseTxt, resStr)

                    binding.progressBar.visibility = View.GONE
//                    binding.promptInputLayout.editText?.setText("")
                }

            } catch (e: Exception){
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    val safetyString = "Content generation stopped. Reason: SAFETY"
                    binding.responseTxt.text = "Sorry for inconvenience, but there is an issue.\n\nIssue: ${e.message}"
                    if (e.message.toString() == safetyString) {
                        binding.responseTxt.text = "Sorry for inconvenience, but as an AI I'm not allowed to answer such question for Safety purposes."
                    }
                    Log.d(TAG, "generateGemini: "+e.message)
                }

            }

        }
    }

    private fun removeKeyboard() {
        if (currentFocus != null) {
            Log.d(TAG, "removeKeyboard: ")
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }

    }

}