package com.example.budgettingtogether.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettingtogether.R
import com.example.budgettingtogether.core.AppDatabase
import com.example.budgettingtogether.databinding.ActivityPairingBinding
import com.example.budgettingtogether.databinding.ItemUserSearchResultBinding
import kotlinx.coroutines.launch

class PairingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPairingBinding
    private lateinit var pairingRepository: PairingRepository
    private lateinit var sessionManager: SessionManager
    private val currentUserId: String get() = sessionManager.getUserId() ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPairingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)
        pairingRepository = PairingRepository(database.userDao(), database.userPairingDao())

        setupToolbar()
        setupSearch()
        loadPairings()
    }

    override fun onResume() {
        super.onResume()
        loadPairings()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.pairing_title)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupSearch() {
        binding.buttonSearch.setOnClickListener { performSearch() }
        binding.editTextSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }
    }

    private fun performSearch() {
        val query = binding.editTextSearch.text.toString().trim()
        if (query.isEmpty()) return

        lifecycleScope.launch {
            val results = pairingRepository.searchUsers(query, currentUserId)
            showSearchResults(results)
        }
    }

    private fun showSearchResults(users: List<User>) {
        val adapter = SearchResultAdapter(users) { user ->
            sendPairingRequest(user)
        }
        binding.recyclerViewSearchResults.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewSearchResults.adapter = adapter
    }

    private fun sendPairingRequest(user: User) {
        lifecycleScope.launch {
            val result = pairingRepository.sendPairingRequest(currentUserId, user.id)
            result.fold(
                onSuccess = {
                    Toast.makeText(this@PairingActivity, getString(R.string.request_sent, user.username), Toast.LENGTH_SHORT).show()
                    loadPairings()
                    performSearch()
                },
                onFailure = { e ->
                    Toast.makeText(this@PairingActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun loadPairings() {
        lifecycleScope.launch {
            loadPendingReceived()
            loadPendingSent()
            loadPartners()
        }
    }

    private suspend fun loadPendingReceived() {
        val requests = pairingRepository.getPendingReceivedRequests(currentUserId)
        binding.cardPendingReceived.visibility = if (requests.isEmpty()) View.GONE else View.VISIBLE

        binding.linearLayoutPendingReceived.removeAllViews()
        for (request in requests) {
            val requester = AppDatabase.getDatabase(this).userDao().getUserById(request.requesterId)
            val itemBinding = ItemUserSearchResultBinding.inflate(LayoutInflater.from(this), binding.linearLayoutPendingReceived, false)
            itemBinding.textViewUsername.text = requester?.username ?: request.requesterId
            itemBinding.textViewEmail.text = requester?.email ?: ""
            itemBinding.buttonAction.text = getString(R.string.accept)
            itemBinding.buttonAction.setOnClickListener {
                lifecycleScope.launch {
                    pairingRepository.acceptPairingRequest(request.id, currentUserId)
                    Toast.makeText(this@PairingActivity, R.string.request_accepted, Toast.LENGTH_SHORT).show()
                    loadPairings()
                }
            }
            binding.linearLayoutPendingReceived.addView(itemBinding.root)
        }
    }

    private suspend fun loadPendingSent() {
        val requests = pairingRepository.getPendingSentRequests(currentUserId)
        binding.cardPendingSent.visibility = if (requests.isEmpty()) View.GONE else View.VISIBLE

        binding.linearLayoutPendingSent.removeAllViews()
        for (request in requests) {
            val receiver = AppDatabase.getDatabase(this).userDao().getUserById(request.receiverId)
            val itemBinding = ItemUserSearchResultBinding.inflate(LayoutInflater.from(this), binding.linearLayoutPendingSent, false)
            itemBinding.textViewUsername.text = receiver?.username ?: request.receiverId
            itemBinding.textViewEmail.text = receiver?.email ?: ""
            itemBinding.buttonAction.text = getString(R.string.cancel)
            itemBinding.buttonAction.setOnClickListener {
                lifecycleScope.launch {
                    pairingRepository.rejectPairingRequest(request.id)
                    Toast.makeText(this@PairingActivity, R.string.request_cancelled, Toast.LENGTH_SHORT).show()
                    loadPairings()
                }
            }
            binding.linearLayoutPendingSent.addView(itemBinding.root)
        }
    }

    private suspend fun loadPartners() {
        val partners = pairingRepository.getPartners(currentUserId)

        if (partners.isEmpty()) {
            binding.textViewNoPartners.visibility = View.VISIBLE
            binding.linearLayoutPartners.removeAllViews()
            return
        }

        binding.textViewNoPartners.visibility = View.GONE
        binding.linearLayoutPartners.removeAllViews()

        for (partner in partners) {
            val itemBinding = ItemUserSearchResultBinding.inflate(LayoutInflater.from(this), binding.linearLayoutPartners, false)
            itemBinding.textViewUsername.text = partner.username
            itemBinding.textViewEmail.text = partner.email
            itemBinding.buttonAction.text = getString(R.string.unpair)
            itemBinding.buttonAction.setOnClickListener {
                confirmUnpair(partner)
            }
            binding.linearLayoutPartners.addView(itemBinding.root)
        }
    }

    private fun confirmUnpair(partner: User) {
        AlertDialog.Builder(this)
            .setTitle(R.string.unpair_confirm_title)
            .setMessage(getString(R.string.unpair_confirm_message, partner.username))
            .setPositiveButton(R.string.unpair) { _, _ ->
                lifecycleScope.launch {
                    pairingRepository.unpair(currentUserId, partner.id)
                    Toast.makeText(this@PairingActivity, getString(R.string.unpaired_success, partner.username), Toast.LENGTH_SHORT).show()
                    loadPairings()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private class SearchResultAdapter(
        private val users: List<User>,
        private val onSendRequest: (User) -> Unit
    ) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemUserSearchResultBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemUserSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]
            holder.binding.textViewUsername.text = user.username
            holder.binding.textViewEmail.text = user.email
            holder.binding.buttonAction.text = holder.itemView.context.getString(R.string.send_request)
            holder.binding.buttonAction.setOnClickListener { onSendRequest(user) }
        }

        override fun getItemCount() = users.size
    }
}
