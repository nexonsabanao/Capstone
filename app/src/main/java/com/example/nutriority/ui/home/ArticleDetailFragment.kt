package com.example.nutriority.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nutriority.data.model.Article
import com.example.nutriority.databinding.FragmentArticleDetailBinding
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ArticleDetailFragment : Fragment() {

    private var _binding: FragmentArticleDetailBinding? = null
    private val binding get() = _binding!!
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

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Get article data from arguments
        val articleJson = arguments?.getString("article_json")
        if (articleJson != null) {
            currentArticle = Gson().fromJson(articleJson, Article::class.java)
            displayArticleDetails()
        }
    }

    private fun displayArticleDetails() {
        currentArticle?.let { article ->
            binding.collapsingToolbar.title = article.title
            binding.articleTitle.text = article.title
            binding.articleAuthor.text = article.author
            binding.articleReadingTime.text = article.readingTime
            binding.articleCategory.text = article.category
            
            val resId = resources.getIdentifier(article.imageName, "drawable", requireContext().packageName)
            if (resId != 0) {
                binding.articleImage.setImageResource(resId)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
