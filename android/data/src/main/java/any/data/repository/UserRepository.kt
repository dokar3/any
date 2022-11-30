package any.data.repository

import android.content.Context
import android.util.Log
import any.data.FetchState
import any.data.db.AppDatabase
import any.data.db.UserDao
import any.data.entity.ServiceManifest
import any.data.entity.User
import any.data.js.ServiceBridge
import any.data.js.ServiceBridgeImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

class UserRepository(
    private val userDao: UserDao,
    private val appBridge: ServiceBridge,
) : ReactiveRepository<String, User>() {
    fun fetchFollowing(serviceId: String?): Flow<List<User>> = flow {
        if (serviceId != null) {
            emit(userDao.getFollowing(serviceId = serviceId))
        } else {
            emit(userDao.getFollowing())
        }
    }

    fun fetchUserById(
        service: ServiceManifest,
        userId: String,
        control: FetchControl,
    ): Flow<FetchState<User>> {
        return fetchUser(
            app = service,
            userId = userId,
            userUrl = null,
            control = control,
        )
    }

    fun fetchUserByUrl(
        service: ServiceManifest,
        userUrl: String,
        control: FetchControl,
    ): Flow<FetchState<User>> {
        return fetchUser(
            app = service,
            userId = null,
            userUrl = userUrl,
            control = control,
        )
    }

    private fun fetchUser(
        app: ServiceManifest,
        userId: String?,
        userUrl: String?,
        control: FetchControl,
    ): Flow<FetchState<User>> = channelFlow {
        require(userId != null || userUrl != null)

        if (userId != null && control.includesSource(FetchSource.Cache)) {
            val user = userDao.get(serviceId = app.id, id = userId)
            if (user != null) {
                send(FetchState.success(value = user, isRemote = false))
                if (control.isOneShot()) {
                    channel.close()
                    return@channelFlow
                }
            }
        }

        if (!control.includesSource(FetchSource.Remote)) {
            channel.close()
            Log.d("UserRepo", "fetchUser: no remote source included, s: $control")
            return@channelFlow
        }

        val flow = if (userId != null) {
            appBridge.fetchUserById(service = app, id = userId)
        } else {
            appBridge.fetchUserByUrl(service = app, url = userUrl!!)
        }
        flow
            .onEach {
                if (it !is FetchState.Success) {
                    send(it)
                }
            }
            .mapNotNull { it as? FetchState.Success }
            .collect { state ->
                val user = state.value
                val cached = userDao.get(serviceId = app.id, id = user.id)
                val updated = updateUserFromCache(user, cached)
                if (cached == null) {
                    add(updated)
                } else {
                    update(updated)
                }
                send(FetchState.success(value = updated, isRemote = true))
            }

        channel.close()
    }

    private fun updateUserFromCache(
        user: User,
        cached: User?,
    ): User {
        if (cached == null) {
            return user
        }
        return user.copy(
            pageKeyOfPage2 = cached.pageKeyOfPage2,
            followedAt = cached.followedAt,
            group = cached.group,
        )
    }

    suspend fun add(user: User) {
        userDao.add(user)
        notifyInserted(user)
    }

    suspend fun update(user: User) {
        userDao.update(user)
        notifyUpdated(user)
    }

    suspend fun update(users: List<User>) {
        userDao.update(users)
        notifyUpdated(users)
    }

    suspend fun remove(user: User) {
        userDao.remove(user)
        notifyDeletedByItem(user)
    }

    companion object {
        @Volatile
        private var instance: UserRepository? = null

        fun getDefault(context: Context): UserRepository {
            return instance ?: synchronized(UserRepository::class) {
                instance ?: UserRepository(
                    userDao = AppDatabase.get(context).userDao(),
                    appBridge = ServiceBridgeImpl.getDefault(context),
                ).also { instance = it }
            }
        }
    }
}