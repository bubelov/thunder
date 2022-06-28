package activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.bubelov.thunder.R
import com.bubelov.thunder.databinding.ActivityBinding

class Activity : AppCompatActivity() {

    private lateinit var binding: ActivityBinding

    private val navController by lazy {
        findNavController(R.id.nav_host_fragment)
    }

    private val destinationsWithBottomBar = arrayOf(
        R.id.paymentsFragment,
        R.id.channelsFragment,
        R.id.nodeFragment,
    )

    private val navListener = NavController.OnDestinationChangedListener { _, destination, _ ->
        binding.bottomNavigation.isVisible = destinationsWithBottomBar.contains(destination.id)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }

    override fun onStart() {
        super.onStart()
        navController.addOnDestinationChangedListener(navListener)
        binding.bottomNavigation.setupWithNavController(navController)
    }
}