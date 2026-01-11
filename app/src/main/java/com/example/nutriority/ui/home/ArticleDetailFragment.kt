package com.example.nutriority.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.nutriority.data.model.Article
import com.example.nutriority.databinding.FragmentArticleDetailBinding
import com.example.nutriority.ui.NavigationViewModel
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ArticleDetailFragment : Fragment() {

    private var _binding: FragmentArticleDetailBinding? = null
    private val binding get() = _binding!!
    
    private val navigationViewModel: NavigationViewModel by activityViewModels()
    private var currentArticle: Article? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArticleDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up toolbar back button
        binding.toolbar.setNavigationOnClickListener {
            navigationViewModel.goBack()
        }

        observeArticleData()
    }

    private fun observeArticleData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigationViewModel.selectedArticleJson.collect { json ->
                    if (json != null) {
                        currentArticle = Gson().fromJson(json, Article::class.java)
                        displayArticleDetails()
                        // Scroll to top
                        binding.nestedScrollView.scrollTo(0, 0)
                    }
                }
            }
        }
    }

    private fun displayArticleDetails() {
        currentArticle?.let { article ->
            binding.articleTitle.text = article.title
            binding.articleContent.text = article.content
            if (article.imageResId != 0) {
                binding.articleImage.setImageResource(article.imageResId)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
