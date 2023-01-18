package any.ui.home.following

import android.content.Context
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import any.data.entity.ServiceManifest
import any.data.entity.User
import any.data.repository.ServiceRepository
import any.data.repository.UserRepository
import any.domain.entity.UiUser
import any.domain.user.toUiUser
import any.richtext.html.DefaultHtmlParser
import any.richtext.html.HtmlParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FollowingViewModel(
    private val userRepository: UserRepository,
    private val serviceRepository: ServiceRepository,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val htmlParser: HtmlParser = DefaultHtmlParser(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(FollowingUiState())
    val uiState: StateFlow<FollowingUiState> = _uiState

    private var allFollowingUsers: List<UiUser>? = null

    init {
        viewModelScope.launch {
            userRepository.changes.collect { fetchFollowingUsers() }
        }
    }

    fun fetchFollowingUsers() {
        viewModelScope.launch(workerDispatcher) {
            userRepository.fetchFollowing(serviceId = null)
                .onStart {
                    _uiState.update {
                        it.copy(isLoading = true)
                    }
                }
                .collect { onFetchedUsers(it) }
        }
    }

    private suspend fun onFetchedUsers(users: List<User>) {
        val uiUsers = users.toFollowingUser()
        allFollowingUsers = uiUsers
        val result = filterUsers(
            users = uiUsers,
            services = uiState.value.services,
        )
        _uiState.update {
            it.copy(
                services = result.services,
                users = result.users,
                isLoading = false,
                showEmpty = result.users.isEmpty(),
            )
        }
    }

    private fun createServiceListFromUsers(
        users: List<UiUser>,
        currServices: List<ServiceOfFollowingUsers>,
    ): List<ServiceOfFollowingUsers> {
        val currServiceMap = currServices.associateBy { it.id }
        val services = mutableMapOf<String, ServiceOfFollowingUsers>()
        for (user in users) {
            val service = services.getOrPut(user.serviceId) {
                ServiceOfFollowingUsers(
                    id = user.serviceId,
                    name = user.serviceName ?: "???",
                    icon = user.serviceIcon,
                    themeColor = user.serviceThemeColor,
                    darkThemeColor = user.serviceDarkThemeColor,
                    isSelected = currServiceMap[user.serviceId]?.isSelected ?: false,
                )
            }
            services[user.serviceId] = service.copy(userCount = service.userCount + 1)
        }
        val serviceList = services.values.toMutableList()
        // Sort by name
        serviceList.sortBy { it.name }
        // Add the all item
        val selectAll = currServices.isEmpty() ||
                currServices.find { it.isAll }.let { it != null && it.isSelected } ||
                serviceList.find { it.isSelected } == null
        serviceList.add(
            0,
            ServiceOfFollowingUsers(
                userCount = users.size,
                isAll = true,
                isSelected = selectAll,
            )
        )
        return serviceList
    }

    fun updateFilterText(text: TextFieldValue) {
        _uiState.update {
            it.copy(filterText = text)
        }
        filterAndUpdateUsers(allFollowingUsers)
    }

    private fun filterAndUpdateUsers(
        users: List<UiUser>?,
        services: List<ServiceOfFollowingUsers> = uiState.value.services,
        filterText: String = uiState.value.filterText.text,
    ) {
        viewModelScope.launch(workerDispatcher) {
            val result = filterUsers(users, services, filterText)
            _uiState.update {
                it.copy(
                    services = result.services,
                    users = result.users,
                    showEmpty = result.users.isEmpty(),
                )
            }
        }
    }

    private fun filterUsers(
        users: List<UiUser>?,
        services: List<ServiceOfFollowingUsers> = uiState.value.services,
        filterText: String = uiState.value.filterText.text,
    ): FilterResult {
        if (users == null) {
            return FilterResult(users = emptyList(), services = services)
        }
        if (users.isEmpty()) {
            return FilterResult(users = emptyList(), services = services)
        }
        val byText = filterUsersByText(users, filterText)
        val bySelectedServices = filterUsersBySelectedServices(byText, services)
        val servicesFilteredByText = createServiceListFromUsers(
            users = byText,
            currServices = services,
        )
        return FilterResult(
            users = bySelectedServices,
            services = servicesFilteredByText,
        )
    }

    private fun filterUsersBySelectedServices(
        users: List<UiUser>?,
        services: List<ServiceOfFollowingUsers>,
    ): List<UiUser> {
        if (users == null) {
            return emptyList()
        }
        if (services.isEmpty()) {
            return users
        }
        val isAllSelected = services.find { it.isAll }?.isSelected == true
        if (isAllSelected) {
            return users
        }
        val selectedServiceIds = services.filter { it.isSelected }
            .map { it.id }
            .toSet()
        return users.filter { selectedServiceIds.contains(it.serviceId) }
    }

    private fun filterUsersByText(
        users: List<UiUser>?,
        text: String,
    ): List<UiUser> {
        if (users == null) {
            return emptyList()
        }
        if (text.isEmpty()) {
            return users
        }
        return users.filter {
            if (it.name.contains(text, ignoreCase = true)) {
                return@filter true
            }
            if (it.alternativeName?.contains(text, ignoreCase = true) == true) {
                return@filter true
            }
            if (it.id.contains(text, ignoreCase = true)) {
                return@filter true
            }
            false
        }
    }

    fun unfollowSelectedUsers() {
        val selected = uiState.value.selection
        if (selected.isEmpty()) {
            return
        }
        viewModelScope.launch {
            val updated = uiState.value.users
                .filter { selected.contains(it.id) }
                .map { u -> u.raw.markUnfollowed() }
            userRepository.update(updated)
            cancelUserSelection()
        }
    }

    fun selectUser(user: UiUser) {
        val selection = uiState.value.selection.toMutableSet()
        selection.add(user.id)
        _uiState.update { it.copy(selection = selection) }
    }

    fun selectAllUsers() {
        val selection = uiState.value.users
            .map { it.id }
            .toSet()
        _uiState.update { it.copy(selection = selection) }
    }

    fun unselectUser(user: UiUser) {
        val selection = uiState.value.selection.toMutableSet()
        selection.remove(user.id)
        _uiState.update { it.copy(selection = selection) }
    }

    fun cancelUserSelection() {
        _uiState.update { it.copy(selection = emptySet()) }
    }

    fun selectService(service: ServiceOfFollowingUsers) {
        if (service.isSelected) {
            return
        }
        viewModelScope.launch(workerDispatcher) {
            val users = filterUsersByText(
                users = allFollowingUsers,
                text = uiState.value.filterText.text
            )
            val services = createServiceListFromUsers(
                users = users,
                currServices = uiState.value.services
            ).toMutableList()
            if (services.isEmpty()) {
                return@launch
            }

            val idx = services.indexOf(service)
            if (idx == -1) {
                return@launch
            }

            if (service.isAll) {
                services[idx] = service.copy(isSelected = true)
                for (i in 1 until services.size) {
                    services[i] = services[i].copy(isSelected = false)
                }
            } else {
                services[idx] = service.copy(isSelected = true)
                updateAllServiceItemState(services)
            }

            filterAndUpdateUsers(users = allFollowingUsers, services = services)
        }
    }

    fun unselectService(service: ServiceOfFollowingUsers) {
        if (service.isAll || !service.isSelected) {
            return
        }
        viewModelScope.launch(workerDispatcher) {
            val users = filterUsersByText(
                users = allFollowingUsers,
                text = uiState.value.filterText.text
            )
            val services = createServiceListFromUsers(
                users = users,
                currServices = uiState.value.services
            ).toMutableList()
            if (services.isEmpty()) {
                return@launch
            }

            if (services.count { it.isSelected } == 1) {
                return@launch
            }

            val idx = services.indexOf(service)
            if (idx == -1) {
                return@launch
            }

            services[idx] = services[idx].copy(isSelected = false)
            updateAllServiceItemState(services)

            filterAndUpdateUsers(users = allFollowingUsers, services = services)
        }
    }

    private fun updateAllServiceItemState(
        services: MutableList<ServiceOfFollowingUsers>,
    ) {
        val allIdx = services.indexOfFirst { it.isAll }
        if (allIdx != -1) {
            var allItemSelected = services[allIdx].isSelected
            for ((idx, service) in services.withIndex()) {
                if (idx == allIdx) {
                    continue
                }
                if (service.isSelected) {
                    allItemSelected = false
                    break
                }
            }
            services[allIdx] = services[allIdx].copy(isSelected = allItemSelected)
        }
    }

    private suspend fun List<User>.toFollowingUser(): List<UiUser> {
        val serviceMap = mutableMapOf<String, ServiceManifest?>()
        return map {
            val service = serviceMap.getOrPut(it.serviceId) {
                serviceRepository.findDbService(it.serviceId)
            }
            it.toUiUser(service = service, htmlParser = htmlParser)
        }
    }

    private data class FilterResult(
        val users: List<UiUser>,
        val services: List<ServiceOfFollowingUsers>,
    )

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FollowingViewModel(
                userRepository = UserRepository.getDefault(context),
                serviceRepository = ServiceRepository.getDefault(context),
            ) as T
        }
    }
}