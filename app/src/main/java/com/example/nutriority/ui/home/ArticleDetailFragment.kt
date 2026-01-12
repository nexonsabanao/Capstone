package com.example.nutriority.ui.home

import android.graphics.Color
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
import com.google.android.material.appbar.AppBarLayout
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.abs

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

        // Set up custom back button listener
        binding.btnBack.setOnClickListener {
            navigationViewModel.goBack()
        }

        // Initially hide the toolbar title and background
        binding.tvToolbarTitle.alpha = 0f
        binding.toolbar.setBackgroundColor(Color.TRANSPARENT)

        // Handle app bar collapse state with a smooth fade effect
        binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            if (totalScrollRange == 0) return@OnOffsetChangedListener

            val percentage = abs(verticalOffset).toFloat() / totalScrollRange

            // Start fading in the background and title during the last 20% of scroll
            val startFadeAt = 0.8f
            if (percentage > startFadeAt) {
                // Map the 0.8 -> 1.0 range to 0.0 -> 1.0
                val alphaProgress = (percentage - startFadeAt) / (1f - startFadeAt)
                val alphaInt = (alphaProgress * 255).toInt().coerceIn(0, 255)

                // Set white background with calculated alpha
                binding.toolbar.setBackgroundColor(Color.argb(alphaInt, 255, 255, 255))

                // Fade in the title
                binding.tvToolbarTitle.alpha = alphaProgress
            } else {
                binding.toolbar.setBackgroundColor(Color.TRANSPARENT)
                binding.tvToolbarTitle.alpha = 0f
            }
        })

        observeArticleData()
    }

    private fun observeArticleData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigationViewModel.selectedArticleJson.collect { json ->
                    if (json != null) {
                        currentArticle = Gson().fromJson(json, Article::class.java)
                        displayArticleDetails()
                        // Scroll to top and ensure app bar is expanded
                        binding.nestedScrollView.scrollTo(0, 0)
                        binding.appBarLayout.setExpanded(true)
                    }
                }
            }
        }
    }

    private fun displayArticleDetails() {
        currentArticle?.let { article ->
            binding.articleTitle.text = article.title
            binding.tvToolbarTitle.text = article.title
            binding.articleCategory.text = article.category
            binding.articleAuthor.text = article.author
            binding.articleReadingTime.text = article.readingTime
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
