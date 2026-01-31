package io.github.diarmaidob.anyfin.feature.settings

import io.github.diarmaidob.anyfin.core.entity.AnyfinDatabase
import io.github.diarmaidob.anyfin.core.entity.SessionState
import io.github.diarmaidob.anyfin.core.session.SessionRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val db: AnyfinDatabase,
    private val sessionRepo: SessionRepo
) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        db.transaction {
            db.mediaItemQueries.clearAll()
        }
        sessionRepo.saveSessionState(SessionState.LoggedOut)
    }
}