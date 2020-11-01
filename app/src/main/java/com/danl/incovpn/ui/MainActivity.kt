package com.danl.incovpn.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.clear
import coil.decode.SvgDecoder
import coil.load
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.customListAdapter
import com.danl.incovpn.IncoVPNApp.Companion.BASE_URL
import com.danl.incovpn.IncoVPNApp.Companion.KEY_CONNECT_AT_START
import com.danl.incovpn.R
import com.danl.incovpn.databinding.ActivityMainBinding
import com.danl.incovpn.databinding.CountryListItemBinding
import com.danl.incovpn.util.*
import com.danl.viewbindinghelper.ViewBindingActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint
import de.blinkt.openvpn.core.*
import splitties.activities.start
import splitties.resources.appStr
import splitties.resources.str
import splitties.snackbar.snack
import splitties.systemservices.layoutInflater
import splitties.views.onClick
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.concurrent.schedule

@AndroidEntryPoint
class MainActivity : ViewBindingActivity<ActivityMainBinding>() {

    companion object {
        private const val START_VPN_PROFILE = 70
        private var connectedCountry: String? = null
    }

    private val viewModel by viewModels<MainViewModel>()
    private val timer = Timer()
    private val isConnected: Boolean
        get() = viewModel.status.value == ConnectionStatus.LEVEL_CONNECTED

    private var countriesDialog: MaterialDialog? = null
    private var service: IOpenVPNServiceInternal? = null

    private lateinit var connectionTimeTask: TimerTask

    @Inject
    lateinit var preferences: SharedPreferences

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            this@MainActivity.service = IOpenVPNServiceInternal.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            service = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, OpenVPNService::class.java)
        intent.action = OpenVPNService.START_SERVICE
        bindService(intent, connection, BIND_AUTO_CREATE)
        connectionTimeTask = timer.schedule(0, 1000) {
            val connectTime = service?.connectTime
            if (connectTime == null || connectTime == 0L || connectedCountry == null || !isConnected) return@schedule
            val time = System.currentTimeMillis() - connectTime
            val hours = TimeUnit.MILLISECONDS.toHours(time)
            val minutes =
                TimeUnit.MILLISECONDS.toMinutes(time) - TimeUnit.HOURS.toMinutes(hours)
            val seconds =
                TimeUnit.MILLISECONDS.toSeconds(time) -
                        TimeUnit.HOURS.toSeconds(hours) -
                        TimeUnit.MINUTES.toSeconds(minutes)
            binding.timeTextView.post {
                binding.timeTextView.text =
                    str(
                        R.string.connection_time_format,
                        connectedCountry,
                        hours,
                        minutes,
                        seconds
                    )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        unbindService(connection)
        connectionTimeTask.cancel()
    }

    override fun onBindingCreated(savedInstanceState: Bundle?) {
        MobileAds.initialize(this)
        binding.adView.loadAd(AdRequest.Builder().build())
        binding.settingsButton.onClick {
            start<SettingsActivity>()
        }
        binding.country.onClick {
            countriesDialog?.show()
        }
        binding.actionButton.onClick {
            if (viewModel.status.value == ConnectionStatus.LEVEL_NOTCONNECTED) {
                viewModel.createProfile()
            } else {
                ProfileManager.setConntectedVpnProfileDisconnected(this)
                try {
                    service?.stopVPN(false)
                } catch (e: RemoteException) {
                    VpnStatus.logException(e)
                }
            }
        }
        viewModel.countries.observe(this) { res ->
            res.success { countries ->
                countriesDialog = MaterialDialog(this).apply {
                    title(text = str(R.string.select_country))
                    customListAdapter(CountryAdapter(countries) {
                        dismiss()
                        viewModel.selectCountry(it)
                    })
                }
            }
            res.error {
                binding.root.snack(it) {
                    view.translationY = -binding.adView.height.toFloat()
                }
            }
        }
        viewModel.selectedCountry.observe(this) {
            if (it == null) {
                binding.countryImageView.load(R.drawable.ic_worldwide)
                binding.countryNameTextView.text = str(R.string.any)
            } else {
                binding.countryImageView.load(it.toImageUrl()) {
                    decoder(SvgDecoder(this@MainActivity))
                }
                binding.countryNameTextView.text = it.toDisplayCountry()
            }
        }
        viewModel.connect.observe(this, EventObserver { res ->
            res.success {
                connectedCountry = it.country.toDisplayCountry()
                binding.connectingTextView.text = str(R.string.connecting_format, connectedCountry)
                binding.timeTextView.text = str(R.string.connection_time_format, connectedCountry, 0, 0, 0)
                ProfileManager.getInstance(this).addProfile(viewModel.profile)
                val intent = VpnService.prepare(this)

                if (intent != null) {
                    VpnStatus.updateStateString(
                        "USER_VPN_PERMISSION", "", R.string.state_user_vpn_permission,
                        ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT
                    )
                    startActivityForResult(intent, START_VPN_PROFILE)
                } else {
                    onActivityResult(START_VPN_PROFILE, RESULT_OK, null)
                }
            }
            res.error {
                binding.root.snack(it) {
                    view.translationY = -binding.adView.height.toFloat()
                }
            }
        })
        viewModel.status.observe(this) {
            binding.connectionLayout.isVisible = isConnected
            binding.connectingTextView.isVisible =
                it != ConnectionStatus.LEVEL_CONNECTED &&
                        it != ConnectionStatus.LEVEL_NOTCONNECTED &&
                        it != ConnectionStatus.LEVEL_VPNPAUSED
            when (it) {
                ConnectionStatus.LEVEL_CONNECTED, ConnectionStatus.LEVEL_VPNPAUSED -> {
                    animateIncognito(1f)
                    binding.actionButton.text = str(R.string.disconnect_action)
                }
                ConnectionStatus.LEVEL_NOTCONNECTED -> {
                    animateIncognito(0.25f)
                    binding.actionButton.text = str(R.string.connect_action)
                    connectedCountry = null
                }
                else -> {
                    animateIncognito(0.25f, 0.5f)
                    binding.actionButton.text = str(R.string.disconnect_action)
                }
            }
        }
        viewModel.byteCount.observe(this) {
            val inString = OpenVPNService.humanReadableByteCount(it.first, false, resources)
            val outString = OpenVPNService.humanReadableByteCount(it.second, false, resources)
            binding.inTextView.text = inString
            binding.outTextView.text = outString
        }
        if (preferences.getBoolean(KEY_CONNECT_AT_START, false) && !VpnStatus.isVPNActive()) {
            viewModel.createProfile()
        }
    }

    private fun animateIncognito(alpha1: Float, alpha2: Float? = null) {
        if (!isDestroyed) {
            binding.incognitoImageView.animate()
                .setDuration(1000)
                .withEndAction(alpha2?.let {
                    Runnable {
                        animateIncognito(it, alpha1)
                    }
                })
                .alpha(alpha1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == START_VPN_PROFILE) {
            if (resultCode == RESULT_OK) {
                VPNLaunchHelper.startOpenVpn(viewModel.profile, baseContext)
            } else if (resultCode == RESULT_CANCELED) {
                VpnStatus.updateStateString(
                    "USER_VPN_PERMISSION_CANCELLED",
                    "",
                    R.string.state_user_vpn_permission_cancelled,
                    ConnectionStatus.LEVEL_NOTCONNECTED
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) VpnStatus.logError(R.string.nought_alwayson_warning)
            }
        }
    }

    private class CountryAdapter(val countries: List<String>, val onSelect: (String?) -> Unit) :
        RecyclerView.Adapter<CountryAdapter.ViewHolder>() {

        inner class ViewHolder(private val binding: CountryListItemBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(country: String?) {
                if (country != null) {
                    binding.imageView.load(country.toImageUrl()) {
                        decoder(SvgDecoder(binding.imageView.context))
                    }
                    binding.nameTextView.text = country.toDisplayCountry()
                } else {
                    binding.imageView.load(R.drawable.ic_worldwide)
                    binding.nameTextView.text = appStr(R.string.any)
                }
                binding.root.onClick {
                    onSelect(country)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
            CountryListItemBinding.inflate(parent.layoutInflater, parent, false)
        )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (position == 0) {
                holder.bind(null)
            } else {
                holder.bind(countries[position - 1])
            }
        }

        override fun getItemCount() = countries.size + 1
    }
}