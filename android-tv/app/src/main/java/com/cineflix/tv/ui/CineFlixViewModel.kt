package com.cineflix.tv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cineflix.tv.data.local.AppPreferencesState
import com.cineflix.tv.data.local.CineFlixPreferences
import com.cineflix.tv.data.model.GenreDto
import com.cineflix.tv.data.model.MediaItem
import com.cineflix.tv.data.model.WatchHistoryEntry
import com.cineflix.tv.data.model.WatchlistEntry
import com.cineflix.tv.data.remote.CloudSyncRepository
import com.cineflix.tv.data.remote.LocalCompanionPairServer
import com.cineflix.tv.data.remote.TmdbApiClient
import com.cineflix.tv.ui.components.TvDestination
import com.cineflix.tv.ui.screens.ActivePlayerSession
import com.cineflix.tv.ui.screens.BrowseMode
import com.cineflix.tv.ui.screens.BrowseUiState
import com.cineflix.tv.ui.screens.HomeCatalogState
import com.cineflix.tv.ui.screens.SearchUiState
import com.cineflix.tv.ui.screens.TitleDetailsUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CineFlixViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = CineFlixPreferences(application)
    private val cloudSync = CloudSyncRepository()

    private val _prefs = MutableStateFlow(AppPreferencesState())
    val prefs: StateFlow<AppPreferencesState> = _prefs.asStateFlow()

    private val tmdbClient = TmdbApiClient { _prefs.value.tmdbCredential }

    private val _destination = MutableStateFlow(TvDestination.HOME)
    val destination: StateFlow<TvDestination> = _destination.asStateFlow()

    private val _genreMap = MutableStateFlow(TmdbApiClient.DEFAULT_GENRE_MAP)
    val genreMap: StateFlow<Map<Int, String>> = _genreMap.asStateFlow()

    private val _movieGenres = MutableStateFlow<List<GenreDto>>(emptyList())
    val movieGenres: StateFlow<List<GenreDto>> = _movieGenres.asStateFlow()

    private val _tvGenres = MutableStateFlow<List<GenreDto>>(emptyList())
    val tvGenres: StateFlow<List<GenreDto>> = _tvGenres.asStateFlow()

    private val _homeCatalog = MutableStateFlow(HomeCatalogState())
    val homeCatalog: StateFlow<HomeCatalogState> = _homeCatalog.asStateFlow()

    private val _browseState = MutableStateFlow(BrowseUiState())
    val browseState: StateFlow<BrowseUiState> = _browseState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _detailsStack = MutableStateFlow<List<TitleDetailsUiState>>(emptyList())
    val detailsStack: StateFlow<List<TitleDetailsUiState>> = _detailsStack.asStateFlow()

    private val _activePlayer = MutableStateFlow<ActivePlayerSession?>(null)
    val activePlayer: StateFlow<ActivePlayerSession?> = _activePlayer.asStateFlow()

    // Setup / Settings feedback state
    private val _isVerifyingKey = MutableStateFlow(false)
    val isVerifyingKey: StateFlow<Boolean> = _isVerifyingKey.asStateFlow()

    private val _keyError = MutableStateFlow<String?>(null)
    val keyError: StateFlow<String?> = _keyError.asStateFlow()

    private val _keyFeedback = MutableStateFlow<String?>(null)
    val keyFeedback: StateFlow<String?> = _keyFeedback.asStateFlow()

    private val _keyVerifiedFlag = MutableStateFlow(false)
    val keyVerifiedFlag: StateFlow<Boolean> = _keyVerifiedFlag.asStateFlow()

    private val _cloudAuthLoading = MutableStateFlow(false)
    val cloudAuthLoading: StateFlow<Boolean> = _cloudAuthLoading.asStateFlow()

    private val _cloudAuthError = MutableStateFlow<String?>(null)
    val cloudAuthError: StateFlow<String?> = _cloudAuthError.asStateFlow()

    private var searchDebounceJob: Job? = null

    val companionServer = LocalCompanionPairServer(
        onCredentialReceived = { key ->
            verifyAndSaveTmdbCredential(key)
        },
        onCustomStreamReceived = { streamUrl ->
            viewModelScope.launch {
                preferences.saveCustomStreamUrl(streamUrl)
                preferences.savePreferredServer("custom_url")
            }
        }
    )

    val companionUrl: String
        get() = "http://${companionServer.getLocalIpAddress()}:8765"

    init {
        companionServer.start(viewModelScope)

        viewModelScope.launch {
            val initial = preferences.stateFlow.first()
            _prefs.value = initial

            if (initial.tmdbCredential.isNotBlank()) {
                loadGenres()
                loadHomeCatalog()
                loadSearchSuggestions()
            }

            preferences.stateFlow.collect { updated ->
                val prevCred = _prefs.value.tmdbCredential
                _prefs.value = updated
                if (updated.tmdbCredential.isNotBlank() && updated.tmdbCredential != prevCred) {
                    tmdbClient.clearCache()
                    loadGenres()
                    loadHomeCatalog()
                    loadSearchSuggestions()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        companionServer.stop()
    }

    fun navigateTo(dest: TvDestination) {
        _detailsStack.value = emptyList()
        _destination.value = dest
        when (dest) {
            TvDestination.HOME -> {
                if (_homeCatalog.value.trending.isEmpty() && !_homeCatalog.value.isLoading) {
                    loadHomeCatalog()
                }
            }
            TvDestination.MOVIES -> switchBrowseMode(BrowseMode.MOVIE)
            TvDestination.TV -> switchBrowseMode(BrowseMode.TV)
            TvDestination.TRENDING -> switchBrowseMode(BrowseMode.TRENDING)
            TvDestination.SEARCH -> {
                if (_searchState.value.trendingSuggestions.isEmpty()) {
                    loadSearchSuggestions()
                }
            }
            TvDestination.MY_LIST, TvDestination.SETTINGS -> Unit
        }
    }

    fun verifyAndSaveTmdbCredential(rawKey: String) {
        viewModelScope.launch {
            _isVerifyingKey.value = true
            _keyError.value = null
            _keyFeedback.value = null
            _keyVerifiedFlag.value = false

            val result = tmdbClient.verifyCredential(rawKey)
            _isVerifyingKey.value = false

            if (result.valid) {
                val cleaned = TmdbApiClient.cleanCredential(rawKey)
                preferences.saveTmdbCredential(cleaned)
                tmdbClient.clearCache()
                _keyVerifiedFlag.value = true
                _keyFeedback.value = "TMDB key verified (${result.mode.uppercase()}) and saved."
                loadGenres()
                loadHomeCatalog()
                loadSearchSuggestions()
            } else {
                _keyError.value = result.errorMessage ?: "Invalid TMDB credential."
            }
        }
    }

    private fun loadGenres() {
        viewModelScope.launch {
            runCatching {
                coroutineScope {
                    val mDeferred = async { tmdbClient.getMovieGenres() }
                    val tvDeferred = async { tmdbClient.getTvGenres() }
                    val mList = mDeferred.await()
                    val tvList = tvDeferred.await()
                    _movieGenres.value = mList
                    _tvGenres.value = tvList
                    val merged = TmdbApiClient.DEFAULT_GENRE_MAP.toMutableMap()
                    mList.forEach { merged[it.id] = it.name }
                    tvList.forEach { merged[it.id] = it.name }
                    _genreMap.value = merged
                }
            }
        }
    }

    fun loadHomeCatalog() {
        viewModelScope.launch {
            _homeCatalog.update { it.copy(isLoading = true, errorMessage = null) }
            val affinity = _prefs.value.genreAffinity
            val topGenres = affinity.entries
                .sortedByDescending { it.value }
                .take(2)
                .map { it.key }
            val genreOrQuery = if (topGenres.isNotEmpty()) topGenres.joinToString("|") else "28|878"
            val firstWatchlist = _prefs.value.watchlist.firstOrNull()

            try {
                coroutineScope {
                    val trendingDef = async { runCatching { tmdbClient.getTrendingAllDay(1) } }
                    val recDef = async {
                        runCatching {
                            if (firstWatchlist != null) {
                                tmdbClient.getRecommendations(firstWatchlist.mediaType, firstWatchlist.id, 1)
                            } else {
                                tmdbClient.discoverMovies(genreQuery = genreOrQuery, sortBy = "popularity.desc", page = 1)
                            }
                        }
                    }
                    val popMoviesDef = async { runCatching { tmdbClient.getPopularMovies(1) } }
                    val topMoviesDef = async { runCatching { tmdbClient.getTopRatedMovies(1) } }
                    val upcomingDef = async { runCatching { tmdbClient.getUpcomingMovies(1) } }
                    val popTvDef = async { runCatching { tmdbClient.getPopularTv(1) } }
                    val topTvDef = async { runCatching { tmdbClient.getTopRatedTv(1) } }
                    val actionDef = async { runCatching { tmdbClient.discoverMovies("28,12", "popularity.desc", 1) } }
                    val sciFiDef = async { runCatching { tmdbClient.discoverMovies("878", "popularity.desc", 1) } }
                    val comedyDef = async { runCatching { tmdbClient.discoverMovies("35", "popularity.desc", 1) } }
                    val horrorDef = async { runCatching { tmdbClient.discoverMovies("27", "popularity.desc", 1) } }

                    val trendingRes = trendingDef.await()
                    val recRes = recDef.await()
                    val popMoviesRes = popMoviesDef.await()
                    val topMoviesRes = topMoviesDef.await()
                    val upcomingRes = upcomingDef.await()
                    val popTvRes = popTvDef.await()
                    val topTvRes = topTvDef.await()
                    val actionRes = actionDef.await()
                    val sciFiRes = sciFiDef.await()
                    val comedyRes = comedyDef.await()
                    val horrorRes = horrorDef.await()

                    val allResults = listOf(
                        trendingRes, recRes, popMoviesRes, topMoviesRes, upcomingRes,
                        popTvRes, topTvRes, actionRes, sciFiRes, comedyRes, horrorRes
                    )
                    if (allResults.none { it.isSuccess }) {
                        val firstErr = allResults.firstNotNullOfOrNull { it.exceptionOrNull() }
                        _homeCatalog.value = HomeCatalogState(
                            isLoading = false,
                            errorMessage = firstErr?.message ?: "Failed to load catalog from TMDB."
                        )
                        return@coroutineScope
                    }

                    val trendingList = trendingRes.getOrNull()?.results.orEmpty()
                    val rawRec = recRes.getOrNull()?.results.orEmpty()
                    val baseRec = if (rawRec.isNotEmpty()) rawRec else trendingList.drop(4).take(14)

                    _homeCatalog.value = HomeCatalogState(
                        isLoading = false,
                        errorMessage = null,
                        trending = trendingList,
                        recommended = TmdbApiClient.sortPersonalized(baseRec, affinity),
                        popularMovies = popMoviesRes.getOrNull()?.results.orEmpty(),
                        popularTv = popTvRes.getOrNull()?.results.orEmpty(),
                        topRatedMovies = topMoviesRes.getOrNull()?.results.orEmpty(),
                        upcomingMovies = upcomingRes.getOrNull()?.results.orEmpty(),
                        topRatedTv = topTvRes.getOrNull()?.results.orEmpty(),
                        actionAdventure = actionRes.getOrNull()?.results.orEmpty(),
                        sciFi = sciFiRes.getOrNull()?.results.orEmpty(),
                        comedy = comedyRes.getOrNull()?.results.orEmpty(),
                        horror = horrorRes.getOrNull()?.results.orEmpty()
                    )
                }
            } catch (e: Exception) {
                _homeCatalog.value = HomeCatalogState(
                    isLoading = false,
                    errorMessage = e.message ?: "Unexpected error loading CineFlix catalog."
                )
            }
        }
    }

    private fun switchBrowseMode(mode: BrowseMode) {
        _browseState.value = BrowseUiState(
            mode = mode,
            selectedGenreId = null,
            sortBy = "popularity.desc",
            isLoading = true
        )
        fetchBrowsePage(page = 1, append = false)
    }

    fun selectBrowseGenre(genreId: Int?) {
        _browseState.update { it.copy(selectedGenreId = genreId, isLoading = true) }
        fetchBrowsePage(page = 1, append = false)
    }

    fun selectBrowseSort(sortBy: String) {
        _browseState.update { it.copy(sortBy = sortBy, isLoading = true) }
        fetchBrowsePage(page = 1, append = false)
    }

    fun loadMoreBrowse() {
        val st = _browseState.value
        if (st.isLoadingMore || st.page >= st.totalPages) return
        _browseState.update { it.copy(isLoadingMore = true) }
        fetchBrowsePage(page = st.page + 1, append = true)
    }

    fun retryBrowse() {
        _browseState.update { it.copy(isLoading = true, errorMessage = null) }
        fetchBrowsePage(page = 1, append = false)
    }

    private fun fetchBrowsePage(page: Int, append: Boolean) {
        viewModelScope.launch {
            val st = _browseState.value
            try {
                val resp = when (st.mode) {
                    BrowseMode.TRENDING -> tmdbClient.getTrendingAllDay(page)
                    BrowseMode.MOVIE -> tmdbClient.discoverMovies(
                        genreQuery = st.selectedGenreId?.toString(),
                        sortBy = st.sortBy,
                        page = page
                    )
                    BrowseMode.TV -> tmdbClient.discoverTv(
                        genreQuery = st.selectedGenreId?.toString(),
                        sortBy = st.sortBy,
                        page = page
                    )
                }

                var incoming = resp.results
                if (st.mode == BrowseMode.TRENDING && st.selectedGenreId != null) {
                    incoming = incoming.filter { it.allGenreIds().contains(st.selectedGenreId) }
                }

                val merged = if (append) {
                    val existingKeys = st.items.map { "${it.resolvedMediaType()}_${it.id}" }.toSet()
                    st.items + incoming.filter { !existingKeys.contains("${it.resolvedMediaType()}_${it.id}") }
                } else {
                    incoming
                }

                _browseState.update {
                    it.copy(
                        items = merged,
                        page = page,
                        totalPages = resp.totalPages.coerceAtMost(500),
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _browseState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = e.message ?: "Unable to load catalog."
                    )
                }
            }
        }
    }

    fun loadSearchSuggestions() {
        viewModelScope.launch {
            runCatching {
                val resp = tmdbClient.getTrendingAllDay(1)
                val ranked = TmdbApiClient.sortPersonalized(resp.results, _prefs.value.genreAffinity)
                _searchState.update { it.copy(trendingSuggestions = ranked.take(15), errorMessage = null) }
            }.onFailure { err ->
                if (_searchState.value.query.isBlank()) {
                    _searchState.update { it.copy(errorMessage = err.message) }
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchState.update { it.copy(query = query) }
        searchDebounceJob?.cancel()
        if (query.isBlank()) {
            _searchState.update { it.copy(results = emptyList(), isLoading = false, errorMessage = null) }
            return
        }
        searchDebounceJob = viewModelScope.launch {
            delay(320L)
            executeSearch(query.trim(), page = 1, append = false)
        }
    }

    fun updateSearchTypeFilter(type: String) {
        _searchState.update { it.copy(typeFilter = type) }
    }

    fun updateSearchSortBy(sort: String) {
        _searchState.update { it.copy(sortBy = sort) }
    }

    fun loadMoreSearch() {
        val st = _searchState.value
        if (st.isLoadingMore || st.page >= st.totalPages || st.query.isBlank()) return
        _searchState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            executeSearch(st.query.trim(), page = st.page + 1, append = true)
        }
    }

    private suspend fun executeSearch(query: String, page: Int, append: Boolean) {
        if (!append) {
            _searchState.update { it.copy(isLoading = true, errorMessage = null) }
        }
        try {
            val resp = tmdbClient.searchMulti(query, page)
            val incoming = TmdbApiClient.sortPersonalized(resp.results, _prefs.value.genreAffinity)
            val merged = if (append) {
                val existing = _searchState.value.results.map { "${it.resolvedMediaType()}_${it.id}" }.toSet()
                _searchState.value.results + incoming.filter { !existing.contains("${it.resolvedMediaType()}_${it.id}") }
            } else incoming

            _searchState.update {
                it.copy(
                    results = merged,
                    page = page,
                    totalPages = resp.totalPages.coerceAtMost(500),
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            _searchState.update {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = e.message ?: "Search failed."
                )
            }
        }
    }

    fun openDetails(item: MediaItem) {
        val mediaType = item.resolvedMediaType()
        val initial = TitleDetailsUiState(
            mediaId = item.id,
            mediaType = mediaType,
            item = item,
            isLoadingDetails = true,
            isLoadingEpisodes = mediaType == "tv"
        )
        _detailsStack.update { it + initial }

        viewModelScope.launch {
            try {
                coroutineScope {
                    val detailsDef = async { runCatching { tmdbClient.getDetails(mediaType, item.id) } }
                    val creditsDef = async { runCatching { tmdbClient.getCredits(mediaType, item.id) } }
                    val videosDef = async { runCatching { tmdbClient.getVideos(mediaType, item.id) } }
                    val recDef = async { runCatching { tmdbClient.getRecommendations(mediaType, item.id, 1) } }
                    val simDef = async { runCatching { tmdbClient.getSimilar(mediaType, item.id, 1) } }
                    val seasonDef = if (mediaType == "tv") {
                        async { runCatching { tmdbClient.getTvSeason(item.id, 1) } }
                    } else null

                    val fullItem = detailsDef.await().getOrNull() ?: item
                    val credits = creditsDef.await().getOrNull()
                    val trailers = videosDef.await().getOrNull()?.selectBestTrailers().orEmpty()
                    val affinity = _prefs.value.genreAffinity
                    val recs = TmdbApiClient.sortPersonalized(recDef.await().getOrNull()?.results.orEmpty(), affinity)
                    val sims = TmdbApiClient.sortPersonalized(simDef.await().getOrNull()?.results.orEmpty(), affinity)
                    val episodes = seasonDef?.await()?.getOrNull()?.episodes.orEmpty()

                    recordGenreInteraction(fullItem.allGenreIds(), weight = 1)

                    _detailsStack.update { stack ->
                        stack.map { entry ->
                            if (entry.mediaId == item.id && entry.mediaType == mediaType) {
                                entry.copy(
                                    item = fullItem,
                                    cast = credits?.cast.orEmpty(),
                                    crew = credits?.crew.orEmpty(),
                                    trailers = trailers,
                                    recommended = recs,
                                    similar = sims,
                                    episodes = episodes,
                                    isLoadingDetails = false,
                                    isLoadingEpisodes = false
                                )
                            } else entry
                        }
                    }
                }
            } catch (e: Exception) {
                _detailsStack.update { stack ->
                    stack.map { entry ->
                        if (entry.mediaId == item.id && entry.mediaType == mediaType) {
                            entry.copy(
                                isLoadingDetails = false,
                                isLoadingEpisodes = false,
                                errorMessage = e.message
                            )
                        } else entry
                    }
                }
            }
        }
    }

    fun selectDetailsSeason(seasonNumber: Int) {
        val top = _detailsStack.value.lastOrNull() ?: return
        _detailsStack.update { stack ->
            stack.dropLast(1) + top.copy(selectedSeason = seasonNumber, isLoadingEpisodes = true)
        }
        viewModelScope.launch {
            val eps = runCatching { tmdbClient.getTvSeason(top.mediaId, seasonNumber).episodes }.getOrDefault(emptyList())
            _detailsStack.update { stack ->
                stack.map { entry ->
                    if (entry.mediaId == top.mediaId && entry.mediaType == top.mediaType && entry.selectedSeason == seasonNumber) {
                        entry.copy(episodes = eps, isLoadingEpisodes = false)
                    } else entry
                }
            }
        }
    }

    fun popDetails(): Boolean {
        if (_detailsStack.value.isEmpty()) return false
        _detailsStack.update { it.dropLast(1) }
        return true
    }

    fun startPlayback(
        item: MediaItem,
        season: Int = 1,
        episode: Int = 1,
        forceServerId: String? = null,
        explicitYoutubeKey: String? = null
    ) {
        val mediaType = item.resolvedMediaType()
        val currentPrefs = _prefs.value
        val savedHistory = currentPrefs.history.find { it.id == item.id && it.mediaType == mediaType }
        val resumeSeconds = if (savedHistory != null &&
            savedHistory.seasonNumber == season &&
            savedHistory.episodeNumber == episode &&
            savedHistory.progressSeconds > 5L
        ) {
            savedHistory.progressSeconds
        } else 0L

        val detailsTop = _detailsStack.value.lastOrNull()
        val knownTrailers = if (detailsTop?.mediaId == item.id) detailsTop.trailers else emptyList()
        val knownEpisodes = if (detailsTop?.mediaId == item.id && detailsTop.selectedSeason == season) {
            detailsTop.episodes
        } else emptyList()

        val initialSession = ActivePlayerSession(
            item = item,
            mediaType = mediaType,
            seasonNumber = season,
            episodeNumber = episode,
            serverId = forceServerId ?: currentPrefs.preferredServerId,
            customDirectUrl = currentPrefs.customStreamUrl,
            youtubeTrailerKey = explicitYoutubeKey ?: knownTrailers.firstOrNull()?.key,
            initialProgressSeconds = resumeSeconds,
            episodes = knownEpisodes
        )
        _activePlayer.value = initialSession

        recordGenreInteraction(item.allGenreIds(), weight = 2)
        recordWatchProgress(item, mediaType, season, episode, resumeSeconds, 7200L)

        // Fetch trailers and TV season episodes in background if not yet loaded
        viewModelScope.launch {
            if (initialSession.youtubeTrailerKey == null) {
                val ytKey = runCatching {
                    tmdbClient.getVideos(mediaType, item.id).selectBestTrailers().firstOrNull()?.key
                }.getOrNull()
                if (ytKey != null) {
                    _activePlayer.update { it?.copy(youtubeTrailerKey = ytKey) }
                }
            }
            if (mediaType == "tv" && initialSession.episodes.isEmpty()) {
                val eps = runCatching { tmdbClient.getTvSeason(item.id, season).episodes }.getOrDefault(emptyList())
                if (eps.isNotEmpty()) {
                    _activePlayer.update { it?.copy(episodes = eps) }
                }
            }
        }
    }

    fun selectPlayerServer(serverId: String) {
        viewModelScope.launch {
            preferences.savePreferredServer(serverId)
        }
        _activePlayer.update { it?.copy(serverId = serverId) }
    }

    fun selectPlayerEpisode(season: Int, episode: Int) {
        val current = _activePlayer.value ?: return
        _activePlayer.value = current.copy(
            seasonNumber = season,
            episodeNumber = episode,
            initialProgressSeconds = 0L
        )
        recordWatchProgress(current.item, current.mediaType, season, episode, 0L, 2700L)
    }

    fun updatePlayerCustomUrl(url: String) {
        viewModelScope.launch {
            preferences.saveCustomStreamUrl(url)
            preferences.savePreferredServer("custom_url")
        }
        _activePlayer.update { it?.copy(customDirectUrl = url.trim(), serverId = "custom_url") }
    }

    fun closePlayer() {
        _activePlayer.value = null
    }

    fun isInWatchlist(tmdbId: Int, mediaType: String): Boolean =
        _prefs.value.watchlist.any { it.id == tmdbId && it.mediaType == mediaType }

    fun toggleWatchlist(item: MediaItem) {
        val mediaType = item.resolvedMediaType()
        val current = _prefs.value.watchlist
        val exists = current.any { it.id == item.id && it.mediaType == mediaType }

        viewModelScope.launch {
            val account = _prefs.value.cloudAccount
            if (exists) {
                val updated = current.filterNot { it.id == item.id && it.mediaType == mediaType }
                preferences.updateWatchlist(updated)
                if (account != null) {
                    cloudSync.deleteWatchlistItem(account, mediaType, item.id)
                }
            } else {
                val entry = WatchlistEntry(
                    id = item.id,
                    mediaType = mediaType,
                    title = item.resolvedTitle,
                    overview = item.overview.orEmpty(),
                    posterPath = item.posterPath,
                    backdropPath = item.backdropPath,
                    releaseDate = item.releaseDate ?: item.firstAirDate.orEmpty(),
                    voteAverage = item.voteAverage,
                    genreIds = item.allGenreIds()
                )
                val updated = listOf(entry) + current
                preferences.updateWatchlist(updated)
                recordGenreInteraction(item.allGenreIds(), weight = 2)
                if (account != null) {
                    cloudSync.upsertWatchlistItem(account, entry)
                }
            }
        }
    }

    fun removeFromWatchlist(entry: WatchlistEntry) {
        viewModelScope.launch {
            val updated = _prefs.value.watchlist.filterNot {
                it.id == entry.id && it.mediaType == entry.mediaType
            }
            preferences.updateWatchlist(updated)
            _prefs.value.cloudAccount?.let { acc ->
                cloudSync.deleteWatchlistItem(acc, entry.mediaType, entry.id)
            }
        }
    }

    fun recordWatchProgress(
        item: MediaItem,
        mediaType: String,
        season: Int,
        episode: Int,
        progressSec: Long,
        durationSec: Long
    ) {
        viewModelScope.launch {
            val entry = WatchHistoryEntry(
                id = item.id,
                mediaType = mediaType,
                title = item.resolvedTitle,
                overview = item.overview.orEmpty(),
                posterPath = item.posterPath,
                backdropPath = item.backdropPath,
                releaseDate = item.releaseDate ?: item.firstAirDate.orEmpty(),
                voteAverage = item.voteAverage,
                genreIds = item.allGenreIds(),
                progressSeconds = progressSec,
                durationSeconds = durationSec,
                seasonNumber = season,
                episodeNumber = episode,
                watchedAt = System.currentTimeMillis()
            )
            val current = _prefs.value.history.filterNot {
                it.id == item.id && it.mediaType == mediaType
            }
            val updated = (listOf(entry) + current).take(50)
            preferences.updateHistory(updated)
            _prefs.value.cloudAccount?.let { acc ->
                cloudSync.upsertHistoryItem(acc, entry)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            preferences.updateHistory(emptyList())
        }
    }

    private fun recordGenreInteraction(genreIds: List<Int>, weight: Int) {
        if (genreIds.isEmpty()) return
        viewModelScope.launch {
            val updated = _prefs.value.genreAffinity.toMutableMap()
            genreIds.forEach { gid ->
                updated[gid] = ((updated[gid] ?: 0) + weight).coerceAtMost(25)
            }
            preferences.updateGenreAffinity(updated)
        }
    }

    fun setPreferredServer(serverId: String) {
        viewModelScope.launch { preferences.savePreferredServer(serverId) }
    }

    fun setAutoFailover(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoFailover(enabled) }
    }

    fun setAutoplayTrailers(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoplayTrailers(enabled) }
    }

    fun saveCustomStreamUrl(url: String) {
        viewModelScope.launch { preferences.saveCustomStreamUrl(url) }
    }

    fun signInCloud(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _cloudAuthError.value = "Please enter both email and password."
            return
        }
        viewModelScope.launch {
            _cloudAuthLoading.value = true
            _cloudAuthError.value = null
            val res = cloudSync.signInWithEmail(email, password)
            _cloudAuthLoading.value = false
            res.onSuccess { account ->
                preferences.saveCloudAccount(account)
                cloudSync.fetchWatchlist(account).onSuccess { cloudList ->
                    if (cloudList.isNotEmpty()) {
                        val merged = (cloudList + _prefs.value.watchlist)
                            .distinctBy { "${it.mediaType}_${it.id}" }
                        preferences.updateWatchlist(merged)
                    }
                }
            }.onFailure { err ->
                _cloudAuthError.value = err.message ?: "Sign-in failed."
            }
        }
    }

    fun signUpCloud(email: String, password: String) {
        if (email.isBlank() || password.length < 6) {
            _cloudAuthError.value = "Enter a valid email and a password of at least 6 characters."
            return
        }
        viewModelScope.launch {
            _cloudAuthLoading.value = true
            _cloudAuthError.value = null
            val res = cloudSync.signUpWithEmail(email, password)
            _cloudAuthLoading.value = false
            res.onSuccess { account ->
                preferences.saveCloudAccount(account)
                _prefs.value.watchlist.forEach { entry ->
                    cloudSync.upsertWatchlistItem(account, entry)
                }
            }.onFailure { err ->
                _cloudAuthError.value = err.message ?: "Registration failed."
            }
        }
    }

    fun signOutCloud() {
        viewModelScope.launch {
            preferences.saveCloudAccount(null)
        }
    }
}
