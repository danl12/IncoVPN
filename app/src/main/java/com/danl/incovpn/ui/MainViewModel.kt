package com.danl.incovpn.ui

import android.content.Intent
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.danl.incovpn.IncoVPNApp.Companion.KEY_SAVE_COUNTRY
import com.danl.incovpn.IncoVPNApp.Companion.KEY_SELECTED_COUNTRY
import com.danl.incovpn.data.ServerRepository
import com.danl.incovpn.data.model.Resource
import com.danl.incovpn.data.model.Server
import com.danl.incovpn.util.Event
import com.danl.incovpn.util.toDisplayCountry
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.VpnStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

class MainViewModel @ViewModelInject constructor(
    private val serverRepository: ServerRepository,
    private val preferences: SharedPreferences
) : ViewModel(), VpnStatus.StateListener,
    VpnStatus.ByteCountListener {

    lateinit var profile: VpnProfile
        private set

    private val _selectedCountry = MutableLiveData<String?>(
        if (preferences.getBoolean(KEY_SAVE_COUNTRY, true))
            preferences.getString(
                KEY_SELECTED_COUNTRY, null
            ) else null
    )
    val selectedCountry: LiveData<String?>
        get() = _selectedCountry

    private val _status = MutableLiveData<ConnectionStatus>()
    val status: LiveData<ConnectionStatus>
        get() = _status

    private val _connect = MutableLiveData<Event<Resource<Server>>>()
    val connect: LiveData<Event<Resource<Server>>>
        get() = _connect

    private val _byteCount = MutableLiveData<Pair<Long, Long>>()
    val byteCount: LiveData<Pair<Long, Long>>
        get() = _byteCount

    val countries = liveData {
        emit(getResource {
            try {
                Resource.Success(serverRepository.getCountries())
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Unknown error.")
            }
        })
    }

    init {
        VpnStatus.addStateListener(this)
        VpnStatus.addByteCountListener(this)
    }

    fun selectCountry(country: String?) {
        _selectedCountry.value = country
        preferences.edit {
            putString(KEY_SELECTED_COUNTRY, country)
        }
        if (status.value == ConnectionStatus.LEVEL_CONNECTED) {
            createProfile()
        }
    }

    private var createProfileJob: Job? = null

    private suspend fun <T> getResource(get: suspend () -> Resource<T>): Resource<T> {
        var res: Resource<T>? = null
        for (i in 0 until 3) {
            res = get()

            if (res is Resource.Success) break
        }
        return res!!
    }

    fun createProfile() {
        createProfileJob?.cancel()
        createProfileJob = viewModelScope.launch(context = Dispatchers.IO) {
            val res = getResource {
                try {
                    Resource.Success(serverRepository.get(selectedCountry.value))
                } catch (e: Exception) {
                    Resource.Error(e.message ?: "Unknown error.")
                }
            }
            res.success { server ->
                val configParser = ConfigParser()
                configParser.parseConfig(
                    InputStreamReader(
                        ByteArrayInputStream(
                            Base64.decode(
                                server.config,
                                Base64.DEFAULT
                            )
                        )
                    )
                )
                profile = configParser.convertProfile()
                profile.mName = server.country.toDisplayCountry()
            }
            _connect.postValue(Event(res))
        }
    }

    override fun onCleared() {
        VpnStatus.removeStateListener(this)
        VpnStatus.removeByteCountListener(this)
    }

    override fun updateState(
        state: String?,
        logmessage: String?,
        localizedResId: Int,
        level: ConnectionStatus,
        Intent: Intent?
    ) {
        _status.postValue(level)
    }

    override fun setConnectedVPN(uuid: String?) {

    }

    override fun updateByteCount(`in`: Long, out: Long, diffIn: Long, diffOut: Long) {
        _byteCount.postValue(Pair(`in`, out))
    }
}