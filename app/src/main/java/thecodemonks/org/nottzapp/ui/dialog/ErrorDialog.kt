/**********************************************************
 *  ERROR DIALOG FRAGMENT
 **********************************************************/

/*
 * This code defines a BottomSheetDialogFragment that displays an error dialog.
 */

package thecodemonks.org.nottzapp.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import thecodemonks.org.nottzapp.databinding.ErrorDialogLayoutBinding


class ErrorDialog : BottomSheetDialogFragment() {
    // Binding for the error dialog layout
    private var _binding: ErrorDialogLayoutBinding? = null
    private val binding get() = _binding!!

    // Retrieve arguments passed to the dialog
    private val args: ErrorDialogArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflates the error dialog layout and returns the root view
        _binding = ErrorDialogLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.run {
            // Set the dialog title and message from the arguments
            dialogTitle.text = args.title
            dialogMessage.text = args.message

            // Handle the button click to dismiss the dialog
            dialogButtonOk.setOnClickListener { dialog?.dismiss() }
        }
    }

    override fun onStart() {
        super.onStart()
        // Configure the dialog window layout to match the parent's dimensions
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the binding when the fragment is destroyed
        _binding = null
    }
}
