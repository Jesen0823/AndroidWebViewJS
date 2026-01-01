package org.dev.jesen.androidwebviewjs

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.dev.jesen.androidwebviewjs.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    companion object {
        // 简单WebView页面
        const val ACTION_OPEN_SIMPLE = "org.dev.jesen.androidwebviewjs.OPEN_SIMPLE"
        const val CATEGORY_SIMPLE_PAGE = "org.dev.jesen.androidwebviewjs.SIMPLE_PAGE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()
    }

    private fun initView() {
        binding.webViewSimpleBtn.setOnClickListener {
            jumpState1ToSimple()
        }
    }

    private fun jumpState1ToSimple() {
        val intent = Intent().also {
            it.action = ACTION_OPEN_SIMPLE
            it.addCategory(CATEGORY_SIMPLE_PAGE)
            it.putExtra("title", "Stage1:简单加载WebView")
            it.putExtra("content", "通过 Action/Category 跳转的简单WebView")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // 无匹配 Activity 时的容错处理（如提示用户）
            binding.tvTip.text = "未找到可跳转的页面，请检查配置"
        }
    }
}