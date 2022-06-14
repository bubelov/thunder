package activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.bubelov.thunder.R
import com.bubelov.thunder.databinding.ActivityBinding

class Activity : AppCompatActivity() {

    private lateinit var binding: ActivityBinding

    private val navController by lazy {
        findNavController(R.id.nav_host_fragment)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.paymentsFragment -> {
                    navController.navigate(R.id.paymentsFragment)
                }

                R.id.channelsFragment -> {
                    navController.navigate(R.id.channelsFragment)
                }

                R.id.nodeFragment -> {
                    navController.navigate(R.id.nodeFragment)
                }
            }

            true
        }
    }
}