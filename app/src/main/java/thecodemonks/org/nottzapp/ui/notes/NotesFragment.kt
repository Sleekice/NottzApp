/*
 *
 *  *
 *  *  * MIT License
 *  *  *
 *  *  * Copyright (c) 2020 Spikey Sanju
 *  *  *
 *  *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  *  * of this software and associated documentation files (the "Software"), to deal
 *  *  * in the Software without restriction, including without limitation the rights
 *  *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  *  * copies of the Software, and to permit persons to whom the Software is
 *  *  * furnished to do so, subject to the following conditions:
 *  *  *
 *  *  * The above copyright notice and this permission notice shall be included in all
 *  *  * copies or substantial portions of the Software.
 *  *  *
 *  *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  *  * SOFTWARE.
 *  *
 *
 *
 */

package thecodemonks.org.nottzapp.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import thecodemonks.org.nottzapp.R
import thecodemonks.org.nottzapp.adapter.NotesAdapter
import thecodemonks.org.nottzapp.databinding.NotesFragmentBinding
import thecodemonks.org.nottzapp.model.Notes
import thecodemonks.org.nottzapp.utils.NotesViewState
import thecodemonks.org.nottzapp.utils.hide
import thecodemonks.org.nottzapp.utils.show
import thecodemonks.org.nottzapp.utils.toast

@AndroidEntryPoint
class NotesFragment : Fragment(R.layout.notes_fragment) {

    private val viewModel: NotesViewModel by activityViewModels()
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var _binding: NotesFragmentBinding
    private val binding get() = _binding

    private lateinit var searchView: SearchView

    private val originalNotesList = mutableListOf<Notes>()
    private val filteredNotesList = mutableListOf<Notes>()



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflates the layout for the NotesFragment and returns the root view.

        _binding = NotesFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        setUpRV()
        initViews()
        observeNotes()
        initSwipeToDeleteNote()
        onClickNotes()
    }



    // Filters the list of notes based on the search query.
    private fun filterNotes(query: String?) {

        filteredNotesList.clear()

        if (query.isNullOrEmpty()) {
            // If the query is empty or null, show all original notes
            filteredNotesList.addAll(originalNotesList)
        } else {
            val lowerCaseQuery = query.toLowerCase()
            for (note in originalNotesList) {
                // Check if the note's title or description contains the query
                if (note.title.toLowerCase().contains(lowerCaseQuery) || note.description.toLowerCase().contains(lowerCaseQuery)) {
                    filteredNotesList.add(note)
                }
            }
        }
        notesAdapter.differ.submitList(filteredNotesList)
    }

    // Handles the click event for individual notes, allowing navigation to note details.
    private fun onClickNotes() {
        // onclick navigate to add notes
        notesAdapter.setOnItemClickListener {
            val bundle = Bundle().apply {
                putSerializable("notes", it)
            }
            findNavController().navigate(
                R.id.action_notesFragment_to_notesDetailsFragment,
                bundle
            )
        }
    }


    // Observes the state of notes and updates the UI accordingly.
    private fun observeNotes() {
        // This will block will run on started state & will suspend when view moves to stop state
        lifecycleScope.launchWhenStarted {
            // Triggers the flow and starts listening for values
            viewModel.uiState.collect { uiState ->
                when (uiState) {
                    is NotesViewState.Loading -> binding.progress.show()
                    is NotesViewState.Success -> {
                        binding.progress.hide()
                        onNotesLoaded(uiState.notes)
                    }
                    is NotesViewState.Error -> {
                        binding.progress.hide()
                        requireActivity().toast("Error...")
                    }
                    is NotesViewState.Empty -> {
                        binding.progress.hide()
                        showEmptyState()
                    }
                }
            }
        }
    }

    // Sets up swipe-to-delete functionality for notes and handles undo.
    private fun initSwipeToDeleteNote() {
        // init item touch callback for swipe action
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // get item position & delete notes
                val position = viewHolder.adapterPosition
                val notes = notesAdapter.differ.currentList[position]
                viewModel.deleteNoteByID(
                    notes.id
                )
                Snackbar.make(
                    binding.root,
                    getString(R.string.note_deleted_msg),
                    Snackbar.LENGTH_LONG
                )
                    .apply {
                        setAction(getString(R.string.undo)) {
                            viewModel.insertNotes(
                                notes.title,
                                notes.description
                            )
                        }
                        show()
                    }
            }
        }

        // attach swipe callback to rv
        ItemTouchHelper(itemTouchHelperCallback).apply {
            attachToRecyclerView(binding.notesRv)
        }
    }

    // Handles UI elements and navigation to the add notes screen.
    private fun initViews() {
        // onclick navigate to add notes
        binding.btnAddNotes.setOnClickListener {
            findNavController().navigate(R.id.action_notesFragment_to_addNotesFragment)
        }
    }

    // Displays an empty state message when there are no notes.
    private fun showEmptyState() {
        binding.emptyStateLayout.show()
        notesAdapter.differ.submitList(emptyList())
    }

    // Updates the UI with loaded notes, considering filtered notes if applicable.
    private fun onNotesLoaded(notes: List<Notes>) {
        binding.emptyStateLayout.hide()
        notesAdapter.differ.submitList(notes)

            if (filteredNotesList.isNotEmpty()) {
                // If there are filtered notes, display them
                notesAdapter.differ.submitList(filteredNotesList)
            } else {
                // If there are no filtered notes, display all the loaded notes
                notesAdapter.differ.submitList(notes)
            }

    }

    // Configures the RecyclerView for displaying notes.
    private fun setUpRV() {
        notesAdapter = NotesAdapter()
        binding.notesRv.apply {
            adapter = notesAdapter
            layoutManager = LinearLayoutManager(activity)
        }
    }

    // Inflates the options menu and sets up the search functionality.
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.ui_menu, menu)


        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as SearchView
        searchView.queryHint = "Search..."
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    // Perform the search with the query
                    filterNotes(query)
                } else {
                    // If the query is empty, you can choose to handle it in some way, e.g., show all original notes.
                    filterNotes(null) // Pass null or an empty string to reset the search and show all notes.
                }

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Handle search query text changes
                filterNotes(newText)
                return true
            }
        })


        // Set the item state
        lifecycleScope.launch {
            val isChecked = viewModel.getUIMode.first()
            val item = menu.findItem(R.id.action_night_mode)
            item.isChecked = isChecked
            setUIMode(item, isChecked)
        }
    }

    // Handles user interactions with options menu items.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        return when (item.itemId) {
            R.id.action_night_mode -> {
                item.isChecked = !item.isChecked
                setUIMode(item, item.isChecked)
                true
            }

            R.id.action_about -> {
                findNavController().navigate(R.id.action_notesFragment_to_aboutFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Sets the UI mode (night or day mode) and updates the UI.
    private fun setUIMode(item: MenuItem, isChecked: Boolean) {
        if (isChecked) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            viewModel.saveToDataStore(true)
            item.setIcon(R.drawable.ic_night)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            viewModel.saveToDataStore(false)
            item.setIcon(R.drawable.ic_day)
        }
    }
}
