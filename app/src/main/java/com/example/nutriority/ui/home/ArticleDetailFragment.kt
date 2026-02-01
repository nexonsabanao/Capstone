package com.example.nutriority.ui.home

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.nutriority.R
import com.example.nutriority.data.model.Article
import com.example.nutriority.databinding.FragmentArticleDetailBinding
import com.example.nutriority.ui.NavigationViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
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

        binding.btnBack.setOnClickListener {
            navigationViewModel.goBack()
        }

        binding.tvToolbarTitle.alpha = 0f
        binding.toolbar.setBackgroundColor(Color.TRANSPARENT)

        binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            if (totalScrollRange == 0) return@OnOffsetChangedListener

            val percentage = abs(verticalOffset).toFloat() / totalScrollRange
            val startFadeAt = 0.8f
            if (percentage > startFadeAt) {
                val alphaProgress = (percentage - startFadeAt) / (1f - startFadeAt)
                val alphaInt = (alphaProgress * 255).toInt().coerceIn(0, 255)
                binding.toolbar.setBackgroundColor(Color.argb(alphaInt, 255, 255, 255))
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
            binding.articleCategory.text = article.category.uppercase()
            binding.articleAuthor.text = article.author
            binding.articleSource.text = article.source.ifEmpty { "Wellness" }
            
            // Format the date string (e.g., "2026-01-30T00:53:23Z" -> "Jan 30, 2026")
            if (article.date.isNotEmpty()) {
                try {
                    val sdfIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    val sdfOut = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                    val date = sdfIn.parse(article.date)
                    binding.articleDate.text = if (date != null) sdfOut.format(date) else article.date
                } catch (e: Exception) {
                    binding.articleDate.text = article.date
                }
            }

            binding.articleDescription.text = article.description
            binding.articleContent.text = article.content

            // Make the source/URL link clickable
            if (article.articleUrl.isNotEmpty()) {
                binding.articleUrl.text = "Read full article on ${article.source.ifEmpty { "Source" }}"
                binding.articleUrl.setOnClickListener {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.articleUrl))
                        startActivity(intent)
                    } catch (e: Exception) {
                        // Handle error
                    }
                }
                binding.articleUrl.visibility = View.VISIBLE
            } else {
                binding.articleUrl.visibility = View.GONE
            }

            Glide.with(this)
                .load(article.imageName)
                .centerCrop()
                .placeholder(R.drawable.img_balanced_diet)
                .error(R.drawable.img_balanced_diet)
                .into(binding.articleImage)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
