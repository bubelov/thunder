package activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.navOptions
import com.bubelov.thunder.R
import com.bubelov.thunder.databinding.ActivityBinding
import conf.ConfRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get

class Activity : AppCompatActivity() {

    private lateinit var binding: ActivityBinding

    private val navController by lazy {
        findNavController(R.id.nav_host_fragment)
    }

    private val navListener = NavController.OnDestinationChangedListener { _, destination, _ ->
        binding.bottomNavigation.isVisible = destination.id != R.id.authFragment
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

    override fun onStart() {
        super.onStart()

        navController.addOnDestinationChangedListener(navListener)

        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                val conf = get<ConfRepo>().load().first()

                if (
                    conf.serverUrl.isBlank()
                    || conf.serverCertificate.isBlank()
                    || conf.clientCertificate.isBlank()
                    || conf.clientPrivateKey.isBlank()
                ) {
                    withContext(Dispatchers.Main) {
                        navController.navigate(R.id.authFragment, null, navOptions {
                            popUpTo(R.id.paymentsFragment) { inclusive = true }
                        })
                    }
                }
            }
        }
    }
}