package com.syedsaifhossain.alorferiuserapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}


        class NoteRepository @Inject constructor(private val noteAPI: NoteAPI) {

            private val _notesLiveData = MutableLiveData<NetworkResult<List<NoteResponse>>>()
            val notesLiveData get() = _notesLiveData

            private val _statusLiveData = MutableLiveData<NetworkResult<Pair<Boolean, String>>>()
            val statusLiveData get() = _statusLiveData

            suspend fun createNote(noteRequest: NoteRequest) {
                _statusLiveData.postValue(NetworkResult.Loading())
                val response = noteAPI.createNote(noteRequest)
                handleResponse(response, "Note Created")
            }

            suspend fun getNotes() {
                _notesLiveData.postValue(NetworkResult.Loading())
                val response = noteAPI.getNotes()
                if (response.isSuccessful && response.body() != null) {
                    _notesLiveData.postValue(NetworkResult.Success(response.body()!!))
                } else if (response.errorBody() != null) {
                    val errorObj = JSONObject(response.errorBody()!!.charStream().readText())
                    _notesLiveData.postValue(NetworkResult.Error(errorObj.getString("message")))
                } else {
                    _notesLiveData.postValue(NetworkResult.Error("Something Went Wrong"))
                }
            }

            suspend fun updateNote(id: String, noteRequest: NoteRequest) {
                _statusLiveData.postValue(NetworkResult.Loading())
                val response = noteAPI.updateNote(id, noteRequest)
                handleResponse(response, "Note Updated")
            }

            suspend fun deleteNote(noteId: String) {
                _statusLiveData.postValue(NetworkResult.Loading())
                val response = noteAPI.deleteNote(noteId)
                handleResponse(response, "Note Deleted")
            }

            private fun handleResponse(response: Response<NoteResponse>, message: String) {
                if (response.isSuccessful && response.body() != null) {
                    _statusLiveData.postValue(NetworkResult.Success(Pair(true, message)))
                } else {
                    _statusLiveData.postValue(
                        NetworkResult.Success(
                            Pair(
                                false,
                                "Something went wrong"
                            )
                        )
                    )
                }
            }
        }















        @AndroidEntryPoint
        class RegisterFragment : Fragment() {

            private var _binding: FragmentRegisterBinding? = null
            private val binding get() = _binding!!

            private val authViewModel by activityViewModels<AuthViewModel>()

            @Inject
            lateinit var tokenManager: TokenManager

            override fun onCreateView(
                inflater: LayoutInflater,
                container: ViewGroup?,
                savedInstanceState: Bundle?
            ): View? {

                _binding = FragmentRegisterBinding.inflate(inflater, container, false)
                if (tokenManager.getToken() != null) {
                    findNavController().navigate(R.id.action_registerFragment_to_mainFragment)
                }
                return binding.root

            }

            override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)
                binding.btnLogin.setOnClickListener {
                    it.findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                }
                binding.btnSignUp.setOnClickListener {
                    hideKeyboard(it)
                    val validationResult = validateUserInput()
                    if (validationResult.first) {
                        val userRequest = getUserRequest()
                        authViewModel.registerUser(userRequest)
                    } else {
                        showValidationErrors(validationResult.second)
                    }
                }
                bindObservers()
            }

            private fun validateUserInput(): Pair<Boolean, String> {
                val emailAddress = binding.txtEmail.text.toString()
                val userName = binding.txtUsername.text.toString()
                val password = binding.txtPassword.text.toString()
                return authViewModel.validateCredentials(emailAddress, userName, password, false)
            }

            private fun showValidationErrors(error: String) {
                binding.txtError.text = String.format(resources.getString(R.string.txt_error_message, error))
            }


            private fun getUserRequest(): UserRequest {
                return binding.run {
                    UserRequest(
                        txtEmail.text.toString(),
                        txtPassword.text.toString(),
                        txtUsername.text.toString()
                    )
                }
            }

            private fun bindObservers() {
                authViewModel.userResponseLiveData.observe(viewLifecycleOwner, Observer {
                    binding.progressBar.isVisible = false
                    when (it) {
                        is NetworkResult.Success -> {
                            tokenManager.saveToken(it.data!!.token)
                            findNavController().navigate(R.id.action_registerFragment_to_mainFragment)
                        }
                        is NetworkResult.Error -> {
                            showValidationErrors(it.message.toString())
                        }
                        is NetworkResult.Loading ->{
                            binding.progressBar.isVisible = true
                        }
                    }
                })
            }


            override fun onDestroyView() {
                super.onDestroyView()
                _binding = null
            }
        }

        @AndroidEntryPoint
        class MainFragment : Fragment() {

            private var _binding: FragmentMainBinding? = null
            private val binding get() = _binding!!
            private val noteViewModel by viewModels<NoteViewModel>()

            private lateinit var adapter: NoteAdapter

            override fun onCreateView(
                inflater: LayoutInflater, container: ViewGroup?,
                savedInstanceState: Bundle?
            ): View? {
                _binding = FragmentMainBinding.inflate(inflater, container, false)
                adapter = NoteAdapter(::onNoteClicked)
                return binding.root
            }

            override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)
                noteViewModel.getAllNotes()
                binding.noteList.layoutManager =
                    StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                binding.noteList.adapter = adapter
                binding.addNote.setOnClickListener {
                    findNavController().navigate(R.id.action_mainFragment_to_noteFragment)
                }
                bindObservers()
            }

            private fun bindObservers() {
                noteViewModel.notesLiveData.observe(viewLifecycleOwner, Observer {
                    binding.progressBar.isVisible = false
                    when (it) {
                        is NetworkResult.Success -> {
                            adapter.submitList(it.data)
                        }
                        is NetworkResult.Error -> {
                            Toast.makeText(requireContext(), it.message.toString(), Toast.LENGTH_SHORT)
                                .show()
                        }
                        is NetworkResult.Loading -> {
                            binding.progressBar.isVisible = true
                        }
                    }
                })
            }

            private fun onNoteClicked(noteResponse: NoteResponse){
                val bundle = Bundle()
                bundle.putString("note", Gson().toJson(noteResponse))
                findNavController().navigate(R.id.action_mainFragment_to_noteFragment, bundle)
            }

            override fun onDestroyView() {
                super.onDestroyView()
                _binding = null
            }

        }



        class NoteAdapter(private val onNoteClicked: (NoteResponse) -> Unit) :
            ListAdapter<NoteResponse, NoteAdapter.NoteViewHolder>(ComparatorDiffUtil()) {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
                val binding = NoteItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return NoteViewHolder(binding)
            }

            override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
                val note = getItem(position)
                note?.let {
                    holder.bind(it)
                }
            }

            inner class NoteViewHolder(private val binding: NoteItemBinding) :
                RecyclerView.ViewHolder(binding.root) {

                fun bind(note: NoteResponse) {
                    binding.title.text = note.title
                    binding.desc.text = note.description
                    binding.root.setOnClickListener {
                        onNoteClicked(note)
                    }
                }

            }

            class ComparatorDiffUtil : DiffUtil.ItemCallback<NoteResponse>() {
                override fun areItemsTheSame(oldItem: NoteResponse, newItem: NoteResponse): Boolean {
                    return oldItem._id == newItem._id
                }

                override fun areContentsTheSame(oldItem: NoteResponse, newItem: NoteResponse): Boolean {
                    return oldItem == newItem
                }
            }
        }


        @AndroidEntryPoint
        class NoteFragment : Fragment() {

            private var _binding: FragmentNoteBinding? = null
            private val binding get() = _binding!!
            private val noteViewModel by viewModels<NoteViewModel>()
            private var note: NoteResponse? = null

            override fun onCreateView(
                inflater: LayoutInflater, container: ViewGroup?,
                savedInstanceState: Bundle?
            ): View? {
                _binding = FragmentNoteBinding.inflate(inflater, container, false)
                return binding.root
            }

            override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)
                setInitialData()
                bindHandlers()
                bindObservers()
            }

            private fun bindObservers() {
                noteViewModel.statusLiveData.observe(viewLifecycleOwner, Observer {
                    when (it) {
                        is NetworkResult.Success -> {
                            findNavController().popBackStack()
                        }
                        is NetworkResult.Error -> {

                        }
                        is NetworkResult.Loading -> {

                        }
                    }
                })
            }

            private fun bindHandlers() {
                binding.btnDelete.setOnClickListener {
                    note?.let { noteViewModel.deleteNote(it!!._id) }
                }
                binding.apply {
                    btnSubmit.setOnClickListener {
                        val title = txtTitle.text.toString()
                        val description = txtDescription.text.toString()
                        val noteRequest = NoteRequest(title, description)
                        if (note == null) {
                            noteViewModel.createNote(noteRequest)
                        } else {
                            noteViewModel.updateNote(note!!._id, noteRequest)
                        }
                    }
                }
            }

            private fun setInitialData() {
                val jsonNote = arguments?.getString("note")
                if (jsonNote != null) {
                    note = Gson().fromJson<NoteResponse>(jsonNote, NoteResponse::class.java)
                    note?.let {
                        binding.txtTitle.setText(it.title)
                        binding.txtDescription.setText(it.description)
                    }
                }
                else{
                    binding.addEditText.text = resources.getString(R.string.add_note)
                }
            }


            override fun onDestroyView() {
                super.onDestroyView()
                _binding = null
            }

        }


        @HiltViewModel
        class NoteViewModel @Inject constructor(private val noteRepository: NoteRepository) : ViewModel() {

            val notesLiveData get() = noteRepository.notesLiveData
            val statusLiveData get() = noteRepository.statusLiveData

            fun createNote(noteRequest: NoteRequest) {
                viewModelScope.launch {
                    noteRepository.createNote(noteRequest)
                }
            }

            fun getAllNotes() {
                viewModelScope.launch {
                    noteRepository.getNotes()
                }
            }

            fun updateNote(id: String, noteRequest: NoteRequest){
                viewModelScope.launch {
                    noteRepository.updateNote(id, noteRequest)
                }
            }

            fun deleteNote(noteId: String) {
                viewModelScope.launch {
                    noteRepository.deleteNote(noteId)
                }
            }

        }

        class ProfileFragment : Fragment() {

            override fun onCreateView(
                inflater: LayoutInflater, container: ViewGroup?,
                savedInstanceState: Bundle?
            ): View? {
                return inflater.inflate(R.layout.fragment_profile, container, false)
            }

        }


        <?xml version="1.0" encoding="utf-8"?>
        <ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scrollbars="none">

        <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:layout_marginLeft="30dp"
        android:layout_marginRight="30dp"
        android:paddingTop="32dp"
        android:paddingBottom="60dp">

        <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:fontFamily="@font/sf_pro_display_bold"
        android:text="@string/txt_login_header"
        android:textColor="@color/black"
        android:textSize="30sp" />

        <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:fontFamily="@font/sf_pro_display_regular"
        android:text="@string/txt_login_subheader"
        android:textColor="@color/light_gray"
        android:textSize="16sp" />

        <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="72dp"
        android:fontFamily="@font/sf_pro_display_regular"
        android:text="@string/txt_email"
        android:textColor="@color/black"
        android:textSize="12sp" />

        <EditText
        android:id="@+id/txt_email"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="7dp"
        android:background="@android:color/transparent"
        android:fontFamily="@font/sf_pro_display_semibold"
        android:hint="@string/txt_email_hint"
        android:inputType="textEmailAddress"
        android:textColor="@color/black"
        android:textColorHint="@color/light_gray"
        android:textSize="16sp" />

        <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:layout_marginTop="16dp"
        android:background="@color/light_gray" />

        <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="20dp"
        android:fontFamily="@font/sf_pro_display_regular"
        android:text="@string/txt_password"
        android:textColor="@color/black"
        android:textSize="12sp" />

        <EditText
        android:id="@+id/txt_password"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="7dp"
        android:background="@android:color/transparent"
        android:fontFamily="@font/sf_pro_display_semibold"
        android:hint="@string/txt_password_hint"
        android:inputType="textPassword"
        android:textColor="@color/black"
        android:textColorHint="@color/light_gray"
        android:textSize="16sp" />

        <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:layout_marginTop="16dp"
        android:background="@color/light_gray" />

        <TextView
        android:id="@+id/txt_error"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="20dp"
        android:fontFamily="@font/sf_pro_display_regular"
        android:text=""
        android:textColor="@color/highlight_text_color"
        android:textSize="12sp" />

        <Button
        android:id="@+id/btn_login"
        style="@style/Widget.Material3.Button"
        android:backgroundTint="@color/black"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="30dp"
        android:fontFamily="@font/sf_pro_display_bold"
        android:text="@string/txt_login"
        android:textAllCaps="true"
        android:textColor="@color/white"
        android:textSize="16sp" />

        <TextView
        android:id="@+id/btn_sign_up"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:layout_marginTop="20dp"
        android:fontFamily="@font/sf_pro_display_semibold"
        android:text="@string/txt_no_account_signup"
        android:textSize="14sp" />

        <com.github.ybq.android.spinkit.SpinKitView
        android:visibility="gone"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        android:id="@+id/progress_bar"
        android:indeterminate="true"
        style="@style/SpinKitView.ThreeBounce"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        app:SpinKit_Color="@color/black" />

        </LinearLayout>


        </ScrollView>
