package org.dev.jesen.advancewebview

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.dev.jesen.advancewebview.databinding.ActivityMainAdvanceBinding
import org.dev.jesen.advancewebview.ui.AdvanceBasicFunctionActivity
import org.dev.jesen.advancewebview.ui.AdvanceCacheStrategyActivity
import org.dev.jesen.advancewebview.ui.AdvancePerformanceOptActivity
import org.dev.jesen.advancewebview.ui.AdvanceSecurityDefenseActivity

/**
 *  主入口 Activity（跳转各个测试页面，独立无依赖）
 */
class AdvanceMainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainAdvanceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainAdvanceBinding.inflate(layoutInflater)

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
        // 绑定点击事件（跳转各个测试页面）
        binding.btnBasicFunction.setOnClickListener {
            startActivity(Intent(this, AdvanceBasicFunctionActivity::class.java))
        }

        binding.btnPerformanceOpt.setOnClickListener {
            startActivity(Intent(this, AdvancePerformanceOptActivity::class.java))
        }

        binding.btnCacheStrategy.setOnClickListener {
            startActivity(Intent(this, AdvanceCacheStrategyActivity::class.java))
        }

        binding.btnSecurityDefense.setOnClickListener {
            startActivity(Intent(this, AdvanceSecurityDefenseActivity::class.java))
        }
    }
}